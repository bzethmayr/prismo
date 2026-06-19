package io.github.bzethmayr.prismo.reals;

import io.github.bzethmayr.prismo.model.FakeRStats;

/**
 * A fake real domain for prismatic (Pokemon-style) RNG testing.
 * Based loosely on the notion of the relative emptiness of countable vs uncountable sets.
 */
public interface FakeReals extends FakeRStats {
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
}
