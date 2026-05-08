package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;

public record PeriodicalE(AnalyticElement action, int period, long iterations, long offset, String tag) implements TaggedAnalyticElement {
    public PeriodicalE(final AnalyticElement action, final int period, final long iterations) {
        this(action, period, iterations, null);
    }

    public PeriodicalE(final AnalyticElement action, final int period, final long iterations, final long offset) {
        this(action, period, iterations, offset, null);
    }

    public PeriodicalE(final AnalyticElement action, final int period, final long iterations, String tag) {
        this(action, period, iterations, 0L, tag);
    }

    public PeriodicalE(final AnalyticElement action, final int period, final long iterations, final long offset, String tag) {
        this.action = action;
        this.iterations = iterations;
        this.period = Math.toIntExact(period % iterations);
        this.offset = offset % this.period;
        this.tag = tag;
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
