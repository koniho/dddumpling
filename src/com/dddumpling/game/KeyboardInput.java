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
        return (c.state == GameCore.PLAY || c.state == GameCore.BONUS && !c.starBonus)
                && !c.paused && !c.settingsOpen && !c.starter.open && !c.townOpen
                && !c.highScoreScreen.open && !c.releaseNotes.open && !c.updatePrompt.open && !c.storyOpen()
                && c.returnFade <= 0 && c.rosterSceneT <= 0
                && c.onboarding.sceneWait <= 0 && c.onboarding.practice == null
                && !c.onboarding.briefing && !c.onboarding.ownsTouch && !c.onboarding.rescueGuide;
    }

    // -1: unbound, 0: consumed, 1: dispatched to the deck.
    int down(GameCore c, Layout layout, int letter, boolean repeat) {
        int g = glyph(letter);
        if (g < 0 || !Roster.active(c.playRosterFull(), g)) return -1;
        int bit = 1 << g;
        if (repeat || (held & bit) != 0) return 0;
        held |= bit;
        if (!available(c) || !c.keyActive(g)) return 0;
        if (c.state == GameCore.BONUS) c.tapBonus(g, 0f);
        else c.tapKey(g, layout);
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
