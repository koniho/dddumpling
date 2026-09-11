package com.dddumpling.game;

final class TestBack extends Check {
    static void navigation(Layout L) {
        Mem m = new Mem(); m.collected = 1;
        GameCore c = new GameCore(m, 701L);
        check("title delegates back to Android", !Pause.handlesBack(c) && !Pause.back(c));
        c.openCase(); c.caseIndex = 0; c.openStory();
        Pause.back(c);
        check("story back leaves display case open", !c.storyOpen() && c.caseOpen);
        Pause.back(c);
        check("case back returns to title", !c.caseOpen && c.state == GameCore.TITLE);
        c.openSettings(); Pause.back(c);
        check("settings back closes just settings", !c.settingsOpen && !c.paused);
        c.startGame(); c.openSettings(); Pause.back(c);
        check("game settings close without opening pause", c.state == GameCore.PLAY && !c.paused);
        Pause.back(c);
        float clock = c.clock, time = c.time, spawn = c.spawnTimer;
        int score = c.score;
        c.update(50, L); c.tapKey(0, L);
        check("pause freezes world, timers and gameplay input", c.paused && c.clock == clock
                && c.time == time && c.spawnTimer == spawn && c.score == score);
        Pause.action(c, 2);
        check("end run first requires confirmation", c.paused && c.confirmEnd && c.state == GameCore.PLAY);
        Pause.back(c);
        check("back from confirmation returns to pause", c.paused && !c.confirmEnd);
        Pause.back(c);
        check("back from pause resumes the same run", !c.paused && c.state == GameCore.PLAY && c.time == time);
        Pause.open(c); Pause.action(c, 2); Pause.action(c, 1);
        check("keep playing dismisses confirmation and resumes", !c.paused && !c.confirmEnd);

        for (int kind = 0; kind < Boss.COUNT; kind++) {
            c.startGame(); c.boss.begin(kind, (kind+1)*5, c.rnd, c.playRosterFull());
            c.boss.intro = 0; c.boss.held = -2;
            c.stars.left = c.stars.right = true; c.stars.beginDrag();
            c.beginStroke(L.w*.4f, L.h*.4f);
            Pause.back(c);
            float hp = c.boss.hp, age = c.boss.age, mode = c.modeLeft;
            c.update(30, L);
            check("boss " + kind + " freezes and drops held gestures", c.boss.hp == hp
                    && c.boss.age == age && c.modeLeft == mode && c.boss.held == -1
                    && !c.touchDown && !c.stars.dragging && !c.stars.left && !c.stars.right);
            Pause.back(c); c.update(DT, L);
            check("boss " + kind + " resumes", !c.paused && c.boss.age > age);
        }
        for (boolean stars : new boolean[] {false, true}) {
            c.startGame(); c.starNext = stars;
            Interlude.enterBonus(c, L); Pause.back(c);
            float timer = c.bonusTimer, starTimer = c.stars.timer;
            int hits = c.steamer.hits;
            c.update(60, L); c.tapBonus(0);
            check("paused minigame retains timer and rejects input: " + stars,
                    c.bonusTimer == timer && c.stars.timer == starTimer && c.steamer.hits == hits);
            Pause.action(c, 1);
            check("minigame resumes in place: " + stars, c.state == GameCore.BONUS && !c.paused);
        }
        c.startGame(); c.score = 3210; Interlude.awardBossPrize(c, Boss.OCTOPUS);
        long owned = c.collected; int total = c.collectTotal;
        int losses = c.earlyLosses;
        c.boss.begin(Boss.OCTOPUS, 15, c.rnd, c.playRosterFull());
        c.mode = Power.TEAM; c.modeLeft = 8;
        Pause.open(c); Pause.action(c, 2); Pause.action(c, 2);
        check("confirmed end saves score and collected rewards", c.state == GameCore.TITLE
                && c.best == 3210 && m.best == 3210 && c.collected == owned
                && m.collected == owned && c.collectTotal == total);
        check("ending clears active boss and frenzy without counting a loss", !c.boss.active()
                && c.mode == -1 && c.power == null && !c.paused && !c.confirmEnd && c.earlyLosses == losses);
        c.startGame(); c.lives = 1; c.takeHit(L.w*.5f, L);
        Pause.back(c);
        check("summary back returns to title", c.state == GameCore.TITLE && !Pause.handlesBack(c));
        c.beginStart();
        Pause.back(c); c.update(10, L);
        check("back cancels a pending start transition", c.state == GameCore.TITLE && !c.starting());
        for (int width : new int[] {320,640,1080}) {
            Layout small = new Layout(); small.compute(width,width*2,0,0,0,0);
            for (int button = 1; button <= 2; button++)
                check("pause button has its own touch target at " + width,
                        Pause.hit(small,width*.5f,Pause.buttonY(small,button)) == button);
            check("outside pause buttons is inert at " + width, Pause.hit(small,0,0) == 0);
        }
    }
}
