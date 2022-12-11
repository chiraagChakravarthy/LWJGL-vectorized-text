package font_test;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import static text_renderer.FileUtil.loadFont;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;

public class GlyphRenderer {
    Main main;
    ArrayList<float[]>[] quadratic,//ax, bx, cx, ay, by, cy
     linear;//ax, bx, ay, by

    STBTTFontinfo fontinfo;
    public int glyph=68;

    public GlyphRenderer(Main main) {
        this.main = main;
        quadratic = new ArrayList[300];
        linear = new ArrayList[300];

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
                ArrayList<float[]> linear = new ArrayList<>(),
                        quadratic = new ArrayList<>();
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
                        linear.add(new float[]{ax-bx, bx, ay-by, by});
                    }
                }
                this.quadratic[j] = quadratic;
                this.linear[j] = linear;
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
        int zoom = 4;

        BufferedImage image = new BufferedImage(width/zoom, height/zoom, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width/zoom; x++) {
            for (int y = 0; y < height/zoom; y++) {

                float qx=(x)*100/16f*zoom-500, qy=(y)*100/16f*zoom-1000;
                //Result result = trace(qx, qy, (float) Math.cos(ticks*0.01), (float) Math.sin(ticks*0.01));
                float u = trace(qx, qy, sx, sy);
                float shade;
                if(u==Float.POSITIVE_INFINITY){
                    shade = 1;
                } else {
                    shade = Math.min(Math.abs(u), 1);
                }

                if(u<0){
                    shade = 1-shade;
                }
                //shade = count%2==0?1:0;
                image.setRGB(x, height/zoom-y-1, new Color((int)(shade*255), (int)(shade*255), (int)(shade*255)).getRGB());
            }
        }
        g.drawImage(image, 0, 0, width, height, null);
    }

    float epsilon = 0.0001f;
    private float trace(float qx, float qy, float sx, float sy){
        //returns u
        float closestPositiveU = Float.POSITIVE_INFINITY, closestNegativeU = Float.NEGATIVE_INFINITY;

        float closestInsideU=Float.POSITIVE_INFINITY,
                closestOutsideU=Float.POSITIVE_INFINITY;
        int closestInsideI=-1, closestOutsideI=-1;

        boolean positiveHighConfidence = false,
                positiveInside = false,
                negativeInside = false;
        //tells us whether the closest intersection on either side is high confidence


        float h = (float) Math.sqrt(sx*sx+sy*sy);
        sx /= h;
        sy /= h;
        if(Math.abs(sx)<epsilon){
            sx = (sx<0?-1:1)*epsilon;
        }
        if (Math.abs(sy) < epsilon){
            sy = (sy<0?-1:1)*epsilon;
        }

        ArrayList<float[]> quadratic = this.quadratic[glyph],
                linear = this.linear[glyph];

        for (int j = 0; j < quadratic.size(); j++) {
            float[] bezier = quadratic.get(j);

            float a, b, c;
            a = bezier[0] / sx - bezier[3] / sy;
            b = bezier[1] / sx - bezier[4] / sy;
            c = (bezier[2] - qx) / sx - (bezier[5] - qy) / sy;

            float dis = b*b-4*a*c;

            if(dis>epsilon&&Math.abs(a)>epsilon){
                float sqrt = (float) Math.sqrt(dis);
                float[] ts = new float[]{(-b+sqrt)/2f/a, (-b-sqrt)/2f/a};
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
        }

        for (int j = 0; j < linear.size(); j++) {
            float[] line = linear.get(j);
            float a = line[0]/sx-line[2]/sy;
            float b = (line[1]-qx)/sx-(line[3]-qy)/sy;
            float t = -b / a;
            if(-epsilon<t&&t<1+epsilon){
                //intercept registered
                float ix = t*line[0]+line[1];
                float iy = t*line[2]+line[3];

                float dx = line[0], dy = line[2];
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
        if(closestInsideI==-1 && closestOutsideI==-1){
            return Float.POSITIVE_INFINITY;
        }

        boolean inside;
        if(positiveHighConfidence){
            inside = positiveInside;
        } else {
            inside = negativeInside;
        }
        return inside ? -closestInsideU : closestOutsideU;
    }
}
