package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.model.Resolving;

import java.util.*;
import java.util.function.Function;

import static io.github.bzethmayr.prismo.model.Resolving.*;
import static io.github.bzethmayr.prismo.reals.FanR.resolvingReduction;
import static io.github.bzethmayr.prismo.reals.MismatchR.DEFAULT_TOLERANCE;
import static io.github.bzethmayr.prismo.reals.RectR.DEFAULT_SAMPLE_SIZE;
import static net.zethmayr.fungu.core.ExceptionFactory.becauseIllegal;

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
            throw becauseIllegal("Unknown reals type: %s", type);
        }
        return factory.build(children, params, tag);
    }

    private static TaggedFakeReals fan(
            final List<FakeRealsDefinition> children, final FanR.Reduction reduction, final String tag
    ) {
        FakeReals[] basis = children.stream()
                .map(FakeRealsDefinition::build)
                .toArray(FakeReals[]::new);
        return new FanR(tag, reduction, basis);
    }

    private static TaggedFakeReals sameOneOrFan(
            final List<FakeRealsDefinition> children, final FanR.Reduction reduction, final String tag
    ) {
        if (children.size() == 1) {
            return children.getFirst().build();
        }
        return fan(children, reduction, tag);
    }

    static {
        final Resolving.IntResolver widthParam = registeredParamInt("width");
        final Resolving.IntResolver heightParam = registeredParamInt("height");
        final Resolving.IntResolver sampleParam = registeredParamInt("sampleSize", DEFAULT_SAMPLE_SIZE); // should be inherited, tho
        realsDefinition("rect", reals((children, params, tag) ->
                new RectR(tag, widthParam.resolve(params), heightParam.resolve(params), sampleParam.resolve(params))));
        realsDefinition("fan", reals((children, params, tag) ->
                fan(children, resolvingReduction(params), tag)));
        final Resolving.GenericResolver<String> mixParam = registeredParamString("mix");
        realsDefinition("mix", reals((children, params, tag) ->
                new MixR(tag, sameOneOrFan(children, resolvingReduction(params), tag),
                        MIX_REGISTRY.getOrDefault(mixParam.resolve(params), Function.identity()))));
        final Resolving.LongResolver ocResolver = registeredParamLong("originalCount"); // should be inherited tho
        final Resolving.DoubleResolver toleranceResolver = registeredParamDouble("tolerance", DEFAULT_TOLERANCE);
        realsDefinition("mismatch", reals((children, params, tag) ->
                new MismatchR(tag, ocResolver.resolve(params), sameOneOrFan(children, resolvingReduction(params), tag),
                        toleranceResolver.resolve(params))));
    }

    public static class FakeRealsBuilder {
        private String type;
        private final Map<String, Object> params = new HashMap<>(); // both direct and inherited (some prepopulation expected)
        private final List<FakeRealsDefinition.FakeRealsBuilder> children = new LinkedList<>();
        private String tag;
        public FakeRealsBuilder type(final String type) {
            this.type = type;
            return this;
        }
        public FakeRealsBuilder param(final String name, final Object value) {
            params.put(name, value);
            return this;
        }
        public FakeRealsBuilder child(final FakeRealsDefinition.FakeRealsBuilder child) {
            children.add(child);
            return this;
        }
        public FakeRealsBuilder tag(final String tag) {
            this.tag = tag;
            return this;
        }
        public FakeRealsBuilder inheritGlobals(final Map<String, Object> globals) {
            globals.forEach((k, v) -> params.putIfAbsent(k, v));
            children.forEach(c -> c.inheritGlobals(globals));
            return this;
        }

        public FakeRealsDefinition build() {
            return new FakeRealsDefinition(type, params, children.stream().map(FakeRealsBuilder::build).toList(), tag);
        }
    }
}
