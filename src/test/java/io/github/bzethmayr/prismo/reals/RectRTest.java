package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.reals.RectR;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class RectRTest {

    private RectR byteSized() {
        return new RectR(16, 16, 1);
    }

    private RectR charSized() {
        return new RectR(256, 256, 2);
    }

    @Test
    void originalCount() {
    }

    @Test
    void test_whenPresent_returnsTrue() {

    }

    @Test
    void test_whenAbsent_returnsFalse() {

    }

    @Test
    void remove() {
    }

    @Test
    void survivorCount() {
    }

    @Test
    void computeRawXs() {
    }

    @Test
    void computeRawYs() {
    }

    @Test
    void survivors_oneBytePath() {
        final RectR underTest = byteSized();
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
        final RectR underTest = charSized();
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