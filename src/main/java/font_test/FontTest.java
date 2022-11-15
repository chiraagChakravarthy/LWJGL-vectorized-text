package font_test;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import org.lwjgl.stb.STBTruetype;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.lwjgl.stb.STBTruetype.*;

public class FontTest {
    public static void main(String[] args) throws IOException {
        InputStream input = FontTest.class.getResourceAsStream("/font/arial.ttf");

        byte[] file = input.readAllBytes();

        ByteBuffer buffer = BufferUtils.createByteBuffer(file.length);
        buffer = buffer.put(file).flip();

        STBTTFontinfo info = STBTTFontinfo.create();
        stbtt_InitFont(info, buffer);

        int glyph = 68;
        STBTTVertex.Buffer vertices = stbtt_GetGlyphShape(info, glyph);
        int count = vertices.remaining();

        ArrayList<int[]> quadratic = new ArrayList<>(), linear = new ArrayList<>();

        int[] x0a = new int[1], x1a = new int[1], y0a = new int[1], y1a = new int[1];
        stbtt_GetGlyphBox(info, glyph, x0a, y0a, x1a, y1a);
        int x0 = x0a[0];
        int x1 = x1a[0];
        int y0 = y0a[0];
        int y1 = y1a[0];


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
                System.out.printf("(%ft^2+%dt+%d, %dt^2+%dt+%d)%n", climp(ax-2*bx+cx, 0.01f), 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy);
                quadratic.add(new int[]{ax-2*bx+cx, 2*bx-2*cx, cx, ay-2*by+cy, 2*by-2*cy, cy});
            } else if(type==2){
                STBTTVertex nextVertex = vertices.get(i-1);
                int bx = nextVertex.x(), by = nextVertex.y();
                System.out.printf("(%ft^2+%dt+%d, %dt^2+%dt+%d)%n", 0.01f, ax-bx, bx, 0, ay-by, by);
                linear.add(new int[]{ax-bx, bx, ay-by, by});
            }

        }

    }

    static float climp(float a, float min){
        return Math.abs(a)<min?min:a;
    }
}
