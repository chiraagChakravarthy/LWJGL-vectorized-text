package font_test;

public class XORTest {
    public static void main(String[] args) {
        float a0 = 5, a1 = 20, b0 = 0, b1 = 10;
        float intMin = Math.max(a0, b0),
                intMax = Math.min(a1, b1);
        if(intMin<intMax) {
            if (a0 < intMin && intMax < a1) {
                float oldMax = a1;
                a1 = intMin;
                b0 = intMax;
                b1 = oldMax;
            } else if (b0 < intMin && intMax < b1) {
                float oldMax = b1;
                b1 = intMin;
                a0 = intMax;
                a1 = oldMax;
            } else {
                if (intMin == b0) {
                    b0 = intMax;
                } else {
                    b1 = intMin;
                }
                if (intMin == a0) {
                    a0 = intMax;
                } else {
                    a1 = intMin;
                }
            }
        }
        System.out.printf("(%f,%f)U(%f,%f)", a0, a1, b0, b1);
    }
}
