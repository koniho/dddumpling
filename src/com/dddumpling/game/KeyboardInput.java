package com.dddumpling.game;

/** Physical deck keys; native hosts filter devices and system shortcuts. */
final class KeyboardInput {
    private int held;
    int presses, lastGlyph = -1;

    static int glyph(int letter) {
        switch (Character.toLowerCase((char) letter)) {
            case 's': return 0;
            case 'd': return 1;
            case 'f': return 2;
            case 'j': return 3;
            case 'k': return 4;
            case 'l': return 5;
            default: return -1;
        }
    }

    static boolean available(GameCore c) {
        return !c.paused && !c.settingsOpen && !c.starter.open && !c.townOpen && !c.cloudUi.open
                && !c.highScoreScreen.open && !c.releaseNotes.open && !c.updatePrompt.open && !c.storyOpen()
                && c.returnFade <= 0 && c.rosterSceneT <= 0
                && (c.state != GameCore.OVER || c.overReady())
                && c.onboarding.sceneWait <= 0
                && (c.onboarding.practice == null || c.onboarding.success <= 0)
                && !c.onboarding.briefing && !c.onboarding.rescueGuide
                && (!c.onboarding.ownsTouch || c.onboarding.practice != null)
                && !(target(c).state == GameCore.BONUS && target(c).starBonus);
    }

    private static GameCore target(GameCore c) {
        return c.onboarding.practice == null ? c : c.onboarding.practice;
    }

    // -1: unbound, 0: consumed, 1: dispatched to the deck.
    int down(GameCore c, Layout layout, int letter, boolean repeat) {
        int g = glyph(letter);
        GameCore deck = target(c);
        if (g < 0 || !Roster.active(deck.playRosterFull(), g)) return -1;
        int bit = 1 << g;
        if (repeat || (held & bit) != 0) return 0;
        held |= bit;
        if (!available(c) || !deck.keyActive(g)) return 0;
        if (deck.state == GameCore.BONUS) deck.tapBonus(g, 0f);
        else deck.tapKey(g, layout);
        presses++;
        lastGlyph = g;
        return 1;
    }

    boolean up(int letter) {
        int g = glyph(letter);
        if (g < 0) return false;
        boolean owned = (held & (1 << g)) != 0;
        held &= ~(1 << g);
        return owned;
    }

    void clear() { held = 0; }
}
