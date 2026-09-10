package com.sram.hexatype;

/** The collectible catalogue, the blind-box odds, the display case and its persistence. */
final class TestCollect extends Check {

    /**
     * Empties the wave so the interlude opens and the spinner has landed, without having to
     * play a whole stage first. The interlude only follows a cleared wave, so the quota has to
     * be marked out as well as the field.
     */
    private static boolean toBonus(GameCore c, Layout L) {
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        return advanceToMash(c, L);
    }

    /** Alternates the wanted pair enough times to lift the lid clear. */
    private static void mash(GameCore c) {
        for (int i = 0; i < c.steamer.goal() * 2 + 4; i++) c.tapBonus(c.steamer.wanted());
        c.swipeBonus();
    }

    static void catalogue(Layout L) {
        group("collectible catalogue");
        check("thirty blind, five star, ten cube and four boss entries", Collect.BLIND_COUNT == 30
                && Collect.STAR_COUNT == 5 && Collect.CUBE_COUNT == 10 && Collect.BOSS_COUNT == Boss.COUNT && Collect.COUNT == 49);
        check("every table is the same length",
                Collect.NAME.length == Collect.COUNT && Collect.FAMILY.length == Collect.COUNT
                        && Collect.SHAPE.length == Collect.COUNT
                        && Collect.FINISH.length == Collect.COUNT
                        && Collect.TIER.length == Collect.COUNT
                        && Collect.BODY.length == Collect.COUNT
                        && Collect.ACCENT.length == Collect.COUNT);

        boolean named = true, unique = true, inRange = true, opaque = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Collect.NAME[i] == null || Collect.NAME[i].length() == 0) named = false;
            for (int k = i + 1; k < Collect.COUNT; k++) {
                if (Collect.NAME[i].equals(Collect.NAME[k])) unique = false;
            }
            if (Collect.SHAPE[i] < 0 || Collect.SHAPE[i] >= Collect.SHAPE_COUNT) inRange = false;
            if (Collect.FINISH[i] < 0 || Collect.FINISH[i] >= Collect.FINISH_COUNT) {
                inRange = false;
            }
            if (Collect.TIER[i] < 0 || Collect.TIER[i] >= Collect.TIER_NAME.length) {
                inRange = false;
            }
            if (Collect.FAMILY[i] < 0 || Collect.FAMILY[i] >= Collect.FAMILY_NAME.length) {
                inRange = false;
            }
            // Fully opaque, so fadeBy has the whole range to dim a neighbour through.
            if ((Collect.BODY[i] >>> 24) != 0xFF || (Collect.ACCENT[i] >>> 24) != 0xFF) {
                opaque = false;
            }
        }
        check("every entry is named", named);
        check("no two entries share a name", unique);
        check("every shape, finish, tier and family is a real one", inRange);
        check("every colour is opaque", opaque);

        // All fifteen shapes and all nine finishes earn their keep; an unused one is either a
        // catalogue gap or dead drawing code.
        boolean[] shapeSeen = new boolean[Collect.SHAPE_COUNT];
        boolean[] finishSeen = new boolean[Collect.FINISH_COUNT];
        int[] tierCount = new int[Collect.TIER_NAME.length];
        int[] familyCount = new int[Collect.FAMILY_NAME.length];
        for (int i = 0; i < Collect.COUNT; i++) {
            shapeSeen[Collect.SHAPE[i]] = true;
            finishSeen[Collect.FINISH[i]] = true;
            tierCount[Collect.TIER[i]]++;
            familyCount[Collect.FAMILY[i]]++;
        }
        boolean allShapes = true, allFinishes = true;
        for (int i = 0; i < shapeSeen.length; i++) if (!shapeSeen[i]) allShapes = false;
        for (int i = 0; i < finishSeen.length; i++) if (!finishSeen[i]) allFinishes = false;
        check("every shape is used", allShapes);
        check("every finish is used", allFinishes);

        boolean everyTier = true;
        for (int t = 0; t < tierCount.length; t++) if (tierCount[t] == 0) everyTier = false;
        check("every tier has entries", everyTier);
        check("exactly one grail", tierCount[Collect.GRAIL] == 1);
        check("commons outnumber chases", tierCount[Collect.COMMON] > tierCount[Collect.CHASE]);
        boolean everyFamily = true;
        for (int f = 0; f < familyCount.length; f++) {
            if (familyCount[f] < (f == Collect.BOSSES ? 4 : 5)) everyFamily = false;
        }
        check("all collectible families are properly stocked", everyFamily);
        System.out.printf("    tiers %d/%d/%d/%d/%d, families %d/%d/%d%n",
                tierCount[0], tierCount[1], tierCount[2], tierCount[3], tierCount[4],
                familyCount[0], familyCount[1], familyCount[2]);

        // Trinket fits banded finishes to an ellipse, so they only look right on a shape that
        // fills one. Nothing stops a new row breaking that but this.
        boolean bandsFit = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Collect.banded(Collect.FINISH[i]) && !Collect.roundish(Collect.SHAPE[i])) {
                bandsFit = false;
                System.out.println("    banded finish on a non-round shape: " + Collect.NAME[i]);
            }
        }
        check("banded finishes only sit on round shapes", bandsFit);

        boolean weightsFall = true;
        for (int t = 1; t <= Collect.GRAIL; t++) {
            if (Collect.TIER_WEIGHT[t] >= Collect.TIER_WEIGHT[t - 1]) weightsFall = false;
        }
        check("rarer tiers are strictly rarer", weightsFall);
    }

    static void ownedSet(Layout L) {
        group("collected set");
        check("nothing owned to begin with", Collect.owned(0L) == 0);
        check("an empty case is not complete", !Collect.complete(0L));
        check("the full mask is complete", Collect.complete(Collect.MASK));
        check("the mask covers exactly the entries",
                Long.bitCount(Collect.MASK) == Collect.COUNT);

        long m = 0L;
        boolean roundTrip = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Collect.has(m, i)) roundTrip = false;
            m = Collect.add(m, i);
            if (!Collect.has(m, i)) roundTrip = false;
            if (Collect.owned(m) != i + 1) roundTrip = false;
        }
        check("adding each entry once fills the case", roundTrip && Collect.complete(m));
        check("adding twice is a no-op", Collect.add(m, 3) == m);
        check("out-of-range indices are ignored",
                !Collect.has(m, -1) && !Collect.has(m, Collect.COUNT)
                        && Collect.add(0L, Collect.COUNT) == 0L);
        // A corrupt store must not be able to inflate the count past the catalogue.
        check("junk in the high bits is masked off",
                Collect.owned(-1L) == Collect.COUNT);
    }

    static void blindBox(Layout L) {
        group("blind box odds");
        java.util.Random rnd = new java.util.Random(77L);
        int[] tierHits = new int[Collect.TIER_NAME.length];
        boolean inRange = true;
        int draws = 60000;
        for (int i = 0; i < draws; i++) {
            int pick = Collect.roll(rnd, 0L);
            if (pick < 0 || pick >= Collect.COUNT) inRange = false;
            else tierHits[Collect.TIER[pick]]++;
        }
        check("every roll is a real entry", inRange);
        System.out.printf("    %d draws from an empty case: %d common, %d uncommon, %d rare, "
                + "%d chase, %d grail%n", draws, tierHits[0], tierHits[1], tierHits[2],
                tierHits[3], tierHits[4]);
        check("commons dominate", tierHits[Collect.COMMON] > tierHits[Collect.RARE]);
        check("rares beat chases", tierHits[Collect.RARE] > tierHits[Collect.CHASE]);
        check("chases beat the grail", tierHits[Collect.CHASE] > tierHits[Collect.GRAIL]);
        check("the grail is reachable", tierHits[Collect.GRAIL] > 0);

        // The re-roll is what makes early opens feel like progress: with one entry missing,
        // rolling should land on it far more often than its bare weight would suggest.
        long allButOne = Collect.MASK & ~(1L << 5);
        int found = 0;
        for (int i = 0; i < 2000; i++) {
            if (Collect.roll(rnd, allButOne) == 5) found++;
        }
        System.out.printf("    the one missing entry came up %d times in 2000%n", found);
        check("rolling favours what is missing", found > 2000 / Collect.COUNT);

        // And a full case must still hand something back rather than spinning or failing.
        boolean fullOk = true;
        for (int i = 0; i < 500; i++) {
            int pick = Collect.roll(rnd, Collect.MASK);
            if (pick < 0 || pick >= Collect.COUNT) fullOk = false;
        }
        check("a full case still rolls a valid entry", fullOk);
    }

    static void winning(Layout L) {
        group("winning a collectible");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 91L);
        c.startGame();
        check("no prize before the steamer opens", c.prize < 0);
        check("case starts empty", Collect.owned(c.collected) == 0);

        check("reached the interlude", toBonus(c, L));
        mash(c);
        check("freeing the dumpling awards a collectible", c.prize >= 0
                && c.prize < Collect.COUNT);
        check("the first one is always new", c.prizeNew);
        check("it lands in the case", Collect.has(c.collected, c.prize));
        check("the case is written straight to the store",
                store.collected == c.collected && store.collectedSaves == 1);
        check("the display case moves to the prize", c.caseIndex == c.prize);
        if (c.prize >= 0) {
            System.out.printf("    won %s (%s)%n", Collect.NAME[c.prize],
                    Collect.TIER_NAME[Collect.TIER[c.prize]]);
        }

        GameCore pools = new GameCore(new Mem(), 92L);
        pools.startGame();
        pools.stage = 4;
        Interlude.awardPrize(pools);
        check("before stage 5 the steamer keeps its original pool", pools.prize < Collect.BLIND_COUNT);
        Interlude.awardStarPrize(pools);
        check("before stage 5 the star path keeps its original pool",
                pools.prize >= Collect.STAR_FIRST && pools.prize < Collect.CUBE_FIRST);
        pools.stage = 5;
        pools.cubeUnlocked = true;
        boolean cubeOnly = true;
        for (int i = 0; i < 100; i++) {
            Interlude.awardPrize(pools);
            if (pools.prize < Collect.CUBE_FIRST || pools.prize >= Collect.COUNT) cubeOnly = false;
            Interlude.awardStarPrize(pools);
            if (pools.prize < Collect.CUBE_FIRST || pools.prize >= Collect.COUNT) cubeOnly = false;
        }
        check("both post-slime minigames award only gelatinous cubes", cubeOnly);
        pools.startGame();
        Interlude.awardPrize(pools);
        check("the next playthrough returns to the original pool", pools.prize < Collect.BLIND_COUNT);

        // With everything already owned, an open has to pay out instead of adding.
        GameCore d = new GameCore(new Mem(), 93L);
        d.startGame();
        d.collected = Collect.MASK;
        check("reached the interlude with a full case", toBonus(d, L));
        int before = d.score;
        mash(d);
        check("a duplicate is flagged as one", d.prize >= 0 && !d.prizeNew);
        check("a duplicate pays the consolation",
                d.score >= before + GameCore.FREE_BONUS + GameCore.DUPE_BONUS);
        check("a duplicate leaves the case as it was", Collect.complete(d.collected));

        // The collection is the one thing that outlives a run.
        Mem kept = new Mem();
        kept.collected = 0b1011L;
        GameCore e = new GameCore(kept, 95L);
        check("the case reloads from the store", Collect.owned(e.collected) == 3);
        e.startGame();
        check("starting a run keeps the case", Collect.owned(e.collected) == 3);
        check("starting a run clears the last prize", e.prize < 0);
    }

    /** The parade that closes a winning interlude, and that play waits for it. */
    static void parade(Layout L) {
        group("collection parade");
        Mem store = new Mem();
        // A few already in the case, so the new one has companions to line up with.
        store.collected = 0b1011011L;
        GameCore c = new GameCore(store, 131L);
        c.startGame();
        check("no parade scheduled at the start",
                c.paradeTimer == 0f && !c.bonusParading() && !c.bonusPrizeWon());

        check("reached the mash", toBonus(c, L));
        check("still no parade while mashing", !c.bonusPrizeWon());
        mash(c);
        check("winning schedules one", c.prize >= 0
                && Math.abs(c.paradeTimer - GameCore.PARADE_TIME) < 0.001f);
        check("and it counts as won for the rest of the interlude", c.bonusPrizeWon());
        check("but it has not started yet", !c.bonusParading());

        // Winning ends the round then and there: what is left is the escape, and no status
        // report, so the parade follows the win closely instead of minutes later.
        check("the round is down to the escape", c.bonusEscape() && !c.bonusMashing());
        check("no status report on a won round", !c.bonusStatus());
        check("the escape is all that is left",
                Math.abs(c.bonusTimer - c.steamer.freedT) < 0.001f);
        float before = c.paradeTimer;
        advance(c, L, 0.5f);
        check("the parade does not tick during the escape", c.paradeTimer == before);

        // Run the interlude out. The state must stay in BONUS for the parade rather than
        // dropping into play the moment the countdown hits zero.
        int wait = 0;
        for (; wait < 60 * 30 && !c.bonusParading(); wait++) c.update(DT, L);
        check("the parade starts when the escape ends", c.bonusParading());
        System.out.printf("    parade begins %.2fs after the win%n", (wait + 30) * DT);
        check("and that is promptly, not seconds later", wait * DT < 2.5f);
        check("play has not resumed", c.state == GameCore.BONUS);
        check("the stage has not turned over", c.stage == 1);
        check("progress starts at the beginning", c.paradeProgress() < 0.1f);

        // It runs through its three movements and only then hands over.
        boolean sawIn = false, sawJoin = false, sawOff = false, monotonic = true;
        float last = -1f;
        for (int i = 0; i < 60 * 30 && c.bonusParading(); i++) {
            float t = c.paradeProgress();
            if (t < last) monotonic = false;
            last = t;
            if (t < Parade.IN_END) sawIn = true;
            else if (t < Parade.JOIN_END) sawJoin = true;
            else sawOff = true;
            c.update(DT, L);
        }
        check("progress only ever moves forward", monotonic);
        check("all three movements are seen", sawIn && sawJoin && sawOff);
        check("play resumes once they have gone", c.state == GameCore.PLAY);
        check("and the stage turned over then", c.stage == 2);
        check("the parade timer is spent", c.paradeTimer == 0f);
        check("progress reads zero outside a parade", c.paradeProgress() == 0f);

        // An interlude that ran out of time gets no parade and no delay.
        GameCore d = new GameCore(new Mem(), 133L);
        d.startGame();
        toBonus(d, L);
        check("nothing won", d.prize < 0 && !d.bonusPrizeWon());
        int frames = 0;
        for (; frames < 60 * 30 && d.state == GameCore.BONUS; frames++) d.update(DT, L);
        check("a lost round goes straight back to play", d.state == GameCore.PLAY);
        check("with no parade at all", d.paradeTimer == 0f);

        // The line: companions come from the collection, never include the prize, and are
        // capped at what fits.
        GameCore e = new GameCore(new Mem(), 135L);
        e.collected = Collect.MASK;
        e.prize = 5;
        int[] out = new int[Parade.LINE - 1];
        int n = Parade.companions(e, out);
        check("a full case fills the line", n == out.length);
        boolean clean = true;
        for (int i = 0; i < n; i++) {
            if (out[i] == e.prize) clean = false;
            if (!Collect.has(e.collected, out[i])) clean = false;
            for (int k = i + 1; k < n; k++) if (out[i] == out[k]) clean = false;
        }
        check("no duplicates, no prize, nothing uncollected", clean);
        check("the line starts next to the prize in the catalogue", n > 0 && out[0] == 6);

        // The very first win has nobody to line up with, which must not break the line.
        e.collected = 1L << 5;
        check("a first win parades alone", Parade.companions(e, out) == 0);
    }

    static void displayCase(Layout L) {
        group("display case");
        GameCore c = new GameCore(new Mem(), 97L);
        check("opens on the title screen", c.state == GameCore.TITLE);
        check("opens on the first entry", c.caseIndex == 0);
        check("with the case itself shut", !c.caseOpen && c.caseFade == 0f && !c.caseShown());

        // Shut, nothing browses it. The badge is the only way in, which is what keeps the
        // collection out of the way of somebody who came to play.
        c.scrollCase(1);
        check("a shut case does not step", c.caseIndex == 0);
        c.caseTo(7);
        check("nor jump", c.caseIndex == 0);
        c.beginCaseDrag(L.w / 2f);
        c.caseDragTo(L.w, L);
        check("nor drag", c.caseIndex == 0 && !c.caseDragging);

        c.openCase();
        check("the badge opens it", c.caseOpen);
        check("and it fades in rather than appearing", c.caseFade == 0f);
        advance(c, L, 0.5f);
        check("the fade reaches full", c.caseFade == 1f && c.caseShown());
        check("the fade is brief", 1f / GameCore.CASE_FADE_RATE < 0.4f);

        c.scrollCase(1);
        check("scrolling right advances to the next bao variant", c.caseIndex == 2);
        check("the shelf slides in from the right", c.caseSlide > 0f);
        advance(c, L, 1f);
        check("the slide settles", c.caseSlide == 0f);

        c.scrollCase(-1);
        check("scrolling left goes back", c.caseIndex == 0);
        check("the shelf slides in from the left", c.caseSlide < 0f);

        c.scrollCase(-1);
        check("scrolling off the front wraps within the character row",
                c.caseIndex == 12);
        c.scrollCase(1);
        check("and back round to the front", c.caseIndex == 0);

        boolean wrapped = true;
        for (int i = -3; i < Collect.COUNT + 3; i++) {
            int w = Showcase.wrap(i);
            if (w < 0 || w >= Collect.COUNT) wrapped = false;
        }
        check("wrap always lands inside the catalogue", wrapped);

        boolean balanced = Showcase.ROW_NAME.length == 7;
        for (int row = 0; row < Showcase.ROW_NAME.length; row++)
            balanced &= Showcase.columns(row) >= (row == 6 ? 4 : 5) && Showcase.columns(row) <= 10;
        check("seven categories hold between four and ten collectibles each", balanced);
        boolean fruitRow = true, candyRow = true;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Collect.FAMILY[i] == Collect.FRUITS && Showcase.row(i) != Showcase.FRUIT_ROW) fruitRow = false;
            if (Collect.FAMILY[i] == Collect.GLOBS && Showcase.row(i) != Showcase.CANDY_ROW) candyRow = false;
        }
        check("all nine fruits share one row", fruitRow && Showcase.columns(Showcase.FRUIT_ROW) == 9);
        check("all eight candies share one row", candyRow && Showcase.columns(Showcase.CANDY_ROW) == 8);
        boolean[] seen = new boolean[Collect.COUNT];
        for (int row = 0; row < Showcase.ROW_NAME.length; row++) {
            c.caseTo(Showcase.entry(row, 0));
            for (int col = 0; col < Showcase.columns(row); col++) {
                check("each row holds only its character category", Showcase.row(c.caseIndex) == row);
                check("grid entries appear exactly once", !seen[c.caseIndex]);
                seen[c.caseIndex] = true;
                c.scrollCase(1);
            }
            check("a row wraps to its first variant", Showcase.column(c.caseIndex) == 0);
        }
        boolean all = true;
        for (boolean found : seen) if (!found) all = false;
        check("rows and columns reach the entire collection", all);
        c.caseTo(0);
        c.scrollCaseRow(-1);
        check("up wraps to the final character row", Showcase.row(c.caseIndex) == Showcase.ROW_NAME.length - 1);
        c.scrollCaseRow(1);
        check("down wraps to the first row", c.caseIndex == 0);
        c.caseTo(12);
        c.scrollCaseRow(1);
        check("shorter rows clamp to their last valid variant", c.caseIndex == 10);
        c.caseTo(0);

        c.closeCase();
        check("closing starts the fade out", !c.caseOpen && c.caseFade > 0f);
        advance(c, L, 0.5f);
        check("and it goes away", c.caseFade == 0f && !c.caseShown());
        c.scrollCase(1);
        check("and stops browsing with it", c.caseIndex == 0);
    }

    /** Browsing by touch: what the tap targets are, and what a drag does. */
    static void caseTouch(Layout L) {
        group("browsing by touch");
        Mem store = new Mem();
        store.collected = Collect.MASK;
        GameCore c = new GameCore(store, 99L);
        float cx = L.w / 2f, cy = Showcase.focusCy(L);

        // At rest the badge is dead centre, where the shelf appears.
        check("the badge is where the case will open", Showcase.inIcon(L, 0f, cx, cy));
        check("the sky above it is not", !Showcase.inIcon(L, 0f, cx, L.topSafe + 1f));

        // It drifts, so everything about it has to hold all the way round the arc.
        float lo = L.w, hi = 0f, lowest = 0f;
        boolean clearOfKeys = true, onScreen = true;
        for (float t = 0; t < Showcase.ARC_TIME; t += Showcase.ARC_TIME / 240f) {
            float ix = Showcase.iconCx(L, t);
            lo = Math.min(lo, ix);
            hi = Math.max(hi, ix);
            lowest = Math.max(lowest, Showcase.iconCy(L, t));
            if (ix - Showcase.iconHalfW(L) < 0 || ix + Showcase.iconHalfW(L) > L.w) {
                onScreen = false;
            }
            for (int g = 0; g < Glyph.COUNT; g++) {
                if (Showcase.inIcon(L, t, L.keyX[g], L.keyY[g])) clearOfKeys = false;
            }
        }
        check("the arc spans 30% of the width",
                Math.abs((hi - lo) - L.w * Showcase.ARC_SPAN) < L.w * 0.005f);
        check("and keeps the whole case on screen", onScreen);
        check("no key sits inside it, wherever it has drifted", clearOfKeys);
        check("it dips as it swings out, being an arc", lowest > cy);


        c.openCase();
        advance(c, L, 0.5f);

        check("empty glass does not step between entries",
                Showcase.hit(L, cx - Showcase.padX(L) * 0.8f, cy) == Showcase.HIT_NONE);
        check("the selected tile opens its story",
                Showcase.hit(c, L, cx, cy) == Showcase.HIT_FOCUS);
        check("the X closes",
                Showcase.hit(L, Showcase.closeCx(L), Showcase.closeCy(L)) == Showcase.HIT_CLOSE);
        check("the sky is outside", Showcase.hit(L, cx, L.topSafe + 1f) == Showcase.HIT_OUTSIDE);
        check("and so is the deck", Showcase.hit(L, cx, L.deckTop + 1f) == Showcase.HIT_OUTSIDE);
        // The caption belongs to the panel: a tap on it must not shut the case under the finger.
        check("the selected name does not navigate",
                Showcase.hit(L, cx, cy + Showcase.focusR(L) * 3.63f) == Showcase.HIT_NONE);
        check("the caption is neither",
                Showcase.hit(L, cx, Showcase.plaqueBot(L) + L.unit * 0.6f) == Showcase.HIT_NONE);

        float step = Showcase.step(L), rowStep = Showcase.rowStep(L);
        c.caseTo(3);
        c.beginCaseDrag(cx, cy);
        c.caseDragTo(cx + step * 0.3f, cy - rowStep * 0.25f, L);
        float panX = Showcase.column(c.caseIndex) - c.caseSlide;
        float panY = Showcase.row(c.caseIndex) - c.caseSlideY;
        check("diagonal panning moves both axes continuously",
                Math.abs(panX - 1.7f) < 0.01f && Math.abs(panY - 0.25f) < 0.01f);
        c.endCaseDrag();
        advance(c, L, 1f);
        check("release does not snap to a row or column",
                Math.abs(Showcase.column(c.caseIndex) - c.caseSlide - panX) < 0.01f
                && Math.abs(Showcase.row(c.caseIndex) - c.caseSlideY - panY) < 0.01f);
        c.beginCaseDrag(cx, cy);
        c.caseDragTo(cx - step * 0.2f, cy + rowStep * 0.1f, L);
        check("a second pan continues from the released position",
                Math.abs(Showcase.column(c.caseIndex) - c.caseSlide - 1.9f) < 0.01f
                && Math.abs(Showcase.row(c.caseIndex) - c.caseSlideY - 0.15f) < 0.01f);
        c.endCaseDrag();
        panX = Showcase.column(c.caseIndex) - c.caseSlide;
        panY = Showcase.row(c.caseIndex) - c.caseSlideY;
        CaseUi.select(c, 1);
        check("selecting a tile starts its pan without an immediate jump",
                Math.abs(Showcase.column(c.caseIndex) - c.caseSlide - panX) < 0.01f
                && Math.abs(Showcase.row(c.caseIndex) - c.caseSlideY - panY) < 0.01f);
        float tileX = cx + (Showcase.column(1) - panX) * step;
        float tileY = cy + (Showcase.row(1) - panY) * rowStep;
        check("panned tile hit testing follows its visible position",
                Showcase.hit(c, L, tileX, tileY) == Showcase.HIT_ENTRY + 1);
        advance(c, L, 0.1f);
        check("selection pans toward center on both axes",
                Math.abs(c.caseSlide) < Math.abs(Showcase.column(1) - panX)
                && Math.abs(c.caseSlideY) < Math.abs(Showcase.row(1) - panY));
        advance(c, L, 1f);
        check("selected character arrives at the center", c.caseIndex == 1
                && c.caseSlide == 0f && c.caseSlideY == 0f);
        check("the centered character opens its story",
                Showcase.hit(c, L, cx, cy) == Showcase.HIT_FOCUS);
        CaseUi.select(c, 3);
        advance(c, L, 0.1f);
        c.beginCaseDrag(cx, cy);
        c.endCaseDrag();
        float interruptedX = c.caseSlide, interruptedY = c.caseSlideY;
        advance(c, L, 1f);
        check("touching the surface interrupts centering without snapping",
                c.caseSlide == interruptedX && c.caseSlideY == interruptedY);
        c.beginCaseDrag(cx, cy);
        c.caseDragTo(cx + L.w * 100f, cy + L.h * 100f, L);
        check("pan stops at the top left catalogue bounds",
                Math.abs(Showcase.column(c.caseIndex) - c.caseSlide + 0.35f) < 0.01f
                && Math.abs(Showcase.row(c.caseIndex) - c.caseSlideY + 0.35f) < 0.01f);
        c.caseDragTo(cx - L.w * 100f, cy - L.h * 100f, L);
        check("pan stops at the bottom of the catalogue",
                Math.abs(Showcase.row(c.caseIndex) - c.caseSlideY
                - (Showcase.ROW_NAME.length - 0.65f)) < 0.01f);
        c.endCaseDrag();
        int last = c.caseIndex;
        c.closeCase();
        c.beginCaseDrag(cx, cy);
        c.caseDragTo(0f, 0f, L);
        check("closed cases cannot pan", c.caseIndex == last && !c.caseDragging);

        // Stories still come off the shelf, and only while it is out.
        GameCore d = new GameCore(store, 101L);
        d.openStory();
        check("no story from a shut case", !d.storyOpen());
        d.openCase();
        d.openStory();
        check("and one from an open one", d.storyOpen());
        d.closeCase();
        check("closing the case takes the story with it", !d.storyOpen());
    }

    static void screenKeys(Layout L) {
        group("screen keys");
        // Every key does the same thing on each screen: start from the title, back to the title
        // from game over. No key on either has a role of its own any more.

        // On the title every key starts, now that none of them browses.
        for (int g = 0; g < Glyph.COUNT; g++) {
            GameCore c = new GameCore(new Mem(), 100L + g);
            check("key " + g + " starts a run from the title", startFromTitle(c, L, g));
        }

        GameCore c = new GameCore(new Mem(), 111L);
        c.tapKey(0, L);
        check("no key browses the case", c.caseIndex == 0 && !c.caseOpen);
        check("and the outer key starts like the rest", c.starting());
        check("starting does not consume a press as a hit", c.hits == 0 && c.misses == 0);

        // With the case up every key puts it away first, rather than starting a run out from
        // under somebody reading it.
        GameCore b = new GameCore(new Mem(), 112L);
        b.openCase();
        b.tapKey(0, L);
        check("a key closes the case instead of starting", !b.caseOpen && !b.starting());
        b.tapKey(0, L);
        check("and the next one starts", b.starting());

        // Game over: every key goes back to the title, which is also the only route back to the
        // display case once a run has started. A replay is two presses of anything.
        c.startGame();
        check("starting a run puts the case away", !c.caseOpen && c.caseFade == 0f);
        c.lives = 1;
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("reached game over", c.state == GameCore.OVER);
        c.tapKey(2, L);
        check("game over ignores keys during the grace period", c.state == GameCore.OVER);
        advancePastDeath(c, L);
        c.tapKey(0, L);
        check("an outer key returns to the title", c.state == GameCore.TITLE);
        check("and a run starts again from there", startFromTitle(c, L, 3));

        // And an inner one does the same: no key drops straight back into a run any more.
        boolean allToTitle = true, noneToPlay = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            GameCore d = new GameCore(new Mem(), 113L + g);
            d.startGame();
            d.lives = 1;
            d.enemies.clear();
            add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1);
            advance(d, L, GameCore.ATTACK_TIME + 2 * DT);
            advancePastDeath(d, L);
            d.tapKey(g, L);
            if (d.state != GameCore.TITLE) allToTitle = false;
            if (d.state == GameCore.PLAY) noneToPlay = false;
        }
        check("every key returns to the title from game over", allToTitle);
        check("and none of them skips straight into a run", noneToPlay);
    }

    /** The title screen dissolving on a start press, rather than cutting to play. */
    static void startFade(Layout L) {
        group("title fade-out");
        Mem store = new Mem();
        store.collected = 0b1101L;
        GameCore c = new GameCore(store, 141L);
        Ear ear = new Ear();
        c.sound = ear;
        check("nothing fading to begin with", !c.starting() && c.startFade == 0f);

        c.tapKey(2, L);
        check("a start key begins the fade", c.starting() && c.startFade > 0f);
        check("and play has not begun", c.state == GameCore.TITLE);
        check("the tone leads it rather than following", ear.starts == 1);

        // It runs down, and the game is still the title screen for all of it.
        boolean heldTitle = true, ranDown = true;
        float last = c.startFade;
        for (int i = 0; i < 60 * 5 && c.state == GameCore.TITLE; i++) {
            c.update(DT, L);
            if (c.state == GameCore.TITLE) {
                if (c.startFade > last) ranDown = false;
                last = c.startFade;
                if (!c.starting() && c.state == GameCore.TITLE) heldTitle = false;
            }
        }
        check("the fade only ever runs down", ranDown);
        check("the title holds for the whole of it", heldTitle);
        check("play begins when it is spent", c.state == GameCore.PLAY);
        check("and the tone did not play twice", ear.starts == 1);
        check("nothing left fading in play", c.startFade == 0f);
        check("the fade is brief", GameCore.START_FADE < 1f);

        // A second press during the fade must not restart it or double the tone.
        GameCore d = new GameCore(store, 143L);
        Ear ear2 = new Ear();
        d.sound = ear2;
        d.tapKey(2, L);
        advance(d, L, GameCore.START_FADE * 0.4f);
        float mid = d.startFade;
        d.tapKey(3, L);
        check("a second press is ignored", d.startFade == mid && ear2.starts == 1);
        d.tapKey(0, L);
        check("and so is an outer one", d.caseIndex == 0 && !d.caseOpen);
        // Nor can the case be opened over a screen that is already leaving.
        d.openCase();
        check("the case cannot open mid-fade", !d.caseOpen);

        // A story and the case both go before a run does: each press unstacks one of them.
        GameCore e = new GameCore(store, 145L);
        e.openCase();
        e.caseIndex = 0;
        e.openStory();
        check("a story is open", e.storyOpen());
        e.tapKey(2, L);
        check("the first press dismisses the story instead of starting",
                !e.storyOpen() && !e.starting());
        e.tapKey(2, L);
        check("the next one puts the case away", !e.caseOpen && !e.starting());
        e.tapKey(2, L);
        check("and the one after that starts the fade", e.starting());
        check("with no story hanging over it", !e.storyOpen());

        // Leaving game over is a cut, not a fade: the fade belongs to the title screen, and one
        // is now on the way in rather than out.
        GameCore f = new GameCore(store, 147L);
        f.startGame();
        f.lives = 1;
        f.enemies.clear();
        add(f, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(f, L, GameCore.ATTACK_TIME + 2 * DT);
        advancePastDeath(f, L);
        f.tapKey(3, L);
        check("game over cuts to the title with nothing fading",
                f.state == GameCore.TITLE && f.startFade == 0f);
        check("and the case is shut on arrival", !f.caseOpen && f.caseFade == 0f);
        f.tapKey(3, L);
        check("the press after that fades into a run", f.starting());
    }

    /** The send-off: the case's squishy bouncing out of the screen as a run starts. */
    static void sendOff(Layout L) {
        group("start send-off");
        Mem store = new Mem();
        store.collected = 0b101L;               // entries 0 and 2
        GameCore c = new GameCore(store, 151L);
        Ear ear = new Ear();
        c.sound = ear;
        check("nothing being sent off to begin with", c.launchWho < 0 && c.launchT == 0f);
        check("the send-off outlasts the title fade", Launch.TIME > GameCore.START_FADE);
        check("its phases are in order", Launch.POP < Launch.LAND
                && Launch.LAND < Launch.TOP && Launch.TOP < 1f);

        // Let the badge drift off centre first: the send-off has to leave from where it is.
        advance(c, L, 1.4f);
        float pressed = c.clock;
        c.caseIndex = 0;
        c.tapKey(2, L);
        check("the case entry is the one sent off", c.launchWho == 0);
        check("with the whole send-off ahead of it", c.launchT == Launch.TIME);
        check("and the badge's drift frozen at the press", c.launchClock == pressed);

        // Play waits for it: the title screen is long gone by the time it lands.
        advance(c, L, GameCore.START_FADE + 2 * DT);
        check("the fade is spent", c.startFade == 0f);
        check("but play has not begun", c.state == GameCore.TITLE && c.starting());
        float u = Launch.progress(c);
        check("the send-off is part way through", u > 0.4f && u < 1f);
        advance(c, L, Launch.TIME);
        check("play begins once it is done", c.state == GameCore.PLAY);
        check("and nothing is left of it", c.launchWho < 0 && c.launchT == 0f);
        check("both bounces sounded", ear.squishes == 2);
        check("the start tone still led the whole thing", ear.starts == 1);

        // An uncollected entry has nothing to send off, so that press starts as it always did.
        GameCore d = new GameCore(store, 153L);
        d.caseIndex = 1;
        d.tapKey(2, L);
        check("an uncollected entry is not sent off", d.launchWho < 0 && d.launchT == 0f);
        check("and the fade is the whole wait", d.starting() && d.startFade > 0f);
        advance(d, L, GameCore.START_FADE + 2 * DT);
        check("play begins on the fade alone", d.state == GameCore.PLAY);

        // Progress only ever runs forward, whatever the frame rate.
        GameCore e = new GameCore(store, 155L);
        e.caseIndex = 2;
        e.tapKey(2, L);
        boolean forward = true;
        float last = -1f;
        for (int i = 0; i < 60 * 5 && e.state == GameCore.TITLE; i++) {
            float now = e.launchT > 0f ? Launch.progress(e) : 1f;
            if (now < last) forward = false;
            last = now;
            e.update(DT, L);
        }
        check("progress only runs forward", forward && e.state == GameCore.PLAY);

        // And going back to the title cancels one mid-flight.
        GameCore f = new GameCore(store, 157L);
        f.caseIndex = 0;
        f.tapKey(2, L);
        f.toTitle();
        check("returning to the title cancels it", f.launchWho < 0 && f.launchT == 0f);
    }

    static void clearing(Layout L) {
        group("clearing the case");
        Mem store = new Mem();
        store.collected = Collect.MASK;
        GameCore c = new GameCore(store, 121L);
        c.startGame();
        c.openSettings();
        check("the button is not armed when the panel opens", !c.clearArmed);

        c.tapClearCase();
        check("one tap only arms it", c.clearArmed && Collect.complete(c.collected));
        c.tapClearCase();
        check("the second tap empties the case", Collect.owned(c.collected) == 0);
        check("and persists the empty case", store.collected == 0L);
        check("the button disarms itself again", !c.clearArmed);
        check("the case falls back to the first entry", c.caseIndex == 0);
        check("and the last prize goes with it", c.prize < 0);

        // Closing the panel must not leave a live erase waiting for the next visit.
        c.collected = Collect.MASK;
        c.tapClearCase();
        c.closeSettings();
        c.openSettings();
        c.tapClearCase();
        check("reopening the panel disarms the button", Collect.complete(c.collected));
    }
}
