package com.sram.hexatype;

/** The thirty stories, and the popup that shows them. */
final class TestLore extends Check {

    static void stories(Layout L) {
        group("collectible stories");
        check("one story per collectible", Lore.STORY.length == Collect.COUNT
                && Lore.WHERE.length == Collect.COUNT && Lore.BEAT.length == Collect.COUNT
                && Lore.PARTNER.length == Collect.COUNT);

        boolean shaped = true, filled = true, fits = true, renders = true;
        int widest = 0;
        String worst = "";
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Lore.STORY[i].length != Lore.LINES) shaped = false;
            if (Lore.WHERE[i] == null || Lore.WHERE[i].length() == 0) filled = false;
            if (Lore.WHERE[i].length() > Lore.MAX_LINE) {
                fits = false;
                System.out.println("    setting too wide: " + Lore.WHERE[i]);
            }
            if (!printable(Lore.WHERE[i])) {
                renders = false;
                System.out.println("    unrenderable setting: " + Lore.WHERE[i]);
            }
            for (int k = 0; k < Lore.STORY[i].length; k++) {
                String line = Lore.STORY[i][k];
                if (line == null || line.length() == 0) filled = false;
                if (line.length() > widest) {
                    widest = line.length();
                    worst = line;
                }
                if (line.length() > Lore.MAX_LINE) {
                    fits = false;
                    System.out.println("    line too wide: " + line);
                }
                // The harness font has no bitmap for anything outside its own set, so a
                // character it does not know silently vanishes from every preview PNG —
                // apostrophes are the easy way to trip this.
                if (!printable(line)) {
                    renders = false;
                    System.out.println("    unrenderable line: " + line);
                }
            }
        }
        check("every story is exactly " + Lore.LINES + " lines", shaped);
        check("no blank settings or lines", filled);
        check("every line fits the popup", fits);
        check("every character can actually be drawn", renders);
        System.out.printf("    widest line is %d of %d: %s%n", widest, Lore.MAX_LINE, worst);

        // Thirty stories that were meant to be unique, so a copy-paste slip should fail here
        // rather than be discovered by a reader.
        boolean unique = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            for (int k = i + 1; k < Collect.COUNT; k++) {
                if (Lore.STORY[i][0].equals(Lore.STORY[k][0])) unique = false;
                if (Lore.WHERE[i].equals(Lore.WHERE[k])) unique = false;
            }
        }
        check("no story or setting is repeated", unique);

        // Every story is supposed to carry the same four things. Family is the one that can be
        // checked mechanically, so it is.
        String[] kin = {"SISTER", "BROTHER", "COUSIN", "FAMILY", "MUM", "DAD", "GRANDMA",
                "TWINS", "SIBLING", "PARENTS", "KID", "BUNCH"};
        int withKin = 0;
        for (int i = 0; i < Collect.COUNT; i++) {
            String all = String.join(" ", Lore.STORY[i]);
            for (int k = 0; k < kin.length; k++) {
                if (all.contains(kin[k])) {
                    withKin++;
                    break;
                }
            }
        }
        System.out.printf("    %d of %d stories name somebody in the family%n", withKin,
                Collect.COUNT);
        check("every story has family in it", withKin == Collect.COUNT);
    }

    static void casting(Layout L) {
        group("story casting");
        boolean inRange = true, notSelf = true, sameFamily = true, thirdOk = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            int mate = Lore.PARTNER[i];
            if (mate < 0 || mate >= Collect.COUNT) inRange = false;
            if (mate == i) notSelf = false;
            // A scene must not put a bao bun in with the stress balls.
            if (Collect.FAMILY[mate] != Collect.FAMILY[i]) {
                sameFamily = false;
                System.out.println("    cross-family pairing: " + Collect.NAME[i] + " + "
                        + Collect.NAME[mate]);
            }
            int third = Lore.third(i);
            if (third < 0 || third >= Collect.COUNT) thirdOk = false;
            if (Collect.FAMILY[third] != Collect.FAMILY[i]) thirdOk = false;
            if (Lore.BEAT[i] < 0 || Lore.BEAT[i] >= Lore.BEATS) inRange = false;
        }
        check("every partner and beat is a real one", inRange);
        check("nobody stars opposite themselves", notSelf);
        check("scenes stay inside the family", sameFamily);
        check("the third figure is in range and in the family", thirdOk);

        // An unused beat is either a casting gap or dead drawing code.
        boolean[] seen = new boolean[Lore.BEATS];
        for (int i = 0; i < Collect.COUNT; i++) seen[Lore.BEAT[i]] = true;
        boolean all = true;
        for (int i = 0; i < seen.length; i++) if (!seen[i]) all = false;
        check("every beat gets used", all);

        // Every scene has three slots and all three must be different characters, whether or
        // not the beat currently uses all of them. This is the assertion that catches the
        // failure the derived third figure had: most pairings are mutual, so partner-of-partner
        // handed back the entry itself and the scene cast one character twice.
        boolean distinct = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            int mate = Lore.PARTNER[i], third = Lore.third(i);
            if (third == i || third == mate) {
                distinct = false;
                System.out.println("    duplicate casting: " + Collect.NAME[i] + " with "
                        + Collect.NAME[mate] + " and " + Collect.NAME[third]);
            }
        }
        check("all three figures in a scene are different", distinct);
        check("the third table is the right length", Lore.THIRD.length == Collect.COUNT);
    }

    static void popup(Layout L) {
        group("story popup");
        Mem store = new Mem();
        store.collected = 0b101L;          // entries 0 and 2
        GameCore c = new GameCore(store, 61L);
        check("no story on screen to begin with", !c.storyOpen() && c.story < 0);
        // Stories come off the shelf, so the case has to be out for any of this.
        c.openCase();

        // Uncollected: nothing opens, because the shelf will not even name it.
        c.caseIndex = 1;
        c.openStory();
        check("an uncollected entry has no story", !c.storyOpen());

        c.caseIndex = 0;
        c.openStory();
        check("a collected entry opens", c.storyOpen() && c.story == 0);
        check("the clock starts at zero", c.storyT == 0f);
        advance(c, L, 0.5f);
        check("the clock runs while it is open", c.storyT > 0.4f);

        // Modal: the shelf must not move out from under the panel.
        c.scrollCase(1);
        check("browsing is blocked behind a story", c.caseIndex == 0 && c.storyOpen());
        c.openStory();
        check("opening twice does not restart it", c.storyT > 0.4f);

        // A key press dismisses rather than acting. Getting this wrong starts a run from
        // behind the panel, which is the one thing a modal must not allow.
        c.tapKey(2, L);
        check("an inner key dismisses instead of starting",
                !c.storyOpen() && c.state == GameCore.TITLE);
        c.openStory();
        c.tapKey(0, L);
        check("any key dismisses instead of stepping the shelf",
                !c.storyOpen() && c.caseIndex == 0);
        check("the clock resets on close", c.storyT == 0f);

        // Starting a run and coming back must not leave one open.
        c.openStory();
        c.startGame();
        check("starting a run closes it", !c.storyOpen());
        c.toTitle();
        c.openCase();
        c.openStory();
        c.toTitle();
        check("returning to the title closes it", !c.storyOpen());

        // Emptying the case has to take the story with it: it describes something now unowned.
        c.openCase();
        c.openStory();
        check("open again for the clear test", c.storyOpen());
        c.openSettings();
        c.tapClearCase();
        c.tapClearCase();
        check("clearing the collection closes it", !c.storyOpen());
        c.closeSettings();
        c.openStory();
        check("and it cannot be reopened once uncollected", !c.storyOpen());

        // Only the title screen tells stories.
        GameCore d = new GameCore(store, 63L);
        d.collected = Collect.MASK;
        d.startGame();
        d.openStory();
        check("no stories during play", !d.storyOpen());

        // The hit target has to sit on the focused entry and nowhere near the keys.
        check("the focused entry is tappable",
                Showcase.inFocus(L, L.w / 2f, Showcase.focusCy(L)));
        check("the sky above it is not", !Showcase.inFocus(L, L.w / 2f, L.topSafe + 1f));
        boolean clearOfKeys = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (Showcase.inFocus(L, L.keyX[g], L.keyY[g])) clearOfKeys = false;
        }
        check("no key sits inside the story target", clearOfKeys);
    }

    /** The story read aloud: the words, the delivery, and when it starts and stops. */
    static void narration(Layout L) {
        group("story narration");
        Mem store = new Mem();
        store.collected = Collect.MASK;
        GameCore c = new GameCore(store, 71L);
        Ear ear = new Ear();
        c.sound = ear;
        c.openCase();
        c.caseIndex = 4;
        c.openStory();
        check("opening a story starts the reading",
                ear.narrations == 1 && ear.narrated == 4);
        c.closeStory();
        check("and dismissing it stops mid-sentence", ear.hushes == 1);
        // Called on the way into and out of half the states in the game, and must stay quiet
        // about it when there was nothing being read.
        c.closeStory();
        c.startGame();
        c.toTitle();
        check("nothing to hush when no story is open", ear.hushes == 1);

        // Every entry has to be sayable. The panel's text is upper case, which a speech engine
        // reads as an initialism, and its lines break mid-sentence.
        boolean lowered = true, punctuated = true, named = true, full = true;
        boolean noDanglingAnd = true, whole = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            String[] said = Narration.lines(i);
            if (said.length != Narration.CHUNKS) full = false;
            if (!said[0].equals(Collect.NAME[i].toLowerCase() + "!")) named = false;
            for (int k = 0; k < said.length; k++) {
                String t = said[k];
                if (!t.equals(t.toLowerCase())) lowered = false;
                if (t.length() == 0 || !t.equals(t.trim())) full = false;
                char last = t.charAt(t.length() - 1);
                if (last != '.' && last != '!' && last != '?') punctuated = false;
                // The giveaway that a typographic line was handed over as if it were a sentence.
                if (t.endsWith(" and.") || t.endsWith(" and")) noDanglingAnd = false;
            }
            // The story goes over whole, punctuation and all, because that is what the engine
            // works its intonation out from. Every one of these is more than one sentence, so a
            // chunk that had been split would not contain a full stop before its last character.
            String story = said[Narration.PREAMBLE];
            if (story.indexOf('.') == story.length() - 1) whole = false;
        }
        check("every entry is spoken in lower case", lowered);
        check("every chunk is a trimmed, non-empty line", full);
        check("the name leads the reading", named);
        check("every chunk ends on punctuation the engine can hear", punctuated);
        check("no chunk breaks where a written line does", noDanglingAnd);
        check("the story is handed over in one piece", whole);

        // The delivery: one pitch for the whole reading, high, and never faster than the engine's
        // own speed. The pitch used to step between utterances and that is exactly what made the
        // inflection lurch — an engine intones a whole utterance, so stepping between short ones
        // reads as flat fragments at arbitrary heights.
        check("the voice is pitched right up", Narration.PITCH > 1.6f && Narration.PITCH <= 2f);
        check("and not sped up with it", Narration.RATE <= 1f);
        String[] said = Narration.lines(0);
        boolean paced = true;
        for (int k = 0; k < said.length; k++) {
            if (Narration.rate(k) < 0.85f || Narration.rate(k) > 1f) paced = false;
        }
        check("every chunk is paced for following word by word", paced);
        check("the name is announced a little slower", Narration.rate(0) < Narration.rate(1));
        boolean gapped = true;
        for (int k = 0; k < said.length; k++) {
            int gap = Narration.gapMs(said, k);
            if (k == said.length - 1 ? gap != 0 : gap <= 0) gapped = false;
        }
        check("a beat between chunks and none after the last", gapped);
        check("the longest beat follows the name",
                Narration.gapMs(said, 0) > Narration.gapMs(said, 1));

        // Printed because this is the one thing in the game the PNGs cannot show. Reading it
        // back is how the phrasing gets checked without a device that can talk.
        System.out.printf("    voice: pitch %.2f%n", Narration.PITCH);
        for (int k = 0; k < said.length; k++) {
            System.out.printf("    say  rate %.2f  then %3dms  \"%s\"%n",
                    Narration.rate(k), Narration.gapMs(said, k), said[k]);
        }
    }
}
