package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.function.Consumer;

public class EfficiencyE implements AnalyticElement {
    private final Consumer<IterationVariable<Double>> acceptsEfficiency;

    public EfficiencyE(final Consumer<IterationVariable<Double>> acceptsEfficiency) {
        this.acceptsEfficiency = acceptsEfficiency;
    }

    @Override
    public void accept(byte[] bytes, IterationStats iterationStats) {
        acceptsEfficiency.accept(iterationStats.withValue(
                (double) (iterationStats.originalCount() - iterationStats.survivorCount())
                / (iterationStats.iteration() + 1)
        ));
    }
}
