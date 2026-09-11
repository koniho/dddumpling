package com.dddumpling.game;

/**
 * Browsing the display case on the title screen: scroll, jump, drag, and the two-tap wipe.
 *
 * Works on {@code GameCore}'s case fields rather than owning them, the {@link Fx} seam. Geometry and
 * drawing are {@link Showcase}'s; what lives here is what a tap or a drag does.
 */
final class CaseUi {

    private CaseUi() {}

    private static void highlight(GameCore c, int index) {
        if (c.caseIndex != index) c.caseHighlightAge = 0f;
        c.caseIndex = index;
    }

    /**
     * Opens the case. Title screen only, and not once a start press has begun the dissolve — the
     * case would be fading in over a screen fading out.
     */
    static void open(GameCore c) {
        if (c.state != GameCore.TITLE || c.starting() || c.caseOpen) return;
        c.caseOpen = true;
        c.caseSlide = c.caseSlideY = 0f;
        c.caseFreePan = false;
        c.casePanMotionX = c.casePanMotionY = 0f;
        c.caseT = c.caseHighlightAge = 0f;
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
        highlight(c, Showcase.across(c.caseIndex, dir > 0 ? 1 : -1));
        // Full slide, decaying to zero: the shelf glides in from the side it came from.
        c.caseFreePan = false;
        c.caseSlide = dir > 0 ? 1f : -1f;
        c.caseSlideY = 0f;
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
        highlight(c, n);
        c.caseSlide = c.caseSlideY = 0f;
        c.caseFreePan = false;
        c.casePanMotionX = c.casePanMotionY = 0f;
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    static void row(GameCore c, int dir) {
        if (dir == 0 || !c.caseOpen || c.storyOpen()) return;
        highlight(c, Showcase.down(c.caseIndex, dir > 0 ? 1 : -1));
        c.caseFreePan = false;
        c.caseSlideY = dir > 0 ? 1f : -1f;
        c.caseSlide = 0f;
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    /** Start at the visible position, then glide the selected collectible to the center. */
    static void select(GameCore c, int index) {
        if (!c.caseOpen || c.storyOpen()) return;
        float panX = Showcase.column(c.caseIndex) - c.caseSlide;
        float panY = Showcase.row(c.caseIndex) - c.caseSlideY;
        highlight(c, Showcase.wrap(index));
        c.caseHighlightAge = 0f;
        c.caseSlide = Showcase.column(c.caseIndex) - panX;
        c.caseSlideY = Showcase.row(c.caseIndex) - panY;
        c.caseFreePan = false;
        c.casePanMotionX = -Math.max(-1f, Math.min(1f, c.caseSlide));
        c.casePanMotionY = -Math.max(-1f, Math.min(1f, c.caseSlideY));
        if (c.sound != null) c.sound.squish(c.caseIndex % Glyph.COUNT, 1);
    }

    static void beginDrag(GameCore c, float x, float y) {
        if (!c.caseOpen || c.storyOpen()) return;
        c.caseDragging = c.caseFreePan = true;
        c.caseDragX = x;
        c.caseDragY = y;
    }

    /** Pixel-continuous pan on both axes. Only the outer catalogue bounds limit movement. */
    static void dragTo(GameCore c, float x, float y, Layout L) {
        if (!c.caseDragging || !c.caseOpen || c.storyOpen()) return;
        float dx = (x - c.caseDragX) / Showcase.step(L);
        float dy = (y - c.caseDragY) / Showcase.rowStep(L);
        c.caseDragX = x;
        c.caseDragY = y;
        float panX = Showcase.column(c.caseIndex) - c.caseSlide - dx;
        float panY = Showcase.row(c.caseIndex) - c.caseSlideY - dy;
        int maxColumns = 1;
        for (int row = 0; row < Showcase.ROW_NAME.length; row++)
            maxColumns = Math.max(maxColumns, Showcase.columns(row));
        panX = Math.max(-0.35f, Math.min(maxColumns - 0.65f, panX));
        panY = Math.max(-0.35f, Math.min(Showcase.ROW_NAME.length - 0.65f, panY));
        int row = Math.max(0, Math.min(Showcase.ROW_NAME.length - 1, Math.round(panY)));
        highlight(c, Showcase.entry(row, Math.round(panX)));
        c.caseSlide = Showcase.column(c.caseIndex) - panX;
        c.caseSlideY = Showcase.row(c.caseIndex) - panY;
        c.casePanMotionX = Math.max(-1f, Math.min(1f, dx * 5f));
        c.casePanMotionY = Math.max(-1f, Math.min(1f, dy * 5f));
    }

    /** No snapping: leave the surface exactly where it was released. */
    static void endDrag(GameCore c) {
        c.caseDragging = false;
    }

    /**
     * The clear-case button: arms on the first tap, empties on the second. The collection is the one
     * thing here that took several runs to build, so it is behind a confirmation.
     */
    static void tapClear(GameCore c) {
        if (!BuildFlags.DEVELOPER) return;
        if (!c.clearArmed) {
            c.clearArmed = true;
            return;
        }
        c.clearArmed = false;
        c.collected = 0L;
        // The tally goes too: it counts baskets opened for entries that no longer exist, and leaving
        // it would put "COLLECTIONS: 40" over an empty case. Same for the run's haul.
        c.collectTotal = 0;
        java.util.Arrays.fill(c.collectionCounts, 0);
        c.prize = -1;
        c.roundPrizes = 0L;
        c.homeT = 0f;
        c.homeLanded = 0;
        c.closeStory();
        c.caseIndex = 0;
        c.caseSlide = c.caseSlideY = 0f;
        c.caseFreePan = false;
        c.casePanMotionX = c.casePanMotionY = 0f;
        if (c.store != null) {
            c.store.saveCollected(0L);
            c.store.saveCollectTotal(0);
            c.store.saveCollectionCounts(c.collectionCounts);
        }
        if (c.sound != null) c.sound.wrong();
    }
}
