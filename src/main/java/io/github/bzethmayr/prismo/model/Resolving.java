package io.github.bzethmayr.prismo.model;

import java.util.Map;

public final class Resolving {
    public static String paramString(final Map<String, Object> params, final String name) {
        return (String) params.get(name);
    }

    public static long paramLong(final Map<String, Object> params, final String name) {
        final Object value = params.get(name);
        if (value != null) {
            return (long) value;
        }
        return 0;
    }

    public static int paramInt(final Map<String, Object> params, final String name) {
        final Object value = params.get(name);
        if (value != null) {
            return (int) value;
        }
        return 0;
    }
}
