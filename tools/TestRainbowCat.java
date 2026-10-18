package com.dddumpling.game;

final class TestRainbowCat extends Check {
    static GameCore fight(Layout L) {
        GameCore c=new GameCore(TestTimeAttack.store(),151,true);c.startGame();
        c.stage=30;c.stageBanner=c.stageGap=0;c.landBlend=1;c.landFrom=Lands.RAINBOW;c.enemies.clear();c.power=null;c.modeLeft=0;
        c.boss.begin(Boss.RAINBOW_CAT,30,c.rnd,true);c.boss.intro=0;c.boss.update(0,L,c.rnd);
        return c;
    }
    static void challenge(GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;a.release();
        for(int i=0;i<240 && a.phase!=RainbowCat.OPEN;i++) {
            if(a.phase==RainbowCat.CHASE) {a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);}
            if(a.phase==RainbowCat.CIRCLE)circle(c,L,1);
            c.boss.update(1f/60,L,c.rnd);
        }
    }
    static void readyToCircle(GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;a.release();
        for(int i=0;i<120 && a.phase!=RainbowCat.CIRCLE;i++) {
            if(a.phase==RainbowCat.CHASE) {a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);}
            c.boss.update(1f/60,L,c.rnd);
        }
    }
    static void circle(GameCore c,Layout L,int direction) {rainbowCircle(c,L,direction);}
    private static void circleGestures(Layout L) {
        group("Rainbow Cat circle and fishing net");
        for(int direction:new int[]{-1,1}) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;readyToCircle(c,L);
            check("two taps wait for a circle "+direction,a.taps==2 && a.phase==RainbowCat.CIRCLE && !c.boss.open());
            check("circle fits safely inside play area "+direction,a.circleX()-a.circleRadius()>L.playLeft
                    && a.circleX()+a.circleRadius()<L.playRight && a.circleY()-a.circleRadius()>L.playTop
                    && a.circleY()+a.circleRadius()<L.deckTop);
            a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
            check("extra tap cannot substitute for circle "+direction,a.phase==RainbowCat.CIRCLE && a.taps==2);
            circle(c,L,direction);
            check("either circle direction starts net catch "+direction,a.phase==RainbowCat.CATCH && a.carries(c) && c.boss.boltCount()==0);
            check("net targets the cat capture position "+direction,a.catchX==a.x && a.catchY==a.y);
            c.boss.update(RainbowCat.NET_TIME*.5f,L,c.rnd);
            check("net animation cannot consume prompt time "+direction,!c.boss.open() && a.timer==0);
            c.boss.update(RainbowCat.NET_TIME*.5f,L,c.rnd);
            check("net catch opens full duration challenge "+direction,c.boss.open() && a.timer==a.promptTime());
            while(a.phase==RainbowCat.OPEN)c.boss.press(a.prompt[a.at],c.rnd,L);
            check("companion returns after escape "+direction,a.netReturn>0 && a.carries(c));
            c.boss.update(RainbowCat.NET_RETURN+.01f,L,c.rnd);
            check("return completes and clears companion ownership "+direction,!a.carries(c));
        }
        GameCore c=fight(L);RainbowCat a=c.boss.cat;readyToCircle(c,L);float r=a.circleRadius();
        a.touch(c,L,0,151,a.x+r,a.y);
        for(int i=0;i<30;i++) {float angle=(i%2==0?.8f:-.8f);a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*r,a.y+(float)Math.sin(angle)*r);}
        check("back and forth swipes do not accumulate a circle",a.phase==RainbowCat.CIRCLE);
        a.touch(c,L,3,151,a.x+r,a.y);
        check("cancel clears circle progress",a.pointer==-1 && !a.circling && a.circleSweep==0);
        a.touch(c,L,0,151,a.x+r,a.y);a.touch(c,L,2,152,a.x,a.y+r);
        check("another pointer cannot extend the circle",a.circleSweep==0);
        a.touch(c,L,2,151,a.x,a.y);
        check("crossing through cat invalidates circle",!a.circling && a.phase==RainbowCat.CIRCLE);
        Pause.release(c);check("pause clears circle ownership",a.pointer==-1);
        circle(c,L,1);c.boss.leave();
        check("leaving clears net and circle",!a.carries(c) && a.netReturn==0 && !a.circling);
    }
    private static void tapFlash(Layout L) {
        group("Cat rainbow flash gates repeat taps until scared");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        java.util.HashSet<Integer> colors=new java.util.HashSet<Integer>();
        for(int i=0;i<35;i++) {
            colors.add(RainbowCatArt.tapColor(a));
            c.boss.update(1f/60,L,c.rnd);
            a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        }
        check("flash visits every rainbow color",colors.size()==RainbowLand.CLEAR_COLORS.length);
        check("spam cannot tap through rainbow flash",a.taps==1 && a.tapFlash>0 && !a.canTap() && a.scared==0);
        for(int i=0;i<10;i++)c.boss.update(1f/60,L,c.rnd);
        check("scared expression appears before next tap is enabled",a.tapFlash==0 && a.scared>=.65f && a.canTap());
        float shake=0;
        for(int i=0;i<38;i++) {c.boss.update(1f/60,L,c.rnd);shake=Math.max(shake,Math.abs(a.shakeOffset()));}
        check("cat visibly shakes off fright",a.scared>0 && a.scared<1 && shake>a.r*.05f && a.canTap());
        for(int i=0;i<30;i++)c.boss.update(1f/60,L,c.rnd);
        check("normal expression returns without relocking taps",a.scared==0 && Math.abs(a.shakeOffset())<.001f && a.canTap());
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        check("second tap restarts color flash",a.taps==2 && a.tapFlash==RainbowCat.TAP_FLASH && a.scared==0);
        for(int i=0;i<24;i++)c.boss.update(1f/60,L,c.rnd);
        check("circle waits for second color cycle",a.phase==RainbowCat.DASH && a.tapFlash>0);
        for(int i=0;i<24;i++)c.boss.update(1f/60,L,c.rnd);
        check("scared cat invites circling",a.phase==RainbowCat.CIRCLE && a.scared==1);
        c.boss.leave();check("encounter exit clears tap reaction",a.tapFlash==0 && a.scared==0);
    }
    private static void traceShapes(Layout L) {
        group("Ordered shape tracing supports crossed paths");
        float[] star=new float[12];
        for(int i=0;i<=5;i++) {float angle=-Softbody.TAU*.25f+Softbody.TAU*(i*2%5)/5;
            star[i*2]=(float)Math.cos(angle);star[i*2+1]=(float)Math.sin(angle);}
        for(int direction:new int[]{-1,1}) {
            TraceGesture trace=new TraceGesture();trace.setShape(star);
            check("star begins at tip "+direction,trace.begin(star[0],star[1]));
            boolean complete=false;
            for(int edge=1;edge<=5;edge++) {
                int point=direction>0?edge:5-edge;
                complete=trace.move(star[point*2],star[point*2+1]);
                if(edge<5)check("crossing cannot skip star edge "+direction+"/"+edge,!complete);
            }
            check("five straight strokes complete star "+direction,complete);
        }
        TraceGesture trace=new TraceGesture();trace.begin(1,0);
        check("straight chord cannot replace circle",!trace.move(-1,0) && !trace.active);
        trace.begin(1,0);
        for(int i=0;i<12;i++)trace.move((float)Math.cos(i%2*.6f),(float)Math.sin(i%2*.6f));
        check("reversing direction subtracts progress",trace.progress()<.2f);
        trace.setShape(star);trace.begin(star[0],star[1]);
        trace.move(star[2],star[3]);float before=trace.progress();trace.move(star[0],star[1]);
        check("retracing star edge does not count twice",trace.progress()<before*.1f);
    }
    private static void windUp(Layout L) {
        group("Companion winds up with drawing progress");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;readyToCircle(c,L);
        float radius=a.circleRadius();a.touch(c,L,0,151,a.x+radius,a.y);
        for(int i=1;i<=10;i++) {
            float angle=Softbody.TAU*i/40;
            a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*radius,a.y+(float)Math.sin(angle)*radius);
            c.boss.update(.03f,L,c.rnd);
        }
        float first=a.windUp;
        check("companion rises during first part of circle",first>.1f && a.carries(c));
        for(int i=11;i<=30;i++) {
            float angle=Softbody.TAU*i/40;
            a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*radius,a.y+(float)Math.sin(angle)*radius);
            c.boss.update(.03f,L,c.rnd);
        }
        check("wind-up increases as player traces",a.windUp>first*2 && a.phase==RainbowCat.CIRCLE);
        a.touch(c,L,3,151,a.x,a.y);
        for(int i=0;i<30;i++)c.boss.update(.03f,L,c.rnd);
        check("cancel smoothly returns companion home",a.windUp<.002f && !a.carries(c));
        circle(c,L,1);c.boss.update(.35f,L,c.rnd);
        check("big release lands before the prompt opens",a.cue==3 && a.phase==RainbowCat.CATCH && a.netAmount()==1);
    }
    private static void battleBackground(Layout L) {
        group("Rainbow Cat accelerated background");
        GameCore normal=TestFastEnemies.scene(L,26,151);
        normal.skyClock=10;normal.update(.1f,L);
        check("ordinary Rainbow Land retains normal drift",Math.abs(normal.skyClock-10.1f)<.001f);
        for(boolean timeAttack:new boolean[]{false,true}) {
            GameCore c=fight(L);c.timeAttack.active=timeAttack;c.skyClock=10;
            for(int phase:new int[]{RainbowCat.CHASE,RainbowCat.DASH,RainbowCat.OPEN,RainbowCat.CIRCLE,RainbowCat.CATCH}) {
                c.boss.cat.phase=phase;c.boss.cat.timer=10;
                float before=c.skyClock;c.update(.1f,L);
                check("battle scenery advances threefold in phase "+phase+" time attack "+timeAttack,
                        Math.abs(c.skyClock-before-.3f)<.001f);
            }
            float before=c.skyClock;c.paused=true;c.update(.1f,L);
            check("pause holds battle scenery "+timeAttack,c.skyClock==before);
            c.paused=false;c.settingsOpen=true;c.update(.1f,L);
            check("settings holds battle scenery "+timeAttack,c.skyClock==before);
            c.settingsOpen=false;c.boss.leave();c.update(.1f,L);
            check("leaving battle restores normal drift without resetting position "+timeAttack,
                    Math.abs(c.skyClock-before-.1f)<.001f);
        }
        GameCore other=fight(L);other.boss.kind=Boss.SLIME;other.skyClock=10;other.update(.1f,L);
        check("other bosses retain normal drift",Math.abs(other.skyClock-10.1f)<.001f);
    }
    static void trailAndCadence(Layout L) {
        group("Rainbow Cat attack pacing and motion trail");
        for(boolean kids:new boolean[]{false,true}) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;a.kids=kids;a.launchLeft=0;
            c.boss.update(.01f,L,c.rnd);float interval=kids?2.3f:1.6f;
            check("chase launch interval doubled "+kids,Math.abs(a.launchLeft-interval)<.001f);
            c.boss.clearBolts();
            for(int i=0;i<(int)(interval*60)-1;i++)c.boss.update(1f/60,L,c.rnd);
            check("no early chase shot "+kids,c.boss.boltCount()==0);
            c.boss.update(.04f,L,c.rnd);
            check("next shot arrives at slower cadence "+kids,c.boss.boltCount()==1);
        }
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        float[] historyX=new float[120],historyY=new float[120];
        for(int i=0;i<120;i++) {
            c.boss.update(1f/60,L,c.rnd);historyX[i]=a.x;historyY[i]=a.y;
        }
        boolean follows=a.trailCount>=119;
        for(int i=0;i<Math.min(119,a.trailCount);i++)
            follows &= RainbowCat.distance(a.trailX[i],a.trailY[i],historyX[119-i],historyY[119-i])<.01f;
        check("continuous chase ribbon records the actual path",follows);
        check("ribbon is four times longer",Math.abs(RainbowCat.TRAIL_LENGTH-11.6f)<.001f
                && Math.abs(RainbowCat.TRAIL_TIME-2.2f)<.001f);
        check("only final quarter fades",RainbowCat.trailFade(.74f)==1
                && Math.abs(RainbowCat.trailFade(.875f)-.5f)<.001f && RainbowCat.trailFade(1)==0);
        a.phase=RainbowCat.OPEN;a.timer=99;
        for(int i=0;i<140;i++)c.boss.update(1f/60,L,c.rnd);
        check("motion trail expires while cat holds prompt pose",a.trailCount==0);
    }
    static void all(Layout L) {
        tapFlash(L);traceShapes(L);windUp(L);circleGestures(L);battleBackground(L);trailAndCadence(L);catalog();enemies(L);clearParticles(L);titleRainbow(L);background(L);guidance(L);
        group("Rainbow Cat chase, taps and challenge");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        check("Rainbow Land follows swamp with stable cave slot",Lands.firstStage(Lands.RAINBOW)==26 && Lands.forStage(26)==Lands.RAINBOW && !Lands.playable(Cave.LAND));
        check("cat is fifty percent larger",Math.abs(a.r-L.w*.073f*1.5f)<.01f);
        a.touch(c,L,0,3,L.w*.5f,L.deckTop+1);
        check("deck remains available",a.pointer==-1);
        float oldX=a.x,oldY=a.y,hp=c.boss.hp;
        a.touch(c,L,0,1,a.x,a.y);
        check("tap starts dash without teleport or premature damage",a.phase==RainbowCat.DASH && a.taps==1 && a.x==oldX && a.y==oldY && c.boss.hp==hp);
        a.touch(c,L,5,2,a.x,a.y);a.touch(c,L,2,1,a.x,a.y);
        check("held touch and extra finger cannot earn more taps",a.taps==1 && a.pointer==1);
        for(int i=0;i<24;i++)c.boss.update(1f/60,L,c.rnd);
        check("dash crosses field with a rainbow trail",RainbowCat.distance(oldX,oldY,a.x,a.y)>L.w*.35f && a.trailCount>0);
        a.touch(c,L,2,1,a.x,a.y);check("holding through dash never auto hits",a.taps==1);
        a.touch(c,L,3,1,a.x,a.y);check("cancel releases pointer",a.pointer==-1);
        a.touch(c,L,0,2,L.playLeft,a.y);a.touch(c,L,2,2,a.x,a.y);a.touch(c,L,1,2,a.x,a.y);
        check("swiping across cat does not substitute for tap",a.taps==1);
        challenge(c,L);
        check("two taps and circle open net challenge without damage",a.phase==RainbowCat.OPEN && a.taps==2 && a.count==6 && c.boss.open() && c.boss.hp==hp);
        int mask=0;for(int i=0;i<a.count;i++)mask|=1<<a.prompt[i];
        check("challenge includes each full roster key exactly once",Integer.bitCount(mask)==6);
        float deadline=a.timer;int wrong=(a.prompt[0]+1)%Glyph.COUNT;
        check("wrong key preserves progress and timer",c.boss.press(wrong,c.rnd,L)==Boss.REBUFF && a.at==0 && a.timer==deadline && c.boss.hp==hp);
        while(a.phase==RainbowCat.OPEN)c.boss.press(a.prompt[a.at],c.rnd,L);
        check("completed sequence damages one third and resumes chase",c.boss.hp==hp-1 && a.captures==1 && a.count==0 && a.taps==0);
        challenge(c,L);int expected=a.count;a.timer=.01f;c.boss.update(.02f,L,c.rnd);
        check("timeout launches one star per key without instant damage",c.boss.boltCount()==expected && c.boss.hp==hp-1 && a.phase==RainbowCat.DASH);
        boolean fast=true;float previous=1;
        for(int i=0;i<expected;i++) {fast&=c.boss.bfast[i] && c.boss.bt[i]<previous;previous=c.boss.bt[i];}
        check("rainbow flurry is fast and staggered",fast);
        int glyph=c.boss.bglyph[0];c.boss.press(glyph,c.rnd,L);
        check("matching key defends rush projectile",!c.boss.blive[0] && c.boss.hp==hp-1);
        c.boss.clearBolts();a.phase=RainbowCat.CHASE;a.launchLeft=0;c.boss.update(.01f,L,c.rnd);
        check("running cat launches rush stars",c.boss.boltCount()==1 && c.boss.bfast[0]);
        int hits=0;for(int i=0;i<85;i++) {a.launchLeft=99;hits+=c.boss.update(1f/60,L,c.rnd);}
        check("unanswered star costs one heart",hits==1);
        a.touch(c,L,0,7,L.playLeft,a.y);Pause.release(c);check("pause clears pointer",a.pointer==-1);
        c.boss.leave();check("leaving clears encounter and trails",!a.placed && a.pointer==-1 && a.count==0 && a.trailCount==0);
        for(boolean full:new boolean[]{false,true}) {
            c=fight(L);c.boss.rosterFull=full;challenge(c,L);a=c.boss.cat;mask=0;
            for(int i=0;i<a.count;i++)mask|=1<<a.prompt[i];
            check("challenge uses each active key once "+full,a.count==Roster.count(full) && Integer.bitCount(mask)==a.count);
        }
        c=fight(L);a=c.boss.cat;float travelled=0;
        for(int i=0;i<180;i++) {
            oldX=a.x;oldY=a.y;c.boss.update(1f/60,L,c.rnd);
            travelled+=RainbowCat.distance(oldX,oldY,a.x,a.y);
            if(a.x<a.left-.01f || a.x>a.right+.01f || a.y<a.top-.01f || a.y>a.bottom+.01f)break;
        }
        check("fast curved movement stays within arena",travelled>L.w*1.8f && a.x>=a.left && a.x<=a.right && a.y>=a.top && a.y<=a.bottom);
        group("Rainbow Cat uses the whole arena without alternating corners");
        c=fight(L);a=c.boss.cat;
        check("roaming reaches the sides and both ends of the play area",a.left<L.w*.15f && a.right>L.w*.85f
                && a.top<L.playTop+L.h*.08f && a.bottom>L.dangerY-L.h*.06f);
        float minX=a.x,maxX=a.x,minY=a.y,maxY=a.y,minDuration=99,maxDuration=0;
        int sameSide=0,sameHeight=0,routes=0;float previousTime=0;
        for(int frame=0;frame<60*90;frame++) {
            c.boss.update(1f/60,L,c.rnd);
            minX=Math.min(minX,a.x);maxX=Math.max(maxX,a.x);minY=Math.min(minY,a.y);maxY=Math.max(maxY,a.y);
            if(a.routeTime<previousTime || frame==0) {
                routes++;minDuration=Math.min(minDuration,a.routeDuration);maxDuration=Math.max(maxDuration,a.routeDuration);
                if((a.route[0]<(a.left+a.right)*.5f)==(a.route[6]<(a.left+a.right)*.5f))sameSide++;
                if((a.route[1]<(a.top+a.bottom)*.5f)==(a.route[7]<(a.top+a.bottom)*.5f))sameHeight++;
            }
            previousTime=a.routeTime;
        }
        check("paths visit all four outer regions",minX<a.left+(a.right-a.left)*.12f && maxX>a.right-(a.right-a.left)*.12f
                && minY<a.top+(a.bottom-a.top)*.12f && maxY>a.bottom-(a.bottom-a.top)*.12f);
        check("routes include same-side and same-height runs",sameSide>routes/6 && sameHeight>routes/6);
        check("travel durations vary instead of repeating one beat",maxDuration>minDuration*2);
        group("Rainbow reward save migration");
        long oldOwned=(1L<<60)|1L;
        long oldSave=Collect.PREVIOUS_TAG|oldOwned;
        check("61-entry tag does not become cat ownership",Collect.decode(oldSave)==oldOwned && Collect.currentSave(oldSave));
        long now=oldOwned|(1L<<61);
        check("new cat survives tagged save",Collect.decode(Collect.encode(now))==now && Collect.currentSave(Collect.encode(now)));
        check("old ducks retain bit identities",Collect.decode(Collect.OLD_TAG|(1L<<59))==(1L<<59));
        group("Rainbow challenge reachability");
        for(float fps:new float[]{30,60,120}) {
            c=fight(L);
            for(int frame=0;frame<fps*90 && !c.boss.beaten;frame++) {
                bossPlay(c,L);c.boss.update(1/fps,L,c.rnd);
            }
            check("three completed challenges beat cat at "+fps,c.boss.beaten && c.boss.cat.captures==3);
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
        check("chase explains tapping",a.companionHint().equals("TAP CAT TWICE!"));
        a.taps=1;check("companion counts remaining taps",a.companionHint().equals("ONE MORE TAP!"));
        a.phase=RainbowCat.CIRCLE;check("circle hint explains capture",a.companionHint().equals("CIRCLE THE CAT!"));
        a.phase=RainbowCat.DASH;check("dash hint tracks the trail",a.companionHint().equals("FOLLOW RAINBOW!"));
        a.phase=RainbowCat.OPEN;check("challenge explains order",a.companionHint().equals("MATCH IN ORDER!"));
        a.phase=RainbowCat.CHASE;
        TestOnboarding.touch(c,L,0,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        TestOnboarding.touch(c,L,1,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        check("help opens rush lesson despite skipped tutorials",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_RUSH);
        float time=c.time,px=a.x;
        c.update(.2f,L);
        check("help pauses live fight",c.time==time && a.x==px);
        TestOnboarding.acknowledge(c,L);
        check("help advances to tapping",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_TAPS);
        TestOnboarding.acknowledge(c,L);
        check("help advances to challenge keys",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.CAT_PROMPT);
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
