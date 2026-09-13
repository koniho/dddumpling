package com.dddumpling.game;

/** Best-effort gameplay observations, separate from accounts and cloud saves. */
public final class Analytics {
    private Analytics() {}
    public interface Sink {
        /** One observation; amount is a counter increment or elapsed milliseconds, never a user ID. */
        void event(String name, int amount);
    }
}
