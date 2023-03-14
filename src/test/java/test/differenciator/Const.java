package test.differenciator;

public class Const implements Function {
    private final String name;
    private float value;
    public Const(String name){
        this.name = name;
        value = 0;
    }

    @Override
    public Function differentiate() {
        return INumber.ZERO;
    }

    @Override
    public double eval(double in) {
        return value;
    }

    @Override
    public Function simplify() {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
