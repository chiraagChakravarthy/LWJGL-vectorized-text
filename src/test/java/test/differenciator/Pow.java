package test.differenciator;

public class Pow implements Function {
    private final Function base;
    private final double exponent;
    public Pow(Function base, double exponent){
        this.base = base;
        this.exponent = exponent;
    }

    @Override
    public Function differentiate() {
        return new Prod(new Number(exponent), new Pow(base, exponent-1), base.differentiate());
    }

    @Override
    public double eval(double in) {
        return Math.pow(base.eval(in), exponent);
    }

    @Override
    public Function simplify() {
        Function simpleB = base.simplify();
        if(simpleB instanceof Number || simpleB instanceof INumber){
            return new Number(Math.pow(simpleB.eval(0), exponent));
        }
        return new Pow(simpleB, exponent);
    }

    @Override
    public String toString() {
        return "(" + base + ")^" + exponent;
    }
}
