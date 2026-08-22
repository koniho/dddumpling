package com.sram.hexatype;

/**
 * Browsing the display case on the title screen: scroll, jump, drag, and the two-tap wipe.
 *
 * Works on {@code GameCore}'s case fields rather than owning them, the {@link Fx} seam. Geometry and
 * drawing are {@link Showcase}'s; what lives here is what a tap or a drag does.
 */
final class CaseUi {

    private CaseUi() {}

    /**
     * Opens the case. Title screen only, and not once a start press has begun the dissolve — the
     * case would be fading in over a screen fading out.
     */
    static void open(GameCore c) {
        if (c.state != GameCore.TITLE || c.starting() || c.caseOpen) return;
        c.caseOpen = true;
        c.caseSlide = 0f;
        c.caseT = 0f;
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    /** Puts it away. The fade runs itself down from wherever it had got to. */
    static void close(GameCore c) {
        if (!c.caseOpen) return;
        c.caseOpen = false;
        c.caseDragging = false;
        c.closeStory();
    }

    /** One entry along, for a tap beside the shelf. Wraps, so neither side ever does nothing. */
    static void scroll(GameCore c, int dir) {
        if (dir == 0 || !c.caseOpen || c.storyOpen()) return;
        c.caseIndex = Showcase.wrap(c.caseIndex + (dir > 0 ? 1 : -1));
        // Full slide, decaying to zero: the shelf glides in from the side it came from.
        c.caseSlide = dir > 0 ? 1f : -1f;
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    /**
     * Straight to an entry, for the position bar being dragged. No slide: the finger is already the
     * animation, and easing in behind it would only lag it.
     */
    static void to(GameCore c, int i) {
        if (!c.caseOpen || c.storyOpen()) return;
        int n = Showcase.wrap(i);
        if (n == c.caseIndex) return;
        c.caseIndex = n;
        c.caseSlide = 0f;
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    static void beginDrag(GameCore c, float x) {
        if (!c.caseOpen || c.storyOpen()) return;
        c.caseDragging = true;
        c.caseDragX = x;
    }

    /**
     * The shelf following a finger. The offset rides in {@code caseSlide}, which the drawing and the
     * position bar already read, so a drag needs no second channel.
     *
     * Whole steps commit as the shelf passes halfway rather than on release, so the caption, the bar
     * and the story target are always the entry nearest the middle — the one being looked at.
     */
    static void dragTo(GameCore c, float x, Layout L) {
        if (!c.caseDragging) return;
        float step = Showcase.step(L);
        float o = (x - c.caseDragX) / step;
        int whole = Math.round(o);
        if (whole != 0) {
            c.caseIndex = Showcase.wrap(c.caseIndex - whole);
            c.caseDragX += whole * step;
            o -= whole;
            // One tick per entry passed, so a long drag ratchets.
            if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
        }
        c.caseSlide = o;
    }

    /** Lets go. Whatever offset is left eases out through the usual slide decay. */
    static void endDrag(GameCore c) {
        c.caseDragging = false;
    }

    /**
     * The clear-case button: arms on the first tap, empties on the second. The collection is the one
     * thing here that took several runs to build, so it is behind a confirmation.
     */
    static void tapClear(GameCore c) {
        if (!c.clearArmed) {
            c.clearArmed = true;
            return;
        }
        c.clearArmed = false;
        c.collected = 0L;
        // The tally goes too: it counts baskets opened for entries that no longer exist, and leaving
        // it would put "COLLECTIONS: 40" over an empty case. Same for the run's haul.
        c.collectTotal = 0;
        c.prize = -1;
        c.roundPrizes = 0L;
        c.homeT = 0f;
        c.homeLanded = 0;
        c.closeStory();
        c.caseIndex = 0;
        c.caseSlide = 0f;
        if (c.store != null) {
            c.store.saveCollected(0L);
            c.store.saveCollectTotal(0);
        }
        if (c.sound != null) c.sound.wrong();
    }
}
