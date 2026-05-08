package io.github.bzethmayr.prismo.strawman;

/**
 * A Java implementation of the
 */
import java.util.function.Consumer;

public final class RANDU implements ConcatenatesIntLcg {
    // 31-bit state, must stay in [1, 2^31 - 1]
    private int state;

    public RANDU(int seed) {
        seed = (seed << 1) + 1;
        this.state = seed & 0x7FFFFFFF; // enforce 31-bit
    }

    // RANDU core: X_{n+1} = (65539 * X_n) mod 2^31
    public int nextInt() {
        long x = (long) state * 65539L;
        state = (int) (x & 0x7FFFFFFF); // mod 2^31
        return state;
    }
}