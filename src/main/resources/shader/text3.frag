#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define PI 3.14159265359
#define window 1
//scaling factor of intersect window for pixel

uniform isamplerBuffer u_Atlas;
uniform vec4 u_Tint;
uniform float u_EmScale;
uniform mat4 u_Mvp;
uniform mat4 u_Pose;
uniform mat4 u_Screen;
uniform vec4 u_Viewport;

//size of pixel in glyph space
//depending on program to provide this info

in vec2 vScreenPos;//position in screen space
in float vGlyph;//which glyph to render
in float vAdvance;

float cbrt(in float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

int solveQuartic(in float a, in float b, in float c, in float d, in float e, inout vec4 roots) {
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
    float ra =  2.0 * p;
    float rb =  p * p - 4.0 * r;
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
    for(int i=0; i < 2; i++) {
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
        float h = +t - u;
        roots.xy = vec2(h - z, h + z);
        n += 2;
    }

    float w = +alpha - beta;
    if (w > 0.0) {
        w = sqrt(w) * 0.5;
        float h = -t - u;
        roots.zw = vec2(h - w, h + w);
        if (n == 0) roots.xy = roots.zw;
        n += 2;
    }

    return n;
}

int findIntercepts(float , out vec4 roots){
    //(ax2+bx+c-ox)^2+(dx2+ex+f-oy)^2-r2=0

    return solveQuartic(A, B, C, D, E, roots);
}

//optimize and stabilize later, if needed
//integrates the window function 1-r^2 over the polar region from tb to ta
//negative at the end bc clockwise is positive
float integrateWindow(float a, float b, float c, float d, float e, float f, float g,  float ta, float tb){
    float t2a = ta*ta;
    float t3a = t2a * ta;
    float t4a = t3a * ta;
    float t5a = t4a * ta;
    float t6a = t5a * ta;
    float t7a = t6a * ta;

    float t2b = tb*tb;
    float t3b = t2b * tb;
    float t4b = t3b * tb;
    float t5b = t4b * tb;
    float t6b = t5b * tb;
    float t7b = t6b * tb;

    float val = -(a*(t7a-t7b)+b*(t6a-t6b)+c*(t5a-t5b)+d*(t4a-t4b)+e*(t3a-t3b)+f*(t2a-t2b)+g*(ta-tb));

    return val;
}

void windowIntegralCoeff(float A, float B, float C, float D, float E, float F, out float a, out float b, out float c, out float d, out float e, out float f, out float g){
    a = 15 * (D * D + A * A) * (A * E - B * D)/210;
    b = 35 * (A * D * D * F + A * A * A * F + A * D * E * E - B * D * D * E + A * A * B * E - C * D * D * D - A * A * C * D - A * B * B * D)/210;
    c = 21*(6 * A * D * E * F - B * D * D * F + 5 * A * A * B * F + A * E * E * E - B * D * E * E - 5 * C * D * D * E + A * A * C * E + A * B * B * E - 6 * A * B * C * D - B * B * B * D)/210;
    d = 105 * (A * F - C * D) * (D * F + E * E + A * C + B * B)/210;
    e = 35 * (5 * A * E * F * F + B * D * F * F + B * E * E * F - 6 * C * D * E * F + 6 * A * B * C * F + B * B * B * F - C * E * E * E - A * C * C * E - B * B * C * E - 2 * A * E - B * (5 * C * C - 2) * D)/210;
    f = 105 * (F * (A * F * F + B * E * F - C * D * F - C * E * E + A * C * C + B * B * C - 2 * A) - C * (B * C * E + C * C * D - 2 * D))/210;
    g = 105 * (B * F * F * F - 105 * C * E * F * F + 105 * B * (C * C - 2) * F - 105 * C * (C * C - 2) * E)/210;
}

//counterclockwise is negative, clockwise is positive
float integrateArea(float A, float B, float C, float D, float E, float F, float ta, float tb){
    float avg = (A+B+C+D+E+F)/6;
    A /= avg;
    B /= avg;
    C /= avg;
    D /= avg;
    E /= avg;
    F /= avg;

    float t2a = ta*ta;
    float t3a = t2a *ta;
    float t2b = tb*tb;
    float t3b = t2b*tb;
    return -(1/3*(B*D-A*E)*(t3a-t3b) + (D*C-A*F)*(t2a-t2b) + (C*E-B*F)*(ta-tb))/2*avg;
}

//negative area means ccw means negative angle
float integrateAngle(float A, float B, float C, float D, float E, float F, float t0, float t1, float area){
    vec2 a = vec2(A,C);
    vec2 b = vec2(B,E);
    vec2 c = vec2(C,F);
    vec2 p0 = a*t0*t0+b*t0+c;
    vec2 p1 = a*t1*t1+b*t1+c;
    float t0 = atan(p0.y, p0.x);
    float t1 = atan(p1.y, p1.x);
    float dt = -(t1-t0);//signed delta. in range [-2pi, 2pi]

    return dt-sign(dt)*mix(0, 2*PI, area*dt<0);

    /*
    if(dt<0 && area>0){
        return 2*PI+dt;
    }
    if(dt>0 && area<0){
        return dt-2*PI;
    }*/
}

vec2 fetch(int j){
    return  vec2(texelFetch(u_Atlas, j).x, texelFetch(u_Atlas, j+3).x);
}

float calcArea(vec2 o){
    float total = 0;

    int iGlyph = int(vGlyph);

    //iterate through beziers
    int start = texelFetch(u_Atlas, iGlyph).x, end = texelFetch(u_Atlas, iGlyph+1).x;
    mat4 transform = u_Mvp * u_Pose;

    for (int i = start; i < end; i++) {
        float overlap = 0;
        int j = i*6+257+256*4;

        vec2 a = (transform*vec4(u_EmScale*fetch(j), 0, 0)).xy*u_Viewport.zw/2/u_EmScale;
        vec2 b = (transform*vec4(u_EmScale*fetch(j+1), 0, 0)).xy*u_Viewport.zw/2/u_EmScale;
        vec2 c = ((transform*vec4(u_EmScale*(fetch(j+2)+vec2(vAdvance, 0)), 0, 1)).xy+vec2(1,1))*u_Viewport.zw/2/u_EmScale;

        c = c-o;//move origin to pixel

        //Polar quartic r2 = At4 + Bt3 + Ct2 + Dt + E
        float A = a.x*a.x+a.y*a.y;
        float B = 2*a.x*b.x+2*a.y*b.y;
        float C = 2*a.x*c.x+b.x*b.x+2*a.y*c.y+b.y*b.y;
        float D = 2*(b.x*c.x + b.x*a.y + b.y*c.y + c.y*a.y);
        float E = c.x*c.x+c.y*c.y;
        float r = window/u_EmScale;

        vec4 roots;
        int count = solveQuartic(A, B, C, D, E-r*r, roots);//compute roots

        float t0 = roots.x, t1 = roots.y;

        float wa, wb, wc, wd, we, wf, wg;//window integral coefficients
        windowIntegralCoeff(a.x, b.x, c.x, a.y, b.y, c.y, wa, wb, wc, wd, we, wf, wg);




    }
    return total;
}

void main () {
    vec2 gPos = (vScreenPos+vec2(1))*u_Viewport.zw/2/u_EmScale;

    float area = calcArea(gPos);
    area = abs(area);
    area = area/(PI/4)*u_EmScale*u_EmScale;//normalize
    area = clamp(area, 0.0, 1.0);
    //area = pow(area, 1/2.2);
    color = vec4(u_Tint.rgb, area * u_Tint.a);
}
