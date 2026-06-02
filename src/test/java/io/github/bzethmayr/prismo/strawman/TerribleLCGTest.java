package io.github.bzethmayr.prismo.strawman;

import io.github.bzethmayr.prismo.analytics.AnalyticElement;
import io.github.bzethmayr.prismo.analytics.CountE;
import io.github.bzethmayr.prismo.analytics.PeriodicalE;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.reals.FakeR;
import io.github.bzethmayr.prismo.reals.FanR;
import io.github.bzethmayr.prismo.reals.RectR;
import io.github.bzethmayr.prismo.reals.TestsWithRectR;
import org.junit.jupiter.api.RepeatedTest;

import java.util.LinkedList;
import java.util.List;

import static io.github.bzethmayr.prismo.Prismo.guessPrismaticIterations;
import static io.github.bzethmayr.prismo.Prismo.runPrismaticTest;
import static net.zethmayr.fungu.test.TestConstants.TEST_RANDOM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TerribleLCGTest implements TestsWithRectR {
    int seed;

    @RepeatedTest(64)
    void terribleLcg_breaksOnDistortedRects() {
        seed = TEST_RANDOM.nextInt();
        final TerribleLCG strawman = new TerribleLCG(seed);
        final FakeR[] tests = {
                new RectR(CHAR_SQRT, CHAR_SQRT, 2),
                new RectR(CHAR_WIDE, CHAR_THIN, 2),
                new RectR(CHAR_THIN, CHAR_WIDE, 2)
        };
        final FakeR real = new FanR(FanR.Reduction.UNION, tests);
        final List<IterationVariable<Long>> survivorCounts = new LinkedList<>();
        final List<IterationVariable<Double>> efficiencies = new LinkedList<>();
        final CountE counts = new CountE(survivorCounts::add, "left");
        final long iterations = guessPrismaticIterations(65536, 30);
        final int period = (int) iterations / 32;

        runPrismaticTest(real, strawman, iterations, 2, new AnalyticElement.FanE(
                new PeriodicalE(counts, period, iterations),
                new PeriodicalE(counts, period, iterations, 1),
                new PeriodicalE(counts, period, iterations, 2)
        ));

        System.out.printf("Terrible(%s): %s of %s%n", seed, real.survivorCount(), real.originalCount());
        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertThat(survived, lessThanOrEqualTo(48L));
            assertThat(survived, greaterThanOrEqualTo(13L));
        }
        System.out.println(survivorCounts);
        System.out.println(efficiencies);
    }
}