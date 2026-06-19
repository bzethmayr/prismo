package io.github.bzethmayr.prismo;

import io.github.bzethmayr.prismo.cli.SampleSetupParser;
import io.github.bzethmayr.prismo.strawman.Counter;
import io.github.bzethmayr.prismo.strawman.RANDU;
import io.github.bzethmayr.prismo.strawman.TerribleLCG;

import java.security.SecureRandom;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        final SampleSetupParser parser = new SampleSetupParser();
        final int badSeed = (int) System.nanoTime() + (int) System.currentTimeMillis();
        parser.addSource("SecureRandom", new SecureRandom()::nextBytes);
        parser.addSource("Random", new Random()::nextBytes);
        parser.addSource("Counter", new Counter());
        parser.addSource("RANDU", new RANDU(badSeed % 128));
        parser.addSource("Terrible", new TerribleLCG(badSeed));

        final Prismo.SampleSetup setup = parser.parseArgs(args);
        Prismo.runPrismaticTest(setup);

        System.out.printf("Iterations: %d%n", setup.iterations());
        System.out.printf("Sample size: %d%n", setup.sampleSize());
        System.out.printf("Survivors: %d / %d%n",
                setup.real().survivorCount(), setup.real().originalCount());
    }
}