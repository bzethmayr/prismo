package io.github.bzethmayr.prismo.model;

public interface IterationVariable<T> extends Tagged {
    long iteration();
    T value();

    static <T> IterationVariable<T> iterationValue(final long iteration, final T value, final String tag) {
        return new IterationValue<>(iteration, value, tag);
    }

    default <Q> IterationVariable<Q> withValue(final Q anotherValue, String tag) {
        return iterationValue(iteration(), anotherValue, tag);
    }

    default <Q> IterationVariable<Q> withValue(final Q anotherValue) {
        return iterationValue(iteration(), anotherValue, tag());
    }

    record IterationValue<T>(long iteration, T value, String tag) implements IterationVariable<T> {
    }
}
