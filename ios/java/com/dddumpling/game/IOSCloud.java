package com.dddumpling.game;

/** Main-thread cloud coordinator; the host owns GameKit and account identity. */
public final class IOSCloud implements Progress.Sink {
    public interface Host {
        void fetch(int token);
        void save(byte[] data, int token);
    }
    private final GameCore core;
    private final Host host;
    private final boolean enabled;
    private String owner = "";
    private boolean foreground, dirty = true, merging, blocked;
    private int generation, busy = -1;
    private double clock, due;
    private String status = "Playing locally";

    public IOSCloud(IOSGame game, Host host) { this(game.core(), host, BuildFlags.DEVELOPER); }
    IOSCloud(GameCore core, Host host, boolean enabled) {
        this.core = core; this.host = host; this.enabled = enabled;
    }
    public String status() { return status; }
    public void session(String identity, boolean active) {
        if (!enabled) return;
        identity = identity == null ? "" : identity;
        if (!owner.equals(identity)) {
            owner = identity; generation++; busy = -1; blocked = false; dirty = true; due = clock;
            core.progress.attach(null);
            status = "Playing locally";
        }
        if (foreground && !active) {
            core.progress.checkpoint(core.score);
            if (dirty) sync();
        }
        if (!foreground && active) due = clock;
        foreground = active;
        core.progress.attach(active && !owner.isEmpty() && !blocked ? this : null);
    }
    public void update(double elapsed) {
        if (!enabled) return;
        clock += Math.max(0, Math.min(elapsed, 1));
        if (foreground && clock >= due) sync();
    }
    private boolean valid(int token) {
        return enabled && token == generation && busy == token && !owner.isEmpty();
    }
    private void sync() {
        if (!enabled || owner.isEmpty() || blocked || busy != -1 || !core.progress.available()) return;
        busy = ++generation; dirty = false; status = "Syncing";
        host.fetch(generation);
    }
    public void fetched(byte[][] saves, int token) {
        if (!valid(token)) return;
        try {
            // Decode every conflict before touching local progress or writing back to iCloud.
            ProgressData merged = new ProgressData();
            for (byte[] save : saves) {
                if (save == null || save.length == 0) throw new java.io.IOException("Empty cloud save");
                merged.merge(ProgressData.decode(save));
            }
            merging = true;
            core.progress.restore(merged.encode(), core);
            if (!core.progress.available()) throw new java.io.IOException("Local save unavailable");
            host.save(core.progress.snapshot(), token);
        } catch (Exception e) {
            failed(token, true);
        } finally { merging = false; }
    }
    public void saved(int token) {
        if (!valid(token)) return;
        busy = -1; due = clock + (dirty ? 5 : 60); status = "Synced";
    }
    public void failed(int token, boolean invalidSave) {
        if (!valid(token)) return;
        busy = -1; dirty = true; blocked = invalidSave; due = clock + 60;
        status = invalidSave ? "Cloud save unreadable; sync paused" : "Playing locally";
        if (blocked) core.progress.attach(null);
    }
    @Override public void changed() { if (!merging) { dirty = true; due = Math.min(due, clock + 5); } }
    // GameKit has no Play Events equivalent; counters stay in the merged progress document.
    @Override public void event(String name, int amount) { }
}
