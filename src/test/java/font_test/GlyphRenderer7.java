package font_test;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static font_test.FileUtil.loadFont;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;

public class GlyphRenderer7 {
    Main main;
    ArrayList<float[]>[] glyphs;

    STBTTFontinfo fontinfo;
    public int glyph=68;

    public GlyphRenderer7(Main main) {
        this.main = main;
        glyphs = new ArrayList[300];

        try {
            fontinfo = loadFont("/font/arial.ttf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //4-200
        initGlyphs();
    }

    private void initGlyphs(){
        for (int j = 0; j < 300; j++) {
            STBTTVertex.Buffer vertices = stbtt_GetGlyphShape(fontinfo, j);
            if(vertices!=null){
                ArrayList<float[]> quadratic = new ArrayList<>();
                int count = vertices.remaining();
                for (int i = count-1; i >=0; i--) {
                    STBTTVertex vertex = vertices.get(i);
                    int ax = vertex.x(), ay = vertex.y();
                    byte type = vertex.type();

                    if(type==3){
                        int bx = vertex.cx(), by = vertex.cy();
                        STBTTVertex nextVertex = vertices.get(i-1);
                        int cx = nextVertex.x(), cy = nextVertex.y();
                        //System.out.printf("(%dt^2+%dt+%d, %dt^2+%dt+%d)%n", ax-2*bx+cx, 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy);
                        quadratic.add(new float[]{climp(ax-2*bx+cx, 0.01f), 2*bx-2*cx, cx, climp(ay-2*by+cy, 0.01f), 2*by-2*cy, cy});
                    } else if(type==2){
                        STBTTVertex nextVertex = vertices.get(i-1);
                        int bx = nextVertex.x(), by = nextVertex.y();
                        //System.out.printf("(%dt+%d,%dt+%d)%n", ax-bx, bx, ay-by, by);
                        quadratic.add(new float[]{climp(0, 0.01f), ax-bx, bx, climp(0, 0.01f), ay-by, by});
                    }
                }
                this.glyphs[j] = quadratic;
            }
        }
    }
    int ticks;
    public void tick() {
        ticks++;
    }

    private float clamp(float a, float min, float max){
        return a<min?min:Math.min(a, max);
    }

    private float climp(float a, float min){
        //return Math.abs(a)<min?a<0?-min:min:a;
        return a;
    }

    float zoom = 5;

    int scale = 1;//number of screen pixels per buffer pixel


    public void render(Graphics2D g) {
        int width = main.getWidth(), height = main.getHeight();
        width -= width%scale;
        height -= height%scale;

        BufferedImage image = new BufferedImage(width/scale, height/scale, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                float qx = (float) ((x-.1f*width/scale+Math.cos(ticks)*25)*zoom);
                float qy = (float) ((y-.1f*height/scale+Math.sin(ticks)*25)*zoom);

                //float qx = (x-0.1f*width)*zoom;
                //float qy = (y-0.1f*height)*zoom;

                float shade = clamp(area(qx, qy, glyph), 0, 0.99999f);

                shade = (float) Math.pow(shade, 1/2.2);

                image.setRGB(x, height/scale-y-1, new Color((int)(shade*256), (int)(shade*256), (int)(shade*256)).getRGB());
            }
        }
        g.drawImage(image, 0, 0, width, height, null);
    }

    float epsilon = 0.0001f;


    //area of intersection between the glyph and the x,y pixel
    private float area(float x, float y, int glyph) {
        ArrayList<float[]> beziers = this.glyphs[glyph];

        float xMin = x, xMax = x + zoom;
        float yMin = y, yMax = y + zoom;

        float overlap = 0;

        Intercept[] roots = new Intercept[8];
        for (float[] bezier : beziers) {
            float a = bezier[0],
                    b = bezier[1],
                    c = bezier[2],
                    d = bezier[3],
                    e = bezier[4],
                    f = bezier[5];

            findRoots(a, b, c-xMin, roots, 0);//left
            findRoots(a, b, c-xMax, roots, 2);//right
            findRoots(d, e, f-yMin, roots, 4);//bottom
            findRoots(d, e, f-yMax, roots, 6);//top


            Arrays.sort(roots, (o1, o2) -> Float.compare(o1.t, o2.t));//problem 1

            int squareDepth = (a==0&&b==0&&c>xMin&&c<xMax&&d==0 || d==0&&e==0&&f>yMin&&f<yMax&&a==0)?1:0,
                    aboveDepth = (d>0||d==0&&e<0||d==0&&e==0&&f>=yMax)?1:0;

            for (int i = 0; i < roots.length; i++) {
                int type = roots[i].type;//problem 2
                squareDepth += 1-(type&1)*2;
                aboveDepth += ((type>>1)==2)?0:(((type>>1)==3)?((type&1)*2-1):(1-(type&1)*2));
                if(squareDepth==2){
                    overlap += integrate(a, b, d, e, f, yMin, clamp(roots[i].t, 0, 1), clamp(roots[i+1].t, 0, 1));
                }
                if(aboveDepth==2){
                    float t0 = clamp(roots[i].t, 0, 1), t1 = clamp(roots[i+1].t, 0, 1);
                    float dx = a*t1*t1+b*t1-(a*t0*t0+b*t0);
                    overlap += dx*zoom;
                }
            }
        }

        overlap = overlap/zoom/zoom;

        return 1-overlap;
    }

    /*integral (Y(t) * dx/dt)dt from t to t1
                Y(t)=dt^2+et+f
                f = bezier[5]-yMin

                dx/dt = 2at+b

                y*dx/dt = (dt^2+et+f)(2at+b)
                =(2ad)t^3 + (2ea+bd)t^2 + (2af+eb)t + bf

                integral = 1/4(2ad)t^4 + 1/3(2ea+bd)t^3 + 1/2(2af+eb)t^2 + bft + C
    */
    private double integrate(double a, double b, double d, double e, double f, double y, double t0, double t1){
        f -= y;
        double upper = (t1*(b*(6*f+t1*(3*e+2*d*t1))+a*t1*(6*f+t1*(4*e+3*d*t1))))/6;
        double lower = (t0*(b*(6*f+t0*(3*e+2*d*t0))+a*t0*(6*f+t0*(4*e+3*d*t0))))/6;

        return upper-lower;
    }

    float[] norms = new float[]{-1, 1, -1, 1};
    //enter, exit
    private void findRoots(float a, float b, float c, Intercept[] t, int i){
        t[i] = new Intercept(0, i);
        t[i+1] = new Intercept(0, i+1);
        if(Math.abs(a)<epsilon&&Math.abs(b)>epsilon){
            float t0 = -c/b;
            int type = 0;
            if((2*a*t0+b)*norms[i/2]>0){
                type = 1;
            }
            t[i] = new Intercept(t0, type+i);
            t[i+1] = new Intercept(1, 1-type+i);
        } else {
            float dis = b * b - 4 * a * c;
            if (dis > 0) {
                float sqrt = (float) Math.sqrt(dis);
                float t0 = (-b - sqrt) / (2f * a);
                float t1 = (-b + sqrt) / (2f * a);
                float d = 2*a*t0+b;
                if(d*norms[i/2]>0){
                    float temp = t0;
                    t0 = t1;
                    t1 = temp;
                }
                t[i] = new Intercept(t0, i);
                t[i+1] = new Intercept(t1, i+1);
            }
        }
    }

    class Intercept {
        float t;
        int type;//side * 2 + (enter/exit)
        Intercept(float t, int type){
            this.t = t;
            this.type = type;
        }
    }
}
