package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;

import java.util.concurrent.atomic.AtomicLong;

public final class ReactingE implements TaggedAnalyticElement {
    private final AnalyticElement action;
    private final AtomicLong lastKnown = new AtomicLong(Long.MIN_VALUE);
    private final int delta;
    private final String tag;

    public ReactingE(final AnalyticElement action, final int delta, final String tag) {
        this.action = action;
        this.delta = delta;
        this.tag = tag;
    }

    public ReactingE(final AnalyticElement action, final int delta) {
        this(action, delta, null);
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public void accept(byte[] bytes, IterationStats iterationStats) {
        long last = lastKnown.get();
        final long surviving = iterationStats.survivorCount();
        if (last == Long.MIN_VALUE) {
            last = surviving + 1;
            lastKnown.set(last);
        }
        if (last - surviving >= delta) {
            lastKnown.set(surviving);
            action.accept(bytes, iterationStats);
        }
    }
}
