package io.github.bzethmayr.prismo.reals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static net.zethmayr.fungu.core.ExceptionFactory.becauseIllegal;

public final class RectR implements FakeR {
    private final int width;
    private final int height;
    private final long originalCount;
    private final BitSet removed;
    private final int sampleSize;
    private final int xBits;
    private final int yBits;

    static int needBits(final int length) {
        return length == 0
                ? 1
                : 32 - Integer.numberOfLeadingZeros(length);
    }

    static int needBytes(final int needBits) {
        return needBits == 0
                ? 1
                : (needBits + 7) >>> 3; // ceil(required / 8)
    }

    public RectR(int width, int height) {
        this(width, height, 4);
    }

    public RectR(int width, int height, int sampleSize) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Positive dimensions required");
        }
        xBits = needBits(width - 1);
        yBits = needBits(height - 1);
        if (xBits + yBits > 32) {
            throw new IllegalArgumentException("Non-overlapping dimensions required");
        }
        this.width = width;
        this.height = height;
        this.originalCount = (long) width * height;
        this.removed = new BitSet((int) originalCount);
        this.sampleSize = sampleSize;
    }

    @Override
    public long originalCount() {
        return originalCount;
    }

    @Override
    public boolean test(byte[] putative) {
        int idx = indexOf(putative);
        return !removed.get(idx);
    }

    @Override
    public void remove(byte[] present) {
        int idx = indexOf(present);
        removed.set(idx);
    }

    @Override
    public long survivorCount() {
        return originalCount - removed.cardinality();
    }

    int[] collectSurvivors() {
        final ArrayList<Integer> survivors = new ArrayList<>();
        int survived = -1;
        while ((survived = removed.nextClearBit(survived + 1)) < originalCount) {
            survivors.add(survived);
        }
        return survivors.stream().mapToInt(n -> n).toArray();
    }

    @Override
    public Iterable<byte[]> survivors() {
        final AtomicInteger index = new AtomicInteger();
        final int[] survivors = collectSurvivors();
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return index.get() < survivors.length;
            }

            @Override
            public byte[] next() {
                if (!hasNext()) throw new NoSuchElementException();
                final int survivor = survivors[index.getAndIncrement()];
                return sampleFor(survivor);
            }
        };
    }

    // --- Fully correct reversible geometry ---
    int checkIndex(int index) {
        if (index < 0 || index > originalCount) {
            throw becauseIllegal("Invalid index %s", index);
        }
        return index;
    }

    long flattenSample(final byte[] bytes) {
        long v = 0;
        for (int i = 0; i < sampleSize; i++) {
            v |= ((long) bytes[i] & 0xFF) << (8 * i);
        }
        return v;
    }

    int indexOf(byte[] bytes) {
        // Interpret the sample as a single little-endian integer
        long v = flattenSample(bytes);

        // Extract X and Y from bitfield
        int x = extractX(v);
        int y = extractY(v);

        // Map into rectangle
        return checkIndex(y * width + x);
    }

    byte[] sampleFor(final int index) {
        // Compute x and y
        int x = index % width;
        int y = index / width;

        // Pack into bitfield
        long v = ((long) y << xBits) | (long) x;

        // Emit little-endian sample
        byte[] out = new byte[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            out[i] = (byte) (v >>> (8 * i));
        }
        return out;
    }

    int extractX(long flat) {
        return (int) (flat & ((1L << xBits) - 1)) % width;
    }

    int extractX(byte[] bytes) {
        return extractX(flattenSample(bytes));
    }

    int extractY(long flat) {
        return (int) ((flat >>> xBits) & ((1L << yBits) - 1)) % height;
    }

    int extractY(byte[] bytes) {
        return extractY(flattenSample(bytes));
    }
}

