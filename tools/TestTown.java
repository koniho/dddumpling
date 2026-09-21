package com.dddumpling.game;

final class TestTown extends Check {
    private TestTown() {}

    static void all(Layout L) {
        persistenceAndRewards(L);
        meadowAndSlide(L);
        slimeFight(L);
        coreBridge(L);
        decorationMotion(L);
        translatedLabels(L);
        flowerSettings(L);
    }

    private static void flowerSettings(Layout L) {
        GameCore c=new GameCore(new Mem(),927L);
        Town t=c.town;t.flowerCount=3;t.dialogue=Town.TALK_NONE;t.markSaved();
        TownScreen.down(c,L,L.w*.5f,TownScreen.flowerChipY(L));
        check("flower chip sets blooming and saves without scene touch",t.flowerCount==12
                && t.dirty && t.touchAge>=2f && t.targetPoi==Town.NONE);
        t.cycleFlowerGrowth();
        Town restored=new Town();restored.load(t.save());
        check("full bloom setting persists",restored.flowerCount==Town.MAX_FLOWERS);
        t.cycleFlowerGrowth();
        check("flower setting cycles back to sprouts",t.flowerCount==3&&t.flowerTime==0f);
    }

    private static void translatedLabels(Layout L) {
        RasterPainter p = new RasterPainter((int)L.w,(int)L.h,1);
        RasterPainter.clearFit();
        p.save();
        p.translate(-L.w,0);
        p.text("TOWN",L.w*1.5f,L.h*.5f,20,0xFFFFFFFF,Painter.CENTER,true);
        check("scrolled label diagnostics use screen coordinates",RasterPainter.unfit.isEmpty());
        p.text("TOWN",L.w*1.99f,L.h*.5f,20,0xFFFFFFFF,Painter.CENTER,true);
        check("scrolled label diagnostics still detect cropped text",!RasterPainter.unfit.isEmpty());
        p.restore();
        RasterPainter.clearFit();
    }

    private static void decorationMotion(Layout L) {
        group("town decoration motion");
        GameCore c = new GameCore(new Mem(), 919L);
        Town t = c.town;
        t.dialogue = Town.TALK_NONE;
        t.path = .5f;
        float x=L.w*.3f,y=L.playTop+L.unit;
        int tickets=t.tickets;
        check("scenery taps are handled",TownScreen.down(c,L,x,y));
        check("scenery touch uses world coordinates",Math.abs(t.touchX-x-TownScreen.cameraX(t,L))<.01f
                && t.touchY==y && t.touchAge==0f);
        t.update(.05f,L);
        check("nearby furniture springs while distant objects stay still",
                t.motion(t.touchX,t.touchY,L.w*.2f)>.1f
                && t.motion(t.touchX+L.w,t.touchY,L.w*.2f)==0f);
        check("decoration play leaves travel and tickets alone",t.targetPoi==Town.NONE && t.tickets==tickets);
        for(int i=0;i<30;i++)t.update(.1f,L);
        check("touch springs settle completely",t.motion(t.touchX,t.touchY,L.w)==0f);
        t.react(x,y);
        TownScreen.down(c,L,StarScreen.sliderRight(L),StarScreen.sliderY(L));
        check("control touches do not trigger scenery",t.touchX==x && t.touchY==y);
        t.enter(new int[Collect.COUNT],false);
        check("opening town clears old touch impulses",t.touchAge>=2f);
    }

    private static void persistenceAndRewards(Layout L) {
        group("town persistence and rewards");
        Town t = new Town();
        long run = t.beginRun();
        int paid = t.grantRunReward(run, 2700);
        check("a completed run earns town tickets", paid > 1 && t.tickets == paid);
        check("the same run cannot pay twice", t.grantRunReward(run, 999999) == 0
                && t.tickets == paid);
        check("an older run cannot pay after a newer one", t.grantRunReward(run - 1, 999999) == 0);

        t.enter(new int[Collect.COUNT], false);
        check("first visit provides a decoration budget", t.welcomed
                && t.tickets >= Town.ARCH_COST && t.dialogue == Town.TALK_WELCOME);
        t.flowerTime = 13.95f;
        int flowers = t.flowerCount;
        t.update(.1f, L);
        check("time in town grows flowers", t.flowerCount == flowers + 1 && t.dirty);

        String saved = t.save();
        Town restored = new Town();
        restored.load(saved);
        check("town save restores durable progress", restored.tickets == t.tickets
                && restored.visits == t.visits && restored.flowerCount == t.flowerCount
                && restored.welcomed == t.welcomed && restored.lastClaimedRun == t.lastClaimedRun);
        restored.load("v1;t=oops");
        check("bad town save leaves progress intact", restored.tickets == t.tickets);
    }

    private static void meadowAndSlide(Layout L) {
        group("town meadow and slide");
        Town t = new Town();
        int[] owned = new int[Collect.COUNT];
        owned[7] = 1;
        t.enter(owned, true);
        check("the slide has a default rider without collection ownership", t.rider == 0 && !t.owns(0));
        t.nextRider();
        check("rider selection includes collected dumplings", t.rider == 7 && t.owns(7));
        t.nextRider();
        t.enter(owned, true);
        check("rider selection cycles to and preserves the default", t.rider == 0);

        float start = t.path;
        t.steer(.3f);
        t.update(.5f, L);
        float slow = t.path - start;
        t.path = start;
        t.steer(1f);
        t.update(.5f, L);
        check("the control bar modulates bounce travel speed", t.path - start > slow * 1.8f);

        t.path = .1f;
        float x1 = TownScreen.pathX(.1f, L), x2 = TownScreen.pathX(.6f, L);
        float y1 = TownScreen.pathY(.1f, L), y2 = TownScreen.pathY(.35f, L);
        check("the route travels left to right and meanders vertically", x2 > x1
                && Math.abs(y2 - y1) > L.w * .02f);
        boolean grounded=true;
        for(int i=0;i<=100;i++) {
            float at=i/100f;
            grounded &= TownScreen.pathY(at,L)-L.w*.063f >= TownScreen.meadowTop(TownScreen.pathX(at,L),L)
                    && TownScreen.landmarkGround(at,L) >= TownScreen.meadowTop(TownScreen.pathX(at,L),L)+L.w*.06f;
        }
        check("the full route and landmark bases stay on the main meadow",grounded);
        float low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
        for(int i=0;i<=100;i++) {
            float y=TownScreen.meadowTop(L.w*3f*i/100f,L);
            low=Math.min(low,y);high=Math.max(high,y);
        }
        check("main meadow horizon has rolling hills",high-low>L.w*.04f);
        boolean plotsClear=true;
        for(float at:new float[]{.20f,.66f,.88f})
            plotsClear &= Math.abs(TownScreen.pathY(at,L)-TownScreen.landmarkGround(at,L))>L.w*.20f
                    && TownScreen.inPlot(TownScreen.pathX(at,L),TownScreen.landmarkGround(at,L),0f,L);
        check("attraction plots are separate from the walking path",plotsClear);
        check("path bends leave plots on both sides",TownScreen.pathY(.20f,L)>TownScreen.landmarkGround(.20f,L)
                && TownScreen.pathY(.66f,L)<TownScreen.landmarkGround(.66f,L));
        check("camera clamps at the entrance", TownScreen.cameraX(t, L) == 0f);
        t.path = .5f;
        check("camera follows the traveler through the wider town",
                Math.abs(TownScreen.screenPathX(t, t.path, L) - L.w*.5f) < .01f);
        t.path = .945f;
        check("camera clamps at the far end", TownScreen.cameraX(t, L) == L.w*2f);
        t.travelTo(Town.FRIEND);
        for (int i = 0; i < 240 && t.targetPath >= 0f; i++) t.update(DT, L);
        check("tapping an interest walks there and opens it", t.targetPath < 0f
                && t.dialogue == Town.TALK_BUILD_ARCH && Math.abs(t.path - .2f) < .001f);

        int before = t.tickets;
        check("the friend offers an optional meadow decoration",
                t.dialogue == Town.TALK_BUILD_ARCH);
        check("tickets buy the flower arch once", t.buyArch() && t.archBuilt
                && t.tickets == before - Town.ARCH_COST && !t.buyArch());
        t.path = .1f;
        t.travelTo(Town.FRIEND);
        for (int i = 0; i < 240 && t.targetPath >= 0f; i++) t.update(DT, L);
        check("the decorated friend shares a repeatable secret", t.dialogue == Town.TALK_FRIEND);

        t.path = .1f;
        t.travelTo(Town.SLIDE_POI);
        for (int i = 0; i < 360 && t.targetPath >= 0f; i++) t.update(DT, L);
        check("the launch slide is playable without a purchase", t.mode == Town.SLIDE);
        t.holdSlide();
        for (int i = 0; i < 75; i++) t.update(DT, L);
        float charge = t.slideCharge;
        t.releaseSlide();
        check("release starts the slide ramp", charge > .5f && t.slideFlying && !t.slideAirborne);
        for (int i = 0; i < 180 && !t.slideAirborne; i++) t.update(DT, L);
        check("the ramp launches the rider into the air", t.slideAirborne
                && t.slideVX > 0f && t.slideVY < 0f);
        for (int i = 0; i < 300 && !t.slideLanded; i++) t.update(DT, L);
        check("the slide flight lands safely", t.slideLanded && !t.slideFlying
                && Math.abs(t.slideY - .78f) < .001f);

        t.finishSlide();
        t.beginSlimeFight(L);
        check("Slime owner can enter the attraction", t.mode == Town.FIGHT);
        t.finishSlimeFight();
        check("leaving Slime Fight returns to its town stop", t.mode == Town.MEADOW
                && Math.abs(t.path - .88f) < .001f);
        Town locked = new Town();
        locked.enter(owned, false);
        locked.beginSlimeFight(L);
        check("Slime Fight stays locked without the boss friend", locked.mode == Town.MEADOW);
    }

    private static void slimeFight(Layout L) {
        group("town slime fight");
        SlimeFight idle = new SlimeFight();
        idle.begin(L, 99);
        for (int i = 0; i < 360; i++) idle.update(DT, L);
        check("watching slime without joining cannot farm FUN", idle.phase == SlimeFight.PLAY
                && idle.fun == 0 && idle.dodges == 0 && idle.splashed == 0 && !idle.engaged);

        SlimeFight seededA = new SlimeFight(), seededB = new SlimeFight();
        seededA.begin(L, 771); seededB.begin(L, 771);
        for (int i = 0; i < 240; i++) { seededA.update(DT, L); seededB.update(DT, L); }
        boolean sameGlobs = true;
        for (int i = 0; i < SlimeFight.MAX_GLOBS; i++)
            sameGlobs &= seededA.globLive[i] == seededB.globLive[i]
                    && Math.abs(seededA.globX[i] - seededB.globX[i]) < .001f
                    && Math.abs(seededA.globY[i] - seededB.globY[i]) < .001f;
        check("fight RNG is repeatable within its own seed", sameGlobs
                && Math.abs(seededA.rivalX - seededB.rivalX) < .001f);

        SlimeFight f = new SlimeFight();
        f.begin(L, 1234);
        check("fight begins ready with reused spring bodies", f.phase == SlimeFight.READY
                && f.playerBody != null && f.rivalBody != null && f.fun == 0);
        for (int i = 0; i < 100; i++) f.update(DT, L);
        check("ready opens into timed play", f.phase == SlimeFight.PLAY
                && f.timeLeft < SlimeFight.PLAY_TIME);

        float sliderY = StarScreen.sliderY(L), x = f.playerX;
        f.touch(L, 0, 1, StarScreen.sliderRight(L), sliderY);
        check("Star Path control places the player", f.playerX > x && f.steerPointer == 1);
        float placed = f.playerX;
        f.touch(L, 0, 3, StarScreen.sliderLeft(L), sliderY);
        check("a second finger cannot steal steering", f.steerPointer == 1
                && Math.abs(f.playerX - placed) < .001f);

        float y = SlimeFight.playerY(L);
        f.touch(L, 0, 2, f.playerX, y);
        f.update(.08f, L);
        f.touch(L, 2, 2, f.playerX + L.w * .04f, y - L.w * .22f);
        f.touch(L, 1, 2, f.playerX + L.w * .04f, y - L.w * .22f);
        check("steering and an upward fling work with two fingers", f.throwsMade == 1
                && f.hits == 0 && f.fun >= 2 && f.steerPointer == 1 && f.flingPointer < 0);
        f.cancelInput();
        check("input cancellation releases both gestures", f.steerPointer < 0
                && f.flingPointer < 0 && !f.sliderGesture && !f.flingGesture);

        int baseFun = f.fun;
        f.globLive[3] = true; f.globPlayer[3] = false;
        f.globX[3] = 0; f.globY[3] = L.deckTop + SlimeFight.globR(L) + 1;
        f.globVX[3] = f.globVY[3] = 0;
        f.update(DT, L);
        check("dodging slime earns FUN", f.dodges == 1 && f.fun >= baseFun + 3);
        baseFun = f.fun;
        f.globLive[4] = true; f.globPlayer[4] = false;
        f.globX[4] = f.playerX; f.globY[4] = SlimeFight.playerY(L);
        f.globVX[4] = f.globVY[4] = 0;
        f.update(DT, L);
        check("getting splashed is still playful", f.splashed == 1 && f.fun == baseFun + 1);
        int splashFun = f.fun, splashes = f.splashed;
        f.update(DT, L);
        check("one slime event scores once and is nonfatal", f.splashed == splashes
                && f.fun == splashFun && f.phase == SlimeFight.PLAY);
        baseFun = f.fun;
        f.globLive[5] = true; f.globPlayer[5] = true;
        f.globX[5] = f.rivalX; f.globY[5] = SlimeFight.rivalY(L);
        f.globVX[5] = f.globVY[5] = 0;
        f.update(DT, L);
        check("splatting the rival earns FUN", f.hits == 1 && f.fun == baseFun + 8);

        SlimeFight active = new SlimeFight();
        active.begin(L, 456);
        for (int i = 0; i < 100; i++) active.update(DT, L);
        active.steer(-1f, L); active.steer(1f, L);
        for (int i = 0; i < 120; i++) active.update(DT, L);
        check("active no-hit play still earns FUN", active.phase == SlimeFight.PLAY
                && active.hits == 0 && active.fun > 0 && active.movementFun > 0);

        f.dodges = 5; f.hits = 1; f.splashed = 1; f.movementFun = 3;
        f.update(DT, L);
        check("play achievements recognize varied kinds of FUN", (f.achievements()
                & (SlimeFight.ACH_DODGER | SlimeFight.ACH_SPLAT_PALS | SlimeFight.ACH_BOUNCER))
                == (SlimeFight.ACH_DODGER | SlimeFight.ACH_SPLAT_PALS | SlimeFight.ACH_BOUNCER));

        f.timeLeft = .01f;
        f.update(.02f, L);
        check("the attraction ends on a FUN report", f.phase == SlimeFight.RESULT
                && f.feedback.contains("FUN") && f.steerPointer < 0 && f.flingPointer < 0);
    }

    private static void coreBridge(Layout L) {
        group("town main-game bridge");
        GameCore gate = new GameCore(new Mem(), 817L);
        check("a fresh title keeps town hidden", !LandPicker.townUnlocked(gate));
        gate.allLandsEnabled = true;
        check("developer land override does not reveal town", !LandPicker.townUnlocked(gate));
        gate.collected |= 1L << Collect.BOSS_FIRST;
        check("the first boss friend reveals town", LandPicker.townUnlocked(gate));
        gate.landChoice = LandPicker.TOWN;
        gate.landSuppressed |= 2;
        LandPicker.updateTravel(gate, 0f);
        check("reset hides town and clears a stale town selection",
                !LandPicker.townUnlocked(gate) && gate.landChoice == 0);

        Mem store = new Mem();
        GameCore c = new GameCore(store, 818L);
        c.collected |= 1L << Collect.BOSS_FIRST;
        c.collectionCounts[Collect.BOSS_FIRST] = 1;
        check("town unlock follows the first boss friend", LandPicker.townUnlocked(c));
        int bossCount = c.collectionCounts[Collect.BOSS_FIRST];
        c.openTown();
        check("main game opens town and persists the visit", c.townOpen && store.townSaves > 0
                && store.townState.length() > 0);
        check("town reads collection ownership without changing it", c.town.owns(Collect.BOSS_FIRST)
                && c.collectionCounts[Collect.BOSS_FIRST] == bossCount);
        c.town.dialogue = Town.TALK_NONE;
        float friendX = TownScreen.screenPathX(c.town, .2f, L);
        float friendY = TownScreen.landmarkGround(.2f, L) - L.w * .07f;
        TownScreen.down(c, L, friendX, friendY);
        check("town points of interest accept taps", c.town.targetPoi == Town.FRIEND);
        c.town.path = .66f;
        TownScreen.down(c, L, TownScreen.screenPathX(c.town,.66f,L),
                TownScreen.landmarkGround(.66f,L)-L.w*.07f);
        check("attraction taps follow the camera", c.town.targetPoi == Town.SLIDE_POI);
        c.town.targetPoi = Town.NONE; c.town.targetPath = -1f;
        float sliderY = StarScreen.sliderY(L);
        TownScreen.down(c, L, StarScreen.sliderRight(L), sliderY);
        check("town control bar steers at the tapped speed", c.town.dragging && c.town.steer > .9f);
        TownScreen.up(c, L, StarScreen.sliderRight(L), sliderY);
        check("releasing the town control bar stops steering", !c.town.dragging && c.town.steer == 0f);
        c.requestCloseTown();
        check("exit first holds town for the meadow wash",c.townOpen && c.townClosing);
        c.update(GameCore.TOWN_FADE,L);
        check("exit wash returns to the town card",!c.townOpen && c.townReturnFade>0f);
        c.closeTown();
        check("closing town returns to its land card", !c.townOpen
                && c.landChoice == LandPicker.TOWN);

        c.landChoice = 0;
        c.startGame();
        long run = c.townRunId;
        c.score = 2700;
        c.toTitle();
        int paid = c.townRunTickets, tickets = c.town.tickets;
        c.toTitle();
        check("a main-game run credits town once", run > 0 && paid > 0
                && c.town.tickets == tickets && c.townRunId == 0);

        GameCore restored = new GameCore(store, 819L);
        check("a fresh core reloads town progress", restored.town.tickets == tickets
                && restored.town.lastClaimedRun == c.town.lastClaimedRun);

        GameCore townRng = new GameCore(new Mem(), 4488L);
        GameCore plainRng = new GameCore(new Mem(), 4488L);
        townRng.collected |= 1L << Collect.BOSS_FIRST;
        townRng.collectionCounts[Collect.BOSS_FIRST] = 1;
        townRng.openTown();
        townRng.town.dialogue = Town.TALK_NONE;
        townRng.town.steer(1f);
        for (int i = 0; i < 90; i++) townRng.town.update(DT, L);
        townRng.closeTown();
        check("visiting town does not consume main-game RNG",
                townRng.rnd.nextLong() == plainRng.rnd.nextLong());

        Mem flaky = new Mem();
        flaky.townSaveSucceeds = false;
        GameCore retry = new GameCore(flaky, 820L);
        retry.collected |= 1L << Collect.BOSS_FIRST;
        retry.collectionCounts[Collect.BOSS_FIRST] = 1;
        retry.openTown();
        check("a failed town save stays dirty", retry.town.dirty
                && flaky.townSaveAttempts == 1 && flaky.townState.length() == 0);
        flaky.townSaveSucceeds = true;
        retry.update(1.1f, L);
        check("the main game retries a failed town save", !retry.town.dirty
                && flaky.townSaveAttempts >= 2 && flaky.townState.length() > 0);
    }
}
