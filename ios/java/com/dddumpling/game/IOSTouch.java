package com.dddumpling.game;

/** Immutable UIKit touch packet. IDs survive array reordering and remain present on pointer-up. */
public final class IOSTouch {
    public static final int ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2,
            ACTION_CANCEL = 3, ACTION_POINTER_DOWN = 5, ACTION_POINTER_UP = 6;
    private final int action, actionIndex;
    private final int[] ids;
    private final float[] xs, ys;
    private final float[][] historicalXs, historicalYs;

    public IOSTouch(int action, int actionIndex, int[] ids, float[] xs, float[] ys) {
        this(action, actionIndex, ids, xs, ys, new float[0][], new float[0][]);
    }

    /** History rows are chronological samples, columns use the current packet's pointer order. */
    public IOSTouch(int action, int actionIndex, int[] ids, float[] xs, float[] ys,
            float[][] historicalXs, float[][] historicalYs) {
        if (ids == null || xs == null || ys == null || ids.length != xs.length
                || ids.length != ys.length || ids.length == 0 || actionIndex < 0
                || actionIndex >= ids.length || historicalXs == null || historicalYs == null
                || historicalXs.length != historicalYs.length)
            throw new IllegalArgumentException("Invalid touch packet");
        if (action != ACTION_DOWN && action != ACTION_UP && action != ACTION_MOVE
                && action != ACTION_CANCEL && action != ACTION_POINTER_DOWN && action != ACTION_POINTER_UP)
            throw new IllegalArgumentException("Unknown touch action");
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] < 0) throw new IllegalArgumentException("Negative pointer ID");
            for (int j = 0; j < i; j++)
                if (ids[i] == ids[j]) throw new IllegalArgumentException("Duplicate pointer ID");
        }
        this.action = action;
        this.actionIndex = actionIndex;
        this.ids = ids.clone();
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.historicalXs = copyHistory(historicalXs, ids.length);
        this.historicalYs = copyHistory(historicalYs, ids.length);
    }

    private static float[][] copyHistory(float[][] samples, int pointers) {
        float[][] copy = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] == null || samples[i].length != pointers)
                throw new IllegalArgumentException("History pointer count changed");
            copy[i] = samples[i].clone();
        }
        return copy;
    }

    public int getActionMasked() { return action; }
    public int getActionIndex() { return actionIndex; }
    public int getPointerCount() { return ids.length; }
    public int getPointerId(int index) { return ids[index]; }
    public int findPointerIndex(int id) {
        for (int i = 0; i < ids.length; i++) if (ids[i] == id) return i;
        return -1;
    }
    public float getX() { return xs[0]; }
    public float getY() { return ys[0]; }
    public float getX(int index) { return xs[index]; }
    public float getY(int index) { return ys[index]; }
    public int getHistorySize() { return historicalXs.length; }
    public float getHistoricalX(int pointer, int sample) { return historicalXs[sample][pointer]; }
    public float getHistoricalY(int pointer, int sample) { return historicalYs[sample][pointer]; }
}
