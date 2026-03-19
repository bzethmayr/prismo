package io.github.bzethmayr.prismo.model;

import static java.util.Collections.emptySet;

public interface FakeRStats {
    long originalCount();
    long survivorCount();

    default Iterable<byte[]> survivors() {
        return emptySet();
    }

    default FakeRStats statsView() {
        record StatsView(FakeRStats viewed) implements FakeRStats {
            @Override
            public long originalCount() {
                return viewed.originalCount();
            }

            @Override
            public long survivorCount() {
                return viewed.survivorCount();
            }
        }
        return new StatsView(this);
    }
}
