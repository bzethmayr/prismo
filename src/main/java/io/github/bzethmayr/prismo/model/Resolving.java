package io.github.bzethmayr.prismo.model;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Resolving {

    private static final Map<String, Class<?>> KNOWN_TYPES = new ConcurrentHashMap<>();

    public static Class<?> getKnownType(final String name) {
        return KNOWN_TYPES.get(name);
    }

    public static void hintType(final String name, final Class<?> hint) {
        KNOWN_TYPES.putIfAbsent(name, hint);
    }

    public interface IntResolver {
        int resolve(final Map<String, Object> params);
    }

    public interface LongResolver {
        long resolve(final Map<String, Object> params);
    }

    public interface DoubleResolver {
        double resolve(final Map<String, Object> params);
    }

    public interface GenericResolver<T> {
        T resolve(final Map<String, Object> params);
    }

    public static String paramString(final Map<String, Object> params, final String name) {
        return (String) params.get(name);
    }

    public static String paramString(final Map<String, Object> params, final String name, final String defaultValue) {
        return Optional.ofNullable(paramString(params, name))
                .orElse(defaultValue);
    }

    public static GenericResolver<String> registeredParamString(final String name, final String defaultValue) {
        KNOWN_TYPES.putIfAbsent(name, String.class);
        return m -> paramString(m, name, defaultValue);
    }

    public static GenericResolver<String> registeredParamString(final String name) {
        return registeredParamString(name, null);
    }

    public static long paramLong(final Map<String, Object> params, final String name) {
        return paramLong(params, name, 0);
    }

    public static long paramLong(final Map<String, Object> params, final String name, final long defaultValue) {
        final Object value = params.get(name);
        if (value != null) {
            return (long) value;
        }
        return defaultValue;
    }

    public static LongResolver registeredParamLong(final String name, final long defaultValue) {
        KNOWN_TYPES.putIfAbsent(name, long.class);
        return m -> paramLong(m, name, defaultValue);
    }

    public static LongResolver registeredParamLong(final String name) {
        return registeredParamLong(name, 0);
    }

    public static int paramInt(final Map<String, Object> params, final String name) {
        return paramInt(params, name, 0);
    }

    public static int paramInt(final Map<String, Object> params, final String name, final int defaultValue) {
        final Object value = params.get(name);
        if (value != null) {
            return (int) value;
        }
        return defaultValue;
    }

    public static IntResolver registeredParamInt(final String name, final int defaultValue) {
        KNOWN_TYPES.put(name, int.class);
        return m -> paramInt(m, name, defaultValue);
    }

    public static IntResolver registeredParamInt(final String name) {
        return registeredParamInt(name, 0);
    }

    public static double paramDouble(final Map<String, Object> params, final String name, final double defaultValue) {
        final Object value = params.get(name);
        if (value != null) {
            return (double) value;
        }
        return defaultValue;
    }

    public static DoubleResolver registeredParamDouble(final String name, final double defaultValue) {
        KNOWN_TYPES.put(name, double.class);
        return m -> paramDouble(m, name, defaultValue);
    }

    public static DoubleResolver registeredParamDouble(final String name) {
        return registeredParamDouble(name, 0);
    }
}
