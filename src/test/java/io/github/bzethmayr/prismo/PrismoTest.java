package io.github.bzethmayr.prismo;

import io.github.bzethmayr.prismo.analytics.*;
import io.github.bzethmayr.prismo.analytics.AnalyticElement.FanE;
import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.reals.FakeR;
import io.github.bzethmayr.prismo.reals.FakeR.FanR;
import io.github.bzethmayr.prismo.reals.FakeR.MismatchR;
import io.github.bzethmayr.prismo.reals.RectR;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.bzethmayr.prismo.Prismo.guessPrismaticIterations;
import static io.github.bzethmayr.prismo.Prismo.runPrismaticTest;
import static java.math.RoundingMode.HALF_EVEN;
import static org.junit.jupiter.api.Assertions.*;

class PrismoTest {

    static Stream<Consumer<byte[]>> samplers() {
        final Random random = new Random();
        final SecureRandom randomer = new SecureRandom();
        final int n = 10;
        return Stream.concat(
                IntStream.range(0, n / 2).mapToObj(x -> random::nextBytes),
                IntStream.range(0, n / 2).mapToObj(x -> randomer::nextBytes)
        );
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_twoBytes(final Consumer<byte[]> sampler) {
        final FakeR[] tests = {
                new RectR(256, 256),
                new RectR(512, 128),
                new RectR(1024, 64)
        };
        final FakeR real = new FanR(tests);

        runPrismaticTest(real, sampler, guessPrismaticIterations(65536, 30), 2);

        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertNotEquals(65536, survived);
        }
        System.out.printf("%s of %s%n", real.survivorCount(), real.originalCount());
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_twoBytes_samplingCounts(final Consumer<byte[]> sampler) {
        final FakeR[] tests = {
                new RectR(256, 256),
                new RectR(512, 128),
                new RectR(1024, 64)
        };
        final FakeR real = new FanR(tests);
        final long iterations = guessPrismaticIterations(65536, 30);
        final int period = (int) iterations / 32;
        final List<IterationVariable<Long>> survivorCounts = new LinkedList<>();
        final CountE counts = new CountE(survivorCounts::add, "left");

        runPrismaticTest(real, sampler, iterations, 2, new AnalyticElement.FanE(
                new PeriodicalE(counts, period, iterations),
                new PeriodicalE(counts, period, iterations, 1),
                new PeriodicalE(counts, period, iterations, 2)
        ));

        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertNotEquals(65536, survived);
        }
        System.out.printf("%s of %s%n", real.survivorCount(), real.originalCount());
        System.out.println(survivorCounts);
    }


    private BiConsumer<byte[], IterationStats> badAnalytics(final AtomicLong iterations) {
        final AtomicLong priorSurvivors = new AtomicLong(Long.MAX_VALUE);
        return (s, r) -> {
            iterations.incrementAndGet();
            final long priorCapture = priorSurvivors.get();
            final long survivors = r.survivorCount();
            if (survivors < priorCapture) {
                priorSurvivors.compareAndExchange(priorCapture, survivors);
            }
        };
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_twoBytesWithAnalytics(final Consumer<byte[]> sampler) {
        final FakeR[] tests = {
                new RectR(256, 256),
                new RectR(512, 128),
                new RectR(1024, 64)
        };
        final FakeR real = new FanR(tests);
        final AtomicLong iterations = new AtomicLong();
        final BiConsumer<byte[], IterationStats> analytics = badAnalytics(iterations);

        runPrismaticTest(
                real, sampler, guessPrismaticIterations(65536, 30),
                2, analytics);

        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertNotEquals(65536, survived);
        }
        assertTrue(iterations.get() > 65536);
        System.out.printf("%s of %s%n", real.survivorCount(), real.originalCount());
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_twoBytesMultipleGeometries(final Consumer<byte[]> sampler) {
        final FakeR[] tests = new FakeR[]{
                new RectR(256, 256),
                new MismatchR(65536, new RectR(257, 255)),
                new MismatchR(65536, new RectR(258, 253))
        };
        final FakeR real = new FanR(tests);

        runPrismaticTest(real, sampler, guessPrismaticIterations(65536, 30), 2);

        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertNotEquals(65536, survived);
        }
        System.out.printf("%s of %s%n", real.survivorCount(), real.originalCount());
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_threeBytesSamplingCountsAndSlopes(final Consumer<byte[]> samplers) {
        final FakeR[] tests = {
                new RectR(600, 600, 3),
                new RectR(2000, 180, 3),
                new RectR(360, 1000, 3)
        };
        final long iterations = guessPrismaticIterations(360000, 100);
        final FakeR real = new FanR(tests);
        final List<IterationVariable<Long>> survivorCounts = new LinkedList<>();
        final List<IterationVariable<Double>> efficiencies = new LinkedList<>();
        final CountE counts = new CountE(survivorCounts::add);
        final EfficiencyE slopes = new EfficiencyE(efficiencies::add);

        runPrismaticTest(real, samplers, iterations, 3, new FanE(
                new PeriodicalE(counts, 125000, iterations),
                new PeriodicalE(counts, 125000, iterations, 1),
                new PeriodicalE(counts, 125000, iterations, 2),
                new PeriodicalE(slopes, 125000, iterations),
                new PeriodicalE(slopes, 125000, iterations, 1),
                new PeriodicalE(slopes, 125000, iterations, 2)
        ));

        assertEquals(75, survivorCounts.size());
        System.out.println(survivorCounts);
        assertEquals(75, efficiencies.size());
        System.out.println(efficiencies);
        assertTrue(real.survivorCount() < real.originalCount());
    }

    @ParameterizedTest
    @MethodSource("samplers")
    void runPrismaticTest_threeBytesReactiveSamplingSlopes(final Consumer<byte[]> samplers) {
        final FakeR[] tests = {
                new RectR(600, 600, 3),
                new RectR(2000, 180, 3),
                new RectR(360, 1000, 3)
        };
        final long iterations = guessPrismaticIterations(360000, 100);
        final List<IterationVariable<Double>> efficiencies = new LinkedList<>();
        final FakeR real = new FanR(tests);
        final EfficiencyE slopes = new EfficiencyE(efficiencies::add);

        runPrismaticTest(real, samplers, iterations, 3, new FanE(
                new ReactingE(slopes, 3600)
        ));

        assertTrue(efficiencies.size() <= 100);
        System.out.println(efficiencies.stream().map(d -> d.withValue(BigDecimal.valueOf(d.value()).setScale(7, HALF_EVEN))).toList());
        assertTrue(real.survivorCount() < real.originalCount());
    }
}