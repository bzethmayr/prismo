package io.github.bzethmayr.prismo.model;

public interface IterationStats extends FakeRStats, IterationVariable<FakeRStats> {
    long iteration();

    @Override
    default FakeRStats value() {
        return statsView();
    }
}
