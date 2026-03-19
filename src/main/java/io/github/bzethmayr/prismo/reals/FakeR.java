package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.model.FakeRStats;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A fake real domain for prismatic (Pokemon-style) RNG testing.
 * Based loosely on the notion of the relative emptiness of countable vs uncountable sets.
 */
public interface FakeR extends FakeRStats {
    /**
     * Is this a surviving element?
     * @param putative some RNG output.
     * @return whether this real domain still contains this element.
     */
    boolean test(byte[] putative);

    /**
     * Removes an element from the domain.
     * @param present the element to remove.
     */
    void remove(byte[] present);

    /**
     * Mixes (vs hashes) bits.
     * Sorting all the 1-bits to one side counts as hashing. Please do not.
     * @param basis the fake real domain to pre-mix for.
     * @param mix a bit-mixing function over equal-sized byte-arrays.
     */
    record MixR(FakeR basis, Function<byte[], byte[]> mix) implements FakeR {

        @Override
        public long originalCount() {
            return basis.originalCount();
        }

        @Override
        public boolean test(byte[] putative) {
            return basis.test(mix.apply(putative));
        }

        @Override
        public void remove(byte[] present) {
            basis.remove(mix.apply(present));
        }

        @Override
        public long survivorCount() {
            return basis.survivorCount();
        }

        @Override
        public Iterable<byte[]> survivors() {
            return basis.survivors();
        }
    }

    record FanR(FakeR... basis) implements FakeR {
        public FanR {
            if (basis == null || basis.length == 0) {
                throw new IllegalArgumentException("Provide some basis");
            }
            final long originalCount = basis[0].originalCount();
            if (Stream.of(basis).skip(1).map(FakeR::originalCount).anyMatch(n -> n != originalCount)) {
                throw new IllegalArgumentException("Basis must cover the same space");
            }
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
            Stream.of(basis).parallel().forEach(f -> f.remove(present));
        }

        @Override
        public long survivorCount() {
            return Stream.of(basis).mapToLong(FakeR::survivorCount).min().orElseThrow(IllegalStateException::new);
        }

        @SuppressWarnings("unchecked") // I checked.
        private static Iterator<byte[]>[] combineSurvivors(final FakeRStats... basis) {
            return (Iterator<byte[]>[]) Stream.of(basis).map(FakeRStats::survivors).map(Iterable::iterator).toArray(Iterator[]::new);
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

    record MismatchR(long originalCount, FakeR mismatched, double tolerance) implements FakeR {

        public MismatchR(long originalCount, FakeR mismatched) {
            this(originalCount, mismatched, 1.05d);
        }

        public MismatchR {
            if (originalCount <= 0) {
                throw new IllegalArgumentException("Declared domain must be positive");
            }

            long actual = mismatched.originalCount();
            if (actual <= 0) {
                throw new IllegalArgumentException("Underlying FakeR has invalid domain");
            }

            // Distortion ratio: how far off the declared domain is
            double ratio = Math.max(
                    (double) originalCount / actual,
                    (double) actual / originalCount
            );

            // If ratio > tolerance, reject
            if (ratio > tolerance) {
                throw new IllegalArgumentException(
                        "Domain mismatch too large: declared=" + originalCount +
                                ", actual=" + actual +
                                ", ratio=" + ratio +
                                ", tolerance=" + tolerance
                );
            }
        }

        @Override
        public boolean test(byte[] putative) {
            return mismatched.test(putative);
        }

        @Override
        public void remove(byte[] present) {
            mismatched.remove(present);
        }

        @Override
        public long survivorCount() {
            return mismatched.survivorCount();
        }

        @Override
        public Iterable<byte[]> survivors() {
            return mismatched.survivors();
        }
    }
}
