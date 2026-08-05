package com.sram.hexatype;

/**
 * The story popup read aloud: what to say, and how to say it.
 *
 * Pure, so both the words and the delivery can be asserted for all thirty entries. The device's
 * speech engine only takes strings and a pitch and rate per utterance — everything that decides
 * what those are lives here rather than in {@link Audio}.
 *
 * Two things it has to fix up. The text on the panel is upper case, and a speech engine reads a
 * short all-capitals word as an initialism — "MUM" comes out M-U-M — so everything is lowered
 * before it goes out. And the four story lines are typographic rather than grammatical: one of
 * them ends on "AND". Reading a line per utterance therefore put a pause in the middle of a
 * sentence, which is exactly what makes a voice sound like it is reading a list. They are joined
 * and re-split on full stops instead.
 */
final class Narration {

    private Narration() {}

    /** Fixed chunks before the story itself: the name, then where it lives. */
    static final int PREAMBLE = 2;

    /**
     * What to say, in order: the name, where it lives, then one chunk per sentence of the story.
     * Each is its own utterance so it can have its own delivery.
     */
    static String[] lines(int i) {
        String[] sentences = split(join(Lore.STORY[i]));
        String[] out = new String[PREAMBLE + sentences.length];
        // The name is the title of the piece, so it is announced rather than read.
        out[0] = say(Collect.NAME[i] + "!");
        out[1] = say(Lore.WHERE[i] + ".");
        for (int k = 0; k < sentences.length; k++) out[PREAMBLE + k] = say(sentences[k]);
        return out;
    }

    /**
     * Cute is high, and this is high throughout: 1 is the engine's own voice and everything here
     * sits well above it. The name is the brightest thing in the reading, the setting drops back
     * like an aside, and the story settles line by line and lifts again on a question or an
     * exclamation — that arc is what carries the feeling, since the engine gives no other handle
     * on it.
     */
    static float pitch(String[] lines, int k) {
        if (k == 0) return 1.50f;
        if (k == 1) return 1.22f;
        int nth = k - PREAMBLE;
        float p = 1.38f - 0.05f * nth;
        String s = lines[k];
        if (s.endsWith("!")) p += 0.12f;
        else if (s.endsWith("?")) p += 0.18f;
        // The last sentence is the punchline of every one of these, so it comes back up.
        if (k == lines.length - 1) p += 0.06f;
        return p < 1.12f ? 1.12f : p;
    }

    /**
     * Words per second, near enough: 1 is the engine's default and this reads a little under it
     * throughout, which is what "clearly" costs. The name is slower still, and the last sentence
     * slows again to land.
     */
    static float rate(String[] lines, int k) {
        if (k == 0) return 0.82f;
        if (k == 1) return 0.94f;
        float r = 0.92f;
        if (lines[k].endsWith("!")) r += 0.05f;
        if (k == lines.length - 1) r -= 0.05f;
        return r;
    }

    /**
     * Silence after chunk {@code k}, in milliseconds. Longest after the name, so it lands as a
     * title; a beat between sentences; nothing after the last one.
     */
    static int gapMs(String[] lines, int k) {
        if (k >= lines.length - 1) return 0;
        if (k == 0) return 280;
        if (k == 1) return 200;
        return 150;
    }

    /** Lower case, and trimmed. See the class comment for why the case matters. */
    private static String say(String s) {
        return s.trim().toLowerCase();
    }

    private static String join(String[] lines) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) b.append(' ');
            b.append(lines[i]);
        }
        return b.toString();
    }

    /**
     * Splits on sentence ends, keeping the punctuation — the engine needs it to know whether to
     * fall or rise. Anything left over at the end comes back as a chunk of its own, so a story
     * that forgets its final full stop is still read rather than dropped.
     */
    private static String[] split(String text) {
        int count = 0;
        for (int pass = 0; pass < 2; pass++) {
            String[] out = pass == 0 ? null : new String[count];
            count = 0;
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch != '.' && ch != '!' && ch != '?') continue;
                // Run on through "?!" and the like, so it is one end and not two.
                while (i + 1 < text.length() && isEnd(text.charAt(i + 1))) i++;
                if (out != null) out[count] = text.substring(start, i + 1).trim();
                count++;
                start = i + 1;
            }
            if (text.substring(start).trim().length() > 0) {
                if (out != null) out[count] = text.substring(start).trim();
                count++;
            }
            if (out != null) return out;
        }
        return new String[0];
    }

    private static boolean isEnd(char ch) {
        return ch == '.' || ch == '!' || ch == '?';
    }
}
