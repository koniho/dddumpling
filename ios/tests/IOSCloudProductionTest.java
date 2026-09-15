package com.dddumpling.game;

public final class IOSCloudProductionTest extends Check {
    public static void main(String[] args) {
        check("iOS production flag is off", !BuildFlags.DEVELOPER);
        IOSGame game = new IOSGame(new Mem(), new Ear(), 9);
        IOSCloud cloud = new IOSCloud(game, new IOSCloud.Host() {
            public void fetch(int token) { throw new AssertionError("Release fetched cloud data"); }
            public void save(byte[] data, int token) { throw new AssertionError("Release uploaded cloud data"); }
        });
        byte[] before = game.core().progress.snapshot();
        cloud.session("player", true); cloud.changed(); cloud.update(120);
        cloud.fetched(new byte[][] {new byte[] {1}}, 0); cloud.saved(0);
        cloud.session("player", false);
        check("Release cloud entry points leave local progress unchanged",
            java.util.Arrays.equals(before, game.core().progress.snapshot()));
        check("Release retains local progress recording", game.core().progress.available());
        System.out.println("iOS cloud production: " + pass + " passed, " + fail + " failed");
        if (fail != 0) throw new AssertionError("iOS cloud production regressions");
    }
}
