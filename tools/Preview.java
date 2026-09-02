package com.sram.hexatype;

import java.io.File;

/**
 * Drives {@link GameCore} headlessly to interesting states and renders each one to a PNG
 * through {@link RasterPainter}. This is how the game gets looked at during development
 * without a build/install cycle: same Layout, same Renderer, same state machine as the APK.
 */
final class Preview {

    private static final float DT = 1f / 60f;

    /** Lines that ran off the screen across every frame rendered, for the summary at the end. */
    private static int unfitFrames;

    private static final class Mem implements GameCore.Store {
        int best;
        float speed = 1f;
        int bgm;
        long collected;
        int collectTotal;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; }
        public float loadSpeed() { return speed; }
        public void saveSpeed(float v) { speed = v; }
        public int loadBgm() { return bgm; }
        public void saveBgm(int v) { bgm = v; }
        public long loadCollected() { return collected; }
        public void saveCollected(long v) { collected = v; }
        public int loadCollectTotal() { return collectTotal; }
        public void saveCollectTotal(int v) { collectTotal = v; }
    }

    public static void main(String[] args) throws Exception {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 640;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : 1400;
        int ss = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        File dir = new File(args.length > 3 ? args[3] : "out");
        dir.mkdirs();

        Layout L = new Layout();
        L.compute(w, h, 0, 0, 0, 0);
        System.out.printf("layout %dx%d  keyR=%.1f  keyTop=%.0f  dangerY=%.0f  enemyR=%.1f%n",
                w, h, L.keyR, L.keyTop, L.dangerY, L.enemyR);

        // Named, so a frame filter skips them along with everything else it did not ask for.
        if (wanted("chars")) characterSheet(dir, w, h, ss);
        if (wanted("skits")) skitSheet(dir, L, w, h, ss);
        if (wanted("collect")) collectSheet(dir, w, h, ss);
        if (wanted("sfx")) sounds(dir);

        Mem store = new Mem();
        store.best = 1840;
        // A part-filled case, so the title screen shows both a collected entry and the
        // silhouettes either side of it.
        store.collected = 0b0000_0100_1000_0011_0010_0110_1101L;
        // More baskets opened than entries owned, which is the normal case and the whole reason the
        // tally exists: seven of these seventeen were duplicates.
        store.collectTotal = 17;

        // Title screen with the case shut, which is how it is arrived at: the badge in the
        // middle is the whole of the collection's footprint until it is tapped.
        GameCore c = new GameCore(store, 7L);
        step(c, L, 0.55f);
        System.out.printf("title: case=%d of %d collected, shut=%s%n",
                Collect.owned(c.collected), Collect.COUNT, !c.caseOpen);
        shot(dir, "1-title", c, L, w, h, ss);

        // The title screen's demo, caught just after its second letter is struck: two cleared,
        // one to go, and the key for it lit under the word.
        GameCore c21 = new GameCore(store, 87L);
        step(c21, L, Demo.LOOP * 0.57f);
        System.out.printf("title demo: lit key=%d amount=%.2f%n", Demo.litKey(c21),
                Demo.litAmount(c21));
        shot(dir, "49-title-demo", c21, L, w, h, ss);

        // Just after the third press: typed out, the finishing bullet still in the air, the ring
        // flashing over the dimmed tiles. Exactly the state a real word is in between the last
        // press and its shot landing.
        GameCore c22 = new GameCore(store, 87L);
        step(c22, L, Demo.LOOP * 0.715f);
        System.out.printf("title last shot: lit key=%d amount=%.2f%n", Demo.litKey(c22),
                Demo.litAmount(c22));
        shot(dir, "50-title-shot", c22, L, w, h, ss);

        // The demo word arriving. It fades and swells up on enterT, the same field a real word's
        // entrance rides, rather than appearing whole.
        GameCore c23 = new GameCore(store, 87L);
        step(c23, L, Demo.LOOP * 0.055f);
        shot(dir, "51-title-arriving", c23, L, w, h, ss);

        // And coming apart once that bullet lands: the field's own fly-apart, outer tiles splitting
        // left and right off Renderer.enemy's destroy path.
        GameCore c24 = new GameCore(store, 87L);
        step(c24, L, Demo.LOOP * 0.80f);
        shot(dir, "52-title-destroyed", c24, L, w, h, ss);

        // Part-way through fading in on that tap.
        c.openCase();
        step(c, L, 0.78f / GameCore.CASE_FADE_RATE);
        System.out.printf("case opening: fade=%.2f%n", c.caseFade);
        shot(dir, "39-case-opening", c, L, w, h, ss);
        step(c, L, 1f);

        // Open and parked on a gap, mid-slide, which is what most of the strip looks like early
        // on. Rendered at 0.06s in so the shelf is caught between two entries.
        c.scrollCase(1);
        step(c, L, 0.06f);
        System.out.printf("title gap: index=%d known=%s slide=%.2f%n", c.caseIndex,
                Collect.has(c.collected, c.caseIndex), c.caseSlide);
        shot(dir, "20-title-locked", c, L, w, h, ss);

        // Mid-drag: the shelf carried off centre by a finger, and the bar thumb lit to say it
        // is the finger doing it.
        c.beginCaseDrag(w / 2f);
        c.caseDragTo(w / 2f + Showcase.step(L) * 0.42f, L);
        step(c, L, 2 * DT);
        System.out.printf("case drag: index=%d slide=%.2f dragging=%s%n", c.caseIndex,
                c.caseSlide, c.caseDragging);
        shot(dir, "40-case-drag", c, L, w, h, ss);
        c.endCaseDrag();
        step(c, L, 0.5f);

        // The badge out at one end of its arc, where the box is turned hardest.
        GameCore c20 = new GameCore(store, 83L);
        step(c20, L, Showcase.ARC_TIME * 0.25f);
        System.out.printf("badge drift: x=%.0f of %.0f, turn=%.2f%n",
                Showcase.iconCx(L, c20.clock), (float) w, Showcase.iconTurn(L, c20.clock));
        shot(dir, "41-badge-turned", c20, L, w, h, ss);

        // The title screen part-way through dissolving on a start press.
        GameCore c18 = new GameCore(store, 79L);
        step(c18, L, 0.55f);
        c18.tapKey(2, L);
        step(c18, L, GameCore.START_FADE * 0.45f);
        System.out.printf("title fade: %.2f left of %.2f, state=%d%n", c18.startFade,
                GameCore.START_FADE, c18.state);
        shot(dir, "35-title-fading", c18, L, w, h, ss);

        // The send-off: the squishy swelling out of the badge in a pip of stars, and again as it
        // bounces off the top of the screen with the title long gone.
        GameCore c19 = new GameCore(store, 81L);
        step(c19, L, 1.4f);
        c19.tapKey(2, L);
        step(c19, L, Launch.TIME * Launch.POP * 0.75f);
        System.out.printf("send-off pop: who=%d left=%.2f progress=%.2f%n", c19.launchWho,
                c19.launchT, Launch.progress(c19));
        shot(dir, "42-sendoff-pop", c19, L, w, h, ss);
        step(c19, L, Launch.TIME * (Launch.TOP - Launch.POP * 0.75f));
        System.out.printf("send-off roof: progress=%.2f state=%d%n", Launch.progress(c19),
                c19.state);
        shot(dir, "43-sendoff-roof", c19, L, w, h, ss);

        // Story popup, mid-panel-spring and again settled with the scene playing.
        c.caseIndex = 0;
        c.openStory();
        step(c, L, 0.12f);
        System.out.printf("story opening: entry=%s t=%.2f%n", Collect.NAME[c.story], c.storyT);
        shot(dir, "26-story-opening", c, L, w, h, ss);
        step(c, L, 0.9f);
        shot(dir, "27-story", c, L, w, h, ss);
        c.closeStory();

        // Sheet of every beat, so all ten vignettes can be reviewed at once.
        beatSheet(dir, L, w, h, ss);

        // A wave in flight, nothing typed yet.
        c.startGame();
        step(c, L, 7.0f);
        shot(dir, "2-wave", c, L, w, h, ss);

        // Locked on, first letter struck: colour strobe and scale pop, shot mid-flight.
        GameCore.Enemy e = lowest(c);
        if (e != null) {
            c.tapKey(e.word[e.pos], L);
            step(c, L, 3 * DT);
        }
        shot(dir, "3-hit", c, L, w, h, ss);

        // Perfect play into a later stage.
        GameCore c2 = new GameCore(store, 11L);
        c2.startGame();
        autoplay(c2, L, 60f);
        System.out.printf("autoplay 60s: stage=%d score=%d squishes=%d lives=%d combo=%d%n",
                c2.stage, c2.score, c2.squishes, c2.lives, c2.combo);
        shot(dir, "4-stage" + c2.stage, c2, L, w, h, ss);

        // Stage-up banner.
        int before = c2.stage;
        float guard = 0;
        while (c2.stage == before && c2.state == GameCore.PLAY && guard < 40f) {
            autoplay(c2, L, DT * 2);
            guard += DT * 2;
        }
        step(c2, L, 0.2f);
        shot(dir, "5-stage-up", c2, L, w, h, ss);

        // Word closing in, on the last life: red tint, alarm line, agitated letters.
        GameCore c4 = new GameCore(store, 17L);
        c4.startGame();
        c4.score = 1310;
        c4.squishes = 14;
        c4.stage = 2;
        c4.lives = 1;
        step(c4, L, 1.8f);   // past the opening stage banner
        c4.enemies.clear();
        GameCore.Enemy near = new GameCore.Enemy();
        near.word = new int[] {2, 5, 0};
        near.need = new int[] {1, 2, 1};
        near.gone = new boolean[3];
        near.goneT = new float[3];
        near.goneDx = new float[3];
        near.goneDy = new float[3];
        near.baseX = (L.playLeft + L.playRight) / 2f;
        near.y = L.dangerY - (L.dangerY - L.playTop) * 0.06f;
        near.speed = 0;
        c4.enemies.add(near);
        step(c4, L, 2 * DT);
        System.out.printf("danger frame: warn=%.2f harm=%.2f%n", c4.warnLevel, c4.harm());
        shot(dir, "6-danger", c4, L, w, h, ss);

        // The push-back offered: a word inside the warning band and the swipe strip lit.
        System.out.printf("push offered: ready=%s warn=%.2f%n", c4.pushReady(), c4.warnLevel);
        shot(dir, "36-push-ready", c4, L, w, h, ss);

        // Mid-slide: the shockwave climbing and the word travelling back with it, caught partway
        // so the frame shows the trip rather than the destination.
        float wasY = near.y;
        c4.pushBack(L);
        step(c4, L, GameCore.PUSH_SLIDE * 0.45f);
        System.out.printf("push fired: moved %d, %.0f of %.0fpx so far, wave=%.2f%n", c4.pushCount,
                wasY - near.y, wasY - near.slideTo, c4.pushT);
        shot(dir, "37-push-wave", c4, L, w, h, ss);
        // And settled, once the slide has run out: the word up the field, the strip dark.
        step(c4, L, GameCore.PUSH_SLIDE);
        System.out.printf("push settled: %.0fpx back, slide=%.2f ready=%s%n", wasY - near.y,
                near.slideT, c4.pushReady());
        shot(dir, "38-push-settled", c4, L, w, h, ss);
        // Put it back on the line for the lunge frame below, and hand the swipe back so the
        // frames after this are not quietly missing the strip.
        near.y = L.dangerY - (L.dangerY - L.playTop) * 0.06f;
        near.slideT = 0f;
        c4.pushUsed = false;
        c4.pushT = 0f;
        step(c4, L, 2 * DT);

        // Mid-lunge attack.
        near.y = L.dangerY - L.enemyR + 1;
        step(c4, L, GameCore.ATTACK_TIME * 0.55f);
        System.out.printf("attack frame: attacking=%s flash=%.2f%n", near.attacking, c4.flash);
        shot(dir, "7-attack", c4, L, w, h, ss);

        // Game over.
        GameCore c3 = new GameCore(store, 5L);
        c3.startGame();
        c3.score = 2450;
        c3.squishes = 26;
        c3.stage = 4;
        c3.maxCombo = 19;
        float t = 0;
        while (c3.state == GameCore.PLAY && t < 120f) {
            c3.update(DT, L);
            t += DT;
        }
        c3.hits = 184;
        c3.misses = 61;   // 75% -> mid mood

        // The death hold, caught twice: the words that were on the field swirling away, the deck
        // gone red and sad, the sky draining green. The summary is not up yet. Not at zero — that
        // frame is the field exactly as it was, with none of the sequence started.
        step(c3, L, GameCore.DEATH_TIME * 0.15f);
        System.out.printf("death hold: %.0f%% through, words=%d, summary=%.2f%n",
                c3.deathProgress() * 100f, c3.enemies.size(), c3.overFade());
        shot(dir, "44-death", c3, L, w, h, ss);
        step(c3, L, GameCore.DEATH_TIME * 0.35f);
        System.out.printf("death hold: %.0f%% through, words=%d, summary=%.2f%n",
                c3.deathProgress() * 100f, c3.enemies.size(), c3.overFade());
        shot(dir, "45-death-late", c3, L, w, h, ss);

        // And the summary halfway up, over the drained field.
        step(c3, L, GameCore.DEATH_TIME * 0.5f + GameCore.OVER_FADE * 0.5f);
        System.out.printf("summary fading: %.2f, dying=%s%n", c3.overFade(), c3.dying());
        shot(dir, "46-summary-fading", c3, L, w, h, ss);

        step(c3, L, 1.0f);
        System.out.printf("gameover: accuracy=%d%% mood=%.2f%n",
                c3.accuracyPercent(), c3.accuracyMood());
        shot(dir, "8-gameover", c3, L, w, h, ss);

        // Same screen at both mood extremes.
        c3.hits = 92;
        c3.misses = 84;   // 52% -> saddest
        shot(dir, "9-gameover-sad", c3, L, w, h, ss);
        c3.hits = 240;
        c3.misses = 9;    // 96% -> happiest
        shot(dir, "10-gameover-happy", c3, L, w, h, ss);

        // The run's haul dancing on the summary, then carrying itself to the case. The prizes are
        // set by hand: playing far enough to win three of them takes longer than a preview should.
        c3.roundPrizes = Collect.add(Collect.add(Collect.add(0L, 0), 13), 24);
        step(c3, L, 0.4f);
        System.out.printf("haul dance: %d dumplings, summary=%.2f%n",
                Collect.owned(c3.roundPrizes), c3.overFade());
        shot(dir, "47-haul-dance", c3, L, w, h, ss);

        // Any key sends them home. Caught mid-flight, with the star trails strung out behind.
        c3.tapKey(2, L);
        step(c3, L, GameCore.HOME_TIME * 0.45f);
        System.out.printf("haul homeward: %.0f%% of the way, state=%d%n",
                c3.homeProgress() * 100f, c3.state);
        shot(dir, "48-haul-homeward", c3, L, w, h, ss);

        // Flawless wave celebration.
        GameCore c5 = new GameCore(store, 23L);
        c5.startGame();
        c5.score = 980;
        c5.spawnedThisStage = c5.stageQuota();
        c5.enemies.clear();
        c5.shots.clear();
        c5.update(DT, L);
        step(c5, L, 0.42f);   // into the bounce, past the fade-in
        System.out.printf("perfect wave: banner=%.2f stage=%d%n", c5.perfectBanner, c5.stage);
        shot(dir, "11-perfect", c5, L, w, h, ss);

        // Mid-destruction: a cleared word flying apart.
        GameCore c7 = new GameCore(store, 31L);
        c7.startGame();
        c7.score = 640;
        step(c7, L, 1.8f);
        c7.enemies.clear();
        c7.target = null;
        GameCore.Enemy boom = new GameCore.Enemy();
        boom.word = new int[] {1, 3, 0, 4};
        boom.need = new int[] {1, 1, 1, 1};
        boom.gone = new boolean[4];
        boom.goneT = new float[4];
        boom.goneDx = new float[4];
        boom.goneDy = new float[4];
        boom.baseX = (L.playLeft + L.playRight) / 2f;
        boom.y = L.playTop + (L.dangerY - L.playTop) * 0.45f;
        boom.enterT = 1f;
        c7.enemies.add(boom);
        for (int i = 0; i < boom.word.length; i++) c7.tapKey(boom.word[i], L);
        step(c7, L, 0.16f);          // shot has landed, tiles are on their way out
        step(c7, L, GameCore.DESTROY_TIME * 0.45f);
        System.out.printf("destruction: destroyed=%s t=%.2f dirs=%s shake=%.2f%n",
                boom.destroyed, boom.destroyT, java.util.Arrays.toString(boom.flyDir), c7.shake);
        shot(dir, "13-destroy", c7, L, w, h, ss);

        // Between-stages minigame, part-way through prising the lid off.
        GameCore c8 = new GameCore(store, 37L);
        c8.startGame();
        c8.score = 2100;
        c8.spawnedThisStage = c8.stageQuota();
        c8.enemies.clear();
        c8.shots.clear();
        c8.update(DT, L);
        // Wait out the flawless-wave celebration that precedes the interlude, then the
        // spinner: presses are refused for the whole of it.
        for (int i = 0; i < 60 * 8 && c8.state != GameCore.BONUS; i++) c8.update(DT, L);
        for (int i = 0; i < 60 * 8 && c8.bonusRolling(); i++) c8.update(DT, L);
        for (int i = 0; i < 26; i++) c8.tapBonus(c8.steamer.wanted());
        // One more press keeps the lid pulse and the flash mid-decay, which is the point of
        // this frame. The fade-in is long over by now: the spinner ran first.
        c8.tapBonus(c8.steamer.wanted());
        step(c8, L, 0.05f);
        System.out.printf("bonus: state=%d hits=%d open=%.2f lidPulse=%.2f flash=%.2f%n",
                c8.state, c8.steamer.hits, c8.steamer.lidOpen(), c8.steamer.lidPulse, c8.steamer.flash);
        shot(dir, "14-bonus", c8, L, w, h, ss);

        // A wrong press: the basket jolts rose and the lid stays exactly where it was.
        GameCore c17 = new GameCore(store, 73L);
        c17.startGame();
        c17.score = 1800;
        c17.spawnedThisStage = c17.stageQuota();
        c17.enemies.clear();
        c17.shots.clear();
        for (int i = 0; i < 60 * 8 && c17.state != GameCore.BONUS; i++) c17.update(DT, L);
        for (int i = 0; i < 60 * 8 && c17.bonusRolling(); i++) c17.update(DT, L);
        for (int i = 0; i < 14; i++) c17.tapBonus(c17.steamer.wanted());
        step(c17, L, 0.5f);
        int badKey = 0;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (g != c17.steamer.leftKey && g != c17.steamer.rightKey) badKey = g;
        }
        c17.tapBonus(badKey);
        step(c17, L, 2 * DT);
        System.out.printf("wrong press: bad=%.2f lidPulse=%.2f flash=%.2f hits=%d%n",
                c17.steamer.badPulse, c17.steamer.lidPulse, c17.steamer.flash,
                c17.steamer.hits);
        shot(dir, "34-bonus-wrong", c17, L, w, h, ss);

        // The final point arms the lid and replaces the key prompt with the bouncing arrow.
        for (int i = 0; i < 16; i++) c8.tapBonus(c8.steamer.wanted());
        step(c8, L, 0.10f);
        System.out.printf("bonus swipe: ready=%s hits=%d%n",
                c8.bonusSwipeReady(), c8.steamer.hits);
        shot(dir, "59-bonus-swipe", c8, L, w, h, ss);

        // And the moment the upward swipe breaks it free.
        c8.swipeBonus();
        step(c8, L, 0.5f);
        System.out.printf("bonus freed: opens=%d freedT=%.2f score=%d lives=%d prize=%s new=%s%n",
                c8.steamer.opens, c8.steamer.freedT, c8.score, c8.lives,
                c8.prize >= 0 ? Collect.NAME[c8.prize] : "none", c8.prizeNew);
        shot(dir, "15-bonus-freed", c8, L, w, h, ss);

        // Star-path ready lesson and a mid-flight course with earlier pickups ghosted.
        GameCore cs = new GameCore(store, 83L);
        cs.startGame();
        cs.state = GameCore.BONUS;
        cs.starBonus = true;
        cs.stars.make(new java.util.Random(83L));
        cs.stars.begin(c8.prize, L);
        cs.bonusTimer = cs.stars.timer;
        step(cs, L, 0.45f);
        shot(dir, "53-stars-ready", cs, L, w, h, ss);
        // The end of the lesson, where the lean has settled and the flyer is standing still on the
        // spot the course leaves from. The frame it used to jump from.
        cs.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT + 0.10f;
        cs.bonusTimer = cs.stars.timer;
        shot(dir, "53b-stars-settled", cs, L, w, h, ss);
        cs.stars.collected = 0b11111 | (1 << 10);
        cs.stars.burst[10] = 0.82f;
        // Offsets into the flight, kept where they were as fractions of it now that FLY is 3.6.
        cs.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 2.35f;
        cs.stars.x = cs.stars.starX(10, L);
        cs.bonusTimer = cs.stars.timer;
        shot(dir, "54-stars-flight", cs, L, w, h, ss);
        // And the top of the sweep, where the course turns back on itself: the two moments say
        // between them whether a course reads as a swoop or as a diagonal.
        cs.stars.collected = 0b111;
        cs.stars.burst[10] = 0f;
        cs.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 1.20f;
        cs.stars.x = cs.stars.starX(6, L);
        cs.bonusTimer = cs.stars.timer;
        shot(dir, "54b-stars-turn", cs, L, w, h, ss);
        // The wake at full strength: nineteen in hand, which is what the last stretch of a course
        // that is nearly won looks like. It is the only thing on screen that says how far along a
        // playthrough is without a number on it.
        cs.stars.collected = (1 << (StarPath.COUNT - 1)) - 1;
        cs.stars.x = cs.stars.starX(6, L);
        cs.stars.vx = L.w * StarPath.MAX_VX * 0.7f;
        shot(dir, "54c-stars-wake", cs, L, w, h, ss);

        // Taking the last star: the course stops dead and the prize climbs out of the checkpoint
        // that ended it, early in the tableau and again once it is standing in place.
        GameCore cw = new GameCore(store, 91L);
        cw.startGame();
        cw.state = GameCore.BONUS;
        cw.starBonus = true;
        cw.stars.make(new java.util.Random(91L));
        cw.stars.begin(c8.prize, L);
        cw.stars.collected = (1 << (StarPath.COUNT - 1)) - 1;
        cw.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 3.05f;
        // Steered onto the last star frame by frame: where the course has scrolled to is the only
        // thing that knows where that star is.
        for (int i = 0; i < 60 * 6 && !cw.stars.won; i++) {
            cw.stars.x = cw.stars.starX(StarPath.COUNT - 1, L);
            cw.update(DT, L);
        }
        System.out.printf("stars won: star=%d prize=%s new=%s parade=%.2f%n", cw.stars.winStar,
                cw.prize >= 0 ? Collect.NAME[cw.prize] : "none", cw.prizeNew, cw.paradeTimer);
        step(cw, L, 0.28f);
        shot(dir, "55-stars-won", cw, L, w, h, ss);
        step(cw, L, 1.1f);
        shot(dir, "56-stars-victory", cw, L, w, h, ss);
        // And the handover: the same parade a won steamer ends on.
        for (int i = 0; i < 60 * 10 && !cw.bonusParading(); i++) cw.update(DT, L);
        step(cw, L, GameCore.PARADE_TIME * 0.45f);
        shot(dir, "57-stars-parade", cw, L, w, h, ss);

        // The parade that closes a winning interlude: marching in, the new one joining, and
        // the line on its way off to the right.
        GameCore c13 = new GameCore(store, 59L);
        c13.startGame();
        c13.score = 3400;
        c13.collected = 0b0110_1101_0011_0110_1101L;
        c13.spawnedThisStage = c13.stageQuota();
        c13.enemies.clear();
        c13.shots.clear();
        for (int i = 0; i < 60 * 8 && c13.state != GameCore.BONUS; i++) c13.update(DT, L);
        for (int i = 0; i < 60 * 8 && c13.bonusRolling(); i++) c13.update(DT, L);
        for (int i = 0; i < GameCore.STEAMER_HITS * 2 + 4; i++) {
            c13.tapBonus(c13.steamer.wanted());
        }
        c13.swipeBonus();
        for (int i = 0; i < 60 * 30 && !c13.bonusParading(); i++) c13.update(DT, L);
        System.out.printf("parade: won %s, %d collected%n", Collect.NAME[c13.prize],
                Collect.owned(c13.collected));
        step(c13, L, GameCore.PARADE_TIME * 0.22f);
        System.out.printf("  marching in at t=%.2f%n", c13.paradeProgress());
        shot(dir, "28-parade-in", c13, L, w, h, ss);
        step(c13, L, GameCore.PARADE_TIME * 0.28f);
        System.out.printf("  joining at t=%.2f%n", c13.paradeProgress());
        shot(dir, "29-parade-join", c13, L, w, h, ss);
        step(c13, L, GameCore.PARADE_TIME * 0.22f);
        System.out.printf("  marching off at t=%.2f%n", c13.paradeProgress());
        shot(dir, "30-parade-off", c13, L, w, h, ss);

        // The same reveal for a duplicate, and for the grail — the two ends of the payout.
        c8.prizeNew = false;
        shot(dir, "21-prize-dupe", c8, L, w, h, ss);
        c8.prize = Collect.COUNT - 1;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (Collect.TIER[i] == Collect.GRAIL) c8.prize = i;
        }
        c8.prizeNew = true;
        System.out.printf("grail reveal: %s%n", Collect.NAME[c8.prize]);
        shot(dir, "22-prize-grail", c8, L, w, h, ss);

        // Mid fade-in, to check the interlude eases in rather than cutting.
        GameCore c11 = new GameCore(store, 47L);
        c11.startGame();
        c11.score = 3100;
        c11.spawnedThisStage = c11.stageQuota();
        c11.enemies.clear();
        c11.shots.clear();
        for (int i = 0; i < 60 * 8 && c11.state != GameCore.BONUS; i++) c11.update(DT, L);
        step(c11, L, 0.16f);
        System.out.printf("bonus fade-in: state=%d time=%.2f timer=%.2f roll=%.2f%n",
                c11.state, c11.time, c11.bonusTimer, c11.rollProgress());
        shot(dir, "19-bonus-fadein", c11, L, w, h, ss);

        // Mid-spinner, past the fade-in: the pair is being drawn for and nothing is wanted yet.
        step(c11, L, 1.0f);
        System.out.printf("spinner: roll=%.2f showing %s/%s, will land on %s/%s%n",
                c11.rollProgress(), Glyph.NAME[c11.bonusLeftKey()],
                Glyph.NAME[c11.bonusRightKey()], Glyph.NAME[c11.steamer.leftKey],
                Glyph.NAME[c11.steamer.rightKey]);
        shot(dir, "24-bonus-spinner", c11, L, w, h, ss);

        // And the beat on zero, after the clock runs out and before anything fades.
        GameCore c12 = new GameCore(store, 53L);
        c12.startGame();
        c12.score = 2750;
        c12.spawnedThisStage = c12.stageQuota();
        c12.enemies.clear();
        c12.shots.clear();
        for (int i = 0; i < 60 * 8 && c12.state != GameCore.BONUS; i++) c12.update(DT, L);
        for (int i = 0; i < 60 * 8 && c12.bonusRolling(); i++) c12.update(DT, L);
        for (int i = 0; i < 9; i++) c12.tapBonus(c12.steamer.wanted());
        for (int i = 0; i < 60 * 8 && !c12.bonusHolding(); i++) c12.update(DT, L);
        step(c12, L, 0.25f);
        System.out.printf("time up: holding=%s left=%.2f hits=%d%n", c12.bonusHolding(),
                c12.bonusLeft(), c12.steamer.hits);
        shot(dir, "25-bonus-timeup", c12, L, w, h, ss);

        // The powerup letter drifting across, before it is caught.
        GameCore c9 = new GameCore(store, 41L);
        c9.startGame();
        c9.score = 1750;
        step(c9, L, 2.0f);
        Power drift = new Power();
        drift.glyph = 3;
        drift.effect = Power.FLURRY;
        drift.y = L.playTop + (L.dangerY - L.playTop) * 0.28f;
        drift.x = L.w * 0.42f;
        drift.vx = L.w / Power.CROSS_TIME;
        c9.power = drift;
        step(c9, L, 0.4f);
        System.out.printf("powerup adrift: %s at x=%.0f%n", drift.name(), drift.x);
        shot(dir, "16-powerup", c9, L, w, h, ss);

        // Mid-frenzy: chaotic sky, rainbow letters, mode bar counting down.
        c9.tapKey(3, L);
        step(c9, L, 2.5f);
        System.out.printf("frenzy: mode=%s left=%.1f enemies=%d%n",
                Power.NAMES[c9.mode], c9.modeLeft, c9.enemies.size());
        shot(dir, "17-frenzy", c9, L, w, h, ss);

        // FLING with the instructional finger and its sparkle trail.
        GameCore c10 = new GameCore(store, 43L);
        c10.startGame();
        c10.score = 2400;
        step(c10, L, 1.9f);
        Power fl = new Power();
        fl.glyph = 5;
        fl.effect = Power.FLING;
        fl.y = L.playTop + 100f;
        fl.x = L.w * 0.5f;
        c10.power = fl;
        c10.tapKey(5, L);
        step(c10, L, 1.6f);
        System.out.printf("fling hint: showing=%s demo=(%.0f,%.0f) sparkles=%d%n",
                c10.showFlingHint(), c10.demoX, c10.demoY, c10.particles.size());
        shot(dir, "18-fling-hint", c10, L, w, h, ss);

        // The blade mid-stroke, having just taken two words at once: the streak, the doubled
        // sparkle ribbon, the gold rim and the payoff readout.
        GameCore c14 = new GameCore(store, 61L);
        c14.startGame();
        c14.score = 4100;
        step(c14, L, 1.9f);
        c14.enemies.clear();
        c14.target = null;
        Power blade = new Power();
        blade.glyph = 4;
        blade.effect = Power.FLING;
        blade.y = L.playTop + 100f;
        blade.x = L.w * 0.5f;
        c14.power = blade;
        c14.tapKey(4, L);
        float row = L.playTop + (L.dangerY - L.playTop) * 0.42f;
        GameCore.Enemy g1 = new GameCore.Enemy();
        GameCore.Enemy g2 = new GameCore.Enemy();
        for (GameCore.Enemy g : new GameCore.Enemy[] {g1, g2}) {
            g.word = new int[] {1, 3};
            g.need = new int[] {1, 1};
            g.gone = new boolean[2];
            g.goneT = new float[2];
            g.goneDx = new float[2];
            g.goneDy = new float[2];
            g.y = row;
            g.enterT = 1f;
            c14.enemies.add(g);
        }
        g1.baseX = L.playLeft + L.enemyR * 3.2f;
        g2.baseX = L.playRight - L.enemyR * 3.2f;
        // Sweep across both, in samples, the way a real swipe arrives.
        c14.beginStroke(L.playLeft - L.enemyR, row);
        for (int i = 1; i <= 11; i++) {
            c14.sliceTo(L.playLeft - L.enemyR + (L.playRight - L.playLeft) * i / 12f, row, L);
            step(c14, L, DT);
        }
        // One more sample without a frame between, so the edge is caught mid-swing rather than
        // at the instant the trail has just been brought up to the finger.
        c14.sliceTo(L.playRight, row, L);
        System.out.printf("blade: %d words and %d letters in one stroke, slowdown=%.2f%n",
                c14.strokeKills, c14.strokeCuts, c14.slowdown);
        shot(dir, "31-blade", c14, L, w, h, ss);

        // The same stroke a moment later with the finger stopped but never lifted: the dwell has
        // ended it, so the edge is dying away along the last stretch it swept and the ribbon has
        // stopped growing. The readout stands on the count it froze at. That fade is the whole
        // visible answer to "why did my combo start over" — the next move is a new swipe.
        step(c14, L, Blade.STROKE_DWELL + DT);
        step(c14, L, Blade.STROKE_FADE * 0.45f);
        System.out.printf("blade rest: live=%s fade=%.2f, readout still says %d in one%n",
                c14.fingerDown, c14.strokeFade, c14.callKills);
        shot(dir, "58-blade-rest", c14, L, w, h, ss);

        // A MULTI chain mid-reveal: the bolt, the flares, and what it paid.
        GameCore c15 = new GameCore(store, 67L);
        c15.startGame();
        c15.score = 5200;
        step(c15, L, 1.9f);
        c15.enemies.clear();
        c15.target = null;
        Power mp = new Power();
        mp.glyph = 2;
        mp.effect = Power.MULTI;
        mp.y = L.playTop + 100f;
        mp.x = L.w * 0.5f;
        c15.power = mp;
        c15.tapKey(2, L);
        for (int k = 0; k < 4; k++) {
            GameCore.Enemy ce = new GameCore.Enemy();
            ce.word = new int[] {3, 1, 3};
            ce.need = new int[] {1, 1, 1};
            ce.gone = new boolean[3];
            ce.goneT = new float[3];
            ce.goneDx = new float[3];
            ce.goneDy = new float[3];
            ce.baseX = L.playLeft + (L.playRight - L.playLeft) * (0.22f + 0.19f * k);
            ce.y = L.playTop + (L.dangerY - L.playTop) * (0.24f + 0.16f * k);
            ce.enterT = 1f;
            c15.enemies.add(ce);
        }
        c15.tapKey(3, L);
        System.out.printf("chain: %d hops on %s worth %d%n", c15.chainLen,
                Glyph.NAME[c15.chainGlyph], c15.chainScore);
        // Part-way through the reveal, so the head of the chain is visibly still travelling.
        step(c15, L, GameCore.CHAIN_TIME * GameCore.CHAIN_REVEAL * 0.75f);
        System.out.printf("  revealed %d of %d%n", c15.chainShown, c15.chainLen);
        shot(dir, "32-chain", c15, L, w, h, ss);

        // TEAM SQUISH: the squishy grown a few sizes, mid-charge at a word.
        GameCore c16 = new GameCore(store, 71L);
        c16.startGame();
        c16.score = 6100;
        c16.collected = 0b0110_1101_0011_0110_1101L;
        step(c16, L, 1.9f);
        c16.playtestMode(Power.TEAM, L);
        // A few words to bounce off, and a few squishes already banked so the bubble is grown.
        for (int k = 0; k < 3; k++) {
            GameCore.Enemy te = new GameCore.Enemy();
            te.word = new int[] {2, 4};
            te.need = new int[] {1, 1};
            te.gone = new boolean[2];
            te.goneT = new float[2];
            te.goneDx = new float[2];
            te.goneDy = new float[2];
            te.baseX = L.playLeft + (L.playRight - L.playLeft) * (0.28f + 0.22f * k);
            te.y = L.playTop + (L.dangerY - L.playTop) * (0.30f + 0.18f * k);
            te.enterT = 1f;
            c16.enemies.add(te);
        }
        c16.buddy.squishes = 4;
        c16.tapKey(2, L);
        step(c16, L, 3 * DT);
        System.out.printf("team squish: %s, %d squished, r=%.0f glow=%.2f chasing=%s%n",
                Collect.NAME[c16.buddy.who], c16.buddy.squishes, c16.buddy.radius(L),
                c16.buddy.glow(), c16.buddy.chase != null);
        shot(dir, "33-team", c16, L, w, h, ss);

        // Settings panel, opened mid-game.
        GameCore c6 = new GameCore(store, 29L);
        c6.startGame();
        c6.score = 1420;
        c6.stage = 3;
        step(c6, L, 6f);
        c6.setSpeed(1.2f);
        c6.setBgm(Music.DRIFT);
        c6.collected = 0b0000_0100_1000_0011_0010_0110_1101L;
        c6.openSettings();
        step(c6, L, 0.3f);
        shot(dir, "12-settings", c6, L, w, h, ss);

        // Clear-collection button armed, waiting for the confirming tap.
        c6.tapClearCase();
        step(c6, L, 0.2f);
        System.out.printf("settings: clearArmed=%s case=%d%n", c6.clearArmed,
                Collect.owned(c6.collected));
        shot(dir, "23-settings-clear", c6, L, w, h, ss);

        bossFrames(dir, L, w, h, ss);
    }

    /**
     * One frame per boss, plus the arrival card and the burst.
     *
     * Every boss gets one because every boss draws something different — the whole point of five of
     * them — and a mechanic nobody has looked at is a mechanic nobody has checked.
     */
    private static void bossFrames(File dir, Layout L, int w, int h, int ss) throws Exception {
        // The arrival card, part way in.
        GameCore ci = toBoss(L, Boss.SLIME, 501L, false);
        step(ci, L, Boss.INTRO * 0.45f);
        System.out.printf("boss intro: %s, %.2f through%n", ci.boss.name(),
                ci.boss.introProgress());
        shot(dir, "59-boss-intro", ci, L, w, h, ss);

        // Each of the five, mid-fight, with its window open so the ornament is showing the thing it
        // is asking for.
        String[] tag = {"60-boss-slime", "61-boss-triplets", "62-boss-drum", "63-boss-magpie",
                "64-boss-sumo"};
        for (int k = 0; k < Boss.COUNT; k++) {
            GameCore c = toBoss(L, k, 510L + k, true);
            // Land a couple of hits so the health bar is part-spent and the body is dented, and so
            // the bosses that shed things have shed them.
            for (int n = 0; n < 3; n++) {
                for (int i = 0; i < 60 * 8 && !c.boss.open(); i++) c.update(DT, L);
                if (k == Boss.TRIPLETS) {
                    for (int i = 0; i < 3; i++) c.tapBoss(c.boss.ex[i], c.boss.ey[i], L);
                }
                if (k == Boss.SUMO) {
                    // Sink it into reach, press its belt to bank the swipe, and spend one — so the
                    // frame shows a part-spent bar and a part-full charge row. Only on the first
                    // round: at five health a staggered shove every round beats it before the shot,
                    // and the frame then had no boss in it at all.
                    if (n == 0) {
                        for (int i = 0; i < 60 * 12 && c.boss.depth < Boss.SHOVE_REACH; i++) {
                            c.update(DT, L);
                        }
                        c.target = null;
                        c.tapKey(c.boss.want(), L);
                        c.swipeUp(L);
                    }
                    continue;
                }
                c.target = null;
                for (int g = 0; g < Glyph.COUNT; g++) {
                    if (c.boss.wants(g)) {
                        c.tapKey(g, L);
                        break;
                    }
                }
                step(c, L, 0.12f);
            }
            // Held part way through the window, so the open glow and the caret are both up.
            for (int i = 0; i < 60 * 8 && !c.boss.open(); i++) c.update(DT, L);
            step(c, L, 0.10f);
            System.out.printf("boss %s: hp %.0f/%.0f, open=%s, elements=%d%n", c.boss.name(),
                    c.boss.hp, c.boss.hpMax, c.boss.open(), liveElems(c.boss));
            shot(dir, tag[k], c, L, w, h, ss);

            // The slime gets three more: a split still sitting inside it, the skin stretched out
            // after a drag, and the rebound the moment the glob comes free.
            if (k == Boss.SLIME) {
                // A bullet in flight at the boss, caught half way. Presses at the boss fire the
                // same shot presses at a word do, so this is the frame that proves it.
                for (int i = 0; i < 60 * 8 && !c.boss.open(); i++) c.update(DT, L);
                c.target = null;
                c.shots.clear();
                for (int g2 = 0; g2 < Glyph.COUNT; g2++) {
                    if (c.boss.wants(g2)) {
                        c.tapKey(g2, L);
                        break;
                    }
                }
                step(c, L, GameCore.SHOT_TIME * 0.5f);
                System.out.printf("boss bullet: %d in flight, at %.0f%% of the way%n",
                        c.shots.size(), c.shots.isEmpty() ? 0f : c.shots.get(0).t * 100f);
                shot(dir, "68-boss-bullet", c, L, w, h, ss);

                // A missed prompt launching, caught during the laugh and recoil. Kept on its own
                // slime so the wart recovery state below correctly suppresses this attack.
                GameCore volley = toBoss(L, Boss.SLIME, 519L, true);
                for (int i = 0; i < 60 * 8 && !volley.boss.open(); i++) volley.update(DT, L);
                volley.boss.promptT = 0f;
                volley.update(DT, L);
                step(volley, L, 0.16f);
                System.out.printf("boss volley: %d in the air, at %.2f/%.2f/%.2f%n",
                        volley.boss.boltCount(), volley.boss.boltAt(0), volley.boss.boltAt(1),
                        volley.boss.boltAt(2));
                shot(dir, "69-boss-volley", volley, L, w, h, ss);

                // Work one loose. Five presses of the chain split a glob off, so unlike every other
                // boss here there is nothing draggable on the field until the chain has been run —
                // which is what this used to look for before it had happened.
                int glob = -1;
                for (int q = 0; q < Boss.SPLIT_HITS * 4 && glob < 0; q++) {
                    for (int i = 0; i < 60 * 8 && !c.boss.open(); i++) c.update(DT, L);
                    c.target = null;
                    c.tapKey(c.boss.chainLetter(), L);
                    step(c, L, 0.10f);
                    for (int i = 0; i < Boss.ELEMS; i++) {
                        if (c.boss.draggable(i)) glob = i;
                    }
                }

                if (glob >= 0) {
                    step(c, L, 0.18f);
                    System.out.printf("boss split inside: at %.0f,%.0f, body at %.0f,%.0f r=%.0f%n",
                            c.boss.ex[glob], c.boss.ey[glob], c.boss.body.centreX(),
                            c.boss.body.centreY(), c.boss.body.radius());
                    shot(dir, "65-boss-split-inside", c, L, w, h, ss);

                    // Hauled most of the way to the edge, so the skin is stretched after it. Walked
                    // there over several frames: the tug is a force, so it needs time to act.
                    c.grabBoss(c.boss.ex[glob], c.boss.ey[glob]);
                    float fromX = c.boss.ex[glob], fromY = c.boss.ey[glob];
                    float toX = L.playLeft + c.boss.er[glob] * 2.6f, toY = L.h * 0.30f;
                    for (int q = 1; q <= 22; q++) {
                        float t = q / 22f;
                        c.dragBoss(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t, L);
                        c.update(DT, L);
                    }
                    System.out.printf("boss stretched: pulled=%s deform=%.3f held=%s%n",
                            c.boss.body.pulled(), c.boss.body.deform(), c.boss.held >= 0);
                    shot(dir, "65b-boss-stretched", c, L, w, h, ss);

                    // And free: the skin snaps back and rings.
                    c.dragBoss(L.playLeft - 4f, toY, L);
                    step(c, L, 0.10f);
                    System.out.printf("boss rebound: deform=%.3f held=%s%n",
                            c.boss.body.deform(), c.boss.held >= 0);
                    shot(dir, "65c-boss-rebound", c, L, w, h, ss);
                }
            }
        }

        // The settings panel's stage jump, parked on a boss stage so the row names the boss it is
        // sitting on — which is the state the control exists for.
        GameCore cj = toBoss(L, Boss.MAGPIE, 520L, true);
        cj.openSettings();
        step(cj, L, 0.3f);
        System.out.printf("stage jump: on stage %d, boss %s%n", cj.stage,
                Boss.NAMES[Boss.kindFor(cj.stage)]);
        shot(dir, "67-settings-stage-jump", cj, L, w, h, ss);

        // Beaten, mid-burst.
        GameCore cb = toBoss(L, Boss.SLIME, 530L, true);
        for (int i = 0; i < 60 * 60 && !cb.boss.beaten; i++) {
            cb.enemies.clear();
            cb.target = null;
            cb.lives = GameCore.START_LIVES;
            Check.bossPlay(cb, L);
            cb.update(DT, L);
        }
        step(cb, L, Boss.LEAVE * 0.4f);
        System.out.printf("boss beaten: %.2f through the burst%n", cb.boss.leaveProgress());
        shot(dir, "66-boss-beaten", cb, L, w, h, ss);
    }

    private static int liveElems(Boss b) {
        int n = 0;
        for (int i = 0; i < Boss.ELEMS; i++) if (b.etype[i] != Boss.E_OFF) n++;
        return n;
    }

    /** A run parked on the boss stage of the given kind, optionally past the arrival card. */
    private static GameCore toBoss(Layout L, int kind, long seed, boolean fighting) {
        Mem store = new Mem();
        GameCore c = new GameCore(store, seed);
        c.startGame();
        c.stage = Boss.EVERY * (kind + 1) - 1;
        c.enemies.clear();
        c.spawnedThisStage = c.stageQuota();
        for (int i = 0; i < 60 * 60 && !c.boss.active(); i++) c.update(DT, L);
        if (fighting) {
            for (int i = 0; i < 60 * 10 && !c.boss.fighting(); i++) c.update(DT, L);
        }
        return c;
    }

    // ---- driving ------------------------------------------------------------

    private static void step(GameCore c, Layout L, float seconds) {
        for (float t = 0; t < seconds; t += DT) c.update(DT, L);
    }

    private static GameCore.Enemy lowest(GameCore c) {
        GameCore.Enemy best = null;
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.typeable()) continue;
            if (best == null || e.y > best.y) best = e;
        }
        return best;
    }

    /** Kept across calls so a cadence limit survives short autoplay slices. */
    private static int autoBudget;

    /** Plays perfectly: always types the next glyph of the most urgent word. */
    private static void autoplay(GameCore c, Layout L, float seconds) {
        float t = 0;
        while (t < seconds && (c.state == GameCore.PLAY || c.state == GameCore.BONUS)) {
            c.update(DT, L);
            t += DT;
            // Human-ish cadence: at most one keypress every other frame.
            if (++autoBudget % 2 != 0) continue;
            if (c.state == GameCore.BONUS) {
                c.tapBonus(c.steamer.wanted());   // ignored while the spinner runs
                continue;
            }
            GameCore.Enemy e =
                    c.target != null && c.enemies.contains(c.target) && c.target.typeable()
                            ? c.target : lowest(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }
    }

    /**
     * Frame names to render, from the FRAMES env var, or null for all. Prefix match, so "65" takes
     * 65b and 65c with it.
     */
    private static final String[] only = pick();
    /** Crop box as 0..1 fractions from the CROP env var, or null for the whole frame. */
    private static final float[] crop = box();

    private static String[] pick() {
        String v = System.getenv("FRAMES");
        return v == null || v.isEmpty() ? null : v.split(",");
    }

    private static float[] box() {
        String v = System.getenv("CROP");
        if (v == null || v.isEmpty()) return null;
        String[] p = v.split(",");
        if (p.length != 4) return null;
        float[] b = new float[4];
        for (int i = 0; i < 4; i++) b[i] = Float.parseFloat(p[i]);
        return b;
    }

    /** True when this frame is wanted. Checked before rendering, since that is the cost. */
    static boolean wanted(String name) {
        if (only == null) return true;
        for (String o : only) {
            if (name.startsWith(o.trim())) return true;
        }
        return false;
    }

    private static void shot(File dir, String name, GameCore c, Layout L, int w, int h, int ss)
            throws Exception {
        if (!wanted(name)) return;
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        RasterPainter.clearFit();
        Renderer.draw(p, c, L);
        File f = new File(dir, name + ".png");
        int[] px = p.resolve();
        if (crop != null) {
            int x0 = (int) (crop[0] * w), y0 = (int) (crop[1] * h);
            int x1 = (int) (crop[2] * w), y1 = (int) (crop[3] * h);
            x0 = Math.max(0, Math.min(w - 1, x0));
            y0 = Math.max(0, Math.min(h - 1, y0));
            x1 = Math.max(x0 + 1, Math.min(w, x1));
            y1 = Math.max(y0 + 1, Math.min(h, y1));
            int cw = x1 - x0, ch = y1 - y0;
            int[] sub = new int[cw * ch];
            for (int y = 0; y < ch; y++) {
                System.arraycopy(px, (y0 + y) * w + x0, sub, y * cw, cw);
            }
            Png.write(f, sub, cw, ch);
        } else {
            Png.write(f, px, w, h);
        }
        System.out.printf("  wrote %-18s state=%d enemies=%d shots=%d particles=%d score=%d%n",
                f.getName(), c.state, c.enemies.size(), c.shots.size(), c.particles.size(),
                c.score);
        // Any line that ran off the screen on this frame. A clipped word is invisible in a PNG
        // unless you go looking, so the frame says so itself.
        for (String bad : RasterPainter.unfit) {
            System.out.println("    DOES NOT FIT  " + bad);
            unfitFrames++;
        }
    }

    /** Writes every effect and the music loop to WAV so they can be auditioned. */
    private static void sounds(File dir) throws Exception {
        File sfxDir = new File(dir, "sfx");
        sfxDir.mkdirs();
        String[] names = {"squish-dumpling", "squish-strawberry", "squish-cat", "squish-grapes",
                "squish-squishy", "squish-blob", "damage-drip", "clear-word", "wrong",
                "achievement", "game-start", "stage-clear", "power-clear", "chop", "zap",
                "collect", "star", "course-start", "tally", "parade-join", "game-over", "boss-laugh", "boss-damage", "boss-split", "bolt-pop"};
        int peak = 0;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            peak = Math.max(peak, max);
            Wav.write(new File(sfxDir, names[id] + ".wav"), pcm, Sfx.RATE);
        }
        for (int style = 0; style < Music.NAMES.length; style++) {
            if (!Music.isSynth(style)) continue;
            short[] loop = Music.loop(style);
            String slug = Music.NAMES[style].toLowerCase().replace(' ', '-');
            Wav.write(new File(sfxDir, "bgm-" + slug + ".wav"), loop, Sfx.RATE);
            int lmax = 0;
            for (int i = 0; i < loop.length; i++) lmax = Math.max(lmax, Math.abs(loop[i]));
            System.out.printf("  wrote bgm-%-12s %.2fs peak=%d%n", slug,
                    (float) loop.length / Sfx.RATE, lmax);
        }
        short[] fren = Music.loop(Music.SWING_STYLE, true);
        short[] boss = Music.bossLoop(Music.SWING_STYLE);
        Wav.write(new File(sfxDir, "bgm-frenzy.wav"), fren, Sfx.RATE);
        Wav.write(new File(sfxDir, "bgm-boss.wav"), boss, Sfx.RATE);
        System.out.printf("  wrote bgm-frenzy      %.2fs (four on the floor)%n",
                (float) fren.length / Sfx.RATE);
        System.out.printf("  wrote %d sfx, peak=%d%n", Sfx.COUNT, peak);
    }

    /**
     * Harness-only sheet: all ten stage vignettes, each sampled at four points through its
     * run, so the whole set can be reviewed in one look.
     */
    private static void skitSheet(File dir, Layout L, int w, int h, int ss) throws Exception {
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        p.fillRect(0, 0, w, h, Renderer.BG);
        float unit = 0.042f * w;
        p.text("STAGE VIGNETTES", w / 2f, unit * 2.2f, unit * 1.0f, Renderer.INK,
                Painter.CENTER, true);

        float[] samples = {0.15f, 0.45f, 0.7f, 0.95f};
        float rowH = (h - unit * 4f) / Skits.COUNT;
        float r = Math.min(rowH * 0.32f, w / (samples.length * 4.6f));
        for (int i = 0; i < Skits.COUNT; i++) {
            float cy = unit * 3.6f + rowH * (i + 0.55f);
            p.text(Skits.NAMES[i], unit * 0.6f, cy - rowH * 0.30f, unit * 0.50f,
                    Renderer.INK_DIM, Painter.LEFT, true);
            for (int k = 0; k < samples.length; k++) {
                float cx = w * (0.30f + 0.205f * k);
                Skits.draw(p, L, i, cx, cy, r, samples[k], 235, samples[k] * 4f);
            }
        }
        File f = new File(dir, "0-skits.png");
        Png.write(f, p.resolve(), w, h);
        System.out.println("  wrote " + f.getName() + " (" + Skits.COUNT + " vignettes)");
    }

    /**
     * Harness-only sheet: every story beat, sampled four times through its loop. Ten rows so
     * the whole cast of vignettes can be checked without opening thirty popups.
     */
    private static void beatSheet(File dir, Layout L, int w, int h, int ss) throws Exception {
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        p.fillRect(0, 0, w, h, Renderer.BG);
        float unit = 0.042f * w;
        p.text("STORY BEATS", w / 2f, unit * 2.0f, unit * 1.0f, Renderer.INK,
                Painter.CENTER, true);
        p.text("EACH SAMPLED THREE TIMES THROUGH ITS LOOP", w / 2f, unit * 3.0f, unit * 0.46f,
                Renderer.INK_DIM, Painter.CENTER, false);

        // One entry per beat, picked as the first collectible cast in it.
        int[] cast = new int[Lore.BEATS];
        for (int b = 0; b < Lore.BEATS; b++) cast[b] = -1;
        for (int i = Collect.COUNT - 1; i >= 0; i--) cast[Lore.BEAT[i]] = i;

        String[] names = {"HANDOFF", "STACK", "PUSH", "PEEK", "TUMBLE", "BOUNCE", "PICNIC",
                "CARRY", "CHEER", "SEEK"};
        float[] samples = {0.08f, 0.38f, 0.68f};
        float top = unit * 4.4f;
        float rowH = (h - top - unit) / Lore.BEATS;
        float r = Math.min(rowH * 0.28f, w / (samples.length * 7.4f));
        for (int b = 0; b < Lore.BEATS; b++) {
            float cy = top + rowH * (b + 0.5f);
            p.text(names[b], unit * 0.5f, cy, unit * 0.44f, Renderer.INK_DIM,
                    Painter.LEFT, true);
            p.text(Collect.NAME[cast[b]], unit * 0.5f, cy + unit * 0.62f, unit * 0.34f,
                    Glyph.withAlpha(Renderer.INK_DIM, 170), Painter.LEFT, false);
            for (int k = 0; k < samples.length; k++) {
                float cx = w * (0.30f + 0.26f * k);
                Storybook.beat(p, cast[b], cx, cy, r, samples[k], samples[k] * 5f);
            }
        }
        File f = new File(dir, "0-beats.png");
        Png.write(f, p.resolve(), w, h);
        System.out.println("  wrote " + f.getName() + " (" + Lore.BEATS + " beats)");
    }

    /**
     * Harness-only sheets: all thirty collectibles, once as collected and once as the unknown
     * silhouette. Two pages rather than one, because a single grid of sixty left no room for
     * the names and the labels landed on top of the row below.
     */
    private static void collectSheet(File dir, int w, int h, int ss) throws Exception {
        grid(dir, w, h, ss, "0-collect", "THE THIRTY COLLECTIBLES",
                "EVERY ENTRY, COLLECTED", true);
        grid(dir, w, h, ss, "0-collect-unknown", "NOT YET COLLECTED",
                "SILHOUETTE AND QUESTION MARK", false);
    }

    private static void grid(File dir, int w, int h, int ss, String file, String title,
            String sub, boolean known) throws Exception {
        final int cols = 5;
        int rows = (Collect.COUNT + cols - 1) / cols;
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        p.fillRect(0, 0, w, h, Renderer.BG);
        float unit = 0.042f * w;
        p.text(title, w / 2f, unit * 1.9f, unit * 0.95f, Renderer.INK, Painter.CENTER, true);
        p.text(sub, w / 2f, unit * 2.9f, unit * 0.52f, Renderer.INK_DIM, Painter.CENTER, false);

        float top = unit * 4.4f;
        float cellW = w / (float) cols;
        float cellH = (h - top - unit * 1.5f) / rows;
        // Leaves room under each hex for two lines of label without reaching the next row.
        float r = Math.min(cellW * 0.33f, cellH * 0.27f);
        for (int i = 0; i < Collect.COUNT; i++) {
            float cx = cellW * (i % cols + 0.5f);
            float cy = top + cellH * (i / cols) + r * 1.25f;
            int tint = known ? Collect.TIER_COLOR[Collect.TIER[i]] : Renderer.INK_DIM;
            p.fillPoly(Glyph.hex(cx, cy, r * 1.14f), Glyph.withAlpha(tint, known ? 34 : 18));
            p.strokePoly(Glyph.hex(cx, cy, r * 1.14f), Glyph.withAlpha(tint, known ? 190 : 70),
                    r * 0.06f);
            Trinket.draw(p, i, cx, cy, r * 0.92f, i * 0.7f, known, 1f);
            p.text(known ? Collect.NAME[i] : "??????", cx, cy + r * 1.62f, unit * 0.40f,
                    known ? Renderer.INK : Renderer.INK_DIM, Painter.CENTER, true);
            p.text(known ? Collect.TIER_NAME[Collect.TIER[i]]
                    : Collect.FAMILY_NAME[Collect.FAMILY[i]].split(" ")[0],
                    cx, cy + r * 2.06f, unit * 0.34f, tint, Painter.CENTER, false);
        }
        File f = new File(dir, file + ".png");
        Png.write(f, p.resolve(), w, h);
        System.out.println("  wrote " + f.getName() + " (" + Collect.COUNT + " entries)");
    }

    /** Harness-only sheet: every character large, for checking the faces read clearly. */
    private static void characterSheet(File dir, int w, int h, int ss) throws Exception {
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        p.fillRect(0, 0, w, h, Renderer.BG);
        float unit = 0.042f * w;
        p.text("THE SIX LETTERS", w / 2f, unit * 2.6f, unit * 1.1f, Renderer.INK,
                Painter.CENTER, true);

        float r = w / 7.6f;
        for (int g = 0; g < Glyph.COUNT; g++) {
            float cx = w * (0.22f + 0.28f * (g % 3));
            float cy = h * (0.24f + 0.22f * (g / 3));
            int col = Glyph.COLOR[g];
            p.fillPoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, 46));
            p.strokePoly(Glyph.hex(cx, cy, r), Glyph.withAlpha(col, 200), r * 0.07f);
            Kawaii.draw(p, g, cx, cy, r * 0.60f, col, 1f, 0.35f);
            p.text(Glyph.NAME[g].toUpperCase(), cx, cy + r * 1.40f, unit * 0.56f,
                    Renderer.INK_DIM, Painter.CENTER, true);
        }

        // Same characters at real tile size, plus a struck-and-strobing variant.
        float tr = 0.052f * w * Layout.HEAD_SCALE;
        p.text("AT TILE SIZE", w / 2f, h * 0.755f, unit * 0.66f, Renderer.INK_DIM,
                Painter.CENTER, true);
        for (int g = 0; g < Glyph.COUNT; g++) {
            float cx = w * (0.135f + 0.146f * g);
            int col = Glyph.COLOR[g];
            p.fillPoly(Glyph.hex(cx, h * 0.815f, tr), Glyph.withAlpha(col, 52));
            p.strokePoly(Glyph.hex(cx, h * 0.815f, tr), Glyph.withAlpha(col, 165), tr * 0.075f);
            Kawaii.draw(p, g, cx, h * 0.815f, tr * 0.60f, col, 1f, 0.4f);

            int hot = Glyph.mix(col, Glyph.cycle(g / 6f), 0.9f);
            p.fillPoly(Glyph.hex(cx, h * 0.90f, tr), Glyph.withAlpha(hot, 52));
            p.strokePoly(Glyph.hex(cx, h * 0.90f, tr), Glyph.withAlpha(hot, 165), tr * 0.075f);
            Kawaii.draw(p, g, cx, h * 0.90f, tr * 0.60f * 1.34f, hot, 1.16f, 0.4f);
        }
        p.text("STRUCK", w / 2f, h * 0.955f, unit * 0.56f, Renderer.INK_DIM, Painter.CENTER, true);

        File f = new File(dir, "0-characters.png");
        Png.write(f, p.resolve(), w, h);
        System.out.println("  wrote " + f.getName());
    }
}
