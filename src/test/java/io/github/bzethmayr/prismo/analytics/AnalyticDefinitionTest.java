package io.github.bzethmayr.prismo.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticDefinitionTest {

    private final AnalyticDefinition.CollectorSource emptyCollectors = new MappedCollectors();

    @Test
    void elementDefinition_forCount_noCollector_goesNowhere() {
        final AnalyticDefinition underTest = new AnalyticDefinition(
                "count",
                null,
                null
        );

        final AnalyticElement result = underTest.build(emptyCollectors);

        assertNotNull(result);
    }
}