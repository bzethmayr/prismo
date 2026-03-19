package io.github.bzethmayr.prismo.unrelated;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public interface RepresentsCLIArgs {
    Map<Object, List<String>> getOptions();
    List<String> getArguments();

    final class MutableCLIArgs implements RepresentsCLIArgs {
        private final Map<Object, List<String>> options = new HashMap<>();
        private final List<String> arguments = new LinkedList<>();

        public MutableCLIArgs() {
        }

        public MutableCLIArgs(final Map<Object, List<String>> options, final List<String> arguments) {
            this.options.putAll(options);
            this.arguments.addAll(arguments);
        }

        public MutableCLIArgs(final RepresentsCLIArgs another) {
            this(another.getOptions(), another.getArguments());
        }

        public Map<Object, List<String>> getOptions() {
            return options;
        }

        public List<String> getArguments() {
            return arguments;
        }
    }

    final class ImmutableCLIArgs implements RepresentsCLIArgs {
        private final Map<Object, List<String>> options;
        private final List<String> arguments;

        public ImmutableCLIArgs(final RepresentsCLIArgs another) {
            this(another.getOptions(), another.getArguments());
        }

        public ImmutableCLIArgs(final Map<Object, List<String>> options, final List<String> arguments) {
            this.options = options.entrySet().stream()
                    .map(e -> new SimpleImmutableEntry<>(
                            e.getKey(),
                            new ArrayList<>(e.getValue())
                    ))
                    .collect(Collectors.toUnmodifiableMap(
                            Entry::getKey, Entry::getValue
                    ));
            this.arguments = arguments.stream().toList();
        }

        @Override
        public Map<Object, List<String>> getOptions() {
            return options;
        }

        @Override
        public List<String> getArguments() {
            return arguments;
        }
    }
}
