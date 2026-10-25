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
        for(int i=0;i<600 && a.phase!=RainbowCat.OPEN;i++) {
            if(a.phase==RainbowCat.CHASE) {a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);}
            if(a.phase==RainbowCat.CIRCLE)circle(c,L,1);
            c.boss.update(1f/60,L,c.rnd);
        }
        // Most prompt fixtures isolate key damage; transition retention has dedicated coverage below.
        c.boss.clearBolts();
    }
    static void readyToCircle(GameCore c,Layout L) {
        RainbowCat a=c.boss.cat;a.release();
        for(int i=0;i<480 && a.phase!=RainbowCat.CIRCLE;i++) {
            if(a.phase==RainbowCat.CHASE) {a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);}
            c.boss.update(1f/60,L,c.rnd);
        }
    }
    static void circle(GameCore c,Layout L,int direction) {rainbowCircle(c,L,direction);}
    private static void arrival(Layout L) {
        group("Rainbow Cat approaches from distance before the battle cry");
        float lastScale=0;int turns=0;float priorDelta=0;
        for(int step=1;step<=100;step++) {
            float u=step/100f,scale=RainbowCatArrival.scale(u);
            check("arrival grows steadily "+step,scale>=lastScale && scale<=1);lastScale=scale;
            float dx=RainbowCatArrival.x(L,u)-RainbowCatArrival.x(L,u-.01f);
            if(dx*priorDelta<0)turns++;if(Math.abs(dx)>.01f)priorDelta=dx;
        }
        check("approach weaves four times",turns==4);
        check("starts distant and ends full size at center",RainbowCatArrival.scale(0)<.1f
                && RainbowCatArrival.scale(1)==1 && Math.abs(RainbowCatArrival.x(L,1)-L.w*.5f)<.01f);
        check("burst fits complete long meow",RainbowCatArrival.BURST>CatMeowRecording.duration(CatMeowRecording.STRUGGLE));
        for(float fps:new float[]{30,60,120}) {
            GameCore c=fight(L);c.boss.begin(Boss.RAINBOW_CAT,30,c.rnd,true);Ear ear=new Ear();c.sound=ear;
            RainbowCat a=c.boss.cat;boolean earlyAttack=false,earlySound=false,fullStars=false;
            while(c.boss.intro>0) {
                c.update(1/fps,L);
                if(c.boss.intro>0) {
                    earlyAttack|=c.boss.boltCount()>0 || c.boss.fighting();
                    earlyAttack|=a.touch(c,L,0,1,a.x,a.y);
                }
                if(RainbowCatArrival.elapsed(c.boss)<RainbowCatArrival.FLIGHT-.001f)earlySound|=ear.catMeows>0;
                else fullStars|=RainbowCatArrival.starFade(c.boss)==1;
            }
            check("intro protects player until burst finishes "+fps,!earlyAttack && !earlySound && c.lives==GameCore.START_LIVES);
            check("battle cry fires once at burst "+fps,ear.catMeows==1 && ear.lastCatMeow==CatMeowRecording.STRUGGLE);
            check("stars fully present at foreground and remain in battle "+fps,fullStars && RainbowCatArrival.starFade(c.boss)==1);
            check("entry meets live position without teleport "+fps,Math.abs(a.x-L.w*.5f)<.01f && Math.abs(a.y-RainbowCatArrival.groundY(L))<.01f);
            c.boss.leave();check("exit resets arrival clock "+fps,a.clock==0 && !a.placed);
        }
        GameCore paused=fight(L);paused.boss.begin(Boss.RAINBOW_CAT,30,paused.rnd,true);paused.update(.3f,L);
        paused.paused=true;float intro=paused.boss.intro,clock=paused.boss.cat.clock;paused.update(.5f,L);
        check("pause freezes approach and stars",paused.boss.intro==intro && paused.boss.cat.clock==clock);
    }
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
            check("either circle direction starts net catch "+direction,a.phase==RainbowCat.CATCH && a.carries(c));
            check("net targets the cat capture position "+direction,a.catchX==a.x && a.catchY==a.y);
            c.boss.update(RainbowCat.NET_TIME*.5f,L,c.rnd);
            check("net animation cannot consume prompt time "+direction,!c.boss.open() && a.timer==0);
            c.boss.update(RainbowCat.NET_TIME*.5f,L,c.rnd);
            check("net catch opens full duration challenge "+direction,c.boss.open() && a.timer==a.promptTime());
            while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,c.rnd,L);
            check("companion returns after escape "+direction,a.netReturn>0 && a.carries(c));
            c.boss.update(RainbowCat.ESCAPE_TIME+.01f,L,c.rnd);
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
    private static void fiveShapes(Layout L) {
        group("Five distinct captures and forgiving cat taps");
        GameCore target=fight(L);RainbowCat t=target.boss.cat;
        check("pastry accepts taps in the added 15 percent margin",t.hit(t.x-t.r*1.28f*1.14f,t.y)
                && t.hit(t.x,t.y+t.r*1.16f*1.14f));
        check("pastry margin stays bounded",!t.hit(t.x-t.r*1.28f*1.16f,t.y));
        check("face also gains 15 percent margin",t.hit(t.head.centreX()+t.r*.84f*1.14f,t.head.centreY()));
        check("face margin stays bounded",!t.hit(t.head.centreX()+t.r*.84f*1.16f,t.head.centreY()));
        for(int direction:new int[]{-1,1}) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;
            check("cat starts with five health "+direction,c.boss.hp==5 && c.boss.hpMax==5);
            java.util.HashSet<String> shapes=new java.util.HashSet<String>();
            for(int capture=0;capture<5;capture++) {
                readyToCircle(c,L);shapes.add(java.util.Arrays.toString(TraceGesture.shape(a.captures)));
                check("next unique shape fits play area "+direction+"/"+capture,a.captures==capture
                        && a.x-a.circleRadius()>L.playLeft && a.x+a.circleRadius()<L.playRight
                        && a.y-a.circleRadius()>L.playTop && a.y+a.circleRadius()<L.deckTop);
                circle(c,L,direction);
                check("shape catches in either direction "+direction+"/"+capture,a.phase==RainbowCat.CATCH);
                c.boss.update(RainbowCat.NET_TIME,L,c.rnd);c.boss.clearBolts();
                for(int key=0;key<RainbowCat.HITS_PER_BAND && a.phase==RainbowCat.OPEN;key++)c.boss.press(a.promptKey,c.rnd,L);
                check("one damage per completed capture "+direction+"/"+capture,c.boss.hp==4-capture
                        && c.boss.beaten==(capture==4));
                check("capture chooses flurry or victory "+direction+"/"+capture,capture==4 ? a.rocket.active && c.boss.boltCount()==0 : c.boss.boltCount()==(capture>=3?12:6) && a.cue==2 && a.launchLeft==4.4f);
                boolean origin=true;
                for(int bolt=0;bolt<a.flurryCount;bolt++)origin&=c.boss.bsx[bolt]==a.x && c.boss.bsy[bolt]==a.y+a.r*.45f;
                check("damage flurry comes from cat "+direction+"/"+capture,origin);
            }
            check("all five shapes differ "+direction,shapes.size()==5);
            int exitHits=0;for(int frame=0;frame<500;frame++)exitHits+=c.boss.update(1f/60,L,c.rnd);
            check("victory animates away without hurting player "+direction,exitHits==0 && c.boss.boltCount()==0);
            c.boss.leave();check("exit clears flurry cue "+direction,a.cue==0);
        }
        group("Rainbow flurry sweeps keyboard in order");
        for(boolean full:new boolean[]{false,true})for(final boolean fromLeft:new boolean[]{false,true})for(boolean success:new boolean[]{false,true}) {
            GameCore c=fight(L);c.boss.rosterFull=full;challenge(c,L);
            RainbowCat a=c.boss.cat;int n=Roster.count(full);
            java.util.Random direction=new java.util.Random(151) {public boolean nextBoolean(){return fromLeft;}};
            if(success) {while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,direction,L);}
            else {a.timer=.01f;c.boss.update(.02f,L,direction);}
            boolean ordered=c.boss.boltCount()==n;float previous=fromLeft?-1:Float.MAX_VALUE;
            for(int i=0;i<n;i++) {
                float x=Roster.keyX(L,c.boss.bglyph[i],full?1:0);
                ordered&=fromLeft?x>previous:x<previous;previous=x;
                ordered&=Math.abs(c.boss.bt[i]+(RainbowCat.ESCAPE_TIME+RainbowCat.SWEEP_SETUP+RainbowCat.SWEEP_LEAD)/RainbowCat.PROJECTILE_TIME+a.flurryDelay(i)/RainbowCat.PROJECTILE_TIME)<.00001f && c.boss.bfast[i];
            }
            check("flurry preserves left/right order and stagger "+full+"/"+fromLeft+"/"+success,ordered);
        }
    }
    private static void doubleFlurries(Layout L) {
        group("Last two trace phases fire two stars per key");
        for(boolean full:new boolean[]{false,true})for(final boolean left:new boolean[]{false,true})
        for(int phase=0;phase<TraceGesture.SHAPE_COUNT;phase++)for(boolean success:new boolean[]{false,true}) {
            GameCore c=fight(L);c.boss.rosterFull=full;c.boss.hp=c.boss.hpMax-phase;challenge(c,L);
            RainbowCat a=c.boss.cat;
            java.util.Random direction=new java.util.Random(151) {public boolean nextBoolean(){return left;}};
            if(success)while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,direction,L);
            else {a.timer=.01f;c.boss.update(.02f,L,direction);}
            if(phase==4 && success) {
                check("final capture replaces flurry with rocket",a.rocket.active && c.boss.boltCount()==0 && !a.escapeSequence);
                continue;
            }
            int n=Roster.count(full),repeats=phase>=3?2:1;
            String tag=" "+full+left+phase+success;
            check("phase controls doubled count"+tag,c.boss.boltCount()==n*repeats && a.doubleFlurry==(repeats==2));
            boolean ordered=true;int[] counts=new int[Glyph.COUNT];
            for(int i=0;i<n*repeats;i++) {
                int key=Roster.at(full,left?i/repeats:n-1-i/repeats);counts[c.boss.bglyph[i]]++;
                ordered&=c.boss.bglyph[i]==key && c.boss.bfast[i] && c.boss.bhp[i]==1;
                if(i>0)ordered&=Math.abs(c.boss.bt[i-1]-c.boss.bt[i]-(RainbowCat.SWEEP_STEP+(repeats==2 && i%2==0?RainbowCat.DOUBLE_PAIR_GAP:0))/RainbowCat.PROJECTILE_TIME)<.00001f;
            }
            for(int i=0;i<n;i++)ordered&=counts[Roster.at(full,i)]==repeats;
            check("each key gets ordered individually staggered stars"+tag,ordered);
            check("companion announces double only in late phases"+tag,a.companionHint().equals(repeats==2?"DOUBLE RAINBOW?!":"RAINBOW INCOMING!"));
            if(repeats==2 && !c.boss.beaten) {
                while(c.boss.bt[1]<0)c.boss.update(1f/120,L,c.rnd);
                c.boss.press(c.boss.bglyph[0],c.rnd,L);
                check("first tap clears just the first star"+tag,!c.boss.blive[0] && c.boss.blive[1]);
                c.boss.press(c.boss.bglyph[1],c.rnd,L);
                check("second tap clears second star"+tag,!c.boss.blive[1]);
            }
            c.boss.leave();check("leaving clears double cue and all stars"+tag,!a.doubleFlurry && c.boss.boltCount()==0);
        }
    }
    private static void doubleSpeech(Layout L) {
        group("Double rainbow uses one cutesy companion TTS announcement");
        for(int phase:new int[]{0,3,4}) {
            GameCore c=fight(L);c.boss.hp=c.boss.hpMax-phase;challenge(c,L);Ear ear=new Ear();c.sound=ear;
            while(c.boss.cat.phase==RainbowCat.OPEN)c.boss.press(c.boss.cat.promptKey,c.rnd,L);
            c.update(1f/60,L);
            check("only double flurry speaks phrase "+phase,ear.companionExclaims==(phase==3?1:0)
                    && (phase!=3 || Narration.DOUBLE_RAINBOW.equals(ear.explanation)));
            for(int i=0;i<30;i++)c.update(1f/60,L);
            check("announcement is consumed once "+phase,ear.companionExclaims==(phase==3?1:0) && c.boss.cat.cue==0);
        }
    }
    private static void finalCaptureStar(Layout L) {
        group("Final Rainbow Cat capture uses a large crossed star");
        for(int direction:new int[]{-1,1}) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;readyToCircle(c,L);
            float circleRadius=a.circleRadius();
            check("first capture stays a circle "+direction,!a.starShape());
            for(int capture=0;capture<4;capture++) {
                challenge(c,L);
                for(int i=0;i<RainbowCat.HITS_PER_BAND && a.phase==RainbowCat.OPEN;i++)c.boss.press(a.promptKey,c.rnd,L);
            }
            readyToCircle(c,L);
            check("fourth damage selects star "+direction,a.captures==4 && a.starShape() && a.phase==RainbowCat.CIRCLE);
            check("star is larger and fits play area "+direction,a.circleRadius()>circleRadius
                    && a.x-a.circleRadius()>L.playLeft && a.x+a.circleRadius()<L.playRight
                    && a.y-a.circleRadius()>L.playTop && a.y+a.circleRadius()<L.deckTop);
            check("companion explains star "+direction,a.companionHint().equals("TRACE THE STAR!"));
            float[] star=TraceGesture.star();float radius=a.circleRadius();
            a.touch(c,L,0,151,a.x+star[0]*radius,a.y+star[1]*radius);
            for(int edge=1;edge<=5;edge++) {
                int point=direction>0?edge:5-edge;
                a.touch(c,L,2,151,a.x+star[point*2]*radius,a.y+star[point*2+1]*radius);
                if(edge<5)check("star crossing does not finish early "+direction+"/"+edge,a.phase==RainbowCat.CIRCLE);
            }
            a.touch(c,L,1,151,a.x+star[0]*radius,a.y+star[1]*radius);
            check("star catches cat "+direction,a.phase==RainbowCat.CATCH);
            c.boss.update(RainbowCat.NET_TIME,L,c.rnd);
            a.timer=.01f;c.boss.update(.02f,L,c.rnd);readyToCircle(c,L);
            check("missed prompt keeps star "+direction,a.captures==4 && a.starShape());
            challenge(c,L);
            for(int i=0;i<RainbowCat.HITS_PER_BAND && a.phase==RainbowCat.OPEN;i++)c.boss.press(a.promptKey,c.rnd,L);
            check("fifth damage completes star round "+direction,a.captures==5 && c.boss.beaten);
        }
    }
    private static void tapFlash(Layout L) {
        group("Cat rainbow flash gates repeat taps until scared");
        GameCore c=fight(L);RainbowCat a=c.boss.cat;
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        float tappedX=a.x,tappedY=a.y;
        java.util.HashSet<Integer> colors=new java.util.HashSet<Integer>();
        for(int i=0;i<35;i++) {
            colors.add(RainbowCatArt.tapColor(a));
            c.boss.update(1f/60,L,c.rnd);
            a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        }
        check("tap reaction stays at impact position",a.x==tappedX && a.y==tappedY && a.routeTime==0 && a.vx==0 && a.vy==0);
        check("flash visits every rainbow color",colors.size()==RainbowLand.CLEAR_COLORS.length);
        check("spam cannot tap through rainbow flash",a.taps==1 && a.tapFlash>0 && !a.canTap() && a.scared==0);
        for(int i=0;i<10;i++)c.boss.update(1f/60,L,c.rnd);
        check("scared reaction launches dash while taps remain locked",a.tapFlash==0 && a.scared>=.65f && a.phase==RainbowCat.DASH && !a.canTap() && a.routeTime>0);
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        check("tap during escape cannot score another hit",a.taps==1);
        float shake=0;
        for(int i=0;i<38;i++) {c.boss.update(1f/60,L,c.rnd);shake=Math.max(shake,Math.abs(a.shakeOffset()));}
        check("cat visibly shakes off fright",a.scared>0 && a.scared<1 && shake>a.r*.05f && a.canTap());
        for(int i=0;i<30;i++)c.boss.update(1f/60,L,c.rnd);
        check("escape reaches another location before reopening taps",a.canTap() && RainbowCat.distance(tappedX,tappedY,a.route[0],a.route[1])>a.r*2);
        check("normal expression returns without relocking taps",a.scared==0 && Math.abs(a.shakeOffset())<.001f && a.canTap());
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        check("second tap restarts color flash",a.taps==2 && a.tapFlash==RainbowCat.TAP_FLASH && a.scared==0);
        for(int i=0;i<24;i++)c.boss.update(1f/60,L,c.rnd);
        check("circle waits for second color cycle",a.phase==RainbowCat.DASH && a.tapFlash>0);
        for(int i=0;i<60 && a.phase!=RainbowCat.CIRCLE;i++)c.boss.update(1f/60,L,c.rnd);
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
        for(int direction:new int[]{-1,1}) {
            TraceGesture loose=new TraceGesture();
            check("trace can start beyond old tolerance "+direction,loose.begin(1.62f,0));
            boolean done=false;
            for(int i=1;i<=96 && !done;i++) {
                float angle=direction*Softbody.TAU*i/96;
                done=loose.move((float)Math.cos(angle)*1.62f,(float)Math.sin(angle)*1.62f);
            }
            check("loose trace closes in either direction "+direction,done);
            check("starting far from path is still rejected "+direction,!loose.begin(1.80f,0));
        }
        for(int tip=0;tip<5;tip++)for(int direction:new int[]{-1,1}) {
            TraceGesture rough=new TraceGesture();rough.setShape(TraceGesture.star(),true);
            int first=tip*2;float scale=.70f;
            rough.begin(star[first]*scale,star[first+1]*scale);
            boolean done=false;
            for(int edge=1;edge<=5;edge++) {
                int next=Math.floorMod(tip+direction*edge,5)*2;
                done=rough.move(star[next]*scale,star[next+1]*scale);
                if(edge<5)check("rough star still needs all five lines "+tip+"/"+direction+"/"+edge,!done);
            }
            check("short rounded star tips finish "+tip+"/"+direction,done);
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
    private static void recordedMeows(Layout L) {
        group("Recorded Rainbow Cat event voices");
        GameCore c=fight(L);Ear ear=new Ear();c.sound=ear;RainbowCat a=c.boss.cat;
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        check("tap uses annotated surprise",ear.catMeows==1 && ear.lastCatMeow==CatMeowRecording.SURPRISED);
        a.shortMeow(c);check("routine meow waits for reaction to finish",ear.catMeows==1);
        readyToCircle(c,L);int beforeCatch=ear.catMeows;circle(c,L,1);
        check("net catch uses surprised take",ear.catMeows==beforeCatch+1 && ear.lastCatMeow==CatMeowRecording.SURPRISED);
        for(int i=0;i<60 && a.phase!=RainbowCat.OPEN;i++)c.update(1f/60,L);
        check("caught cat uses retained meow instead of spoken aside",ear.lastCatMeow==CatMeowRecording.STRUGGLE);
        int before=ear.catMeows;
        while(a.phase==RainbowCat.OPEN) {
            int key=a.promptKey,verdict=c.boss.press(key,c.rnd,L);
            BossPlay.press(c,key,verdict,L);
        }
        check("every alternating hit plays a laser even while meow is busy",ear.catLasers==RainbowCat.HITS_PER_BAND);
        check("damage overrides chatter with annotated hurt",ear.catMeows==before+2 && ear.lastCatMeow==CatMeowRecording.HURT);
        c.update(.01f,L);
        check("damage flurry does not mask hurt with extra meow",ear.catMeows==before+2);
        a.voiceLeft=0;c.rnd.setSeed(151);java.util.Random expected=new java.util.Random(151);
        java.util.HashSet<Integer> takes=new java.util.HashSet<>();
        for(int i=0;i<CatMeowRecording.SHORT_COUNT;i++) {
            a.shortMeow(c);takes.add(ear.lastCatMeow);a.voiceLeft=0;
        }
        check("short takes cycle without repetition",takes.size()==CatMeowRecording.SHORT_COUNT);
        check("voice choices preserve gameplay RNG",c.rnd.nextLong()==expected.nextLong());
        a.reset();check("reset clears recorded voice cadence",a.voiceLeft==0 && a.nextMeow==1);
    }
    private static void traceFeedback(Layout L) {
        group("Tracing feedback follows progress and loss");
        GameCore c=fight(L);readyToCircle(c,L);RainbowCat a=c.boss.cat;
        Ear ear=new Ear();c.sound=ear;float radius=a.circleRadius();
        a.touch(c,L,0,151,a.x+radius,a.y);
        c.update(.02f,L);
        check("starting trace emits a light native haptic",c.bossDeathHaptic==1);
        c.update(.02f,L);
        check("holding still does not repeat haptics",c.bossDeathHaptic==0);
        boolean tick=false;
        for(int i=1;i<=16;i++) {
            float angle=i*.06f;
            a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*radius,a.y+(float)Math.sin(angle)*radius);
            c.update(.02f,L);tick|=c.bossDeathHaptic==1;
        }
        check("advancing along trace emits haptic ticks",tick);
        float traveled=a.gesture.travel;
        a.touch(c,L,2,151,a.x,L.playTop-1);
        check("leaving path preserves failed stroke and plays sound",a.traceLost>0 && a.lostTravel==traveled && ear.wrongs==1 && !a.circling);
        c.update(.02f,L);
        check("trace loss emits strong native haptic",c.bossDeathHaptic==2);
        a.touch(c,L,2,151,a.x,L.playTop-1);a.touch(c,L,1,151,a.x,L.playTop-1);
        check("loss only sounds once per attempt",ear.wrongs==1 && a.traceLost>0);
        a.touch(c,L,0,151,a.x+radius,a.y);
        check("retry immediately clears loss color",a.circling && a.traceLost==0);
        a.touch(c,L,2,151,a.x+radius*.9f,a.y+radius*.4f);
        a.touch(c,L,1,151,a.x+radius*.9f,a.y+radius*.4f);
        check("lifting before completion also reports loss",ear.wrongs==2 && a.traceLost>0 && a.pointer==-1);
        a.release();
        check("pause cancellation clears feedback without another failure",a.traceLost==0 && a.takeTraceHaptic()==0 && ear.wrongs==2);
        a.touch(c,L,0,151,a.x+radius,a.y);a.release();
        check("system cancellation is silent",ear.wrongs==2 && a.takeTraceHaptic()==0);
        circle(c,L,1);
        check("successful trace gives strong feedback without loss",a.phase==RainbowCat.CATCH && a.takeTraceHaptic()==2 && a.traceLost==0 && ear.wrongs==2);
        a.reset();check("encounter reset clears trace feedback",a.traceLost==0 && a.takeTraceHaptic()==0);
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
        check("small net follows drawing towards bottom",a.netAimY>.8f && Math.abs(a.netAimX)<.35f);
        check("companion rises during first part of circle",first>.1f && a.carries(c));
        for(int i=11;i<=30;i++) {
            float angle=Softbody.TAU*i/40;
            a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*radius,a.y+(float)Math.sin(angle)*radius);
            c.boss.update(.03f,L,c.rnd);
        }
        check("small net follows drawing around to top",a.netAimY<-.8f && Math.abs(a.netAimX)<.35f);
        check("wind-up increases as player traces",a.windUp>first*2 && a.phase==RainbowCat.CIRCLE);
        a.touch(c,L,3,151,a.x,a.y);
        for(int i=0;i<30;i++)c.boss.update(.03f,L,c.rnd);
        check("cancel smoothly returns companion home",a.windUp<.002f && !a.carries(c));
        circle(c,L,1);c.boss.update(.35f,L,c.rnd);
        check("big release lands before the prompt opens",a.cue==3 && a.phase==RainbowCat.CATCH && a.netAmount()==1);
        c.boss.update(RainbowCat.NET_TIME,L,c.rnd);
        float min=1,max=-1;
        for(int i=0;i<75;i++) {c.boss.update(1f/60,L,c.rnd);min=Math.min(min,a.pouchSway());max=Math.max(max,a.pouchSway());}
        check("caught pouch struggles to both sides",min<-.3f && max>.3f);
        c.boss.leave();check("leaving clears net following and struggle",a.netAimX==0 && a.netAimY==0 && a.caughtStrength()==0);
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
        GameCore other=fight(L);other.boss.kind=Boss.SPLITTER;other.skyClock=10;other.update(.1f,L);
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
    private static final class RingBudget extends OpacityPainter {
        int paths,circles,batches,vertices;
        RingBudget() {super(null,1);}
        public void polyline(float[] xy,int color,float width) {paths++;}
        public void fastPolyline(float[] xy,int color,float width) {batches++;vertices+=xy.length/2;}
        public void strokeCircle(float x,float y,float radius,int color,float width) {circles++;}
    }
    private static void ringBudget(Layout L) {
        group("Shape rings use bounded reusable primitive geometry");
        for(int shape=0;shape<TraceGesture.SHAPE_COUNT;shape++) {
            GameCore c=fight(L);c.boss.hp=c.boss.hpMax-shape;readyToCircle(c,L);circle(c,L,1);
            RainbowCat a=c.boss.cat;c.boss.update(.36f,L,c.rnd);float[] work=a.burstWork;
            RingBudget budget=new RingBudget();RainbowCatArt.shapeRings(new OpacityPainter(budget,.7f),a);
            check("no dynamic stroked paths for shape "+shape,budget.paths==0);
            check("at most two primitive layers per rainbow ring "+shape,budget.circles+budget.batches<=12 && budget.circles+budget.batches>0);
            check("circle uses native circles; other shapes batch segments "+shape,shape==0?budget.circles==12:budget.batches==12 && budget.vertices<=396);
            RainbowCatArt.shapeRings(budget,a);
            check("reuse ring geometry across frames "+shape,a.burstWork==work);
            a.reset();check("reset releases ring geometry "+shape,a.burstWork==null);
        }
    }
    private static void hitShape(Layout L) {
        group("Alternating hits flash without horizontally squashing the cat");
        GameCore hit=fight(L),idle=fight(L);challenge(hit,L);challenge(idle,L);
        RainbowCat a=hit.boss.cat,b=idle.boss.cat;Ear ear=new Ear();hit.sound=ear;
        int key=a.promptKey;hit.tapKey(key,L);
        a.animate(hit.boss,.02f,L);b.animate(idle.boss,.02f,L);
        check("hit leaves head and pastry spring motion unchanged",
                java.util.Arrays.equals(a.head.outline(),b.head.outline()) && java.util.Arrays.equals(a.pastry.outline(),b.pastry.outline()));
        check("flash and laser remain immediate",a.keyFlash>0 && ear.catLasers==1);
        hit.tapKey(key,L);check("repeated wrong hand cannot fire laser",ear.catLasers==1);
    }
    private static void hitCones(Layout L) {
        group("Prompt cones preserve hit origin and overlap without affecting damage");
        GameCore c=fight(L);challenge(c,L);RainbowCat a=c.boss.cat;
        int left=a.leftKey,right=a.rightKey;
        c.boss.press(left,c.rnd,L);c.boss.update(.08f,L,c.rnd);c.boss.press(right,c.rnd,L);
        check("alternating hits retain both prompt colors",a.hitConeKey[0]==left && a.hitConeKey[1]==right && a.hitConeLife[0]>0 && a.hitConeLife[1]>0);
        check("cones originate on opposite displayed prompts",a.hitConeX[0]==a.promptX(L,left) && a.hitConeX[1]==a.promptX(L,right) && a.hitConeX[0]<a.hitConeX[1]);
        int next=a.nextHitCone;c.boss.press(right,c.rnd,L);
        check("wrong key does not emit a cone",a.nextHitCone==next);
        a.timer=.01f;c.boss.update(.02f,L,c.rnd);
        check("last cones survive net escape",a.escapeSequence && a.hitConeLife[1]>0);
        c.boss.update(RainbowCat.HIT_CONE_TIME,L,c.rnd);
        boolean clear=true;for(float life:a.hitConeLife)clear&=life==0;
        check("cones expire during escape",clear);
        a.hitConeLife[0]=1;a.reset();check("reset clears cones",a.hitConeLife[0]==0 && a.nextHitCone==0);
    }
    private static void damageWindows(Layout L) {
        group("Fixed left/right pair deals small capped persistent damage");
        for(boolean full:new boolean[]{false,true}) {
            GameCore c=fight(L);c.boss.rosterFull=full;challenge(c,L);RainbowCat a=c.boss.cat;
            float timer=a.timer;int previous=-1,left=a.leftKey,right=a.rightKey;
            check("pair uses opposite hands "+full,Roster.keyX(L,left,full?1:0)<L.w*.5f && Roster.keyX(L,right,full?1:0)>L.w*.5f);
            check("left key starts alternating pair "+full,a.promptKey==left);
            for(int hit=0;hit<2;hit++) {
                int key=a.promptKey;boolean active=false;
                for(int i=0;i<Roster.count(full);i++)active|=key==Roster.at(full,i);
                check("random prompt stays in roster and changes "+full+hit,active && key!=previous && key==(hit%2==0?left:right));
                float hp=c.boss.hp;
                check("each hit deals immediate damage "+full+hit,c.boss.press(key,c.rnd,L)==Boss.HIT && Math.abs(c.boss.hp-(hp-.05f))<.0001f);
                check("hit flashes and changes key without resetting timer "+full+hit,a.keyFlash>0 && a.promptKey!=key && a.timer==timer && a.leftKey==left && a.rightKey==right);
                hp=c.boss.hp;c.boss.press(key,c.rnd,L);
                check("repeating previous key cannot damage "+full+hit,c.boss.hp==hp && a.timer==timer);
                previous=key;
            }
            float partial=c.boss.hp;a.timer=.01f;c.boss.update(.02f,L,c.rnd);
            check("timeout retains partial damage and current shape "+full,c.boss.hp==partial && a.captures==0 && a.escapeSequence);
            challenge(c,L);check("recapture retains band floor "+full,a.captures==0 && a.damageFloor==4);
            int hits=0;
            while(a.phase==RainbowCat.OPEN && hits<RainbowCat.HITS_PER_BAND) {c.boss.press(a.promptKey,c.rnd,L);hits++;}
            check("only remaining band damage is available "+full,hits==RainbowCat.HITS_PER_BAND-2 && c.boss.hp==4 && a.captures==1);
            for(int key=0;key<Glyph.COUNT;key++)c.boss.press(key,c.rnd,L);
            check("extra keys cannot skip shape phase "+full,c.boss.hp==4);
            readyToCircle(c,L);
            check("next trace follows health band "+full,a.captures==1 && !a.starShape());
        }
        for(boolean full:new boolean[]{false,true})for(float rate:new float[]{2,6}) {
            GameCore paced=fight(L);paced.boss.rosterFull=full;challenge(paced,L);RainbowCat a=paced.boss.cat;
            while(a.phase==RainbowCat.OPEN) {
                paced.boss.update(1/rate,L,paced.rnd);
                if(a.phase==RainbowCat.OPEN)paced.boss.press(a.promptKey,paced.rnd,L);
            }
            check("fast alternating taps fill band; slower taps preserve partial damage "+full+rate,
                    rate==6?paced.boss.hp==4:paced.boss.hp>4 && paced.boss.hp<5);
        }
        GameCore c=fight(L);c.boss.hp=1.4f;readyToCircle(c,L);
        check("shape derives from sustained damage ",c.boss.cat.captures==3);
        c.boss.cat.reset();check("reset clears damage window state",c.boss.cat.promptKey<0 && c.boss.cat.windowHits==0 && c.boss.cat.keyFlash==0);
    }
    private static void escapeSweep(Layout L) {
        group("Cat waits for net then sweeps with its ordered flurry");
        for(boolean full:new boolean[]{false,true})for(final boolean left:new boolean[]{false,true})for(boolean late:new boolean[]{false,true}) {
            GameCore c=fight(L);c.boss.rosterFull=full;challenge(c,L);RainbowCat a=c.boss.cat;
            if(late) {c.boss.hp=2;a.damageFloor=1;a.captures=3;}
            java.util.Random rnd=new java.util.Random(151) {public boolean nextBoolean(){return left;}};
            while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,rnd,L);
            float x=a.x,y=a.y;
            for(int frame=0;frame<60;frame++)c.boss.update(1f/60,L,c.rnd);
            boolean queued=true;for(int i=0;i<a.flurryCount;i++)queued&=c.boss.bt[i]<0;
            check("hold and queued flurry "+full+left+late,a.x==x && a.y==y && queued && !a.canTap() && a.angry()>.9f);
            boolean unavailable=true;for(int i=0;i<a.flurryCount;i++)unavailable&=!c.boss.boltWants(c.boss.bglyph[i]);
            check("queued flurry cannot be cleared before launch "+full+left+late,unavailable);
            while(a.escapeAge<RainbowCat.ESCAPE_TIME+RainbowCat.SWEEP_SETUP+.02f)c.boss.update(1f/120,L,c.rnd);
            boolean ordered=true,origin=true;float previous=a.x,highest=a.y;
            while(a.escapeSequence) {
                float[] before=c.boss.bt.clone();c.boss.update(1f/120,L,c.rnd);
                if(a.escapeSequence)ordered&=left?a.x>=previous-.01f:a.x<=previous+.01f;
                for(int i=0;i<a.flurryCount;i++)if(before[i]<0 && c.boss.bt[i]>=0)
                    origin&=a.shotReach==1 && a.shotWind==0 && Math.abs(c.boss.bsx[i]-a.shotX())<.01f && Math.abs(c.boss.bsy[i]-a.shotY())<.01f;
                previous=a.x;highest=Math.min(highest,a.y);
            }
            check("slash arc rises above both endpoints "+full+left+late,highest<y-a.r && highest>=a.top-.01f);
            check("cat sweeps in key order "+full+left+late,ordered && a.flurryFromLeft==left);
            check("flurry originates from sweeping cat "+full+left+late,origin);
            check("sweep releases chase after normal and double flurry "+full+left+late,(a.phase==RainbowCat.DASH || a.phase==RainbowCat.CHASE) && !a.escaping);
        }
    }
    private static void netEscape(Layout L) {
        group("Completed cat prompt bursts net and recoils companion");
        for(boolean late:new boolean[]{false,true}) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;challenge(c,L);
            if(late) {c.boss.hp=2;a.damageFloor=1;a.captures=3;}
            int lives=c.lives;float x=a.x,y=a.y;
            for(int i=0,n=RainbowCat.HITS_PER_BAND;i<n;i++) {
                c.boss.press(a.promptKey,c.rnd,L);
                check("escape only after final correct key "+late+"/"+i,(a.escapeLeft>0)==(i==n-1));
            }
            check("escape keeps companion owned and anchors burst "+late,a.carries(c) && a.escapeX==x && a.escapeY==y);
            c.boss.update(.2f,L,c.rnd);
            check("angry cat holds during companion recoil "+late,a.carries(c) && a.x==x && a.y==y && a.angry()>.9f && c.lives==lives);
            c.boss.update(RainbowCat.ESCAPE_TIME,L,c.rnd);
            check("escape finishes on normal and late capture "+late,a.escapeLeft==0 && !a.carries(c));
            a.escapeLeft=1;c.boss.leave();check("leaving clears escape "+late,a.escapeLeft==0 && !a.escapeSequence);
        }
        GameCore miss=fight(L);challenge(miss,L);RainbowCat a=miss.boss.cat;
        miss.boss.press((a.promptKey+1)%Glyph.COUNT,miss.rnd,L);
        check("wrong key does not burst net",a.escapeLeft==0);
        a.timer=.01f;miss.boss.update(.02f,L,miss.rnd);
        check("timeout also plays net escape",a.escapeLeft>0 && a.escapeSequence);
    }
    private static void traceCelebration(Layout L) {
        group("Rainbow Cat miniature shape dance and capture rings");
        for(int shape=0;shape<5;shape++) {
            GameCore c=fight(L);RainbowCat a=c.boss.cat;c.boss.hp=c.boss.hpMax-shape;
            readyToCircle(c,L);float x=a.circleX(),y=a.circleY();
            for(int frame=0;frame<75;frame++)c.boss.update(1f/60,L,c.rnd);
            check("waiting cat moves inside fixed guide "+shape,a.circleX()==x && a.circleY()==y
                    && Math.hypot(a.traceX,a.traceY)>a.r*.1f && Math.hypot(a.traceX,a.traceY)<a.r*.8f);
            circle(c,L,1);
            check("moving cat still accepts anchored trace "+shape,a.phase==RainbowCat.CATCH && a.shapeBurst>0
                    && java.util.Arrays.equals(a.burstShape,TraceGesture.shape(shape)));
            float[] snapshot=a.burstShape;a.captures++;
            c.boss.update(.34f,L,c.rnd);
            check("capture rings keep completed shape and cat settles into net "+shape,a.burstShape==snapshot
                    && Math.hypot(a.traceX,a.traceY)<a.r*.01f);
            c.boss.update(1,L,c.rnd);
            check("capture rings expire "+shape,a.shapeBurst==0 && a.burstShape==null);
            a.reset();check("reset clears miniature motion "+shape,a.traceX==0 && a.traceY==0 && a.traceClock==0);
        }
    }
    private static void retainedProjectiles(Layout L) {
        group("Cat transitions preserve incoming stars");
        GameCore c=fight(L);c.boss.hp=2;readyToCircle(c,L);Boss b=c.boss;RainbowCat a=b.cat;
        b.clearBolts();
        for(int i=0;i<3;i++) {int slot=b.tailBolt(c.rnd,L.w*.2f,L.playTop);b.bglyph[slot]=i;b.bfast[slot]=true;b.bt[slot]=.05f;}
        circle(c,L,1);
        check("capture retains all three existing shots",a.phase==RainbowCat.CATCH && b.boltCount()==3 && b.bt[0]==.05f);
        b.update(RainbowCat.NET_TIME,L,c.rnd);
        check("opening keeps stars moving toward player",a.phase==RainbowCat.OPEN && b.boltCount()==3 && b.bt[0]>.05f+RainbowCat.NET_TIME/RainbowCat.PROJECTILE_TIME-.01f);
        float hp=b.hp;
        check("incoming matching star takes priority over vulnerable prompt",b.press(0,c.rnd,L)==Boss.PARRY && b.hp==hp && b.boltCount()==2);
        int pending=b.tailBolt(c.rnd,L.w*.1f,L.playTop);b.bt[pending]=-.2f;
        float origin=b.bsx[pending];float oldProgress=b.bt[1];
        while(a.phase==RainbowCat.OPEN)a.press(b,a.promptKey,L,c.rnd);
        check("double flurry fits alongside three retained shots",a.doubleFlurry && b.boltCount()==3+Glyph.COUNT*2);
        boolean distinct=true;java.util.HashSet<Integer> slots=new java.util.HashSet<Integer>();
        for(int slot:a.flurrySlots)distinct&=slot>=0 && slot!=pending && slots.add(slot);
        check("new volley has separate slots and a faster launch cadence",distinct && slots.size()==Glyph.COUNT*2
                && Math.abs(a.flurryDelay(1)-.192f)<.0001f && Math.abs(a.flurryDelay(2)-.464f)<.0001f);
        b.update(.01f,L,c.rnd);
        check("escape neither resets live motion nor moves older queued origin",b.bt[1]>oldProgress && b.bsx[pending]==origin && b.bt[pending]>-.2f);
        int landed=0;for(int frame=0;frame<60;frame++)landed+=b.update(1f/60,L,c.rnd);
        check("retained shots still damage at the deck",landed>=2);
        b.leave();check("exit still clears all owned projectiles",b.boltCount()==0);
        c=fight(L);c.boss.hp=1;challenge(c,L);Ear ear=new Ear();c.sound=ear;
        while(c.boss.cat.phase==RainbowCat.OPEN)c.tapKey(c.boss.cat.promptKey,L);
        check("final hit prioritizes long low defeat recording",c.boss.beaten && ear.lastCatMeow==CatMeowRecording.DEFEAT
                && c.boss.cat.voiceLeft>=CatMeowRecording.duration(CatMeowRecording.DEFEAT));
        int calls=ear.catMeows;for(int i=0;i<30;i++)c.update(1f/60,L);
        check("defeat recording is not retriggered during cage transition",ear.catMeows==calls);
    }
    private static void rocketShakeFeedback(Layout L) {
        group("Overturned net, shake feedback and rocket engine lifecycle");
        GameCore c=fight(L);c.boss.hp=1;challenge(c,L);
        while(c.boss.cat.phase==RainbowCat.OPEN)c.boss.press(c.boss.cat.promptKey,c.rnd,L);
        RainbowCat a=c.boss.cat;RainbowCatRocket r=a.rocket;Ear ear=new Ear();c.sound=ear;
        int shakes=0,landing=0,launch=0;boolean inverted=true;
        while(r.age<RainbowCatRocket.CLOSE+.2f) {
            c.update(1f/120,L);
            if(r.age>=RainbowCatRocket.TIP && r.age<RainbowCatRocket.RELEASE && c.bossDeathHaptic>0)shakes++;
            if(r.age>=RainbowCatRocket.DROP && r.age<RainbowCatRocket.CLOSE && c.bossDeathHaptic==2)landing++;
            if(r.age>=RainbowCatRocket.TIP && r.age<RainbowCatRocket.RELEASE)
                inverted&=r.netAngle()<-3 && r.catAngle()<-3;
        }
        check("net and occupant stay inverted while shaking",inverted);
        check("three deliberate shakes each get one tactile and sound cue",shakes==RainbowCatRocket.SHAKES && ear.swishes==RainbowCatRocket.SHAKES);
        check("cat landing gets its own heavy impact",landing==1);
        check("engine spools up after cage closes",ear.rocketThrust>0 && ear.rocketThrust<1 && ear.courseFinishes==0);
        float pausedAt=r.age;Pause.open(c);check("pause silences rocket loop",ear.rocketThrust==0);
        c.update(.2f,L);check("pause holds the scoop sequence",r.age==pausedAt);
        Pause.resume(c);c.update(1f/120,L);check("resume restarts current engine thrust",ear.rocketThrust>0);
        while(c.boss.kind==Boss.RAINBOW_CAT && r.active) {
            float before=r.age;c.update(1f/120,L);
            if(before>=RainbowCatRocket.LAUNCH-.01f && before<RainbowCatRocket.LAUNCH+.5f && c.bossDeathHaptic>0)launch++;
        }
        check("blastoff plays once with a short haptic rumble",ear.courseFinishes==1 && launch==4 && ear.maxRocket>=.99f);
        check("engine stops before reward and remains silent",ear.rocketThrust==0);
        c=fight(L);c.boss.hp=1;challenge(c,L);
        while(c.boss.cat.phase==RainbowCat.OPEN)c.boss.press(c.boss.cat.promptKey,c.rnd,L);
        r=c.boss.cat.rocket;ear=new Ear();c.sound=ear;
        r.age=RainbowCatRocket.LAUNCH;r.feedback(c);c.toTitle();
        check("returning to title cannot leave engine running",ear.rocketThrust==0);
        if(BuildFlags.DEVELOPER) {
            c=fight(L);c.boss.hp=1;challenge(c,L);
            while(c.boss.cat.phase==RainbowCat.OPEN)c.boss.press(c.boss.cat.promptKey,c.rnd,L);
            r=c.boss.cat.rocket;ear=new Ear();c.sound=ear;
            r.age=RainbowCatRocket.LAUNCH;r.feedback(c);c.endCurrentRun();
            check("forced death stops rocket audio",ear.rocketThrust==0);
        }
    }
    private static void rocketVictory(Layout L) {
        group("Rainbow Cat carrier rocket victory");
        GameCore impact=fight(L);impact.boss.hp=1;challenge(impact,L);
        while(impact.boss.cat.phase==RainbowCat.OPEN)impact.boss.press(impact.boss.cat.promptKey,impact.rnd,L);
        RainbowCatRocket burst=impact.boss.cat.rocket;
        burst.feedback(impact);
        check("final impact flashes, shakes and pulses once",burst.finalImpact && impact.shake>=1
                && impact.flash>=.38f && impact.bossDeathHaptic==2);
        impact.shake=impact.flash=0;impact.bossDeathHaptic=0;burst.feedback(impact);
        check("final impact is not retriggered every frame",impact.shake==0 && impact.flash==0 && impact.bossDeathHaptic==0);
        burst.reset();check("final impact resets with victory",!burst.finalImpact && !burst.active);
        for(int fps:new int[]{30,60,120}) {
            GameCore c=fight(L);c.boss.hp=1;challenge(c,L);RainbowCat a=c.boss.cat;
            while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,c.rnd,L);
            RainbowCatRocket r=a.rocket;
            check("final hit keeps cat netted and clears threats "+fps,c.boss.beaten && r.active
                    && a.carries(c) && a.escapeLeft==0 && c.boss.boltCount()==0 && a.promptKey<0);
            check("carrier starts fully off left "+fps,r.rocketX(L)+r.radius(L)<0);
            int hits=0;boolean early=false;
            while(r.age<RainbowCatRocket.PULL)hits+=c.boss.update(1f/fps,L,c.rnd);
            check("carrier pulled to middle before drop "+fps,Math.abs(r.rocketX(L)-L.w*.5f)<.01f
                    && r.catY(a,L)<r.rocketY(L)-r.radius(L));
            while(r.age<RainbowCatRocket.DROP)hits+=c.boss.update(1f/fps,L,c.rnd);
            check("cat drops into carrier before closure "+fps,Math.abs(r.catY(a,L)-r.rocketY(L))<r.radius(L)*.1f);
            while(r.age<RainbowCatRocket.LAUNCH)hits+=c.boss.update(1f/fps,L,c.rnd);
            check("carrier stays grounded until sealed "+fps,Math.abs(r.rocketY(L)-r.ground(L))<L.h*.005f);
            while(r.age<RainbowCatRocket.DEPART)hits+=c.boss.update(1f/fps,L,c.rnd);
            check("rocket fully above screen while trail fades "+fps,r.rocketY(L)+r.radius(L)*1.2f<0 && !c.boss.gone());
            while(r.age<RainbowCatRocket.DURATION) {
                hits+=c.boss.update(1f/fps,L,c.rnd);
                early|=c.boss.gone() && r.age<RainbowCatRocket.DURATION;
            }
            check("exit waits for exhaust and cannot hurt player "+fps,!early && c.boss.gone() && hits==0 && c.boss.boltCount()==0);
            c.boss.leave();check("reset releases companion and rocket "+fps,!r.active && r.age==0 && !a.carries(c));
        }
        GameCore c=fight(L);c.boss.hp=1;challenge(c,L);
        while(c.boss.cat.phase==RainbowCat.OPEN)c.boss.press(c.boss.cat.promptKey,c.rnd,L);
        RainbowCatRocket r=c.boss.cat.rocket;
        c.paused=true;c.update(.1f,L);check("pause freezes rocket",r.age==0);
        c.paused=false;c.update(.1f,L);check("resume advances rocket",r.age>0);
    }
    private static void shotAnimation(Layout L) {
        group("Cat winds up and reaches on release, with slower star travel");
        for(int fps:new int[]{30,60,120}) {
            GameCore c=fight(L);Boss b=c.boss;RainbowCat a=b.cat;
            a.launchLeft=.32f;float wind=0;boolean fired=false;
            for(int frame=0;frame<fps;frame++) {
                b.update(1f/fps,L,c.rnd);wind=Math.max(wind,a.shotWind);
                if(b.boltCount()>0) {fired=true;break;}
            }
            check("windup before release "+fps,wind>.9f && fired);
            check("release at full reach "+fps,a.shotReach==1 && a.shotWind==0
                    && b.bsx[0]==a.shotX() && b.bsy[0]==a.shotY());
            a.launchLeft=99;b.update(.12f,L,c.rnd);
            check("paws settle after release "+fps,a.shotReach==0);
            check("25 percent slower travel "+fps,Math.abs(b.bt[0]-.12f/(1.2f/.75f))<.0001f);
            a.reset();check("shot pose reset "+fps,a.shotReach==0 && a.shotWind==0 && a.shotRecovery==0);
        }
    }
    private static final class TauntBudget extends OpacityPainter {
        int paths,vertices;boolean finite=true;
        TauntBudget(){super(null,1);}
        public int skin(){return Skins.DEFAULT;}
        public void fillPoly(float[] points,int color){paths++;vertices+=points.length/2;for(float v:points)finite &= Float.isFinite(v);}
        public void line(float x,float y,float x2,float y2,int color,float width){}
    }
    private static void tauntBudget(Layout L) {
        group("Taunt mouth uses bounded reusable ribbon geometry");
        RainbowCatTaunt.Mesh mesh=new RainbowCatTaunt.Mesh();
        int maxPaths=0,maxVertices=0;boolean finite=true;
        for(int frame=0;frame<264;frame++) {
            TauntBudget p=new TauntBudget();RainbowCatTaunt.ribbons(p,L,frame/60f,mesh);
            maxPaths=Math.max(maxPaths,p.paths);maxVertices=Math.max(maxVertices,p.vertices);finite &= p.finite;
        }
        check("whole animated face keeps a bounded path budget",maxPaths<=80 && maxPaths>0 && maxVertices<4000);
        check("partial mouth and moving sheen retain finite geometry",finite);
        RainbowCat a=new RainbowCat();a.tauntMesh=mesh;a.reset();
        check("reset releases taunt mesh",a.tauntMesh==null);
    }
    private static void lossAndFlurry(Layout L) {
        group("Cat loss doodle and release-timed flurry notes");
        for(int fps:new int[]{30,60,120})for(boolean doubled:new boolean[]{false,true}) {
            GameCore c=fight(L);c.devInfiniteLives=true;c.boss.hp=doubled?2:5;challenge(c,L);
            RainbowCat a=c.boss.cat;Ear ear=new Ear();c.sound=ear;
            while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,c.rnd,L);
            int count=a.flurryCount;c.update(.1f,L);
            check("queued flurry is silent "+fps+doubled,ear.catFlurries==0);
            c.paused=true;c.update(.5f,L);c.paused=false;
            check("pause cannot release notes "+fps+doubled,ear.catFlurries==0);
            for(int frame=0;frame<fps*5 && a.escapeSequence;frame++)c.update(1f/fps,L);
            check("every released star gets its ascending note once "+fps+doubled,
                    ear.catFlurries==count && ear.catFlurryOrdered && ear.lastFlurryNote==count-1 && a.flurryNotes==0);
            a.flurryFeedback(c);check("feedback drain never repeats notes "+fps+doubled,ear.catFlurries==count);
            c.toTitle();check("title clears queued notes "+fps+doubled,c.boss.cat.flurryNotes==0);
        }
        GameCore c=fight(L);Ear ear=new Ear();c.sound=ear;c.lives=1;
        c.takeHit(c.boss.cat.x,L);RainbowCat retained=c.bossVictory.cat;
        check("cat gets a complete loss painting and one taunt",c.deathDuration()==RainbowCatTaunt.DURATION
                && ear.bossTaunts==1 && !c.boss.active() && !retained.rocket.active);
        for(int stroke=0;stroke<4;stroke++)check("doodle stroke completes "+stroke,
                RainbowCatTaunt.ink(stroke,0)==0 && RainbowCatTaunt.ink(stroke,3.4f)==1);
        check("mouth is a downward smile",RainbowCatTaunt.point(L,3,.5f,1)<RainbowCatTaunt.point(L,3,0,1));
        check("cat loss keeps the battle palette",c.drained()==0);
        float sky=c.skyClock;c.update(.1f,L);
        check("taunt starts at battle scroll speed",Math.abs(c.skyClock-sky-.3f)<.001f && c.drained()==0);
        for(int frame=0;frame<90;frame++)c.update(1f/60,L);
        sky=c.skyClock;c.update(.1f,L);
        check("taunt accelerates to triple battle speed",Math.abs(c.skyClock-sky-.9f)<.001f && c.drained()==0);
        check("star motion remains continuous through acceleration",RainbowCatTaunt.starTravel(0)==0
                && Math.abs(RainbowCatTaunt.starTravel(1.201f)-RainbowCatTaunt.starTravel(1.2f)-.003f)<.0001f);
        float left=c.deathT;c.paused=true;c.update(.5f,L);c.paused=false;
        check("pause freezes taunt progress",c.deathT==left);
        for(int frame=0;frame<300 && c.dying();frame++)c.update(1f/60,L);
        check("loss painting ends cleanly without repeated meows",!c.dying() && ear.bossTaunts==1);
        c.toTitle();check("title releases loss snapshot",c.bossVictory==null && c.bossVictoryKind<0);
    }
    static void all(Layout L) {
        tauntBudget(L);lossAndFlurry(L);shotAnimation(L);rocketShakeFeedback(L);retainedProjectiles(L);rocketVictory(L);arrival(L);hitShape(L);doubleSpeech(L);doubleFlurries(L);hitCones(L);ringBudget(L);damageWindows(L);escapeSweep(L);netEscape(L);traceCelebration(L);fiveShapes(L);finalCaptureStar(L);tapFlash(L);traceShapes(L);recordedMeows(L);traceFeedback(L);windUp(L);circleGestures(L);battleBackground(L);trailAndCadence(L);catalog();enemies(L);clearParticles(L);titleRainbow(L);background(L);guidance(L);
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
        for(int i=0;i<90 && a.phase==RainbowCat.DASH;i++)c.boss.update(1f/60,L,c.rnd);
        check("dash crosses field with a rainbow trail",RainbowCat.distance(oldX,oldY,a.x,a.y)>L.w*.35f && a.trailCount>0);
        a.touch(c,L,2,1,a.x,a.y);check("holding through dash never auto hits",a.taps==1);
        a.touch(c,L,3,1,a.x,a.y);check("cancel releases pointer",a.pointer==-1);
        a.touch(c,L,0,2,L.playLeft,a.y);a.touch(c,L,2,2,a.x,a.y);a.touch(c,L,1,2,a.x,a.y);
        check("swiping across cat does not substitute for tap",a.taps==1);
        challenge(c,L);
        check("two taps and circle open net challenge without damage",a.phase==RainbowCat.OPEN && a.taps==2 && a.promptKey>=0 && c.boss.open() && c.boss.hp==hp);
        float deadline=a.timer;int wrong=(a.promptKey+1)%Glyph.COUNT;
        check("wrong key preserves progress and timer",c.boss.press(wrong,c.rnd,L)==Boss.REBUFF && a.windowHits==0 && a.timer==deadline && c.boss.hp==hp);
        while(a.phase==RainbowCat.OPEN)c.boss.press(a.promptKey,c.rnd,L);
        check("completed sequence damages one point and resumes chase",c.boss.hp==hp-1 && a.captures==1 && a.promptKey<0 && a.taps==0);
        challenge(c,L);int expected=Roster.count(c.boss.rosterFull);a.timer=.01f;c.boss.update(.02f,L,c.rnd);
        check("timeout launches one star per key without instant damage",c.boss.boltCount()==expected && c.boss.hp==hp-1 && a.phase==RainbowCat.ESCAPE);
        boolean fast=true;float previous=1;
        for(int i=0;i<expected;i++) {fast&=c.boss.bfast[i] && c.boss.bt[i]<previous;previous=c.boss.bt[i];}
        check("rainbow flurry is fast and staggered",fast);
        while(c.boss.bt[0]<0)c.boss.update(1f/120,L,c.rnd);
        int glyph=c.boss.bglyph[0];c.boss.press(glyph,c.rnd,L);
        check("matching key defends rush projectile",!c.boss.blive[0] && c.boss.hp==hp-1);
        c.boss.clearBolts();a.escapeSequence=false;a.phase=RainbowCat.CHASE;a.launchLeft=0;c.boss.update(.01f,L,c.rnd);
        check("running cat launches rush stars",c.boss.boltCount()==1 && c.boss.bfast[0]);
        int hits=0;for(int i=0;i<110;i++) {a.launchLeft=99;hits+=c.boss.update(1f/60,L,c.rnd);}
        check("unanswered star costs one heart",hits==1);
        a.touch(c,L,0,7,L.playLeft,a.y);Pause.release(c);check("pause clears pointer",a.pointer==-1);
        c.boss.leave();check("leaving clears encounter and trails",!a.placed && a.pointer==-1 && a.promptKey<0 && a.trailCount==0);
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
            check("five completed challenges beat cat at "+fps,c.boss.beaten && c.boss.cat.captures==5);
        }
        group("bounded Rainbow Cat one-finger play");
        // Faster requested flurries demand quicker defenses; keep both bounded skill profiles visible.
        for(float fps:new float[]{30,60,120})for(boolean brisk:new boolean[]{false,true}) {
            c=fight(L);Bot bot=new Bot(brisk?6:4,brisk?.18f:.30f,brisk?.04f:.08f,false,151);
            float bestHp=c.boss.hp;
            for(int frame=0;frame<fps*150 && !c.boss.beaten && c.state==GameCore.PLAY;frame++) {
                bestHp=Math.min(bestHp,c.boss.hp);
                bot.step(c,L,1/fps);c.update(1/fps,L);
            }
            System.out.println("    cat "+fps+"fps "+(brisk?"brisk":"steady")+": best hp="+bestHp+", lives="+c.lives+", won="+c.boss.beaten);
            if(brisk)check("bounded brisk player completes faster flurries at "+fps,c.boss.beaten && c.lives>0);
            else check("steady player can complete a capture at "+fps,bestHp<=4);
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
        for(int stage:new int[]{26,27})for(int mode:new int[]{-1,Power.FLURRY,Power.NINJA,Power.TEAM}) {
            GameCore c=TestFastEnemies.scene(L,stage,151);c.pairSpawnSlots=0;c.spawnedThisStage=1;
            c.mode=mode;c.modeLeft=mode<0?0:100;
            int rush=0,dive=0,fog=0;
            c.rnd.setSeed(151);
            for(int i=0;i<1000;i++) {
                c.enemies.clear();GameCore.Enemy e=add(c,L,new int[]{0,1,4},L.h*.4f);e.phase=6.283f*(i+.5f)/1000;
                if(mode<0){FastEnemies.nextDelay(c,L);if(c.fastEnemyNext)rush++;}
                else {FastEnemies.assign(c,e);if(e.fastSingle)rush++;}
                SwampEnemies.assign(c,e);if(e.swampKind==SwampEnemies.DIVE)dive++;
                if(e.swampKind==SwampEnemies.FOG)fog++;
            }
            check("increased rush chance "+mode,rush==(mode<0?920:390));
            check("increased dive chance "+mode,dive==360);
            check("Rainbow fog is half Swampland rate "+stage+"/"+mode,fog==(stage==26?100:125));
        }
    }

    private static void clearParticles(Layout L) {
        group("Rainbow projectile clear uses stronger rush burst");
        GameCore cat=fight(L);cat.boss.cat.launchLeft=0;cat.boss.update(.01f,L,cat.rnd);
        cat.boss.bt[0]=.4f;cat.particles.clear();
        cat.tapKey(cat.boss.bglyph[0],L);
        GameCore.Particle burst=null;
        for(GameCore.Particle p:cat.particles)if(p.rainbowStar)burst=p;
        check("clearing cat projectile creates star burst",burst!=null && cat.boss.boltDestroyed);
        if(burst!=null) {
            check("burst starts at cleared projectile",burst.x==cat.boss.hitX && burst.y==cat.boss.hitY);
            float y=burst.y;Fx.updateParticles(cat,.2f);
            check("burst expands in place without particle gravity",burst.y==y && burst.life<burst.max);
            Fx.updateParticles(cat,1);
            check("burst expires with other particles",!cat.particles.contains(burst));
        }
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
        a.phase=RainbowCat.OPEN;check("challenge explains changing key",a.companionHint().equals("LEFT, RIGHT!"));
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
