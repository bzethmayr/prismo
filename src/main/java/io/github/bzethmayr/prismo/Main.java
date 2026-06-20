package io.github.bzethmayr.prismo;

import io.github.bzethmayr.prismo.cli.SampleSetupParser;
import io.github.bzethmayr.prismo.model.IterationVariable;
import io.github.bzethmayr.prismo.strawman.Counter;
import io.github.bzethmayr.prismo.strawman.RANDU;
import io.github.bzethmayr.prismo.strawman.TerribleLCG;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        final SampleSetupParser parser = new SampleSetupParser();
        final int badSeed = (int) System.nanoTime() + (int) System.currentTimeMillis();
        final Map<String, Integer> seeds = new HashMap<>();
        seeds.put("RANDU", badSeed % 128);
        seeds.put("Terrible", badSeed);
        parser.addSource("SecureRandom", new SecureRandom()::nextBytes);
        parser.addSource("Random", new Random()::nextBytes);
        parser.addSource("Counter", new Counter());
        parser.addSource("RANDU", new RANDU(seeds.get("RANDU")));
        parser.addSource("Terrible", new TerribleLCG(seeds.get("Terrible")));

        final Prismo.SampleSetup setup = parser.parseArgs(args);
        Prismo.runPrismaticTest(setup);

        final String randomName = setup.randomName();
        final Integer seed = seeds.get(randomName);
        if (seed != null) {
            System.out.printf("%s seed was %d%n", randomName, seed);
        }
        System.out.printf("Iterations: %d%n", setup.iterations());
        System.out.printf("Sample size: %d%n", setup.sampleSize());
        System.out.printf("Survivors: %d / %d%n",
                setup.real().survivorCount(), setup.real().originalCount());
        final Map<String, List<IterationVariable<Long>>> longsCollected = setup.longs();
        final Map<String, List<IterationVariable<Double>>> doublesCollected = setup.doubles();
        if (longsCollected.size() + doublesCollected.size() > 0) {
            System.out.println("Collected:");
            for (String key : longsCollected.keySet()) {
                System.out.printf("%s = %s%n", key, longsCollected.get(key));
            }
            for (String key : doublesCollected.keySet()) {
                System.out.printf("%s = %s%n", key, doublesCollected.get(key));
            }
        }
    }
}