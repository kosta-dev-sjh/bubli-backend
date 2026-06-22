package com.bubli.global.trace;

public class TraceIdHolder {

    private static final ThreadLocal<String> traceId = new ThreadLocal<>();

    public static void set(String id) {
        traceId.set(id);
    }

    public static String get() {
        return traceId.get();
    }

    public static void clear() {
        traceId.remove();
    }
}
