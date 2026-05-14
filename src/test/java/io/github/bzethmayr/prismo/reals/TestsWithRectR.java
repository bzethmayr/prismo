package io.github.bzethmayr.prismo.reals;

public interface TestsWithRectR {
    int BYTES = 256;
    int CHARS = 65536;
    int BYTE_SQRT = 16;
    int CHAR_SQRT = 256;
    int BYTE_THIN = 8;
    int BYTE_WIDE = 32;
    int CHAR_THIN = 128;
    int CHAR_WIDE = 512;

    default RectR byteSquare() {
        return new RectR(BYTE_SQRT, BYTE_SQRT, 1);
    }

    default RectR charSquare() {
        return new RectR(CHAR_SQRT, CHAR_SQRT, 2);
    }

    default RectR byteRect() {
        return new RectR(BYTE_WIDE, BYTE_THIN, 1);
    }

    default RectR charRect() {
        return new RectR(CHAR_WIDE, CHAR_THIN, 2);
    }
}
