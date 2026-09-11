package com.dddumpling.game;

import java.io.File;

/** Exports the actual game character for the store and Android launcher. */
final class AppIcon {
    private static final int SLIME_GREEN = 0xFF9EE65B;
    public static void main(String[] args) throws Exception {
        render("app-store/google-play/icon.png", 512, 0.38f);
        render("res/drawable-nodpi/dumpling_icon_legacy.png", 192, 0.38f);
        // Keep the whole character inside Android's central 66/108 safe area.
        render("res/drawable-nodpi/dumpling_icon_foreground.png", 432, 0.29f);
    }

    private static void render(String path, int size, float radius) throws Exception {
        RasterPainter p = new RasterPainter(size, size, 4);
        p.clear(Draw.BG);
        float r = size * radius;
        Kawaii.draw(p, Kawaii.DUMPLING, size * 0.5f, size * 0.5f + r * 0.05f,
                r, SLIME_GREEN, 1f, 0.35f);
        Png.write(new File(path), p.resolve(), size, size);
    }
}
