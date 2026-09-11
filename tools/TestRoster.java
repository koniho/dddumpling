package com.dddumpling.game;

import java.util.Random;

final class TestRoster extends Check {
    static void adaptive(Layout L) {
        group("adaptive key roster");
        Mem fresh = new Mem(); fresh.rosterState = 0;
        GameCore c = new GameCore(fresh, 44);
        check("a first run has four keys", !c.fullRoster && Roster.count(c.fullRoster) == 4);
        check("starter cast is active", c.keyActive(0) && c.keyActive(1) && c.keyActive(4) && c.keyActive(5));
        check("cat and grapes begin off the deck", !c.keyActive(2) && !c.keyActive(3));
        check("hidden key cannot be hit", c.keyAt(L.keyX[2], L.keyY[2], L) == -1);
        check("four-key deck is enlarged", c.keyScale() > 1.1f);
        for (int n = 0; n < 500; n++) {
            GameCore.Enemy e = new GameCore.Enemy();
            Words.fill(e, 5, 0.5f, new Random(n), false);
            for (int g : e.word) check("starter words never request a hidden key", Roster.active(false, g));
        }
        c.startGame(); c.stage = Boss.EVERY;
        c.boss.begin(Boss.SLIME, c.stage, c.rnd, false); c.boss.beaten = true;
        BossPlay.endBoss(c, L);
        check("first boss adds cat and grapes", c.fullRoster && c.rosterScene == GameCore.ROSTER_JOIN);
        check("join is saved immediately", (fresh.rosterState & 1) != 0);
        advance(c, L, GameCore.ROSTER_SCENE_TIME + 0.1f);
        check("six-key deck settles at normal size", c.rosterScene == 0 && Math.abs(c.keyScale() - 1f) < 0.01f);

        Mem losses = new Mem(); losses.rosterState = 1;
        for (int run = 0; run < 3; run++) {
            GameCore loss = new GameCore(losses, 80 + run); loss.startGame(); loss.lives = 1;
            loss.takeHit(L.w / 2f, L);
        }
        check("three early losses remove advanced keys", (losses.rosterState & 1) == 0);
        check("farewell survives until title", (losses.rosterState & 8) != 0);
        GameCore farewell = new GameCore(losses, 90);
        check("pending farewell starts on title", farewell.rosterScene == GameCore.ROSTER_LEAVE && farewell.rosterMix() > 0.99f);
        advance(farewell, L, GameCore.ROSTER_SCENE_TIME + 0.1f);
        check("farewell leaves the four-key deck", farewell.rosterScene == 0 && !farewell.fullRoster);
        check("farewell is one shot", (losses.rosterState & 8) == 0);

        Mem safe = new Mem(); safe.rosterState = 1 | (2 << 1);
        GameCore reached = new GameCore(safe, 100); reached.startGame(); reached.jumpToStage(6, L);
        check("reaching stage six resets early losses", reached.earlyLosses == 0);
        for (int i = 0; i < 24; i++) check("title demo follows next roster", Roster.active(false, Demo.letter(farewell, i)));

        GameCore choice = new GameCore(new Mem(), 101); choice.startGame();
        boolean current = choice.runFullRoster;
        choice.setNextRoster(!current);
        check("settings choice changes only the next run", choice.runFullRoster == current
                && choice.fullRoster != current);
        choice.toTitle(); choice.startGame();
        check("next run adopts the chosen roster", choice.runFullRoster == choice.fullRoster);

        SettingsUi ui = new SettingsUi(); ui.compute(L, Music.NAMES.length);
        float mid = (ui.optionL() + ui.optionR()) / 2f;
        float left = (ui.optionL() + mid) / 2f, right = (mid + ui.optionR()) / 2f;
        check("next-roster chip is hittable", ui.hit(left, ui.runY + ui.runH / 2f)
                == SettingsUi.HIT_ROSTER);
        check("end-run button is hittable", ui.hit(right, ui.runY + ui.runH / 2f)
                == SettingsUi.HIT_GAMEOVER);
        choice.openSettings(); choice.endCurrentRun();
        check("end-run uses game over and closes settings", choice.state == GameCore.OVER
                && !choice.settingsOpen && choice.deathT == GameCore.DEATH_TIME);
    }
}
