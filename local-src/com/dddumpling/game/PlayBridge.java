package com.dddumpling.game;

/** Offline production builds contain no Play Games SDK or network reporting. */
final class PlayBridge {
    private final GameCore core;
    void showLeaderboard(int board) {core.preferences.externalFinished(core);}
    void requestReview() {}
    void showAchievements() {}
    void signIn() {}
    PlayBridge(MainActivity activity, GameCore core) {this.core=core;core.leaderboards.account(null,false);}
    void resume() {}
    void pause() {}
    void close() {}
}
