package com.dddumpling.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/** Grow-only per-install counters: merging an offline reward twice never awards it twice. */
final class ProgressData {
    static final int MAX_BYTES = 512 * 1024, MAX_ENTRIES = 4096;
    private static final int MAGIC = 0x44445047, VERSION = 1;
    private final TreeMap<String, Long> values = new TreeMap<>();

    int size() { return values.size(); }

    void increment(String replica, String metric, long amount) {
        if (amount < 0 || !replica.matches("[a-zA-Z0-9_-]{1,64}")
                || !metric.matches("[a-z0-9_]{1,64}")) throw new IllegalArgumentException();
        String key = "c:" + replica + ":" + metric;
        putMax(key, add(value(key), amount));
    }

    void maximum(String metric, long value) { putMax("m:" + metric, value); }
    long maximum(String metric) { return value("m:" + metric); }
    long total(String metric) {
        long total = 0;
        for (Map.Entry<String, Long> e : values.entrySet())
            if (e.getKey().startsWith("c:") && e.getKey().endsWith(":" + metric))
                total = add(total, e.getValue());
        return total;
    }
    void legacy(String metric, long value) { putMax("c:legacy:" + metric, value); }
    private long value(String key) { Long v = values.get(key); return v == null ? 0 : v; }
    private void putMax(String key, long value) {
        if (value <= value(key)) return;
        if (!values.containsKey(key) && values.size() >= MAX_ENTRIES)
            throw new IllegalStateException("Progress save is full");
        values.put(key, value);
    }
    void merge(ProgressData other) {
        TreeMap<String, Long> merged = new TreeMap<>(values);
        for (Map.Entry<String, Long> e : other.values.entrySet()) {
            Long old = merged.get(e.getKey());
            if (old == null || e.getValue() > old) merged.put(e.getKey(), e.getValue());
        }
        if (merged.size() > MAX_ENTRIES) throw new IllegalArgumentException("Progress save is full");
        values.clear(); values.putAll(merged);
    }
    byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(values.size());
            for (Map.Entry<String, Long> e : values.entrySet()) {
                out.writeUTF(e.getKey()); out.writeLong(e.getValue());
            }
            out.flush();
            if (bytes.size() > MAX_BYTES) throw new IllegalStateException("Progress save is full");
            return bytes.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    static ProgressData decode(byte[] bytes) throws IOException {
        ProgressData data = new ProgressData();
        if (bytes == null || bytes.length == 0) return data;
        if (bytes.length > MAX_BYTES) throw new IOException("Progress save is too large");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != MAGIC || in.readInt() != VERSION)
            throw new IOException("Unsupported progress save version");
        int size = in.readInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IOException("Invalid progress entry count");
        for (int i = 0; i < size; i++) {
            String key = in.readUTF(); long value = in.readLong();
            if (!key.matches("(m:[a-z0-9_]{1,64}|c:[a-zA-Z0-9_-]{1,64}:[a-z0-9_]{1,64})")
                    || value < 0 || data.values.containsKey(key)) throw new IOException("Invalid progress entry");
            data.values.put(key, value);
        }
        if (in.available() != 0) throw new IOException("Trailing progress data");
        return data;
    }
    static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    static int integer(long value) { return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value)); }
}
