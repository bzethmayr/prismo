package io.github.bzethmayr.prismo.reals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FakeRealsTest implements TestsWithRectR {

    @Test
    void intersectionSurvivors_elementsPresentInAllBases() {
        final RectR basisA = byteSquare();
        final RectR basisB = byteSquare();
        basisA.remove(new byte[]{0});
        basisB.remove(new byte[]{1});
        assertEquals(255, basisA.survivorCount());
        assertEquals(255, basisB.survivorCount());

        final FanR fan = new FanR(FanR.Reduction.INTERSECTION, basisA, basisB);
        assertEquals(255, fan.survivorCount());

        final List<byte[]> survivors = collectSurvivors(fan);
        assertEquals(254, survivors.size());

        for (final byte[] s : survivors) {
            assertNotEquals(0, s[0] & 0xFF);
            assertNotEquals(1, s[0] & 0xFF);
            assertTrue(fan.test(s));
        }
    }

    @Test
    void unionSurvivors_elementsPresentInAnyBasis() {
        final RectR basisA = byteSquare();
        final RectR basisB = byteSquare();
        basisA.remove(new byte[]{0});
        basisB.remove(new byte[]{1});

        final FanR fan = new FanR(FanR.Reduction.UNION, basisA, basisB);
        assertEquals(255, fan.survivorCount());

        final List<byte[]> survivors = collectSurvivors(fan);
        assertEquals(256, survivors.size());

        for (final byte[] s : survivors) {
            assertTrue(fan.test(s));
        }

        assertTrue(fan.test(new byte[]{0}));
        assertTrue(fan.test(new byte[]{1}));
    }

    @Test
    void intersectionSurvivors_fullyExhausted_returnsEmpty() {
        final RectR basisA = byteSquare();
        final RectR basisB = byteSquare();
        for (int i = 0; i < BYTES; i++) {
            basisA.remove(new byte[]{(byte) i});
            basisB.remove(new byte[]{(byte) i});
        }
        assertEquals(0, basisA.survivorCount());
        assertEquals(0, basisB.survivorCount());

        final FanR fan = new FanR(FanR.Reduction.INTERSECTION, basisA, basisB);
        assertEquals(0, fan.survivorCount());

        final Iterator<byte[]> it = fan.survivors().iterator();
        assertFalse(it.hasNext());
    }

    @Test
    void unionSurvivors_overlappingRemoval_deduplicates() {
        final RectR basisA = byteSquare();
        final RectR basisB = byteSquare();
        basisA.remove(new byte[]{0});
        basisA.remove(new byte[]{1});
        basisB.remove(new byte[]{0});
        basisB.remove(new byte[]{2});

        final FanR fan = new FanR(FanR.Reduction.UNION, basisA, basisB);
        assertEquals(254, fan.survivorCount());

        final List<byte[]> survivors = collectSurvivors(fan);
        assertEquals(255, survivors.size());

        final Set<Integer> seen = new HashSet<>();
        for (final byte[] s : survivors) {
            final int v = s[0] & 0xFF;
            assertTrue(v != 0, "Element 0 should not survive (removed in both)");
            assertTrue(seen.add(v), "Duplicate survivor: " + v);
            assertTrue(fan.test(s));
        }
    }

    private static List<byte[]> collectSurvivors(final FanR fan) {
        final List<byte[]> result = new ArrayList<>();
        fan.survivors().forEach(result::add);
        return result;
    }
}
