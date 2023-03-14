package test.differenciator;

public class X implements Function {
    @Override
    public Function differentiate() {
        return new INumber(1);
    }

    @Override
    public double eval(double in) {
        return in;
    }

    @Override
    public Function simplify() {
        return this;
    }

    @Override
    public String toString() {
        return "t";
    }
}
