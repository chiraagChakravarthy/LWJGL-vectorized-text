package font_test;

import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;

public class FontTest2 {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Font font;
        try {
            font = Font.createFont(0, GpuGlyphRenderer.class.getResourceAsStream("/font/arial.ttf"));
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        //font = fonts[41];

        Object[] out = new Object[2];
        getGlyphMethod(font, out);
        Method getGlyphOutline = (Method)out[1];
        Object fontstrike = out[0];
        System.out.println(fontstrike.getClass().getName());
        GeneralPath path = (GeneralPath) getGlyphOutline.invoke(fontstrike, 68, 0, 0);
        path.closePath();

        //FontRenderContext renderSettings = new FontRenderContext(null, false, true);

        //GlyphVector v = font.createGlyphVector(renderSettings, "abcde");

        //Shape path = v.getOutline();

        PathIterator iterator = path.getPathIterator(null);

        Field fNumTypes = Path2D.class.getDeclaredField("numTypes");
        Field unsafeF = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeF.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeF.get(null);
        unsafe.putBoolean(fNumTypes, 12, true);
        int k = (int) fNumTypes.get(path);
        System.out.println(k);


        ArrayList<Integer> types = new ArrayList<>();
        ArrayList<float[]> segments = new ArrayList<>();
        do {

            float[] segment = new float[6];
            int type = iterator.currentSegment(segment);
            iterator.next();
            types.add(type);
            segments.add(segment);

            //System.out.println(type + ": " + Arrays.toString(segment));
        } while (!iterator.isDone());


        for (int i = 0; i < types.size(); i++) {
            int type = types.get(i);
            float[] segment = segments.get(i);
            if(type==1){
                //connect to the previous one
                float[] prevSegment = segments.get(i-1);
                //from prev to curr
                int off = types.get(i-1)==2?2:0;
                float ax = segment[0], ay = segment[1];
                float bx = prevSegment[off], by = prevSegment[1+off];
                System.out.printf("(%ft+%f, %ft+%f)%n", ax-bx, bx, -(ay-by), -by);

            } else if(type==2){
                float[] prevSegment = segments.get(i-1);
                int off = types.get(i-1)==2?2:0;
                float ax = prevSegment[off], ay = prevSegment[off+1];

                float bx = segment[0], by = segment[1];
                float cx = segment[2], cy = segment[3];
                System.out.printf("(%ft^2+%ft+%f, %ft^2+%ft+%f)%n", ax-2*bx+cx, 2*bx-2*cx, cx, -(ay-2*by+cy), -(2*by-2*cy), -cy);
            } else if(type==3){
                //panic
            }
        }
    }

    private static void getGlyphMethod(Font font, Object[] out) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"),
                f1 = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        f1.setAccessible(false);
        Unsafe unsafe = (Unsafe) f.get(null);
        int i;//accessible boolean byte offset. should result in 12 for java 17
        for (i = 0; unsafe.getBoolean(f, i) == unsafe.getBoolean(f1, i); i++);

        Class<?> cl = Class.forName("sun.font.StandardGlyphVector$GlyphStrike");
        Field strike = cl.getDeclaredField("strike");
        unsafe.putBoolean(strike, i, true);//write directly into override to bypass perms
        FontRenderContext frc = new FontRenderContext(null, false, false);
        GlyphVector gv = font.createGlyphVector(frc, "a");
        Method getGlyphStrike = Class.forName("sun.font.StandardGlyphVector").getDeclaredMethod("getGlyphStrike", int.class);
        unsafe.putBoolean(getGlyphStrike, i, true);
        Object glyphStrike = getGlyphStrike.invoke(gv, 1);
        Object fontStrike = strike.get(glyphStrike);
        Method getGlyphOutline = fontStrike.getClass().getDeclaredMethod("getGlyphOutline", int.class, float.class, float.class);
        unsafe.putBoolean(getGlyphOutline, i, true);
        out[0] = fontStrike;
        out[1] = getGlyphOutline;
    }
}
