package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.analytics.AnalyticDefinition.CollectorSource;
import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.model.TestsWithIterationStats;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static net.zethmayr.fungu.test.TestConstants.TEST_RANDOM;
import static org.junit.jupiter.api.Assertions.*;
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
}