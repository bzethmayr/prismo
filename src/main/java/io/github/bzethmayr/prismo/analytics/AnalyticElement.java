package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.Tagged;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
public interface AnalyticElement extends BiConsumer<byte[], IterationStats> {
    record FanE(String tag, AnalyticElement... elements) implements TaggedAnalyticElement {
        public FanE {
            if (elements == null || elements.length == 0) {
                throw new IllegalArgumentException("Provide some elements");
            }
        }

        public FanE(AnalyticElement... elements) {
            this(null, elements);
        }

        @Override
        public void accept(byte[] sample, IterationStats fakeRStats) {
            Stream.of(elements).parallel().forEach(a -> a.accept(sample, fakeRStats));
        }
    }

    static Function<Long, Long> longDelta() {
        final AtomicLong prior = new AtomicLong(Long.MIN_VALUE);
        return n -> {
            if (prior.get() == Long.MIN_VALUE) {
                prior.set(n);
                return null;
            }
            final long delta = prior.get() - n;
            prior.set(n);
            return delta;
        };
    }

    static Function<Double, Double> doubleDelta() {
        final AtomicReference<Double> prior = new AtomicReference<>();
        return d -> {
            if (prior.get() == null) {
                prior.set(d);
                return null;
            }
            final double delta = prior.get() - d;
            prior.set(d);
            return delta;
        };
    }
}
