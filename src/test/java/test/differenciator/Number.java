package test.differenciator;

public class Number implements Function {

    private final double val;
    public Number(double val){
        this.val = val;
    }
    @Override
    public Function differentiate() {
        return new INumber(0);
    }

    @Override
    public double eval(double in) {
        return val;
    }

    @Override
    public Function simplify() {
        return this;
    }

    @Override
    public String toString() {
        return val + "";
    }
}
