package test.differenciator;

public class Exp implements Function {
    private final double base;
    private final Function exponent;
    public Exp(double base, Function exponent){
        this.base = base;
        this.exponent = exponent;
    }
    @Override
    public Function differentiate() {
        return new Prod(new Number(Math.log(base)), this, exponent.differentiate());
    }

    @Override
    public double eval(double in) {
        return Math.pow(base, exponent.eval(in));
    }

    @Override
    public Function simplify() {
        Function simpleExp = exponent.simplify();
        if(simpleExp instanceof Number || simpleExp instanceof INumber){
            return new Number(Math.pow(base, simpleExp.eval(0)));
        }
        return new Exp(base, simpleExp);
    }

    @Override
    public String toString() {
        return base + "^(" + exponent.toString() + ")";
    }
}
