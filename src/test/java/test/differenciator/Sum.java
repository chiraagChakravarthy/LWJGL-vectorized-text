package test.differenciator;

import java.util.ArrayList;
import java.util.Arrays;

public class Sum implements Function{
    private final Function[] functions;
    public Sum(Function... functions){
        this.functions = functions;
    }
    @Override
    public Function differentiate() {
        Function[] derivative = new Function[functions.length];
        for (int i = 0; i < functions.length; i++) {
            derivative[i] = functions[i].differentiate();
        }
        return new Sum(derivative);
    }

    @Override
    public double eval(double in) {
        double out = 0;
        for(Function function : functions){
            out += function.eval(in);
        }
        return out;
    }

    @Override
    public Function simplify() {
        ArrayList<Function> simplified = new ArrayList<>();
        for (Function function : functions) {
            Function f = function.simplify();
            if (f.equals(INumber.ZERO)) {
                continue;
            }
            if (f instanceof Sum) {
                Sum sf = (Sum) f;
                simplified.addAll(Arrays.asList(sf.functions));
            }
            simplified.add(f);
        }
        if(simplified.isEmpty()){
            return INumber.ZERO;
        }
        if(simplified.size()==1){
            return simplified.get(0);
        }
        return new Sum(simplified.toArray(new Function[0]));
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(functions[0].toString());
        for(int i = 1; i < functions.length; i++) {
            out.append("+").append(functions[i].toString());
        }
        return out.toString();
    }
}
