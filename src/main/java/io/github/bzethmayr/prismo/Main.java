package io.github.bzethmayr.prismo;

import io.github.bzethmayr.prismo.cli.SampleSetupParser;

import java.security.SecureRandom;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        final SampleSetupParser parser = new SampleSetupParser();
        parser.addSource("SecureRandom", new SecureRandom()::nextBytes);
        parser.addSource("Random", new Random()::nextBytes);

        final Prismo.SampleSetup setup = parser.parseArgs(args);
        Prismo.runPrismaticTest(setup);

        System.out.printf("Iterations: %d%n", setup.iterations());
        System.out.printf("Sample size: %d%n", setup.sampleSize());
        System.out.printf("Survivors: %d / %d%n",
                setup.real().survivorCount(), setup.real().originalCount());
    }
}