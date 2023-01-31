package test.alg_test;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import test.lib_test.FileUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;

public class GlyphRenderer4 {
    Main main;
    ArrayList<float[]>[] quadratic;

    STBTTFontinfo fontinfo;
    public int codepoint =68;

    public GlyphRenderer4(Main main) {
        this.main = main;
        quadratic = new ArrayList[300];

        try {
            initFont("/font/arial.ttf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //4-200
        initGlyphs();
    }

    private void initFont(String font) throws IOException {
        fontinfo = FileUtil.loadFont(font);
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
    public void render(Graphics2D g) {
        int width = main.getWidth(), height = main.getHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                //float qx = (float) ((x-.1f*width+Math.cos(ticks*.01f)*25)*zoom);
                //float qy = (float) ((y-.1f*height+Math.sin(ticks*.01f)*25)*zoom);

                float qx = (x-0.1f*width)*zoom;
                float qy = (y-0.1f*height)*zoom;

                int samples = 32;
                float shade = 0;
                for (int i = 0; i < samples; i++) {
                    float a = (float) (Math.PI/samples*i+0.01f);
                    float s1 = sample(qx, qy, (float) Math.cos(a), (float) Math.sin(a));
                    float s2 = sample(qx, qy, (float) -Math.cos(a), (float) -Math.sin(a));
                    shade += s1+s2;
                }
                shade /= samples*2;

                //shade = count%2==0?1:0;
                image.setRGB(x, height-y-1, new Color((int)(shade*255), (int)(shade*255), (int)(shade*255)).getRGB());
            }
        }
        g.drawImage(image, 0, 0, width, height, null);
    }

    float epsilon = 0.0001f;

    /*
    circular aperature centered at q, sampling in direction x
    want to figure out fraction of that line inside the glyph
    each sample follows basic scheme:
    closest exit, closest entrance
     */


    private float sample(float qx, float qy, float sx, float sy){

        float exit = Float.POSITIVE_INFINITY;
        float enter = Float.POSITIVE_INFINITY;

        float h = (float) Math.sqrt(sx*sx+sy*sy);
        sx /= h;
        sy /= h;

        float aaRange = clamp(-0.00875f*zoom+1.675f, 1f, 1.5f)*zoom;

        qx = qx - sx*aaRange/2;
        qy = qy - sy*aaRange/2;

        ArrayList<float[]> glyph = this.quadratic[this.codepoint];

        for (float[] bezier : glyph) {
            float a, b, c;
            if (Math.abs(sx) < epsilon) {
                a = bezier[0];
                b = bezier[1];
                c = bezier[2] - qx;
            } else if (Math.abs(sy) < epsilon) {
                a = bezier[3];
                b = bezier[4];
                c = bezier[5] - qy;
            } else {
                a = bezier[0] / sx - bezier[3] / sy;
                b = bezier[1] / sx - bezier[4] / sy;
                c = (bezier[2] - qx) / sx - (bezier[5] - qy) / sy;
            }
            float t1 = Float.POSITIVE_INFINITY, t2 = Float.POSITIVE_INFINITY;
            //want to solve at^2+bt+c=0 for t
            if (Math.abs(a) < epsilon) {
                if (Math.abs(b) > epsilon) {
                    t1 = -c / b;
                }
                //otherwise no intercept
            } else {
                float dis = b * b - 4 * a * c;
                if (dis > 0) {
                    float sqrt = (float) Math.sqrt(dis);
                    t1 = (-b + sqrt) / (2f * a);
                    t2 = (-b - sqrt) / (2f * a);
                }
                //otherwise no intercept
            }

            float[] ts = new float[]{t1, t2};
            for (float t : ts) {
                if (-epsilon*epsilon < t && t < 1 + epsilon*epsilon) {
                    //intercept registered
                    float ix = t * t * bezier[0] + t * bezier[1] + bezier[2];
                    float iy = t * t * bezier[3] + t * bezier[4] + bezier[5];
                    float dx = t * 2 * bezier[0] + bezier[1], dy = t * 2 * bezier[3] + bezier[4];
                    float nx = dy, ny = -dx;
                    float dot = sx * nx + sy * ny;

                    float u;
                    if (Math.abs(sx) > epsilon) {
                        u = (ix - qx) / sx;
                    } else {
                        u = (iy - qy) / sy;
                    }
                    //inside = dot < 0
                    //basically, if dot<0, that means the point of origin is on the inside side of the line
                    //thus this is where it is exiting the glyph

                    if(u>0){
                        if(dot<0 && u < exit){
                            exit = u;
                        }
                        if(dot > 0 && u < enter){
                            enter = u;
                        }
                    }
                }
            }
        }

        //if the line does not exit the glyph, no part of the line is in the glyph

        if(exit==Float.POSITIVE_INFINITY){
            return 1;
        }
        //entered then exited
        if(enter<exit&&enter<aaRange){
            return (aaRange-Math.min(exit-enter, aaRange))/aaRange;
        }

        if(exit<enter){
            return (aaRange-(exit<aaRange?exit+(enter<aaRange?aaRange-enter:0):aaRange))/aaRange;
        }
        return 1;
    }
}