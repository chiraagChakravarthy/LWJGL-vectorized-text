#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define R 0.7071067812
#define PI 3.1415926535
//scaling factor of intersect window for pixel

uniform isamplerBuffer u_Atlas;
uniform float u_EmScale;
uniform mat4 u_Mvp;
uniform vec4 u_Viewport;
uniform int u_FontLen;

//size of pixel in glyph space
//depending on program to provide this info

in vec2 vScreenPos;//position in screen space
in float vIndex;//which glyph to render
in mat4 vPose;
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
    vec2 roots = vec2(1);
    if(abs(a)<epsilon){
        if(abs(b)>epsilon){
            roots.x = -c/b;
            float t = -c/b;
        }
    } else {
        float dis = b*b-4*a*c;
        if(dis >= 0){
            float sqr = sqrt(dis);
            float t1 = (-b-sqr)/(2*a);
            float t2 = (-b+sqr)/(2*a);
            roots.x = min(t1, t2);
            roots.y = max(t1, t2);
        }
    }
    return roots;
}

vec2 interceptLineBezier(float a, float b, float c, float d, float e, float f, float sx, float sy){
    float A,B,C;
    if (abs(sx) < epsilon) {
        A = a;
        B = b;
        C = c;
    } else if (abs(sy) < epsilon) {
        A = d;
        B = e;
        C = f;
    } else {
        A = a / sx - d / sy;
        B = b / sx - e / sy;
        C = c / sx - f / sy;
    }
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

vec3 solveCubic(float d, float a, float b, float c){
    a = a/d;
    b = b/d;
    c = c/d;

    float p = b/3-a*a/9;
    float q = a*a*a/27-a*b/6+c/2;
    float D = p*p*p+q*q;
    vec3 roots = vec3(1);

    if(D>=0){
        if(D==0){
            float r = cbrt(-q);
            roots.x = 2*r;
            roots.y = -r;
        } else {
            float r = cbrt(-q+sqrt(D)),
            s = cbrt(-q-sqrt(D));
            roots.x = r+s;
        }
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
    diff = mix(mix(diff, diff-2*PI, diff>PI), diff+2*PI, diff<-PI);
    if(abs(diff)<1){
        return diff;//removes degenerate cases
    }
    float theta2 = theta0+diff/2;
    float sx = cos(theta2),
    sy = sin(theta2);
    vec2 roots = interceptLineBezier(a, b, c, d, e, f, sx, sy);
    float t12 = mix(t1, t0, 0.5);
    float t2 = mix(roots.y, roots.x, abs(roots.y-t12)>abs(roots.x-t12));

    vec2 x2 = vec2(a,d)*t2*t2+vec2(b,e)*t2+vec2(c,f);
    float prod = dot(x2, vec2(sx, sy));

    if(prod>0){
        return diff;//short way
    }

    //it took the long way round
    return mix(mix(diff, diff-2*PI, diff>0), diff+2*PI, diff<0);
    //if somehow a 0 diff manages to make it through here then there you go
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

    vec3 stationary = sort(solveCubic(4*a, 3*b, 2*c, d));
    vec4 roots = vec4(quadApprox(a, b, c, d, e, stationary.x), quadApprox(a, b, c, d, e, stationary.z));
    vec3 v = evalQuartic(a, b, c, d, e, vec4(stationary, 1)).xyz;
    roots = mix(vec4(1), roots, bvec4(v.x<0, v.x<0&&v.y>0, v.z<0&&v.y>0, v.z<0));
    return roots;
}


float calcArea(vec2 pos, float r){

    float total = 0;

    int index = int(vIndex);

    //iterate through beziers
    int start = texelFetch(u_Atlas, index).x, end = texelFetch(u_Atlas, index +1).x;
    mat4 transform = u_Mvp * vPose;
    float R2 = r*r;

    vec2 minA = pos-vec2(r), maxA = pos+vec2(r);

    for (int i = start; i < end; i++) {
        int j = i*6 + u_FontLen*4+u_FontLen+1;

        vec2 A = (transform * vec4(fetch(j), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
        vec2 B = (transform * vec4(fetch(j + 1), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
        vec2 C = ((transform * vec4(fetch(j + 2), 0, 1)).xy + vec2(1, 1)) * u_Viewport.zw / 2 / u_EmScale;
        C -= pos;
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
            total -= angleIntegrate(a, b, c, d, e, f, 0, 1);
            continue;
        }

        float k4 = a*a+d*d,
        k3 = 2*(a*b+d*e),
        k2 = 2*a*c+b*b+2*d*f+e*e,
        k1 = 2*(b*c+e*f),
        k0 = c*c+f*f-R2;

        vec4 roots = solveQuartic(k4, k3, k2, k1, k0);

        float i3 = (d*b-a*e)*.5/3,
        i2 = .5*(c*d-a*f),
        i1 = .5*(e*c-b*f);

        float ta = 0;
        float tb = roots.x;
        float delta = 0;
        delta -= angleIntegrate(a, b, c, d, e, f, clamp(ta, 0, 1), clamp(tb, 0, 1));

        ta = tb;
        tb = roots[1];
        delta -= windowIntegrate(i3, i2, i1, clamp(ta, 0, 1), clamp(tb, 0, 1), r);

        ta = tb;
        tb = roots[2];
        delta -= angleIntegrate(a, b, c, d, e, f, clamp(ta, 0, 1), clamp(tb, 0, 1));

        ta = tb;
        tb = roots[3];
        delta -= windowIntegrate(i3, i2, i1, clamp(ta, 0, 1), clamp(tb, 0, 1), r);

        ta = tb;
        tb = 1;
        delta -= angleIntegrate(a, b, c, d, e, f, clamp(ta, 0, 1), clamp(tb, 0, 1));
        total += delta;
    }

    return total;
}

void main () {
    vec2 pixelPos = (vScreenPos+vec2(1))*u_Viewport.zw/2/u_EmScale;

    float r = R/u_EmScale;

    float area = calcArea(pixelPos, r)/PI/2;

    color = vec4(vTint.rgb, area);
    //color = vec4(1, 0, 0, 1);
}
