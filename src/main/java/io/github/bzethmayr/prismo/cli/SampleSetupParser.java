package io.github.bzethmayr.prismo.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The options to Prismo represent a test to run.
 */
public class SampleSetupParser {

    private final Map<String, Function<byte[], byte[]>> mixers = new HashMap<>();

    public void addMixer(final String name, final Function<byte[], byte[]> mixer) {
        mixers.put(name, mixer);
    }
}
