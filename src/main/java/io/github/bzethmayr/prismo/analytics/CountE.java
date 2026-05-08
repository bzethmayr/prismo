package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.function.Consumer;

public class CountE implements TaggedAnalyticElement {
    private final String tag;
    private final Consumer<IterationVariable<Long>> acceptsCounts;

    public CountE(final Consumer<IterationVariable<Long>> acceptsCounts, final String tag) {
        this.tag = tag;
        this.acceptsCounts = acceptsCounts;
    }

    public CountE(final Consumer<IterationVariable<Long>> acceptsCounts) {
        this(acceptsCounts, null);
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public void accept(byte[] bytes, IterationStats stats) {
        acceptsCounts.accept(stats.withValue(stats.survivorCount(), tag));
    }
}
