package fun.neural.api.utils.animation;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}