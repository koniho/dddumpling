package com.dddumpling.game;

import java.io.File;

/**
 * Drives {@link GameCore} headlessly to interesting states and renders each one to a PNG
 * through {@link RasterPainter}. This is how the game gets looked at during development
 * without a build/install cycle: same Layout, same Renderer, same state machine as the APK.
 */
final class Preview {

    private static void caveCollectFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(wanted("110")) {
            RasterPainter p=new RasterPainter(w,h,ss);p.clear(Renderer.BG);
            float cell=w/5f,top=h*.12f,gap=h*.21f,r=Math.min(cell*.36f,gap*.29f);
            p.text("CAVE FRIENDS",w*.5f,h*.055f,w*.035f,Renderer.INK,Painter.CENTER,true);
            for(int row=0;row<4;row++)for(int col=0;col<5;col++) {
                int i=Collect.MOLE_FIRST+(row%2)*5+col;
                float x=cell*(col+.5f),y=top+gap*row+r;
                Trinket.draw(p,i,x,y,r,1.3f,row<2,1f);
                p.text(row<2?Collect.NAME[i]:"???",x,y+r*1.6f,w*.014f,Renderer.INK,Painter.CENTER,true);
            }
            Png.write(new File(dir,"110-cave-families.png"),p.resolve(),w,h);
        }
        GameCore c=new GameCore(new Mem(),615L);c.collected=Collect.MASK;c.openCase();c.caseFade=1;
        c.caseTo(Collect.MOLE_FIRST);shot(dir,"110-case-moles",c,L,w,h,ss);
        c.caseTo(Collect.SNAKE_FIRST);shot(dir,"110-case-snakes",c,L,w,h,ss);
    }

    private static void miningFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        GameCore c=TestCaveMining.game(L,new Check.Mem());CaveMining m=c.mining;
        shot(dir,"109-mine-start",c,L,w,h,ss);
        m.ready=0;m.update(c,1.3f);shot(dir,"109-mine-cheering",c,L,w,h,ss);
        c.tapBonus(m.sequence[0]);m.update(c,.08f);
        shot(dir,"109-mine-sequence",c,L,w,h,ss);
        m.update(c,.22f);shot(dir,"109-mine-prompt-rock-flight",c,L,w,h,ss);
        c.tapBonus(m.sequence[1]);m.update(c,.15f);
        shot(dir,"109-mine-falling-rocks",c,L,w,h,ss);
        m.update(c,.10f);shot(dir,"109-mine-walk-forward",c,L,w,h,ss);
        TestCaveMining.fill(c);for(int i=0;i<m.loads;i++)m.falling[i]=0;
        shot(dir,"109-mine-full-cart",c,L,w,h,ss);
        m.launch(c,1);m.update(c,.30f);
        shot(dir,"109-mine-helpers",c,L,w,h,ss);
        m.update(c,.60f);shot(dir,"109-mine-cart-rush",c,L,w,h,ss);
        m.update(c,CaveMining.PUSH_TIME);
        shot(dir,"109-mine-three-keys",c,L,w,h,ss);
        TestCaveMining.fill(c);m.launch(c,-1);m.update(c,CaveMining.PUSH_TIME);
        m.left=CaveMining.TIME*.18f;
        for(int pos=0;pos<4;pos++){m.pos=pos;shot(dir,"109-mine-four-prompt-"+pos,c,L,w,h,ss);}
        shot(dir,"109-mine-four-keys-dim",c,L,w,h,ss);
        m.update(c,CaveMining.TIME);
        shot(dir,"109-mine-timeout",c,L,w,h,ss);
        m.carts=4;m.begin(c);m.ready=0;TestCaveMining.fill(c);m.launch(c,1);
        c.update(CaveMining.PUSH_TIME,L);
        shot(dir,"109-mine-reward",c,L,w,h,ss);
        c.update(CaveMining.REPORT_TIME+.01f,L);
        shot(dir,"109-mine-parade",c,L,w,h,ss);
    }

    private static void cartFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        GameCore c=TestCaveCart.game(L,new Check.Mem());CaveCart m=c.cart;
        shot(dir,"111-cart-ready",c,L,w,h,ss);
        m.ready=0;m.progress=3;m.segment=.5f;m.lean=-.8f;m.turn=-.8f;m.scene.clock=2;
        shot(dir,"111-cart-left",c,L,w,h,ss);
        int gentle=0,sharp=0;
        for(int i=1;i<CaveCart.TRACK;i++){
            if(Math.abs(CaveCart.bend(i))<Math.abs(CaveCart.bend(gentle)))gentle=i;
            if(Math.abs(CaveCart.bend(i))>Math.abs(CaveCart.bend(sharp)))sharp=i;
        }
        m.progress=gentle;m.segment=CaveCart.SEGMENT*.7f;m.turn=CaveCart.curve(gentle+.7f);m.lean=m.turn;
        shot(dir,"111-cart-gentle-bank",c,L,w,h,ss);
        m.progress=sharp;m.turn=CaveCart.curve(sharp+.7f);m.lean=m.turn;
        shot(dir,"111-cart-sharp-bank",c,L,w,h,ss);
        m.progress=5;m.lean=.8f;m.turn=.8f;m.scene.clock=3;
        shot(dir,"111-cart-right",c,L,w,h,ss);
        m.scene.clock=1.35f;shot(dir,"111-cart-foreground-arches",c,L,w,h,ss);
        m.balance=.85f;m.lean=-.5f;m.scene.rumble=.6f;
        m.danger=.25f;shot(dir,"111-cart-danger",c,L,w,h,ss);
        m.danger=1.05f;shot(dir,"111-cart-red-countdown",c,L,w,h,ss);
        m.balance=-.85f;shot(dir,"111-cart-balance-right",c,L,w,h,ss);
        m.balance=.85f;
        m.phase=CaveCart.REPORT;m.spilled=true;m.scene.spillAge=.32f;
        shot(dir,"111-cart-spill",c,L,w,h,ss);
        m.progress=14;m.begin(c);shot(dir,"111-cart-resume",c,L,w,h,ss);
        m.progress=CaveCart.TRACK;m.begin(c);c.update(.01f,L);
        shot(dir,"111-cart-reward",c,L,w,h,ss);
    }

    private static void caveFrames(File dir, Layout L, int w, int h, int ss) throws Exception {
        GameCore c=new GameCore(new Mem(),921L);c.caveChoice=0;c.startGame();c.jumpToStage(21,L);
        c.landBlend=1f;c.update(.6f,L);
        shot(dir,"106-cave-stage-introduction",c,L,w,h,ss);
        c.stageBanner=0f;Cave v=c.cave;
        shot(dir,"106-cave-entrance",c,L,w,h,ss);
        v.z=v.cameraZ=1.2f;v.aim=v.route.heading(v.z);v.focus=.6f;
        shot(dir,"106-cave-irregular-walls",c,L,w,h,ss);v.focus=0;

        v.z=v.cameraZ=2f;v.phase=Cave.FORK;v.timer=.10f;v.fork=0;
        shot(dir,"106-cave-first-fork",c,L,w,h,ss);
        v.timer=.42f;shot(dir,"106-cave-fork-countdown",c,L,w,h,ss);
        v.choose(c,-1);v.z=v.cameraZ=2.6f;v.aim=v.route.heading(v.z);
        shot(dir,"106-cave-chosen-path",c,L,w,h,ss);
        v.routes[1]=-1;v.routes[2]=1;
        for(int bend=0;bend<3;bend++) {
            v.z=v.cameraZ=3.2f+bend*2.5f;v.aim=v.route.heading(v.z);
            shot(dir,"106-cave-winding-"+bend,c,L,w,h,ss);
        }
        v.z=v.cameraZ=2.75f;v.nextEvent=2;v.encounter(c,Cave.SHADOW);v.update(c,.06f,L);
        shot(dir,"106-cave-side-ambush",c,L,w,h,ss);
        v.update(c,.36f,L);
        shot(dir,"106-cave-enemy-stomp",c,L,w,h,ss);
        v.update(c,.35f,L);shot(dir,"106-cave-enemy-rush",c,L,w,h,ss);
        v.press(c,v.wanted(),L);v.update(c,.05f,L);
        shot(dir,"106-cave-enemy-bolt",c,L,w,h,ss);
        v.update(c,.06f,L);shot(dir,"106-cave-enemy-tummy-hit",c,L,w,h,ss);
        while(v.phase==Cave.FIGHT||v.phase==Cave.SHADOW)v.press(c,v.wanted(),L);
        v.update(c,.3f,L);shot(dir,"106-cave-enemy-retreat",c,L,w,h,ss);
        v.update(c,.4f,L);shot(dir,"106-cave-enemy-hidden",c,L,w,h,ss);
        v.phase=Cave.WALK;v.focus=0;v.cameraZ=v.z-.13f;v.encounter(c,Cave.ROCKS);
        shot(dir,"106-cave-rock-immediate",c,L,w,h,ss);
        v.update(c,.15f,L);shot(dir,"106-cave-rock-zoom-mid",c,L,w,h,ss);
        v.update(c,.15f,L);shot(dir,"106-cave-rock-closeup",c,L,w,h,ss);
        v.traps.x=v.traps.targetX=.2f;v.update(c,CaveTraps.FALL-.3f+.02f,L);v.effects.update(.12f);
        shot(dir,"106-cave-rock-breakup",c,L,w,h,ss);
        v.phase=Cave.WALK;v.effects.update(.22f);
        shot(dir,"106-cave-rock-dust-after",c,L,w,h,ss);
        v.phase=Cave.ROCKS;v.traps.age=1.12f/Cave.PACE;v.traps.landed[0]=true;v.traps.x=.65f;
        shot(dir,"106-cave-falling-rocks",c,L,w,h,ss);
        v.phase=Cave.WALK;v.z=v.cameraZ=5.75f;v.routes[1]=-1;v.focus=0;v.cameraZ=v.z-.13f;v.encounter(c,Cave.SAND);
        shot(dir,"106-cave-quicksand-zoom-start",c,L,w,h,ss);
        v.update(c,.15f,L);shot(dir,"106-cave-quicksand-zoom-mid",c,L,w,h,ss);
        v.update(c,.15f,L);shot(dir,"106-cave-quicksand-start",c,L,w,h,ss);
        v.traps.age=1.4f/Cave.PACE;v.traps.hits=0;
        shot(dir,"106-cave-quicksand-panic",c,L,w,h,ss);
        v.traps.hits=6;shot(dir,"106-cave-quicksand-escape",c,L,w,h,ss);
        for(int variation=0;variation<3;variation++) {
            v.route.make(new java.util.Random(100+variation));v.phase=Cave.WALK;v.focus=0;v.z=v.cameraZ=3.7f;
            v.aim=v.route.heading(v.z);v.effects.reset();
            shot(dir,"106-cave-generated-"+variation,c,L,w,h,ss);
        }
        v.z=v.cameraZ=Cave.LENGTH;v.phase=Cave.EXIT;v.focus=0;
        shot(dir,"106-cave-exit",c,L,w,h,ss);
        for(int finish=0;finish<CaveDumpling.COUNT;finish++) {
            GameCore pick=new GameCore(new Mem(),973L);pick.startGame();pick.jumpToStage(21,L);
            step(pick,L,.7f);
            if(finish==0)shot(dir,"107-cave-choose-explorer",pick,L,w,h,ss);
            pick.cave.selection.pick(pick,finish);step(pick,L,.4f);
            if(finish==1)shot(dir,"107-cave-selected-departure",pick,L,w,h,ss);
            step(pick,L,.5f);step(pick,L,.25f);
            shot(dir,"107-cave-walker-"+finish,pick,L,w,h,ss);
            if(finish==2)for(int pose=0;pose<3;pose++) {
                step(pick,L,.10f);shot(dir,"107-cave-bounce-"+pose,pick,L,w,h,ss);
            }
        }
    }

    private static final float DT = 1f / 60f;

    /** Lines that ran off the screen across every frame rendered, for the summary at the end. */
    private static int unfitFrames;

    private static final class Mem implements GameCore.Store {
        boolean pushLessonSeen = true; // Ordinary simulations model a player past onboarding.
        public boolean loadPushLessonSeen() { return pushLessonSeen; }
        public void savePushLessonSeen(boolean value) { pushLessonSeen = value; }
        byte[] progress;
        public byte[] loadProgress() { return progress == null ? null : progress.clone(); }
        public void saveProgress(byte[] data) { progress = data.clone(); }
        public String progressReplica() { return "test"; }
        int best;
        long collected;
        int collectTotal;
        int[] collectionCounts = new int[Collect.COUNT];
        int steamerOpens;
        int starWins, starWinSaves;
        int rosterState = 1;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; }
        public long loadCollected() { return collected; }
        public void saveCollected(long v) { collected = v; }
        public int[] loadCollectionCounts() { return collectionCounts.clone(); }
        public void saveCollectionCounts(int[] v) { collectionCounts = v.clone(); }
        public int loadCollectTotal() { return collectTotal; }
        public void saveCollectTotal(int v) { collectTotal = v; }
        public int loadStarWins() { return starWins; }
        public void saveStarWins(int v) { starWins = v; starWinSaves++; }
        public int loadSteamerOpens() { return steamerOpens; }
        public void saveSteamerOpens(int v) { steamerOpens = v; }
        public int loadRosterState() { return rosterState; }
        public void saveRosterState(int v) { rosterState = v; }
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

        if(wanted("130")) {
            GameCore scores=new GameCore(new Mem(),101L);
            shot(dir,"130-high-scores-title",scores,L,w,h,ss);
            scores.highScoreScreen.show(scores);
            shot(dir,"130-high-scores-empty",scores,L,w,h,ss);
            for(int i=0;i<10;i++) {
                scores.startGame();scores.score=(10-i)*1357;scores.stage=new int[]{5,10,15,20,1,6,11,16,21,25}[i];
                scores.hits=123;scores.misses=7;scores.squishes=56;scores.maxCombo=48;
                scores.highScores.stages=scores.stage-1;scores.highScores.bosses=(1<<Math.min(Boss.COUNT,(scores.stage-1)/Boss.EVERY))-1;
                scores.highScores.dumplings=14-i;scores.highScores.powers=7;scores.highScores.swipes=3;
                scores.lives=0;scores.highScores.finish(scores);
                if(i==0) {
                    scores.toTitle();scores.returnFade=0;scores.highScoreScreen.show(scores);
                    shot(dir,"130-high-scores-partial",scores,L,w,h,ss);
                }
            }
            scores.toTitle();scores.returnFade=0;scores.highScoreScreen.show(scores);
            shot(dir,"130-high-scores-full",scores,L,w,h,ss);
            scores.highScoreScreen.scrollTo(scores,L,scores.highScoreScreen.maxScroll(scores,L));
            shot(dir,"130-high-scores-latest",scores,L,w,h,ss);
            scores.highScoreScreen.selected=0;
            shot(dir,"130-high-scores-summary",scores,L,w,h,ss);
        }

        GameCore cover = new GameCore(new Mem(),3001L);
        cover.startGame(); cover.jumpToStage(5,L);
        cover.boss.intro=0f; cover.stageGap=0f; cover.stageBanner=0f;
        cover.update(1f/60f,L);
        cover.boss.chainAt=2; cover.boss.slimePromptHits=2; cover.boss.slimeCoverLearned=true;
        float[] coverPhases={4.7f,4.80f,4.85f,4.9f,4.95f,0.08f,0.2f,0.34f,1f,1.03f,1.10f,1.20f,1.30f};
        for(int frame=0;frame<coverPhases.length;frame++) {
            cover.boss.phase=coverPhases[frame];
            shot(dir,"101-slime-cover-"+frame,cover,L,w,h,ss);
        }

        GameCore mystery = new GameCore(new Mem(),1101L);
        mystery.startGame(); mystery.jumpToStage(11,L);
        mystery.stageGap=0f; mystery.spawnTimer=0f; mystery.powerTimer=100f;
        step(mystery,L,3f); mystery.stageBanner=0f;
        for(int effect : new int[]{Power.INCOGNITO,Power.MONOCHROME}) {
            Power icon=new Power(); icon.effect=effect; icon.x=L.w*0.5f;
            icon.y=L.playTop+L.enemyR*5f; mystery.power=icon;
            shot(dir,"100-debuff-icon-"+effect,mystery,L,w,h,ss);
        }
        mystery.power.mystery=true; mystery.power.teamAvailable=true; mystery.power.effect=Power.MONOCHROME;
        mystery.power.hit=true;
        for(int frame=0;frame<5;frame++) {
            mystery.power.hitT=frame*0.19f;
            shot(dir,"100-mystery-roulette-"+frame,mystery,L,w,h,ss);
        }
        for(int effect : new int[]{Power.FLURRY,Power.INCOGNITO}) {
            mystery.power.effect=effect;
            for(int frame=0;frame<4;frame++) {
                mystery.power.hitT=Power.SELECT_TIME+frame*0.28f;
                shot(dir,"100-reveal-"+effect+"-"+frame,mystery,L,w,h,ss);
            }
        }
        mystery.power=null; mystery.startDebuff(Power.INCOGNITO);
        for(int frame=0;frame<4;frame++) {
            mystery.incognitoMorph=frame/3f;
            shot(dir,"100-incognito-morph-"+frame,mystery,L,w,h,ss);
        }
        shot(dir,"100-incognito",mystery,L,w,h,ss);
        mystery.incognitoMorph=0f;
        mystery.startDebuff(Power.MONOCHROME);
        for(int frame=0;frame<3;frame++) {
            mystery.monochromeFade=frame*0.5f;
            shot(dir,"100-monochrome-fade-"+frame,mystery,L,w,h,ss);
        }

        GameCore linked = new GameCore(new Mem(), 1601L);
        linked.startGame();
        linked.jumpToStage(16, L);
        shot(dir, "95-linked-intro", linked, L, w, h, ss);
        linked.stageGap = 0f;
        linked.spawnTimer = 0f;
        linked.powerTimer = 100f;
        step(linked, L, 2.2f);
        linked.stageBanner = 0f;
        shot(dir, "95-linked-hands", linked, L, w, h, ss);
        float pairY = linked.enemies.get(0).y;
        for (int frame = 0; frame < 3; frame++) {
            for (GameCore.Enemy e : linked.enemies) e.y = L.playTop+(frame-1)*L.enemyR*0.65f;
            shot(dir,"95-linked-header-"+frame,linked,L,w,h,ss);
        }
        for (GameCore.Enemy e : linked.enemies) e.y = pairY;
        int originalLeft = linked.enemies.get(0).word[0], originalRight = linked.enemies.get(1).word[0];
        for (int left = 0; left < 3; left++) {
            for (int right = 3; right < 6; right++) {
                linked.enemies.get(0).word[0] = left;
                linked.enemies.get(1).word[0] = right;
                shot(dir, "96-linked-material-" + left + "-" + right, linked, L, w, h, ss);
                for (int side = 0; side < 2; side++) {
                    GameCore.Enemy held = linked.enemies.get(side);
                    held.linkWaiting = true;
                    held.linkLeft = LinkedPairs.WINDOW * 0.6f;
                    shot(dir, "97-linked-reaching-" + left + "-" + right + "-" + side, linked, L, w, h, ss);
                    held.linkWaiting = false;
                    held.linkLeft = 0f;
                }

            }
        }
        linked.enemies.get(0).word[0] = originalLeft;
        linked.enemies.get(1).word[0] = originalRight;

        GameCore.Enemy friend = linked.enemies.get(0);
        linked.destroyWord(friend, linked.enemyCentreX(friend), friend.y, L);
        shot(dir, "95-linked-pointing", linked, L, w, h, ss);
        step(linked, L, 0.12f);
        shot(dir, "95-linked-countdown", linked, L, w, h, ss);
        step(linked, L, 0.10f);
        shot(dir, "95-linked-resist", linked, L, w, h, ss);
        for (int frame = 0; frame < 5; frame++) {
            for (GameCore.Enemy e : linked.enemies) e.linkStrain = 1f-frame*0.2f;
            shot(dir, "98-linked-flex-" + frame, linked, L, w, h, ss);
        }


        linked.destroyWord(friend,linked.enemyCentreX(friend),friend.y,L);
        GameCore.Enemy releasePartner = friend.link;
        linked.destroyWord(releasePartner,linked.enemyCentreX(releasePartner),releasePartner.y,L);
        for (int frame = 0; frame < 3; frame++) {
            step(linked,L,0.06f);
            shot(dir,"99-linked-release-"+frame,linked,L,w,h,ss);
        }

        GameCore spinning = new GameCore(new Mem(),1601L);
        spinning.startGame(); spinning.jumpToStage(16,L);
        spinning.stageGap = 0f; spinning.spawnTimer = 0f; spinning.powerTimer = 100f;
        step(spinning,L,2.2f);
        spinning.stageBanner = 0f; spinning.collected = Collect.MASK;
        spinning.startFrenzy(Power.TEAM,L);
        GameCore.Enemy spinLeft = spinning.enemies.get(0), spinRight = spinLeft.link;
        shot(dir,"99-power-bond-team",spinning,L,w,h,ss);
        spinning.buddy.x = spinning.enemyCentreX(spinLeft); spinning.buddy.y = spinLeft.y;
        spinning.buddySquish(spinLeft,L);
        spinning.flash = spinning.shake = 0f;
        for (int frame = 0; frame < 4; frame++) {
            spinLeft.destroyT = spinRight.destroyT = 0.06f+frame*0.12f;
            shot(dir,"99-power-bond-spin-"+frame,spinning,L,w,h,ss);
        }

        for (int land = 0; land < Lands.COUNT; land++) {
            GameCore themed = new GameCore(new Mem(), 810L);
            themed.startGame();
            if (land == Cave.LAND) { themed.caveChoice=0; themed.jumpToStage(land * Boss.EVERY + 1, L); }
            else themed.stage = land * Boss.EVERY + 1;
            step(themed, L, 5f);
            themed.stageBanner = 0f;
            shot(dir, "80-land-" + land + "-play", themed, L, w, h, ss);
            for (int variant = 0; variant < 3; variant++) {
                themed.stage = land * Boss.EVERY + variant + 1;
                themed.stageBanner = GameCore.BANNER_TIME * 0.55f;
                shot(dir, "81-land-" + land + "-skit-" + variant, themed, L, w, h, ss);
            }
        }

        caveFrames(dir,L,w,h,ss);
        cartFrames(dir,L,w,h,ss);
        miningFrames(dir,L,w,h,ss);
        caveCollectFrames(dir,L,w,h,ss);

        Check.Mem newsSave=new Check.Mem();newsSave.releaseSeen="";
        GameCore news=new GameCore(newsSave,7001L);news.releaseMascot.update(news,.1f);
        shot(dir,"103-release-steamer",news,L,w,h,ss);
        for(int frame=1;frame<=2;frame++) {
            news.clock=frame*.4f;
            shot(dir,"103-release-steamer-idle-"+frame,news,L,w,h,ss);
        }
        news.releaseMascot.read(news);
        shot(dir,"103-release-steamer-read",news,L,w,h,ss);
        shot(dir,"103-release-steamer-corner",news,L,w,h,ss);
        news.settingsOpen=true;
        shot(dir,"103-release-steamer-settings",news,L,w,h,ss);
        Check.Mem inviteSave=new Check.Mem();inviteSave.releaseSeen="";
        GameCore invite=new GameCore(inviteSave,7002L);invite.releaseMascot.update(invite,.1f);
        invite.releaseNotes.show(invite,L);
        shot(dir,"103-release-invite-enter-0",invite,L,w,h,ss);
        invite.update(.5f,L);
        shot(dir,"103-release-invite-enter-1",invite,L,w,h,ss);

        GameCore book=new GameCore(new Mem(),7001L);
        book.releaseNotes.show(book,L);
        for(int frame=0;frame<7;frame++) {
            shot(dir,"103-release-enter-"+frame,book,L,w,h,ss);
            book.update(.2f,L);
        }
        shot(dir,"103-release-book-list",book,L,w,h,ss);
        book.releaseNotes.listScroll=ReleaseNotes.maxScroll(L);
        shot(dir,"103-release-book-oldest",book,L,w,h,ss);
        book.releaseNotes.listScroll=0f;
        book.releaseNotes.select(0,0,L);book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
        shot(dir,"103-release-current-first",book,L,w,h,ss);
        book.releaseNotes.touch(book,L,L.w*.5f,(book.releaseNotes.demoTop(L)+book.releaseNotes.demoBottom(L))*.5f);
        book.releaseNotes.update(2.5f,L);
        shot(dir,"103-release-current-first-held",book,L,w,h,ss);
        if(ReleaseChange.ITEMS[0].length>1) {
            book.releaseNotes.select(0,1,L);book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
            shot(dir,"103-release-current-second",book,L,w,h,ss);
            book.releaseNotes.touch(book,L,L.w*.5f,(book.releaseNotes.demoTop(L)+book.releaseNotes.demoBottom(L))*.5f);
            book.releaseNotes.update(2.5f,L);
            shot(dir,"103-release-current-second-held",book,L,w,h,ss);
        }
        if(ReleaseChange.ITEMS[0].length>2) {
            book.releaseNotes.select(0,2,L);book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
            shot(dir,"103-release-current-third",book,L,w,h,ss);
            book.releaseNotes.touch(book,L,L.w*.5f,(book.releaseNotes.demoTop(L)+book.releaseNotes.demoBottom(L))*.5f);
            book.releaseNotes.update(2.5f,L);
            shot(dir,"103-release-current-third-held",book,L,w,h,ss);
        }
        book.releaseNotes.back();book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
        book.releaseNotes.listScroll=ReleaseNotes.maxScroll(L);
        shot(dir,"103-release-book-list-scrolled",book,L,w,h,ss);
        int[] releaseItems=ReleaseChange.ITEMS[0];
        try {
            ReleaseChange.ITEMS[0]=new int[ReleaseNotes.columns(L)*9+2];
            for(int i=0;i<ReleaseChange.ITEMS[0].length;i++) ReleaseChange.ITEMS[0][i]=releaseItems[i%releaseItems.length];
            book.releaseNotes.listScroll=0f;
            shot(dir,"103-release-book-wrapped",book,L,w,h,ss);
            book.releaseNotes.listScroll=ReleaseNotes.maxScroll(L);
            shot(dir,"103-release-book-wrapped-scrolled",book,L,w,h,ss);
        } finally { ReleaseChange.ITEMS[0]=releaseItems;book.releaseNotes.listScroll=0f; }
        try(Check.ReleaseExamples examples=new Check.ReleaseExamples()) {
        for(int page=0;page<3;page++) {
            book.releaseNotes.select(page,L);book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
            shot(dir,"103-release-book-"+page,book,L,w,h,ss);
            book.releaseNotes.touch(book,L,L.w*0.65f,(book.releaseNotes.demoTop(L)+book.releaseNotes.demoBottom(L))*.5f);
            book.releaseNotes.update(page==1 ? 0.8f : 0.3f,L);
            shot(dir,"103-release-book-active-"+page,book,L,w,h,ss);
            book.releaseNotes.update(ReleaseNotes.RESTART_DELAY,L);
            shot(dir,"103-release-book-restarted-"+page,book,L,w,h,ss);
        }

        book.releaseNotes.reset(L);
        GameCore.Enemy bookA=book.releaseNotes.demo.enemies.get(0),bookB=bookA.link;
        float bookY=book.releaseNotes.demoTop(L)+bookA.y;
        book.releaseNotes.touch(book,L,L.w*0.08f+book.releaseNotes.demo.enemyCentreX(bookA),bookY);
        book.releaseNotes.update(0.1f,L);
        book.releaseNotes.touch(book,L,L.w*0.08f+book.releaseNotes.demo.enemyCentreX(bookB),bookY);
        book.releaseNotes.update(0.15f,L);
        shot(dir,"103-release-book-pair-clear",book,L,w,h,ss);
        }

        for(int release=0;release<ReleaseNotes.VERSIONS.length;release++)
            for(int feature=0;feature<ReleaseChange.ITEMS[release].length;feature++) {
                book.releaseNotes.select(release,feature,L);book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
                shot(dir,"103-release-feature-"+release+"-"+feature,book,L,w,h,ss);
            }

        book.releaseNotes.back();book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
        book.releaseNotes.select(0,L);
        for(int frame=0;frame<5;frame++) {
            shot(dir,"103-release-page-enter-"+frame,book,L,w,h,ss);
            book.update(.1f,L);
        }
        book.releaseNotes.back();
        for(int frame=0;frame<5;frame++) {
            shot(dir,"103-release-page-back-"+frame,book,L,w,h,ss);
            book.update(.1f,L);
        }
        book.releaseNotes.close();
        for(int frame=0;frame<7;frame++) {
            shot(dir,"103-release-exit-"+frame,book,L,w,h,ss);
            book.update(.2f,L);
        }

        for(int selected:new int[]{0,1,Lands.COUNT-1}) {
            GameCore trail=new GameCore(new Mem(),839L);
            trail.collected=Collect.MASK;trail.landSeen=14;trail.landChoice=selected;
            shot(dir,"105-land-trail-"+selected,trail,L,w,h,ss);
        }

        GameCore defaultPicker = new GameCore(new Mem(), 839L);
        defaultPicker.collected = Collect.add(defaultPicker.collected, Collect.BOSS_FIRST);
        shot(dir, "84-land-picker-default", defaultPicker, L, w, h, ss);
        for (int unlocked = 1; unlocked <= 3; unlocked++) {
            GameCore picker = new GameCore(new Mem(), 840L);
            for (int boss = 0; boss < unlocked; boss++) picker.collected = Collect.add(picker.collected, Collect.BOSS_FIRST + boss);
            LandPicker.select(picker, unlocked);
            for (int frame = 0; frame < 180; frame++) picker.update(DT, L);
            shot(dir, "84-land-picker-" + unlocked, picker, L, w, h, ss);
        }

        for(int direction:new int[]{1,-1}) {
            for(int frame=0;frame<5;frame++) {
                GameCore trip=new GameCore(new Mem(),841L);
                trip.collected=Collect.MASK;trip.landSeen=14;
                trip.landChoice=direction>0 ? 1 : 2;
                LandPicker.step(trip,direction);
                LandPicker.updateTravel(trip,LandPicker.TRAVEL_TIME*(0.08f+frame*0.20f));
                shot(dir,"102-land-travel-"+(direction>0 ? "right" : "left")+"-"+frame,trip,L,w,h,ss);
            }
        }

        for(int state=0;state<5;state++) {
            GameCore explorer=new GameCore(new Mem(),843L);
            explorer.collected=Collect.MASK;explorer.landSeen=LandPicker.stateMask();
            if(state<2) LandPicker.updateTravel(explorer,state==0 ? 0f : 1.6f);
            else {
                LandPicker.select(explorer,3);
                LandPicker.updateTravel(explorer,LandPicker.TRAVEL_TIME*(state==2 ? 1f : state==3 ? 1.5f : 3f));
            }
            shot(dir,"102-land-explorer-"+state,explorer,L,w,h,ss);
        }

        for (int land = 0; land < Lands.COUNT; land++) {
            for (int frame = 0; frame < 2; frame++) {
                GameCore logo = new GameCore(new Mem(), 892L);
                logo.collected = Collect.MASK;
                logo.landSeen = 14;
                logo.landChoice = land;
                logo.clock = 0.6f + frame * 1.2f;
                shot(dir, "88-land-logo-" + land + "-" + frame, logo, L, w, h, ss);
            }
        }

        for (int frame = 0; frame < 3; frame++) {
            GameCore discovery = new GameCore(new Mem(), 891L);
            discovery.collected = Collect.add(discovery.collected, Collect.BOSS_FIRST);
            step(discovery, L, new float[]{0.12f, 0.48f, 1.02f}[frame]);
            shot(dir, "87-land-discovery-" + frame, discovery, L, w, h, ss);
        }

        for(int frame=0;frame<8;frame++) {
            GameCore tour=new GameCore(new Mem(),893L);
            for(int land=1;land<Lands.COUNT;land++) tour.collected=Collect.add(tour.collected,Collect.BOSS_FIRST+land-1);
            step(tour,L,new float[]{.12f,.46f,1.02f,1.39f,1.80f,2.48f,3.25f,3.95f}[frame]);
            shot(dir,"104-land-discovery-tour-"+frame,tour,L,w,h,ss);
        }

        for (int kind = 0; kind < Boss.COUNT; kind++) {
            GameCore bossLoss = toBoss(L, kind, 855L + kind, true);
            step(bossLoss, L, 0.25f);
            bossLoss.lives = 1;
            bossLoss.takeHit(L.w * 0.5f, L);
            step(bossLoss, L, 0.6f);
            shot(dir, "85-boss-loss-confetti-" + kind, bossLoss, L, w, h, ss);
            if (kind == Boss.MUSHROOM) {
                step(bossLoss, L, 0.6f);
                shot(dir, "85-mushroom-victory-storm", bossLoss, L, w, h, ss);
            }
            if (kind == Boss.OCTOPUS) {
                step(bossLoss, L, 0.45f);
                shot(dir, "85-octopulse-victory-wave", bossLoss, L, w, h, ss);
            }
        }

        for (int skit = 0; skit < 2; skit++) {
            GameCore sea = new GameCore(new Mem(), 870L);
            sea.startGame(); sea.stage = 12 + skit; sea.landBlend = 1f;
            float[] moments = {0.12f, 0.48f, 0.78f};
            for (int sample = 0; sample < moments.length; sample++) {
                sea.stageBanner = GameCore.BANNER_TIME * (1f - moments[sample]);
                shot(dir, "86-sea-skit-" + skit + "-" + sample, sea, L, w, h, ss);
            }
        }

        GameCore landFade = new GameCore(new Mem(), 823L);
        landFade.startGame();
        for (int sample = 0; sample < 3; sample++) {
            landFade.landBlend = sample * 0.5f;
            landFade.stageBanner = 0f;
            shot(dir, "83-intro-land-crossfade-" + sample, landFade, L, w, h, ss);
        }
        landFade.jumpToStage(6, L);
        for (int sample = 0; sample < 3; sample++) {
            landFade.landBlend = sample * 0.5f;
            landFade.stageBanner = 0f;
            shot(dir, "82-land-crossfade-" + sample, landFade, L, w, h, ss);
        }

        // Named, so a frame filter skips them along with everything else it did not ask for.
        if (wanted("chars")) characterSheet(dir, w, h, ss);
        if (wanted("skits")) skitSheet(dir, L, w, h, ss);
        if (wanted("collect")) collectSheet(dir, w, h, ss);
        if (wanted("sfx")) sounds(dir);

        Mem store = new Mem();
        store.best = 1840;
        store.rosterState = 0;
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

        GameCore paused = new GameCore(store, 94L);
        paused.startGame(); step(paused, L, 2f); Pause.open(paused);
        shot(dir, "96-pause", paused, L, w, h, ss);
        Pause.action(paused, 2);
        shot(dir, "97-confirm-end", paused, L, w, h, ss);

        // The title lesson identifying its second target and matching key before pressing it.
        GameCore c21 = new GameCore(store, 87L);
        step(c21, L, Demo.ACQUIRE[1] + 0.28f);
        System.out.printf("title demo: lit key=%d amount=%.2f%n", Demo.litKey(c21),
                Demo.litAmount(c21));
        shot(dir, "49-title-demo", c21, L, w, h, ss);
        c21.clock = Demo.POWER_START + Demo.POWER_APPROACH + 0.85f;
        shot(dir, "49b-title-touch-approach", c21, L, w, h, ss);
        c21.clock = Demo.POWER_START + Demo.POWER_TOUCH + 0.08f;
        shot(dir, "49c-title-touch-collect", c21, L, w, h, ss);

        // After the third press: the finishing bullet visibly travelling from its actual key.
        GameCore c22 = new GameCore(store, 87L);
        step(c22, L, Demo.fireAt(2) + Demo.SHOT * 0.48f);
        System.out.printf("title last shot: lit key=%d amount=%.2f%n", Demo.litKey(c22),
                Demo.litAmount(c22));
        shot(dir, "50-title-shot", c22, L, w, h, ss);

        // The demo word arriving. It fades and swells up on enterT, the same field a real word's
        // entrance rides, rather than appearing whole.
        GameCore c23 = new GameCore(store, 87L);
        step(c23, L, 0.48f);
        shot(dir, "51-title-arriving", c23, L, w, h, ss);

        // And coming apart once that bullet lands: the field's own fly-apart, outer tiles splitting
        // left and right off Renderer.enemy's destroy path.
        GameCore c24 = new GameCore(store, 87L);
        step(c24, L, Demo.impactAt(2) + GameCore.DESTROY_TIME * 0.55f);
        shot(dir, "52-title-destroyed", c24, L, w, h, ss);

        // Adaptive roster: the first-run deck, friends bouncing in, and their sad farewell.
        Mem joinStore = new Mem(); joinStore.rosterState = 0;
        GameCore cJoin = new GameCore(joinStore, 88L);
        cJoin.unlockRoster();
        step(cJoin, L, GameCore.ROSTER_SCENE_TIME * 0.52f);
        shot(dir, "58-roster-join", cJoin, L, w, h, ss);
        Mem leaveStore = new Mem(); leaveStore.rosterState = 8;
        GameCore cLeave = new GameCore(leaveStore, 89L);
        step(cLeave, L, GameCore.ROSTER_SCENE_TIME * 0.52f);
        shot(dir, "59-roster-leave", cLeave, L, w, h, ss);
        store.rosterState = 1;

        GameCore caseScore = new GameCore(store, 728L);
        for (int sample = 0; sample <= 4; sample++) {
            caseScore.caseOpen = true;
            caseScore.caseFade = sample * .25f;
            shot(dir, "110-case-score-fade-" + sample, caseScore, L, w, h, ss);
        }

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
        GameCore grid = new GameCore(store, 184L);
        grid.collected = Collect.MASK;
        grid.openCase();
        step(grid, L, 0.5f);
        grid.caseTo(3);
        shot(dir, "40b-case-grid", grid, L, w, h, ss);
        grid.beginCaseDrag(w / 2f, Showcase.focusCy(L));
        grid.caseDragTo(w / 2f, Showcase.focusCy(L) - Showcase.rowStep(L) * 0.35f, L);
        shot(dir, "40c-case-vertical-drag", grid, L, w, h, ss);
        grid.endCaseDrag();
        grid.caseTo(Collect.STAR_FIRST + 2);
        shot(dir, "40d-case-star-row", grid, L, w, h, ss);
        grid.caseTo(Showcase.entry(Showcase.FRUIT_ROW, 2));
        grid.beginCaseDrag(w / 2f, Showcase.focusCy(L));
        grid.caseDragTo(w / 2f + Showcase.step(L) * 0.35f, Showcase.focusCy(L), L);
        shot(dir, "40e-case-fruit-glass", grid, L, w, h, ss);
        grid.endCaseDrag();
        grid.caseTo(Showcase.entry(Showcase.CANDY_ROW, 2));
        shot(dir, "40f-case-candy-row", grid, L, w, h, ss);
        grid.beginCaseDrag(w / 2f, Showcase.focusCy(L));
        grid.caseDragTo(w / 2f - Showcase.step(L) * 0.3f,
                Showcase.focusCy(L) + Showcase.rowStep(L) * 0.4f, L);
        shot(dir, "40g-case-free-pan", grid, L, w, h, ss);
        grid.endCaseDrag();
        step(grid, L, 0.6f);
        shot(dir, "40h-case-animated", grid, L, w, h, ss);
        CaseUi.select(grid, Showcase.entry(Showcase.FRUIT_ROW, 3));
        step(grid, L, 0.08f);
        shot(dir, "40i-case-centering", grid, L, w, h, ss);
        step(grid, L, 0.6f);
        shot(dir, "40j-case-centered", grid, L, w, h, ss);
        grid.caseTo(Collect.BOSS_FIRST + 2);
        shot(dir, "40k-case-bosses", grid, L, w, h, ss);
        CaseUi.select(grid, Collect.BOSS_FIRST + Boss.OCTOPUS);
        step(grid, L, 0.26f);
        grid.collectionCounts[grid.caseIndex] = 12;
        shot(dir, "40l-case-highlight-tap", grid, L, w, h, ss);
        grid.beginCaseDrag(w / 2f, Showcase.focusCy(L));
        grid.caseDragTo(w / 2f - Showcase.step(L) * 0.7f, Showcase.focusCy(L), L);
        step(grid, L, 0.22f);
        shot(dir, "40m-case-highlight-pan", grid, L, w, h, ss);
        grid.endCaseDrag();
        for (int boss = 0; boss < Boss.COUNT; boss++) {
            GameCore welcome = new GameCore(store, 199L + boss);
            welcome.stage = Boss.EVERY;
            Interlude.awardBossPrize(welcome, boss);
            Interlude.enterBonus(welcome, L);
            welcome.bonusTimer = BossCollect.REVEAL_TIME - 2.2f;
            shot(dir, "95-boss-friend-" + boss, welcome, L, w, h, ss);
        }
        c.endCaseDrag();
        step(c, L, 0.5f);

        // The badge out at one end of its arc, where the box is turned hardest.
        GameCore c20 = new GameCore(store, 83L);
        step(c20, L, Showcase.ARC_TIME * 0.25f);
        System.out.printf("badge drift: x=%.0f of %.0f%n",
                Showcase.iconCx(L, c20.clock), (float) w);
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

        float lessonY = near.y;
        near.y = PushLesson.triggerY(L);
        c4.pushLesson.seen = false;
        c4.update(DT, L);
        c4.pushLesson.clock = .2f;
        shot(dir, "199-push-lesson", c4, L, w, h, ss);
        c4.pushLesson.clock = .6f;
        shot(dir, "199-push-lesson-swipe", c4, L, w, h, ss);
        c4.pushLesson.clock = 1.2f;
        shot(dir, "199-push-lesson-lift", c4, L, w, h, ss);
        near.y = lessonY;
        GameCore reset = new GameCore(store, 18L);
        reset.settingsOpen = true; reset.settingsPage = 1; reset.settingsTab = SettingsUi.PROGRESS;
        shot(dir, "200-swipe-reset", reset, L, w, h, ss);
        c4.pushLesson.reset();
        c4.pushLesson.seen = true;

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

        c3.hits = c3.misses = 0;
        shot(dir, "8b-gameover-zero-hits", c3, L, w, h, ss);

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
        step(c3, L, GameCore.RETURN_FADE * 0.25f);
        shot(dir, "89-return-summary-fade", c3, L, w, h, ss);
        step(c3, L, GameCore.RETURN_FADE * 0.25f);
        shot(dir, "89-return-midpoint", c3, L, w, h, ss);
        step(c3, L, GameCore.RETURN_FADE * 0.25f);
        shot(dir, "89-return-title-fade", c3, L, w, h, ss);
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

        // A run's title selection survives awards moving the case to a different character.
        GameCore selected = new GameCore(new Mem(), 173L);
        selected.collected = Collect.add(selected.collected, 7);
        selected.caseIndex = 7;
        selected.startGame();
        Interlude.awardBossPrize(selected, 0);
        selected.bossPrizePending = false;
        selected.starNext = true;
        Interlude.enterBonus(selected, L);
        step(selected, L, 0.45f);
        System.out.printf("selected pilot: who=%d case=%d prize=%d%n", selected.stars.who,
                selected.caseIndex, selected.prize);
        shot(dir, "53c-stars-selected-after-reward", selected, L, w, h, ss);

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
        cs.stars.wins = StarPath.MAX_DIFFICULTY;
        cs.stars.make(new java.util.Random(83L));
        cs.stars.collected = 0b111;
        cs.stars.vx = 0f;
        cs.stars.x = cs.stars.starX(6, L);
        shot(dir, "54d-stars-max-bends", cs, L, w, h, ss);
        for(int frame=0;frame<3;frame++) {
            cs.stars.timer=StarPath.REPORT+(frame==0 ? 0.02f : frame==1 ? -0.01f : -0.8f);
            cs.bonusTimer=cs.stars.timer;
            shot(dir,"54e-stars-incomplete-exit-"+frame,cs,L,w,h,ss);
        }


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
        for (int i = 0; i < c13.steamer.goal() * 2 + 4; i++) {
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
        c9.tapPower(c9.power.x, c9.power.y, L);
        step(c9, L, 2.5f);
        System.out.printf("frenzy: mode=%s left=%.1f enemies=%d%n",
                Power.NAMES[c9.mode], c9.modeLeft, c9.enemies.size());
        shot(dir, "17-frenzy", c9, L, w, h, ss);

        for(int frame=0;frame<4;frame++) {
            GameCore rainbow=new GameCore(store,431L);rainbow.startGame();
            Power pickup=new Power();pickup.effect=Power.FLURRY;pickup.x=L.w*0.4f;
            pickup.y=L.playTop+(L.dangerY-L.playTop)*0.35f;rainbow.power=pickup;
            rainbow.tapPower(pickup.x,pickup.y,L);
            rainbow.modeLeft=Power.DURATION-Power.FLURRY_BURST*(0.07f+frame*0.22f);
            rainbow.shake=rainbow.flash=0f;
            shot(dir,"18a-flurry-rainbow-"+frame,rainbow,L,w,h,ss);
        }

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
        c10.tapPower(c10.power.x, c10.power.y, L);
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
        c14.tapPower(c14.power.x, c14.power.y, L);
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

        // Real frenzy spawns at entry, the vertical join, and the settled descent.
        GameCore entry = new GameCore(store, 274L);
        entry.startGame();
        entry.playtestMode(Power.FLURRY, L);
        GameCore.Enemy leftEntry = null, rightEntry = null;
        for (int k = 0; k < 80 && (leftEntry == null || rightEntry == null); k++) {
            entry.enemies.clear();
            entry.spawnTimer = 0f;
            entry.update(DT, L);
            if (entry.enemies.isEmpty()) continue;
            GameCore.Enemy sideWord = entry.enemies.get(0);
            if (!sideWord.sideEntry) continue;
            if (sideWord.pathStartX < L.playLeft) leftEntry = sideWord; else rightEntry = sideWord;
        }
        entry.enemies.clear();
        if (leftEntry != null) entry.enemies.add(leftEntry);
        if (rightEntry != null) entry.enemies.add(rightEntry);
        entry.stageBanner = 0f;
        for (int frame = 0; frame < 3; frame++) {
            for (GameCore.Enemy sideWord : entry.enemies) {
                sideWord.y = sideWord.pathStartY + (L.dangerY - L.enemyR - sideWord.pathStartY)
                        * (frame == 0 ? 0.04f : frame == 1 ? 0.15f : 0.5f);
                entry.updateSidePath(sideWord, L);
            }
            shot(dir, frame == 0 ? "33b-side-entering"
                    : frame == 1 ? "33c-side-visible" : "33d-side-vertical",
                    entry, L, w, h, ss);
        }

        GameCore traffic = new GameCore(store, 171L);
        traffic.startGame();
        traffic.playtestMode(Power.FLURRY, L);
        step(traffic, L, 3.5f);
        shot(dir, "33e-side-spacing", traffic, L, w, h, ss);

        // A mass clear during FLING, followed by its short replacement burst.
        GameCore refill = new GameCore(store, 711L);
        refill.startGame();
        refill.stage = 13;
        refill.startFrenzy(Power.FLING, L);
        step(refill, L, 2f);
        for (GameCore.Enemy word : refill.enemies) {
            if (word.typeable()) refill.destroyWord(word, refill.enemyCentreX(word), word.y, L);
        }
        step(refill, L, 0.7f);
        shot(dir, "33f-frenzy-refill", refill, L, w, h, ss);

        GameCore kids = new GameCore(store, 582L);
        kids.preferences.kids=true;kids.startGame();kids.stage=19;
        step(kids,L,8f);
        for(int i=0;i<kids.enemies.size();i++) {
            GameCore.Enemy word=kids.enemies.get(i);
            word.y=L.playTop+(L.dangerY-L.playTop)*(word.link!=null?.2f:.65f);
        }
        shot(dir,"33g-kids-late-words",kids,L,w,h,ss);
        kids.enemies.clear();kids.shots.clear();
        kids.starNext=false;kids.earnedMash=GameCore.MASH_PANIC;Interlude.enterBonus(kids,L);
        kids.bonusTimer=kids.bonusRollEnd;kids.time=1f;
        shot(dir,"33h-kids-steamer-five",kids,L,w,h,ss);
        kids.stars.wins=StarPath.MAX_DIFFICULTY;kids.starNext=true;Interlude.enterBonus(kids,L);
        step(kids,L,StarPath.READY+.3f);
        shot(dir,"33i-kids-star-cap",kids,L,w,h,ss);

        // Settings panel, opened mid-game.
        GameCore c6 = new GameCore(store, 29L);
        c6.startGame();
        c6.score = 1420;
        c6.stage = 3;
        step(c6, L, 6f);
        c6.collected = 0b0000_0100_1000_0011_0010_0110_1101L;
        c6.openSettings();
        step(c6, L, 0.3f);
        shot(dir, "12-settings", c6, L, w, h, ss);
        c6.settingsTab = SettingsUi.MINIGAMES;
        c6.setStarDifficulty(0);
        shot(dir, "12a-settings-minigames", c6, L, w, h, ss);
        c6.setStarDifficulty(StarPath.MAX_DIFFICULTY);
        shot(dir, "12b-settings-minigames-max", c6, L, w, h, ss);
        c6.settingsTab = SettingsUi.GENERAL;

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
     * Every boss gets one because every boss draws something different — the whole point of six of
     * them — and a mechanic nobody has looked at is a mechanic nobody has checked.
     */
    private static void bossFrames(File dir, Layout L, int w, int h, int ss) throws Exception {
        // The arrival card, part way in.
        GameCore ci = toBoss(L, Boss.SLIME, 501L, false);
        step(ci, L, Boss.INTRO * 0.45f);
        System.out.printf("boss intro: %s, %.2f through%n", ci.boss.name(),
                ci.boss.introProgress());
        shot(dir, "59-boss-intro", ci, L, w, h, ss);

        // Each of the six, mid-fight, with its window open so the ornament is showing the thing it
        // is asking for.
        String[] tag = {"60-boss-slime", "70-boss-dark-divide", "76-boss-octopulse",
                "77-boss-fly-agaric"};
        for (int k = 0; k < Boss.COUNT; k++) {
            GameCore c = toBoss(L, k, 510L + k, true);
            // Land a couple of hits so the health bar is part-spent and the body is dented, and so
            // the bosses that shed things have shed them.
            for (int n = 0; n < 3; n++) {
                for (int i = 0; i < 60 * 8 && !c.boss.open(); i++) c.update(DT, L);

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
                    float guideLife = c.boss.elife[glob];
                    for (int demo = 0; demo < 3; demo++) {
                        c.boss.elife[glob] = Boss.GLOB_TIME - 1.8f * (0.15f + demo * 0.325f);
                        shot(dir, "65d-slime-side-guide-" + demo, c, L, w, h, ss);
                    }
                    c.boss.elife[glob] = guideLife;


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

        GameCore defeatedOcto = toBoss(L, Boss.OCTOPUS, 538L, true);
        defeatedOcto.boss.hp = 1f;
        defeatedOcto.boss.octoVulnerableArm = 3; defeatedOcto.boss.held = -3;
        defeatedOcto.boss.octoDragStarted = defeatedOcto.boss.octoDragCanDamage = true;
        defeatedOcto.boss.dragTo(L.playLeft - L.keyR, L.playTop, L);
        for (int i=0;i<24;i++) defeatedOcto.boss.update(DT,L,defeatedOcto.rnd);
        shot(dir,"76g-octopulse-defeated-shrug",defeatedOcto,L,w,h,ss);
        for (int i=0;i<48;i++) defeatedOcto.boss.update(DT,L,defeatedOcto.rnd);
        shot(dir,"76h-octopulse-defeated-droop",defeatedOcto,L,w,h,ss);
        GameCore wilt = toBoss(L, Boss.MUSHROOM, 539L, true);
        wilt.stageBanner = wilt.rosterSceneT = 0f;
        wilt.boss.hp = 1f;
        for (int i = 0; i < 1200 && !wilt.boss.beaten; i++) {
            Check.bossPlay(wilt, L);
            wilt.update(DT, L);
        }
        float[] wiltTimes = {0f, .85f, 1.55f, 2.2f, 2.9f, 3.25f, 3.5f};
        String[] wiltNames = {"last-shake", "brown-shriveled", "flattening", "flat", "spread", "melting", "faded"};
        for (int phase = 0; phase < wiltTimes.length; phase++) {
            while (wilt.boss.leaveProgress() * Boss.LEAVE + DT * .5f < wiltTimes[phase])
                wilt.update(DT, L);
            shot(dir, "77i-agaric-death-" + phase + "-" + wiltNames[phase], wilt, L, w, h, ss);
        }
        GameCore agaric = toBoss(L, Boss.MUSHROOM, 537L, true);
        agaric.stageBanner = agaric.rosterSceneT = 0f;
        agaric.boss.mushroomCharge = Boss.MUSHROOM_CHARGE_TIME * 0.5f;
        shot(dir, "77b-agaric-charged", agaric, L, w, h, ss);
        agaric.boss.mushroomCharge = 0f;
        agaric.boss.mushroomCapDX = Boss.bodyR(L) * 0.55f;
        agaric.boss.mushroomCapDY = -Boss.bodyR(L) * 0.15f;
        shot(dir, "77c-agaric-bending", agaric, L, w, h, ss);
        agaric.boss.mushroomCapDX = Boss.bodyR(L) * 2.2f;
        shot(dir, "77d-agaric-hard-right", agaric, L, w, h, ss);
        agaric.boss.mushroomCapDX = -Boss.bodyR(L) * 2.2f;
        shot(dir, "77e-agaric-hard-left", agaric, L, w, h, ss);
        agaric.boss.mushroomCapDX = Boss.bodyR(L) * 0.75f;
        agaric.boss.held = -2;
        agaric.boss.hp = agaric.boss.hpMax * 0.25f;
        agaric.boss.shedMushroomDust(L.w * 0.3f, L);
        for (int i = 0; i < 18; i++) agaric.update(DT, L);
        agaric.boss.shedMushroomDust(L.w * 0.3f, L);
        shot(dir, "77f-agaric-shaken-dust", agaric, L, w, h, ss);
        agaric.boss.held = -1;
        agaric.boss.mushroomAngry = Boss.MUSHROOM_ANGER_TIME;
        agaric.boss.mushroomReaction = true;
        for (int i=0;i<12;i++) agaric.update(DT,L);
        shot(dir,"77g-agaric-damage-pulse",agaric,L,w,h,ss);
        for (int i=0;i<54;i++) agaric.update(DT,L);
        shot(dir,"77h-agaric-reactive-spores",agaric,L,w,h,ss);


        GameCore pulseDemo = toBoss(L, Boss.OCTOPUS, 538L, true);
        pulseDemo.stageBanner = pulseDemo.rosterSceneT = 0f;
        for (int i = 0; i < 360 && (pulseDemo.boss.octoTarget < 0 || pulseDemo.boss.octoSweep < 0.25f); i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76m-octopulse-early-recoil", pulseDemo, L, w, h, ss);
        for (int i = 0; i < 360 && pulseDemo.boss.octoSweep < 1f; i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76b-octopulse-settled", pulseDemo, L, w, h, ss);
        for (int i = 0; i < 60 && pulseDemo.boss.octoCharge < 0.5f; i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76c-octopulse-charge", pulseDemo, L, w, h, ss);
        for (int i = 0; i < 60 && pulseDemo.boss.octoCharge < 1f; i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76d-octopulse-strike", pulseDemo, L, w, h, ss);
        for (int i = 0; i < 60 && pulseDemo.boss.octoReach < 0.75f; i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76i-octopulse-full-body-reach", pulseDemo, L, w, h, ss);
        for (int i = 0; i < 60 && pulseDemo.boss.octoCaptured < 0; i++)
            pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        for (int i = 0; i < 8; i++) pulseDemo.boss.update(DT, L, pulseDemo.rnd);
        shot(dir, "76j-octopulse-too-slow-taunt", pulseDemo, L, w, h, ss);

        GameCore pulsePain = toBoss(L, Boss.OCTOPUS, 538L, true);
        pulsePain.stageBanner = pulsePain.rosterSceneT = 0f;
        for (int i = 0; i < 360 && pulsePain.boss.octoCharge < 1f; i++)
            pulsePain.boss.update(DT, L, pulsePain.rnd);
        pulsePain.boss.press(pulsePain.boss.octoTarget, pulsePain.rnd, L);
        for (int frame = 0; frame < 3; frame++) {
            for (int i = 0; i < 24; i++) pulsePain.update(DT, L);
            shot(dir, "76n-octopulse-pain-wave-" + frame, pulsePain, L, w, h, ss);
        }

        GameCore throwing = TestOctoThrow.tear(L, 7);
        throwing.stageBanner = throwing.rosterSceneT = 0f;
        String[] throwTags = {"76o-octopulse-throw-windup", "76p-octopulse-throw-release",
                "76q-octopulse-throw-volley"};
        int[] throwFrames = {35, 42, 100};
        int thrownFrame = 0;
        for (int pose = 0; pose < throwTags.length; pose++) {
            while (thrownFrame < throwFrames[pose]) {
                throwing.boss.update(DT, L, throwing.rnd);
                thrownFrame++;
            }
            shot(dir, throwTags[pose], throwing, L, w, h, ss);
        }

        GameCore pairThrow = TestOctoThrow.tear(L, 6);
        pairThrow.stageBanner = pairThrow.rosterSceneT = 0f;
        for (int i = 0; i < 48; i++) pairThrow.boss.update(DT, L, pairThrow.rnd);
        shot(dir, "76r-octopulse-pair-windup", pairThrow, L, w, h, ss);
        for (int i = 0; i < 15; i++) pairThrow.boss.update(DT, L, pairThrow.rnd);
        shot(dir, "76s-octopulse-pair-release", pairThrow, L, w, h, ss);

        GameCore slimeRest = toBoss(L, Boss.SLIME, 539L, true);
        slimeRest.stageBanner = slimeRest.rosterSceneT = 0f;
        slimeRest.boss.body.reset(L.w * 0.5f, Boss.restY(L), Boss.bodyR(L), 2f);
        slimeRest.boss.hurt = 0f;
        shot(dir, "60b-slime-reference-rest", slimeRest, L, w, h, ss);

        // Dark Divide gets a short visual sequence of its own: impact, charged gesture, split,
        // then two independent bodies with one hurt and its neglected twin about to fire.
        GameCore ddHit = toBoss(L, Boss.SPLITTER, 540L, true);
        ddHit.boss.pieceHits[0] = 3;
        ddHit.boss.hurt = 0.92f;
        shot(dir, "71-divide-damage", ddHit, L, w, h, ss);

        GameCore ddReady = toBoss(L, Boss.SPLITTER, 541L, true);
        ddReady.stageBanner = ddReady.rosterSceneT = 0f;
        ddReady.boss.pieceHits[0] = Boss.DIVIDE_HITS;
        ddReady.boss.hurt = 0f;
        shot(dir, "72-divide-vulnerable", ddReady, L, w, h, ss);
        float readyX = ddReady.boss.pieceX(0, L), readyY = ddReady.boss.pieceY(0, L);
        float spread = ddReady.boss.pieceBody(0).spanY() * 1.1f;
        ddReady.boss.beginPinch(spread, readyX, readyY - spread * 0.5f, readyX, readyY + spread * 0.5f);
        ddReady.boss.pinch(spread * 1.15f, readyX, readyY - spread * 0.575f,
                readyX, readyY + spread * 0.575f, ddReady.rnd);
        shot(dir, "72b-divide-finger-morph", ddReady, L, w, h, ss);

        GameCore ddSplit = toBoss(L, Boss.SPLITTER, 542L, true);
        ddSplit.boss.pieceHits[0] = Boss.DIVIDE_HITS;
        float ddx = ddSplit.boss.pieceX(0, L), ddy = ddSplit.boss.pieceY(0, L);
        ddSplit.boss.beginPinch(100f, ddx - 50f, ddy, ddx + 50f, ddy);
        ddSplit.boss.pinch(100f * Boss.DIVIDE_SCALE, ddx - 85f, ddy, ddx + 85f, ddy, ddSplit.rnd);
        step(ddSplit, L, 0.08f);
        shot(dir, "73-divide-split", ddSplit, L, w, h, ss);

        GameCore ddVolley = toBoss(L, Boss.SPLITTER, 544L, true);
        ddVolley.boss.halfIdle[0] = ddVolley.boss.divideBoltInterval() - DT * .5f;
        step(ddVolley, L, DT);
        step(ddVolley, L, .08f);
        shot(dir, "75a-divide-unsplit-volley", ddVolley, L, w, h, ss);
        step(ddVolley, L, .9f);
        shot(dir, "75c-divide-three-projectiles", ddVolley, L, w, h, ss);

        GameCore ddDanger = toBoss(L, Boss.SPLITTER, 543L, true);
        ddDanger.boss.pieceHits[0] = Boss.DIVIDE_HITS;
        float ddx2 = ddDanger.boss.pieceX(0, L), ddy2 = ddDanger.boss.pieceY(0, L);
        ddDanger.boss.beginPinch(100f, ddx2 - 50f, ddy2, ddx2 + 50f, ddy2);
        ddDanger.boss.pinch(100f * Boss.DIVIDE_SCALE, ddx2 - 85f, ddy2, ddx2 + 85f, ddy2, ddDanger.rnd);
        ddDanger.boss.divideBurst = 0f;
        ddDanger.boss.halfHurt[1] = 0.90f;
        ddDanger.boss.halfIdle[1] = 0.10f;
        ddDanger.boss.halfIdle[2] = ddDanger.boss.divideBoltInterval() * 0.91f;
        shot(dir, "74-divide-half-danger", ddDanger, L, w, h, ss);
        ddDanger.boss.halfIdle[2] = ddDanger.boss.divideBoltInterval() - DT * 0.5f;
        step(ddDanger, L, DT);
        shot(dir, "75-divide-bolt", ddDanger, L, w, h, ss);
        step(ddDanger, L, 0.08f);
        shot(dir, "75b-divide-firing-recoil", ddDanger, L, w, h, ss);

        GameCore ddDeath = toBoss(L, Boss.SPLITTER, 545L, true);
        while (!ddDeath.boss.beaten) {
            int node = ddDeath.boss.pieceNodeIndex(0);
            ddDeath.boss.pieceHits[node] = Boss.DIVIDE_HITS;
            ddDeath.boss.beginPinch(100f);
            ddDeath.boss.pinch(100f * (Boss.DIVIDE_SCALE + .01f));
        }
        float[] deathTimes = {.4f, .95f, 1.4f, 1.85f, 2.03f, 2.35f, 2.85f};
        for (int i = 0; i < deathTimes.length; i++) {
            while (DivideDeath.elapsed(ddDeath.boss) < deathTimes[i]) step(ddDeath, L, DT);
            shot(dir, "75d-divide-supernova-" + i, ddDeath, L, w, h, ss);
        }

        // The settings panel's stage jump, parked on a boss stage so the row names the boss it is
        // sitting on — which is the state the control exists for.
        GameCore cj = toBoss(L, Boss.SLIME, 520L, true);
        cj.openSettings();
        step(cj, L, 0.3f);
        System.out.printf("stage jump: on stage %d, boss %s%n", cj.stage,
                Boss.NAMES[cj.boss.kind]);
        shot(dir, "67-settings-stage-jump", cj, L, w, h, ss);
        GameCore settings=new GameCore(new Mem(),819L);
        PlayerSettings.open(settings);
        for(int v=0;v<4;v++) {
            settings.preferences.music=settings.preferences.effects=new float[]{.1f,.5f,1f,.5f}[v];
            settings.preferences.musicMuted=settings.preferences.effectsMuted=v==3;
            shot(dir,"120-settings-audio-"+v,settings,L,w,h,ss);
        }
        settings.preferences.musicMuted=settings.preferences.effectsMuted=false;
        settings.preferences.music=settings.preferences.effects=1f;
        for(int beat=0;beat<3;beat++) {
            settings.clock=beat*.19f;
            shot(dir,"120b-settings-loud-motion-"+beat,settings,L,w,h,ss);
        }
        settings.settingsPage=1;
        for(int tab=0;tab<4;tab++) {
            settings.settingsTab=tab;
            shot(dir,"121-settings-disabled-"+tab,settings,L,w,h,ss);
        }
        settings.startGame();settings.openSettings();
        for(int tab=0;tab<4;tab++) {
            settings.settingsTab=tab;
            shot(dir,"122-settings-active-"+tab,settings,L,w,h,ss);
        }

        for (int kind = 0; kind < Boss.COUNT; kind++) {
            if (kind == Boss.SPLITTER) continue; // Its seven supernova frames include the impact.
            GameCore death = toBoss(L, kind, 590L + kind, true);
            death.boss.beaten = true; death.boss.hp = 0f; death.boss.leaveT = Boss.LEAVE;
            step(death, L, death.boss.deathImpactTime() + .04f);
            System.out.printf("boss death %s: shake %.2f%n", death.boss.name(), death.shake);
            shot(dir, "66b-boss-death-impact-" + kind, death, L, w, h, ss);
        }

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
        c.stage = Boss.EVERY;
        c.enemies.clear();
        c.boss.begin(kind, c.stage, c.rnd);
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
                "collect", "star", "course-start", "tally", "parade-join", "game-over", "boss-laugh", "boss-damage", "boss-split", "bolt-pop", "divide-damage", "divide-split",
                "divide-boing-heavy", "divide-boing-medium", "divide-boing-light", "roster-join", "divide-deactivate", "shield-bounce", "slime-damage", "octo-cue", "octo-lock",
                "taunt-slime", "taunt-divide", "taunt-octopus", "taunt-mushroom", "bolt-death",
                "mushroom-shake", "mushroom-spore", "linked-thud", "shuffle-blip", "debuff-down", "slime-cover", "slime-release", "land-shuffle", "ui-bloop", "blast-off", "divide-supernova", "octo-wave", "cave-rumble", "cave-crash", "cave-ambush", "cave-sink", "mining-cheer", "cart-roll", "cart-squeal", "cart-tumble", "octo-damage"};
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
        for(int song=0;song<CaveSong.COUNT;song++) {
            Wav.write(new File(sfxDir,"cave-band-"+song+".wav"),CaveSong.performance(song),Sfx.RATE);
        }
        short[] fren = Music.loop(Music.SWING_STYLE, true);
        short[] boss = Music.bossLoop(Music.SWING_STYLE);
        Wav.write(new File(sfxDir, "bgm-frenzy.wav"), fren, Sfx.RATE);
        Wav.write(new File(sfxDir, "bgm-boss-from-" + Music.BOSS_SOURCE + ".wav"), boss, Sfx.RATE);
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
        grid(dir, w, h, ss, "0-collect", "THE "+Collect.COUNT+" COLLECTIBLES",
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
