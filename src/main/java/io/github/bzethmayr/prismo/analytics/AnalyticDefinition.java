package io.github.bzethmayr.prismo.analytics;

import io.github.bzethmayr.prismo.model.IterationVariable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Declarative description of an {@link AnalyticElement}.
 * <p>
 * The type of analytic is stored as a {@link String} rather than an enum.  A registry
 * maps the string name to a factory that knows how to build the concrete element.
 * The registry is a singleton that is populated during class initialisation.
 */
public final class AnalyticDefinition {
    /**
     * Registry entry – holds the factory and the list of child names (if any).
     */
    private static final Map<String, ElementFactory> ELEMENT_REGISTRY = new HashMap<>();
    static {
        // Primitive analytics – no children
        elementDefinition("count", element((children, params, collectors) ->
                new CountE(iv -> {})));
        elementDefinition("efficiency", element((children, params, collectors) ->
                new EfficiencyE(iv -> {})));
        elementDefinition("differential", element((children, params, collectors) ->
                new DifferentialE(iv -> {})));

        // Composite analytics – children are specified by name
        elementDefinition("reacting", element((children, params, collectors) ->
                new ReactingE(children.getFirst().build(collectors),
                        (int) params.get("delta")))); // , List.of("action", "delta")
        elementDefinition("periodical", element((children, params, collectors) ->
                new PeriodicalE(children.getFirst().build(collectors),
                        (int) params.get("period"),
                        (long) params.get("iterations"),
                        (long) params.get("offset")))); // , List.of("action", "period", "iterations", "offset")
        elementDefinition("fan", element((children, params, collectors) ->
                new AnalyticElement.FanE(children.stream()
                        .map(d -> d.build(collectors))
                        .toArray(AnalyticElement[]::new)))); //, List.of("elements")
    }

    static ElementFactory element(final ElementFactory elementFactory) {
        return elementFactory;
    }

    static void elementDefinition(final String type, final ElementFactory elementFactory) {
        ELEMENT_REGISTRY.put(type, elementFactory);
    }

    interface CollectorSource {
        Consumer<IterationVariable<Long>> longCollector(final String name);
        Consumer<IterationVariable<Double>> doubleCollector(final String name);
    }

    /**
     * Factory interface – builds an {@link AnalyticElement} from the supplied
     * child elements and parameters.
     */
    @FunctionalInterface
    interface ElementFactory {
        AnalyticElement build(
                List<AnalyticDefinition> children,
                Map<String, Object> params,
                CollectorSource collectors
        );
    }

    private final String type;
    private final Map<String, Object> params;
    private final List<AnalyticDefinition> children;

    public AnalyticDefinition(String type, Map<String, Object> params, List<AnalyticDefinition> children) {
        this.type = Objects.requireNonNull(type);
        this.params = params == null ? Collections.emptyMap() : new HashMap<>(params);
        this.children = children == null ? Collections.emptyList() : new ArrayList<>(children);
    }

    /* --------------------------------------------------------------------- */
    /*  Build the concrete element from this definition                        */
    /* --------------------------------------------------------------------- */

    public AnalyticElement build(final CollectorSource collectors) {
        ElementFactory factory = ELEMENT_REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalStateException("Unknown analytic type: " + type);
        }
        return factory.build(children, params, collectors);
    }

    /* --------------------------------------------------------------------- */
    /*  Getters – useful for introspection or for a parser that builds the
     *  definition from a string/YAML representation.                           */
    /* --------------------------------------------------------------------- */

    public String getType() { return type; }
    public Map<String, Object> getParams() { return Collections.unmodifiableMap(params); }
    public List<AnalyticDefinition> getChildren() { return Collections.unmodifiableList(children); }
}
