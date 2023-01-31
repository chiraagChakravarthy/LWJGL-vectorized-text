package test.alg_test;

import java.util.Arrays;

public class QuarticTest {
    //-1, -2.206
    public static void main(String[] args) {
        float[] roots = new float[4];
        GlyphRenderer6.solveQuartic(30, -3, -2, -1, -1, roots);
        System.out.println(Arrays.toString(roots));
    }
}
