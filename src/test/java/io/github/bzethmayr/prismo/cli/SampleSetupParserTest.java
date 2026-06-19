package io.github.bzethmayr.prismo.cli;

import io.github.bzethmayr.prismo.Prismo;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class SampleSetupParserTest {

    @Test
    void parseCliExpressionAndRunTest() {
        final SampleSetupParser parser = new SampleSetupParser();
        parser.addSource("SecureRandom", new SecureRandom()::nextBytes);

        final Prismo.SampleSetup setup = parser.parseArgs(
                "-s", "SecureRandom",
                "-n", "1",
                "-r", "rect", "width=16", "height=16",
                "-R",
                "-g",
                "-x",
                "-l", "left",
                "-a", "count", "to=left",
                "-A"
        );

        final long originalCount = setup.real().originalCount();
        assertEquals(256, originalCount);

        Prismo.runPrismaticTest(setup);

        assertTrue(setup.real().survivorCount() < originalCount);
        assertFalse(parser.getLongCollectorResults().get("left").isEmpty());
    }
}
