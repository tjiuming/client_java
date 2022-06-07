package io.prometheus.client.exporter.common.formatter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;

class DoubleUtil {
    private static Field _count;
    private static Field _value;
    private static ThreadLocal<StringBuilder> cache;
    private static boolean initialized = true;

    static {
        try {
            Class<?> superKlass = StringBuilder.class.getSuperclass();
            _count = superKlass.getDeclaredField("count");
            _value = superKlass.getDeclaredField("value");

            _count.setAccessible(true);
            _value.setAccessible(true);

            cache = new ThreadLocal<StringBuilder>();
        } catch (Exception e) {
            initialized = false;
        }
    }

    /**
     * To prevent generate string objects.
     *
     * @param writer
     * @param v
     * @throws IOException
     */
    static void append(Writer writer, double v) throws IOException {
        if (v == Double.POSITIVE_INFINITY) {
            writer.write("+Inf");
            return;
        } else if (v == Double.NEGATIVE_INFINITY) {
            writer.write("-Inf");
            return;
        }

        if (initialized) {
            StringBuilder builder = cache.get();
            try {
                try {
                    if (builder == null) {
                        builder = new StringBuilder(32);
                        cache.set(builder);
                    }

                    builder.append(v);

                    int count = _count.getInt(builder);
                    byte[] value = (byte[]) _value.get(builder);

                    for (int idx = 0; idx < count; idx++) {
                        writer.write(value[idx]);
                    }

                } finally {
                    _count.set(builder, 0);
                }

            } catch (IllegalAccessException e) {
              // ignore
            }
        } else {
            writer.write(Double.toString(v));
        }
    }
}
