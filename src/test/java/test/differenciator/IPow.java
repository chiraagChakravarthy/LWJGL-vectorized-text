package test.differenciator;

public class IPow implements Function {
    private final Function base;
    private final int exponent;

    public IPow(Function base, int exponent){
        this.base = base;
        this.exponent = exponent;
    }
    @Override
    public Function differentiate() {
        return new Prod(new INumber(exponent), new IPow(base, exponent-1), base.differentiate());
    }

    @Override
    public double eval(double in) {
        return Math.pow(base.eval(in), exponent);
    }

    @Override
    public Function simplify() {
        Function simple = base.simplify();
        if(exponent==1){
            return simple;
        }
        if(exponent==0){
            return new INumber(1);
        }
        return new IPow(simple, exponent);
    }

    @Override
    public String toString() {
        return "(" + base.toString() + ")^" + exponent;
    }
}
