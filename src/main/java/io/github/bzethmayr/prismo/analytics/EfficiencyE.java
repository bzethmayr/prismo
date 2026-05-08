package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.function.Consumer;

public class EfficiencyE implements TaggedAnalyticElement {
    private final Consumer<IterationVariable<Double>> acceptsEfficiency;
    private final String tag;

    public EfficiencyE(final Consumer<IterationVariable<Double>> acceptsEfficiency, final String tag) {
        this.acceptsEfficiency = acceptsEfficiency;
        this.tag = tag;
    }

    public EfficiencyE(final Consumer<IterationVariable<Double>> acceptsEfficiency) {
        this(acceptsEfficiency, null);
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public void accept(byte[] bytes, IterationStats iterationStats) {
        acceptsEfficiency.accept(iterationStats.withValue(
                (double) (iterationStats.originalCount() - iterationStats.survivorCount())
                / (iterationStats.iteration() + 1), tag
        ));
    }
}
