package io.github.bzethmayr.prismo.strawman;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter implements ConcatenatesIntLcg {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public int nextInt() {
        final int count = counter.getAndIncrement();
        return (count << 24) + count;
    }
}
