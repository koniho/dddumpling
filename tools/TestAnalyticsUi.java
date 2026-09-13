package com.dddumpling.game;

/** Consent geometry is shared with native accessible overlays, so its bounds are contract-tested. */
final class TestAnalyticsUi extends Check {
    private TestAnalyticsUi() {}

    static void layout() {
        verify(320, 640, "minimum phone");
        verify(390, 800, "phone");
        verify(768, 1024, "portrait tablet");
    }

    private static void verify(int w, int h, String name) {
        Layout L = new Layout();
        L.compute(w, h, 0, 0, 0, 0);
        AnalyticsUi ui = new AnalyticsUi();
        check(name + " starts hidden", !ui.visible() && ui.hit(w / 2f, h / 2f) == AnalyticsUi.NONE);
        verifyState(ui, L, false, name);
        verifyState(ui, L, true, name);
    }

    private static void verifyState(AnalyticsUi ui, Layout L, boolean enabled, String name) {
        ui.show(enabled);
        ui.compute(L);
        check(name + " " + (enabled ? "enabled" : "new") + " action labels",
                enabled ? "KEEP ON".equals(ui.actionTitle(AnalyticsUi.ALLOW))
                        && "TURN OFF".equals(ui.actionTitle(AnalyticsUi.DECLINE))
                        : "ALLOW ANALYTICS".equals(ui.actionTitle(AnalyticsUi.ALLOW))
                        && "NO THANKS".equals(ui.actionTitle(AnalyticsUi.DECLINE)));
        boolean boundsFit = true, centresHit = true;
        for (int action = AnalyticsUi.ALLOW; action <= AnalyticsUi.CLOSE; action++) {
            float l = ui.actionLeft(action), t = ui.actionTop(action);
            float r = l + ui.actionWidth(action), b = t + ui.actionHeight(action);
            boundsFit &= l >= 0 && t >= 0 && r <= L.w && b <= L.h
                    && ui.actionWidth(action) > 0 && ui.actionHeight(action) > 0;
            if (action == AnalyticsUi.POLICY || action == AnalyticsUi.CLOSE)
                boundsFit &= ui.actionWidth(action) >= 44f && ui.actionHeight(action) >= 44f;
            centresHit &= ui.hit((l + r) / 2f, (t + b) / 2f) == action;
        }
        check(name + " " + (enabled ? "enabled" : "new") + " targets fit", boundsFit);
        check(name + " " + (enabled ? "enabled" : "new") + " target centres agree", centresHit);
        RasterPainter.clearFit();
        RasterPainter p = new RasterPainter((int) L.w, (int) L.h, 1);
        ui.draw(p, L, 1.5f);
        check(name + " " + (enabled ? "enabled" : "new") + " copy fits", RasterPainter.unfit.isEmpty());
        ui.hide();
        check(name + " " + (enabled ? "enabled" : "new") + " hides input",
                ui.hit(L.w / 2f, L.h / 2f) == AnalyticsUi.NONE);
    }
}
