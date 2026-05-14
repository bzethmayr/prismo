package io.github.bzethmayr.prismo.strawman;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter implements ConcatenatesIntLcg {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public int nextInt() {
        return counter.getAndIncrement();
    }
}
