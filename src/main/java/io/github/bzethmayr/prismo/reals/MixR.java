package io.github.bzethmayr.prismo.reals;

import java.util.function.Function;

/**
 * Mixes (vs hashes) bits.
 * Sorting all the 1-bits to one side counts as hashing. Please do not.
 *
 * @param basis the fake real domain to pre-mix for.
 * @param mix   a bit-mixing function over equal-sized byte-arrays.
 */
public record MixR(String tag, FakeReals basis, Function<byte[], byte[]> mix) implements TaggedFakeReals {

    public MixR(final FakeReals basis, final Function<byte[], byte[]> mix) {
        this(null, basis, mix);
    }

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
