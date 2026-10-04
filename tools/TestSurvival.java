package com.dddumpling.game;

final class TestSurvival extends Check {
    static Mem store() {
        Mem m=new Mem();m.collected=1L|(1L<<Collect.BOSS_FIRST);m.tutorials|=Onboarding.SKIPPED;
        m.best=789;return m;
    }
    static GameCore start(Mem store,long seed) {
        GameCore c=new GameCore(store,seed,true);c.landSeen=LandPicker.stateMask();
        c.landChoice=1;c.fullRoster=false;c.modes.selected=ModeSelector.SURVIVAL;
        c.startGame();return c;
    }
    static void all(Layout L) {
        Mem m=store();GameCore c=start(m,149);
        check("Survival starts from normal selector",c.state==GameCore.PLAY && c.survival.active && c.stage==1);
        check("Survival preserves Adventure land choice",c.landChoice==1 && c.runStartLand==0 && c.best==0);
        long prizes=c.collected;String history=m.highScores;
        c.update(.01f,.25f,L);double before=c.survival.seconds;
        check("Survival counts active elapsed time",Math.abs(before-.25)<.001);
        c.slowdown=1;c.update(.01f,.25f,L);
        check("slow motion does not slow the record clock",Math.abs(c.survival.seconds-.5)<.001);
        Pause.open(c);before=c.survival.seconds;c.update(1,1,L);
        check("pause excludes elapsed time",c.survival.seconds==before);
        Pause.resume(c);PlayerSettings.open(c);c.update(1,1,L);
        check("settings exclude elapsed time",c.survival.seconds==before);c.closeSettings();
        c.spawnedThisStage=c.stageQuota();c.enemies.clear();c.shots.clear();
        check("clearing Adventure quota cannot end Survival",!c.stageCleared());
        c.spawnTimer=0;c.update(DT,L);
        check("Survival spawns beyond the ordinary quota",c.liveEnemies()>0 && !c.pendingBonus && !c.boss.active());
        GameCore.Enemy e=c.enemies.get(0);c.spawnTimer=10;
        c.destroyWord(e,e.baseX,e.y,L);
        check("ordinary clear requests immediate replacement",c.spawnTimer<=.18f);
        c.startFrenzy(Power.NINJA,L);c.modeLeft=.001f;c.update(DT,L);
        check("ending power keeps Survival continuous",!c.powerActive() && !c.stageByPower && !c.pendingBonus && !c.stageCleared());
        c.pushUsed=true;c.survival.rescueLeft=29.9f;c.update(.01f,.2f,L);
        check("rescue recharges without an interlude",!c.pushUsed && c.survival.rescueLeft==0);
        int calm=c.survival.background();float travel=c.travelSeconds(),spawn=c.spawnInterval();
        c.survival.seconds=300;c.survival.update(c,.01f);
        check("difficulty and distinct sky intensify together",c.travelSeconds()<travel && c.spawnInterval()<spawn
                && c.survival.background()!=calm && c.maxEnemies()==5 && c.stage==16);
        c.survival.seconds=1000;c.survival.update(c,.01f);
        check("difficulty has a bounded ceiling",c.stage==19 && c.travelSeconds()==8f && c.maxEnemies()==5);
        c.score=1234;c.lives=1;c.takeHit(L.w*.5f,L);
        check("death saves only Survival records",c.state==GameCore.OVER && c.survival.finished
                && m.best==789 && m.highScores.equals(history) && c.collected==prizes
                && c.progress.maximum("highest_stage")==0 && c.townRunTickets==0);
        int profile=c.survival.profile;long record=c.survival.bestTime[profile];before=c.survival.seconds;
        c.update(10,10,L);
        check("death animation cannot increase time",c.survival.seconds==before && c.survival.bestTime[profile]==record);
        GameCore loaded=new GameCore(m,150,true);
        check("Survival records persist separately",loaded.survival.bestTime[profile]==record && loaded.survival.bestScore[profile]==1234);
        c.survival.resultTap(c,L,L.w*.5f,Survival.resultY(L,true));
        check("retry starts the regular launch with Survival selected",c.starting() && !c.survival.active && c.modes.selected==ModeSelector.SURVIVAL);
        c.startGame();check("retry resets time and transient state",c.survival.active && c.survival.seconds==0 && !c.pushUsed && c.stage==1);
        c.toTitle();check("return restores Adventure progress and keeps mode choice",c.landChoice==1
                && c.best==c.landBests[1] && !c.survival.active && c.modes.selected==ModeSelector.SURVIVAL);
        c.modes.back(c);c.startGame();check("Adventure still starts at its selected land",!c.survival.active && c.stage==6);
        records(L);effects(L);backgroundCoverage();bounded(L);fuzz(L);
    }
    private static void backgroundCoverage() {
        for(int[] size:new int[][]{{320,568},{640,1400}})for(int age:new int[]{0,150,300}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,0,0,0);
            GameCore c=start(store(),156);c.survival.seconds=age;
            java.util.ArrayList<float[]> bodies=new java.util.ArrayList<>();
            java.util.ArrayList<Integer> colors=new java.util.ArrayList<>();
            float[] clip=new float[4];
            Painter painter=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                    new Class<?>[]{Painter.class},(proxy,method,args)-> {
                        if(method.getName().equals("fillRect")) {
                            bodies.add(new float[]{(Float)args[0],(Float)args[1],(Float)args[2],(Float)args[3]});
                            colors.add((Integer)args[4]);
                        }
                        if(method.getName().equals("clipRect"))for(int i=0;i<4;i++)clip[i]=(Float)args[i];
                        return null;
                    });
            boolean covered=true,saturated=true,stable=true,refreshed=true,screenLength=true;
            java.util.ArrayList<Float> firstOrder=null;
            for(float phase:new float[]{0,.001f,19.5f,123.4f,901.2f}) {
                bodies.clear();colors.clear();c.survival.skyPhase=phase;c.survival.scenery(painter,c,l);
                java.util.ArrayList<Float> order=new java.util.ArrayList<>();
                for(float[] b:bodies) {
                    screenLength &= Math.abs((b[3]-b[1])+(b[2]-b[0])-l.h)<.01f;
                    float x=(b[0]+b[2])*.5f;
                    if(order.isEmpty() || x!=order.get(order.size()-1))order.add(x);
                }
                if(firstOrder==null)firstOrder=order;
                else if(phase==.001f)stable &= firstOrder.equals(order);
                else refreshed &= !firstOrder.equals(order);
                for(int row=0;row<=20;row++)for(int column=0;column<=40;column++) {
                    float x=l.w*column/40f,y=l.deckTop*row/20f;
                    boolean found=false;
                    for(float[] b:bodies)if(x>=b[0] && x<=b[2] && y>=b[1] && y<=b[3]) {found=true;break;}
                    covered &= found && x>=clip[0] && y>=clip[1] && x<=clip[2] && y<=clip[3];
                }
                for(int color:colors) {
                    int r=color>>16&255,g=color>>8&255,b=color&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    saturated &= Math.abs((max-min)/(float)max-(.18f+.57f*age/300f))<.02f;
                }
            }
            boolean left=false,right=false;
            for(int i=1;i<firstOrder.size();i++) {
                left |= firstOrder.get(i)<firstOrder.get(i-1);
                right |= firstOrder.get(i)>firstOrder.get(i-1);
            }
            check("band draw order crosses both left and right instead of sweeping columns",left && right);
            check("live stripes keep their horizontal position and depth while scrolling",stable);
            check("incoming stripes refresh horizontal positions after scrolling cycles",refreshed);
            check("stripe length including both round caps is one screen height",screenLength);
            float previous=Float.NaN,minGap=Float.MAX_VALUE,maxGap=0;
            for(float x:new java.util.TreeSet<Float>(firstOrder)) {
                if(!Float.isNaN(previous) && x>previous) {
                    minGap=Math.min(minGap,x-previous);maxGap=Math.max(maxGap,x-previous);
                }
                previous=x;
            }
            check("band spacing has visible clusters and wide gaps",maxGap>minGap*2f);
            check("scrolling bands cover from the screen top through the playfield at "+size[0]+" age "+age,covered);
            check("band saturation follows the muted-to-75-percent ramp",saturated);
        }
    }
    private static void effects(Layout L) {
        GameCore calm=start(store(),155),vivid=start(store(),155);
        vivid.survival.seconds=Survival.RAMP_SECONDS;
        GameCore.Enemy a=add(calm,L,new int[]{0,1,2},L.h*.4f);
        GameCore.Enemy b=add(vivid,L,new int[]{0,1,2},L.h*.4f);
        calm.destroyWord(a,a.baseX,a.y,L);vivid.destroyWord(b,b.baseX,b.y,L);
        check("maximum Survival clears emit ten times the particles",vivid.particles.size()==calm.particles.size()*10);
        check("explosion intensity cannot alter gameplay RNG",vivid.rnd.nextLong()==calm.rnd.nextLong());
        for(int i=0;i<60;i++)vivid.survival.wordBurst(vivid,L.w*.5f,L.h*.5f,L.enemyR,20,Draw.GOLD);
        check("simultaneous clears have a bounded particle budget",vivid.particles.size()<=4200);
        Fx.updateParticles(vivid,1f);
        check("large clear effects expire normally",vivid.particles.isEmpty());
    }
    private static void records(Layout L) {
        Mem m=store();GameCore c=start(m,152);
        c.survival.seconds=12.345;c.score=500;Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("ending early saves time and returns cleanly",c.state==GameCore.TITLE && !c.survival.active && c.survival.bestTime[0]==12345);
        c.preferences.kids=true;c.startGame();c.survival.seconds=30;c.score=800;c.highScores.finish(c);c.toTitle();
        c.preferences.kids=false;c.fullRoster=true;c.startGame();c.survival.seconds=20;c.score=600;c.highScores.finish(c);c.toTitle();
        check("Kids and both normal decks have independent records",c.survival.bestTime[0]==12345
                && c.survival.bestTime[1]==20000 && c.survival.bestTime[2]==30000);
        check("time format is stable",Survival.time(65000).equals("1:05"));
        Survival saved=new Survival();saved.load(c.survival.encode());
        check("all record profiles round trip",saved.encode().equals(c.survival.encode()));
        for(String bad:new String[]{"","future","1;1,2;3,4;-1,8","1;1,2;3,4;5,no"}) {
            saved.load(bad);check("bad Survival record stays empty",saved.bestTime[0]==0 && saved.bestScore[2]==0);
        }
        c.resetHighScores();GameCore reset=new GameCore(m,153,true);
        check("score reset also clears Survival records",reset.survival.bestTime[0]==0 && reset.survival.bestScore[2]==0);
    }
    private static void bounded(Layout L) {
        for(float presses:new float[]{2.5f,5f,8f}) {
            float total=0;int deaths=0;
            for(int seed=0;seed<3;seed++) {
                GameCore c=start(store(),1490+seed);
                Bot bot=new Bot(presses,.18f,.02f,true,1490+seed);
                int frame=0;
                while(c.state==GameCore.PLAY && frame++<60*360) {
                    c.update(DT,L);bot.step(c,L,DT);
                    if(c.state==GameCore.BONUS || c.boss.active() || c.cave.running)break;
                }
                check("bounded Survival has no boss or minigame",c.state!=GameCore.BONUS && !c.boss.active() && !c.cave.running);
                check("bounded Survival keeps rewards isolated",c.collected==store().collected && c.progress.maximum("highest_stage")==0);
                total+=c.survival.seconds;if(c.state==GameCore.OVER)deaths++;
            }
            System.out.printf("    Survival %.1f presses/s: mean %.1fs, %d/3 deaths%n",presses,total/3,deaths);
            check("bounded player gets a playable opening",total/3>15);
        }
    }
    private static void fuzz(Layout L) {
        GameCore c=start(store(),154);java.util.Random random=new java.util.Random(154);
        int deaths=0;boolean sane=true;
        for(int i=0;i<60*180;i++) {
            c.update(DT,L);
            if(c.state==GameCore.OVER) {c.toTitle();c.startGame();deaths++;}
            if(random.nextInt(5)==0)c.tapKey(random.nextInt(Glyph.COUNT),L);
            if(random.nextInt(800)==0) {Pause.open(c);c.update(.1f,L);Pause.resume(c);}
            sane &= c.state==GameCore.PLAY || c.state==GameCore.OVER;
            sane &= !c.boss.active() && !c.pendingBonus && !Double.isNaN(c.survival.seconds);
        }
        check("Survival fuzz survives repeated deaths and pauses",sane && deaths>0);
    }
}
