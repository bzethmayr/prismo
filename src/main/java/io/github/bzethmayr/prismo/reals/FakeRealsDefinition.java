package io.github.bzethmayr.prismo.reals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FakeRealsDefinition() {

    @FunctionalInterface
    interface RealsFactory {
        TaggedFakeReals build(
                List<FakeRealsDefinition> children,
                Map<String, Object> params,
                String tag
        );
    }

    private static final Map<String, RealsFactory> REALS_REGISTRY = new HashMap<>();

}
