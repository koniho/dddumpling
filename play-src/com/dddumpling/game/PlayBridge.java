package com.dddumpling.game;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Tasks;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional production adapter; all game/save decisions remain in the headless progress model. */
final class PlayBridge implements Progress.Sink {
    private static final String SAVE = "dddumpling-progress-v1";
    private final MainActivity activity;
    private final GameCore core;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable scheduled = () -> sync();
    private boolean foreground, closed, authenticated, merging, dirty = true, notified, blocked;
    private int generation, busy = -1;

    PlayBridge(MainActivity activity, GameCore core) { this.activity = activity; this.core = core; }

    void resume() {
        if (closed || BuildFlags.DEVELOPER || !core.progress.available()) return;
        foreground = true; authenticated = false; blocked = false; dirty = true;
        core.progress.attach(null);
        final int token = ++generation;
        try {
            PlayGames.getGamesSignInClient(activity).isAuthenticated().addOnCompleteListener(task -> {
                if (closed || token != generation) return;
                if (!task.isSuccessful() || !task.getResult().isAuthenticated()) {
                    notice("Playing locally. Cloud saves need a Play Games profile."); return;
                }
                PlayGames.getPlayersClient(activity).getCurrentPlayer().addOnCompleteListener(player -> {
                    if (closed || token != generation) return;
                    if (!player.isSuccessful() || player.getResult() == null) return;
                    if (!activity.bindProgressOwner(player.getResult().getPlayerId())) {
                        notice("Local progress belongs to a different Play Games profile. Cloud sync is paused.");
                        return;
                    }
                    authenticated = true;
                    core.progress.attach(this);
                    sync();
                });
            });
        } catch (RuntimeException e) { notice("Playing locally. Play Games is unavailable."); }
    }

    void pause() {
        handler.removeCallbacks(scheduled);
        // Last checkpoint is already local. Let an authenticated in-flight save finish in background.
        if (authenticated && dirty) sync();
        foreground = false;
        core.progress.attach(null);
    }
    void close() {
        closed = true; generation++;
        core.progress.attach(null); handler.removeCallbacks(scheduled); io.shutdown();
    }
    private boolean valid(int token) { return !closed && token == generation && authenticated; }
    private void schedule(long delay) {
        handler.removeCallbacks(scheduled);
        if (foreground && !closed && !blocked) handler.postDelayed(scheduled, delay);
    }
    @Override public void event(String name, int amount) {
        if (!authenticated || !foreground || closed || amount <= 0 || BuildFlags.DEVELOPER) return;
        String id = PlayConfig.event(name);
        if (id == null) return;
        try { PlayGames.getEventsClient(activity).increment(id, amount); }
        catch (RuntimeException ignored) { /* Never retry an ambiguous increment and double-count it. */ }
    }
    @Override public void changed() {
        if (merging) return;
        dirty = true;
        schedule(5000);
    }

    private void sync() {
        if (closed || !authenticated || blocked || !core.progress.available() || busy == generation) return;
        final int token = generation;
        busy = token; dirty = false;
        try {
            PlayGames.getSnapshotsClient(activity).open(SAVE, true,
                    SnapshotsClient.RESOLUTION_POLICY_MANUAL).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) { failed(token, false); return; }
                consume(task.getResult(), token, 0);
            });
        } catch (RuntimeException e) { failed(token, false); }
    }

    private void consume(SnapshotsClient.DataOrConflict<Snapshot> result, int token, int retries) {
        final SnapshotsClient client = PlayGames.getSnapshotsClient(activity);
        final SnapshotsClient.SnapshotConflict conflict = result.isConflict() ? result.getConflict() : null;
        final Snapshot first = conflict == null ? result.getData() : conflict.getSnapshot();
        final Snapshot second = conflict == null ? null : conflict.getConflictingSnapshot();
        if (!valid(token) || retries >= 4) {
            discard(client, first); discard(client, second);
            if (valid(token)) failed(token, false);
            return;
        }
        // Both versions must decode before either can affect the local save or replace a cloud version.
        Tasks.call(io, () -> {
            ProgressData remote = ProgressData.decode(first.getSnapshotContents().readFully());
            if (second != null) remote.merge(ProgressData.decode(second.getSnapshotContents().readFully()));
            return remote.encode();
        }).addOnCompleteListener(read -> {
            if (!valid(token) || !read.isSuccessful()) {
                discard(client, first); discard(client, second);
                if (valid(token)) failed(token, true);
                return;
            }
            final byte[] payload;
            try {
                merging = true;
                core.progress.restore(read.getResult(), core);
                if (!core.progress.available()) throw new IOException("Local save unavailable");
                payload = core.progress.snapshot();
            } catch (Exception e) {
                discard(client, first); discard(client, second); failed(token, true); return;
            } finally { merging = false; }
            final SnapshotContents content = conflict == null ? first.getSnapshotContents()
                    : conflict.getResolutionSnapshotContents();
            Tasks.call(io, () -> {
                if (!content.writeBytes(payload)) throw new IOException("Snapshot write failed");
                return true;
            }).addOnCompleteListener(write -> {
                if (!valid(token) || !write.isSuccessful()) {
                    discard(client, first); discard(client, second);
                    if (valid(token)) failed(token, false);
                    return;
                }
                SnapshotMetadataChange metadata = new SnapshotMetadataChange.Builder()
                        .setDescription("DDDUMPLING scores, collection and progress").build();
                try {
                    if (conflict != null) {
                        client.resolveConflict(conflict.getConflictId(), first.getMetadata().getSnapshotId(),
                                metadata, content).addOnCompleteListener(resolved -> {
                            if (resolved.isSuccessful()) consume(resolved.getResult(), token, retries + 1);
                            else failed(token, false);
                        });
                    } else {
                        client.commitAndClose(first, metadata).addOnCompleteListener(committed -> {
                            if (!valid(token)) return;
                            if (!committed.isSuccessful()) { failed(token, false); return; }
                            busy = -1;
                            schedule(dirty ? 5000 : 60000);
                        });
                    }
                } catch (RuntimeException e) {
                    discard(client, first); discard(client, second); failed(token, false);
                }
            });
        });
    }
    private static void discard(SnapshotsClient client, Snapshot snapshot) {
        if (snapshot != null) try { client.discardAndClose(snapshot); } catch (RuntimeException ignored) { }
    }
    private void failed(int token, boolean invalidSave) {
        if (!valid(token)) return;
        busy = -1; dirty = true; blocked = invalidSave;
        if (invalidSave) notice("Cloud save could not be read. Your local progress is safe; sync is paused.");
        else schedule(60000);
    }
    private void notice(String message) {
        if (!foreground || notified || closed) return;
        notified = true;
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
