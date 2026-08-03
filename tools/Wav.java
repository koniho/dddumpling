package com.sram.hexatype;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Minimal WAV writer, so the synthesised sounds can be auditioned without an install. */
final class Wav {

    private Wav() {}

    static void write(File f, short[] pcm, int rate) throws IOException {
        int dataBytes = pcm.length * 2;
        DataOutputStream out =
                new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            out.writeBytes("RIFF");
            le32(out, 36 + dataBytes);
            out.writeBytes("WAVE");

            out.writeBytes("fmt ");
            le32(out, 16);
            le16(out, 1);            // PCM
            le16(out, 1);            // mono
            le32(out, rate);
            le32(out, rate * 2);     // byte rate
            le16(out, 2);            // block align
            le16(out, 16);           // bits

            out.writeBytes("data");
            le32(out, dataBytes);
            for (int i = 0; i < pcm.length; i++) le16(out, pcm[i]);
        } finally {
            out.close();
        }
    }

    private static void le16(DataOutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void le32(DataOutputStream out, int v) throws IOException {
        le16(out, v & 0xFFFF);
        le16(out, (v >>> 16) & 0xFFFF);
    }
}
