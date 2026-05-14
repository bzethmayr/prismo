package io.github.bzethmayr.prismo.reals;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.stream.IntStream;

import static io.github.bzethmayr.prismo.reals.RectR.needBits;
import static io.github.bzethmayr.prismo.reals.RectR.needBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

class RectRTest implements TestsWithRectR {

    @Test
    void needBits_returnsMinimumBitCount() {

        assertEquals(1, needBits(0));
        assertEquals(1, needBits(1));
        assertEquals(2, needBits(2));
        assertEquals(2, needBits(3));
        assertEquals(3, needBits(4));
        assertEquals(3, needBits(5));
        assertEquals(3, needBits(6));
        assertEquals(3, needBits(7));
        assertEquals(4, needBits(8));
    }

    @Test
    void needBytes_returnsMinimumByteCount() {

        assertEquals(1, needBytes(0));
        assertEquals(1, needBytes(1));
        assertEquals(1, needBytes(2));
        assertEquals(1, needBytes(7));
        assertEquals(1, needBytes(8));
        assertEquals(2, needBytes(9));
        assertEquals(2, needBytes(10));
        assertEquals(2, needBytes(16));
        assertEquals(3, needBytes(17));
        assertEquals(3, needBytes(24));
    }

    @Test
    void squareBytes_extractX_givenThresholds_returnsExpected() {
        final RectR underTest = byteSquare();
        final byte[] sample = new byte[1];

        assertEquals(0, underTest.extractX(sample));
        sample[0] = 1;
        assertEquals(1, underTest.extractX(sample));
        sample[0] = 15;
        assertEquals(15, underTest.extractX(sample));
        sample[0] = 16;
        assertEquals(0, underTest.extractX(sample));
        sample[0] = 31;
        assertEquals(15, underTest.extractX(sample));
        sample[0] = 32;
        assertEquals(0, underTest.extractX(sample));
    }

    @Test
    void squareBytes_extractY_givenThresholds_returnsExpected() {
        final RectR underTest = byteSquare();
        final byte[] sample = new byte[1];

        assertEquals(0, underTest.extractY(sample));
        sample[0] = 1;
        assertEquals(0, underTest.extractY(sample));
        sample[0] = 15;
        assertEquals(0, underTest.extractY(sample));
        sample[0] = 16;
        assertEquals(1, underTest.extractY(sample));
        sample[0] = 31;
        assertEquals(1, underTest.extractY(sample));
        sample[0] = 32;
        assertEquals(2, underTest.extractY(sample));
    }

    @Test
    void squareBytes_collectSurvivors_whenNoneRemoved_returnsExactSurvivors() {
        final RectR underTest = byteSquare();

        final int[] survivors = underTest.collectSurvivors();

        assertEquals(BYTES, survivors.length);
        IntStream.range(0, BYTES).forEach(n -> assertEquals(n, survivors[n]));
    }

    @Test
    void squareBytes_indexIsIdentity() {
        final RectR underTest = byteSquare();
        final byte[] sample = new byte[1];

        IntStream.range(0, BYTES).forEach(n -> {
            sample[0] = (byte) n;
            assertEquals(n, underTest.indexOf(sample));
        });
    }

    @Test
    void rectBytes_indexIsIdentity() {
        final RectR underTest = byteRect();
        final byte[] sample = new byte[1];

        IntStream.range(0, BYTES).forEach(n -> {
            sample[0] = (byte) n;
            assertEquals(n, underTest.indexOf(sample));
        });
    }

    @Test
    void squareBytes_identityIsIndex() {
        final RectR underTest = byteSquare();

        IntStream.range(0, BYTES).forEach(n -> {
            assertArrayEquals(new byte[]{(byte) n}, underTest.sampleFor(n));
        });
    }

    @Test
    void rectBytes_identityIsIndex() {
        final RectR underTest = byteRect();

        IntStream.range(0, BYTES).forEach(n -> {
            assertArrayEquals(new byte[]{(byte) n}, underTest.sampleFor(n));
        });
    }

    @Test
    void squareChars_identityIsIndex() {
        final RectR underTest = charSquare();
        final byte[] sample = new byte[2];

        IntStream.range(0, CHARS).forEach(n -> {
            sample[1] = (byte) (n >> 8);
            sample[0] = (byte) n;
            assertEquals(n, underTest.indexOf(sample));
        });
    }

    @Test
    void square_survivors_oneBytePath() {
        final RectR underTest = byteSquare();
        assertEquals(256, underTest.survivorCount());

        final Iterator<byte[]> iter = underTest.survivors().iterator();
        int count = 0;
        while (iter.hasNext()) {
            count++;
            byte[] preimage = iter.next();
            assertNotNull(preimage);
            underTest.remove(preimage);
            assertThat(count, lessThanOrEqualTo(256));
        }

        assertEquals(256, count);
        assertEquals(0, underTest.survivorCount());
    }

    @Test
    void rect_survivors_oneBytePath() {
        final RectR underTest = byteRect();
        assertEquals(256, underTest.survivorCount());

        final Iterator<byte[]> iter = underTest.survivors().iterator();
        int count = 0;
        while (iter.hasNext()) {
            count++;
            byte[] preimage = iter.next();
            assertNotNull(preimage);
            underTest.remove(preimage);
        }

        assertEquals(256, count);
        assertEquals(0, underTest.survivorCount());
    }

    @Test
    void survivors_multiBytePath() {
        final RectR underTest = charSquare();
        assertEquals(65536, underTest.survivorCount());

        final Iterator<byte[]> iter = underTest.survivors().iterator();
        int count = 0;
        while (iter.hasNext()) {
            count++;
            byte[] preimage = iter.next();
            assertNotNull(preimage);
            underTest.remove(preimage);
        }

        assertEquals(65536, count);
        assertEquals(0, underTest.survivorCount());
    }
}