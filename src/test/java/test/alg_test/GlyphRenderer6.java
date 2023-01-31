package test.alg_test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static java.lang.Math.*;

public class GlyphRenderer6 {
    Main main;
    public char codepoint='e';
    private VectorFont2 font;

    public GlyphRenderer6(Main main) {
        this.main = main;
        font = new VectorFont2();
    }


    int ticks;
    public void tick() {
        ticks++;
        //r += .5f;
    }

    private static float clamp(float t, float min, float max){
        return t<min?min:Math.min(t, max);
    }

    public void render(Graphics2D g) {
        int width = main.getWidth(), height = main.getHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float shade = (float) (sample(x-width/2, y-height/2)/PI*4);
                shade = 1-clamp(shade, 0, 1);
                image.setRGB(x, height-y-1, new Color((int)(shade*255), (int)(shade*255), (int)(shade*255)).getRGB());
            }
        }
        g.drawImage(image, 0, 0, width, height, null);
    }

    private float scale=300;//pixels per em
    float r = 0.7f;//pixel


    //circular aperture centered at (x,y)
    public float sample(int x, int y){

        float shade = 0;

        float emScale = font.emScale;//em per glyph unit

        float scale = this.scale*emScale;//glyph -> pixel


        int[] atlas = font.atlas;
        int len = font.len;
        int start = atlas[font.indexOf(codepoint)];
        int end = atlas[font.indexOf(codepoint)+1];
        for (int i = start; i < end; i++) {
            if(x==-249&&y==-15&&i==1338){
                int k = 0;
            }
            int j = i*6+len*4+len+1;
            float a = atlas[j]*scale, b = atlas[j+1]*scale, c=atlas[j+2]*scale,
                    d = atlas[j+3]*scale, e = atlas[j+4]*scale, f = atlas[j+5]*scale;
            /*
            x=at2+bt+c
            y=dt2+et+f
             */

            //find intersection with circle
            /*
            (Px-x)^2 + (Py-y)^2 = r^2
            (at2+bt+c-x)^2+(dt2+et+f-y)^2-r2=0
            a2t4+abt3+a(c-x)t2+abt3+b2t2+b(c-x)t+a(c-x)t2+b(c-x)t+(c-x)2 + d2t4+det3+d(f-y)t2+det3+e2t2+e(f-y)t+d(f-y)t2+e(f-y)t+(f-y)2-r2=0
            t4 (a2+d2)
            t3 (2ab+2de)
            t2 (2a(c-x)+b2+2d(f-y)+e2)
            t  (2b(c-x)+2e(f-y))
            1  ((c-x)2+(f-y)2-r2)
             */

            c -= x;
            f -= y;

            float k4 = a*a+d*d,
                    k3 = 2*(a*b+d*e),
                    k2 = 2*a*c+b*b+2*d*f+e*e,
                    k1 = 2*(b*c+e*f),
                    k0 = c*c+f*f-r*r;

            float[] roots = new float[4];
            int num = solveQuartic(k4, k3, k2, k1, k0, roots);
            if(num>0){
                int k = 0;
            }

            //now that we have the roots, split the curve into 5 regions
            /*t:
            (-infty, t1): outside
            (t1,t2): inside
            (t2,t3): outside
            (t3,t4): inside
            (t4,infty): outside

            astrics:
            * shit can be outside (0,1)
                * when something falls outside (0,1), it does not contribute
            * a clockwise dtheta contributes positively, and a ccw dtheta contributes negatively
            * we can use
             */

            /*
            need to integrate angle, and segments inside circle
             */
            float[] windowCoeff = new float[7];
            windowCoeff(a, b, c, d, e, f, r, windowCoeff);

            //first increment: angle integrate [0,t1]
            float ta = 0;
            float tb = roots[0];

            float delta = 0;

            //delta -= angleIntegrate(a, b, c, d, e, f, clamp(ta, 0, 1), clamp(tb, 0, 1))/4;

            //second increment: window integrate [t1,t2]
            ta = tb;
            tb = roots[1];
            delta -= windowIntegrate(windowCoeff, clamp(ta, 0, 1), clamp(tb, 0, 1), r);

            //third increment: angle integrate [t2,t3]
            ta = tb;
            tb = roots[2];
            //delta -= angleIntegrate(a, b, c, d, e, f, clamp(min(roots[1], roots[3]), 0, 1), clamp(max(roots[0], roots[2]), 0, 1))/4;

            //fourth increment: window integrate [t3,t4]
            ta = tb;
            tb = roots[3];
            delta -= windowIntegrate(windowCoeff, clamp(ta, 0, 1), clamp(tb, 0, 1), r);

            //fifth increment: angle integrate [t4,t1]
            ta = tb;
            tb = 1;
            //delta -= angleIntegrate(a, b, c, d, e, f, clamp(ta, 0, 1), clamp(tb, 0, 1))/4;
            shade += delta;
        }
        return shade;
    }

    //angle integrate with respect to origin
    static float angleIntegrate(float a, float b, float c, float d, float e, float f, float t0, float t1){
        float x0 = a*t0*t0+b*t0+c,
                y0 = d*t0*t0+e*t0+f,
                x1 = a*t1*t1+b*t1+c,
                y1 = d*t1*t1+e*t1+f,
                theta0 = (float)atan2(y0, x0),
                theta1 = (float)atan2(y1, x1),
                diff = theta1-theta0;
        diff = (float) (diff<-PI?diff+2*PI:diff>PI?diff-2*PI:diff);//short way around
        if(abs(diff)<.01f){
            return diff;//removes degenerate cases
        }

        float theta2 = theta0+diff/2,
                sx = (float) cos(theta2),
                sy = (float) sin(theta2);
        float[] roots = new float[2];
        int num = interceptLineBezier(a, b, c, d, e, f, sx, sy, roots);
        float t2 = roots[0]<t1&&roots[0]>t0?roots[0]:roots[1];//one of the two is in range (t0,t1)

        float x2 = a*t2*t2+b*t2+c,
                y2 = d*t2*t2+e*t2+f,
                dot = x2*sx+y2*sy;
        if(dot>0){
            return diff;
        }
        //it took the long way round
        return (float) (diff<0?diff+2*PI:diff>0?diff-2*PI:diff);
        //if somehow a 0 diff manages to make it through here then there you go
    }

    static int interceptLineBezier(float a, float b, float c, float d, float e, float f, float sx, float sy, float[] roots){
        float A,B,C;
        if (Math.abs(sx) < epsilon) {
            A = a;
            B = b;
            C = c;
        } else if (Math.abs(sy) < epsilon) {
            A = d;
            B = e;
            C = f;
        } else {
            A = a / sx - d / sy;
            B = b / sx - e / sy;
            C = c / sx - f / sy;
        }
        return solveQuadratic(A, B, C, roots);
    }

    //window integrate with respect to origin

    /*
    integral of -1/2(1-1/2((at^2+bt+c)^2+(dt^2+et+f)^2))((2dt+e)(at^2+bt+c)-(2at+b)(dt^2+et+f))dt
     */

    static float windowIntegrate(float[] coeff, float ta, float tb, float r){
        float t7 = tb*tb*tb*tb*tb*tb*tb-ta*ta*ta*ta*ta*ta*ta,
                t6 = tb*tb*tb*tb*tb*tb-ta*ta*ta*ta*ta*ta,
                t5 = tb*tb*tb*tb*tb-ta*ta*ta*ta*ta,
                t4 = tb*tb*tb*tb-ta*ta*ta*ta,
                t3 = tb*tb*tb-ta*ta*ta,
                t2 = tb*tb-ta*ta,
                t1 = tb-ta;

        return (coeff[0]*t7+coeff[1]*t6+coeff[2]*t5+coeff[3]*t4+coeff[4]*t3+coeff[5]*t2+coeff[6]*t1)/420f/r/r/r/r;
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



    static float cbrt(float x) { return sign(x) * pow(abs(x), 1f / 3f); }

    private static float sign(float x) {
        return x<0?-1:1;
    }

    static float pow(float a, float b){
        return (float) java.lang.Math.pow(a, b);
    }

    static final float epsilon = .00001f;
    public static int solveQuadratic(float a, float b, float c, float[] roots){
        Arrays.fill(roots, 1);
        if(abs(a)<epsilon){
            if(abs(b)<epsilon){
                return 0;
            }
            roots[0] = -c/b;
            return 1;
        }
        float det = b*b-4*a*c;
        if(det<0){
            return 0;
        }
        float sqr = (float) sqrt(det);
        float t1 = (-b-sqr)/2/a;
        float t2 = (-b+sqr)/2/a;
        roots[0] = min(t1, t2);
        roots[1] = max(t1, t2);
        return 2;
    }


    public static int solveQuartic(float a, float b, float c, float d, float e, float[] roots) {
        if(abs(a)<epsilon){//never cubic
            return solveQuadratic(c, d, e, roots);
        }
        Arrays.fill(roots, 1);

        float[] stationary = new float[3];
        int num = solveCubic2(4*a, 3*b, 2*c, d, stationary);
        Arrays.sort(stationary);

        int num2 = 0;

        float v0 = evalQuartic(a, b, c, d, e, stationary[0]),
                v1 = evalQuartic(a, b, c, d, e, stationary[1]),
                v2 = evalQuartic(a, b, c, d, e, stationary[2]);

        /*
        there is a zero on the interval [-inf, v0] if v0 < 0
         */
        if(v0 < 0){
            roots[num2++] = seek(a, b, c, d, e, stationary[0], 0);
        }

        /*
        there is a zero on the interval [v0, v1] if v0 < 0 and v1 > 0 only
         */

        if(v0 < 0 && v1 > 0){
            roots[num2++] = seek(a, b, c, d, e, stationary[0], stationary[1]);
        }

        /*
        there is a zero on the interval [v1, v2] if v1 > 0 and v2 < 0 only
         */

        if(v1 > 0 && v2 < 0){
            roots[num2++] = seek(a, b, c, d, e, stationary[2], stationary[1]);
        }

        /*
        there is a zero on the interval [v2, +inf] if v2 < 0 only
         */
        if(v2 < 0){
            roots[num2++] = seek(a, b, c, d, e, stationary[2], 1);
        }


        return num2;
    }

    static float sqrt(float x){
        return (float) Math.sqrt(x);
    }

    /*
    v(t0) < 0, v(t1) > 0
    thus if v(t) < 0, add interval. otherwise subtract interval
     */
    static float seek(float a, float b, float c, float d, float e, float t0, float t1){
        float interval = (t1-t0)/2,
                t2 = t0;
        interval /= 2;
        for (int i = 0; i < 20; i++) {
            float v2 = evalQuartic(a, b, c, d, e, t2);
            t2 += v2 < 0 ? interval : -interval;
            interval /= 2;
        }
        return t2;
    }


    static float evalQuartic(float a, float b, float c, float d, float e, float t){
        float t2 = t*t,
                t3 = t2*t,
                t4 = t3*t;
        return a*t4+b*t3+c*t2+d*t+e;
    }

    static int solveCubic(float d, float a, float b, float c, float[] roots){
        if(a<epsilon){
            return solveQuadratic(b, c, d, roots);
        }
        int num = 0;
        //1 or 3 solutions
        float denom = a;
        a = b/denom;
        b = c/denom;
        c = d/denom;

        float p = (3*b-a*a)/3f,
                q = (2*a*a*a-9*a*b+27*c)/27f,
                delta = q*q+4*p*p*p;
        if(abs(delta)<epsilon){
            roots[num++] = -a/3;
        } else if(delta>0){
            float sqrtDelta = (float) sqrt(delta),
                    u = (-q+sqrtDelta)/2f,
                    v = (-q-sqrtDelta)/2f;
            u = cbrt(u);
            v = cbrt(v);
            roots[num++] = u+v-a/3f;
        } else {
            float theta = (float) acos(-q/(2*sqrt(-p*p*p)));
            float sqrtp = (float) sqrt(-p);
            roots[num++] = (float) (2*sqrtp*cos(theta/3)-a/3);
            roots[num++] = (float) (2*sqrtp*cos((theta+2*PI)/3)-a/3);
            roots[num++] = (float) (2*sqrtp*cos((theta+4*PI)/3)-a/3);
        }
        return num;
    }

    static int solveCubic2(float d, float a, float b, float c, float[] roots){
        a = a/d;
        b = b/d;
        c = c/d;
        int num;

        float p = b/3-a*a/9;
        float q = a*a*a/27-a*b/6+c/2;
        float D = p*p*p+q*q;

        if(Float.compare(D, 0)>=0){
            if(Float.compare(D,0)==0){
                float r = cbrt(-q);
                roots[0] = 2*r;
                roots[1] = -r;
                num = 2;
            } else {
                num = 1;
                float r = cbrt(-q+sqrt(D)),
                        s = cbrt(-q-sqrt(D));
                roots[0] = r+s;
            }
        } else {
            float ang = (float) acos(-q/sqrt(-p*p*p)),
                    r = 2*sqrt(-p);
            num = 3;
            for (int i = -1; i <= 1; i++) {
                float theta = (float) ((ang-2*PI*i)/3);
                roots[i+1] = (float) (r*cos(theta));
            }
        }
        for (int i = 0; i < num; i++) {
            roots[i] -= a/3;
        }
        return num;
    }
}