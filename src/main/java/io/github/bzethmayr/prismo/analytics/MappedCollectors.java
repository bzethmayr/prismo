package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MappedCollectors implements AnalyticDefinition.CollectorSource {
    private final Map<String, Consumer<IterationVariable<Long>>> longCollectors;
    private final Map<String, Consumer<IterationVariable<Double>>> doubleCollectors;

    public MappedCollectors(
            final Map<String, Consumer<IterationVariable<Long>>> longCollectors,
            final Map<String, Consumer<IterationVariable<Double>>> doubleCollectors) {
        this.longCollectors = longCollectors;
        this.doubleCollectors = doubleCollectors;
    }

    public MappedCollectors() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @Override
    public Consumer<IterationVariable<Long>> longCollector(String name) {
        return Optional.of(longCollectors)
                .map(m -> m.get(name))
                .orElse(v -> {});
    }

    public void putLongCollector(final String name, final Consumer<IterationVariable<Long>> longCollector) {
        longCollectors.put(name, longCollector);
    }

    @Override
    public Consumer<IterationVariable<Double>> doubleCollector(String name) {
        return Optional.of(doubleCollectors)
                .map(m -> m.get(name))
                .orElse(v -> {});
    }

    public void putDoubleCollector(final String name, final Consumer<IterationVariable<Double>> doubleCollector) {
        doubleCollectors.put(name, doubleCollector);
    }
}
