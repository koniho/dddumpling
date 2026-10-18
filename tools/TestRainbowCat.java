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
        catalog();enemies(L);
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
        group("Rainbow Land enemy mix and trails");
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
        GameCore c=TestFastEnemies.scene(L,26,151);
        GameCore.Enemy e=add(c,L,new int[]{0,1,4},L.h*.4f);e.enterT=1;
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);
        c.rnd.setSeed(151);RainbowLand.enemyTrail(p,c,e,L,255);
        int[] first=p.resolve();boolean visible=false;for(int pixel:first)visible|=(pixel&0xFFFFFF)!=0;
        check("ordinary word has rainbow trail",visible);
        check("trail rendering preserves gameplay RNG",c.rnd.nextLong()==new java.util.Random(151).nextLong());
        e.destroyed=true;e.clearX=c.enemyCentreX(e);e.clearY=e.y;e.clearClock=c.clock;
        p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);RainbowLand.enemyTrail(p,c,e,L,255);
        check("trail clear starts from existing rainbow shape",java.util.Arrays.equals(first,p.resolve()));
        e.destroyT=GameCore.DESTROY_TIME*.4f;c.clock+=e.destroyT;
        p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);RainbowLand.enemyTrail(p,c,e,L,255);
        int[] dispersed=p.resolve();
        check("clear spreads trail beyond original width",litWidth(dispersed,(int)L.w)>litWidth(first,(int)L.w));
        check("clear fades rainbow brightness",brightest(dispersed)<brightest(first));
        e.destroyT=GameCore.DESTROY_TIME;
        p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);RainbowLand.enemyTrail(p,c,e,L,255);
        check("trail fully gone before enemy removed",brightest(p.resolve())==0);
        e.destroyed=false;
        p=new RasterPainter((int)L.w,(int)L.h,1);p.clear(0);c.stage=21;RainbowLand.enemyTrail(p,c,e,L,255);
        boolean empty=true;for(int pixel:p.resolve())empty&=(pixel&0xFFFFFF)==0;
        check("other lands keep their existing trails",empty);
    }

    private static int litWidth(int[] pixels,int width) {
        int left=width,right=-1;
        for(int i=0;i<pixels.length;i++)if((pixels[i]&0xFFFFFF)!=0){left=Math.min(left,i%width);right=Math.max(right,i%width);}
        return Math.max(0,right-left+1);
    }
    private static int brightest(int[] pixels) {
        int value=0;for(int pixel:pixels)value=Math.max(value,((pixel>>16)&255)+((pixel>>8)&255)+(pixel&255));return value;
    }

}
