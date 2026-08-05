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
        check("an outer key dismisses instead of scrolling",
                !c.storyOpen() && c.caseIndex == 0);
        check("the clock resets on close", c.storyT == 0f);

        // Starting a run and coming back must not leave one open.
        c.openStory();
        c.startGame();
        check("starting a run closes it", !c.storyOpen());
        c.toTitle();
        c.openStory();
        c.toTitle();
        check("returning to the title closes it", !c.storyOpen());

        // Emptying the case has to take the story with it: it describes something now unowned.
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
}
