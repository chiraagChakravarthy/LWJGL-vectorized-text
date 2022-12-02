package font_test;

import java.util.Arrays;

public class SortTest {
    public static void main(String[] args) {
        float[] vals = new float[]{0, 7, 5, 3, 4, 2, 6, 1};
        String net = "[[0,4], [1,5], [2,6], [3,7]]\n" +
                "[[0,2], [1,3], [4,6], [5,7]]\n" +
                "[[2,4], [3,5], [0,1], [6,7]]\n" +
                "[[2,3], [4,5]]\n" +
                "[[1,4], [3,6]]\n" +
                "[[1,2], [3,4], [5,6]]";
        System.out.println(net);
        String[] split1 = net.split("\n");
        for(String line : split1){

            String[] split2 = line.split("]");
            for(String pair : split2){
                pair = pair.replace("[","")
                        .replace(",","")
                        .replace("]","")
                        .replace(" ","");
                int a = (int)pair.charAt(0)-48,
                        b = (int)pair.charAt(1)-48;
                swap(vals, a, b);
            }

            System.out.println(Arrays.toString(vals));
        }
    }

    static void swap(float[] vals, int a, int b){
        float ta = vals[a],
                tb = vals[b];
        vals[a] = Math.min(ta, tb);
        vals[b] = Math.max(ta, tb);
    }
}
