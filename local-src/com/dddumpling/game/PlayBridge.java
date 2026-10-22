package com.dddumpling.game;

/** Offline production builds contain no Play Games SDK or network reporting. */
final class PlayBridge {
    void showAchievements() {}
    PlayBridge(MainActivity activity, GameCore core) {}
    void resume() {}
    void pause() {}
    void close() {}
}
