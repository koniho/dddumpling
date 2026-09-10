package com.sram.hexatype;

/** First-release and developer builds contain no Play Games SDK or network reporting. */
final class PlayBridge {
    PlayBridge(MainActivity activity, GameCore core) {}
    void resume() {}
    void pause() {}
    void close() {}
}
