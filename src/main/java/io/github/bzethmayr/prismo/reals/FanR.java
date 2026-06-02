package io.github.bzethmayr.prismo.reals;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.LongBinaryOperator;
import java.util.stream.Stream;

public record FanR(Reduction reduction, FakeR... basis) implements FakeR {

    public enum Reduction {
        INTERSECTION(Long::min),
        UNION(Long::max);

        private final LongBinaryOperator reducer;

        Reduction(final LongBinaryOperator reducer) {
            this.reducer = reducer;
        }

        public LongBinaryOperator reducer() {
            return reducer;
        }
    }

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
        this(Reduction.INTERSECTION, basis);
    }

    @Override
    public long originalCount() {
        return basis[0].originalCount();
    }

    @Override
    public boolean test(byte[] putative) {
        return switch (reduction) {
            case INTERSECTION -> Stream.of(basis).parallel().allMatch(f -> f.test(putative));
            case UNION -> Stream.of(basis).parallel().anyMatch(f -> f.test(putative));
        };
    }

    @Override
    public void remove(byte[] present) {
        Stream.of(basis).parallel().forEach(f ->
                f.remove(present));
    }

    @Override
    public long survivorCount() {
        return Stream.of(basis).mapToLong(FakeR::survivorCount)
                .reduce(reduction.reducer()).orElseThrow(IllegalStateException::new);
    }

    @Override
    public Iterable<byte[]> survivors() {
        return switch (reduction) {
            case INTERSECTION -> intersectionSurvivors();
            case UNION -> unionSurvivors();
        };
    }

    private Iterable<byte[]> intersectionSurvivors() {
        final Iterable<byte[]> base = basis[0].survivors();
        return () -> {
            final Iterator<byte[]> inner = base.iterator();
            return new Iterator<>() {
                private byte[] next;

                @Override
                public boolean hasNext() {
                    if (next != null) return true;
                    while (inner.hasNext()) {
                        byte[] candidate = inner.next();
                        if (FanR.this.test(candidate)) {
                            next = candidate;
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public byte[] next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    byte[] result = next;
                    next = null;
                    return result;
                }
            };
        };
    }

    private Iterable<byte[]> unionSurvivors() {
        final Set<ByteBuffer> seen = new HashSet<>();
        for (final FakeR f : basis) {
            for (final byte[] arr : f.survivors()) {
                seen.add(ByteBuffer.wrap(arr.clone()));
            }
        }
        return () -> {
            final Iterator<ByteBuffer> it = seen.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public byte[] next() {
                    return it.next().array();
                }
            };
        };
    }
}
