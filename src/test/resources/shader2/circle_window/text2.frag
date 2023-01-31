#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define R 0.7071067812
#define PI 3.1415926535
//scaling factor of intersect window for pixel

uniform isamplerBuffer u_Atlas;
uniform vec4 u_Tint;
uniform float u_EmScale;
uniform mat4 u_Mvp;
uniform mat4 u_Pose;
uniform mat4 u_Screen;
uniform vec4 u_Viewport;
uniform int u_FontLen;

//size of pixel in glyph space
//depending on program to provide this info

in vec2 vScreenPos;//position in screen space
in float vIndex;//which glyph to render
in float vAdvance;

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

vec4 seek(float a, float b, float c, float d, float e, vec4 t0, vec4 t1){
    vec4 interval = (t1-t0)/2;
    vec4 t2 = t0;
    for(int i = 0; i < 20; i++){
        vec4 v2 = evalQuartic(a, b, c, d, e, t2);
        t2 += mix(-interval, interval, lessThan(v2, vec4(0)));
        interval /= 2;
    }
    return t2;
}

vec3 sort(vec3 v){
    vec2 t = vec2(min(v.x, v.y), max(v.x, v.y));
    return vec3(min(t.x, v.z), min(max(v.z, t.x), t.y), max(t.y, v.z));
}

vec4 solveQuartic(float a, float b, float c, float d, float e){
    if(abs(a)<epsilon){
        return vec4(solveQuadratic(c, d, e), 1, 1);
    }

    vec3 st = solveCubic(4*a, 3*b, 2*c, d);
    st = sort(st);
    vec3 v = evalQuartic(a, b, c, d, e, vec4(st, 1)).xyz;

    vec4 t0 = vec4(st.x, st.x, st.z, st.z);
    vec4 t1 = vec4(0, st.y, st.y, 1);
    vec4 roots = seek(a, b, c, d, e, t0, t1);

    roots = mix(vec4(1), roots, bvec4(v.x<0, v.x<0&&v.y>0, v.z<0&&v.y>0, v.z<0));
    return roots;
}

int solveQuartic2(in float a, in float b, in float c, in float d, in float e, inout vec4 roots) {
    roots = vec4(1);
    b /= a; c /= a; d /= a; e /= a; // Divide by leading coefficient to make it 1

    // Depress the quartic to x^4 + px^2 + qx + r by substituting x-b/4a
    // This can be found by substituting x+u and the solving for the value
    // of u that makes the t^3 term go away
    float bb = b * b;
    float p = (8.0 * c - 3.0 * bb) / 8.0;
    float q = (8.0 * d - 4.0 * c * b + bb * b) / 8.0;
    float r = (256.0 * e - 64.0 * d * b + 16.0 * c * bb - 3.0 * bb * bb) / 256.0;
    int n = 0; // Root counter

    // Solve for a root to (t^2)^3 + 2p(t^2)^2 + (p^2 - 4r)(t^2) - q^2 which resolves the
    // system of equations relating the product of two quadratics to the depressed quartic
    float ra = 2.0 * p;
    float rb = p * p - 4.0 * r;
    float rc = -q * q;

    // Depress using the method above
    float ru = ra / 3.0;
    float rp = rb - ra * ru;
    float rq = rc - (rb - 2.0 * ra * ra / 9.0) * ru;

    float lambda;
    float rh = 0.25 * rq * rq + rp * rp * rp / 27.0;
    if (rh > 0.0) { // Use Cardano's formula in the case of one real root
                    rh = sqrt(rh);
                    float ro = -0.5 * rq;
                    lambda = cbrt(ro - rh) + cbrt(ro + rh) - ru;
    }

    else { // Use complex arithmetic in the case of three real roots
           float rm = sqrt(-rp / 3.0);
           lambda = -2.0 * rm * sin(asin(1.5 * rq / (rp * rm)) / 3.0) - ru;
    }

    // Newton iteration to fix numerical problems (using Horners method)
    // Suggested by @NinjaKoala
    for (int i = 0; i < 2; i++) {
        float a_2 = ra + lambda;
        float a_1 = rb + lambda * a_2;
        float b_2 = a_2 + lambda;

        float f = rc + lambda * a_1; // Evaluation of λ^3 + ra * λ^2 + rb * λ + rc
        float f1 = a_1 + lambda * b_2; // Derivative

        lambda -= f / f1; // Newton iteration step
    }

    // Solve two quadratics factored from the quartic using the cubic root
    if (lambda < 0.0) return n;
    float t = sqrt(lambda); // Because we solved for t^2 but want t
    float alpha = 2.0 * q / t, beta = lambda + ra;

    float u = 0.25 * b;
    t *= 0.5;

    float z = -alpha - beta;
    if (z > 0.0) {
        z = sqrt(z) * 0.5;
        float h = + t - u;
        roots.xy = vec2(h - z, h + z);
        n += 2;
    }

    float w = + alpha - beta;
    if (w > 0.0) {
        w = sqrt(w) * 0.5;
        float h = -t - u;
        roots.zw = vec2(h - w, h + w);
        if (n == 0) roots.xy = roots.zw;
        n += 2;
    }

    return n;
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
    if(abs(diff)<.01f){
        return diff;//removes degenerate cases
    }
    float theta2 = theta0+diff/2;
    float sx = cos(theta2),
    sy = sin(theta2);
    vec2 roots = interceptLineBezier(a, b, c, d, e, f, sx, sy);
    float t2 = mix(roots.y, roots.x, roots.x<t1&&roots.x>t0);

    vec2 x2 = vec2(a,d)*t2*t2+vec2(b,e)*t2+vec2(c,f);
    float prod = dot(x2, vec2(sx, sy));

    if(prod>0){
        return diff;
    }
    //it took the long way round

    return mix(mix(diff, diff-2*PI, diff>0), diff+2*PI, diff<0);
    //if somehow a 0 diff manages to make it through here then there you go
}


float calcArea(vec2 pos, float r){

    float total = 0;

    int index = int(vIndex);

    //iterate through beziers
    int start = texelFetch(u_Atlas, index).x, end = texelFetch(u_Atlas, index +1).x;
    mat4 transform = u_Mvp * u_Pose;

    for (int i = start; i < end; i++) {
        int j = i*6 + u_FontLen*4+u_FontLen+1;

        float a, b, c, d, e, f;
        {
            vec2 A = (transform * vec4(u_EmScale * fetch(j), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
            vec2 B = (transform * vec4(u_EmScale * fetch(j + 1), 0, 0)).xy * u_Viewport.zw / 2 / u_EmScale;
            vec2 C = ((transform * vec4(u_EmScale * (fetch(j + 2) + vec2(vAdvance, 0)), 0, 1)).xy + vec2(1, 1)) * u_Viewport.zw / 2 / u_EmScale;
            C -= pos;
            a = A.x;
            b = B.x;
            c = C.x;
            d = A.y;
            e = B.y;
            f = C.y;
        }

        float k4 = a*a+d*d,
        k3 = 2*(a*b+d*e),
        k2 = 2*a*c+b*b+2*d*f+e*e,
        k1 = 2*(b*c+e*f),
        k0 = c*c+f*f-r*r;

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
        delta -= angleIntegrate(a, b, c, d, e, f, clamp(min(roots[1], roots[3]), 0, 1), clamp(max(roots[0], roots[2]), 0, 1));

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

    area = abs(area);
    area = clamp(area, 0.0, 1.0);
    color = vec4(u_Tint.rgb, area * u_Tint.a);
}
