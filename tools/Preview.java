package com.dddumpling.game;

import java.io.File;

/**
 * Drives {@link GameCore} headlessly to interesting states and renders each one to a PNG
 * through {@link RasterPainter}. This is how the game gets looked at during development
 * without a build/install cycle: same Layout, same Renderer, same state machine as the APK.
 */
final class Preview {
    private static void rainbowFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("151-"))return;
        for(int skin=0;skin<Skins.COUNT;skin++) {
            GameCore loss=TestRainbowCat.fight(L);loss.preferences.skin=skin;loss.lives=1;
            loss.takeHit(loss.boss.cat.x,L);
            for(int frame=0;frame<250;frame++) {
                loss.update(1f/60,L);
                if(frame==50 || frame==125 || frame==205)shot(dir,"151-cat-loss-"+skin+"-"+frame,loss,L,w,h,ss);
            }
        }
        if(wanted("151-cat-loss-audio")) {
            Wav.write(new File(dir,"151-cat-loss-chromatic.wav"),Sfx.build(Sfx.bossTauntId(Boss.RAINBOW_CAT)),Sfx.RATE);
            short[] flurry=new short[(int)(Sfx.RATE*3.1f)];
            for(int note=0;note<12;note++) {
                short[] meow=Sfx.build(Sfx.catFlurryId(note,12));int start=(int)(Sfx.RATE*(note*RainbowCat.SWEEP_STEP+(note/2)*RainbowCat.DOUBLE_PAIR_GAP));
                for(int i=0;i<meow.length && start+i<flurry.length;i++)flurry[start+i]=(short)Math.max(-32767,Math.min(32767,flurry[start+i]+meow[i]*Sfx.CAT_FLURRY_GAIN));
            }
            Wav.write(new File(dir,"151-cat-flurry-whole-tone.wav"),flurry,Sfx.RATE);
        }
        GameCore meowcula=TestRainbowCat.fight(L);meowcula.preferences.skin=Skins.HALLOWEEN;
        for(int i=0;i<90;i++)meowcula.update(1f/60,L);
        shot(dir,"151-meowcula-battle",meowcula,L,w,h,ss);
        RasterPainter vampireSheet=new RasterPainter(w,h,ss);vampireSheet.clear(Draw.BG);
        Painter vampire=Skins.painter(vampireSheet,Skins.HALLOWEEN);
        vampire.text("MEOWCULA",w*.5f,h*.07f,w*.065f,Draw.INK,Painter.CENTER,true);
        for(int row=0;row<3;row++)RainbowCatArt.cat(vampire,w*.5f,h*(.25f+row*.27f),w*.17f,row*.38f,row%2==0?-1:1,null,null,row==2,1);
        Png.write(new File(dir,"151-meowcula-sheet.png"),vampireSheet.resolve(),w,h);
        for(int frame=0;frame<4;frame++) {
            RasterPainter flutter=new RasterPainter(w,h,ss);flutter.clear(Draw.BG);
            Painter cloak=Skins.painter(flutter,Skins.HALLOWEEN);
            RainbowCatArt.cat(cloak,w*.47f,h*.48f,w*.20f,frame*.11f,-1,null,null,false,1);
            Png.write(new File(dir,"151-meowcula-cape-"+frame+".png"),flutter.resolve(),w,h);
        }
        GameCore shooting=TestRainbowCat.fight(L);RainbowCat shooter=shooting.boss.cat;shooter.launchLeft=.32f;
        shooter.x=L.w*.5f;shooter.y=L.h*.40f;shooter.routeDuration=99;shooter.routeTime=0;shooter.turn=-1;
        for(int i=0;i<4;i++) {shooter.route[i*2]=shooter.x;shooter.route[i*2+1]=shooter.y;}
        for(int frame=0;frame<30;frame++) {
            shooting.boss.update(1f/60,L,shooting.rnd);
            if(frame==6 || frame==19 || frame==28)shot(dir,"151-cat-shot-"+frame,shooting,L,w,h,ss);
        }
        GameCore rocket=TestRainbowCat.fight(L);rocket.boss.hp=1;TestRainbowCat.challenge(rocket,L);
        while(rocket.boss.cat.phase==RainbowCat.OPEN)rocket.boss.press(rocket.boss.cat.promptKey,rocket.rnd,L);
        for(int frame=0;frame<RainbowCatRocket.DURATION*60;frame++) {
            rocket.update(1f/60,L);
            if(frame==0 || frame==8 || frame==20 || frame==42 || frame==105 || frame==170 || frame==195 || frame==215 || frame==252 || frame==275 || frame==310 || frame==350 || frame==390 || frame==450)
                shot(dir,"151-cat-rocket-"+frame,rocket,L,w,h,ss);
        }
        GameCore arrival=TestRainbowCat.fight(L);arrival.boss.begin(Boss.RAINBOW_CAT,30,arrival.rnd,true);
        for(int frame=0;frame<330;frame++) {
            arrival.update(1f/60,L);
            if(frame==10 || frame==65 || frame==115 || frame==170 || frame==195 || frame==250 || frame==329)
                shot(dir,"151-cat-arrival-"+frame,arrival,L,w,h,ss);
        }
        GameCore vulnerable=TestRainbowCat.fight(L);TestRainbowCat.challenge(vulnerable,L);
        for(int frame=0;frame<20;frame++)vulnerable.update(1f/60,L);
        shot(dir,"151-paired-keys-struggle",vulnerable,L,w,h,ss);
        vulnerable.tapKey(vulnerable.boss.cat.promptKey,L);
        vulnerable.update(.045f,L);
        shot(dir,"151-paired-keys-hit",vulnerable,L,w,h,ss);
        vulnerable.update(.12f,L);
        shot(dir,"151-paired-keys-recover",vulnerable,L,w,h,ss);
        vulnerable.tapKey(vulnerable.boss.cat.promptKey,L);
        vulnerable.update(.055f,L);
        shot(dir,"151-paired-keys-overlap",vulnerable,L,w,h,ss);
        GameCore doubled=TestRainbowCat.fight(L);doubled.boss.hp=2;TestRainbowCat.challenge(doubled,L);
        while(doubled.boss.cat.phase==RainbowCat.OPEN)doubled.boss.press(doubled.boss.cat.promptKey,doubled.rnd,L);
        for(int frame=0;frame<190;frame++) {
            doubled.boss.update(1f/60,L,doubled.rnd);
            if(frame==20 || frame==140 || frame==185)shot(dir,"151-double-rainbow-"+frame,doubled,L,w,h,ss);
        }
        for(final boolean left:new boolean[]{false,true}) {
            GameCore sweep=TestRainbowCat.fight(L);TestRainbowCat.challenge(sweep,L);RainbowCat dancer=sweep.boss.cat;
            java.util.Random direction=new java.util.Random(151) {public boolean nextBoolean(){return left;}};
            while(dancer.phase==RainbowCat.OPEN)sweep.boss.press(dancer.promptKey,direction,L);
            for(int frame=1;frame<=180;frame++) {
                sweep.update(1f/60,L);
                if(frame==40 || frame==95 || frame==125 || frame==155)
                    shot(dir,"151-escape-sweep-"+(left?"right":"left")+"-"+frame,sweep,L,w,h,ss);
            }
        }
        GameCore freed=TestRainbowCat.fight(L);TestRainbowCat.challenge(freed,L);
        RainbowCat escaping=freed.boss.cat;
        for(int key=0;key<RainbowCat.HITS_PER_BAND && escaping.phase==RainbowCat.OPEN;key++)freed.tapKey(escaping.promptKey,L);
        for(int frame=1;frame<=66;frame++) {
            freed.update(1f/60,L);
            if(frame==5 || frame==12 || frame==28 || frame==55)
                shot(dir,"151-net-escape-"+frame,freed,L,w,h,ss);
        }
        GameCore rays=TestRainbowCat.fight(L);RainbowCat ray=rays.boss.cat;
        for(int frame=0;frame<180 && ray.taps<2;frame++) {
            if(ray.canTap()) {ray.touch(rays,L,0,151,ray.x,ray.y);ray.touch(rays,L,1,151,ray.x,ray.y);}
            rays.boss.update(1f/60,L,rays.rnd);
        }
        rays.boss.update(.18f,L,rays.rnd);
        shot(dir,"151-second-tap-rays",rays,L,w,h,ss);
        for(int capture:new int[]{0,1,4}) {
            GameCore dance=TestRainbowCat.fight(L);RainbowCat dancer=dance.boss.cat;dance.boss.hp=dance.boss.hpMax-capture;
            TestRainbowCat.readyToCircle(dance,L);
            for(int frame=0;frame<75;frame++)dance.boss.update(1f/60,L,dance.rnd);
            shot(dir,"151-shape-dance-"+capture,dance,L,w,h,ss);
            TestRainbowCat.circle(dance,L,1);
            for(int frame=0;frame<15;frame++)dance.boss.update(1f/60,L,dance.rnd);
            shot(dir,"151-shape-burst-"+capture,dance,L,w,h,ss);
        }
        GameCore clear=TestRainbowCat.fight(L);
        clear.boss.cat.launchLeft=0;clear.boss.update(.01f,L,clear.rnd);clear.boss.bt[0]=.4f;
        clear.tapKey(clear.boss.bglyph[0],L);
        Fx.updateParticles(clear,.08f);
        shot(dir,"151-projectile-clear",clear,L,w,h,ss);
        Fx.updateParticles(clear,.16f);
        shot(dir,"151-projectile-clear-spread",clear,L,w,h,ss);
        GameCore c=TestRainbowCat.fight(L);
        for(int i=0;i<60;i++)c.boss.update(1f/60,L,c.rnd);
        shot(dir,"151-chase",c,L,w,h,ss);
        RainbowCat a=c.boss.cat;
        a.touch(c,L,0,151,a.x,a.y);a.touch(c,L,1,151,a.x,a.y);
        c.boss.update(.17f,L,c.rnd);
        shot(dir,"151-tap-hold",c,L,w,h,ss);
        for(int i=0;i<40;i++)c.boss.update(1f/60,L,c.rnd);
        shot(dir,"151-dash",c,L,w,h,ss);
        TestRainbowCat.readyToCircle(c,L);
        shot(dir,"151-circle",c,L,w,h,ss);
        a.touch(c,L,0,151,a.x+a.circleRadius(),a.y);
        for(int i=1;i<=40;i++) {
            float angle=Softbody.TAU*i/40;
            a.touch(c,L,2,151,a.x+(float)Math.cos(angle)*a.circleRadius(),a.y+(float)Math.sin(angle)*a.circleRadius());
            if(a.phase==RainbowCat.CIRCLE)c.boss.update(.025f,L,c.rnd);
            if(i==10)shot(dir,"151-net-windup-start",c,L,w,h,ss);
            if(i==30)shot(dir,"151-net-windup-full",c,L,w,h,ss);
        }
        a.touch(c,L,1,151,a.x+a.circleRadius(),a.y);
        c.boss.update(.08f,L,c.rnd);
        shot(dir,"151-net-swing-start",c,L,w,h,ss);
        c.boss.update(.08f,L,c.rnd);
        shot(dir,"151-net-release",c,L,w,h,ss);
        c.boss.update(.10f,L,c.rnd);
        shot(dir,"151-net-swing-wrap",c,L,w,h,ss);
        c.boss.update(.08f,L,c.rnd);
        shot(dir,"151-net-catch",c,L,w,h,ss);
        c.boss.update(RainbowCat.NET_TIME-.34f,L,c.rnd);
        for(int i=0;i<16;i++)c.boss.update(1f/60,L,c.rnd);
        shot(dir,"151-prompt",c,L,w,h,ss);
        c.boss.update(.12f,L,c.rnd);
        shot(dir,"151-net-struggle",c,L,w,h,ss);
        c.boss.update(.4f,L,c.rnd);
        shot(dir,"151-net-struggle-other",c,L,w,h,ss);
        a.timer=.01f;c.boss.update(.02f,L,c.rnd);c.boss.update(.45f,L,c.rnd);
        shot(dir,"151-flurry",c,L,w,h,ss);
        GameCore starGame=TestRainbowCat.fight(L);RainbowCat starCat=starGame.boss.cat;
        for(int capture=0;capture<4;capture++) {
            TestRainbowCat.challenge(starGame,L);
            for(int i=0;i<RainbowCat.HITS_PER_BAND && starCat.phase==RainbowCat.OPEN;i++)starGame.boss.press(starCat.promptKey,starGame.rnd,L);
        }
        TestRainbowCat.readyToCircle(starGame,L);
        shot(dir,"151-star",starGame,L,w,h,ss);
        starCat.touch(starGame,L,0,151,starCat.x,starCat.y-starCat.circleRadius());
        for(int i=1;i<=48;i++) {
            float along=starCat.gesture.length*i/80f;
            starCat.touch(starGame,L,2,151,starCat.x+starCat.gesture.coordinate(along,0)*starCat.circleRadius(),
                    starCat.y+starCat.gesture.coordinate(along,1)*starCat.circleRadius());
            starGame.boss.update(.025f,L,starGame.rnd);
        }
        shot(dir,"151-star-progress",starGame,L,w,h,ss);
        starCat.touch(starGame,L,2,151,starCat.x,L.playTop-1);
        starGame.boss.update(.08f,L,starGame.rnd);
        shot(dir,"151-trace-lost",starGame,L,w,h,ss);
        GameCore shapes=TestRainbowCat.fight(L);RainbowCat shape=shapes.boss.cat;
        String[] shapeNames={"circle","heart","diamond","eight","star"};
        for(int capture=0;capture<5;capture++) {
            TestRainbowCat.readyToCircle(shapes,L);
            shot(dir,"151-shape-"+shapeNames[capture],shapes,L,w,h,ss);
            TestRainbowCat.challenge(shapes,L);
            for(int key=0;key<RainbowCat.HITS_PER_BAND && shape.phase==RainbowCat.OPEN;key++)shapes.boss.press(shape.promptKey,shapes.rnd,L);
            if(capture==0 || capture==4) {
                for(int frame=0;frame<24;frame++)shapes.boss.update(1f/60,L,shapes.rnd);
                shot(dir,capture==0?"151-damage-flurry":"151-final-flurry",shapes,L,w,h,ss);
            }
        }
        GameCore tap=TestRainbowCat.fight(L);RainbowCat tapped=tap.boss.cat;
        tapped.touch(tap,L,0,151,tapped.x,tapped.y);tapped.touch(tap,L,1,151,tapped.x,tapped.y);
        for(int band=0;band<6;band++) {
            for(int frame=0;frame<6;frame++)tap.boss.update(1f/60,L,tap.rnd);
            shot(dir,"151-tap-flash-"+band,tap,L,w,h,ss);
        }
        for(int frame=0;frame<10;frame++)tap.boss.update(1f/60,L,tap.rnd);
        shot(dir,"151-tap-scared",tap,L,w,h,ss);
        for(int frame=0;frame<38;frame++)tap.boss.update(1f/60,L,tap.rnd);
        shot(dir,"151-tap-shake-off",tap,L,w,h,ss);
        for(int frame=0;frame<30;frame++)tap.boss.update(1f/60,L,tap.rnd);
        shot(dir,"151-tap-recovered",tap,L,w,h,ss);
        GameCore sky=TestRainbowCat.fight(L);sky.devInfiniteLives=true;
        shot(dir,"151-battle-sky-0",sky,L,w,h,ss);
        for(int frame=0;frame<120;frame++)sky.update(1f/60,L);
        shot(dir,"151-battle-sky-2s",sky,L,w,h,ss);
        GameCore roam=TestRainbowCat.fight(L);
        for(int side=0;side<4;side++) {
            RainbowCat runner=roam.boss.cat;
            for(int frame=0;frame<2400;frame++) {
                roam.boss.update(1f/60,L,roam.rnd);
                boolean edge=side==0?runner.x<runner.left+(runner.right-runner.left)*.1f
                        :side==1?runner.x>runner.right-(runner.right-runner.left)*.1f
                        :side==2?runner.y<runner.top+(runner.bottom-runner.top)*.1f
                        :runner.y>runner.bottom-(runner.bottom-runner.top)*.1f;
                if(edge)break;
            }
            shot(dir,"151-roam-edge-"+side,roam,L,w,h,ss);
        }
        GameCore help=TestRainbowCat.fight(L);
        TestOnboarding.touch(help,L,0,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        TestOnboarding.touch(help,L,1,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
        for(int message=TutorialSpeech.CAT_RUSH;message<=TutorialSpeech.CAT_PROMPT;message++) {
            TestOnboarding.awaitBubble(help,L);
            shot(dir,"151-help-"+message,help,L,w,h,ss);
            TestOnboarding.acknowledge(help,L);
        }
        c.boss.damage(c.boss.hp);
        c.boss.update(.5f,L,c.rnd);
        shot(dir,"151-defeat",c,L,w,h,ss);
        GameCore field=TestFastEnemies.scene(L,26,151);field.clock=3;
        GameCore.Enemy normal=Check.add(field,L,new int[]{0,1,4},L.h*.34f);normal.enterT=1;
        GameCore.Enemy rush=TestFastEnemies.runner(field,L,L.h*.53f);rush.baseX=L.w*.25f;
        GameCore.Enemy dive=Check.add(field,L,new int[]{2,3},L.h*.68f);dive.baseX=L.w*.65f;
        dive.enterT=1;dive.swampKind=SwampEnemies.DIVE;dive.diving=true;
        shot(dir,"151-enemies",field,L,w,h,ss);
        field.skyClock=8;shot(dir,"151-bands-scrolling",field,L,w,h,ss);field.skyClock=0;
        for(GameCore.Enemy e:field.enemies) {
            field.destroyWord(e,field.enemyCentreX(e),e.y,L);e.destroyT=.15f;
            for(int key=0;key<e.gone.length;key++)if(e.gone[key])e.goneT[key]=.15f*3.2f;
        }
        field.clock+=.15f;Fx.updateParticles(field,.15f);
        shot(dir,"151-clear-bursts",field,L,w,h,ss);
        GameCore title=new GameCore(TestTimeAttack.store(),151,true);
        title.collected=Collect.MASK;title.landSuppressed=0;title.landChoice=Lands.RAINBOW;
        title.landDiscovery=-1;title.clock=3;title.landSeen=LandPicker.stateMask();
        shot(dir,"151-title",title,L,w,h,ss);
        RasterPainter sheet=new RasterPainter(w,h,ss);sheet.clear(Draw.BG);
        sheet.text("RAINBOW CAT",w*.5f,h*.07f,w*.06f,Draw.INK,Painter.CENTER,true);
        for(int row=0;row<4;row++) {
            float x=w*.5f,y=h*(.22f+row*.20f),r=w*.16f;
            RainbowCatArt.cat(sheet,x,y,r,row*.52f,row%2==0?1:-1,null,null,row==2,1);
        }
        Png.write(new File(dir,"151-cat-sheet.png"),sheet.resolve(),w,h);
    }
    private static void skinFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("200-"))return;
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore names=TestTimeAttack.start(boss);names.preferences.skin=Skins.HALLOWEEN;
            names.toTitle();names.returnFade=0;
            if(boss==Boss.OCTOPUS)names.clock=.45f;
            shot(dir,"200-name-title"+(boss==0?"":"-"+boss),names,L,w,h,ss);
            names.startGame();names.timeAttack.versus.update(names,.01f,L);names.timeAttack.versus.time=1.4f;
            shot(dir,"200-name-versus"+(boss==0?"":"-"+boss),names,L,w,h,ss);
            names.timeAttack.versus.time=TimeAttackVersus.DURATION;names.boss.intro=Boss.INTRO*.5f;
            shot(dir,"200-name-arrival"+(boss==0?"":"-"+boss),names,L,w,h,ss);
            names.boss.intro=0;
            shot(dir,"200-name-battle"+(boss==0?"":"-"+boss),names,L,w,h,ss);
        }
        RasterPainter raster=new RasterPainter(w,h,ss);raster.clear(Draw.BG);
        raster.text("HALLOWEEN KEYS",w*.5f,h*.055f,w*.055f,Draw.INK,Painter.CENTER,true);
        for(int g=0;g<6;g++) {
            float y=h*(.17f+g*.145f),r=w*.08f;
            Kawaii.draw(raster,g,w*.19f,y,r,Glyph.COLOR[g],1,.4f);
            Painter p=Skins.painter(raster,Skins.HALLOWEEN);
            Kawaii.draw(p,g,w*.47f,y,r*1.15f,Skins.keyColor(p,g),1,.4f);
            Kawaii.crying(p,g,w*.72f,y,r*.8f,Skins.keyColor(p,g),1,1,1);
            raster.text(Skins.HALLOWEEN_NAMES[g],w*.48f,y+r*1.50f,w*.026f,Draw.INK,Painter.CENTER,true);
        }
        Png.write(new File(dir,"200-keys-sheet.png"),raster.resolve(),w,h);
        GameCore settings=new GameCore(new Check.Mem(),200L);settings.preferences.skin=Skins.HALLOWEEN;
        PlayerSettings.open(settings);step(settings,L,.4f);
        shot(dir,"200-settings",settings,L,w,h,ss);
        settings.preferences.skinsOpen=true;shot(dir,"200-gallery",settings,L,w,h,ss);
        for(boolean full:new boolean[]{false,true}) {
            GameCore words=TestSwampEnemies.scene(L,200L);words.preferences.skin=Skins.HALLOWEEN;words.runFullRoster=full;
            words.enemies.clear();
            for(int row=0;row<3;row++) {
                GameCore.Enemy e=TestSwampEnemies.word(words,L,row==1?SwampEnemies.FOG:0,L.h*(.30f+row*.18f));
                e.word=full?new int[]{row*2,(row*2+1)%6}:new int[]{Roster.STARTERS[row],Roster.STARTERS[(row+1)%4],Roster.STARTERS[(row+2)%4]};
                e.gone=new boolean[e.word.length];e.goneT=new float[e.word.length];
            }
            shot(dir,"200-words-"+(full?6:4),words,L,w,h,ss);
        }
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore c=TestBoss.enterBoss(L,boss,200L);c.preferences.skin=Skins.HALLOWEEN;c.stageBanner=0;
            shot(dir,"200-boss-"+boss,c,L,w,h,ss);
        }
        RasterPainter bosses=new RasterPainter(w,h,ss);bosses.clear(Draw.BG);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            float x=w*(boss%2==0?.26f:.74f),y=h*(.19f+(boss/2)*.30f),r=w*.19f;
            GameCore c=TestBoss.enterBoss(L,boss,200L);c.preferences.skin=Skins.HALLOWEEN;
            if(boss==Boss.OCTOPUS)c.boss.poseOctopusPortrait(.08f,L);
            Painter p=Skins.painter(bosses,Skins.HALLOWEEN);
            BossScreen.versusPortrait(p,c,L,c.boss,x,y,r,2);
            p.text(Skins.bossName(Skins.HALLOWEEN,boss),x,y+r*1.7f,w*.027f,Draw.INK,Painter.CENTER,true);
        }
        Png.write(new File(dir,"200-boss-sheet.png"),bosses.resolve(),w,h);
    }
    private static void slimeOpeningFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("193-slime-opening-"))return;
        for(float time:new float[]{0,.15f,.45f,.85f,1.1f}) {
            GameCore c=new GameCore(new Check.Mem(),193L);c.startGame();c.stage=5;c.stageBanner=0;
            c.boss.begin(Boss.SLIME,5,c.rnd);c.boss.intro=0;c.update(0,L);
            step(c,L,time);
            shot(dir,"193-slime-opening-"+Math.round(time*1000),c,L,w,h,ss);
        }
    }
    private static void slimeDeathFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("193-slime-death-"))return;
        for(int shape=0;shape<3;shape++)for(float time:new float[]{0,.3f,.8f,1.4f,2.1f,2.8f,3.4f}) {
            boolean drag=shape==1;
            GameCore c=drag?TestBoss.slimeAfterDrag(L,0f):TestBoss.enterBoss(L,Boss.SLIME,193);
            c.stageBanner=0;
            if(shape==2)c.boss.body.reset(L.w*.5f,L.h*.3f,L.enemyR*2,.15f);
            if(!drag)c.boss.damage(c.boss.hp);
            step(c,L,time);
            shot(dir,"193-slime-death-"+(drag?"drag-":shape==2?"skinny-":"")+Math.round(time*1000),c,L,w,h,ss);
        }
    }

    private static void slimeSprayFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("193-goo-"))return;
        for(boolean drag:new boolean[]{false,true})for(float time:new float[]{.18f,.48f,.56f,.95f,1.35f}) {
            GameCore c=TestBoss.enterBoss(L,Boss.SLIME,193);
            for(int i=0;i<600 && !c.boss.open();i++)c.update(DT,L);
            java.util.Arrays.fill(c.boss.blive,false);c.stageBanner=0;
            for(int i=0;i<(drag?Boss.SPLIT_HITS:1);i++)c.tapKey(c.boss.chainLetter(),L);
            c.update(GameCore.SHOT_TIME,L);
            if(drag) {
                c.particles.clear();
                int glob=-1;for(int i=0;i<Boss.ELEMS;i++)if(c.boss.etype[i]==Boss.E_GLOB)glob=i;
                c.grabBoss(c.boss.ex[glob],c.boss.ey[glob]);
                c.dragBoss(L.playLeft+c.boss.er[glob]*1.5f,c.boss.body.centreY(),L);
            }
            step(c,L,time);
            shot(dir,"193-goo-"+(drag?"drag-":"shot-")+Math.round(time*1000),c,L,w,h,ss);
        }
    }

    private static void bossImpactFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("193-impact-"))return;
        for(int kind:new int[]{Boss.SLIME,Boss.SPLITTER})for(float time:new float[]{0,.067f,.134f,.20f,.34f,.65f}) {
            GameCore c=TestBoss.enterBoss(L,kind,193);
            for(int i=0;i<600 && !c.boss.open();i++)c.update(DT,L);
            java.util.Arrays.fill(c.boss.blive,false);c.shots.clear();c.particles.clear();
            c.stageBanner=c.shake=c.flash=c.skyGlow=c.boss.hurt=c.boss.slimeKeyPulse=0;
            c.tapKey(kind==Boss.SLIME?c.boss.chainLetter():c.boss.pieceWant(0),L);
            step(c,L,time);
            shot(dir,"193-impact-"+(kind==Boss.SLIME?"slime-":"divide-")+Math.round(time*1000),c,L,w,h,ss);
        }
    }
    private static void clearSpreadFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("clear-spread-"))return;
        for(int length:new int[]{1,3,7}) {
            GameCore c=TestSwampEnemies.scene(L,149);c.clock=2;
            int[] word=new int[length];for(int i=0;i<length;i++)word[i]=i%6;
            GameCore.Enemy e=Check.add(c,L,word,L.h*.44f);e.baseX=L.w*.5f;
            c.destroyWord(e,c.enemyCentreX(e),e.y,L);e.destroyT=.2f;
            for(int i=0;i<12;i++)Fx.updateParticles(c,1f/60);
            shot(dir,"clear-spread-"+length,c,L,w,h,ss);
        }
    }
    private static void clearIntensityFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("clear-intensity-"))return;
        for(int age:new int[]{-1,0,300}) {
            GameCore c=age<0?TestSwampEnemies.scene(L,149):TestSurvival.start(TestSurvival.store(),149);
            c.survival.seconds=Math.max(0,age);c.clock=2;
            GameCore.Enemy e=Check.add(c,L,new int[]{0,1,2},L.h*.44f);e.baseX=L.w*.5f;
            c.destroyWord(e,c.enemyCentreX(e),e.y,L);e.destroyT=.12f;
            Fx.updateParticles(c,.12f);
            shot(dir,"clear-intensity-"+(age<0?"adventure":age),c,L,w,h,ss);
        }
    }
    private static void diveStarFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("dive-star-"))return;
        for(int state=0;state<4;state++) {
            GameCore c=TestSwampEnemies.scene(L,176);c.clock=2;
            GameCore.Enemy e=TestSwampEnemies.word(c,L,SwampEnemies.DIVE,L.h*.44f);e.sway=0;
            if(state==1) {c.tapKey(0,L);c.tapKey(1,L);step(c,L,.12f);}
            if(state==2) {c.destroyWord(e,c.tileX(e,2,L),e.y,L);e.destroyT=.12f;}
            if(state==3) {c.removeTile(e,2,0,-1,L);e.goneT[2]=.3f;}
            shot(dir,"dive-star-"+state,c,L,w,h,ss);
        }
    }
    private static void enemyClearFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("enemy-clear-"))return;
        for(boolean fog:new boolean[]{false,true})for(float time:new float[]{0,.08f,.18f,.30f}) {
            GameCore c=TestSwampEnemies.scene(L,180);c.clock=2;
            GameCore.Enemy e=fog?TestSwampEnemies.word(c,L,SwampEnemies.FOG,L.h*.44f)
                    :TestFastEnemies.runner(c,L,L.h*.44f);
            e.baseX=L.w*.5f;e.sway=0;e.word[0]=2;
            c.destroyWord(e,c.enemyCentreX(e),e.y,L);
            e.destroyT=time;
            shot(dir,"enemy-clear-"+(fog?"fog-":"star-")+Math.round(time*100),c,L,w,h,ss);
        }
    }
    private static void fastEnemyFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("180-"))return;
        for(float time:new float[]{0,.3f,.8f}) {
            GameCore c=TestFastEnemies.scene(L,17,180);c.clock=2;
            GameCore.Enemy ordinary=Check.add(c,L,new int[]{0,1,4},new int[]{2,1,2},L.h*.44f);
            ordinary.baseX=L.w*.35f;ordinary.enterT=1;
            ordinary.speed=(L.dangerY+L.enemyR*2.2f)/c.travelSeconds();
            GameCore.Enemy runner=TestFastEnemies.runner(c,L,L.h*.32f);runner.baseX=L.w*.80f;
            step(c,L,time);
            shot(dir,"180-fast-single-key-"+Math.round(time*100),c,L,w,h,ss);
        }
        if(wanted("180-shooting-star-side")) {
            GameCore c=TestFastEnemies.scene(L,21,180);c.clock=2;
            GameCore.Enemy e=TestFastEnemies.runner(c,L,L.h*.48f);e.word[0]=2;
            e.sideEntry=true;e.pathStartY=L.h*.44f;e.pathStartX=-L.enemyR*2;e.pathEndX=L.w*.7f;
            e.baseX=EnemyEntry.xAt(e,e.y,L);e.sway=0;
            shot(dir,"180-shooting-star-side",c,L,w,h,ss);
        }
        for(int mode:new int[]{Power.FLURRY,Power.NINJA,Power.TEAM}) {
            GameCore c=TestSwampEnemies.scene(L,180);c.clock=2;c.collected=1L;c.runWho=0;
            c.playtestMode(mode,L);c.flash=c.skyGlow=c.shake=0;c.spawnTimer=Float.POSITIVE_INFINITY;
            TestSwampEnemies.word(c,L,SwampEnemies.FOG,L.h*.35f);
            TestSwampEnemies.word(c,L,SwampEnemies.DIVE,L.h*.51f);
            GameCore.Enemy runner=TestFastEnemies.runner(c,L,L.h*.63f);runner.baseX=L.w*.72f;
            shot(dir,"180-power-mix-"+mode,c,L,w,h,ss);
        }
    }
    private static void swampEnemyFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("175-176-"))return;
        for(int beat=0;beat<3;beat++) {
            GameCore c=TestSwampEnemies.scene(L,175);
            GameCore.Enemy fog=TestSwampEnemies.word(c,L,SwampEnemies.FOG,L.h*.32f);
            GameCore.Enemy dive=TestSwampEnemies.word(c,L,SwampEnemies.DIVE,L.h*.54f);
            TestSwampEnemies.word(c,L,SwampEnemies.NONE,L.h*.68f);
            if(beat>0) {
                c.target=fog;c.tapKey(0,L);
                c.target=dive;c.tapKey(0,L);c.tapKey(1,L);
                if(beat==2)step(c,L,.75f);
                else step(c,L,.15f);
            }
            shot(dir,"175-176-swamp-words-"+beat,c,L,w,h,ss);
        }
        for(int cleared:new int[]{0,1,3,4}) {
            GameCore c=TestSwampEnemies.scene(L,175);c.clock=2;
            GameCore.Enemy fog=Check.add(c,L,new int[]{0,1,4,2,3},L.h*.45f);
            fog.swampKind=SwampEnemies.FOG;fog.enterT=1;fog.speed=0;
            for(int i=0;i<cleared;i++)c.tapKey(fog.word[i],L);
            step(c,L,.16f);
            shot(dir,"175-176-fog-recede-"+cleared,c,L,w,h,ss);
            if(cleared==3) {
                c.tapKey(5,L);
                shot(dir,"175-176-fog-after-miss",c,L,w,h,ss);
            }
        }
        for(float time:new float[]{.035f,.28f,.65f}) {
            GameCore c=TestSwampEnemies.scene(L,176);
            GameCore.Enemy dive=TestSwampEnemies.word(c,L,SwampEnemies.DIVE,L.h*.43f);
            c.tapKey(0,L);c.tapKey(1,L);step(c,L,time);
            shot(dir,"175-176-dive-launch-"+Math.round(time*1000),c,L,w,h,ss);
        }
    }

    private static void alligatorFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(wanted("gator-growl-"))for(int phase=0;phase<2;phase++)for(int beat=0;beat<3;beat++) {
            GameCore c=TestAlligator.fight(L);c.stageBanner=0;c.landBlend=1;
            c.boss.hp=c.boss.hpMax*(phase==0?.5f:.25f);c.boss.damage(1);
            float at=GatorTauntRecording.onset(phase,beat)+.09f;
            TestAlligator.advance(c,L,at);c.clock=at;
            shot(dir,"gator-growl-"+(phase+1)+"-"+(beat+1),c,L,w,h,ss);
        }
        if(wantedGroup("gator-final-")) {
            for(float at:new float[]{.25f,.7f,1.2f,1.65f,2.05f,2.5f,3.3f}) {
                GameCore ending=TestAlligator.fight(L);ending.stageBanner=0;ending.landBlend=1;
                ending.boss.damage(ending.boss.hp);TestAlligator.advance(ending,L,at);ending.clock=at;
                shot(dir,"gator-final-defeat-"+Math.round(at*100),ending,L,w,h,ss);
            }
            for(int side:new int[]{-1,1}) {
                GameCore win=TestAlligator.fight(L);win.stageBanner=0;win.landBlend=1;win.lives=1;
                win.boss.alligator.tailPosition=win.boss.alligator.tailSide=side;
                win.takeHit(L.w*.5f,L);
                for(float at:new float[]{.4f,.7f,1f,1.4f}) {
                    win.clock=at;win.deathT=win.deathDuration()-at;
                    shot(dir,"gator-final-taunt-"+(side<0?"left-":"")+Math.round(at*100),win,L,w,h,ss);
                }
            }
            GameCore flies=TestAlligator.fight(L);flies.stageBanner=0;flies.landBlend=1;
            for(float at:new float[]{0f,1.2f,2.4f}) {
                flies.clock=at;shot(dir,"gator-final-fireflies-"+Math.round(at*100),flies,L,w,h,ss);
            }
        }
        for(float at:new float[]{.08f,.16f,.22f,.30f,.41f,.66f,.90f,1.70f}) {
            GameCore parry=TestAlligator.fight(L);parry.stageBanner=0;parry.landBlend=1;parry.companion.begin(6);
            TestAlligator.block(parry,L);TestAlligator.advance(parry,L,at);
            shot(dir,"168-companion-parry-"+Math.round(at*100),parry,L,w,h,ss);
        }
        for(float at:new float[]{.16f,.66f,1.5f}) {
            GameCore props=TestAlligator.fight(L);props.stageBanner=0;props.landBlend=1;props.companion.begin(6);
            TestAlligator.block(props,L);TestAlligator.advance(props,L,at);
            props.companion.react(RunCompanion.BOSS_HIT,1);
            shot(dir,"168-companion-props-"+Math.round(at*100),props,L,w,h,ss);
        }
        GameCore dazed=TestAlligator.fight(L);dazed.stageBanner=0;dazed.landBlend=1;dazed.companion.begin(6);
        for(int limb=0;limb<3;limb++)for(float at:new float[]{.16f,.23f,.55f}) {
            GameCore recoil=TestAlligator.fight(L);recoil.stageBanner=0;recoil.landBlend=1;recoil.companion.begin(6);
            recoil.boss.alligator.next=limb;recoil.boss.alligator.timer=0;
            TestAlligator.block(recoil,L);TestAlligator.advance(recoil,L,at);
            shot(dir,"168-limb-recoil-"+limb+"-"+Math.round(at*100),recoil,L,w,h,ss);
        }
        TestAlligator.longOpening(dazed,L);TestAlligator.advance(dazed,L,.35f);
        shot(dir,"168-companion-daze-impact",dazed,L,w,h,ss);
        for(int who:new int[]{Collect.DUCK_FIRST,Collect.ALLIGATOR_FRIEND}) {
            GameCore fighter=TestAlligator.fight(L);fighter.stageBanner=0;fighter.landBlend=1;fighter.companion.begin(who);
            TestAlligator.block(fighter,L);TestAlligator.advance(fighter,L,.40f);
            shot(dir,"168-companion-fighting-"+who,fighter,L,w,h,ss);
        }
        GameCore crossing=TestAlligator.fight(L);crossing.stageBanner=0;crossing.landBlend=1;crossing.companion.begin(6);
        crossing.boss.alligator.next=0;TestAlligator.block(crossing,L);
        crossing.boss.alligator.next=1;TestAlligator.block(crossing,L);TestAlligator.advance(crossing,L,.08f);
        shot(dir,"168-companion-parry-crossing",crossing,L,w,h,ss);
        for(int phase=0;phase<3;phase++) {
            GameCore warm=TestAlligator.fight(L);warm.stageBanner=0;warm.landBlend=1;
            warm.boss.hp=warm.boss.hpMax*(phase==0?1:phase==1?.49f:.24f);
            warm.boss.alligator.timer=10;TestAlligator.advance(warm,L,.3f);
            shot(dir,"168-gator-phase-color-"+phase,warm,L,w,h,ss);
        }
        for(float at:new float[]{.04f,.32f,.62f,.94f}) {
            GameCore flex=TestAlligator.fight(L);flex.stageBanner=0;flex.landBlend=1;
            flex.boss.alligator.daze=Alligator.DAZE;flex.boss.alligator.longWindow=true;
            flex.boss.hp=flex.boss.hpMax*.5f;flex.boss.damage(1);TestAlligator.advance(flex,L,at);
            shot(dir,"168-gator-cancel-flex-"+Math.round(at*100),flex,L,w,h,ss);
        }
        for(int count:new int[]{2,3})for(int beat=0;beat<count;beat++) {
            GameCore demo=TestAlligator.fight(L);demo.stageBanner=0;demo.landBlend=1;
            demo.boss.hp=demo.boss.hpMax*(count==2?.5f:.25f);demo.boss.damage(1);
            for(float t=0;t<demo.boss.alligator.showoffLead()+Alligator.SHOWOFF_BEAT*(beat+.5f);t+=DT)
                demo.update(DT,L);
            shot(dir,"168-combo-showoff-"+count+"-"+(beat+1),demo,L,w,h,ss);
        }
        for(float at:new float[]{0,.15f,.35f,.55f,.75f,1}) {
            GameCore entry=TestAlligator.fight(L);entry.stageBanner=0;entry.landBlend=1;
            entry.boss.begin(Boss.ALLIGATOR,entry.stage,entry.rnd,entry.playRosterFull());
            TestAlligator.advance(entry,L,Math.max(DT,entry.boss.introDuration()*at));
            shot(dir,"168-gator-rise-"+Math.round(at*100),entry,L,w,h,ss);
        }
        for(float at:new float[]{.12f,.38f,.70f}) {
            GameCore laugh=TestAlligator.fight(L);laugh.stageBanner=0;laugh.landBlend=1;
            laugh.boss.alligator.timer=10;laugh.boss.alligator.playerDamaged();
            TestAlligator.advance(laugh,L,at);
            shot(dir,"168-gator-laugh-"+Math.round(at*100),laugh,L,w,h,ss);
        }
        for(int side:new int[]{-1,1})for(float at:new float[]{0,.9f,1.3f}) {
            GameCore tail=TestAlligator.fight(L);tail.stageBanner=0;tail.landBlend=1;
            Alligator a=tail.boss.alligator;a.tailPosition=a.tailSide=side;a.timer=100;
            if(at>0) {a.next=2;a.timer=0;TestAlligator.advance(tail,L,at);}
            else TestAlligator.advance(tail,L,.3f);
            shot(dir,"168-tail-detail-"+(side<0?"left":"right")+"-"+Math.round(at*100),tail,L,w,h,ss);
        }
        for(int limb=0;limb<3;limb++)for(int phase=0;phase<2;phase++) {
            GameCore cue=TestAlligator.fight(L);cue.stageBanner=0;cue.landBlend=1;
            Alligator a=cue.boss.alligator;a.next=limb;a.timer=0;
            TestAlligator.advance(cue,L,.7f);cue.boss.age=phase==0?.1f:.3f;
            shot(dir,"168-attack-glow-"+limb+"-"+phase,cue,L,w,h,ss);
        }
        for(int side:new int[]{-1,1}) {
            GameCore curved=TestAlligator.fight(L);curved.stageBanner=0;curved.landBlend=1;
            curved.boss.alligator.timer=100;curved.boss.alligator.tailSide=side;
            TestAlligator.advance(curved,L,1.5f);
            shot(dir,"168-gator-body-curve-"+(side<0?"left":"right"),curved,L,w,h,ss);
        }
        for(boolean longer:new boolean[]{false,true}) {
            GameCore opening=TestAlligator.fight(L);opening.stageBanner=0;opening.landBlend=1;
            if(longer)TestAlligator.longOpening(opening,L);
            else TestAlligator.block(opening,L);
            TestAlligator.advance(opening,L,longer?.45f:.24f);
            shot(dir,"168-gator-vulnerable-"+(longer?"long":"short"),opening,L,w,h,ss);
        }
        for(int count:new int[]{2,3})for(float at:new float[]{.48f,.72f,.95f}) {
            GameCore combo=TestAlligator.fight(L);combo.stageBanner=0;combo.landBlend=1;
            combo.boss.hp=combo.boss.hpMax*(count==2?.49f:.24f);combo.boss.alligator.timer=0;
            TestAlligator.advance(combo,L,at);
            shot(dir,"168-gator-combo-"+count+"-"+Math.round(at*100),combo,L,w,h,ss);
        }
        for(float at:new float[]{.08f,.65f,.91f}) {
            GameCore opening=TestAlligator.fight(L);opening.stageBanner=0;opening.landBlend=1;
            TestAlligator.block(opening,L);opening.boss.press(0,opening.rnd,L);
            TestAlligator.advance(opening,L,at);
            shot(dir,"168-gator-short-window-"+Math.round(at*100),opening,L,w,h,ss);
        }
        for(float at:new float[]{.10f,.31f,.49f,.62f}) {
            GameCore counter=TestAlligator.fight(L);counter.stageBanner=0;counter.landBlend=1;
            counter.boss.press(0,counter.rnd,L);TestAlligator.advance(counter,L,at);
            shot(dir,"168-gator-counter-"+Math.round(at*100),counter,L,w,h,ss);
        }
        for(float at:new float[]{.12f,.34f,.56f,.78f,.94f}) {
            GameCore entry=TestAlligator.fight(L);entry.stageBanner=0;entry.landBlend=1;
            entry.boss.begin(Boss.ALLIGATOR,entry.stage,entry.rnd,entry.playRosterFull());
            TestAlligator.advance(entry,L,entry.boss.introDuration()*at);
            shot(dir,"168-gator-water-entry-"+Math.round(at*100),entry,L,w,h,ss);
        }
        for(int frame=0;frame<4;frame++) {
            GameCore eyes=TestAlligator.fight(L);eyes.stageBanner=0;eyes.landBlend=1;
            eyes.boss.alligator.daze=Alligator.DAZE;eyes.boss.alligator.longWindow=true;TestAlligator.advance(eyes,L,.30f);
            eyes.boss.damage(1);TestAlligator.advance(eyes,L,new float[]{0,.05f,.14f,.48f}[frame]);
            shot(dir,"168-gator-eye-morph-"+frame,eyes,L,w,h,ss);
        }
        for(int limb=0;limb<3;limb++) {
            GameCore eyes=TestAlligator.fight(L);eyes.stageBanner=0;eyes.landBlend=1;
            eyes.boss.alligator.next=limb;eyes.boss.alligator.timer=0;eyes.boss.alligator.dazes=1;
            TestAlligator.advance(eyes,L,.20f);
            shot(dir,"168-gator-eye-tell-"+limb,eyes,L,w,h,ss);
        }
        for(int frame=0;frame<2;frame++) {
            GameCore hurt=TestAlligator.fight(L);hurt.stageBanner=0;hurt.landBlend=1;
            hurt.boss.alligator.daze=Alligator.DAZE;hurt.boss.alligator.longWindow=true;hurt.boss.alligator.roundStreak=Alligator.ROUNDS_FOR_DAZE;
            hurt.boss.damage(1);TestAlligator.advance(hurt,L,frame==0?.03f:.12f);
            shot(dir,"168-gator-red-flash-"+frame,hurt,L,w,h,ss);
        }
        for(int n=0;n<3;n++) {
            GameCore idle=TestAlligator.fight(L);idle.stageBanner=0;idle.landBlend=1;
            idle.boss.alligator.timer=10;TestAlligator.advance(idle,L,n*1.7f);
            shot(dir,"168-gator-idle-"+n,idle,L,w,h,ss);
        }
        GameCore motion=TestAlligator.fight(L);motion.stageBanner=0;motion.landBlend=1;
        for(int n=0;n<Alligator.ROUNDS_FOR_DAZE-1;n++)TestAlligator.exchange(motion,L);
        TestAlligator.advance(motion,L,Alligator.OPEN+.04f);
        motion.boss.alligator.next=2;motion.boss.alligator.tailPosition=motion.boss.alligator.tailSide=1;
        TestAlligator.exchange(motion,L);
        TestAlligator.advance(motion,L,motion.boss.alligator.recoil);
        for(int n=0;n<3;n++) {
            TestAlligator.advance(motion,L,.20f);
            shot(dir,"168-gator-tail-crossing-"+n,motion,L,w,h,ss);
        }
        TestAlligator.advance(motion,L,Alligator.DAZE);TestAlligator.ready(motion,L);
        shot(dir,"168-gator-limb-tell",motion,L,w,h,ss);
        GameCore c=TestAlligator.fight(L);c.stageBanner=0;c.landBlend=1;c.landSeen=LandPicker.stateMask();
        c.boss.alligator.next=0;TestAlligator.ready(c,L);
        shot(dir,"168-alligator-arm",c,L,w,h,ss);
        TestAlligator.block(c,L);TestAlligator.advance(c,L,Alligator.RETURN+.04f);
        c.boss.alligator.next=2;TestAlligator.ready(c,L);
        TestAlligator.advance(c,L,.60f);
        shot(dir,"168-alligator-tail",c,L,w,h,ss);
        TestAlligator.block(c,L);TestAlligator.advance(c,L,Alligator.RETURN+.90f);
        c.boss.alligator.limb=-1;c.boss.alligator.placed=false;c.boss.alligator.blocked=false;
        c.boss.alligator.next=2;c.boss.alligator.tailPosition=c.boss.alligator.tailSide=-1;
        TestAlligator.ready(c,L);TestAlligator.advance(c,L,.60f);
        shot(dir,"168-alligator-tail-left",c,L,w,h,ss);
        TestAlligator.longOpening(c,L);
        shot(dir,"168-alligator-dazed",c,L,w,h,ss);
        TestAlligator.advance(c,L,.18f);shot(dir,"168-gator-daze-wriggle",c,L,w,h,ss);
        c.boss.press(0,c.rnd,L);TestAlligator.advance(c,L,.22f);
        shot(dir,"168-alligator-tummy-hit",c,L,w,h,ss);
        c.boss.damage(c.boss.hp);TestAlligator.advance(c,L,1.3f);
        shot(dir,"168-alligator-defeated",c,L,w,h,ss);
        c.boss.leave();c.toTitle();c.returnFade=0;c.landChoice=Lands.SWAMP;c.caseIndex=Collect.ALLIGATOR_FRIEND;
        shot(dir,"168-swamp-title",c,L,w,h,ss);
        c=TestAlligator.fight(L);c.stageBanner=0;c.landBlend=1;TestAlligator.ready(c,L);
        float sy=L.playTop+(L.dangerY-L.playTop)*.65f;
        Alligator a=c.boss.alligator;
        a.touch(c,L,0,168,L.w*.30f,sy);
        for(int n=1;n<=8;n++)a.touch(c,L,2,168,L.w*(.30f+n*.055f),sy-L.w*.12f*(float)Math.sin(n*.25f));
        TestAlligator.advance(c,L,.08f);shot(dir,"168-guard-swipe",c,L,w,h,ss);
        for(int who:new int[]{6,7}) {
            c=TestAlligator.fight(L);c.stageBanner=0;c.landBlend=1;c.runWho=who;c.companion.begin(who);
            TestAlligator.block(c,L);TestAlligator.advance(c,L,.10f);
            shot(dir,who==6?"168-guard-companion-block":"168-guard-yellow-block",c,L,w,h,ss);
        }

    }

    private static void gatorCompanionFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("168-gator-companion"))return;
        GameCore c=TestAlligator.fight(L);c.stageBanner=0;c.landBlend=1;
        c.runWho=Collect.ALLIGATOR_FRIEND;c.companion.begin(c.runWho);
        for(int n=0;n<36;n++){c.companion.update(c,DT);c.gator.update(c,DT);}
        shot(dir,"168-gator-companion-play",c,L,w,h,ss);
        RasterPainter raster=new RasterPainter(w,h,ss);raster.clear(Draw.BG);
        Painter p=new OpacityPainter(raster,1) {public GatorCompanion gator(){return c.gator;}};
        p.text(Collect.NAME[Collect.ALLIGATOR_FRIEND],w*.5f,h*.065f,w*.055f,Draw.INK,Painter.CENTER,true);
        String[] labels={"HELLO!","BOING!","YAY!","NINJA","OUCH!","SLEEPY"};
        int[] moods={0,0,4,6,3,5};
        for(int i=0;i<6;i++) {
            c.gator.reset();c.gator.react(i==0?.15f:.65f);
            for(int tick=0;tick<8+i*6;tick++)c.gator.update(c,DT);
            float x=w*(.26f+.48f*(i%2)),y=h*(.23f+.265f*(i/2)),r=w*.19f;
            BossCollect.draw(p,Boss.ALLIGATOR,x,y,r,c.gator.clock,true,1,moods[i],i%2==0?-.15f:.15f,i==3);
            p.text(labels[i],x,y+r*1.25f,w*.035f,Draw.INK,Painter.CENTER,true);
        }
        Png.write(new File(dir,"168-gator-companion-sheet.png"),raster.resolve(),w,h);
    }

    private static void adventureTitleFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("161-"))return;
        GameCore c=new GameCore(new Mem(),161);c.collected=Collect.MASK;c.landSeen=LandPicker.stateMask();
        int[] companions={0,7,Collect.BOSS_FIRST,Collect.DUCK_FIRST};
        for(int land=0;land<Lands.COUNT;land++) {
            c.landChoice=land;c.caseIndex=companions[land%companions.length];c.clock=1.6f;c.landWanderT=1.6f;
            shot(dir,"161-adventure-land-"+land,c,L,w,h,ss);
        }
        c.landChoice=0;c.modes.adventureTitle.update(c,0);c.landChoice=1;
        c.modes.adventureTitle.update(c,.24f);
        shot(dir,"161-adventure-scenery-changing",c,L,w,h,ss);
        c.modes.adventureTitle.update(c,.6f);
        shot(dir,"161-adventure-slime-bubbles-finishing",c,L,w,h,ss);
        c=new GameCore(new Mem(),161);c.collected=Collect.MASK;c.landSeen=LandPicker.stateMask();
        c.landChoice=0;c.caseIndex=7;LandPicker.select(c,1);
        LandPicker.updateTravel(c,LandPicker.TRAVEL_TIME*.5f);
        shot(dir,"161-adventure-travel",c,L,w,h,ss);
        c=new GameCore(new Mem(),162);c.collected=Collect.add(1,7);c.caseIndex=7;c.clock=1.4f;c.landWanderT=1.4f;
        shot(dir,"161-adventure-first-land",c,L,w,h,ss);
    }

    private static void gatorSilhouetteFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("179-"))return;
        GameCore c=TestTimeAttack.start(Boss.ALLIGATOR);c.toTitle();c.returnFade=0;c.clock=1.6f;
        shot(dir,"179-gator-unlocked",c,L,w,h,ss);
        c.collected &= ~(1L<<Collect.ALLIGATOR_FRIEND);
        shot(dir,"179-gator-silhouette",c,L,w,h,ss);
        c.timeAttack.selected=TimeAttack.ALL;
        c.clock=(Boss.ALLIGATOR+.46f)*TimeAttackDemo.GROUP_TURN;
        shot(dir,"179-gator-group-silhouette",c,L,w,h,ss);
    }
    private static void versusFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("196-versus-"))return;
        for(float fraction:new float[]{0f,.35f,.7f,.95f}) {
            GameCore c=TestTimeAttack.start(Boss.SLIME);c.toTitle();c.returnFade=0;c.beginStart();
            step(c,L,Launch.duration(c)*fraction);
            shot(dir,"196-versus-depart-"+Math.round(fraction*100),c,L,w,h,ss);
        }
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore c=TestTimeAttack.start(boss);
            c.runWho=Collect.COUNT-2;c.companion.begin(c.runWho);c.timeAttack.versus.begin(c,boss);
            step(c,L,.35f);
            shot(dir,"196-versus-boss-"+boss+"-entry",c,L,w,h,ss);
            step(c,L,.85f);
            shot(dir,"196-versus-boss-"+boss,c,L,w,h,ss);
            step(c,L,.23f);
            shot(dir,"196-versus-boss-"+boss+"-jiggle",c,L,w,h,ss);
        }
        for(int who:new int[]{2,Collect.DUCK_FIRST+2}) {
            GameCore c=TestTimeAttack.start(Boss.SLIME);
            c.runWho=who;c.companion.begin(who);c.timeAttack.versus.begin(c,Boss.SLIME);
            step(c,L,1.2f);
            shot(dir,"196-versus-name-"+who,c,L,w,h,ss);
        }
        for(float time:new float[]{.12f,.24f,.35f,.45f,.69f,1.25f,1.95f,2.2f,TimeAttackVersus.READY+.12f,TimeAttackVersus.EXIT+.3f,TimeAttackVersus.DURATION-.01f,TimeAttackVersus.DURATION+.15f}) {
            GameCore c=TestTimeAttack.start(TimeAttack.ALL);
            step(c,L,time);
            shot(dir,"196-versus-phase-"+Math.round(time*100),c,L,w,h,ss);
        }
    }
    private static void timeAttackFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("150-"))return;
        GameCore c=TestTimeAttack.start(Boss.SLIME);c.toTitle();c.returnFade=0;
        for(int frame=0;frame<4;frame++) {
            c.clock=new float[]{.06f,TimeAttackTitle.BEAT*.5f,TimeAttackTitle.BEAT+.06f,TimeAttackTitle.BEAT*1.5f}[frame];
            shot(dir,"150-title-cradle-"+frame,c,L,w,h,ss);
        }
        for(int boss=0;boss<Boss.COUNT;boss++) {
            c.timeAttack.selected=boss;c.caseIndex=boss==Boss.MUSHROOM?Collect.DUCK_FIRST:7;
            for(int beat=0;beat<3;beat++) {
                c.clock=new float[]{.5f,1.6f,2.65f}[beat];
                shot(dir,"150-skit-"+boss+"-"+beat,c,L,w,h,ss);
            }
        }
        c.timeAttack.selected=Boss.SLIME;c.clock=.5f;
        shot(dir,"150-selector-slime",c,L,w,h,ss);
        c.timeAttack.selected=TimeAttack.ALL;
        shot(dir,"150-selector-all",c,L,w,h,ss);
        for(int beat=0;beat<Boss.COUNT;beat++) {
            c.clock=(beat+.46f)*TimeAttackDemo.GROUP_TURN;
            shot(dir,"150-skit-all-"+beat,c,L,w,h,ss);
        }
        c.clock=TimeAttackDemo.GROUP_TURN*1.14f;
        shot(dir,"150-skit-queue-shuffle",c,L,w,h,ss);
        c.timeAttack.selected=Boss.SLIME;c.timeAttack.choose(c,1);c.clock=1.3f;c.timeAttack.slide=.32f;
        shot(dir,"150-boss-scroll-overlap",c,L,w,h,ss);
        c.timeAttack.selected=Boss.SLIME;c.timeAttack.choose(c,-1);c.clock=.5f;
        for(int frame=0;frame<3;frame++) {
            c.timeAttack.slide=frame*.5f;
            shot(dir,"150-boss-scroll-"+frame,c,L,w,h,ss);
        }
        GameCore mode=TestModeSelector.title();float mx=L.w*.5f,my=ModeSelector.y(L);
        mode.modes.touch(mode,L,0,1,mx,my);mode.modes.touch(mode,L,2,1,mx-L.w*.12f,my);
        shot(dir,"150-mode-partial",mode,L,w,h,ss);
        mode.modes.touch(mode,L,1,1,mx-L.w*.12f,my);mode.modes.update(mode,ModeSelector.SWIPE_RETURN*.5f);
        shot(dir,"150-mode-return",mode,L,w,h,ss);
        mode.modes.restore(ModeSelector.SURVIVAL);mode.clock=1;
        mode.modes.touch(mode,L,0,1,mx,my);mode.modes.touch(mode,L,2,1,mx-L.w*.12f,my);
        shot(dir,"150-survival-stripes-drag",mode,L,w,h,ss);
        mode.modes.touch(mode,L,1,1,mx-L.w*.12f,my);mode.modes.update(mode,ModeSelector.SWIPE_RETURN*.5f);
        shot(dir,"150-survival-stripes-return",mode,L,w,h,ss);
        mode.modes.select(mode,ModeSelector.ADVENTURE,-1);mode.modes.update(mode,ModeSelector.CHANGE*.3f);
        shot(dir,"150-survival-stripes-exit",mode,L,w,h,ss);
        c.startGame();c.boss.intro=0;c.update(Check.DT,L);c.timeAttack.seconds=42.56;
        shot(dir,"150-combat",c,L,w,h,ss);
        for(int boss=0;boss<Boss.COUNT;boss++) {
            c.boss.intro=0;c.timeAttack.tick(c,10+boss*3);
            c.hits+=12+boss;c.misses+=boss;c.timeAttack.defended();
            c.timeAttack.bossDamaged(c.boss.hp,c.boss.hp-1);
            if(boss==1)c.takeHit(L.w*.5f,L);
            TestTimeAttack.beat(c,L);
        }
        c.time=c.deathDuration()+2;
        shot(dir,"150-result",c,L,w,h,ss);
        c.toTitle();c.returnFade=0;c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        shot(dir,"150-records",c,L,w,h,ss);
        c.highScoreScreen.action(c,HighScoreScreen.ROW);
        shot(dir,"150-record-summary",c,L,w,h,ss);
        c.highScoreScreen.scrollTo(c,L,c.highScoreScreen.maxScroll(c,L)*.5f);
        shot(dir,"150-record-bosses-middle",c,L,w,h,ss);
        c.highScoreScreen.scrollTo(c,L,c.highScoreScreen.maxScroll(c,L));
        shot(dir,"150-record-bosses-bottom",c,L,w,h,ss);
        c.highScoreScreen.open=false;
        c.startGame();c.boss.intro=0;c.timeAttack.seconds=12.34f;c.hits=19;c.misses=3;
        c.timeAttack.defended=7;c.lives=1;c.takeHit(L.w*.5f,L);c.time=c.deathDuration()+2;c.deathT=c.flash=c.shake=0;
        shot(dir,"150-failed-summary",c,L,w,h,ss);
        c.toTitle();c.returnFade=0;c.timeAttack.selected=Boss.SLIME;
        c.timeAttack.histories[0][Boss.SLIME].legacy(12345,1);c.timeAttack.best[0][Boss.SLIME]=12345;
        c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);c.highScoreScreen.action(c,HighScoreScreen.ROW);
        shot(dir,"150-legacy-summary",c,L,w,h,ss);
        RasterPainter p=new RasterPainter(w,h,ss);p.clear(Draw.BG);
        p.text("TIME ATTACK TURTLES",w*.5f,h*.05f,w*.045f,Draw.INK,Painter.CENTER,true);
        p.text("APPEARANCE REVIEW / NOT IN GAME YET",w*.5f,h*.077f,w*.022f,Draw.INK_DIM,Painter.CENTER,false);
        TurtleConcept.draw(p,0,w*.5f,h*.29f,w*.25f);
        p.text(TurtleConcept.NAMES[0],w*.5f,h*.48f,w*.03f,Draw.INK,Painter.CENTER,true);
        for(int i=0;i<5;i++) {
            float x=w*(.1f+i*.2f),y=h*.64f;
            TurtleConcept.draw(p,i,x,y,w*.078f);
            TurtleConcept.draw(p,i,x,h*.82f,w*.023f);
        }
        p.text("DISPLAY CASE SIZE",w*.5f,h*.73f,w*.026f,Draw.INK_DIM,Painter.CENTER,true);
        p.text("COMPANION SIZE",w*.5f,h*.88f,w*.026f,Draw.INK_DIM,Painter.CENTER,true);
        Png.write(new File(dir,"150-turtle-review.png"),p.resolve(),w,h);
    }

    private static void duckFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("156-ducks-"))return;
        GameCore c=TestSurvival.start(TestSurvival.store(),156);
        c.survival.seconds=300;c.lives=1;c.takeHit(L.w*.5f,L);
        c.shake=c.flash=c.skyGlow=0;
        for(int i=0;i<Collect.DUCK_COUNT;i++) {
            int who=Collect.DUCK_FIRST+i;c.survival.reward.who=who;c.collectionCounts[who]=1;
            c.collected=Collect.add(c.collected,who);
            c.survival.seconds=new int[]{30,30,60,60,100,140,185,230,275,290,300}[i];
            c.deathT=0;c.time=c.deathDuration()+2;
            for(int tick=0;tick<40;tick++)c.ducks.update(c,Check.DT);
            shot(dir,"156-ducks-reveal-"+i,c,L,w,h,ss);
        }
        c.survival.reward.who=Collect.DUCK_FIRST;
        c.survival.seconds=30;
        for(float t:new float[]{.15f,.30f,.48f,.60f,.70f,.78f,.88f,.99f,1f}) {
            c.deathT=c.deathDuration()*(1-t);c.time=c.deathDuration()*t;
            shot(dir,"156-ducks-transition-"+Math.round(t*100),c,L,w,h,ss);
        }
        c.deathT=0;c.time=c.deathDuration()+2;
        c.prize=c.survival.reward.who;c.prizeNew=true;
        c.dismissGameOver();
        for(float t:new float[]{.15f,.48f,.65f,.90f}) {
            c.time=c.survival.reward.joinAt+GameCore.PARADE_TIME*t;
            shot(dir,"156-ducks-joins-"+Math.round(t*100),c,L,w,h,ss);
        }
        c.time=c.survival.reward.joinAt+GameCore.PARADE_TIME+.01f;c.survival.reward.update(c);
        c.time=c.deathDuration()+2;
        shot(dir,"156-ducks-summary",c,L,w,h,ss);
        RasterPainter raster=new RasterPainter(w,h,ss);raster.clear(Draw.BG);
        Painter p=new OpacityPainter(raster,1) { public DuckBodies ducks() {return c.ducks;} };
        p.text("SURVIVAL DUCKS",w*.5f,h*.055f,w*.056f,Draw.INK,Painter.CENTER,true);
        p.text("TEN RAINBOW RIDERS + ONE CHAMPION",w*.5f,h*.083f,w*.023f,Draw.INK_DIM,Painter.CENTER,false);
        float rowH=h*.205f;
        for(int i=0;i<11;i++) {
            int who=Collect.DUCK_FIRST+i;float x=w*(.18f+.32f*(i%3)),y=h*.19f+(i/3)*rowH,r=w*.103f;
            if(i==10)x=w*.50f;
            Trinket.draw(p,who,x,y,r,1.2f,true,1);
            p.text(Collect.NAME[who],x,y+r*1.36f,w*.024f,Draw.INK,Painter.CENTER,true);
            Trinket.draw(p,who,x,y+r*1.83f,r*.22f,1.2f,true,1);
        }
        Png.write(new File(dir,"156-ducks-review-sheet.png"),raster.resolve(),w,h);
    }

    private static void survivalEndingFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("149-rainbow-end-"))return;
        GameCore c=TestSurvival.start(TestSurvival.store(),173);
        c.survival.update(c,150);c.lives=1;c.takeHit(L.w*.5f,L);
        c.shake=c.flash=c.skyGlow=0;
        for(float t:new float[]{.15f,.30f,.48f,.60f,.78f,.88f,1f}) {
            c.deathT=c.deathDuration()*(1-t);c.time=c.deathDuration()*t;
            shot(dir,"149-rainbow-end-"+Math.round(t*100),c,L,w,h,ss);
        }
    }

    private static void survivalFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wantedGroup("149-survival-"))return;
        GameCore rack=TestSurvival.start(TestSurvival.store(),149);
        rack.survival.update(rack,10f);
        shot(dir,"149-survival-rack-ready",rack,L,w,h,ss);
        rack.tapPower(Survival.rackX(L),Survival.rackY(L,0),L);rack.flash=rack.shake=0;
        shot(dir,"149-survival-rack-active",rack,L,w,h,ss);
        rack.modeLeft=.001f;rack.update(Check.DT,L);rack.flash=rack.shake=0;
        shot(dir,"149-survival-rack-used",rack,L,w,h,ss);
        rack.pushUsed=true;rack.survival.rescueLeft=15;
        shot(dir,"149-survival-rescue-half",rack,L,w,h,ss);
        rack.survival.rescueLeft=29.9f;rack.survival.update(rack,.2f);
        shot(dir,"149-survival-rescue-ready",rack,L,w,h,ss);
        GameCore adventure=new GameCore(new Mem(),149);adventure.startGame();adventure.stageBanner=0;
        adventure.pushUsed=true;adventure.warnLevel=1;
        shot(dir,"149-survival-adventure-rescue-used",adventure,L,w,h,ss);
        for(float age:new float[]{0,.5f,1f,2f}) {
            GameCore c=TestSurvival.start(TestSurvival.store(),149);
            c.survival.update(c,age);
            shot(dir,"149-survival-entry-"+Math.round(age*1000),c,L,w,h,ss);
        }
        for(int age:new int[]{0,150,300}) {
            GameCore c=TestSurvival.start(TestSurvival.store(),149);
            c.survival.seconds=age;c.survival.skyPhase=age*(1f+1.5f*c.survival.ramp());
            c.survival.update(c,.001f);c.clock=3;c.score=age*85;
            for(int row=0;row<3;row++) {
                GameCore.Enemy e=Check.add(c,L,new int[]{row%3,(row+1)%3},L.h*(.30f+row*.16f));
                e.baseX=L.w*(.4f+row*.1f);
            }
            shot(dir,"149-survival-"+age,c,L,w,h,ss);
            if(age==300) {
                GameCore.Enemy e=c.enemies.get(1);c.destroyWord(e,e.baseX,e.y,L);
                Fx.updateParticles(c,.08f);shot(dir,"149-survival-clear",c,L,w,h,ss);
                c.lives=1;c.takeHit(L.w*.5f,L);
                for(int frame=0;frame<=4;frame++) {
                    if(frame>0)step(c,L,c.deathDuration()/4);
                    shot(dir,"149-survival-exit-"+frame,c,L,w,h,ss);
                }
                step(c,L,1);
                shot(dir,"149-survival-result",c,L,w,h,ss);
            }
        }
        GameCore scores=TestSurvival.start(TestSurvival.store(),169);
        scores.modes.restore(ModeSelector.SURVIVAL);
        scores.toTitle();scores.returnFade=0;
        scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        shot(dir,"149-survival-scores-empty",scores,L,w,h,ss);
        for(int i=0;i<12;i++) {
            scores.startGame();scores.survival.seconds=310-i*20;scores.score=(12-i)*1234;
            scores.hits=120;scores.misses=5;scores.maxCombo=18;scores.squishes=84;
            scores.runScores().swipes=4;scores.runScores().powers=3;
            for(int effect:Power.OFFERED)scores.runScores().effect(effect);
            scores.lives=0;scores.survival.finish(scores);scores.toTitle();scores.returnFade=0;
        }
        shot(dir,"149-survival-scores-title",scores,L,w,h,ss);
        scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        shot(dir,"149-survival-scores-list",scores,L,w,h,ss);
        scores.highScoreScreen.selected=0;
        shot(dir,"149-survival-scores-summary",scores,L,w,h,ss);
    }

    private static void modeFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("148-mode-"))return;
        GameCore c=TestModeSelector.title();
        c.onboarding.learn(c,TutorialSpeech.LANDS);
        shot(dir,"148-mode-adventure",c,L,w,h,ss);
        c.modes.select(c,ModeSelector.SURVIVAL,1);step(c,L,ModeSelector.CHANGE*.5f);
        shot(dir,"148-mode-changing",c,L,w,h,ss);
        step(c,L,ModeSelector.CHANGE);shot(dir,"148-mode-survival",c,L,w,h,ss);
        step(c,L,.45f);shot(dir,"148-mode-survival-animated",c,L,w,h,ss);
        for(float clock:new float[]{.43f,1.43f,2.45f,3.45f}) {
            c.clock=clock;shot(dir,"148-mode-survival-skit-"+Math.round(clock*100),c,L,w,h,ss);
        }
        c.modes.select(c,ModeSelector.TIME_ATTACK,1);step(c,L,ModeSelector.CHANGE);
        shot(dir,"148-mode-time-attack",c,L,w,h,ss);
        c.openCase();step(c,L,.5f);shot(dir,"148-mode-shared-case",c,L,w,h,ss);
        c.closeCase();step(c,L,.5f);Pause.back(c);step(c,L,ModeSelector.CHANGE);
        c.modes.confirm(c);step(c,L,.16f);shot(dir,"148-mode-confirmed",c,L,w,h,ss);
        c.collected=1;c.modes.select(c,ModeSelector.SURVIVAL,1);step(c,L,ModeSelector.CHANGE);
        shot(dir,"148-mode-locked",c,L,w,h,ss);
        c.modes.confirm(c);step(c,L,.05f);
        shot(dir,"148-mode-locked-fade-out",c,L,w,h,ss);
        step(c,L,.11f);shot(dir,"148-mode-locked-reaction",c,L,w,h,ss);
        step(c,L,.4f);shot(dir,"148-mode-locked-fade-in",c,L,w,h,ss);
        step(c,L,.2f);
        shot(dir,"148-mode-locked-settled",c,L,w,h,ss);
        c.modes.select(c,ModeSelector.TIME_ATTACK,1);step(c,L,ModeSelector.CHANGE);
        shot(dir,"148-mode-locked-time-attack",c,L,w,h,ss);
    }

    private static void starterFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("starter-"))return;
        Check.Mem store=new Check.Mem();store.tutorials=store.powerTutorials=0;
        GameCore c=new GameCore(store,125);step(c,L,1);
        shot(dir,"starter-title",c,L,w,h,ss);
        c.hintTitleKeys();
        shot(dir,"starter-key-hint",c,L,w,h,ss);
        step(c,L,.6f);shot(dir,"starter-key-hint-fading",c,L,w,h,ss);
        step(c,L,.7f);shot(dir,"starter-key-hint-done",c,L,w,h,ss);
        c.beginStart();shot(dir,"starter-controls-start",c,L,w,h,ss);
        step(c,L,Starter.CONTROLS*.5f);shot(dir,"starter-controls-away",c,L,w,h,ss);
        step(c,L,.35f);shot(dir,"starter-enter",c,L,w,h,ss);
        step(c,L,.56f);shot(dir,"starter-arrival",c,L,w,h,ss);
        step(c,L,Starter.READY);shot(dir,"starter-choices",c,L,w,h,ss);
        for(int wave=0;wave<3;wave++) {
            for(int frame=0;frame<180;frame++) {
                step(c,L,1f/60f);boolean waving=false;
                for(int friend=0;friend<Starter.CHOICES.length;friend++)
                    waving|=c.starter.waveProgress(friend)>.35f && c.starter.waveProgress(friend)<.55f;
                if(waving)break;
            }
            shot(dir,"starter-wave-"+wave,c,L,w,h,ss);step(c,L,Starter.WAVE);
        }
        c.starter.choose(c,4);step(c,L,Starter.FOCUS*.5f);shot(dir,"starter-focus",c,L,w,h,ss);
        step(c,L,Starter.FOCUS);shot(dir,"starter-preview",c,L,w,h,ss);
        step(c,L,.35f);shot(dir,"starter-dance",c,L,w,h,ss);
        c.starter.choose(c,0);step(c,L,Starter.FOCUS);shot(dir,"starter-switch",c,L,w,h,ss);
        c.starter.confirm(c);step(c,L,.4f);shot(dir,"starter-exit",c,L,w,h,ss);
        step(c,L,Starter.EXIT-.41f);shot(dir,"starter-before-launch",c,L,w,h,ss);
        step(c,L,.02f);shot(dir,"starter-launch",c,L,w,h,ss);
        step(c,L,Launch.TIME+.1f);
        TestOnboarding.awaitBubble(c,L);
        for(int page=0;page<TutorialSpeech.pageCount(TutorialSpeech.COMPANION);page++) {
            shot(dir,"starter-intro-"+page,c,L,w,h,ss);
            TestOnboarding.advancePage(c,L);TestOnboarding.awaitBubble(c,L);
        }
        step(c,L,.6f);shot(dir,"starter-playing",c,L,w,h,ss);
    }

    private static void onboardingFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        boolean selected=only==null;
        if(only!=null)for(String tag:only)
            selected|="114-".startsWith(tag.trim()) || tag.trim().startsWith("114-");
        if(!selected)return;
        for(int message=TutorialSpeech.MATCH;message<=TutorialSpeech.STORIES;message++) {
            GameCore c=new GameCore(new Mem(),114);c.startGame();
            int lesson=message==TutorialSpeech.DIG?Onboarding.MINE:
                    message==TutorialSpeech.WAIT || message==TutorialSpeech.ALTERNATE || message==TutorialSpeech.LIFT?Onboarding.STEAMER:Onboarding.CORE;
            c.onboarding.begin(c,lesson,L);c.onboarding.sceneWait=0;
            c.onboarding.speech=message;c.onboarding.briefing=true;
            c.onboarding.companionTravel=1;c.onboarding.bubbleAge=Onboarding.BUBBLE_OPEN;
            for(int page=0;page<TutorialSpeech.pageCount(message);page++) {
                c.onboarding.speechPage=page;
                shot(dir,"114-script-"+message+"-page-"+page,c,L,w,h,ss);
            }
        }
        GameCore pickup=TestPowerTutorials.fresh(L,new Check.Mem());
        TestPower.place(pickup,L,Power.NINJA,0);step(pickup,L,.6f);
        shot(dir,"114-power-pickup-explain",pickup,L,w,h,ss);
        TestOnboarding.acknowledge(pickup,L);step(pickup,L,.2f);
        shot(dir,"114-power-pickup-try",pickup,L,w,h,ss);
        for(int kind:Power.OFFERED) {
            GameCore c=TestPowerTutorials.fresh(L,new Check.Mem());
            Check.add(c,L,new int[]{0,1},L.playTop+L.enemyR*4);
            c.startFrenzy(kind,L);step(c,L,.6f);
            shot(dir,"114-power-"+kind+"-explain",c,L,w,h,ss);
            TestOnboarding.acknowledge(c,L);step(c,L,.15f);
            shot(dir,"114-power-"+kind+"-try",c,L,w,h,ss);
            TestPowerTutorials.use(c,L,kind);step(c,L,.2f);
            shot(dir,"114-power-"+kind+"-return",c,L,w,h,ss);
        }
        GameCore lands=new GameCore(new Mem(),139);
        lands.collected=Collect.add(1L,Collect.BOSS_FIRST);
        lands.onboarding.saved=lands.onboarding.savedPowers=0;
        lands.onboarding.learn(lands,TutorialSpeech.STORIES);
        step(lands,L,3f);TestOnboarding.awaitBubble(lands,L);
        shot(dir,"114-lands-intro",lands,L,w,h,ss);
        TestOnboarding.acknowledge(lands,L);step(lands,L,.6f);
        shot(dir,"114-lands-swipe",lands,L,w,h,ss);
        GameCore stars=new GameCore(new Mem(),114);stars.startGame();stars.onboarding.saved=0;
        stars.starNext=true;Interlude.enterBonus(stars,L);step(stars,L,.6f);
        shot(dir,"114-stars-arrival",stars,L,w,h,ss);
        TestOnboarding.reveal(stars,L);
        TestOnboarding.awaitBubble(stars,L);
        for(int page=0;page<TutorialSpeech.pageCount(TutorialSpeech.STARS);page++) {
            TestOnboarding.awaitBubble(stars,L);
            shot(dir,"114-stars-page-"+page,stars,L,w,h,ss);
            if(page+1<TutorialSpeech.pageCount(TutorialSpeech.STARS))TestOnboarding.advancePage(stars,L);
        }
        shot(dir,"114-stars-ready",stars,L,w,h,ss);
        TestOnboarding.acknowledge(stars,L);step(stars,L,StarPath.READY+.4f);
        shot(dir,"114-stars-flight",stars,L,w,h,ss);
        for(int lesson:new int[]{Onboarding.STEAMER,Onboarding.CART,Onboarding.MINE}) {
            GameCore c=new GameCore(new Mem(),114);c.startGame();
            c.onboarding.begin(c,lesson,L);
            if(lesson==Onboarding.STEAMER) {
                step(c,L,.5f);shot(dir,"114-steamer-arrive",c,L,w,h,ss);
                step(c,L,.4f);shot(dir,"114-steamer-picking",c,L,w,h,ss);
                TestOnboarding.reveal(c,L);
            }
            step(c,L,.6f);GameCore q=c.onboarding.practice;
            String name=lesson==2?"steamer":lesson==4?"stars":lesson==8?"cart":"mine";
            shot(dir,"114-"+name+"-ready",c,L,w,h,ss);
            TestOnboarding.acknowledge(c,L);
            if(lesson==Onboarding.STEAMER) {
                shot(dir,"114-steamer-keys",c,L,w,h,ss);
                q.tapBonus(q.steamer.wanted());q.tapBonus(q.steamer.wanted());step(c,L,.6f);
                shot(dir,"114-steamer-progress",c,L,w,h,ss);
                for(int i=0;i<40 && !q.bonusSwipeReady();i++)q.tapBonus(q.steamer.wanted());
                step(c,L,.6f);
                TestOnboarding.awaitBubble(c,L);
                shot(dir,"114-steamer-lid",c,L,w,h,ss);
                TestOnboarding.acknowledge(c,L);shot(dir,"114-steamer-lid-try",c,L,w,h,ss);
            } else if(lesson==Onboarding.CART) {
                step(c,L,CaveCart.READY+.6f);
                shot(dir,"114-cart-steer",c,L,w,h,ss);
            } else if(lesson==Onboarding.MINE) {
                step(c,L,CaveMining.READY+.1f);
                shot(dir,"114-mine-keys",c,L,w,h,ss);
                for(int i=0;i<600 && !q.mining.swipeReady();i++) {
                    if(q.mining.digging())q.tapBonus(q.mining.sequence[q.mining.pos]);
                    c.update(DT,L);
                }
                step(c,L,.6f);shot(dir,"114-mine-swipe",c,L,w,h,ss);
                TestOnboarding.acknowledge(c,L);shot(dir,"114-mine-swipe-try",c,L,w,h,ss);
            }
        }
        for(int kind=0;kind<Boss.COUNT;kind++) {
            GameCore c=new GameCore(new Mem(),114);c.startGame();c.onboarding.saved=0;c.stage=(kind+1)*5;
            c.boss.begin(kind,c.stage,c.rnd);c.update(DT,L);
            shot(dir,"114-boss-"+kind+"-arrival-question",c,L,w,h,ss);
            c.boss.intro=0;c.update(DT,L);
            shot(dir,"114-boss-"+kind+"-question",c,L,w,h,ss);
            float clock=c.clock;
            c.clock=.375f;shot(dir,"114-boss-"+kind+"-question-pulse-large",c,L,w,h,ss);
            c.clock=1.125f;shot(dir,"114-boss-"+kind+"-question-pulse-small",c,L,w,h,ss);
            c.clock=clock;
            TestOnboarding.touch(c,L,0,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
            TestOnboarding.touch(c,L,1,TutorialSpeech.helpX(L),TutorialSpeech.helpY(L));
            if(kind==Boss.SLIME) {
                for(int i=0;i<=10;i++) {
                    c.onboarding.companionTravel=i/10f;
                    shot(dir,"114-companion-hop-"+i,c,L,w,h,ss);
                }
                for(int i=0;i<=4;i++) {
                    c.onboarding.bubbleAge=Onboarding.BUBBLE_OPEN*i/4;
                    shot(dir,"114-companion-bubble-"+i,c,L,w,h,ss);
                }
                c.onboarding.companionTravel=c.onboarding.bubbleAge=0;
            }
            step(c,L,.2f);shot(dir,"114-boss-"+kind+"-travel",c,L,w,h,ss);
            int page=0;
            while(c.onboarding.briefing && page<4) {
                step(c,L,.6f);shot(dir,"114-boss-"+kind+"-help-"+(page++),c,L,w,h,ss);
                TestOnboarding.acknowledge(c,L);
            }
            if(kind==Boss.SLIME) {
                c.stageBanner=0;
                TestOnboarding.makeSlimeVulnerable(c,L);step(c,L,1f);
                shot(dir,"114-boss-0-vulnerable-wait",c,L,w,h,ss);
                step(c,L,1.2f);
                shot(dir,"114-boss-0-vulnerable-help",c,L,w,h,ss);
                TestOnboarding.acknowledge(c,L);step(c,L,.2f);
                shot(dir,"114-boss-0-vulnerable-try",c,L,w,h,ss);
            }
        }
        GameCore rescue=new GameCore(new Mem(),114);rescue.startGame();
        rescue.onboarding.rescue(rescue);
        step(rescue,L,.6f);
        shot(dir,"114-rescue-speech",rescue,L,w,h,ss);
        TestOnboarding.advancePage(rescue,L);step(rescue,L,.6f);
        shot(dir,"114-rescue-limit",rescue,L,w,h,ss);
        GameCore chosen=new GameCore(new Mem(),114);chosen.collected=1L<<16;chosen.caseIndex=16;chosen.startGame();
        chosen.onboarding.begin(chosen,Onboarding.MINE,L);step(chosen,L,.9f);
        shot(dir,"114-chosen-companion",chosen,L,w,h,ss);
        for(int hint=0;hint<3;hint++) {
            GameCore c=new GameCore(new Mem(),114);c.startGame();c.onboarding.saved=Onboarding.CORE;
            if(hint==1)c.misses=1;
            if(hint==2)c.onboarding.saved|=Onboarding.WORD_HINT;
            Check.add(c,L,new int[]{0,1},new int[]{hint==2?2:1,1},L.playTop+L.enemyR*4);
            step(c,L,.6f);shot(dir,"114-hint-"+hint,c,L,w,h,ss);
            if(hint==2)for(int hits=0;hits<3;hits++) {
                c.onboarding.age=hits/1.2f+.05f;
                shot(dir,"114-stack-pips-"+(3-hits),c,L,w,h,ss);
            }
        }
        GameCore collection=new GameCore(new Mem(),114);collection.startGame();collection.collected=1;
        collection.onboarding.savedPowers=0;collection.toTitle();collection.time=2;step(collection,L,.6f);
        shot(dir,"114-case-explain",collection,L,w,h,ss);
        TestOnboarding.acknowledge(collection,L);shot(dir,"114-case-try",collection,L,w,h,ss);
        collection.openCase();step(collection,L,1f);shot(dir,"114-story-explain",collection,L,w,h,ss);
        TestOnboarding.acknowledge(collection,L);shot(dir,"114-story-try",collection,L,w,h,ss);
        collection.openStory();step(collection,L,.6f);shot(dir,"114-story-open",collection,L,w,h,ss);
        GameCore settings=new GameCore(new Mem(),114);PlayerSettings.open(settings);
        settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME);
        shot(dir,"114-settings-reset",settings,L,w,h,ss);
    }

    private static void townFrames(File dir, Layout L, int w, int h, int ss) throws Exception {
        if (!wanted("town-")) return;
        GameCore hidden = new GameCore(new Mem(), 1069L);
        shot(dir, "town-title-hidden", hidden, L, w, h, ss);
        GameCore unlocked = new GameCore(new Mem(), 1070L);
        unlocked.collected |= 1L << Collect.BOSS_FIRST;
        unlocked.landChoice = LandPicker.TOWN;
        shot(dir, "town-title-unlocked", unlocked, L, w, h, ss);

        GameCore c = new GameCore(new Mem(), 1071L);
        c.collected |= 1L << Collect.BOSS_FIRST;
        c.collectionCounts[Collect.BOSS_FIRST] = 1;
        c.openTown(); c.townReveal=0f;
        c.town.dialogue = Town.TALK_NONE;
        c.town.flowerCount = 11;
        c.town.path = .08f;
        shot(dir, "town-meadow", c, L, w, h, ss);
        c.town.path = .50f;
        shot(dir, "town-pan-middle", c, L, w, h, ss);
        c.town.path = .61f;
        shot(dir, "town-slide-clearing", c, L, w, h, ss);
        c.town.react(TownScreen.pathX(.66f,L),TownScreen.landmarkGround(.66f,L)-L.w*.07f);
        for (int frame=0;frame<12;frame++) {
            shot(dir, String.format("town-touch-motion-%02d",frame), c, L, w, h, ss);
            c.update(.1f,L);
        }
        c.town.touchAge=3f;
        c.town.path = .945f;
        shot(dir, "town-pan-end", c, L, w, h, ss);
        for(int frame=0;frame<24;frame++) {
            c.town.path=.10f+.84f*frame/23f;
            c.town.bounceClock+=.45f;
            c.clock+=.1f;
            shot(dir,String.format("town-parallax-%02d",frame),c,L,w,h,ss);
        }

        c.town.dialogue = Town.TALK_BUILD_ARCH;
        c.town.path = .20f;
        shot(dir, "town-flower-arch-plot", c, L, w, h, ss);
        c.town.archBuilt = true;
        c.town.dialogue = Town.TALK_NONE;
        shot(dir, "town-flower-arch-built", c, L, w, h, ss);
        c.town.dialogue = Town.TALK_SLIME;
        c.town.path = .84f;
        shot(dir, "town-slime-conversation", c, L, w, h, ss);

        c.town.beginSlide();
        c.town.holdSlide();
        for (int i = 0; i < 70; i++) c.town.update(DT, L);
        shot(dir, "town-slide-charged", c, L, w, h, ss);
        c.town.releaseSlide();
        for (int i = 0; i < 25; i++) c.town.update(DT, L);
        shot(dir, "town-slide-ramp", c, L, w, h, ss);
        for (int i = 0; i < 180 && !c.town.slideAirborne; i++) c.town.update(DT, L);
        for (int i = 0; i < 12 && !c.town.slideLanded; i++) c.town.update(DT, L);
        shot(dir, "town-slide-flight", c, L, w, h, ss);
        for (int i = 0; i < 240 && !c.town.slideLanded; i++) c.town.update(DT, L);
        shot(dir, "town-slide-landed", c, L, w, h, ss);
    }

    private static void slimeFightFrames(File dir, Layout L, int w, int h, int ss)
            throws Exception {
        if (!wanted("slime-fight-")) return;
        SlimeFight f = new SlimeFight();
        f.begin(L, 1081);
        slimeShot(dir, "slime-fight-ready", f, L, w, h, ss);
        for (int i = 0; i < 100; i++) f.update(DT, L);
        f.steer(.72f, L);
        for (int i = 0; i < 22; i++) f.update(DT, L);
        slimeShot(dir, "slime-fight-play", f, L, w, h, ss);
        float y = SlimeFight.playerY(L);
        f.touch(L, 0, 1, f.playerX, y);
        f.update(.08f, L);
        f.touch(L, 2, 1, f.playerX - L.w * .04f, y - L.w * .24f);
        f.touch(L, 1, 1, f.playerX - L.w * .04f, y - L.w * .24f);
        for (int i = 0; i < 10; i++) f.update(DT, L);
        slimeShot(dir, "slime-fight-fling", f, L, w, h, ss);
        f.timeLeft = .01f;
        f.update(.02f, L);
        slimeShot(dir, "slime-fight-fun-report", f, L, w, h, ss);
    }

    private static void companionFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("118-companion")) return;
        for(int event=0;event<=RunCompanion.CRY;event++) {
            GameCore c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=11;c.startGame();
            c.stageBanner=0;c.companion.react(event,1f);
            int steps=event==RunCompanion.DAMAGE?14:event==RunCompanion.STAGE_CLEAR?36:10;
            for(int step=0;step<steps;step++) c.companion.update(c,DT);
            shot(dir,"118-companion-reaction-"+event,c,L,w,h,ss);
        }
        GameCore touched=new GameCore(new Mem(),118L);touched.collected=Collect.MASK;
        touched.caseIndex=11;touched.startGame();touched.stageBanner=0;
        touched.tapCompanion(RunCompanion.x(L),RunCompanion.y(L),L);
        touched.companion.update(touched,.08f);
        shot(dir,"118-companion-touch",touched,L,w,h,ss);
        for(int frame=0;frame<4;frame++) {
            GameCore c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=11;c.startGame();
            c.stageBanner=0;c.companion.react(RunCompanion.VICTORY,1f);
            c.companion.age=(float)Math.PI/18f+frame*(float)Math.PI/9f;
            c.companion.clock=c.companion.age;
            shot(dir,"118-companion-boss-victory-rock-"+frame,c,L,w,h,ss);
        }
        for(int who:new int[]{0,8,23,Collect.BOSS_FIRST,Collect.BOSS_FIRST+1,Collect.BOSS_FIRST+2,
                Collect.BOSS_FIRST+3}) {
            GameCore c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=who;c.startGame();
            c.stageBanner=0;c.companion.update(c,.1f);
            shot(dir,"118-companion-character-"+who,c,L,w,h,ss);
        }
        GameCore c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.stageBanner=0;c.startFrenzy(Power.TEAM,L);
        for(int frame=0;frame<8;frame++) {
            shot(dir,"118-companion-team-"+frame,c,L,w,h,ss);step(c,L,.09f);
        }
        c.modeLeft=.01f;step(c,L,DT);
        for(int frame=0;frame<8;frame++) {
            shot(dir,"118-companion-team-return-"+frame,c,L,w,h,ss);step(c,L,.09f);
        }
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.stageBanner=0;c.startFrenzy(Power.NINJA,L);c.companion.update(c,.18f);
        shot(dir,"118-companion-ninja-mask",c,L,w,h,ss);
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.stageBanner=0;c.startFrenzy(Power.FLURRY,L);c.companion.update(c,.24f);
        shot(dir,"118-companion-flurry-bubble",c,L,w,h,ss);
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();c.stageBanner=0;
        c.pushUsed=true;c.pushCount=3;c.pushSlowT=GameCore.PUSH_SLOW;c.pushT=GameCore.PUSH_TIME;
        c.companion.rescue();
        for(int frame=0;frame<6;frame++) {
            c.pushT=GameCore.PUSH_TIME*(1f-frame/6f);
            c.pushSlowT=GameCore.PUSH_SLOW-GameCore.PUSH_TIME*(frame/6f);
            c.companion.rescueT=RunCompanion.RESCUE_TIME-GameCore.PUSH_TIME*(frame/6f);
            shot(dir,"118-companion-rescue-"+frame,c,L,w,h,ss);
        }
        c.pushT=0;c.pushSlowT=GameCore.PUSH_SLOW-GameCore.PUSH_TIME-RunCompanion.RESCUE_HOLD*.5f;
        c.companion.rescueT=RunCompanion.RESCUE_TIME-GameCore.PUSH_TIME-RunCompanion.RESCUE_HOLD*.5f;
        shot(dir,"118-companion-rescue-hold",c,L,w,h,ss);
        c.companion.rescueT=RunCompanion.RESCUE_RETURN+.08f;
        shot(dir,"118-companion-rescue-bar-exit",c,L,w,h,ss);
        c.pushSlowT=GameCore.PUSH_SLOW-GameCore.PUSH_TIME-RunCompanion.RESCUE_HOLD
                -RunCompanion.RESCUE_RETURN*.5f;
        c.companion.rescueT=RunCompanion.RESCUE_RETURN*.5f;
        shot(dir,"118-companion-rescue-return",c,L,w,h,ss);
        c.pushSlowT=GameCore.PUSH_SLOW-GameCore.PUSH_TIME-RunCompanion.RESCUE_HOLD
                -RunCompanion.RESCUE_RETURN-.1f;
        c.companion.rescueT=0f;
        shot(dir,"118-companion-rescue-tired",c,L,w,h,ss);
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.playtestSteamer(L);c.time=.7f;
        shot(dir,"118-companion-steamer",c,L,w,h,ss);
        c.bonusTimer=c.bonusRollEnd;
        c.tapBonus(c.steamer.wanted());c.tapBonus(c.steamer.wanted());
        c.companion.update(c,.24f);
        shot(dir,"118-companion-steamer-score",c,L,w,h,ss);
        c.steamer.hits=c.steamer.goal()-1;
        c.steamer.expectLeft=true;
        c.tapBonus(c.steamer.wanted());c.tapBonus(c.steamer.wanted());
        c.swipeBonus();c.companion.update(c,.18f);
        shot(dir,"118-companion-steamer-win",c,L,w,h,ss);
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.starNext=true;Interlude.enterBonus(c,L);
        for(int frame=0;frame<6;frame++) {
            c.time=frame*StarScreen.COMPANION_TRAVEL/5f;
            shot(dir,"118-companion-starpath-entry-"+frame,c,L,w,h,ss);
        }
        c.time=2f;c.stars.timer=StarPath.REPORT+StarPath.EXIT*.5f;
        shot(dir,"118-companion-starpath-blastoff",c,L,w,h,ss);
        for(int frame=0;frame<6;frame++) {
            c.stars.timer=StarPath.REPORT-frame*StarScreen.COMPANION_RETURN/5f;
            shot(dir,"118-companion-starpath-return-"+frame,c,L,w,h,ss);
        }
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.lives=1;c.takeHit(L.w*.5f,L);step(c,L,c.deathDuration()+GameCore.OVER_FADE+.1f);
        shot(dir,"118-companion-gameover-cry",c,L,w,h,ss);
        c.returnFade=GameCore.RETURN_FADE*.75f;
        shot(dir,"118-companion-gameover-exit",c,L,w,h,ss);
        c=new GameCore(new Mem(),118L);c.collected=Collect.MASK;c.caseIndex=49;c.caveChoice=0;
        c.startGame();Check.enterCave(c,21,L);c.stageBanner=0;
        shot(dir,"118-companion-cave",c,L,w,h,ss);
    }

    static void scoreResetFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        if(!wanted("111-reset-settings") && !wanted("111-reset-confirm")
                && !wanted("111-reset-wipe") && !wanted("111-reset-done") && !wanted("111-reset-empty")) return;
        GameCore c=new GameCore(new Mem(),111L);c.collected=3;c.startGame();c.score=9000;
        LandPicker.recordBest(c);c.highScores.finish(c);c.toTitle();c.returnFade=0;
        PlayerSettings.open(c);c.preferences.updatePanel(c,PlayerSettings.PANEL_TIME);
        shot(dir,"111-reset-settings",c,L,w,h,ss);
        c.preferences.scoreReset.action(c,ScoreReset.OPEN);
        shot(dir,"111-reset-confirm",c,L,w,h,ss);
        c.preferences.scoreReset.action(c,ScoreReset.CONFIRM);
        for(int stroke=0;stroke<3;stroke++) {
            c.update(stroke==0?ScoreReset.LEAD+ScoreReset.STROKE_TIME*.55f
                    :ScoreReset.STROKE_TIME+ScoreReset.STROKE_GAP,L);
            shot(dir,"111-reset-wipe-"+(stroke+1),c,L,w,h,ss);
        }
        c.update(.35f,L);shot(dir,"111-reset-done",c,L,w,h,ss);
        c.update(ScoreReset.TIME,L);
        c.closeSettings();c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        shot(dir,"111-reset-empty",c,L,w,h,ss);
    }

    private static void squishyEntryFrames(File dir,Layout L,int w,int h,int ss) throws Exception {
        boolean selected=only==null;
        if(only!=null) for(String tag:only) selected |= "112-".startsWith(tag.trim()) || tag.trim().startsWith("112-");
        if(!selected) return;
        GameCore picker=new GameCore(new Mem(),112L);picker.beginStart();
        for(int frame=0;frame<5;frame++) {
            shot(dir,"112-picker-"+frame,picker,L,w,h,ss);
            step(picker,L,.16f);
        }
        while(picker.pickerT>0f) step(picker,L,DT);
        shot(dir,"112-picker-launch",picker,L,w,h,ss);
        step(picker,L,Launch.TIME*Launch.POP*.75f);
        shot(dir,"112-picker-pop",picker,L,w,h,ss);
        step(picker,L,Launch.TIME*(Launch.POP*1.1f-Launch.POP*.75f));
        for(int frame=0;frame<4;frame++) {
            shot(dir,"112-picker-hello-"+frame,picker,L,w,h,ss);
            step(picker,L,Launch.TIME*(Launch.LAND-Launch.POP)*.20f);
        }
        while(Launch.progress(picker)<Launch.HOME) step(picker,L,DT);
        shot(dir,"112-picker-home",picker,L,w,h,ss);
        GameCore lettering=new GameCore(new Mem(),112L);lettering.collected=Collect.MASK;
        lettering.caseIndex=51;lettering.clock=18.87f;lettering.beginStart();
        step(lettering,L,Launch.CENTER_TIME*.5f);
        shot(dir,"112-center-slide",lettering,L,w,h,ss);
        step(lettering,L,Launch.NAME_START-Launch.CENTER_TIME*.5f);
        shot(dir,"112-center-arrived",lettering,L,w,h,ss);
        for(int frame=0;frame<8;frame++) {
            shot(dir,"112-name-spring-"+frame,lettering,L,w,h,ss);
            step(lettering,L,.10f);
        }
        for(int who:new int[]{0,11,23,51}) {
            GameCore hello=new GameCore(new Mem(),112L);hello.collected=Collect.MASK;hello.caseIndex=who;
            hello.clock=who*.37f;hello.beginStart();
            step(hello,L,Launch.TIME*(Launch.POP+(Launch.LAND-Launch.POP)*.45f));
            shot(dir,"112-greeting-"+who,hello,L,w,h,ss);
        }
        for(int who:new int[]{4,11,23}) {
            GameCore c=new GameCore(new Mem(),112L);c.collected=Collect.MASK;c.caseIndex=who;c.startGame();
            c.stageBanner=0;c.startFrenzy(Power.TEAM,L);c.flash=0;c.shake=0;
            for(int frame=0;frame<9;frame++) {
                shot(dir,"112-entry-"+who+"-"+frame,c,L,w,h,ss);
                step(c,L,.11f);
            }
        }
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
        GameCore c=new GameCore(new Mem(),921L);c.caveChoice=0;c.startGame();Check.enterCave(c,21,L);
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
            GameCore pick=new GameCore(new Mem(),973L);pick.startGame();Check.enterCave(pick,21,L);
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
        public int loadTutorials() { return (1023 & ~Onboarding.SKIPPED)|(1<<(TutorialSpeech.GLOB+9))|(1<<(TutorialSpeech.RESCUE+9)); }
        public int loadPowerTutorials() { return 127; }
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

        if(wanted("settings-gear-")) {
            GameCore gear=TestTimeAttack.start(Boss.SLIME);gear.toTitle();gear.returnFade=0;
            for(int frame=0;frame<3;frame++) {
                gear.preferences.notificationPending=frame>0;
                gear.clock=frame==2?.42f:1.2f;
                shot(dir,"settings-gear-"+frame,gear,L,w,h,ss);
            }
        }
        if(wanted("136-story")) {
            for(int who:new int[]{1,6,Collect.BOSS_FIRST}) {
                Mem saved=new Mem();saved.collected=Collect.MASK;
                GameCore story=new GameCore(saved,136L);story.openCase();story.caseFade=1f;story.time=2f;
                story.caseIndex=Showcase.across(who,Showcase.column(who)>0?-1:1);CaseUi.tap(story,who);
                float prior=0;
                for(float at:new float[]{0f,.25f,.65f,1.6f}) {
                    step(story,L,at-prior);prior=at;
                    shot(dir,"136-story-"+who+"-"+Math.round(at*100),story,L,w,h,ss);
                }
                Storybook.tap(story,L,Storybook.heroX(story,L),Storybook.heroY(story,L));step(story,L,.22f);
                shot(dir,"136-story-"+who+"-reaction",story,L,w,h,ss);
            }
        }
        if(wanted("134-power-rush")) {
            for(int pattern=0;pattern<3;pattern++) {
                GameCore rush=new GameCore(new Mem(),134L);rush.startGame();
                rush.startFrenzy(Power.NINJA,L);rush.enemies.clear();rush.stageBanner=0;
                rush.time=2f;rush.modeLeft=Power.DURATION-2f;rush.ninjaUsed=true;
                rush.powerSpawnedEnemies=pattern*4;rush.spawnTimer=0;rush.stageGap=0;
                for(int frame=0;frame<90;frame++)rush.update(1f/60f,L);
                rush.flash=rush.shake=0f;
                shot(dir,"134-power-rush-"+pattern,rush,L,w,h,ss);
            }
        }

        if(wanted("133-flurry")) {
            GameCore rings=TestPower.flurryClearScene(L);
            shot(dir,"133-flurry-clear-rings",rings,L,w,h,ss);
            for(GameCore.Enemy e:rings.enemies)e.destroyT=GameCore.DESTROY_TIME*.78f;
            shot(dir,"133-flurry-clear-fade",rings,L,w,h,ss);
        }

        if(wanted("135-ninja-icon")) {
            GameCore icon=new GameCore(new Mem(),135L);icon.startGame();icon.stageBanner=0;
            TestPower.place(icon,L,Power.NINJA,0);icon.power.x=L.w*.5f;
            for(float phase:new float[]{0f,.25f,.75f}) {
                icon.power.t=phase;
                shot(dir,"135-ninja-icon-"+Math.round(phase*100),icon,L,w,h,ss);
            }
        }

        if(wanted("132-steamer")) {
            GameCore steam=new GameCore(new Mem(),132L);steam.startGame();
            steam.steamer.opens=5;steam.playtestSteamer(L);
            steam.bonusTimer=steam.bonusRollEnd;steam.time=1f;steam.clock=2.3f;
            for(int points:new int[]{0,1,10,19,20}) {
                while(steam.steamer.hits<points) {
                    steam.tapBonus(steam.steamer.wanted());steam.tapBonus(steam.steamer.wanted());
                }
                steam.steamer.update(.1f);
                shot(dir,"132-steamer-pips-"+points,steam,L,w,h,ss);
            }
            steam.swipeBonus();
            float last=0f;
            for(float age:new float[]{0f,.15f,.32f,.60f,.95f,1.25f,2.30f}) {
                steam.steamer.update(age-last);last=age;
                shot(dir,"132-steamer-release-"+Math.round(age*100),steam,L,w,h,ss);
            }
        }

        if(wanted("132")) {
            for(int combo:new int[]{1,4,10}) {
                GameCore c=TestPower.ninjaCombo(L,combo,false,false);
                c.ninjaSlashes.update(c.ninjaSlashes.life[0]*.32f);
                shot(dir,"132-ninja-combo-"+combo,c,L,w,h,ss);
            }
            for(boolean reverse:new boolean[]{false,true}) {
                GameCore c=TestPower.ninjaCombo(L,10,true,reverse);
                c.ninjaSlashes.update(c.ninjaSlashes.life[0]*.38f);
                shot(dir,"132-ninja-diagonal-"+(reverse?"reverse":"forward"),c,L,w,h,ss);
            }
            GameCore c=TestPower.ninjaCombo(L,10,true,false);
            c.ninjaSlashes.update(c.ninjaSlashes.life[0]*.85f);
            shot(dir,"132-ninja-fading",c,L,w,h,ss);
        }

        if(wanted("130")) {
            GameCore scores=new GameCore(new Mem(),101L);
            shot(dir,"130-high-scores-title",scores,L,w,h,ss);
            scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
            shot(dir,"130-high-scores-empty",scores,L,w,h,ss);
            for(int i=0;i<10;i++) {
                scores.collected=Collect.MASK;scores.caseIndex=i%3==0?0:i;
                if(i%3==0) scores.collected=0L;
                scores.startGame();scores.score=(10-i)*1357;scores.stage=new int[]{5,10,15,20,1,6,11,16,21,25}[i];
                scores.hits=123;scores.misses=7;scores.squishes=56;scores.maxCombo=48;
                scores.highScores.stages=scores.stage-1;scores.highScores.bosses=(1<<Math.min(Boss.COUNT,(scores.stage-1)/Boss.EVERY))-1;
                int prizes=i==0?18:14-i;
                for(int prize=0;prize<prizes;prize++) scores.highScores.prize(prize%Collect.COUNT);
                scores.highScores.powers=7;scores.highScores.swipes=3;
                scores.highScores.effects[Power.FLURRY]=3;scores.highScores.effects[Power.NINJA]=2;
                scores.highScores.effects[Power.TEAM]=2;scores.highScores.effects[Power.INCOGNITO]=1;
                scores.highScores.effects[Power.MONOCHROME]=2;
                scores.lives=0;scores.highScores.finish(scores);
                if(i==0) {
                    scores.toTitle();scores.returnFade=0;scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
                    shot(dir,"130-high-scores-partial",scores,L,w,h,ss);
                }
            }
            scores.toTitle();scores.returnFade=0;scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
            scores.highScoreScreen.entrance=.3f;
            shot(dir,"130-high-scores-entering",scores,L,w,h,ss);
            scores.highScoreScreen.entrance=1f;
            shot(dir,"130-high-scores-full",scores,L,w,h,ss);
            scores.startGame();scores.score=123;scores.stage=6;scores.lives=0;
            scores.highScores.prize(0);scores.highScores.bosses=1;scores.highScores.finish(scores);
            LandPicker.recordBest(scores);scores.toTitle();scores.returnFade=0;
            scores.time=0f;shot(dir,"130-high-scores-title-glow-low",scores,L,w,h,ss);
            scores.time=(float)Math.PI/3.5f;shot(dir,"130-high-scores-title-glow-high",scores,L,w,h,ss);
            scores.highScoreScreen.show(scores);scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
            scores.clock=0f;shot(dir,"130-high-scores-latest-low",scores,L,w,h,ss);
            scores.clock=(float)Math.PI/3.5f;
            shot(dir,"130-high-scores-latest",scores,L,w,h,ss);
            scores.highScoreScreen.selected=0;
            shot(dir,"130-high-scores-summary",scores,L,w,h,ss);
            scores.highScoreScreen.back(scores);scores.highScoreScreen.back(scores);
            scores.highScoreScreen.update(HighScoreScreen.ENTRY_TIME*.65f);
            shot(dir,"130-high-scores-exiting",scores,L,w,h,ss);
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
            if (!Lands.playable(land)) continue;
            themed.stage = Lands.firstStage(land);
            step(themed, L, 5f);
            themed.stageBanner = 0f;
            shot(dir, "80-land-" + land + "-play", themed, L, w, h, ss);
            for (int variant = 0; variant < 3; variant++) {
                themed.stage = Lands.firstStage(land) + variant;
                themed.stageBanner = GameCore.BANNER_TIME * 0.55f;
                shot(dir, "81-land-" + land + "-skit-" + variant, themed, L, w, h, ss);
            }
        }
        if(wantedGroup("207-")) {
            for(int land:new int[]{Lands.SWAMP,Lands.RAINBOW})for(int variant=0;variant<3;variant++) {
                GameCore skit=new GameCore(new Mem(),207L);skit.startGame();skit.landBlend=1;
                skit.stage=Lands.firstStage(land)+variant;skit.stageGap=1;
                for(int frame=0;frame<8;frame++) {
                    skit.stageBanner=GameCore.BANNER_TIME*(1-(.04f+frame*.12f));
                    shot(dir,"207-land-"+land+"-skit-"+variant+"-"+frame,skit,L,w,h,ss);
                }
                skit.preferences.skin=Skins.HALLOWEEN;skit.stageBanner=GameCore.BANNER_TIME*.5f;
                shot(dir,"207-land-"+land+"-skit-"+variant+"-halloween",skit,L,w,h,ss);
            }
        }
        if(wanted("81-slime-")) {
            GameCore slime=new GameCore(new Mem(),145L);slime.startGame();slime.landBlend=1f;
            for(int variant=0;variant<3;variant++)for(int frame=0;frame<9;frame++) {
                slime.stage=variant+1;
                slime.stageBanner=GameCore.BANNER_TIME*(1f-(.05f+frame*.10f));
                shot(dir,"81-slime-"+variant+"-"+frame,slime,L,w,h,ss);
            }
        }
        if(wanted("81-crystal-")) {
            GameCore crystal=new GameCore(new Mem(),146L);crystal.startGame();crystal.landBlend=1f;
            for(int variant=0;variant<3;variant++)for(int frame=0;frame<9;frame++) {
                crystal.stage=Boss.EVERY+variant+1;
                crystal.stageBanner=GameCore.BANNER_TIME*(1f-(.05f+frame*.10f));
                shot(dir,"81-crystal-"+variant+"-"+frame,crystal,L,w,h,ss);
            }
        }

        if(wanted("81-mushroom-")) {
            GameCore mushroom=new GameCore(new Mem(),147L);mushroom.startGame();mushroom.landBlend=1f;
            for(int variant=0;variant<3;variant++)for(int frame=0;frame<9;frame++) {
                mushroom.stage=3*Boss.EVERY+variant+1;
                mushroom.stageBanner=GameCore.BANNER_TIME*(1f-(.05f+frame*.10f));
                shot(dir,"81-mushroom-"+variant+"-"+frame,mushroom,L,w,h,ss);
            }
        }

        caveFrames(dir,L,w,h,ss);
        cartFrames(dir,L,w,h,ss);
        miningFrames(dir,L,w,h,ss);
        squishyEntryFrames(dir,L,w,h,ss);
        townFrames(dir,L,w,h,ss);
        slimeFightFrames(dir,L,w,h,ss);
        companionFrames(dir,L,w,h,ss);
        scoreResetFrames(dir,L,w,h,ss);
        onboardingFrames(dir,L,w,h,ss);
        starterFrames(dir,L,w,h,ss);
        modeFrames(dir,L,w,h,ss);
        duckFrames(dir,L,w,h,ss);
        survivalFrames(dir,L,w,h,ss);
        versusFrames(dir,L,w,h,ss);
        timeAttackFrames(dir,L,w,h,ss);
        gatorSilhouetteFrames(dir,L,w,h,ss);
        alligatorFrames(dir,L,w,h,ss);
        swampEnemyFrames(dir,L,w,h,ss);
        bossImpactFrames(dir,L,w,h,ss);
        slimeSprayFrames(dir,L,w,h,ss);
        slimeDeathFrames(dir,L,w,h,ss);
        slimeOpeningFrames(dir,L,w,h,ss);
        if(wantedGroup("206-")) {
            GameCore retry=TestBossRetry.fight(L,Boss.SLIME);
            TestBossRetry.lose(retry,L);TestBossRetry.ready(retry,L);
            shot(dir,"206-retry-choice",retry,L,w,h,ss);
            for(int kind=0;kind<Boss.COUNT;kind++) {
                GameCore skit=TestBossRetry.fight(L,kind);
                TestBossRetry.lose(skit,L);TestBossRetry.ready(skit,L);
                skit.clock=1.7f+kind*.17f;
                shot(dir,"206-retry-skit-"+kind,skit,L,w,h,ss);
                if(kind==Boss.RAINBOW_CAT) {
                    skit.preferences.skin=Skins.HALLOWEEN;
                    shot(dir,"206-retry-skit-halloween",skit,L,w,h,ss);
                }
            }
            retry.bossRetry.retry(retry);retry.boss.intro=0;
            TestBossRetry.lose(retry,L);TestBossRetry.ready(retry,L);retry.bossRetry.end(retry);
            for(int i=0;i<60;i++)retry.update(1f/60,L);
            shot(dir,"206-retried-summary",retry,L,w,h,ss);
            retry.toTitle();retry.highScoreScreen.show(retry);retry.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
            shot(dir,"206-retried-high-scores",retry,L,w,h,ss);
            retry.highScoreScreen.selected=0;
            shot(dir,"206-retried-record",retry,L,w,h,ss);
        }
        rainbowFrames(dir,L,w,h,ss);
        skinFrames(dir,L,w,h,ss);
        clearSpreadFrames(dir,L,w,h,ss);
        clearIntensityFrames(dir,L,w,h,ss);
        diveStarFrames(dir,L,w,h,ss);
        enemyClearFrames(dir,L,w,h,ss);
        fastEnemyFrames(dir,L,w,h,ss);
        gatorCompanionFrames(dir,L,w,h,ss);
        adventureTitleFrames(dir,L,w,h,ss);
        survivalEndingFrames(dir,L,w,h,ss);

        GameCore updateNews=new GameCore(TestUpdatePrompt.updated(),204);
        updateNews.update(.16f,L);
        shot(dir,"204-update-enter",updateNews,L,w,h,ss);
        updateNews.update(.2f,L);
        shot(dir,"204-update-prompt",updateNews,L,w,h,ss);
        TestUpdatePrompt.tap(updateNews,L,2);updateNews.update(.4f,L);
        shot(dir,"204-update-skip",updateNews,L,w,h,ss);
        updateNews.updatePrompt.rearm(updateNews);updateNews.update(.4f,L);
        TestUpdatePrompt.tap(updateNews,L,1);updateNews.update(.4f,L);
        updateNews.releaseNotes.update(ReleaseTransition.DURATION+ReleaseNotes.PAGE_TIME,L);
        shot(dir,"204-update-read",updateNews,L,w,h,ss);
        GameCore waitingNews=new GameCore(TestUpdatePrompt.updated(),204);
        waitingNews.settingsOpen=true;waitingNews.settingsPage=1;waitingNews.settingsTab=SettingsUi.PROGRESS;
        waitingNews.preferences.panelEntrance=1f;waitingNews.update(.4f,L);
        shot(dir,"204-update-settings-waits",waitingNews,L,w,h,ss);
        waitingNews.settingsOpen=false;waitingNews.onboarding.saved=waitingNews.onboarding.savedPowers=0;
        waitingNews.starter.begin(waitingNews);waitingNews.update(Starter.READY+.2f,L);
        shot(dir,"204-update-starter-waits",waitingNews,L,w,h,ss);

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
            book.releaseNotes.touch(book,L,L.w*.97f,book.releaseNotes.edgeY(L));
            book.releaseNotes.update(ReleaseNotes.PAGE_TIME*.25f,L);
            shot(dir,"103-release-navigation-out",book,L,w,h,ss);
            book.releaseNotes.update(ReleaseNotes.PAGE_TIME*.5f,L);
            shot(dir,"103-release-navigation-in",book,L,w,h,ss);
            book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
            shot(dir,"103-release-navigation-settled",book,L,w,h,ss);
            book.releaseNotes.select(0,ReleaseChange.ITEMS[0].length-1,L);
            book.releaseNotes.update(ReleaseNotes.PAGE_TIME,L);
            shot(dir,"103-release-navigation-last",book,L,w,h,ss);
        }
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
        book.releaseNotes.touch(book,L,L.w*.14f+book.releaseNotes.demo.enemyCentreX(bookA),bookY);
        book.releaseNotes.update(0.1f,L);
        book.releaseNotes.touch(book,L,L.w*.14f+book.releaseNotes.demo.enemyCentreX(bookB),bookY);
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
        if (wanted("versus-music")) Wav.write(new File(dir,"versus-synth-fanfare.wav"),VersusMusic.clip(),Sfx.RATE);

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

        Check.Mem freshStore=new Check.Mem();freshStore.tutorials=freshStore.powerTutorials=0;freshStore.rosterState=0;
        GameCore freshTitle=new GameCore(freshStore,164);
        for(int beat=0;beat<3;beat++) {
            if(beat>0)step(freshTitle,L,.6f);
            shot(dir,"164-first-title-"+beat,freshTitle,L,w,h,ss);
        }
        freshTitle.beginStart();
        for(int frame=0;frame<5;frame++) {
            float at=new float[]{0,.20f,TitleStart.SQUEEZE,.46f,.72f}[frame];
            step(freshTitle,L,Math.max(0,at-freshTitle.starter.age));
            shot(dir,"164-start-transition-"+frame,freshTitle,L,w,h,ss);
        }
        step(freshTitle,L,Math.max(0,TitleStart.TRANSITION-freshTitle.starter.age));
        shot(dir,"164-first-selection",freshTitle,L,w,h,ss);
        GameCore returningTitle=new GameCore(store,164);returningTitle.clock=2.4f;
        shot(dir,"164-returning-title",returningTitle,L,w,h,ss);

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
            welcome.startGame();
            welcome.stage = Boss.EVERY;
            Interlude.awardBossPrize(welcome, boss);
            Interlude.enterBonus(welcome, L);
            welcome.bonusTimer = BossCollect.REVEAL_TIME - .55f;
            shot(dir, "95-boss-friend-travel-" + boss, welcome, L, w, h, ss);
            welcome.bonusTimer = BossCollect.REVEAL_TIME - 1.8f;
            shot(dir, "95-boss-friend-heart-" + boss, welcome, L, w, h, ss);
            welcome.bonusTimer = BossCollect.REVEAL_TIME - 2.2f;
            shot(dir, "95-boss-friend-" + boss, welcome, L, w, h, ss);
            welcome.bonusTimer = .42f;
            shot(dir, "95-boss-friend-return-" + boss, welcome, L, w, h, ss);
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

        // The send-off: the squishy swelling out of the badge, then arriving at its run home and
        // finally holding there as the home bubble fades in.
        GameCore c19 = new GameCore(store, 81L);
        step(c19, L, 1.4f);
        c19.tapKey(2, L);
        step(c19, L, Launch.TIME * Launch.POP * 0.75f);
        System.out.printf("send-off pop: who=%d left=%.2f progress=%.2f%n", c19.launchWho,
                c19.launchT, Launch.progress(c19));
        shot(dir, "42-sendoff-pop", c19, L, w, h, ss);
        step(c19, L, Launch.TIME * (Launch.HOME - Launch.POP * 0.75f));
        System.out.printf("send-off home: progress=%.2f state=%d%n", Launch.progress(c19),
                c19.state);
        shot(dir, "43-sendoff-home", c19, L, w, h, ss);
        step(c19,L,Launch.TIME*(1f-Launch.HOME)*.62f);
        shot(dir,"43b-sendoff-bubble",c19,L,w,h,ss);
        step(c19,L,Math.max(0f,c19.launchT-DT));
        shot(dir,"43c-sendoff-settled",c19,L,w,h,ss);
        step(c19,L,DT*2f);
        shot(dir,"43d-sendoff-play",c19,L,w,h,ss);

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
        near.y = Onboarding.rescueTriggerY(L);
        c4.onboarding.saved&=~(1<<(TutorialSpeech.RESCUE+9));
        c4.update(DT, L);
        TestOnboarding.acknowledge(c4,L);
        c4.onboarding.age = .2f;
        shot(dir, "199-push-lesson", c4, L, w, h, ss);
        c4.onboarding.age = .6f;
        shot(dir, "199-push-lesson-swipe", c4, L, w, h, ss);
        c4.onboarding.age = 1.2f;
        shot(dir, "199-push-lesson-lift", c4, L, w, h, ss);
        near.y = lessonY;
        GameCore reset = new GameCore(store, 18L);
        reset.settingsOpen = true; reset.settingsPage = 1; reset.settingsTab = SettingsUi.PROGRESS;
        shot(dir, "200-progress-settings", reset, L, w, h, ss);
        c4.onboarding.rescued(c4);

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

        starPickupFrames(dir, L, w, h, ss);

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
        cs.stars.collected = (1 << (cs.stars.total() - 1)) - 1;
        cs.stars.x = cs.stars.starX(6, L);
        cs.stars.vx = L.w * StarPath.MAX_VX * 0.7f;
        shot(dir, "54c-stars-wake", cs, L, w, h, ss);
        cs.stars.wins = StarPath.MAX_DIFFICULTY;
        cs.stars.make(new java.util.Random(83L));
        cs.stars.collected = 0b111;
        cs.stars.vx = 0f;
        cs.stars.x = cs.stars.starX(6, L);
        shot(dir, "54d-stars-max-bends", cs, L, w, h, ss);
        starExitFrames(dir,cs,L,w,h,ss,false);
        // The failed-course handoff: full report, midpoint of the same cross-fade used by the
        // winning parade, then the last trace over the play field beneath it.
        for(int frame=0;frame<3;frame++) {
            cs.stars.timer=frame==0 ? StarPath.REPORT-0.01f
                    : frame==1 ? Parade.HANDOFF*.5f : 0.01f;
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
        cw.stars.collected = (1 << (cw.stars.total() - 1)) - 1;
        cw.stars.timer = StarPath.FLY + StarPath.EXIT + StarPath.REPORT - 3.05f;
        // Steered onto the last star frame by frame: where the course has scrolled to is the only
        // thing that knows where that star is.
        for (int i = 0; i < 60 * 6 && !cw.stars.won; i++) {
            cw.stars.x = cw.stars.starX(cw.stars.total() - 1, L);
            cw.update(DT, L);
        }
        System.out.printf("stars won: star=%d prize=%s new=%s parade=%.2f%n", cw.stars.winStar,
                cw.prize >= 0 ? Collect.NAME[cw.prize] : "none", cw.prizeNew, cw.paradeTimer);
        step(cw, L, 0.28f);
        shot(dir, "55-stars-won", cw, L, w, h, ss);
        step(cw, L, 1.1f);
        shot(dir, "56-stars-victory", cw, L, w, h, ss);
        starExitFrames(dir,cw,L,w,h,ss,true);
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

        // NINJA with the instructional finger and its sparkle trail.
        GameCore c10 = new GameCore(store, 43L);
        c10.startGame();
        c10.score = 2400;
        step(c10, L, 1.9f);
        Power fl = new Power();
        fl.glyph = 5;
        fl.effect = Power.NINJA;
        fl.y = L.playTop + 100f;
        fl.x = L.w * 0.5f;
        c10.power = fl;
        c10.tapPower(c10.power.x, c10.power.y, L);
        step(c10, L, 1.6f);
        System.out.printf("ninja hint: showing=%s demo=(%.0f,%.0f) sparkles=%d%n",
                c10.showNinjaHint(), c10.demoX, c10.demoY, c10.particles.size());
        shot(dir, "18-ninja-hint", c10, L, w, h, ss);

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
        blade.effect = Power.NINJA;
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

        // A mass clear during NINJA, followed by its short replacement burst.
        GameCore refill = new GameCore(store, 711L);
        refill.startGame();
        refill.stage = 13;
        refill.startFrenzy(Power.NINJA, L);
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
        shot(dir,"33h-kids-steamer-unlimited",kids,L,w,h,ss);
        for(int i=0;i<Steamer.KIDS_GOAL*2;i++)kids.tapBonus(kids.steamer.wanted());
        shot(dir,"33h-kids-steamer-ready",kids,L,w,h,ss);
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
        c6.openSettings();c6.preferences.updatePanel(c6,PlayerSettings.PANEL_TIME);
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
        if(wanted("182-"))for(int kind=0;kind<Boss.COUNT;kind++)for(float at:new float[]{.45f,.83f,1f}) {
            GameCore c=TestTimeAttack.start(kind);
            c.stageBanner=0;
            step(c,L,c.boss.introDuration()*at);
            shot(dir,"182-boss-header-"+kind+"-"+Math.round(at*100),c,L,w,h,ss);
        }
        if(wanted("182-"))for(int pose=0;pose<3;pose++) {
            GameCore hero=TestTimeAttack.start(Boss.SLIME);hero.stageBanner=0;
            step(hero,L,hero.boss.introDuration()+.1f);
            Softbody body=hero.boss.body;
            if(pose==1)body.squash(.7f);
            if(pose==2)body.pull(body.centreX()+body.radiusX()*1.4f,body.centreY()+body.radiusY(),1f);
            if(pose>0)for(int frame=0;frame<18;frame++)body.update(DT);
            shot(dir,"182-super-slime-"+pose,hero,L,w,h,ss);
        }
        if(wanted("182-"))for(float health:new float[]{.75f,.25f})for(float elapsed:new float[]{0f,.3f,.9f}) {
            GameCore slime=TestBoss.slimeAfterDrag(L,health);
            for(float time=0;time<elapsed;time+=DT)slime.boss.update(DT,L,slime.rnd);
            shot(dir,"182-slime-recovery-"+Math.round(health*100)+"-"+Math.round(elapsed*100),slime,L,w,h,ss);
        }
        if(wanted("195-slime-boip")) {
            GameCore boip=TestBoss.enterBoss(L,Boss.SLIME,195);boip.stageBanner=0;
            boip.boss.etype[0]=Boss.E_GLOB;boip.boss.elife[0]=Boss.GLOB_TIME;
            boip.update(DT,L);
            String[] poses={"rest","first","gap","second"};float[] times={-1f,.02f,.12f,Boss.SLIME_BOIP_GAP+.02f};
            for(int i=0;i<times.length;i++) {
                boip.boss.slimeBoipTime=times[i];
                shot(dir,"195-slime-boip-"+poses[i],boip,L,w,h,ss);
            }
        }
        // The arrival card, part way in.
        GameCore ci = toBoss(L, Boss.SLIME, 501L, false);
        step(ci, L, Boss.INTRO * 0.45f);
        System.out.printf("boss intro: %s, %.2f through%n", ci.boss.name(ci.preferences.skin),
                ci.boss.introProgress());
        shot(dir, "59-boss-intro", ci, L, w, h, ss);

        // Each of the six, mid-fight, with its window open so the ornament is showing the thing it
        // is asking for.
        String[] tag = {"60-boss-slime", "70-boss-dark-divide", "76-boss-octopulse",
                "77-boss-fly-agaric", "168-alligator-overview"};
        for (int k = 0; k < Boss.COUNT; k++) {
            if(k==Boss.ALLIGATOR) {
                GameCore gator=TestAlligator.fight(L);gator.stageBanner=0;
                TestAlligator.ready(gator,L);shot(dir,k<tag.length?tag[k]:"boss-"+WorldCatalog.BOSSES[k].analytics,gator,L,w,h,ss);continue;
            }
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
            System.out.printf("boss %s: hp %.0f/%.0f, open=%s, elements=%d%n", c.boss.name(c.preferences.skin),
                    c.boss.hp, c.boss.hpMax, c.boss.open(), liveElems(c.boss));
            shot(dir, k<tag.length?tag[k]:"boss-"+WorldCatalog.BOSSES[k].analytics, c, L, w, h, ss);

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
        pulseDemo.companion.begin(6);
        for(int feedback=Boss.OCTO_LATE;feedback<=Boss.OCTO_WRONG;feedback++) {
            pulseDemo.boss.octoFeedback=feedback;
            shot(dir,"octopulse-feedback-"+feedback,pulseDemo,L,w,h,ss);
        }
        pulseDemo.boss.octoFeedback=Boss.OCTO_LATE;
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
        cj.openSettings();cj.preferences.updatePanel(cj,PlayerSettings.PANEL_TIME);
        step(cj, L, 0.3f);
        System.out.printf("stage jump: on stage %d, boss %s%n", cj.stage,
                Skins.bossName(Skins.DEFAULT,cj.boss.kind));
        shot(dir, "67-settings-stage-jump", cj, L, w, h, ss);
        cj.settingsPage=1;cj.settingsTab=SettingsUi.GENERAL;cj.devInfiniteLives=true;
        shot(dir,"151-settings-infinite-lives",cj,L,w,h,ss);
        GameCore settings=new GameCore(new Mem(),819L);
        PlayerSettings.open(settings);
        settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME*.5f);
        shot(dir,"120f-settings-entering",settings,L,w,h,ss);
        settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME*.5f);
        PlayerSettings.close(settings);settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME*.5f);
        shot(dir,"120f-settings-exiting",settings,L,w,h,ss);
        settings.closeSettings();PlayerSettings.open(settings);settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME);
        for(boolean kids:new boolean[]{false,true}) {
            settings.preferences.kids=kids;
            shot(dir,"120g-settings-kids-"+(kids?"young":"old"),settings,L,w,h,ss);
        }
        settings.preferences.kids=false;
        for(int direction=0;direction<2;direction++) {
            settings.preferences.toggleKids(settings);
            float start=settings.clock;
            for(int beat=0;beat<=4;beat++) {
                settings.clock=start+PlayerSettings.KIDS_TOGGLE_TIME*beat/4f;
                shot(dir,"120h-settings-kids-toggle-"+direction+"-"+beat,settings,L,w,h,ss);
            }
            settings.clock+=.01f;
        }
        settings.preferences.load(PlayerSettings.DEFAULT);
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
        for(int beat=0;beat<=5;beat++) {
            settings.clock=beat*.8f;
            shot(dir,"120d-settings-share-skit-"+beat,settings,L,w,h,ss);
        }
        for(int beat=0;beat<5;beat++) {
            settings.clock=.325f+beat*.18f;
            shot(dir,"120e-settings-rate-wave-"+beat,settings,L,w,h,ss);
        }
        for(int action:new int[]{PlayerSettings.SHARE,PlayerSettings.RATE}) {
            settings.preferences.animatedAction=action;settings.preferences.externalAt=settings.clock;
            settings.clock+=.18f;
            shot(dir,"120c-settings-social-tap-"+action,settings,L,w,h,ss);
        }
        settings.settingsPage=1;
        for(int tab=0;tab<4;tab++) {
            settings.settingsTab=tab;
            shot(dir,"121-settings-disabled-"+tab,settings,L,w,h,ss);
        }
        settings.startGame();settings.openSettings();settings.preferences.updatePanel(settings,PlayerSettings.PANEL_TIME);
        for(int tab=0;tab<4;tab++) {
            settings.settingsTab=tab;
            shot(dir,"122-settings-active-"+tab,settings,L,w,h,ss);
        }
        settings.resetProgressArmed=true;
        shot(dir,"122a-settings-reset-progress-confirm",settings,L,w,h,ss);
        settings.resetProgressArmed=false;

        for (int kind = 0; kind < Boss.COUNT; kind++) {
            if (kind == Boss.SPLITTER) continue; // Its seven supernova frames include the impact.
            GameCore death = toBoss(L, kind, 590L + kind, true);
            death.boss.damage(death.boss.hp);
            step(death, L, death.boss.deathImpactTime() + .04f);
            System.out.printf("boss death %s: shake %.2f%n", death.boss.name(death.preferences.skin), death.shake);
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
    private static boolean wantedGroup(String prefix) {
        if(only==null)return true;
        for(String filter:only)if(prefix.startsWith(filter.trim()) || filter.trim().startsWith(prefix))return true;
        return false;
    }
    static boolean wanted(String name) {
        if (only == null) return true;
        for (String o : only) {
            if (name.startsWith(o.trim())) return true;
        }
        return false;
    }

    private static void starPickupFrames(File dir, Layout L, int w, int h, int ss)
            throws Exception {
        if (!wantedGroup("202-stars-") && !wantedGroup("205-stars-")) return;
        GameCore c = new GameCore(new Mem(), 202L);
        c.startGame();c.state = GameCore.BONUS;c.starBonus = true;
        StarPath q = c.stars;q.make(new java.util.Random(202L));q.begin(0, L);
        for (int target : new int[]{1, 6, q.total()}) {
            for (int frame = 0; frame < 600 && q.count() < target; frame++) {
                int next = 0;
                while (next < q.total() && (q.collected & (1 << next)) != 0) next++;
                q.x = q.starX(next, L);c.update(DT, L);
            }
            step(c, L, .08f);
            shot(dir, "202-stars-" + (target == 1 ? "single" : target == 6 ? "rapid" : "final"),
                    c, L, w, h, ss);
            if (target == 6) {
                shot(dir, "205-stars-overlap", c, L, w, h, ss);
                step(c, L, .12f);
                shot(dir, "205-stars-scroll", c, L, w, h, ss);
            }
        }
        step(c, L, .30f);
        shot(dir, "202-stars-final-fade", c, L, w, h, ss);
        step(c, L, StarPath.WIN_HOLD - StarPath.EXIT - .38f + .15f);
        shot(dir, "202-stars-final-blast", c, L, w, h, ss);
        q.begin(0, L);c.time=2f;
        q.collected = q.fullMask() & ~1;
        q.timer = StarPath.REPORT + StarPath.EXIT + .03f;
        q.burst[q.total()-1] = 1f;
        step(c, L, .08f);
        shot(dir, "205-stars-exit", c, L, w, h, ss);
        step(c, L, .14f);
        shot(dir, "205-stars-exit-scroll", c, L, w, h, ss);
    }

    private static void starExitFrames(File dir,GameCore c,Layout L,int w,int h,int ss,boolean won)
            throws Exception {
        float timer=c.stars.timer,win=c.stars.winT,bonus=c.bonusTimer,clock=c.clock;
        float[] phases={.03f,.20f,.45f,.70f,.92f,.995f};
        for(int frame=0;frame<phases.length;frame++) {
            float remaining=StarPath.EXIT*(1f-phases[frame]);
            if(won) c.stars.winT=remaining;else c.stars.timer=StarPath.REPORT+remaining;
            c.bonusTimer=c.stars.timer;c.clock=clock+StarPath.EXIT*phases[frame];
            shot(dir,"129-stars-"+(won?"complete":"incomplete")+"-blast-"+frame,c,L,w,h,ss);
        }
        c.stars.timer=timer;c.stars.winT=win;c.bonusTimer=bonus;c.clock=clock;
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

    private static void slimeShot(File dir, String name, SlimeFight f, Layout L, int w, int h,
            int ss) throws Exception {
        if (!wanted(name)) return;
        RasterPainter p = new RasterPainter(w, h, ss);
        p.clear(0xFF000000);
        RasterPainter.clearFit();
        SlimeFightScreen.draw(p, f, L);
        File file = new File(dir, name + ".png");
        int[] pixels = p.resolve();
        if (crop == null) Png.write(file, pixels, w, h);
        else {
            int x0 = Math.max(0, Math.min(w - 1, (int) (crop[0] * w)));
            int y0 = Math.max(0, Math.min(h - 1, (int) (crop[1] * h)));
            int x1 = Math.max(x0 + 1, Math.min(w, (int) (crop[2] * w)));
            int y1 = Math.max(y0 + 1, Math.min(h, (int) (crop[3] * h)));
            int cw = x1 - x0, ch = y1 - y0;
            int[] sub = new int[cw * ch];
            for (int y = 0; y < ch; y++)
                System.arraycopy(pixels, (y0 + y) * w + x0, sub, y * cw, cw);
            Png.write(file, sub, cw, ch);
        }
        System.out.printf("  wrote %-25s FUN=%d throws=%d hits=%d dodges=%d%n",
                file.getName(), f.fun, f.throwsMade, f.hits, f.dodges);
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
                "achievement", "game-start", "stage-clear", "power-clear", "ninja-swish-1", "zap",
                "collect", "star", "course-start", "tally", "parade-join", "game-over", "boss-laugh", "boss-damage", "boss-split", "bolt-pop", "divide-damage", "divide-split",
                "divide-boing-heavy", "divide-boing-medium", "divide-boing-light", "roster-join", "divide-deactivate", "shield-bounce", "slime-damage", "octo-cue", "octo-lock",
                "taunt-slime", "taunt-divide", "taunt-octopus", "taunt-mushroom", "taunt-gator", "bolt-death",
                "mushroom-shake", "mushroom-spore", "linked-thud", "shuffle-blip", "debuff-down", "slime-cover", "slime-release", "land-shuffle", "ui-bloop", "blast-off", "divide-supernova", "octo-wave", "cave-rumble", "cave-crash", "cave-ambush", "cave-sink", "mining-cheer", "cart-roll", "cart-squeal", "cart-tumble", "octo-damage", "score-reset-confirm", "score-reset-brush", "ninja-swish-2", "ninja-swish-3", "ninja-combo", "survival-game-over", "gator-parry", "gator-prompt-1", "gator-prompt-2", "gator-prompt-3", "gator-splash", "gator-taunt-1-1", "gator-taunt-1-2", "gator-taunt-1-3", "gator-taunt-2-1", "gator-taunt-2-2", "gator-taunt-2-3", "slime-squish-1", "slime-squish-2", "slime-squish-3", "slime-squish-4", "slime-squish-5", "slime-squish-6", "slime-vulnerable", "slime-drag-boip"};
        int peak = 0;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            peak = Math.max(peak, max);
            if(id==Sfx.NINJA_COMBO) {
                pcm=pcm.clone();
                for(int i=0;i<pcm.length;i++)pcm[i]=(short)(pcm[i]*Sfx.NINJA_COMBO_GAIN);
            }
            String name=id<names.length?names[id]:id==Sfx.bossTauntId(Boss.RAINBOW_CAT)?"taunt-cat"
                    :id==Sfx.CAT_LASER?"cat-laser":id>=Sfx.CAT_FLURRY_0?"cat-flurry-"+(id-Sfx.CAT_FLURRY_0)
                    :id>=Sfx.CAT_MEOW_0?"cat-meow-"+(id-Sfx.CAT_MEOW_0):"effect-"+id;
            Wav.write(new File(sfxDir, name + ".wav"), pcm, Sfx.RATE);
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
        Wav.write(new File(sfxDir,"bgm-versus-synth.wav"),VersusMusic.clip(),Sfx.RATE);
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
