package com.sram.hexatype;

/** Stage waves, the between-stages minigame, accuracy and danger warnings. */
final class TestStages extends Check {

    static void waves(Layout L) {
        group("stage waves");
        GameCore c = new GameCore(new Mem(), 41L);
        c.startGame();
        check("stage starts with nothing released", c.spawnedThisStage == 0);
        check("quota grows with stage", quotaAt(c, 8) > quotaAt(c, 1));
        check("quota is capped", quotaAt(c, 99) <= 10);
        c.stage = 1;

        int frames = 0, stageChanges = 0;
        int prevStage = c.stage;
        boolean overQuota = false, spawnedEarly = false, emptyOnAdvance = true;

        // Play perfectly through several waves, watching the wave invariants every frame.
        // The interlude counts as being in the run, so BONUS must not end the loop.
        while (frames < 60 * 400 && stageChanges < 4
                && (c.state == GameCore.PLAY || c.state == GameCore.BONUS)) {
            c.update(DT, L);
            frames++;

            if (c.spawnedThisStage > c.stageQuota()) overQuota = true;
            if (c.stage != prevStage) {
                stageChanges++;
                // Sampled on the advancing frame itself: the last word of a wave leaves the
                // field during that same frame, so the previous frame can still hold it.
                if (!c.enemies.isEmpty()) emptyOnAdvance = false;
                if (c.spawnedThisStage != 0) spawnedEarly = true;
                prevStage = c.stage;
            }

            if (frames % 2 != 0) continue;
            if (c.state == GameCore.BONUS) {
                c.tapBonus(frames % Glyph.COUNT);
                continue;
            }
            GameCore.Enemy e =
                    c.target != null && c.enemies.contains(c.target) && c.target.typeable()
                            ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }

        System.out.printf("    %d stage transitions in %.0fs, reached stage %d%n",
                stageChanges, frames * DT, c.stage);
        check("stages do advance", stageChanges >= 3);
        check("a stage never releases more than its quota", !overQuota);
        check("field is empty when a stage advances", emptyOnAdvance);
        check("next wave starts from zero released", !spawnedEarly);

        // Once the quota is out and the field is clear, the gap must hold off the next wave.
        GameCore w = new GameCore(new Mem(), 42L);
        w.startGame();
        w.spawnedThisStage = w.stageQuota();
        w.enemies.clear();
        w.shots.clear();
        check("quota exhausted means the stage is cleared", w.stageCleared());
        int before = w.stage;
        w.update(DT, L);
        check("clearing the wave opens the interlude", advanceToBonus(w, L));
        advance(w, L, GameCore.BONUS_TIME + 0.2f);
        check("the stage advances after the interlude", w.stage == before + 1);
        check("a breather follows the interlude", w.stageGap > 0f);
        int released = w.spawnedThisStage;
        advance(w, L, GameCore.STAGE_GAP * 0.6f);
        check("nothing spawns during the breather",
                w.spawnedThisStage == released && w.enemies.isEmpty());
        advance(w, L, GameCore.STAGE_GAP);
        check("the next wave starts after the breather", w.spawnedThisStage > 0);

        // A breached word still counts as resolved, so a stage cannot stall on a miss.
        GameCore b = new GameCore(new Mem(), 43L);
        b.startGame();
        b.spawnedThisStage = b.stageQuota();
        b.enemies.clear();
        add(b, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        // A second word keeps the field occupied, so the stage cannot advance and reset the
        // counter before it can be observed.
        add(b, L, new int[] {3, 3}, L.playTop + 40);
        int resolved = b.resolvedThisStage;
        int stageWas = b.stage;
        advance(b, L, GameCore.ATTACK_TIME + 2 * DT);
        check("a breached word counts as resolved", b.resolvedThisStage == resolved + 1);
        check("a breach alone does not advance the stage", b.stage == stageWas);

        // Clear the survivor: the stage must then advance despite one word being lost.
        b.enemies.clear();
        b.shots.clear();
        b.update(DT, L);
        check("the interlude opens after a breach", advanceToBonus(b, L));
        advance(b, L, GameCore.BONUS_TIME + 0.2f);
        check("stage still advances after a breach", b.stage == stageWas + 1);
    }

    static int quotaAt(GameCore c, int stage) {
        int was = c.stage;
        c.stage = stage;
        int q = c.stageQuota();
        c.stage = was;
        return q;
    }

    /** The interlude opens only after the flawless-wave celebration has finished. */
    static void bonusOrdering(Layout L) {
        group("interlude ordering");
        GameCore c = new GameCore(new Mem(), 131L);
        c.startGame();
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);

        // A flawless wave: the reward shows first and the interlude waits for it.
        check("the perfect indicator is up", c.perfectBanner > 0f);
        check("still in play, holding", c.state == GameCore.PLAY && c.pendingBonus);
        check("the interlude has not opened", c.state != GameCore.BONUS);
        advance(c, L, GameCore.PERFECT_TIME * 0.5f);
        check("still holding part-way through the celebration",
                c.state == GameCore.PLAY && c.perfectBanner > 0f);
        check("nothing spawns while holding", c.enemies.isEmpty());
        advance(c, L, GameCore.PERFECT_TIME);
        check("the interlude opens once it finishes", c.state == GameCore.BONUS);
        check("the hold is released", !c.pendingBonus);

        // A flawed wave has no celebration to wait for, so it goes straight in.
        GameCore d = new GameCore(new Mem(), 132L);
        d.startGame();
        d.enemies.clear();
        d.tapKey(0, L);                       // a miss forfeits the reward
        d.spawnedThisStage = d.stageQuota();
        d.enemies.clear();
        d.shots.clear();
        d.update(DT, L);
        check("no celebration to wait for", d.perfectBanner == 0f);
        d.update(DT, L);
        check("a flawed wave goes straight to the interlude", d.state == GameCore.BONUS);
    }

    static void steamerBonus(Layout L) {
        group("between-stages minigame");
        GameCore c = new GameCore(new Mem(), 121L);
        c.startGame();
        check("no steamer damage at the start", c.steamer.hits == 0 && c.steamer.opens == 0);

        // Clearing a wave drops into the minigame, not straight into the next stage.
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);
        check("clearing a wave enters the minigame", advanceToBonus(c, L));
        check("the stage has not turned over yet", c.stage == 1);
        check("the lid starts shut", c.steamer.lidOpen() == 0f);

        int hitsBefore = c.hits, missesBefore = c.misses;
        for (int g = 0; g < Glyph.COUNT; g++) c.tapBonus(g);
        check("any of the six keys lands a hit", c.steamer.hits == Glyph.COUNT);
        check("a press pops the lid", c.steamer.lidPulse > 0f);
        check("a press flashes the container", c.steamer.flash > 0f);
        check("the lid is partway open", c.steamer.lidOpen() > 0f && c.steamer.lidOpen() < 1f);
        // Mashing must not pollute the accuracy readout.
        check("mashing is not counted as typing",
                c.hits == hitsBefore && c.misses == missesBefore);

        advance(c, L, 1.0f);
        check("the press animations settle", c.steamer.lidPulse == 0f && c.steamer.flash == 0f);

        // Damage carries over: run the interlude out and check the count survives.
        int carried = c.steamer.hits;
        advance(c, L, GameCore.BONUS_TIME + 0.2f);
        check("the minigame ends by itself", c.state == GameCore.PLAY);
        check("the stage turns over on the way out", c.stage == 2);
        check("steamer damage carries across the interlude", c.steamer.hits == carried);
        check("presses outside the minigame are ignored by it", c.steamer.hits == carried);
        c.tapBonus(0);
        check("tapBonus does nothing during play", c.steamer.hits == carried);

        // Second interlude: finish the job and check the reward. The post-interlude breather
        // has to run out first, or the wave gate is never reached.
        advance(c, L, GameCore.STAGE_GAP + 0.1f);
        c.spawnedThisStage = c.stageQuota();
        c.enemies.clear();
        c.shots.clear();
        c.update(DT, L);
        check("back in the minigame", advanceToBonus(c, L));
        c.lives = GameCore.START_LIVES - 1;
        int scoreBefore = c.score;
        // Bounded: tapBonus is a no-op outside BONUS, so an unbounded loop would hang.
        for (int i = 0; i <= GameCore.STEAMER_HITS && c.steamer.hits > 0; i++) c.tapBonus(2);
        check("twenty presses free the dumpling", c.steamer.opens == 1);
        check("the counter resets so it can be earned again", c.steamer.hits == 0);
        check("freeing it scores", c.score == scoreBefore + GameCore.FREE_BONUS);
        check("freeing it returns a lost life", c.lives == GameCore.START_LIVES);
        check("the escape animation runs", c.steamer.freedT > 0f);
        check("the interlude is held open for it", c.bonusTimer >= c.steamer.freedT);

        int opensNow = c.steamer.opens;
        c.tapBonus(3);
        check("presses during the escape do not re-trigger", c.steamer.opens == opensNow);

        advance(c, L, GameCore.BONUS_TIME + 2f);
        check("play resumes after the celebration", c.state == GameCore.PLAY);
        check("lives are capped at the starting count", c.lives <= GameCore.START_LIVES);

        // A full run must be able to reach the minigame repeatedly without wedging.
        GameCore r = new GameCore(new Mem(), 122L);
        r.startGame();
        int bonuses = 0, frames = 0;
        boolean sane = true;
        while (frames < 60 * 240 && r.state != GameCore.OVER) {
            int was = r.state;
            r.update(DT, L);
            frames++;
            if (was != GameCore.BONUS && r.state == GameCore.BONUS) bonuses++;
            if (r.state == GameCore.BONUS) {
                // Human-ish mash rate, so the reported free count means something.
                if (frames % 8 == 0) r.tapBonus(frames % Glyph.COUNT);
                continue;
            }
            if (r.state != GameCore.PLAY) continue;
            if (frames % 2 != 0) continue;
            GameCore.Enemy e = r.target != null && r.enemies.contains(r.target)
                    && r.target.typeable() ? r.target : urgent(r);
            if (e != null && e.pos < e.word.length) r.tapKey(e.word[e.pos], L);
            if (r.steamer.hits < 0 || r.steamer.hits >= GameCore.STEAMER_HITS) sane = false;
        }
        System.out.printf("    %d interludes in %.0fs, freed %d, stage %d%n",
                bonuses, frames * DT, r.steamer.opens, r.stage);
        check("interludes recur across a run", bonuses >= 3);
        check("steamer damage stays in range", sane);
        check("the dumpling gets freed during a long run", r.steamer.opens >= 1);
    }

    static void accuracyTracking(Layout L) {
        group("accuracy");
        GameCore c = new GameCore(new Mem(), 61L);
        c.startGame();
        check("accuracy starts at 100%", c.accuracyPercent() == 100);
        check("no presses means the happiest face", c.accuracyMood() == 1f);

        c.enemies.clear();
        c.target = null;
        add(c, L, new int[] {0, 1, 2, 3}, L.playTop + 90);
        c.tapKey(0, L);
        check("a correct press counts as a hit", c.hits == 1 && c.misses == 0);
        c.tapKey(5, L);
        check("a wrong press counts as a miss", c.hits == 1 && c.misses == 1);
        check("accuracy halves at one for one", c.accuracyPercent() == 50);
        check("50% is the saddest face", c.accuracyMood() == 0f);

        // Push to 75%: three hits, one miss.
        c.tapKey(0, L);
        c.tapKey(1, L);
        check("accuracy climbs with hits", c.accuracyPercent() == 75);
        check("75% sits between the extremes",
                c.accuracyMood() > 0f && c.accuracyMood() < 1f);

        // Threshold checks, driven directly.
        c.hits = 60; c.misses = 40;
        check("60% is the saddest face", c.accuracyPercent() == 60 && c.accuracyMood() == 0f);
        c.hits = 59; c.misses = 41;
        check("below 60% stays saddest", c.accuracyMood() == 0f);
        c.hits = 90; c.misses = 10;
        check("90% is the happiest face", c.accuracyPercent() == 90 && c.accuracyMood() == 1f);
        c.hits = 97; c.misses = 3;
        check("above 90% stays happiest", c.accuracyMood() == 1f);
        c.hits = 75; c.misses = 25;
        check("75% maps to the midpoint", Math.abs(c.accuracyMood() - 0.5f) < 0.001f);

        // A flawless wave earns the gold dumpling; a single miss forfeits it.
        GameCore g = new GameCore(new Mem(), 62L);
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        // The reward now fires the moment the wave clears, ahead of the interlude.
        check("a flawless wave triggers the gold dumpling", g.perfectBanner > 0f);
        check("the celebration expires", g.perfectBanner <= GameCore.PERFECT_TIME);
        advance(g, L, GameCore.PERFECT_TIME + 0.2f);
        check("the celebration ends", g.perfectBanner == 0f);
        check("the celebration ends", g.perfectBanner == 0f);

        GameCore m = new GameCore(new Mem(), 63L);
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);   // nothing to hit: a miss
        check("the miss is recorded against the stage", m.missesThisStage == 1);
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        advance(m, L, GameCore.BONUS_TIME + 0.2f);
        check("a wave with a miss earns no gold dumpling", m.perfectBanner == 0f);
        check("stage misses reset for the next wave", m.missesThisStage == 0);
        check("run totals are not reset by the stage", m.misses == 1);
    }

    static void warningsAndHarm(Layout L) {
        group("warnings and harm");
        GameCore c = new GameCore(new Mem(), 21L);
        c.startGame();
        c.enemies.clear();

        GameCore.Enemy far = add(c, L, new int[] {0, 0}, L.playTop + 20);
        c.update(DT, L);
        check("a distant word raises no alarm", far.warn == 0f && c.warnLevel == 0f);

        c.enemies.clear();
        float band = (L.dangerY - L.playTop) * 0.20f;
        GameCore.Enemy near = add(c, L, new int[] {0, 0}, L.dangerY - band * 0.4f);
        c.update(DT, L);
        check("a closing word raises the alarm", near.warn > 0.5f && near.warn <= 1f);
        check("warnLevel follows the worst offender", c.warnLevel == near.warn);
        check("a closing word is still typeable", near.typeable());

        check("full health means no red tint", c.harm() == 0f);
        c.lives = 2;
        float h2 = c.harm();
        c.lives = 1;
        check("harm rises as lives fall", c.harm() > h2 && h2 > 0f);
        check("harm peaks below or at 1", c.harm() <= 1f);
        c.lives = 0;
        c.state = GameCore.OVER;
        check("no tint outside play", c.harm() == 0f);

        for (int i = 0; i < 6; i++) {
            check("cycle colour " + i + " is opaque", (Glyph.cycle(i / 6f) >>> 24) == 0xFF);
        }
        check("cycle wraps", Glyph.cycle(0f) == Glyph.cycle(1f));
    }

}
