package test.lib_test;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.stb.STBTruetype.*;
import static test.lib_test.FileUtil.loadFont;

public class FontTest {
    public static void main(String[] args) throws IOException {
        STBTTFontinfo info = loadFont("/font/ariblk.ttf");

        int codepoint = 'Q';

        STBTTVertex.Buffer vertices = stbtt_GetCodepointShape(info, codepoint);
        int count = vertices.remaining();

        ArrayList<int[]> quadratic = new ArrayList<>(), linear = new ArrayList<>();

        int[] x0a = new int[1], x1a = new int[1], y0a = new int[1], y1a = new int[1];
        stbtt_GetCodepointBox(info, codepoint, x0a, y0a, x1a, y1a);
        int x0 = x0a[0];
        int x1 = x1a[0];
        int y0 = y0a[0];
        int y1 = y1a[0];

        System.out.println("scale: " + stbtt_ScaleForMappingEmToPixels(info, 1));


        System.out.printf("(%d+%dt,%d)%n", x0, x1-x0, y0);
        System.out.printf("(%d,%d+%dt)%n", x1, y0, y1-y0);
        System.out.printf("(%d+%dt,%d)%n", x1, x0-x1, y1);
        System.out.printf("(%d,%d+%dt)%n", x0, y1, y0-y1);

        for (int i = count-1; i >=0; i--) {
            STBTTVertex vertex = vertices.get(i);
            int ax = vertex.x(), ay = vertex.y();
            byte type = vertex.type();

            if(type==3){
                int bx = vertex.cx(), by = vertex.cy();
                STBTTVertex nextVertex = vertices.get(i-1);
                int cx = nextVertex.x(), cy = nextVertex.y();
                System.out.printf("(%dt^2+%dt+%d, %dt^2+%dt+%d)%n", ax-2*bx+cx, 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy);
                quadratic.add(new int[]{ax-2*bx+cx, 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy});
            } else if(type==2){
                STBTTVertex nextVertex = vertices.get(i-1);
                int bx = nextVertex.x(), by = nextVertex.y();
                System.out.printf("(%dt^2+%dt+%d, %dt^2+%dt+%d)%n", 0, ax-bx, bx, 0, ay-by, by);
                linear.add(new int[]{ax-bx, bx, ay-by, by});
            }
            //System.out.printf("(%d,%d)%n", ax, ay);
            //System.out.printf("(%d,%d)%n", vertex.cx(), vertex.cy());
        }

    }



    static float climp(float a, float min){
        return Math.abs(a)<min?min:a;
    }
}