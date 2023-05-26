#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define R 0.7071067812
#define PI 3.1415926535
//scaling factor of intersect window for pixel

uniform isamplerBuffer u_Atlas;
uniform float u_EmScale;
uniform vec4 u_Viewport;
uniform int u_FontLen;

//size of pixel in glyph space
//depending on program to provide this info

in vec2 vScreenPos;//position in screen space
in float vIndex;//which glyph to render
in mat4 vTransform;
in vec4 vTint;

vec2 fetch(int j){
    return  vec2(texelFetch(u_Atlas, j).x, texelFetch(u_Atlas, j+3).x);
}

vec4 evalQuartic(float a, float b, float c, float d, float e, vec4 t){
    vec4 t2 = t*t;
    vec4 t3 = t2*t;
    vec4 t4 = t3*t;
    return a*t4+b*t3+c*t2+d*t+e;
}

vec2 solveQuadratic(float a, float b, float c){
    float dis = b*b-4*a*c;
    vec2 roots =  mix(mix(vec2(2), vec2(-c/b, 2), bvec2(abs(b)>epsilon)), mix(vec2(2), (vec2(-b)+vec2(sqrt(dis))*vec2(-1, 1))/(2*a), bvec2(dis>=0)), bvec2(abs(a)>epsilon));
    return vec2(min(roots.x, roots.y), max(roots.x, roots.y));
}

vec2 intersectLineBezier(float a, float b, float c, float d, float e, float f, float sx, float sy){
    float A = a*sy-d*sx,
    B = b*sy-e*sx,
    C = c*sy-f*sx;
    return solveQuadratic(A, B, C);
}

float cbrt(float v){
    float cbr = pow(abs(v), .3333333);
    return mix(cbr, -cbr, v<0);
}

float windowIntegrate(float a, float b, float c, float ta, float tb, float r){
    float t1 = tb-ta,
    t2 = tb*tb-ta*ta,
    t3 = tb*tb*tb-ta*ta*ta;
    return (a*t3+b*t2+c*t1)/r/r;
}

vec3 solveCubic(float a, float b, float c){
    float p = b/3-a*a/9;
    float q = a*a*a/27-a*b/6+c/2;
    float D = p*p*p+q*q;
    vec3 roots = vec3(1);

    if(D>=0){
        float cbrtq = cbrt(-q),
                r = cbrt(-q+sqrt(D)),
                s = cbrt(-q-sqrt(D));
        roots = mix(vec3(r+s, 1, 1), vec3(2*cbrtq, -cbrtq, 1), bvec3(D==0));
    } else {
        float ang = acos(-q/sqrt(-p*p*p)),
        r = 2*sqrt(-p);
        roots.x = r*cos((ang+2*PI)/3);
        roots.y = r*cos(ang/3);
        roots.z = r*cos((ang-2*PI)/3);
    }

    roots -= vec3(a/3);
    return roots;
}


vec3 sort(vec3 v){
    vec2 t = vec2(min(v.x, v.y), max(v.x, v.y));
    return vec3(min(t.x, v.z), min(max(v.z, t.x), t.y), max(t.y, v.z));
}

float angleIntegrate(float a, float b, float c, float d, float e, float f, float t0, float t1){
    float x0 = a*t0*t0+b*t0+c,
    y0 = d*t0*t0+e*t0+f,
    x1 = a*t1*t1+b*t1+c,
    y1 = d*t1*t1+e*t1+f;
    float theta0 = atan(y0, x0),
    theta1 = atan(y1, x1);
    float diff = theta1-theta0;
    return diff;
}

vec2 quadApprox(float a, float b, float c, float d, float e, float t) {
    //Q(t) = P(t)
    //Q'(t) = P'(t)
    //Q''(t) = P''(t)

    float t2 = t*t;
    float t3 = t2*t;
    float t4 = t3*t;

    //2A = d/dt (4at3+3bt2+2ct+d) = 12at2 + 6bt + 2c
    float A = .5*(12*a*t2 + 6*b*t + 2*c);

    //2At+B = 4at3+3bt2+2ct+d
    //B = 4at3+3bt2+2ct+d-2At
    float B = 4*a*t3+3*b*t2+2*(c-A)*t+d;

    //at2+bt+c = at4+bt3+ct2+dt+e
    //c = at4+bt3+ct2+dt+e-bt-at2
    float C = a*t4+b*t3+(c-A)*t2+(d-B)*t+e;

    return solveQuadratic(A, B, C);
}

vec4 solveQuartic(float a, float b, float c, float d, float e){
    if(abs(a)<epsilon){
        return vec4(solveQuadratic(c, d, e), 1, 1);
    }
    vec3 stationary = sort(solveCubic(3*b/4/a, 2*c/4/a, d/4/a));
    vec4 roots = vec4(quadApprox(a, b, c, d, e, stationary.x), quadApprox(a, b, c, d, e, stationary.z));
    vec3 v = evalQuartic(a, b, c, d, e, vec4(stationary, 1)).xyz;
    roots = mix(vec4(1), roots, bvec4(v.x<0, v.x<0&&v.y>0, v.z<0&&v.y>0, v.z<0));
    return roots;
}

int testIntersect(vec2 p, vec2 d, float t){
    float delta = sign(d.y);
    bool good = p.x>0 && t>0 && t<1;
    return int(mix(0., 1., good));
}

int countWinds(vec2 A, vec2 B, vec2 C){
    vec2 s = vec2(1, 0);
    float a = A.x;
    float b = B.x;
    float c = C.x;
    float d = A.y;
    float e = B.y;
    float f = C.y;

    vec2 hits = intersectLineBezier(a, b, c, d, e, f, s.x, s.y);//min, max
    float t0 = hits.x, t1 = hits.y;
    vec2 p0 = A*t0*t0+B*t0+C;
    vec2 p1 = A*t1*t1+B*t1+C;
    vec2 d0 = 2*A*t0+B;
    vec2 d1 = 2*A*t1+B;

    int count = testIntersect(p0, d0, t0);
    count += testIntersect(p1, d1, t1);
    return count;
}


float calcArea(vec2 pos, float r){

    float total = 0;

    int index = int(vIndex);

    //iterate through beziers
    int start = texelFetch(u_Atlas, index).x, end = texelFetch(u_Atlas, index +1).x;
    float R2 = r*r;

    vec2 minA = pos-vec2(r), maxA = pos+vec2(r);
    bool intersects = false;
    int winds = 0;

    for (int i = start; i < end; i++) {
        int j = i*6 + u_FontLen*4+u_FontLen+1;

        vec2 A = (vTransform * vec4(fetch(j), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
        vec2 B = (vTransform * vec4(fetch(j + 1), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
        vec2 C = ((vTransform * vec4(fetch(j + 2), 0, 1)).xy + vec2(1, 1)) * u_Viewport.zw / 2 / u_EmScale;
        C -= pos;
        winds += countWinds(A, B, C);
        float a = A.x;
        float b = B.x;
        float c = C.x;
        float d = A.y;
        float e = B.y;
        float f = C.y;

        float tx = clamp(mix(-b/(2*a), 0, abs(a)<epsilon), 0, 1);
        float ty = clamp(mix(-e/(2*d), 0, abs(d)<epsilon), 0, 1);
        vec2 p0 = C,
                p1 = A+B+C,
                px = A*tx*tx+B*tx+C,
                py = A*ty*ty+B*ty+C;


        vec2 minB = min(min(p0, p1), min(px, py))+pos,
        maxB = max(max(p0, p1), max(px, py))+pos;
        if(!(all(lessThan(minA, maxB))&&all(lessThan(minB, maxA)))){
            continue;
        }

        float k4 = a*a+d*d,
        k3 = 2*(a*b+d*e),
        k2 = 2*a*c+b*b+2*d*f+e*e,
        k1 = 2*(b*c+e*f),
        k0 = c*c+f*f-R2;

        vec4 roots = clamp(solveQuartic(k4, k3, k2, k1, k0), vec4(0), vec4(1));

        intersects = intersects || (abs(roots.x-roots.y)>epsilon || abs(roots.z-roots.w)>epsilon);

        float i3 = (d*b-a*e)*.5/3,
        i2 = .5*(c*d-a*f),
        i1 = .5*(e*c-b*f);

        total -= windowIntegrate(i3, i2, i1, roots.x, roots.y, r);
        total += angleIntegrate(a, b, c, d, e, f, roots.x, roots.y);

        total -= windowIntegrate(i3, i2, i1, roots.z, roots.w, r);
        total += angleIntegrate(a, b, c, d, e, f, roots.z, roots.w);
    }

    return mix(mix(0.0, 2*PI, mod(winds, 2)==1), mod(total, 2*PI), intersects);
}

void main () {
    vec2 pixelPos = (vScreenPos+vec2(1))*u_Viewport.zw/2/u_EmScale;

    float r = R/u_EmScale;

    float area = calcArea(pixelPos, r)/PI/2;

    color = vec4(vTint.rgb, area*vTint.a);
}
