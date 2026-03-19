package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.function.Consumer;

public class CountE implements AnalyticElement {
    private final Consumer<IterationVariable<Long>> acceptsCounts;

    public CountE(final Consumer<IterationVariable<Long>> acceptsCounts) {
        this.acceptsCounts = acceptsCounts;
    }

    @Override
    public void accept(byte[] bytes, IterationStats stats) {
        acceptsCounts.accept(stats.withValue(stats.survivorCount()));
    }
}
