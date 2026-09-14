package com.dddumpling.game;

/** Native packet contract and gesture ownership, using the real game rules. */
public final class IOSInputTest extends Check {
    private static final class Host implements IOSGame.Host {
        int ticks;
        String privacy;
        public void tick() { ticks++; }
        public void openPrivacy(String url) { privacy = url; }
    }

    private static IOSGame game() {
        IOSGame game = new IOSGame(new Mem(), new Ear(), 9L);
        game.layout(393, 852, 0, 59, 0, 34);
        return game;
    }

    private static IOSTouch one(int action, int id, float x, float y) {
        return new IOSTouch(action, 0, new int[] {id}, new float[] {x}, new float[] {y});
    }

    private static IOSTouch two(int action, int index, int id0, float x0, float y0,
            int id1, float x1, float y1) {
        return new IOSTouch(action, index, new int[] {id0, id1},
                new float[] {x0, x1}, new float[] {y0, y1});
    }

    private static void tap(IOSGame game, float x, float y) {
        game.touch(one(IOSTouch.ACTION_DOWN, 42, x, y));
        game.touch(one(IOSTouch.ACTION_UP, 42, x, y));
    }

    private static void packets() {
        int[] ids = {9, 42};
        float[] xs = {10, 20}, ys = {30, 40};
        float[][] history = {{3, 4}, {5, 6}};
        IOSTouch event = new IOSTouch(IOSTouch.ACTION_MOVE, 0, ids, xs, ys, history, history);
        ids[0] = 0; xs[1] = 99; history[0][0] = 99;
        check("packet snapshots stable IDs and coordinates", event.getPointerId(0) == 9 && event.getX(1) == 20);
        check("pointer lookup uses identity rather than index", event.findPointerIndex(42) == 1 && event.findPointerIndex(1) == -1);
        check("coalesced samples are ordered and copied", event.getHistorySize() == 2
                && event.getHistoricalX(0, 0) == 3 && event.getHistoricalY(1, 1) == 6);
        boolean rejected = false;
        try { new IOSTouch(0, 0, new int[] {7, 7}, new float[2], new float[2]); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check("duplicate native identities are rejected", rejected);
    }

    private static void titleAndLifecycle() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        Host host = new Host(); game.setHost(host);
        check("music choice announced after backend attached", ((Ear) c.sound).musicCalls == 1);
        check("UIKit points preserve safe-area geometry", l.w == 393 && l.padT == 59 && l.padB == 34);
        check("title has no back navigation", !game.handlesBack() && !game.back());
        tap(game, l.w - 2*l.unit, l.dangerY - 2*l.unit);
        check("privacy opens host URL without starting play", PrivacyUi.URL.equals(host.privacy) && !c.starting());
        tap(game, l.keyX[0], l.keyY[0]);
        for (int i = 0; i < 180; i++) game.update(DT);
        check("title deck press starts real run", c.state == GameCore.PLAY);
        c.beginStroke(l.w/2, l.playTop + 20); c.stars.beginDrag();
        c.landPickerDragging = true; c.titleTouchDown = true; c.caseDragging = true;
        c.steamer.lidDrag = 30;
        game.background(true);
        float clock = c.clock; game.update(60);
        check("background pauses gameplay and freezes simulation", game.paused() && c.clock == clock);
        check("background releases all held controls", !c.touchDown && !c.stars.dragging
                && !c.landPickerDragging && !c.titleTouchDown && !c.caseDragging && c.steamer.lidDrag == 0);
        tap(game, l.w / 2, Pause.buttonY(l, 1));
        check("background ignores input", game.paused());
        game.background(false);
        check("foreground requires explicit resume", game.paused());
        game.touch(one(0, 8, l.w / 2, Pause.buttonY(l, 1)));
        game.touch(two(5, 1, 8, l.w / 2, Pause.buttonY(l, 1), 9, 0, 0));
        game.touch(one(1, 8, l.w / 2, Pause.buttonY(l, 1)));
        check("second finger cancels pause button selection", game.paused());
        tap(game, l.w / 2, Pause.buttonY(l, 1));
        check("resume button restores play", !game.paused());
        game.back();
        check("back opens pause", game.paused());
        tap(game, l.w / 2, Pause.buttonY(l, 2));
        check("end run requires confirmation", c.confirmEnd && c.state == GameCore.PLAY);
        game.back();
        check("back dismisses confirmation first", !c.confirmEnd && game.paused());
        game.back();
        check("second back resumes", !game.paused());
        clock = c.clock; game.update(Float.NaN); game.update(-1); game.update(Float.POSITIVE_INFINITY);
        check("invalid frame intervals do not poison simulation", c.clock == clock);
    }

    private static void starsAndLand() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        c.startGame(); c.playtestStars(l);
        float x = c.stars.x, y = c.stars.characterY(l);
        game.touch(one(0, 17, x, y));
        check("flyer touch begins direct steering", c.stars.dragging);
        game.touch(two(2, 0, 88, 0, 0, 17, x + 30, y));
        check("flyer follows owner after pointer array reorder", Math.abs(c.stars.x - (x+30)) < .01f);
        game.touch(two(6, 0, 88, 0, 0, 17, x+30, y));
        check("another finger lifting retains flyer", c.stars.dragging);
        game.touch(one(1, 17, x+30, y));
        check("owner lift releases flyer", !c.stars.dragging);
        game.touch(one(0, 17, c.stars.x, y));
        game.touch(one(3, 17, c.stars.x, y));
        check("native cancellation releases flyer", !c.stars.dragging);

        game = game(); c = game.core(); l = game.geometry();
        c.collected |= 7L << Collect.BOSS_FIRST;
        game.touch(one(0, 25, l.w/2, LandPicker.cardY(l)));
        check("unlocked land picker begins drag", c.landPickerDragging);
        game.touch(two(2, 0, 90, l.w, 0, 25, l.w*0.25f, LandPicker.cardY(l)));
        check("land picker follows stable owner", c.landChoice == 1);
        game.touch(one(2,25,0,LandPicker.cardY(l)));
        check("native long swipe cannot skip another land",c.landChoice==1 && c.landTravelQueue.isEmpty());
        game.background(true); game.background(false);
        check("background releases title land picker", !c.landPickerDragging);
        game.touch(one(2, 25, l.w, LandPicker.cardY(l)));
        check("stale land move cannot revive canceled drag", c.landChoice == 1);
    }

    private static void bossOwnership() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        c.startGame(); c.stage = 20; c.boss.begin(Boss.MUSHROOM, 20, c.rnd);
        for (int i = 0; i < 600 && !c.boss.fighting(); i++) game.update(DT);
        float x = c.boss.body.centreX(), y = c.boss.body.centreY();
        game.touch(one(0, 71, x, y));
        check("mushroom cap captures its owner", c.boss.held == -2);
        Host host = new Host(); game.setHost(host);
        game.touch(two(5, 1, 71, x, y, 92, l.keyX[0], l.keyY[0]));
        check("second thumb reaches deck during cap carry", host.ticks == 1 && c.boss.held == -2);
        game.touch(two(6, 1, 71, x, y, 92, l.keyX[0], l.keyY[0]));
        check("non-owner lift retains cap", c.boss.held == -2);
        game.touch(two(2, 0, 92, 0, 0, 71, x+10, y));
        check("cap movement looks up owner identity", Math.abs(c.boss.mushroomCapDX-10) < .01f);
        game.touch(one(3, 71, x+10, y));
        check("cancel releases cap", c.boss.held == -1);

        game = game(); c = game.core(); l = game.geometry();
        c.startGame(); c.stage = 10; c.boss.begin(Boss.SPLITTER, 10, c.rnd);
        for (int i = 0; i < 600 && !c.boss.fighting(); i++) game.update(DT);
        for (int i = 0; i < Boss.DIVIDE_HITS; i++) c.tapKey(c.boss.pieceWant(0), l);
        x = c.boss.pieceX(0,l); y = c.boss.pieceY(0,l);
        float spread = c.boss.pieceBody(0).spanY() * 1.1f;
        game.touch(one(0, 17, x, y-spread*.5f));
        game.touch(two(5, 1, 17, x, y-spread*.5f, 83, x, y+spread*.5f));
        check("two-finger spread captures charged divide", c.boss.pinchNode >= 0);
        game.background(true);
        check("background releases divide pinch", c.boss.pinchNode == -1 && game.paused());
    }

    private static void flingHistory() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        c.startGame(); c.playtestMode(Power.FLING,l); c.enemies.clear();
        GameCore.Enemy e = add(c,l,new int[] {0}, l.playTop + (l.deckTop-l.playTop)*.35f);
        float x = c.tileX(e,0,l), y = e.y;
        game.touch(one(0,4,x-50,y));
        game.touch(new IOSTouch(2,0,new int[] {4},new float[] {x-50},new float[] {y},
                new float[][] {{x+50}}, new float[][] {{y}}));
        check("coalesced fling sample cuts despite identical final position", e.destroyed || e.gone[0]);
        game.touch(one(3,4,x-50,y));
        check("fling cancellation extinguishes held stroke", !c.touchDown);
    }

    private static void steamerAndPanic() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        c.startGame(); c.playtestSteamer(l);
        for (int i = 0; i < 600 && !c.bonusMashing(); i++) game.update(DT);
        c.steamer.swipeReady = true;
        float x = l.w/2, y = Screens.steamerLidY(c,l);
        game.touch(one(0,19,x,y));
        game.touch(two(2,0,90,0,0,19,x,y-l.unit*.5f));
        check("steamer lid follows owning finger", Math.abs(c.steamer.lidDrag-l.unit*.5f)<.01f);
        game.touch(two(6,0,90,0,0,19,x,y-l.unit*.5f));
        check("non-owner lift retains lid drag", c.steamer.lidDrag>0);
        game.touch(one(3,19,x,y));
        check("cancel restores armed lid without claiming reward", c.steamer.lidDrag==0 && c.steamer.swipeReady);
        game.touch(one(0,19,x,y));
        game.touch(one(2,19,x,y-l.unit*2));
        check("upward swipe releases armed steamer", !c.steamer.swipeReady && c.steamer.freedT>0);

        game = game(); c = game.core(); l = game.geometry(); c.startGame();
        for (int i=0;i<180;i++) game.update(DT);
        c.enemies.clear(); add(c,l,new int[] {0}, l.dangerY-10); game.update(DT);
        x = l.w/2; y = (l.h/2+l.deckTop)/2;
        game.touch(one(0,3,x,y));
        game.touch(one(2,3,x,y-l.enemyR*2));
        check("lower-field upward swipe triggers panic", c.pushT>0);
        game.touch(one(1,3,x,y-l.enemyR*2));
    }

    private static void caseAndSettings() {
        IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
        tap(game,Showcase.iconCx(l,c.clock),Showcase.iconCy(l,c.clock));
        check("collection badge opens display case", c.caseOpen);
        game.touch(one(0,3,l.w/2,Showcase.focusCy(l)));
        game.touch(one(2,3,l.w/2-l.unit*2,Showcase.focusCy(l)));
        check("display case supports shelf drag", c.caseDragging);
        game.touch(one(3,3,l.w/2,Showcase.focusCy(l)));
        check("case cancellation releases shelf", !c.caseDragging);
        tap(game,l.keyX[0],l.keyY[0]);
        check("deck closes case without starting run", !c.caseOpen && !c.starting());
        tap(game,l.keyX[0],l.keyY[0]);
        for(int i=0;i<180;i++) game.update(DT);
        tap(game,l.w/2,l.hudY);
        check("stage readout opens developer settings", c.settingsOpen);
        SettingsUi ui = new SettingsUi(); ui.compute(l,Music.NAMES.length);
        tap(game,ui.sliderR,ui.sliderY);
        check("settings slider sets speed", c.speed==GameCore.SPEED_MAX);
        game.touch(one(0,3,ui.sliderL,ui.sliderY));
        game.touch(one(2,3,ui.closeCx,ui.closeCy));
        check("slider drag cannot activate close control", c.settingsOpen);
        int before = c.stage;
        tap(game,(ui.testChipL(2,4)+ui.testChipR(2,4))/2,ui.stageY+ui.stageH/2);
        check("stage chip jumps stage rather than starting frenzy", c.stage==before+1 && c.mode==-1 && c.settingsOpen);
        tap(game,ui.closeCx,ui.closeCy);
        check("settings close resumes play", !c.settingsOpen);
        for(int i=0;i<2;i++) {
            c.settingsOpen=true;
            tap(game,(ui.testChipL(i,2)+ui.testChipR(i,2))/2,ui.debuffY+ui.testH/2);
            check("native debuff chip activates correct effect " + i,!c.settingsOpen
                    && c.debuff==Power.INCOGNITO+i && c.debuffLeft>0f && !c.powerActive());
        }
    }

    private static void debugScenes() {
        IOSGame game = game();
        String before = game.debugStatus();
        game.debugScene("stage:abc"); game.debugScene("unknown");
        check("malformed debug scenes leave the live game intact", before.equals(game.debugStatus()));
        game.debugScene("stage:10");
        check("debug stage uses actual boss setup", game.core().stage==10 && game.core().boss.kind==Boss.SPLITTER);
        game.debugScene("pause");
        check("debug status exposes scene and pause for native UI assertions",
                game.debugStatus().contains("state=1;") && game.debugStatus().contains("paused=true;"));
        game.debugScene("title");
        check("debug title clears pause", !game.paused() && game.core().state==GameCore.TITLE);
    }

    private static void linkedChord() {
        for (float delay : new float[] {0f, 0.199f, 0.2f, 0.201f}) {
            IOSGame game = game(); GameCore c = game.core(); Layout l = game.geometry();
            c.startGame(); c.jumpToStage(16, l);
            c.stageGap = 0f; c.spawnTimer = 0f; c.powerTimer = 100f;
            game.update(DT);
            GameCore.Enemy a = c.enemies.get(0), b = c.enemies.get(1);
            float ax = c.keyX(l, a.word[0]), ay = c.keyY(l, a.word[0]);
            float bx = c.keyX(l, b.word[0]), by = c.keyY(l, b.word[0]);
            game.touch(one(IOSTouch.ACTION_DOWN, 9, ax, ay));
            check("native first thumb immediately cues partner", a.linkWaiting);
            game.update(delay);
            game.touch(two(IOSTouch.ACTION_POINTER_DOWN, 1, 9, ax, ay, 42, bx, by));
            check("native second thumb obeys 200ms window " + delay,
                    a.destroyed == (delay <= 0.2f) && b.destroyed == (delay <= 0.2f));
        }
    }

    public static void main(String[] args) {
        packets(); titleAndLifecycle(); starsAndLand(); bossOwnership(); flingHistory();
        steamerAndPanic(); caseAndSettings();
        debugScenes(); linkedChord();
        System.out.println("iOS input: " + pass + " passed, " + fail + " failed");
        if (fail != 0) throw new AssertionError("iOS input regressions");
    }
}
