package io.github.bzethmayr.prismo.reals;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FakeRealsDefinitionTest {

    @Test
    void rect_withParams_buildsRectR() {
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "rect",
                Map.of("width", 16, "height", 16),
                null
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(RectR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void rect_withSampleSize_buildsRectR() {
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "rect",
                Map.of("width", 16, "height", 8, "sampleSize", 3),
                null
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(RectR.class, built);
        assertEquals(128, built.originalCount());
    }

    @Test
    void fan_withTwoChildren_buildsFanR() {
        final FakeRealsDefinition child1 = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition child2 = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "fan", null, List.of(child1, child2)
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(FanR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void fan_withUnionReduction_buildsFanR() {
        final FakeRealsDefinition child1 = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition child2 = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "fan", Map.of("reduction", "UNION"), List.of(child1, child2)
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(FanR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void mix_withIdentity_buildsMixR() {
        final FakeRealsDefinition child = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "mix", Map.of("mix", "identity"), List.of(child)
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(MixR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void mix_withDefaultIdentity_buildsMixR() {
        final FakeRealsDefinition child = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "mix", null, List.of(child)
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(MixR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void mismatch_withChild_buildsMismatchR() {
        final FakeRealsDefinition child = new FakeRealsDefinition(
                "rect", Map.of("width", 16, "height", 16), null
        );
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "mismatch", Map.of("originalCount", 256L), List.of(child)
        );
        final TaggedFakeReals built = def.build();
        assertInstanceOf(MismatchR.class, built);
        assertEquals(256, built.originalCount());
    }

    @Test
    void mismatch_withTolerance_acceptsCloseDomains() {
        final FakeRealsDefinition child = new FakeRealsDefinition(
                "rect", Map.of("width", 17, "height", 15), null
        );
        final Map<String, Object> params = new HashMap<>();
        params.put("originalCount", 256L);
        params.put("tolerance", 1.1);
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "mismatch", params, List.of(child)
        );
        assertDoesNotThrow(def::build);
    }

    @Test
    void unknownType_throws() {
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "nonexistent", null, null
        );
        assertThrows(IllegalStateException.class, def::build);
    }

    @Test
    void nullParams_usesEmptyCollections() {
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "rect", null, null
        );
        assertNotNull(def.params());
        assertTrue(def.params().isEmpty());
        assertNotNull(def.children());
        assertTrue(def.children().isEmpty());
    }

    @Test
    void tag_preservedThroughBuild() {
        final FakeRealsDefinition def = new FakeRealsDefinition(
                "rect",
                Map.of("width", 16, "height", 16),
                null,
                "myRect"
        );
        final TaggedFakeReals built = def.build();
        assertEquals("myRect", built.tag());
    }
}
