package io.github.bzethmayr.prismo.model;

public interface IterationVariable<T> {
    long iteration();
    T value();

    static <T> IterationVariable<T> iterationValue(final long iteration, final T value) {
        return new IterationValue<>(iteration, value);
    }

    default <Q> IterationVariable<Q> withValue(final Q anotherValue) {
        return iterationValue(iteration(), anotherValue);
    }

    record IterationValue<T>(long iteration, T value) implements IterationVariable<T> {
    }
}
