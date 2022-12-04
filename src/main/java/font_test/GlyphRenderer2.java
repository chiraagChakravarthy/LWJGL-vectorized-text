package font_test;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import static font_test.FileUtil.loadFont;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;

public class GlyphRenderer2 {
    Main main;
    ArrayList<float[]>[] quadratic;

    STBTTFontinfo fontinfo;
    public int glyph=68;

    public GlyphRenderer2(Main main) {
        this.main = main;
        quadratic = new ArrayList[300];

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

    public void render(Graphics2D g) {
        Point mouse = main.mouseLocation;
        int width = main.getWidth(), height = main.getHeight();
        float sx = mouse.x-width/2f, sy = height/2f- mouse.y;
        float s = (float)Math.sqrt(sx*sx+sy*sy);
        sx *= 1/s;
        sy *= 1/s;
        int zoom = 8;

        BufferedImage image = new BufferedImage(width/zoom, height/zoom, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width/zoom; x++) {
            for (int y = 0; y < height/zoom; y++) {


                float qx=(x)*100/16f*zoom-500, qy=(y)*100/16f*zoom-1000;

                //Result result = trace(qx, qy, (float) Math.cos(ticks*0.01), (float) Math.sin(ticks*0.01));
                int result = trace(qx, qy, sx, sy);
                if(result/2==0){
                    result = trace(qx, qy, sy, -sx);
                }
                float shade = (result%2);
                image.setRGB(x, height/zoom-y-1, new Color((int)(shade*255), (int)(shade*255), (int)(shade*255)).getRGB());
            }
        }
        g.drawImage(image, 0, 0, width, height, null);
    }

    float epsilon = 0.0001f;
    //inside low confidence, outside low confidence, inside high confidence, outside high confidence
    private int trace(float qx, float qy, float sx, float sy){
        //returns u
        float closestPositiveU = Float.POSITIVE_INFINITY, closestNegativeU = Float.NEGATIVE_INFINITY;

        float closestInsideU=Float.POSITIVE_INFINITY,
                closestOutsideU=Float.POSITIVE_INFINITY;
        int closestInsideI=-1, closestOutsideI=-1;

        boolean positiveHighConfidence = true,
                negativeHighConfidence = true,
                positiveInside = false,
                negativeInside = false;

        float h = (float) Math.sqrt(sx*sx+sy*sy);
        sx /= h;
        sy /= h;

        ArrayList<float[]> quadratic = this.quadratic[glyph];

        for (int j = 0; j < quadratic.size(); j++) {
            float[] bezier = quadratic.get(j);

            float a, b, c;
            if(Math.abs(sx)<epsilon){
                a = bezier[0];
                b = bezier[1];
                c = bezier[2]-qx;
            } else if(Math.abs(sy)<epsilon){
                a = bezier[3];
                b = bezier[4];
                c = bezier[5]-qy;
            } else {
                a = bezier[0] / sx - bezier[3] / sy;
                b = bezier[1] / sx - bezier[4] / sy;
                c = (bezier[2] - qx) / sx - (bezier[5] - qy) / sy;
            }
            float t1=Float.POSITIVE_INFINITY, t2=Float.POSITIVE_INFINITY;
            //want to solve at^2+bt+c=0 for t
            if(Math.abs(a)<epsilon){
                if(Math.abs(b)>epsilon){
                    t1 = -c/b;
                }
                //otherwise no intercept
            } else {
                float dis = b*b-4*a*c;
                if(dis>0){
                    float sqrt = (float) Math.sqrt(dis);
                    t1 = (-b+sqrt)/(2f*a);
                    t2 = (-b-sqrt)/(2f*a);
                }
                //otherwise no intercept
            }

            float[] ts = new float[]{t1, t2};
            for(float t : ts){
                if(-epsilon<t&&t<1+epsilon){
                    //intercept registered
                    float ix = t*t*bezier[0]+t*bezier[1]+bezier[2];
                    float iy = t*t*bezier[3]+t*bezier[4]+bezier[5];
                    float dx = t*2*bezier[0]+bezier[1], dy = t*2*bezier[3]+bezier[4];
                    float nx = dy, ny = -dx;
                    float dot = sx*nx+sy*ny;

                    float u;
                    if(Math.abs(sx) > epsilon){
                        u = (ix-qx)/sx;
                    } else {
                        u = (iy-qy)/sy;
                    }

                    boolean highConfidence = epsilon<t&&t<1-epsilon;
                    if(u>0){
                        if(u<closestPositiveU) {
                            positiveHighConfidence = highConfidence;
                            closestPositiveU = u;
                            if(dot<0){
                                positiveInside = true;
                            } else {
                                positiveInside = false;
                            }
                        }
                    } else {
                        if(u>closestNegativeU) {
                            negativeHighConfidence = highConfidence;
                            closestNegativeU = u;
                            if(dot>0){
                                negativeInside = true;
                            } else {
                                negativeInside = false;
                            }
                        }
                    }

                    if(u>0){
                        if(dot<0&&u<closestInsideU){
                            closestInsideI = j;
                            closestInsideU = u;
                        } else if(u<closestOutsideU){
                            closestOutsideI = j;
                            closestOutsideU = u;
                        }
                    }
                }
            }
        }
        boolean inside;
        boolean highConfidence;
        if(positiveHighConfidence){
            highConfidence = true;
            inside = positiveInside;
        } else {
            highConfidence = negativeHighConfidence;
            inside = negativeInside;
        }
        return (inside?0:1)+(highConfidence?1:0)*2;
    }
}
