package io.github.bzethmayr.prismo.reals;

public record MismatchR(long originalCount, FakeR mismatched, double tolerance) implements FakeR {

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
