package io.github.bzethmayr.prismo.strawman;

public class TerribleLCG implements ConcatenatesIntLcg {
    private int x;

    public TerribleLCG(final int seed) {
        x = seed;
    }

    public int nextInt() {
        x = (x * 1103515245 + 12345);
        return x;
    }
}
