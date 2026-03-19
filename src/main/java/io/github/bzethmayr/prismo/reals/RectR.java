package io.github.bzethmayr.prismo.reals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class RectR implements FakeR {
    private final int width;
    private final int height;
    private final long originalCount;
    private final BitSet removed;
    private final int sampleSize;
    private final int xBits;
    private final int yBits;
    private final int xBytes;
    private final int yBytes;

    private static int needBits(final int length) {
        return 32 - Integer.numberOfLeadingZeros(length - 1);
    }

    private static int needBytes(final int needBits) {
        return (needBits + 7) >>> 3; // ceil(required / 8)
    }

    public RectR(int width, int height) {
        this(width, height, 4);
    }

    public RectR(int width, int height, int sampleSize) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Positive dimensions required");
        }
        xBits = needBits(width);
        yBits = needBits(height);
        if (xBits + yBits > 32) {
            throw new IllegalArgumentException("Non-overlapping dimensions required");
        }
        xBytes = needBytes(xBits);
        yBytes = needBytes(yBits);
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

    private int[] collectSurvivors() {
        final ArrayList<Integer> survivors = new ArrayList<>();
        int survived = -1;
        while ((survived = removed.nextClearBit(survived + 1)) < originalCount) {
            survivors.add(survived);
        }
        return survivors.stream().mapToInt(n -> n).toArray();
    }

    static int[] computeRawXs(int x, int width, int xMask) {
        int maxN = (xMask - x) / width;
        int[] out = new int[maxN + 1];
        for (int n = 0; n <= maxN; n++) {
            out[n] = x + n * width;
        }
        return out;
    }

    static int[] computeRawYs(int y, int height, int yMask) {
        int maxN = (yMask - y) / height;
        int[] out = new int[maxN + 1];
        for (int n = 0; n <= maxN; n++) {
            out[n] = y + n * height;
        }
        return out;
    }

    @Override
    public Iterable<byte[]> survivors() {
        if (xBits + yBits <= 8) {
            return () -> {
                final int[] survivors = collectSurvivors();
                final int survivorCount = survivors.length;
                final AtomicInteger survivor = new AtomicInteger();

                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return survivor.get() < survivorCount;
                    }

                    @Override
                    public byte[] next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        final int index = survivors[survivor.getAndIncrement()];
                        final int x = index % width;
                        final int y = index / width;
                        final int raw = (y << yBits) | x;
                        return new byte[]{(byte) raw};
                    }
                };
            };
        }
        final int middleBytes = sampleSize - (xBytes + yBytes);
        final int midMax = 1 << (8 * middleBytes);
        int xMask = (1 << xBits) - 1;
        int yMask = (1 << yBits) - 1;
        return () -> {
            // BitSet removed
            // int sampleSize
            final int[] survivors = collectSurvivors();
            final int survivorCount = survivors.length;
            final AtomicInteger survivor = new AtomicInteger();
            final AtomicInteger rawX = new AtomicInteger();
            final AtomicInteger rawY = new AtomicInteger();
            final AtomicInteger freeMiddle = new AtomicInteger();
            final AtomicReference<int[]> rawXs = new AtomicReference<>();
            final AtomicReference<int[]> rawYs = new AtomicReference<>();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return freeMiddle.get() < midMax &&
                            survivor.get() < survivorCount;
                }

                @Override
                public byte[] next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    // Initialize rawXs/rawYs lazily
                    if (rawXs.get() == null) {
                        int idx = survivors[survivor.get()];
                        int x = idx % width;
                        int y = idx / width;

                        rawXs.set(computeRawXs(x, width, xMask));
                        rawYs.set(computeRawYs(y, height, yMask));

                        rawX.set(0);
                        rawY.set(0);
                    }
                    byte[] out = new byte[sampleSize];

                    // Fill Y bytes
                    int rawYVal = rawYs.get()[rawY.get()];
                    for (int i = yBytes - 1; i >= 0; i--) {
                        out[i] = (byte)(rawYVal & 0xFF);
                        rawYVal >>>= 8;
                    }

                    // Fill X bytes
                    int rawXVal = rawXs.get()[rawX.get()];
                    int ptr = sampleSize - 1;
                    for (int i = 0; i < xBytes; i++) {
                        out[ptr--] = (byte)(rawXVal & 0xFF);
                        rawXVal >>>= 8;
                    }

                    // Fill middle bytes
                    int m = freeMiddle.get();
                    for (int i = ptr; i >= yBytes; i--) {
                        out[i] = (byte)(m & 0xFF);
                        m >>>= 8;
                    }

                    // Advance counters in semantic-first order
                    if (rawX.incrementAndGet() < rawXs.get().length) return out;

                    rawX.set(0);
                    if (rawY.incrementAndGet() < rawYs.get().length) return out;

                    rawY.set(0);
                    rawXs.set(null); // force re-init for next survivor
                    survivor.incrementAndGet();

                    if (survivor.get() < survivorCount) return out;

                    // All survivors done → advance middle
                    survivor.set(0);
                    freeMiddle.incrementAndGet();

                    return out;
                }
            };
        };
    }

    private int indexOf(byte[] bytes) {
        // Interpret bytes as two integers (x,y)
        // Then fold into a rectangle index
        int x = extractX(bytes);
        int y = extractY(bytes);
        return y * width + x;
    }

    private int extractX(byte[] bytes) {
        int ptr = bytes.length;
        int x = 0;

        // Pull bytes from the end
        for (int i = 0; i < xBytes; i++) {
            x = (x << 8) | (bytes[--ptr] & 0xFF);
        }
        // Mask off only the required bits
        if (xBits < 32) {
            x &= (1 << xBits) - 1;
        }

        // Final mapping into the geometry
        return x % width;
    }

    private int extractY(byte[] bytes) {
        final int needBits = needBits(height);
        final int needBytes = needBytes(needBits);

        int ptr = 0;
        int y = 0;

        // Pull bytes from the beginning
        for (int i = 0; i < needBytes; i++) {
            y = (y << 8) | (bytes[ptr++] & 0xFF);
        }
        // Mask excess
        if (needBits < 32) {
            y >>>= (8 * needBytes - yBits);
            y &= (1 << yBits) - 1;
        }

        return y % height;
    }
}

