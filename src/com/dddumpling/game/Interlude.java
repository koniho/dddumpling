package com.dddumpling.game;

/**
 * The between-stages interlude: the steamer mash, the star course, the blind box, and the parade.
 *
 * Works on {@code GameCore}'s fields rather than owning them, the {@link Fx} seam. {@link Steamer}
 * and {@link StarPath} own their own rules; this is the round around them — what a press does, what
 * winning pays out, and when the next stage begins.
 */
final class Interlude {

    private Interlude() {}

    static void joinChord(GameCore c) {
        if (c.joinRung || c.paradeProgress() < Parade.JOIN_END) return;
        c.joinRung = true;
        if (c.sound != null) c.sound.paradeJoin();
    }

    /** Drops into the between-stages minigame once the wave is clear. */
    static void enterBonus(GameCore c, Layout L) {
        c.state = GameCore.BONUS;
        c.time = 0;
        c.joinRung = false;
        c.statusRung = false;
        c.bossReward = c.bossPrizePending;
        c.bossPrizePending = false;
        if (c.bossReward) {
            c.starBonus = false;
            c.bonusTimer = BossCollect.REVEAL_TIME;
            c.paradeTimer = 0f;
            c.target = null;
            c.caretOwner = null;
            c.power = null;
            c.stageByPower = false;
            if (c.sound != null) c.sound.achievement();
            return;
        }
        c.starBonus = c.starNext;
        c.progress.startMinigame(c.starBonus);
        if (c.starBonus) {
            // A fresh line every attempt, with whatever is already in hand kept — see
            // StarPath.reroll for why a repeated attempt must not be a repeated course.
            c.stars.reroll(c.rnd);
            c.stars.begin(c.prize, L);
            c.bonusTimer = c.stars.timer;
            c.paradeTimer = 0f;
            c.target = null;
            c.caretOwner = null;
            c.power = null;
            if (c.sound != null) c.sound.stageClear();
            c.stageByPower = false;
            return;
        }
        // The mash is exactly what the round earned, and nothing else adds to it — a frenzy no
        // longer buys extra time here, because that bonus was wider than the whole earned ladder
        // and erased it. See bonusRollEnd for how the one timer carries all four phases.
        c.bonusRollEnd = c.earnedMash + GameCore.MASH_END;
        c.bonusTimer = GameCore.BONUS_ROLL + c.bonusRollEnd;
        c.paradeTimer = 0f;
        c.steamer.lidPulse = 0;
        c.steamer.flash = 0;
        // Chosen up front, before the spinner has shown anything: the spinner animates toward
        // an answer that already exists rather than deciding when it stops.
        c.steamer.pick(c.rnd, c.playRosterFull());
        c.rollTick = -1;
        c.target = null;
        c.caretOwner = null;
        c.power = null;
        if (c.sound != null) {
            // The frenzy tone replaces the ordinary one rather than stacking with it.
            if (c.stageByPower) c.sound.powerClear();
            else c.sound.stageClear();
        }
        c.stageByPower = false;
    }

    /**
     * A press during the interlude. Any of the six keys counts — this is a mash, not a
     * typing test — so it deliberately leaves hits, misses and combo alone, otherwise
     * mashing would inflate the accuracy readout.
     */
    static void tapBonus(GameCore c, int g) {
        if (!c.bonusMashing()) return;
        c.keyPress[g] = 1f;

        int r = c.steamer.press(g);
        if (r == Steamer.WRONG) {
            // Sounds wrong but is not counted as a miss: this is not a typing test, and it
            // must not reach the accuracy readout.
            c.keyBad[g] = 1f;
            if (c.sound != null) c.sound.wrong();
            return;
        }
        if (c.sound != null) c.sound.squish(g, 1);
        if (r == Steamer.READY) return;
        return;

    }

    /**
     * Opens the blind box the freed dumpling was carrying. A new entry goes into the case
     * and is written through to the store immediately; a duplicate pays out instead.
     *
     * The display case is left showing whatever came out, so the next visit to the title
     * screen opens on the prize rather than wherever the player had scrolled to.
     */
    /** Claims an armed steamer lid after an upward swipe over it. */
    static void swipeBonus(GameCore c) {
        if (!c.bonusSwipeReady() || c.steamer.swipe() != Steamer.FREED) return;
        c.progress.finishMinigame(true);
        if (c.store != null) c.store.saveSteamerOpens(c.steamer.opens);
        c.score += GameCore.FREE_BONUS;
        if (c.lives < GameCore.START_LIVES) c.lives++;
        awardPrize(c);
        c.starNext = true;
        c.bonusTimer = c.steamer.freedT;
        if (c.sound != null) c.sound.achievement();
    }

    static void dragBonusLid(GameCore c, float lift) {
        c.steamer.lidDrag = c.bonusSwipeReady() ? Math.max(0f, lift) : 0f;
    }

    /** Every award source records duplicates immediately, before its celebration starts. */
    private static void recordPrize(GameCore c, String source) {
        LandPicker.reward(c, c.prize);
        c.caseIndex = c.prize;
        c.caseSlide = c.caseSlideY = c.caseHighlightAge = 0f;
        c.caseFreePan = false;
        c.prizeNew = !Collect.has(c.collected, c.prize);
        c.roundPrizes = Collect.add(c.roundPrizes, c.prize);
        int previous = Math.max(c.collectionCounts[c.prize], c.prizeNew ? 0 : 1);
        c.collectionCounts[c.prize] = previous == Integer.MAX_VALUE ? previous : previous + 1;
        if (c.collectTotal < Integer.MAX_VALUE) c.collectTotal++;
        if (c.prizeNew) {
            c.collected = Collect.add(c.collected, c.prize);
            if (c.store != null) c.store.saveCollected(c.collected);
        } else c.score += GameCore.DUPE_BONUS;
        c.progress.reward(c.prize, c.prizeNew, source, c.score);
        if (c.store != null) {
            c.store.saveCollectionCounts(c.collectionCounts);
            c.store.saveCollectTotal(c.collectTotal);
        }
    }

    static void awardPrize(GameCore c) {
        c.prize = c.cubeUnlocked && c.stage >= Boss.EVERY
                ? Collect.rollCube(c.rnd, c.collected) : Collect.roll(c.rnd, c.collected);
        recordPrize(c, "steamer");
        // Scheduled, not started: it runs after the rest of the interlude has played out.
        c.paradeTimer = GameCore.PARADE_TIME;
    }

    /** Boss portraits are deterministic trophies, never random minigame drops. */
    static void awardBossPrize(GameCore c, int kind) {
        c.prize = Collect.BOSS_FIRST + kind;
        recordPrize(c, "boss");
        c.paradeTimer = 0f;
        c.bossPrizePending = true;
    }

    /** Star-path prizes are the five catalogue entries reserved for that game. */
    static void awardStarPrize(GameCore c) {
        c.prize = c.cubeUnlocked && c.stage >= Boss.EVERY
                ? Collect.rollCube(c.rnd, c.collected) : Collect.rollStar(c.rnd, c.collected);
        recordPrize(c, "starpath");
        // Scheduled, not started, exactly as the steamer does it: the victory tableau plays first
        // and the parade runs off what is left of the interlude.
        c.paradeTimer = GameCore.PARADE_TIME;
        if (c.sound != null) c.sound.achievement();
    }

    static void holdKey(GameCore c, int g, boolean down) {
        if (c.state == GameCore.BONUS && c.starBonus) c.stars.hold(g, down);
    }

    /**
     * The wave is done. Awards the flawless-wave dumpling and holds here until it has
     * finished, so the interlude opens after that celebration rather than on top of it.
     */
    static void beginStageEnd(GameCore c) {
        c.progress.completeStage(c.score);
        if (c.perfectRound()) {
            c.perfectBanner = GameCore.PERFECT_TIME;
            if (c.sound != null) c.sound.achievement();
        }
        // Taken here, before the counters go: this is the last frame on which how the round went is
        // still knowable. The interlude only spends it.
        c.earnedMash = c.mashEarned();
        c.missesThisStage = 0;
        c.hurtThisStage = 0;
        c.pendingBonus = true;
    }

}
