#version 330 core
layout (location = 0) out vec4 color;

#define epsilon 0.00001
#define PI 3.14159265359
#define R 1.0
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

int solveQuadratic(float a, float b, float c, out vec2 roots){
    roots = vec2(1);
    if(abs(a)<epsilon){
        if(abs(b)<epsilon){
            return 0;
        }
        roots.x = -c/b;
        return 1;
    }

    float det = b*b-4*a*c;
    if(det<0){
        return 0;
    }
    float sqr = sqrt(det);
    roots = (vec2(-b)+vec2(sqr,-sqr))/(2*a);
    return 2;
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

int findIntercepts(float, out vec4 roots) {
    //(ax2+bx+c-ox)^2+(dx2+ex+f-oy)^2-r2=0

    return solveQuartic(A, B, C, D, E, roots);
}

//optimize and stabilize later, if needed
//integrates the window function 1-r^2 over the polar region from tb to ta
//negative at the end bc clockwise is positive
float integrateWindow(float i7, float i6, float i5, float i4, float i3, float i2, float i1, float ta, float tb) {
    float t2a = ta * ta;
    float t3a = t2a * ta;
    float t4a = t3a * ta;
    float t5a = t4a * ta;
    float t6a = t5a * ta;
    float t7a = t6a * ta;

    float t2b = tb * tb;
    float t3b = t2b * tb;
    float t4b = t3b * tb;
    float t5b = t4b * tb;
    float t6b = t5b * tb;
    float t7b = t6b * tb;

    float val = -(i7 * (t7a - t7b) + i6 * (t6a - t6b) + i5 * (t5a - t5b) + i4 * (t4a - t4b) + i3 * (t3a - t3b) + i2 * (t2a - t2b) + i1 * (ta - tb));

    return val;
}

void windowIntegralCoeff(float a, float b, float c, float d, float e, float f, out float I1, out float I2, out float I3, out float I4, out float I5, out float I6, out float I7) {
    float A = b * d - a * e;
    float B = 2*(c*d-a*f);
    float C = e*c-b*f;
    float D = a*a+d*d;
    float E = 2*(a*b+d*e);
    float F = (2*a*c+b*b+2*d*f+e*e);
    float G = 2*(b*c+e*f);
    float H = c*c+f*f;

    float scl = 3.0/(8*R*R);
    I1 = scl*(R*R*C-(0.5*H*C));
    I2 = scl*(R*R*B-(0.25*(H*B-G*C)));
    I3 = scl*(R*R*A-1/6.0*(F*C+G*B+H*A));
    I4 = -scl/8.0*(E*C+F*B+G*A);
    I5 = -scl/10.0*(D*C+E*B+F*A);
    I6 = -scl/12.0*(D*B+E*A);
    I7 = -scl/14.0*(D*A);
}

//counterclockwise is negative, clockwise is positive
float integrateArea(float A, float B, float C, float D, float E, float F, float ta, float tb) {
    float avg = (A + B + C + D + E + F) / 6;
    A /= avg;
    B /= avg;
    C /= avg;
    D /= avg;
    E /= avg;
    F /= avg;

    float t2a = ta * ta;
    float t3a = t2a * ta;
    float t2b = tb * tb;
    float t3b = t2b * tb;
    return -(1 / 3 * (B * D - A * E) * (t3a - t3b) + (D * C - A * F) * (t2a - t2b) + (C * E - B * F) * (ta - tb)) / 2 * avg;
}

//negative area means ccw means negative angle


//d^2theta/dt
float d2(float a, float b, float c, float d, float e, float f, float t){
    float d2y = 2*d;
    float d2x = 2*a;
    float x = a*t*t+b*t+c;
    float y = d*t*t+e*t+f;
    float r2 = x*x+y*y;
    float dx = 2*a*t+b;
    float dy = 2*d*t+e;
    return ((d2y*x-d2x*y)*r2-2*(x*dx+y*dy)*(dy*x-dx*y))/r2;
}

//adds or subtract full rotation to reflect whether the change in angle is negative or positive

float correctDeltaTheta(float dTheta, float sgn){
    if(sgn<0&&dTheta>0){
        return dTheta-2*PI;
    }
    if(sgn>0&&dTheta<0){
        return dTheta+2*PI;
    }
    return dTheta;
}

float integrateAngle(float a, float b, float c, float d, float e, float f, float t0, float t1) {
    float A = b*d-a*e;//dtheta/dt = At2+Bt+C
    float B = 2*(c*d-a*f);
    float C = e*c-b*f;

    float total = 0;

    vec2 roots;
    int num = solveQuadratic(A, B, C, roots);

    vec2 vA = vec2(a, d), vB = vec2(b, e), vC = vec2(c, f);//vector A, B, C for cartesian quadratic bezier
    vec2 p0 = vA *t0*t0+ vB *t0+ vC;//position 0
    float theta0 = atan(p0.y, p0.x);

    float dTheta0 = 2*A*t0+B;//tells us if theta is increasing or decreasing at t0

    if(num>0){
        float ts1 = roots.x;//1st stationary t
        vec2 ps1 = vA* ts1 * ts1 +vB* ts1 +vC;//1st stationary point
        float thetaS1 = atan(ps1.y, ps1.x);//stationary point angle

        float deltaTheta1 = thetaS1 -theta0;//relative theta from P0 to Ps1 unormalized

        deltaTheta1 = correctDeltaTheta(deltaTheta1, dTheta0);
        total += deltaTheta1;

        if(num>1){
            float ts2 = roots.y;//2nd stationary t
            vec2 ps2 = vA*ts2*ts2+vB*ts2+vc;//2nd stationary point
            float thetaS2 = atan(ps2.y, ps2.x);//angle of 2nd stationary point

            float deltaTheta2 = thetaS1-thetaS1;//change in angle between 1st and 2nd stationary points
            float d2Theta1 = d2(a, b, c, d, e, f, ts1);//second derivative of theta with respect to t at first stationary point
            deltaTheta2 = correctDeltaTheta(deltaTheta2, d2Theta1);//corrected based on what direction theta is coming from
            total += deltaTheta2;

            theta0 = thetaS2;
            p0 = ps2;
            t0 = ts2;
            dTheta0 = d2(a, b, c, d, e, f, ts2);
        } else {
            theta0 = thetaS1;
            p0 = ps1;
            t0 = ts1;
            dTheta0 = d2(a, b, c, d, e, f, ts1);
        }

        //float d2ThetaS = d2(a, b, c, d, e, f, t0);

        //now we compute the second derivative
    }
    vec2 p1 = vA*t1*t1+vB*t1+vC;
    float theta1 = atan(p1.y,p1.x);
    float deltaTheta1 = theta1-theta0;
    deltaTheta1 = correctDeltaTheta(deltaTheta1, dTheta0);
    total += deltaTheta1;
    return total;
}

vec2 fetch(int j){
  return vec2(texelFetch(u_Atlas, j).x, texelFetch(u_Atlas, j+3).x);
}

float calcArea(vec2 o){
  float total = 0;

  int iGlyph = int(vGlyph);

  //iterate through beziers
  int start = texelFetch(u_Atlas, iGlyph).x, end = texelFetch(u_Atlas, iGlyph+1).x;
  mat4 transform = u_Mvp * u_Pose;

  for (int i = start; i < end; i++) {
      float overlap = 0;
      int j = i*6 + 257 + 256*iGlyph;

      vec2 a = (transform*vec4(fetch(j), 0, 0)).xy*u_Viewport.zw/2;
      vec2 b = (transform*vec4(fetch(j+1), 0, 0)).xy*u_Viewport.zw/2;
      vec2 c = ((transform*vec4((fetch(j+2)+vec2(vAdvance, 0)), 0, 1)).xy+vec2(1,1))*u_Viewport.zw/2;

      c = c-o;//move origin to pixel

      //Polar quartic r2 = At4 + Bt3 + Ct2 + Dt + E
      float A = a.x*a.x+a.y*a.y;
      float B = 2*a.x*b.x+2*a.y*b.y;
      float C = 2*a.x*c.x+b.x*b.x+2*a.y*c.y+b.y*b.y;
      float D = 2*(b.x*c.x + b.x*a.y + b.y*c.y + c.y*a.y);
      float E = c.x*c.x+c.y*c.y;
      float r = R;

      vec4 roots;
      int count = solveQuartic(A, B, C, D, E-r*r, roots);//compute roots

      float t0 = roots.x, t1 = roots.y;

      float i1, i2, i3, i4, i5, i6, i7;//window integral coefficients
      windowIntegralCoeff(a.x, b.x, c.x, a.y, b.y, c.y, i1, i2, i3, i4, i5, i6, i7);

      overlap += integrateAngle();

      if(count>0){

      }




  }
  return total;
}

void main () {
  vec2 gPos = (vScreenPos+vec2(1))*u_Viewport.zw/2;

  float area = calcArea(gPos);
  area = abs(area);
  area = area/(PI/4);//normalize
  area = clamp(area, 0.0, 1.0);
  //area = pow(area, 1/2.2);
  color = vec4(u_Tint.rgb, area * u_Tint.a);
}
/*a = 15 * (D * D + A * A) * (A * E - B * D)/210;
b = 35 * (A * D * D * F + A * A * A * F + A * D * E * E - B * D * D * E + A * A * B * E - C * D * D * D - A * A * C * D - A * B * B * D)/210;
c = 21*(6 * A * D * E * F - B * D * D * F + 5 * A * A * B * F + A * E * E * E - B * D * E * E - 5 * C * D * D * E + A * A * C * E + A * B * B * E - 6 * A * B * C * D - B * B * B * D)/210;
d = 105 * (A * F - C * D) * (D * F + E * E + A * C + B * B)/210;
e = 35 * (5 * A * E * F * F + B * D * F * F + B * E * E * F - 6 * C * D * E * F + 6 * A * B * C * F + B * B * B * F - C * E * E * E - A * C * C * E - B * B * C * E - 2 * A * E - B * (5 * C * C - 2) * D)/210;
f = 105 * (F * (A * F * F + B * E * F - C * D * F - C * E * E + A * C * C + B * B * C - 2 * A) - C * (B * C * E + C * C * D - 2 * D))/210;
g = 105 * (B * F * F * F - 105 * C * E * F * F + 105 * B * (C * C - 2) * F - 105 * C * (C * C - 2) * E)/210;*/

/*
    if(dt<0 && area>0){
        return 2*PI+dt;
    }
    if(dt>0 && area<0){
        return dt-2*PI;
    }*/