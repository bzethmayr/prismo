package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.model.Resolving;
import io.github.bzethmayr.prismo.model.Tagged;

import java.util.*;
import java.util.function.Consumer;

/**
 * Declarative description of an {@link AnalyticElement}.
 * <p>
 * The type of analytic is stored as a {@link String} rather than an enum.  A registry
 * maps the string name to a factory that knows how to build the concrete element.
 * The registry is a singleton that is populated during class initialisation.
 */
public record AnalyticDefinition(String type, Map<String, Object> params, List<AnalyticDefinition> children, String tag) implements Tagged {

    /**
     * Factory interface – builds an {@link AnalyticElement} from the supplied
     * child elements and parameters.
     */
    @FunctionalInterface
    interface ElementFactory {
        TaggedAnalyticElement build(
                List<AnalyticDefinition> children,
                Map<String, Object> params,
                CollectorSource collectors,
                String tag
        );
    }

    private static final Map<String, ElementFactory> ELEMENT_REGISTRY = new HashMap<>();

    private static TaggedAnalyticElement sameOneOrFan(
            final List<AnalyticDefinition> children, final CollectorSource collectors, final String tag
    ) {
        if (children.size() == 1) {
            return children.getFirst().build(collectors);
        }
        return new AnalyticElement.FanE(tag, children.stream()
                .map(d -> d.build(collectors))
                .toArray(AnalyticElement[]::new));
    }

    static {
        // Primitive analytics – no children
        elementDefinition("count", element((children, params, collectors, tag) ->
                new CountE(tag, collectors.longCollector(Resolving.paramString(params, "to")))));
        elementDefinition("efficiency", element((children, params, collectors, tag) ->
                new EfficiencyE(tag, collectors.doubleCollector(Resolving.paramString(params, "to")))));
        elementDefinition("differential", element((children, params, collectors, tag) ->
                new DifferentialE(tag, collectors.doubleCollector(Resolving.paramString(params, "to")))));

        // Composite analytics – children are specified by name
        elementDefinition("reacting", element((children, params, collectors, tag) ->
                new ReactingE(tag, children.getFirst().build(collectors),
                        Resolving.paramInt(params, "delta")))); // , List.of("action", "delta")
        elementDefinition("periodic", element((children, params, collectors, tag) ->
                new PeriodicalE(tag, children.getFirst().build(collectors),
                        Resolving.paramInt(params, "period"),
                        Resolving.paramLong(params, "iterations"),
                        Resolving.paramLong(params, "offset")
                ))); // , List.of("action", "period", "iterations", "offset")
        elementDefinition("fan", element((children, params, collectors, tag) ->
                sameOneOrFan(children, collectors, tag))); //, List.of("elements")
    }

    static ElementFactory element(final ElementFactory elementFactory) {
        return elementFactory;
    }

    static void elementDefinition(final String type, final ElementFactory elementFactory) {
        ELEMENT_REGISTRY.put(type, elementFactory);
    }

    public interface CollectorSource {
        Consumer<IterationVariable<Long>> longCollector(final String name);

        Consumer<IterationVariable<Double>> doubleCollector(final String name);
    }

    public AnalyticDefinition(String type, Map<String, Object> params, List<AnalyticDefinition> children, String tag) {
        this.type = Objects.requireNonNull(type);
        this.params = params == null ? Collections.emptyMap() : new HashMap<>(params);
        this.children = children == null ? Collections.emptyList() : new ArrayList<>(children);
        this.tag = tag;
    }

    public AnalyticDefinition(String type, Map<String, Object> params, List<AnalyticDefinition> children) {
        this(type, params, children, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Build the concrete element from this definition                        */
    /* --------------------------------------------------------------------- */

    public TaggedAnalyticElement build(final CollectorSource collectors) {
        ElementFactory factory = ELEMENT_REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalStateException("Unknown analytic type: " + type);
        }
        return factory.build(children, params, collectors, tag);
    }

    /* --------------------------------------------------------------------- */
    /*  Getters – useful for introspection or for a parser that builds the
     *  definition from a string/YAML representation.                           */
    /* --------------------------------------------------------------------- */
    @Override
    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public List<AnalyticDefinition> children() {
        return Collections.unmodifiableList(children);
    }
}
