package io.github.bzethmayr.prismo.strawman;

import java.util.function.Consumer;

public interface ConcatenatesIntLcg extends Consumer<byte[]> {
    int nextInt();
    @Override
    default void accept(byte[] buffer) {
        int i = 0;
        final int len = buffer.length;

        while (i < len) {
            int x = nextInt();

            // spill bytes in a fixed order; no mixing
            // choose whatever endianness you like, just keep it consistent
            if (i < len) buffer[i++] = (byte) (x);
            if (i < len) buffer[i++] = (byte) (x >>> 8);
            if (i < len) buffer[i++] = (byte) (x >>> 16);
            if (i < len) buffer[i++] = (byte) (x >>> 24);
        }
    }

}
