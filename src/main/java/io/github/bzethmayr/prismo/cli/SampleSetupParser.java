package io.github.bzethmayr.prismo.cli;

import io.github.bzethmayr.prismo.Prismo;
import io.github.bzethmayr.prismo.Prismo.SampleSetup;
import io.github.bzethmayr.prismo.analytics.AnalyticDefinition;
import io.github.bzethmayr.prismo.analytics.AnalyticDefinition.AnalyticBuilder;
import io.github.bzethmayr.prismo.analytics.AnalyticElement;
import io.github.bzethmayr.prismo.analytics.MappedCollectors;
import io.github.bzethmayr.prismo.analytics.TaggedAnalyticElement;
import io.github.bzethmayr.prismo.model.IterationStats;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.model.Resolving;
import io.github.bzethmayr.prismo.reals.FakeReals;
import io.github.bzethmayr.prismo.reals.FakeRealsDefinition;
import io.github.bzethmayr.prismo.reals.FakeRealsDefinition.FakeRealsBuilder;
import io.github.bzethmayr.prismo.reals.FanR;
import io.github.bzethmayr.prismo.reals.TaggedFakeReals;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The options to Prismo represent a test to run.
 */
public class SampleSetupParser {
    private static class Parsing {
        final MappedCollectors collectors = new MappedCollectors();
        final Deque<FakeRealsBuilder> fakeRealsStack = new LinkedList<>();
        final Deque<AnalyticBuilder> analyticStack = new LinkedList<>();
        FanR.Reduction reduction;
        Consumer<byte[]> random;
        long explicitIterations;
        boolean explicitIterationsSet;
        int guessRemainders = -1;
        int sampleSize;
        final List<FakeRealsBuilder> rootReals = new LinkedList<>();
        final List<AnalyticBuilder> rootAnalytics = new LinkedList<>();
        final Map<String, List<IterationVariable<Long>>> longCollectorResults = new HashMap<>();
        final Map<String, List<IterationVariable<Double>>> doubleCollectorResults = new HashMap<>();
    }

    private final Map<String, Function<byte[], byte[]>> mixers = new HashMap<>();

    public void addMixer(final String name, final Function<byte[], byte[]> mixer) {
        mixers.put(name, mixer);
    }

    private final Map<String, Consumer<byte[]>> sources = new HashMap<>();

    public void addSource(final String name, final Consumer<byte[]> source) {
        sources.put(name, source);
    }

    private Map<String, List<IterationVariable<Long>>> lastLongCollectorResults = Collections.emptyMap();
    private Map<String, List<IterationVariable<Double>>> lastDoubleCollectorResults = Collections.emptyMap();

    public Map<String, List<IterationVariable<Long>>> getLongCollectorResults() {
        return lastLongCollectorResults;
    }

    public Map<String, List<IterationVariable<Double>>> getDoubleCollectorResults() {
        return lastDoubleCollectorResults;
    }

    public SampleSetup parseArgs(final String... args) {
        final Parsing parsing = new Parsing();
        final int n = args.length;
        int i = 0;

        while (i < n) {
            final String arg = args[i];
            switch (arg) {
                case "-r": {
                    i = parseFakeReals(parsing, args, i);
                    break;
                }
                case "-R": {
                    if (parsing.fakeRealsStack.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched -R");
                    }
                    final FakeRealsBuilder builder = parsing.fakeRealsStack.pop();
                    if (parsing.fakeRealsStack.isEmpty()) {
                        parsing.rootReals.add(builder);
                    } else {
                        parsing.fakeRealsStack.peek().child(builder);
                    }
                    i++;
                    break;
                }
                case "-a": {
                    i = parseAnalyticElement(parsing, args, i);
                    break;
                }
                case "-A": {
                    if (parsing.analyticStack.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched -A");
                    }
                    final AnalyticBuilder builder = parsing.analyticStack.pop();
                    if (parsing.analyticStack.isEmpty()) {
                        parsing.rootAnalytics.add(builder);
                    } else {
                        parsing.analyticStack.peek().child(builder);
                    }
                    i++;
                    break;
                }
                case "-l": {
                    i++;
                    if (i >= n) throw new IllegalArgumentException("-l requires a name");
                    final String name = args[i++];
                    final List<IterationVariable<Long>> list = new ArrayList<>();
                    parsing.longCollectorResults.put(name, list);
                    parsing.collectors.putLongCollector(name, list::add);
                    break;
                }
                case "-d": {
                    i++;
                    if (i >= n) throw new IllegalArgumentException("-d requires a name");
                    final String name = args[i++];
                    final List<IterationVariable<Double>> list = new ArrayList<>();
                    parsing.doubleCollectorResults.put(name, list);
                    parsing.collectors.putDoubleCollector(name, list::add);
                    break;
                }
                case "-s": {
                    i++;
                    if (i >= n) throw new IllegalArgumentException("-s requires a name");
                    final String name = args[i++];
                    final Consumer<byte[]> source = sources.get(name);
                    if (source == null) {
                        throw new IllegalArgumentException("Unknown source: " + name);
                    }
                    parsing.random = source;
                    break;
                }
                case "-i": {
                    i++;
                    if (i >= n) throw new IllegalArgumentException("-i requires a number");
                    parsing.explicitIterations = Long.parseLong(args[i++]);
                    parsing.explicitIterationsSet = true;
                    break;
                }
                case "-g": {
                    i++;
                    if (i < n) {
                        try {
                            parsing.guessRemainders = Integer.parseInt(args[i]);
                            i++;
                        } catch (final NumberFormatException e) {
                            parsing.guessRemainders = 30;
                        }
                    } else {
                        parsing.guessRemainders = 30;
                    }
                    break;
                }
                case "-n": {
                    i++;
                    if (i >= n) throw new IllegalArgumentException("-n requires a number");
                    parsing.sampleSize = Integer.parseInt(args[i++]);
                    break;
                }
                case "-u": {
                    parsing.reduction = FanR.Reduction.UNION;
                    i++;
                    break;
                }
                case "-x": {
                    parsing.reduction = FanR.Reduction.INTERSECTION;
                    i++;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown flag: " + arg);
            }
        }

        if (!parsing.fakeRealsStack.isEmpty()) {
            throw new IllegalArgumentException("Unclosed -r block(s)");
        }
        if (!parsing.analyticStack.isEmpty()) {
            throw new IllegalArgumentException("Unclosed -a block(s)");
        }

        final TaggedFakeReals real = buildReals(parsing);

        final long resolvedIterations;
        if (parsing.guessRemainders >= 0) {
            resolvedIterations = Prismo.guessPrismaticIterations(real.originalCount(), parsing.guessRemainders);
        } else if (parsing.explicitIterationsSet) {
            resolvedIterations = parsing.explicitIterations;
        } else {
            resolvedIterations = 0;
        }

        if (!parsing.rootAnalytics.isEmpty()) {
            final Map<String, Object> globals = new HashMap<>();
            if (resolvedIterations > 0) globals.put("iterations", resolvedIterations);
            if (parsing.sampleSize > 0) globals.put("sampleSize", parsing.sampleSize);
            for (final AnalyticBuilder builder : parsing.rootAnalytics) {
                builder.inheritGlobals(globals);
            }
        }

        final BiConsumer<byte[], IterationStats> analytics = buildAnalytics(parsing);

        if (parsing.random == null) {
            throw new IllegalArgumentException("Random source (-s) is required");
        }
        if (parsing.sampleSize <= 0) {
            throw new IllegalArgumentException("Sample size (-n) is required");
        }

        lastLongCollectorResults = Collections.unmodifiableMap(new HashMap<>(parsing.longCollectorResults));
        lastDoubleCollectorResults = Collections.unmodifiableMap(new HashMap<>(parsing.doubleCollectorResults));

        return new SampleSetup(real, parsing.random, resolvedIterations, parsing.sampleSize, analytics);
    }

    private static boolean isFlag(final String arg) {
        return arg.startsWith("-") && !arg.contains("=");
    }

    static Object parseParamValue(final String name, final String value) {
        final Class<?> hint = Resolving.getKnownType(name);
        if (hint != null) {
            if (hint == int.class || hint == Integer.class) return Integer.parseInt(value);
            if (hint == long.class || hint == Long.class) return Long.parseLong(value);
            if (hint == double.class || hint == Double.class) return Double.parseDouble(value);
            if (hint == String.class) return value;
        }
        try { return Integer.parseInt(value); }
        catch (final NumberFormatException e) {}
        try { return Long.parseLong(value); }
        catch (final NumberFormatException e) {}
        try { return Double.parseDouble(value); }
        catch (final NumberFormatException e) {}
        return value;
    }

    private int parseFakeReals(final Parsing parsing, final String[] args, int i) {
        i++;
        if (i >= args.length) throw new IllegalArgumentException("-r requires a type");
        final String type = args[i++];
        final FakeRealsBuilder builder = new FakeRealsBuilder().type(type);
        while (i < args.length && !isFlag(args[i])) {
            final String[] kv = args[i].split("=", 2);
            if (kv.length != 2) throw new IllegalArgumentException("Bad param: " + args[i]);
            builder.param(kv[0], parseParamValue(kv[0], kv[1]));
            i++;
        }
        parsing.fakeRealsStack.push(builder);
        return i;
    }

    private int parseAnalyticElement(final Parsing parsing, final String[] args, int i) {
        i++;
        if (i >= args.length) throw new IllegalArgumentException("-a requires a type");
        final String type = args[i++];
        final AnalyticBuilder builder = new AnalyticBuilder().type(type);
        while (i < args.length && !isFlag(args[i])) {
            final String[] kv = args[i].split("=", 2);
            if (kv.length != 2) throw new IllegalArgumentException("Bad param: " + args[i]);
            builder.param(kv[0], parseParamValue(kv[0], kv[1]));
            i++;
        }
        parsing.analyticStack.push(builder);
        return i;
    }

    private TaggedFakeReals buildReals(final Parsing parsing) {
        if (parsing.rootReals.isEmpty()) {
            throw new IllegalArgumentException("At least one reals definition (-r) is required");
        }
        final Map<String, Object> realsGlobals = new HashMap<>();
        if (parsing.sampleSize > 0) realsGlobals.put("sampleSize", parsing.sampleSize);
        if (parsing.reduction != null) realsGlobals.put("reduction", parsing.reduction.name());
        if (!realsGlobals.isEmpty()) {
            for (final FakeRealsBuilder builder : parsing.rootReals) {
                builder.inheritGlobals(realsGlobals);
            }
        }
        if (parsing.rootReals.size() == 1) {
            return parsing.rootReals.getFirst().build().build();
        }
        final FakeReals[] basis = parsing.rootReals.stream()
                .map(b -> b.build().build())
                .toArray(FakeReals[]::new);
        return new FanR(
                parsing.reduction != null ? parsing.reduction : FanR.Reduction.INTERSECTION,
                basis
        );
    }

    private BiConsumer<byte[], IterationStats> buildAnalytics(final Parsing parsing) {
        if (parsing.rootAnalytics.isEmpty()) {
            return (b, s) -> {};
        }
        if (parsing.rootAnalytics.size() == 1) {
            final TaggedAnalyticElement elem = parsing.rootAnalytics.getFirst().build().build(parsing.collectors);
            return elem::accept;
        }
        final AnalyticElement[] elements = parsing.rootAnalytics.stream()
                .map(b -> b.build().build(parsing.collectors))
                .toArray(AnalyticElement[]::new);
        return new AnalyticElement.FanE(elements)::accept;
    }
}
