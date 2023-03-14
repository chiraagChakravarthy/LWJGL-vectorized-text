package test.differenciator;

import java.util.ArrayList;
import java.util.Arrays;

public class Prod implements Function {
    private final Function[] functions;
    public Prod(Function... functions){
        this.functions = functions;
    }

    @Override
    public Function differentiate() {
        Function[] sum = new Function[functions.length];
        Function[] fCopy = Arrays.copyOf(functions, functions.length);
        for (int i = 0; i < functions.length; i++) {
            fCopy[i] = functions[i].differentiate();
            sum[i] = new Prod(Arrays.copyOf(fCopy, fCopy.length));
            fCopy[i] = functions[i];
        }
        return new Sum(sum);
    }

    @Override
    public double eval(double in) {
        double prod = 1;
        for (int i = 0; i < functions.length; i++) {
            prod *= functions[i].eval(in);
        }
        return prod;
    }

    @Override
    public Function simplify() {
        ArrayList<Function> simplified = new ArrayList<>();
        for (Function function : functions) {
            Function f = function.simplify();
            if (f.equals(INumber.ZERO)) {
                return INumber.ZERO;
            }
            if (f.equals(INumber.ONE)) {
                continue;
            }

            if(f instanceof Prod){
                Prod pf = (Prod) f;
                simplified.addAll(Arrays.asList(pf.functions));
            } else {
                simplified.add(f);
            }
        }
        if(simplified.size()==0){
            return INumber.ONE;
        }
        if (simplified.size()==1){
            return simplified.get(0);
        }
        return new Prod(simplified.toArray(new Function[0]));
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < functions.length; i++) {
            out.append("(").append(functions[i].toString()).append(")");
        }
        return out.toString();
    }
}
