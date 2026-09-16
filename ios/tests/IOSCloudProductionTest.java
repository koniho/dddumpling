package com.dddumpling.game;

public final class IOSCloudProductionTest extends Check {
    public static void main(String[] args) {
        check("iOS production flag is off", !BuildFlags.DEVELOPER);
        IOSGame game = new IOSGame(new Mem(), new Ear(), 9);
        IOSCloud cloud = new IOSCloud(game, new IOSCloud.Host() {
            public void fetch(int token) { throw new AssertionError("Release fetched cloud data"); }
            public void save(byte[] data, int token) { throw new AssertionError("Release uploaded cloud data"); }
        }, false);
        byte[] before = game.core().progress.snapshot();
        cloud.session("player", true); cloud.changed(); cloud.update(120);
        cloud.fetched(new byte[][] {new byte[] {1}}, 0); cloud.saved(0);
        cloud.session("player", false);
        check("Release cloud entry points leave local progress unchanged",
            java.util.Arrays.equals(before, game.core().progress.snapshot()));
        check("Release retains local progress recording", game.core().progress.available());
        final int[] calls = new int[3];
        IOSCloud enabled = new IOSCloud(game, new IOSCloud.Host() {
            public void fetch(int token) { calls[0]++; calls[2] = token; }
            public void save(byte[] data, int token) { calls[1]++; }
        }, true);
        enabled.session("player", true); enabled.update(0);
        check("Opted-in Release fetches cloud progress", calls[0] == 1);
        enabled.fetched(new byte[0][], calls[2]); enabled.saved(calls[2]);
        check("Opted-in Release uploads and finishes sync", calls[1] == 1 && "Synced".equals(enabled.status()));
        System.out.println("iOS cloud production: " + pass + " passed, " + fail + " failed");
        if (fail != 0) throw new AssertionError("iOS cloud production regressions");
    }
}
