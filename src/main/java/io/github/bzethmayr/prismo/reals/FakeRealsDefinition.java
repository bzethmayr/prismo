package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.model.Resolving;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public record FakeRealsDefinition(
        String type,
        Map<String, Object> params,
        List<FakeRealsDefinition> children,
        String tag
) {

    @FunctionalInterface
    interface RealsFactory {
        TaggedFakeReals build(
                List<FakeRealsDefinition> children,
                Map<String, Object> params,
                String tag
        );
    }

    private static final Map<String, RealsFactory> REALS_REGISTRY = new HashMap<>();
    private static final Map<String, Function<byte[], byte[]>> MIX_REGISTRY = new HashMap<>();

    static {
        MIX_REGISTRY.put("identity", Function.identity());
    }

    public FakeRealsDefinition {
        Objects.requireNonNull(type);
        params = params == null ? Collections.emptyMap() : new HashMap<>(params);
        children = children == null ? Collections.emptyList() : new ArrayList<>(children);
    }

    public FakeRealsDefinition(String type, Map<String, Object> params, List<FakeRealsDefinition> children) {
        this(type, params, children, null);
    }

    @Override
    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public List<FakeRealsDefinition> children() {
        return Collections.unmodifiableList(children);
    }

    static RealsFactory reals(final RealsFactory realsFactory) {
        return realsFactory;
    }

    static void realsDefinition(final String type, final RealsFactory realsFactory) {
        REALS_REGISTRY.put(type, realsFactory);
    }

    public TaggedFakeReals build() {
        RealsFactory factory = REALS_REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalStateException("Unknown reals type: " + type);
        }
        return factory.build(children, params, tag);
    }

    static {
        realsDefinition("rect", reals((children, params, tag) -> {
            int width = Resolving.paramInt(params, "width");
            int height = Resolving.paramInt(params, "height");
            int sampleSize = params.containsKey("sampleSize")
                    ? Resolving.paramInt(params, "sampleSize")
                    : RectR.DEFAULT_SAMPLE_SIZE;
            return new RectR(tag, width, height, sampleSize);
        }));
        realsDefinition("fan", reals((children, params, tag) -> {
            FanR.Reduction reduction = "UNION".equals(Resolving.paramString(params, "reduction"))
                    ? FanR.Reduction.UNION
                    : FanR.Reduction.INTERSECTION;
            FakeReals[] basis = children.stream()
                    .map(FakeRealsDefinition::build)
                    .toArray(FakeReals[]::new);
            return new FanR(tag, reduction, basis);
        }));
        realsDefinition("mix", reals((children, params, tag) -> {
            String mixName = Resolving.paramString(params, "mix");
            Function<byte[], byte[]> mix = MIX_REGISTRY.getOrDefault(mixName, Function.identity());
            return new MixR(tag, children.getFirst().build(), mix);
        }));
        realsDefinition("mismatch", reals((children, params, tag) -> {
            long originalCount = Resolving.paramLong(params, "originalCount");
            double tolerance = MismatchR.DEFAULT_TOLERANCE;
            if (params.containsKey("tolerance")) {
                tolerance = ((Number) params.get("tolerance")).doubleValue();
            }
            return new MismatchR(tag, originalCount, children.getFirst().build(), tolerance);
        }));
    }
}
