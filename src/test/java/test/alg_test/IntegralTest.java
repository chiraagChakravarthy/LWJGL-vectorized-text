package test.alg_test;

import static java.lang.Math.*;

public class IntegralTest {
    static float windowIntegrate(float[] coeff, float ta, float tb, float r){
        float t7 = tb*tb*tb*tb*tb*tb*tb-ta*ta*ta*ta*ta*ta*ta,
                t6 = tb*tb*tb*tb*tb*tb-ta*ta*ta*ta*ta*ta,
                t5 = tb*tb*tb*tb*tb-ta*ta*ta*ta*ta,
                t4 = tb*tb*tb*tb-ta*ta*ta*ta,
                t3 = tb*tb*tb-ta*ta*ta,
                t2 = tb*tb-ta*ta,
                t1 = tb-ta;

        return (coeff[0]*t7+coeff[1]*t6+coeff[2]*t5+coeff[3]*t4+coeff[4]*t3+coeff[5]*t2+coeff[6]*t1)/420f/r/r;
    }

    static void windowCoeff(float a, float b, float c, float d, float e, float f, float r, float[] i){
        float a2 = a * a;
        float a3 = a2 * a;
        float d2 = d * d;
        float d3 = d2 * d;
        i[0] = (15*d*a2+15*d3)*b-15*e*a3-15*e*d2*a;

        float b2 = b * b;
        float e2 = e * e;
        i[1] = (-35*a3-35*d2*a)*f+(35*d*a2+35*d3)*c+35*d*a*b2+(35*e*d2-35*e*a2)*b-35*e2*d*a;

        float b3 = b2 * b;
        float e3 = e2 * e;
        i[2] = ((21*d2-105*a2)*b-126*e*d*a)*f+(126*d*a*b-21*e*a2+105*e*d2)*c+21*d*b3-21*e*a*b2+21*e2*d*b-21*e3*a;

        float f2 = f * f;
        float c2 = c * c;
        i[3] = -105*d*a*f2+((105*d2-105*a2)*c-105*a*b2-105*e2*a)*f+105*d*a*c2+(105*d*b2+105*e2*d)*c;

        float R2 = r*r;
        float f3 = f2 * f;
        float c3 = c2 * c;
        i[4] = (-35*d*b-175*e*a)*f2+((210*e*d-210*a*b)*c-35*b3-35*e2*b)*f+(175*d*b+35*e*a)*c2+(35*e*b2+35*e3)*c-70*d*R2*b+70*e*R2*a;
        i[5] = -105*a*f3+(105*d*c-105*e*b)*f2+(-105*a*c2+(105*e2-105*b2)*c+210*R2*a)*f+105*d*c3+105*e*b*c2-210*d*R2*c;
        i[6] = -105*b*f3+105*e*c*f2+(210*R2*b-105*b*c2)*f+105*e*c3-210*e*R2*c;
    }

    //(0.5,2yt-y), y2+(0.5)2=1
    //integrate in t:(0,1)
    public static void main(String[] args) {
        float[] coeff = new float[7];
        windowCoeff(0, 0, 0.5f, 0, (float) (2*sqrt(1-.5*.5)), (float) -sqrt(1-.5*.5), 1, coeff);

        float i1 = windowIntegrate(coeff, 0, 1, 1);

        float i2 = 0;
        float dt = 1/10000f;

        for (int i = 0; i < 10000; i++) {

            float t = i*dt;
            float y = (float) (t*2*sqrt(1-.5*.5)-sqrt(1-.5*.5)),
                    x = 0.5f;
            float R = (float) sqrt(x*x+y*y);

            float dr = 1/100f;
            float rComp = 0;
            for (int j = 0; j < R*100; j++) {
                float r = dr*j;
                rComp += (1-r*r)*r*dr;
            }

            float theta0 = (float) atan(y/x);
            float y1 = (float) (y+dt*2*sqrt(1-.5*.5));
            float theta1 = (float) atan(y1/x);
            float dtheta = -(theta1-theta0);
            if(dtheta>PI){
                dtheta = (float) (2*PI-dtheta);
            }
            i2 += rComp*dtheta;
        }
        System.out.println(i1 + "," + i2);
    }
}
