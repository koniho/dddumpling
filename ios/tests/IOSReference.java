package com.dddumpling.game;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Deterministic Java-side companion to DDRunRenderChecks.
 *
 * RasterPainter intentionally has a bitmap approximation of the game font, so
 * these images are scene and geometry references rather than pixel-for-pixel
 * comparisons with UIKit's Bungee/Core Text output.
 */
public final class IOSReference {
    private static final int WIDTH = 640, HEIGHT = 1400, FRAMES = 30;
    private static final float STEP = 1f / 60f;
    private static final String[] SCENES = {
            "title", "play", "stage:5", "stage:10", "stage:15", "stage:20",
            "case", "stars", "steamer"
    };

    private IOSReference() {}

    private static RasterPainter render(String scene) {
        // Fresh state for every scene makes the seed the whole simulation input.
        IOSGame game = new IOSGame(null, null, 42L);
        game.layout(WIDTH, HEIGHT, 0, 0, 0, 0);
        game.debugScene(scene);
        for (int i = 0; i < FRAMES; i++) game.update(STEP);
        RasterPainter painter = new RasterPainter(WIDTH, HEIGHT, 1);
        game.draw(painter);
        return painter;
    }

    private static int changedPixels(int[] pixels) {
        int first = pixels[0], changed = 0;
        for (int pixel : pixels) if (pixel != first) changed++;
        return changed;
    }

    public static void main(String[] args) throws IOException {
        File output = new File(args.length == 0 ? "out/ios-reference" : args[0]);
        if (!output.isDirectory() && !output.mkdirs())
            throw new IOException("Could not create " + output);

        StringBuilder manifest = new StringBuilder();
        manifest.append("DDDUMPLING Java raster reference\n")
                .append("size=640x1400 logical pixels\n")
                .append("supersample=1\nseed=42\nupdates=30@1/60\n")
                .append("font=RasterPainter bitmap approximation; differs from iOS Bungee-Regular\n")
                .append("scenes=\n");
        for (String scene : SCENES) {
            int[] pixels = render(scene).resolve();
            int changed = changedPixels(pixels);
            if (changed == 0) throw new AssertionError(scene + " rendered a single colour");
            File png = new File(output, scene + ".png");
            Png.write(png, pixels, WIDTH, HEIGHT);
            manifest.append(scene).append(".png changedPixels=").append(changed).append('\n');
            System.out.println("wrote " + png + " (" + changed + " changed pixels)");
        }
        FileWriter writer = new FileWriter(new File(output, "manifest.txt"));
        try { writer.write(manifest.toString()); }
        finally { writer.close(); }
    }
}
