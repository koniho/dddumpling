package com.dddumpling.game;

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
 * them ends on "AND", so they are joined back into prose before anything else happens.
 *
 * The story then goes out as <em>one</em> utterance, punctuation intact. An earlier version cut it
 * into sentences and gave each one its own pitch, on the theory that the arc would carry the
 * feeling. It did the opposite: an engine works out its intonation from the whole sentence it is
 * handed, so short fragments came out flat, and stepping the pitch between them turned the flatness
 * into something that lurched. Handing over the punctuation and letting the engine do the
 * inflection is both simpler and better, and it is the only lever that actually reaches prosody.
 */
final class Narration {

    private Narration() {}

    /** Fixed chunks before the story itself: the name, then where it lives. */
    static final int PREAMBLE = 2;
    /** And the story, whole. Three utterances in total, always. */
    static final int CHUNKS = PREAMBLE + 1;

    /**
     * How high the voice sits. 1 is the engine's own; this is nearly the top of the range every
     * engine supports, which is the chipmunk that was asked for. One value for the whole reading —
     * see the class comment on why the pitch does not move any more.
     */
    static final float PITCH = 1.9f;
    /**
     * And how fast. Left at the engine's own speed rather than sped up with the pitch: high and
     * fast together stops being a voice and starts being a noise, and this still has to be followed
     * word for word.
     */
    static final float RATE = 1.0f;

    /** A bright, cutesy name call, kept brisk enough for the short greeting. */
    static final float NAME_PITCH = 1.65f, NAME_RATE = 1.05f;
    static String name(int entry) {
        return (Collect.NAME[entry]+"!").toLowerCase(java.util.Locale.US);
    }

    /**
     * What to say, in order: the name, where it lives, and the story in one piece.
     *
     * Three utterances rather than one because the first two are not prose — a name and a place are
     * announced, and each wants a beat after it. The story is not cut up at all.
     */
    static String[] lines(int i) {
        String[] out = new String[CHUNKS];
        // The name is the title of the piece, so it is announced rather than read.
        out[0] = say(Collect.NAME[i] + "!");
        out[1] = say(Lore.WHERE[i] + ".");
        out[PREAMBLE] = say(join(Lore.STORY[i]));
        return out;
    }

    /**
     * Speed for chunk {@code k}. The only thing that still varies, and only across utterance
     * boundaries, where it cannot disturb the engine's own intonation: the name is announced a
     * little slower than the rest is read.
     */
    static float rate(int k) {
        return k == 0 ? 0.90f : RATE;
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

}
