package com.sram.hexatype;

/**
 * Geometry and hit-testing for the settings panel, in one pure-Java place so the renderer
 * and the touch handler cannot drift apart about where the controls are.
 */
final class SettingsUi {

    static final int HIT_NONE = 0, HIT_SLIDER = 1, HIT_CLOSE = 2, HIT_OUTSIDE = 3;
    /** Option rows are HIT_OPTION + index. */
    static final int HIT_OPTION = 100;
    /** Playtest chips are HIT_TEST + mode index. */
    static final int HIT_TEST = 200;

    float panelL, panelT, panelR, panelB;
    float titleY;
    float speedLabelY;
    float sliderL, sliderR, sliderY, sliderH;
    float speedValueY;
    float bgmLabelY;
    float optionH;
    float firstOptionY;
    float closeCx, closeCy, closeR;
    /** Playtest row: one chip per powerup mode. */
    float testLabelY, testY, testH;

    private int options;

    void compute(Layout L, int optionCount) {
        options = optionCount;
        float s = L.unit;

        float w = Math.min(L.w * 0.86f, s * 20f);
        panelL = (L.w - w) / 2f;
        panelR = panelL + w;

        optionH = s * 1.5f;
        testH = s * 1.6f;
        float bodyH = s * 8.4f + optionH * optionCount + testH + s * 1.5f;
        panelT = Math.max(L.topSafe + s, (L.h - bodyH) / 2f - s);
        panelB = panelT + bodyH;

        titleY = panelT + s * 1.5f;

        speedLabelY = titleY + s * 1.9f;
        sliderH = s * 0.55f;
        sliderY = speedLabelY + s * 1.1f;
        sliderL = panelL + s * 1.4f;
        sliderR = panelR - s * 1.4f;
        speedValueY = sliderY + s * 1.5f;

        bgmLabelY = speedValueY + s * 1.5f;
        firstOptionY = bgmLabelY + s * 0.6f;

        testLabelY = firstOptionY + optionH * optionCount + s * 1.0f;
        testY = testLabelY + s * 0.35f;

        closeR = s * 1.05f;
        closeCx = panelR - closeR * 1.2f;
        closeCy = panelT + closeR * 1.2f;
    }

    /** Left edge of playtest chip {@code i} of {@code n}. */
    float testChipL(int i, int n) {
        float w = (optionR() - optionL()) / n;
        return optionL() + w * i;
    }

    float testChipR(int i, int n) {
        return testChipL(i, n) + (optionR() - optionL()) / n - 6f;
    }

    /** Centre y of option row {@code i}. */
    float optionCy(int i) {
        return firstOptionY + optionH * (i + 0.5f);
    }

    /** Left edge of the option rows. */
    float optionL() {
        return panelL + Layout.SQ3_2 * 0f + (panelR - panelL) * 0.06f;
    }

    float optionR() {
        return panelR - (panelR - panelL) * 0.06f;
    }

    /** Where the slider knob sits for a given speed. */
    float knobX(float speed) {
        float t = (speed - GameCore.SPEED_MIN) / (GameCore.SPEED_MAX - GameCore.SPEED_MIN);
        return sliderL + (sliderR - sliderL) * t;
    }

    /** Speed implied by a touch at {@code x}, clamped to the legal range. */
    float speedAt(float x) {
        float t = (x - sliderL) / (sliderR - sliderL);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        // Snap to a twentieth, so the value is reachable and readable.
        float v = GameCore.SPEED_MIN + t * (GameCore.SPEED_MAX - GameCore.SPEED_MIN);
        return Math.round(v * 20f) / 20f;
    }

    int hit(float x, float y) {
        if (x < panelL || x > panelR || y < panelT || y > panelB) return HIT_OUTSIDE;

        float dx = x - closeCx, dy = y - closeCy;
        if (dx * dx + dy * dy <= closeR * closeR * 1.3f) return HIT_CLOSE;

        // Generous vertical band: the track itself is thin but the target should not be.
        float grab = Math.max(optionH * 0.55f, sliderH * 2.2f);
        if (y >= sliderY - grab && y <= sliderY + grab) return HIT_SLIDER;

        for (int i = 0; i < options; i++) {
            float cy = optionCy(i);
            if (y >= cy - optionH / 2f && y <= cy + optionH / 2f) return HIT_OPTION + i;
        }

        if (y >= testY && y <= testY + testH) {
            for (int i = 0; i < Power.COUNT; i++) {
                if (x >= testChipL(i, Power.COUNT) && x <= testChipR(i, Power.COUNT)) {
                    return HIT_TEST + i;
                }
            }
        }
        return HIT_NONE;
    }
}
