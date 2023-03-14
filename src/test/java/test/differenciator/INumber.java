package test.differenciator;

public class INumber implements Function {
    private final int val;

    public static final INumber ZERO = new INumber(0), ONE = new INumber(1);

    public INumber(int val) {
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
    public boolean equals(Object obj) {
        return obj instanceof INumber && ((INumber)obj).val==val;
    }

    @Override
    public String toString() {
        return val + "";
    }
}
