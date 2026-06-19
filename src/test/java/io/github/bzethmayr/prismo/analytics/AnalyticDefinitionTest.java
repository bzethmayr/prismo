package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.analytics.AnalyticDefinition.CollectorSource;
import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.model.TestsWithIterationStats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.zethmayr.fungu.test.TestConstants.TEST_RANDOM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AnalyticDefinitionTest implements TestsWithIterationStats {

    private final CollectorSource emptyCollectors = mock();

    @Test
    void elementDefinition_forCountWithoutCollector_goesNowhere() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "count",
                null,
                null
        );

        final AnalyticElement result = underTest.build(emptyCollectors);

        assertNotNull(result);
    }

    @Test
    void elementDefinition_forCountToMissingCollector_goesNowhere() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "count",
                Map.of("to", "foo"),
                null
        );

        final AnalyticElement result = underTest.build(emptyCollectors);

        assertNotNull(result);
    }

    @Test
    void elementDefinition_forCountToCollector_goesToCollector() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "count",
                Map.of("to", "foo"),
                null
        );
        final MappedCollectors collectors = new MappedCollectors();
        final AtomicReference<IterationVariable<Long>> target = new AtomicReference<>();
        collectors.putLongCollector("foo", target::set);
        assertNull(target.get());
        final IterationStats fakeStats = fakeStats(true);
        final long shibboleth = TEST_RANDOM.nextLong();
        doReturn(shibboleth).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        result.accept(new byte[]{}, fakeStats);

        final IterationVariable<Long> collected = target.get();
        assertNotNull(collected);
        assertEquals(shibboleth, collected.value());
    }

    @Test
    void elementDefinition_forEfficiencyToCollector_computesEfficiency() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "efficiency",
                Map.of("to", "eff"),
                null
        );
        final MappedCollectors collectors = new MappedCollectors();
        final AtomicReference<IterationVariable<Double>> target = new AtomicReference<>();
        collectors.putDoubleCollector("eff", target::set);
        final IterationStats fakeStats = fakeStats(true);
        doReturn(256L).when(fakeStats).originalCount();
        doReturn(200L).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        result.accept(new byte[]{}, fakeStats);

        final IterationVariable<Double> collected = target.get();
        assertNotNull(collected);
        assertEquals(56.0, collected.value(), 1e-9);
    }

    @Test
    void elementDefinition_forDifferentialToCollector_computesDifferential() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "differential",
                Map.of("to", "diff"),
                null
        );
        final MappedCollectors collectors = new MappedCollectors();
        final List<IterationVariable<Double>> results = new ArrayList<>();
        collectors.putDoubleCollector("diff", results::add);
        final IterationStats fakeStats = fakeStats(true);
        doReturn(256L).doReturn(200L).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        result.accept(new byte[]{}, fakeStats);
        result.accept(new byte[]{}, fakeStats);

        assertEquals(2, results.size());
        assertEquals(256.0 / 257.0, results.get(0).value(), 1e-9);
        assertEquals(200.0 / 256.0, results.get(1).value(), 1e-9);
    }

    @Test
    void elementDefinition_forReacting_firesActionOnEnoughDelta() {
        final MappedCollectors collectors = new MappedCollectors();
        final AtomicInteger childCount = new AtomicInteger();
        collectors.putLongCollector("child", iv -> childCount.incrementAndGet());

        final AnalyticDefinition childDef = new AnalyticDefinition(
                "count", Map.of("to", "child"), null
        );
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "reacting", Map.of("delta", 5), List.of(childDef)
        );
        final IterationStats fakeStats = fakeStats(true);
        doReturn(100L).doReturn(94L).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        assertEquals(0, childCount.get());

        result.accept(new byte[]{}, fakeStats);
        assertEquals(0, childCount.get());

        result.accept(new byte[]{}, fakeStats);
        assertEquals(1, childCount.get());
    }

    @Test
    void elementDefinition_forPeriodic_firesAtExpectedIterations() {
        final MappedCollectors collectors = new MappedCollectors();
        final AtomicInteger fireCount = new AtomicInteger();
        collectors.putLongCollector("child", iv -> fireCount.incrementAndGet());

        final AnalyticDefinition childDef = new AnalyticDefinition(
                "count", Map.of("to", "child"), null
        );
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "periodic",
                Map.of("period", 3, "iterations", 10L, "offset", 0L),
                List.of(childDef)
        );
        final IterationStats fakeStats = fakeStats(true);
        final AtomicLong iteration = new AtomicLong();
        doAnswer(i -> iteration.get()).when(fakeStats).iteration();
        doReturn(200L).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        for (long i = 0; i < 10; i++) {
            iteration.set(i);
            result.accept(new byte[]{}, fakeStats);
        }

        assertEquals(4, fireCount.get());
    }

    @Test
    void elementDefinition_forFan_dispatchesToAllChildren() {
        final MappedCollectors collectors = new MappedCollectors();
        final AtomicReference<IterationVariable<Long>> child1 = new AtomicReference<>();
        final AtomicReference<IterationVariable<Long>> child2 = new AtomicReference<>();
        collectors.putLongCollector("c1", child1::set);
        collectors.putLongCollector("c2", child2::set);

        final AnalyticDefinition childDef1 = new AnalyticDefinition(
                "count", Map.of("to", "c1"), null
        );
        final AnalyticDefinition childDef2 = new AnalyticDefinition(
                "count", Map.of("to", "c2"), null
        );
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "fan", null, List.of(childDef1, childDef2)
        );
        final IterationStats fakeStats = fakeStats(true);
        final long shibboleth = TEST_RANDOM.nextLong();
        doReturn(shibboleth).when(fakeStats).survivorCount();

        final AnalyticElement result = underTest.build(collectors);
        result.accept(new byte[]{}, fakeStats);

        assertNotNull(child1.get());
        assertEquals(shibboleth, child1.get().value());
        assertNotNull(child2.get());
        assertEquals(shibboleth, child2.get().value());
    }

    @Test
    void elementDefinition_forUnknownType_throws() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "nonexistent", null, null
        );
        assertThrows(IllegalStateException.class, () -> underTest.build(emptyCollectors));
    }
}