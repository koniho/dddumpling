package com.dddumpling.game;
import java.io.File;

/** Landscape trailer backing, drawn with the game's own title and character art. */
final class TrailerPlate {
    public static void main(String[] args) throws Exception {
        RasterPainter p = new RasterPainter(1920, 1080, 1);
        p.clear(Draw.BG);
        p.fillRect(1260, 0, 1920, 1080, 0xFF120F23);
        p.fillRect(1354, 9, 1846, 1071, 0xFF504264);
        p.text("ARCADE GAMEPLAY", 145, 170, 25, Draw.INK_DIM, Painter.LEFT, true);
        String title = "DDDUMPLING";
        for (int i = 0; i < 10; i++)
            TitleBubbleFont.draw(p, title.charAt(i), 220 + i % 5 * 170,
                    i < 5 ? 355 : 538, 170, Glyph.COLOR[i % 6], 1f, i, 1f);
        for (int i = 0; i < 6; i++) {
            float x = 185 + i * 145;
            p.fillPoly(Glyph.hex(x, 930, 53), Glyph.withAlpha(Glyph.COLOR[i], 32));
            p.strokePoly(Glyph.hex(x, 930, 53), Glyph.COLOR[i], 2f);
            Kawaii.draw(p, i, x, 930, 34, Glyph.COLOR[i], 1f, 0.35f);
        }
        Png.write(new File("app-store/video/landscape-background.png"), p.resolve(), 1920, 1080);
    }
}
