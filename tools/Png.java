package com.dddumpling.game;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/** Minimal PNG writer (8-bit RGB, no dependencies) so the harness can emit viewable frames. */
final class Png {

    private Png() {}

    static void write(File f, int[] argb, int w, int h) throws IOException {
        byte[] raw = new byte[h * (1 + w * 3)];
        int o = 0;
        for (int y = 0; y < h; y++) {
            raw[o++] = 0; // filter: none
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int c = argb[row + x];
                raw[o++] = (byte) (c >> 16);
                raw[o++] = (byte) (c >> 8);
                raw[o++] = (byte) c;
            }
        }

        DataOutputStream out =
                new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            out.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});

            ByteArrayOutputStream ih = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(ih);
            d.writeInt(w);
            d.writeInt(h);
            d.write(8);    // bit depth
            d.write(2);    // colour type: truecolour
            d.write(0);
            d.write(0);
            d.write(0);
            chunk(out, "IHDR", ih.toByteArray());
            chunk(out, "IDAT", deflate(raw));
            chunk(out, "IEND", new byte[0]);
        } finally {
            out.close();
        }
    }

    private static byte[] deflate(byte[] data) {
        Deflater def = new Deflater(Deflater.BEST_SPEED);
        def.setInput(data);
        def.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length / 2);
        byte[] buf = new byte[65536];
        while (!def.finished()) {
            int n = def.deflate(buf);
            bos.write(buf, 0, n);
        }
        def.end();
        return bos.toByteArray();
    }

    private static void chunk(DataOutputStream out, String type, byte[] body) throws IOException {
        out.writeInt(body.length);
        byte[] t = type.getBytes("US-ASCII");
        out.write(t);
        out.write(body);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(body);
        out.writeInt((int) crc.getValue());
    }
}
