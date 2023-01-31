package test.alg_test;

import org.joml.Random;

import java.util.Arrays;

import static java.lang.Math.*;
import static org.joml.Math.sqrt;

public class Quartic {

    public static void main(String[] args) {
        Random random = new Random(10000);
        int bad = 0;
        for (int i = 0; i < 1000000; i++) {
            float a = nextFloat(random),
                    b = nextFloat(random),
                    c = nextFloat(random),
                    d = nextFloat(random),
                    e = nextFloat(random);
            float[] roots = new float[4];
            int num = solveQuartic(a, b, c, d, e, roots);
            for (int j = 0; j < num; j++) {
                float t = roots[j];
                float val = evalQuartic(a, b, c, d, e, t);
                if(t>0&&t<1&&abs(val)>100){
                    System.out.printf("%fx^4+%fx^3+%fx^2+%fx+%f%n", a, b, c, d, e);
                    System.out.printf("(%f,%f)%n", roots[j], val);
                    bad++;
                }
            }
        }
        System.out.println(bad);

        //-1.000000x^4+-727.000000x^3+-684.000000x^2+-922.000000x+-680.000000
        /*float a = -1, b = -727, c = -684, d = -922, e = -680;
        float[] roots = new float[4];
        int num = solveQuartic(a, b, c, d, e, roots);
        for (int i = 0; i < num; i++) {
            float t = roots[i];
            float val = evalQuartic(a, b, c, d, e, t);
            System.out.printf("(%f,%f)%n", t, val);
        }*/
    }

    static float nextFloat(Random random){
        return random.nextInt(2000)-1000;
    }

    static float evalQuartic(float a, float b, float c, float d, float e, float t){
        return a*t*t*t*t+b*t*t*t+c*t*t+d*t+e;
    }
    static float cbrt(float x) { return sign(x) * pow(abs(x), 1f / 3f); }

    private static float sign(float x) {
        return x<0?-1:1;
    }

    static float pow(float a, float b){
        return (float)Math.pow(a, b);
    }

    static int solveQuartic(float a, float b, float c, float d, float e, float[] roots) {
        //find n such that a = 1000
        //n4a=10
        //n=(10/a)^(1/4)
        float k = pow(abs(1000/a), .25f);
        //float k=1;
        a = a*k*k*k*k;
        b = (b*k*k*k)/a;
        c = (c*k*k)/a;
        d = (d*k)/a;
        e = e/a;

        // Depress the quartic to x^4 + px^2 + qx + r by substituting x-b/4a
        // This can be found by substituting x+u and the solving for the value
        // of u that makes the t^3 term go away
        float bb = b * b;
        float p = (8f * c - 3f * bb) / 8f;
        float q = (8f * d - 4f * c * b + bb * b) / 8f;
        float r = (256f * e - 64f * d * b + 16f * c * bb - 3f * bb * bb) / 256f;
        int n = 0; // Root counter

        // Solve for a root to (t^2)^3 + 2p(t^2)^2 + (p^2 - 4r)(t^2) - q^2 which resolves the
        // system of equations relating the product of two quadratics to the depressed quartic
        float ra = 2f * p;
        float rb = p * p - 4f * r;
        float rc = -q * q;

        // Depress using the method above
        float ru = ra / 3f;
        float rp = rb - ra * ru;
        float rq = rc - (rb - 2f * ra * ra / 9f) * ru;

        float lambda;
        float rh = 0.25f * rq * rq + rp * rp * rp / 27f;
        if (rh > 0.0) { // Use Cardano's formula in the case of one real root
            rh = sqrt(rh);
            float ro = -0.5f * rq;
            lambda = cbrt(ro - rh) + cbrt(ro + rh) - ru;
        } else { // Use complex arithmetic in the case of three real roots
            float rm = sqrt(-rp / 3f);
            lambda = -2.0f * rm * (float) sin(asin(1.5f * rq / (rp * rm)) / 3.0f) - ru;
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
        float alpha = 2.0f * q / t, beta = lambda + ra;

        float u = 0.25f * b;
        t *= 0.5;

        float z = -alpha - beta;
        if (z > 0.0) {
            z = sqrt(z) * 0.5f;
            float h = +t - u;
            roots[0] = (h - z)*k;
            roots[1] = (h + z)*k;
            n += 2;
        }

        float w = +alpha - beta;
        if (w > 0.0) {
            w = sqrt(w) * 0.5f;
            float h = -t - u;
            roots[2] = (h - w)*k;
            roots[3] = (h + w)*k;
            if (n == 0) {
                roots[0] = roots[2];
                roots[1] = roots[3];
            }
            n += 2;
        }

        return n;
    }
}
