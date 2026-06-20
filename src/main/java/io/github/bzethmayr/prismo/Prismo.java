package io.github.bzethmayr.prismo;

import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.reals.FakeReals;
import io.github.bzethmayr.prismo.model.FakeRStats;
import io.github.bzethmayr.prismo.model.IterationStats;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Prismo {

    public static long guessPrismaticIterations(final long domain, final int remainders) {
        if (domain <= 0) {
            throw new IllegalArgumentException("domain must be positive");
        }
        if (remainders <= 0) {
            // want essentially full coverage; push far out on the curve
            return (long) Math.ceil(domain * Math.log(domain));
        }
        if (remainders >= domain) {
            // no coverage needed
            return 0L;
        }

        // Use the continuous approximation: m ≈ domain * ln(domain / remainders)
        double d = (double) domain;
        double r = remainders;
        double m = d * Math.log(d / r);

        return (long) Math.ceil(m);
    }

    public static void runPrismaticTest(
            final FakeReals real,
            final Consumer<byte[]> random,
            final long iterations,
            final int sampleSize
    ) {
        runPrismaticTest(real, random, iterations, sampleSize, (b, r) -> {});
    }

    private static class RunStats implements IterationStats {
        private final FakeRStats basis;
        public long iteration;

        RunStats(final FakeRStats basis) {
            this.basis = basis;
            this.iteration = -1L;
        }

        @Override
        public String tag() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long iteration() {
            return iteration;
        }

        @Override
        public long originalCount() {
            return basis.originalCount();
        }

        @Override
        public long survivorCount() {
            return basis.survivorCount();
        }

        @Override
        public Iterable<byte[]> survivors() {
            return basis.survivors();
        }

        @Override
        public FakeRStats statsView() {
            return this;
        }
    }

    /**
     * Runs prismatic test.
     * @param real the fake reals.
     * @param random the randomness source, populating with fresh bytes on each call.
     * @param iterations the iteration count - see {@link #guessPrismaticIterations(long, int)}
     * @param sampleSize up to you and the source.
     * @param analytics analytics hook.
     */
    public static void runPrismaticTest(
            final FakeReals real,
            final Consumer<byte[]> random,
            final long iterations,
            final int sampleSize,
            final BiConsumer<byte[], IterationStats> analytics
    ) {
        final byte[] sample = new byte[sampleSize];
        final RunStats stats = new RunStats(real.statsView());
        for (long i = 0; i < iterations; i++) {
            stats.iteration = i;
            random.accept(sample);
            real.remove(sample);
            analytics.accept(sample, stats);
        }
    }

    public record SampleSetup(
            FakeReals real,
            String randomName,
            Consumer<byte[]> random,
            long iterations,
            int sampleSize,
            BiConsumer<byte[], IterationStats> analytics,
            Map<String, List<IterationVariable<Long>>> longs,
            Map<String, List<IterationVariable<Double>>> doubles
    ) {}

    public static void runPrismaticTest(final SampleSetup s) {
        runPrismaticTest(s.real, s.random, s.iterations, s.sampleSize, s.analytics);
    }
}
