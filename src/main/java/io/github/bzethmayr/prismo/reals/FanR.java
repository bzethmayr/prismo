package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.model.FakeRStats;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.stream.Stream;

public record FanR(LongBinaryOperator reducer, FakeR... basis) implements FakeR {
    public FanR {
        if (basis == null || basis.length == 0) {
            throw new IllegalArgumentException("Provide some basis");
        }
        final long originalCount = basis[0].originalCount();
        if (Stream.of(basis).skip(1).map(FakeR::originalCount).anyMatch(n -> n != originalCount)) {
            throw new IllegalArgumentException("Basis must cover the same space");
        }
    }
    public FanR(FakeR... basis) {
        this(Long::min, basis);
    }

    @Override
    public long originalCount() {
        return basis[0].originalCount();
    }

    @Override
    public boolean test(byte[] putative) {
        return Stream.of(basis).parallel().allMatch(f -> f.test(putative));
    }

    @Override
    public void remove(byte[] present) {
        Stream.of(basis).parallel().forEach(f ->
                f.remove(present));
    }

    @Override
    public long survivorCount() {
        return Stream.of(basis).mapToLong(FakeR::survivorCount)
                .reduce(reducer).orElseThrow(IllegalStateException::new);
    }

    @SuppressWarnings("unchecked") // I checked.
    private static Iterator<byte[]>[] combineSurvivors(final FakeRStats... basis) {
        return (Iterator<byte[]>[]) Stream.of(basis).map(FakeRStats::survivors).map(Iterable::iterator)
                .toArray(Iterator[]::new);
    }

    @Override
    public Iterable<byte[]> survivors() {
        final Iterator<byte[]>[] iterators = combineSurvivors(basis);
        return () -> {
            final AtomicInteger index = new AtomicInteger();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    while (!iterators[index.get()].hasNext()) {
                        index.incrementAndGet();
                    }
                    return index.get() < iterators.length;
                }

                @Override
                public byte[] next() {
                    return iterators[index.get()].next();
                }
            };
        };
    }
}
