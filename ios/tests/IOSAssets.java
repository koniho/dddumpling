package com.dddumpling.game;

import java.io.File;

/** Exports the existing launcher artwork at native App Store resolution. */
public final class IOSAssets {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        RasterPainter painter = new RasterPainter(size, size, 4);
        painter.clear(Draw.BG);
        float r = size * .38f;
        Kawaii.draw(painter, Kawaii.DUMPLING, size * .5f, size * .5f + r * .05f,
                r, 0xFF9EE65B, 1f, .35f);
        Png.write(new File(args[0]), painter.resolve(), size, size);
    }
}
