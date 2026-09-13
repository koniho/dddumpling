package com.dddumpling.game;

/** A friendly, blocking choice about optional Firebase gameplay analytics. */
public final class AnalyticsUi extends Draw {
    public static final int NONE = 0;
    public static final int ALLOW = 1;
    public static final int DECLINE = 2;
    public static final int POLICY = 3;
    public static final int CLOSE = 4;

    private static final String NEW_TITLE = "HELP DDDUMPLING GROW";
    private static final String ON_TITLE = "ANALYTICS ON";
    private static final String NEW_MESSAGE = "This sends gameplay events, app and device info, "
            + "approximate location, and an app-instance ID to Google Analytics for Firebase. "
            + "Your choice will not affect play or saves. Change it from Title Privacy.";
    private static final String ON_MESSAGE = "Turning it off stops collection and clears local "
            + "analytics data. It does not delete past reports already sent to Google. Play and "
            + "saves stay the same. Change this from Title Privacy.";
    private static final String[] NEW_LINES = {
            "THIS SENDS GAMEPLAY EVENTS", "APP AND DEVICE INFO",
            "APPROXIMATE LOCATION, AND", "AN APP-INSTANCE ID TO GOOGLE",
            "ANALYTICS FOR FIREBASE.", "YOUR CHOICE WILL NOT AFFECT",
            "PLAY OR SAVES. CHANGE IT FROM", "TITLE PRIVACY."
    };
    private static final String[] ON_LINES = {
            "TURNING IT OFF STOPS COLLECTION", "AND CLEARS LOCAL ANALYTICS DATA.",
            "IT DOES NOT DELETE PAST REPORTS", "ALREADY SENT TO GOOGLE. PLAY AND",
            "SAVES STAY THE SAME. CHANGE THIS", "FROM TITLE PRIVACY."
    };

    private boolean visible;
    private boolean enabled;
    private final float[] left = new float[CLOSE + 1];
    private final float[] top = new float[CLOSE + 1];
    private final float[] width = new float[CLOSE + 1];
    private final float[] height = new float[CLOSE + 1];
    private float cardL, cardT, cardR, cardB, closeX, closeY, closeR;
    private float unit;

    /** Shows the initial request, or the title-screen control for an existing choice. */
    public void show(boolean enabled) {
        this.enabled = enabled;
        visible = true;
    }

    public void hide() { visible = false; }

    public boolean visible() { return visible; }

    public boolean enabled() { return enabled; }

    /** Text shared by the painted UI and native accessibility controls. */
    public String title() { return enabled ? ON_TITLE : NEW_TITLE; }

    /** Text shared by the painted UI and native accessibility controls. */
    public String message() { return enabled ? ON_MESSAGE : NEW_MESSAGE; }

    /** Accessible label for one of this modal's actions. */
    public String actionTitle(int action) {
        if (action == ALLOW) return enabled ? "KEEP ON" : "ALLOW ANALYTICS";
        if (action == DECLINE) return enabled ? "TURN OFF" : "NO THANKS";
        if (action == POLICY) return "PRIVACY POLICY";
        if (action == CLOSE) return "CLOSE";
        return "";
    }

    /** Recomputes the painted and accessible target bounds after a layout change. */
    void compute(Layout L) {
        unit = L.unit;
        float safeB = L.h - L.padB;
        float cardW = Math.min(L.w - 1.4f * unit, L.w * 0.92f);
        float cardH = Math.min(21.7f * unit, safeB - L.topSafe - unit);
        cardL = (L.w - cardW) / 2f;
        cardR = cardL + cardW;
        cardT = L.topSafe + (safeB - L.topSafe - cardH) / 2f;
        cardB = cardT + cardH;

        float buttonGap = unit * 0.45f;
        float buttonW = (cardW - 3.0f * unit - buttonGap) / 2f;
        set(ALLOW, cardL + unit, cardT + 14.55f * unit, buttonW, 2.45f * unit);
        set(DECLINE, left[ALLOW] + buttonW + buttonGap, top[ALLOW], buttonW, height[ALLOW]);
        float policyW = Math.min(cardW - 3f * unit, 13.6f * unit);
        float policyH = Math.max(44f, 2.6f * unit);
        set(POLICY, (L.w - policyW) / 2f, cardT + 19.50f * unit - policyH / 2f,
                policyW, policyH);
        closeR = unit * 0.72f;
        closeX = cardR - unit * 1.18f;
        closeY = cardT + unit * 1.18f;
        float closeTouch = Math.max(44f, closeR * 2.5f);
        set(CLOSE, closeX - closeTouch / 2f, closeY - closeTouch / 2f,
                closeTouch, closeTouch);
    }

    /** Draws the modal. The host keeps all underlying input blocked while it is visible. */
    void draw(Painter p, Layout L, float clock) {
        if (!visible) return;
        compute(L);
        p.fillRect(0, 0, L.w, L.h, 0xD9181430);
        p.fillPoly(roundRect(cardL, cardT, cardR, cardB, unit * 1.15f), 0xFF2A2348);
        p.strokePoly(roundRect(cardL, cardT, cardR, cardB, unit * 1.15f),
                Glyph.withAlpha(INK, 75), unit * 0.06f);

        p.text(title(), L.w / 2f, cardT + 1.75f * unit, type(unit * 0.90f),
                enabled ? Glyph.COLOR[Kawaii.GRAPES] : GOLD, Painter.CENTER, true);
        p.text(enabled ? "YOU ARE IN CONTROL" : "OPTIONAL GAMEPLAY ANALYTICS", L.w / 2f,
                cardT + 2.72f * unit, type(unit * 0.48f), INK_DIM, Painter.CENTER, false);

        float bob = (float) Math.sin(clock * 2.1f) * unit * 0.10f;
        Kawaii.draw(p, Kawaii.DUMPLING, L.w / 2f - unit * 2.1f,
                cardT + 4.55f * unit + bob, unit * 1.04f, Glyph.COLOR[Kawaii.DUMPLING],
                1f, 0.85f);
        Kawaii.draw(p, Kawaii.CAT, L.w / 2f + unit * 2.1f,
                cardT + 4.55f * unit - bob, unit * 1.04f, Glyph.COLOR[Kawaii.CAT],
                1f, 0.85f);
        p.fillCircle(L.w / 2f, cardT + 4.55f * unit, unit * 0.20f,
                Glyph.withAlpha(GOLD, 210));

        String[] lines = enabled ? ON_LINES : NEW_LINES;
        float bodySize = Math.max(14f, type(unit * 0.66f));
        float line = Math.max(15f, unit * 0.96f);
        float y = cardT + (enabled ? 7.20f : 6.65f) * unit;
        for (int i = 0; i < lines.length; i++)
            p.text(lines[i], L.w / 2f, y + i * line, bodySize, INK, Painter.CENTER, false);

        button(p, ALLOW, enabled ? Glyph.COLOR[Kawaii.SQUISHY] : Glyph.COLOR[Kawaii.GRAPES]);
        button(p, DECLINE, enabled ? ROSE : Glyph.COLOR[Kawaii.STRAWBERRY]);
        p.text("PRIVACY POLICY", L.w / 2f, top[POLICY] + height[POLICY] * 0.60f,
                Math.max(14f, type(unit * 0.60f)), INK_DIM, Painter.CENTER, true);
        p.line(left[POLICY] + unit * 1.1f, top[POLICY] + height[POLICY] * 0.78f,
                left[POLICY] + width[POLICY] - unit * 1.1f, top[POLICY] + height[POLICY] * 0.78f,
                Glyph.withAlpha(INK_DIM, 170), unit * 0.045f);

        p.fillCircle(closeX, closeY, closeR, Glyph.withAlpha(ROSE, 75));
        p.strokeCircle(closeX, closeY, closeR, Glyph.withAlpha(ROSE, 235), unit * 0.055f);
        float x = closeR * 0.38f;
        p.line(closeX - x, closeY - x, closeX + x, closeY + x, INK, unit * 0.075f);
        p.line(closeX + x, closeY - x, closeX - x, closeY + x, INK, unit * 0.075f);
    }

    /** Returns an action only while the modal owns input. */
    int hit(float x, float y) {
        if (!visible) return NONE;
        for (int action = ALLOW; action <= CLOSE; action++)
            if (x >= left[action] && x <= left[action] + width[action]
                    && y >= top[action] && y <= top[action] + height[action]) return action;
        return NONE;
    }

    public float actionLeft(int action) { return value(left, action); }
    public float actionTop(int action) { return value(top, action); }
    public float actionWidth(int action) { return value(width, action); }
    public float actionHeight(int action) { return value(height, action); }

    private void set(int action, float l, float t, float w, float h) {
        left[action] = l; top[action] = t; width[action] = w; height[action] = h;
    }

    private static float value(float[] values, int action) {
        return action >= ALLOW && action <= CLOSE ? values[action] : 0f;
    }

    private void button(Painter p, int action, int color) {
        float cx = left[action] + width[action] / 2f;
        float cy = top[action] + height[action] / 2f;
        p.fillPoly(pill(cx, cy, width[action] / 2f, height[action] / 2f, 12), color);
        p.strokePoly(pill(cx, cy, width[action] / 2f, height[action] / 2f, 12),
                Glyph.withAlpha(INK, 175), unit * 0.05f);
        p.text(actionTitle(action), cx, cy + unit * 0.24f, Math.max(14f, type(unit * 0.55f)),
                BG, Painter.CENTER, true);
    }

    private static float[] roundRect(float l, float t, float r, float b, float radius) {
        int corners = 5;
        float rr = Math.min(radius, Math.min((r - l) / 2f, (b - t) / 2f));
        float[] pts = new float[corners * 4 * 2];
        int at = 0;
        float[] starts = {180f, 270f, 0f, 90f};
        for (int c = 0; c < 4; c++) {
            float cx = c == 0 || c == 3 ? l + rr : r - rr;
            float cy = c < 2 ? t + rr : b - rr;
            float start = starts[c];
            for (int k = 0; k < corners; k++) {
                double a = Math.toRadians(start + 90f * k / (corners - 1));
                pts[at++] = cx + rr * (float) Math.cos(a);
                pts[at++] = cy + rr * (float) Math.sin(a);
            }
        }
        return pts;
    }
}
