package font_test;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static font_test.FileUtil.initFont;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;

public class GlyphRenderer6 {
    Main main;
    ArrayList<float[]>[] quadratic;

    STBTTFontinfo fontinfo;
    public int glyph=68;

    public GlyphRenderer6(Main main) {
        this.main = main;
        quadratic = new ArrayList[300];

        try {
            fontinfo = initFont("/font/arial.ttf");
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
                        quadratic.add(new float[]{ax-2*bx+cx, 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy});
                    } else if(type==2){
                        STBTTVertex nextVertex = vertices.get(i-1);
                        int bx = nextVertex.x(), by = nextVertex.y();
                        //System.out.printf("(%dt+%d,%dt+%d)%n", ax-bx, bx, ay-by, by);
                        quadratic.add(new float[]{0, ax-bx, bx, 0, ay-by, by});
                    }
                }
                this.quadratic[j] = quadratic;
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

    float zoom = 5;

    int scale = 1;//number of screen pixels per buffer pixel


    public void render(Graphics2D g) {
        int width = main.getWidth(), height = main.getHeight();
        width -= width%scale;
        height -= height%scale;

        BufferedImage image = new BufferedImage(width/scale, height/scale, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                //float qx = (float) ((x-.1f*width/scale+Math.cos(ticks*.01f)*25)*zoom);
                //float qy = (float) ((y-.1f*height/scale+Math.sin(ticks*.01f)*25)*zoom);

                float qx = (x-0.1f*width)*zoom;
                float qy = (y-0.1f*height)*zoom;

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
        ArrayList<float[]> beziers = this.quadratic[glyph];

        float xMin = x, xMax = x + zoom;
        float yMin = y, yMax = y + zoom;

        float overlap = 0;
        Integer[] indices = new Integer[10];

        float[] roots = new float[10];
        for (float[] bezier : beziers) {
            float a = bezier[0],
                    b = bezier[1],
                    c = bezier[2],
                    d = bezier[3],
                    e = bezier[4],
                    f = bezier[5];
            //compute intersections with horizontal bounds

            //X(t)=x
            //at^2+bt+c=x
            //at^2+bt+c-x=0

            findRoots(a, b, c-xMin, roots, 0);//left
            findRoots(a, b, c-xMax, roots, 2);//right
            findRoots(d, e, f-yMin, roots, 4);//bottom
            findRoots(d, e, f-yMax, roots, 6);//top


            roots[8] = 0;
            roots[9] = 1;

            /*
            first we want to verify intersects as
            * above y
            * within xmin and xmax

            if both conditions are met, we want to classify the intersect as one of the following
            * enter/exit bounds
            * enter/exit square

             */

            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }

            Arrays.sort(indices, 0, 10, (o1, o2) -> Float.compare(roots[o1], roots[o2]));
            float prevIx = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < roots.length-1; i++) {
                int side = indices[i]/2;
                /*
                sides:
                0: left
                1: right
                2: bottom
                3: top
                4: end
                 */

                float t = roots[indices[i]];
                if(t<0||t>=1){
                    continue;
                }

                float ix = a*t*t+b*t+c;
                if(Math.abs(ix-prevIx)*zoom<epsilon){
                    continue;
                }
                float iy = d*t*t+e*t+f;
                float dx = 2*a*t+b;
                float dy = 2*d*t+e;

                boolean clamp = false;
                boolean bounds = true;

                if(side==2||side==3) {
                    if (side == 2 && dy < 0) {
                        bounds = false;
                    }

                    if((ix<xMin+epsilon&&dx<0)||ix<xMin-epsilon||(ix>xMax-epsilon&&dx>0)||ix>xMax+epsilon){
                        bounds = false;
                    }

                    if (side == 3 && dy > 0) {
                        clamp = true;
                    }
                }
                if(side==0||side==1){
                    if(side==0&&dx<0){
                        bounds = false;
                    }

                    if(side==1&&dx>0){
                        bounds = false;
                    }

                    if((iy < yMin+epsilon&&dy<0)||iy<yMin-epsilon){
                        bounds = false;
                    }
                    if((iy>yMax-epsilon&&dy>0)||iy > yMax+epsilon){
                        clamp = true;
                    }
                }
                if(side==4){
                    if((ix<xMin+epsilon&&dx<0)||ix<xMin-epsilon||(ix>xMax-epsilon&&dx>0)||ix>xMax+epsilon){
                        bounds = false;
                    }
                    if((iy < yMin+epsilon&&dy<0)||iy<yMin-epsilon){
                        bounds = false;
                    }
                    if((iy>yMax-epsilon&&dy>0)||iy > yMax+epsilon){
                        clamp = true;
                    }
                }

                if(bounds){
                    float t1 = roots[indices[i+1]];
                    float nextIx = a*t1*t1+b*t1+c;
                    if(clamp){
                        overlap += (nextIx-ix)*zoom;
                    } else {
                        overlap += (float)integrate(a, b, d, e, f, yMin, t, t1);
                    }
                }
            }
        }

        overlap = overlap/zoom/zoom;

        if(overlap<-epsilon||overlap>1+epsilon){
            //System.out.println(overlap);
            //System.out.printf("(%f,%f)", x, y);
            //System.out.printf("glyph: %d", glyph);
            //System.exit(0);
        }

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

    private void findRoots(float a, float b, float c, float[] t, int i){
        t[i] = Float.POSITIVE_INFINITY;
        t[i+1] = Float.POSITIVE_INFINITY;
        //want to solve at^2+bt+c=0 for t
        if (Math.abs(a) < epsilon) {
            if (Math.abs(b) > epsilon) {
                t[i] = -c / b;
            }
            //otherwise no intercept
        } else {
            float dis = b * b - 4 * a * c;
            if (dis > 0) {
                float sqrt = (float) Math.sqrt(dis);
                t[i] = (-b + sqrt) / (2f * a);
                t[i+1] = (-b - sqrt) / (2f * a);
            }
            //otherwise no intercept
        }
    }
}
