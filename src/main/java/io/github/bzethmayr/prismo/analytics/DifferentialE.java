package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DifferentialE implements AnalyticElement {
    private final AtomicLong prior = new AtomicLong();

    private final Consumer<IterationVariable<Double>> acceptsDifferential;

    public DifferentialE(final Consumer<IterationVariable<Double>> acceptsDifferential) {
        this.acceptsDifferential = acceptsDifferential;
    }

    public AnalyticElement preSample() {
        return (b, s) -> prior.set(s.survivorCount());
    }

    @Override
    public void accept(byte[] bytes, IterationStats stats) {
        final long survivors = stats.survivorCount();
        long lastSurvivors = prior.get();
        if (lastSurvivors == 0) {
            lastSurvivors = survivors + 1;
        }
        acceptsDifferential.accept(stats.withValue((double) survivors / (double) lastSurvivors));
        prior.set(survivors);
    }
}
