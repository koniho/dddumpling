package com.dddumpling.game;

final class TestRainbowCat extends Check {
    static GameCore fight(Layout L) {
        GameCore c=new GameCore(TestTimeAttack.store(),151,true);c.startGame();
        c.stage=30;c.stageBanner=c.stageGap=0;c.landBlend=1;c.landFrom=Lands.RAINBOW;c.enemies.clear();c.power=null;c.modeLeft=0;
        c.boss.begin(Boss.RAINBOW_CAT,30,c.rnd,true);c.boss.intro=0;c.boss.update(0,L,c.rnd);
        return c;
    }
    static void capture(GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;a.release();
        // Keep the overlapping ball out of this catch; its input priority is tested separately.
        a.ballX=a.left;a.ballY=a.bottom;
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.cageX,a.cageY);
    }
    static void all(Layout L) {
        catalog();enemies(L);clearParticles(L);titleRainbow(L);background(L);guidance(L);
        group("Rainbow Cat touch ownership, cage and yarn");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        check("Rainbow Land follows swamp with stable cave slot",Lands.firstStage(Lands.RAINBOW)==26 && Lands.forStage(25)==Lands.SWAMP && Lands.forStage(26)==Lands.RAINBOW && !Lands.playable(Cave.LAND));
        check("cat at stage thirty, gator stays twenty-five",Boss.kindFor(25)==Boss.ALLIGATOR && Boss.kindFor(30)==Boss.RAINBOW_CAT && Boss.kindFor(35)<0);
        check("gator friend unlocks Rainbow Land",Lands.unlockBoss(Lands.RAINBOW)==Boss.ALLIGATOR);
        int old=a.pointer;a.touch(c,L,0,3,L.w*.5f,L.deckTop+1);
        check("deck remains available",a.pointer==old);
        a.touch(c,L,0,1,a.ballX,a.ballY);
        float bx=a.ballX;
        a.touch(c,L,2,2,bx+100,a.ballY);
        check("another pointer cannot steal yarn",a.held==RainbowCat.YARN && a.ballX==bx);
        a.touch(c,L,3,1,bx,a.ballY);
        check("cancel releases yarn",a.pointer==-1 && a.held==0);
        a.ballX=a.x;a.ballY=a.y;a.touch(c,L,0,4,a.x,a.y);
        check("overlap consistently chooses yarn",a.held==RainbowCat.YARN);a.release();
        a.ballX=a.left;a.ballY=a.bottom;
        a.touch(c,L,0,1,a.x-a.r*2,a.y);a.touch(c,L,2,1,a.x+a.r*2,a.y);
        check("fast swept touch catches cat",a.held==RainbowCat.CAT);
        a.release();capture(c,L);
        check("cage latch owns prompt",a.phase==RainbowCat.CAGED && a.at==0 && a.count==3 && a.pointer==-1 && c.boss.open());
        float hp=c.boss.hp,deadline=a.timer;
        int wrong=(a.prompt[0]+1)%Glyph.COUNT;
        check("wrong key neither resets prompt nor damages cat",c.boss.press(wrong,c.rnd,L)==Boss.REBUFF && a.at==0 && a.timer==deadline && c.boss.hp==hp);
        a.ballY=L.dangerY+10;
        check("yarn cannot damage during cage",a.update(c.boss,.1f,L,c.rnd)==0);
        while(a.phase==RainbowCat.CAGED)c.boss.press(a.prompt[a.at],c.rnd,L);
        check("completed capture damages one third",c.boss.hp==hp-1 && a.captures==1 && a.count==0 && a.safe>0);
        capture(c,L);a.timer=.01f;a.update(c.boss,.02f,L,c.rnd);
        check("timeout returns to chase without life or boss damage",a.phase==RainbowCat.RECOVER && a.count==0 && c.boss.hp==hp-1);
        a.phase=RainbowCat.CHASE;a.ballY=L.dangerY;a.safe=0;
        int hits=a.update(c.boss,.01f,L,c.rnd)+a.update(c.boss,.01f,L,c.rnd);
        check("yarn contact only costs one heart",hits==1 && a.safe>0);
        a.phase=RainbowCat.WINDUP;a.timer=.01f;a.x=a.ballX;a.y=a.ballY-a.r*1.8f;
        a.update(c.boss,.02f,L,c.rnd);
        check("paw physically bats yarn downward",a.ballVY>0 && a.phase==RainbowCat.RECOVER);
        capture(c,L);a.touch(c,L,0,7,a.x,a.y);Pause.release(c);
        check("pause clears pointer",a.pointer==-1 && a.held==0);
        c.boss.leave();check("leaving clears encounter",!a.placed && a.pointer==-1 && a.count==0);
        group("Rainbow reward save migration");
        long oldOwned=(1L<<60)|1L;
        long oldSave=Collect.PREVIOUS_TAG|oldOwned;
        check("61-entry tag does not become cat ownership",Collect.decode(oldSave)==oldOwned && Collect.currentSave(oldSave));
        long now=oldOwned|(1L<<61);
        check("new cat survives tagged save",Collect.decode(Collect.encode(now))==now && Collect.currentSave(Collect.encode(now)));
        check("old ducks retain bit identities",Collect.decode(Collect.OLD_TAG|(1L<<59))==(1L<<59));
        group("Rainbow capture reachability");
        for(float fps:new float[]{30,60,120}) {
            c=fight(L);
            for(int frame=0;frame<fps*90 && !c.boss.beaten;frame++) {
                bossPlay(c,L);c.boss.update(1/fps,L,c.rnd);
            }
            check("three captures beat cat at "+fps,c.boss.beaten && c.boss.cat.captures==3);
        }
        group("bounded Rainbow Cat one-finger play");
        for(float fps:new float[]{30,60,120}) {
            c=fight(L);Bot bot=new Bot(4,.30f,.08f,false,151);
            for(int frame=0;frame<fps*75 && !c.boss.beaten && c.state==GameCore.PLAY;frame++) {
                bot.step(c,L,1/fps);c.update(1/fps,L);
            }
            check("bounded one-finger cat fight completes at "+fps,c.boss.beaten && c.lives>0);
        }
        group("five-boss Time Attack archive migration");
        TimeAttack saved=new TimeAttack();
        StringBuilder oldFive=new StringBuilder("5");
        for(int p=0;p<3;p++)for(int i=0;i<6;i++)oldFive.append(';').append(i==5?12345:0);
        for(int p=0;p<3;p++)for(int i=0;i<6;i++) {
            TimeAttackHistory h=new TimeAttackHistory();if(i==5)h.legacy(12345,5);
            oldFive.append('|').append(h.encode());
        }
        for(int p=0;p<3;p++)oldFive.append("|0:0");
        saved.load(oldFive.toString());
        check("old all-boss score archived, cat and six-boss scores empty",saved.fiveBest[0]==12345 && saved.best[0][Boss.RAINBOW_CAT]==0 && saved.best[0][TimeAttack.ALL]==0);
        TimeAttack restored=new TimeAttack();restored.load(saved.encode());
        check("five-boss archive survives re-save",restored.fiveBest[0]==12345 && !restored.fiveHistory[0].runs.isEmpty());
        String[] fields=oldFive.toString().split("\\|",-1);
        StringBuilder sized=new StringBuilder(fields[0].replaceFirst("5;","7;5;3;"));
        for(int i=1;i<=18;i++)sized.append('|').append(fields[i]);
        for(int p=0;p<3;p++)sized.append("|4,").append(p).append(",0:0");
        restored.load(sized.toString());
        check("counted schema automatically archives a shorter boss roster",restored.fiveBest[0]==12345 && restored.best[0][TimeAttack.ALL]==0);
        restored.clearRecords();
        check("reset clears every archived roster",restored.fiveBest[0]==0 && restored.fiveHistory[0].runs.isEmpty());
    }
    private static void catalog() {
        group("world catalog identities and storage boundaries");
        check("collection IDs fit tagged long; widen storage before adding beyond 63",Collect.COUNT<=63);
        check("land IDs fit saved visit/suppression bytes",Lands.COUNT<=8);
        check("land presentation covers catalog",Lands.BG.length==Lands.COUNT && Lands.TINT.length==Lands.COUNT);
        java.util.HashSet<Integer> lands=new java.util.HashSet<>(),friends=new java.util.HashSet<>(),endings=new java.util.HashSet<>(),sounds=new java.util.HashSet<>();
        for(int b=0;b<Boss.COUNT;b++) {
            WorldCatalog.BossEntry entry=WorldCatalog.BOSSES[b];
            check("unique playable chapter and reward "+b,lands.add(entry.land) && friends.add(entry.friend)
                    && entry.land>=0 && entry.land<Lands.COUNT && entry.friend>=0 && entry.friend<Collect.COUNT);
            check("boss chapter round trip "+b,Boss.kindFor(WorldCatalog.bossStage(b))==b
                    && Lands.forStage(WorldCatalog.bossStage(b))==entry.land && Collect.bossKind(entry.friend)==b);
            check("boss death ID stable and unique "+b,endings.add(entry.ending) && HighScores.BLURBS[entry.ending]!=null);
            check("boss voice mapped without fixed-effect collisions "+b,sounds.add(Sfx.bossTauntId(b))
                    && Sfx.bossTauntId(b)<Sfx.COUNT && Sfx.bossTauntId(b)!=Sfx.BOLT_DEATH);
        }
        for(WorldCatalog.Land land:WorldCatalog.LANDS)check("land death ID unique "+land.ending,endings.add(land.ending));
        for(int key=0;key<Glyph.COUNT;key++)check("key voice identity "+key,sounds.add(Sfx.squishId(key)) && Sfx.squishId(key)<Sfx.COUNT && Sfx.squishId(key)!=Sfx.DRIP);
    }
    private static void enemies(Layout L) {
        group("Rainbow Land enemy mix");
        for(int mode:new int[]{-1,Power.FLURRY,Power.NINJA,Power.TEAM}) {
            GameCore c=TestFastEnemies.scene(L,26,151);c.pairSpawnSlots=0;c.spawnedThisStage=1;
            c.mode=mode;c.modeLeft=mode<0?0:100;
            int rush=0,dive=0;
            c.rnd.setSeed(151);
            for(int i=0;i<1000;i++) {
                c.enemies.clear();GameCore.Enemy e=add(c,L,new int[]{0,1,4},L.h*.4f);e.phase=6.283f*(i+.5f)/1000;
                if(mode<0){FastEnemies.nextDelay(c,L);if(c.fastEnemyNext)rush++;}
                else {FastEnemies.assign(c,e);if(e.fastSingle)rush++;}
                SwampEnemies.assign(c,e);if(e.swampKind==SwampEnemies.DIVE)dive++;
            }
            check("increased rush chance "+mode,rush==(mode<0?920:390));
            check("increased dive chance "+mode,dive==360);
        }
    }

    private static void clearParticles(Layout L) {
        group("Rainbow clear colors preserve particle behavior");
        GameCore rainbow=TestFastEnemies.scene(L,26,151),ordinary=TestFastEnemies.scene(L,21,151);
        rainbow.particles.clear();ordinary.particles.clear();rainbow.rnd.setSeed(151);ordinary.rnd.setSeed(151);
        int original=Glyph.COLOR[0];
        rainbow.survival.wordBurst(rainbow,100,200,20,12,original);
        ordinary.survival.wordBurst(ordinary,100,200,20,12,original);
        check("rainbow clears preserve particle count",rainbow.particles.size()==ordinary.particles.size());
        boolean motion=true,plain=true,colors=true;java.util.HashSet<Integer> palette=new java.util.HashSet<>();
        for(int i=0;i<rainbow.particles.size();i++) {
            GameCore.Particle a=rainbow.particles.get(i),b=ordinary.particles.get(i);
            motion&=a.x==b.x && a.y==b.y && a.vx==b.vx && a.vy==b.vy && a.max==b.max && a.size==b.size;
            plain&=b.color==original;palette.add(a.color);
            boolean found=false;for(int color:RainbowLand.CLEAR_COLORS)found|=a.color==color;colors&=found;
        }
        check("rainbow clears use every vibrant band",colors && palette.size()==RainbowLand.CLEAR_COLORS.length);
        check("other lands preserve original particle colors",plain);
        check("recoloring preserves motion and gameplay randomness",motion && rainbow.rnd.nextLong()==ordinary.rnd.nextLong());
        rainbow.particles.clear();rainbow.survival.active=true;
        rainbow.survival.wordBurst(rainbow,100,200,20,12,original);
        boolean survival=true;for(GameCore.Particle p:rainbow.particles)survival&=p.color==original;
        check("Survival retains its own clear palette",survival);
    }

    private static void guidance(Layout L) {
        group("Rainbow Cat companion guidance");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        check("skipped tutorials still offer cat help",c.onboarding.offersBossHelp(c));
        check("chase explains catching",a.companionHint().equals("CATCH THE CAT!"));
        a.ballVY=10;check("incoming yarn takes priority",a.companionHint().equals("DRAG YARN AWAY!"));
        a.held=RainbowCat.YARN;check("held yarn explains safe direction",a.companionHint().equals("MOVE THE YARN UP!"));
        a.held=RainbowCat.CAT;check("held cat explains destination",a.companionHint().equals("DRAG CAT TO CAGE!"));
        a.held=RainbowCat.NONE;a.ballVY=0;a.phase=RainbowCat.RECOVER;
        check("resting cat advertises catch opportunity",a.companionHint().equals("CATCH HIM NOW!"));
        a.phase=RainbowCat.CAGED;a.pointer=7;
        check("capture explains releasing gesture",a.companionHint().equals("LIFT, THEN TAP!"));
        a.release();check("cage explains keys",a.companionHint().equals("MATCH THE KEYS!"));
        a.phase=RainbowCat.CHASE;
        TestOnboarding.touch(c,L,0,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        TestOnboarding.touch(c,L,1,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        check("help opens yarn lesson despite skipped tutorials",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_YARN);
        float time=c.time,escape=a.timer;
        c.update(.2f,L);
        check("help pauses live fight",c.time==time && a.timer==escape);
        TestOnboarding.acknowledge(c,L);
        check("help advances to catching",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_CATCH);
        TestOnboarding.acknowledge(c,L);
        check("help advances to cage keys",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_CAGE);
        TestOnboarding.acknowledge(c,L);
        check("help resumes without resetting tutorial preferences",!c.onboarding.briefing && (c.onboarding.saved&Onboarding.SKIPPED)!=0);
    }

    private static int[] backdropPixels(GameCore c,Layout L) {
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);Lands.backdrop(p,c,L);return p.resolve();
    }
    private static boolean painted(int[] pixels) {
        for(int pixel:pixels)if((pixel&0xFFFFFF)!=0)return true;return false;
    }
    private static void background(Layout L) {
        group("Rainbow bands behind clouds");
        GameCore c=TestFastEnemies.scene(L,26,151);c.landFrom=Lands.SWAMP;c.landBlend=1;c.skyClock=0;
        c.rnd.setSeed(151);int[] first=backdropPixels(c,L);
        check("rainbow backdrop is already visible at time zero",painted(first));
        check("rainbow backdrop does not consume gameplay randomness",c.rnd.nextLong()==new java.util.Random(151).nextLong());
        check("rainbow backdrop is deterministic",java.util.Arrays.equals(first,backdropPixels(c,L)));
        c.skyClock=8;check("bands scroll with the scenery clock",!java.util.Arrays.equals(first,backdropPixels(c,L)));
        c.landBlend=0;check("incoming bands respect land transition",!painted(backdropPixels(c,L)));
        c.landBlend=1;c.stage=21;check("other lands keep their backdrops",!painted(backdropPixels(c,L)));
        c.stage=26;c.survival.active=true;check("Survival retains its own background",!painted(backdropPixels(c,L)));
        c.survival.active=false;c.state=GameCore.TITLE;check("title keeps its existing background",!painted(backdropPixels(c,L)));
        c.state=GameCore.PLAY;c.skyGlow=0;
        final boolean[] seen={false,false,true,true};
        Painter recorder=new OpacityPainter(new RasterPainter((int)L.w,(int)L.h,1),1) {
            public void fillPoly(float[] points,int color) {
                for(int band:RainbowLand.CLEAR_COLORS)if((color&0xFFFFFF)==(band&0xFFFFFF)) {
                    seen[0]=true;seen[2]&=!seen[1];seen[3]&=(color>>>24)>0 && (color>>>24)<128;
                }
                for(int layer=0;layer<GameCore.CLOUD_LAYERS;layer++)if((color&0xFFFFFF)==(Lands.cloudTint(c,layer)&0xFFFFFF))seen[1]=true;
                super.fillPoly(points,color);
            }
        };
        Renderer.draw(recorder,c,L);
        check("every cloud layer is drawn after the rainbow bands",seen[0] && seen[1] && seen[2]);
        check("vibrant bands remain translucent",seen[3]);
    }

    private static void titleRainbow(Layout L) {
        group("Rainbow title ground and headroom");
        GameCore c=new GameCore(TestTimeAttack.store(),151,true);
        c.collected=Collect.MASK;c.landSuppressed=0;c.landChoice=Lands.RAINBOW;
        c.landDiscovery=-1;c.landSeen=LandPicker.stateMask();
        int w=(int)L.w,h=(int)L.h;
        RasterPainter p=new RasterPainter(w,h,1);p.clear(0);c.modes.draw(p,c,L);
        int crestY=(int)(ModeSelector.y(L)-L.unit*2.9f);
        check("mode viewport preserves the rainbow crest above old clip",(p.resolve()[crestY*w+w/2]&0xFFFFFF)!=0);
        p=new RasterPainter(w,h,1);p.clear(0);
        float x=L.w*.5f,y=ModeSelector.y(L),s=L.unit,span=Math.min(L.w*.36f,Draw.type(s*1.74f)*3.22f);
        RainbowLand.title(p,x,y,s,span,1,0,1,true);
        int[] pixels=p.resolve();int ground=(int)Math.ceil(y+s*.30f);boolean below=false;
        for(int row=ground+1;row<h;row++)for(int column=0;column<w;column++)below|=(pixels[row*w+column]&0xFFFFFF)!=0;
        check("rainbow stops at a flat ground plane",!below);
        int left=-1,right=-1,row=ground-2;
        for(int column=0;column<w;column++)if((pixels[row*w+column]&0xFFFFFF)!=0){if(left<0)left=column;right=column;}
        check("both rainbow feet reach the ground beyond the text",left>=0 && left<x-span*.90f && right>x+span*.90f);
    }
}
