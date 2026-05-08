package io.github.bzethmayr.prismo.strawman;

import io.github.bzethmayr.prismo.analytics.AnalyticElement;
import io.github.bzethmayr.prismo.analytics.CountE;
import io.github.bzethmayr.prismo.analytics.PeriodicalE;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.reals.FakeR;
import io.github.bzethmayr.prismo.reals.RectR;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;

import java.util.LinkedList;
import java.util.List;

import static io.github.bzethmayr.prismo.Prismo.guessPrismaticIterations;
import static io.github.bzethmayr.prismo.Prismo.runPrismaticTest;
import static net.zethmayr.fungu.test.TestConstants.TEST_RANDOM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TerribleLCGTest {

    @RepeatedTest(10)
    void terribleLcg_isNotVeryGood() {
        final TerribleLCG strawman = new TerribleLCG(TEST_RANDOM.nextInt(1, Integer.MAX_VALUE));
        final FakeR[] tests = {
                new RectR(256, 256),
                new RectR(512, 128),
                new RectR(1024, 64)
        };
        final FakeR real = new FakeR.FanR(tests);
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

        for (final FakeR lens : tests) {
            final long original = lens.originalCount();
            assertEquals(65536, original);
            final long survived = lens.survivorCount();
            assertNotEquals(32768, survived);
        }
        System.out.printf("Terrible: %s of %s%n", real.survivorCount(), real.originalCount());
        System.out.println(survivorCounts);
        System.out.println(efficiencies);
    }
}