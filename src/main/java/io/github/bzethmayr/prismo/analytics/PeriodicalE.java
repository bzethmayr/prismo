package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;

public record PeriodicalE(AnalyticElement action, int period, long iterations, long offset) implements AnalyticElement {
    public PeriodicalE(final AnalyticElement action, final int period, final long iterations) {
        this(action, period, iterations, 0);
    }

    public PeriodicalE(final AnalyticElement action, final int period, final long iterations, final long offset) {
        this.action = action;
        this.iterations = iterations;
        this.period = Math.toIntExact(period % iterations);
        this.offset = offset % this.period;
    }

    @Override
    public void accept(final byte[] sample, final IterationStats stats) {
        final long iteration = stats.iteration();
        if (iteration >= iterations) {
            return;
        }
        if (iteration == offset
                || (iteration - offset) % period == 0
                || (iteration + offset) == iterations - 1
        ) {
            action.accept(sample, stats);
        }
    }
}
