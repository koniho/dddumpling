package com.dddumpling.game;

import java.io.File;

/** Google Play feature artwork using the game's title face and character renderer. */
final class StoreGraphic extends Draw {
    public static void main(String[] args) throws Exception {
        RasterPainter p = new RasterPainter(1024, 500, 4);
        p.clear(BG);
        for (int i = 0; i < 7; i++) {
            float x = 65 + i * 153, y = i % 2 == 0 ? 62 : 440;
            p.fillPoly(Glyph.hex(x, y, 43), Glyph.withAlpha(Glyph.COLOR[i % 6], 12));
        }
        String title = "DDDUMPLING";
        for (int i = 0; i < title.length(); i++) {
            float x = 100 + (i % 5) * 104;
            float y = i < 5 ? 209 : 327;
            TitleBubbleFont.draw(p, title.charAt(i), x, y, 106,
                    Glyph.COLOR[i % 6], 1f, i * 0.6f, 1f);
        }
        p.text("TAP. BATTLE. COLLECT.", 307, 388, 22, INK, Painter.CENTER, true);
        tile(p, Kawaii.STRAWBERRY, 775, 149, 78, Glyph.COLOR[Kawaii.STRAWBERRY]);
        tile(p, Kawaii.DUMPLING, 708, 330, 86, 0xFF9EE65B);
        tile(p, Kawaii.CAT, 904, 302, 76, Glyph.COLOR[Kawaii.CAT]);
        Png.write(new File("app-store/google-play/feature-graphic.png"), p.resolve(), 1024, 500);
    }

    private static void tile(Painter p, int g, float x, float y, float r, int color) {
        p.fillPoly(Glyph.hex(x, y, r), Glyph.withAlpha(color, 35));
        p.strokePoly(Glyph.hex(x, y, r), Glyph.mix(color, INK, 0.25f), 4f);
        Kawaii.draw(p, g, x, y, r * 0.66f, color, 1f, 0.35f);
    }
}
