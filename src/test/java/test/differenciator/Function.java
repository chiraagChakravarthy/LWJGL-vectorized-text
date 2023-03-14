package test.differenciator;

public interface Function {
    Function differentiate();
    double eval(double in);
    Function simplify();
}
