package com.dddumpling.game;

final class TestAlligator extends Check {
    private static void endingPresentation(Layout L) {
        group("gator victory and water exit");
        for(int arm=0;arm<2;arm++) {
            float minX=L.w,maxX=0,minY=L.h,maxY=0;
            for(int frame=0;frame<120;frame++) {
                float x=AlligatorAtmosphere.waveX(L,arm,frame/60f);
                float y=AlligatorAtmosphere.waveY(L,arm,frame/60f);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            check("victory arm sweeps broadly without leaving the screen "+arm,
                    maxX-minX>L.w*.22f && maxY-minY>(L.dangerY-L.playTop)*.26f
                    && minX>=L.w*.09f && maxX<=L.w*.91f);
        }
        for(int side:new int[]{-1,1}) {
            float minX=L.w,maxX=0,minY=L.h,maxY=0;
            for(int frame=0;frame<180;frame++) {
                float x=AlligatorAtmosphere.tailWaveX(L,side,frame/60f);
                float y=AlligatorAtmosphere.tailWaveY(L,frame/60f);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            check("victory tail sweeps visibly within the lower play area "+side,
                    maxX-minX>L.w*.30f && maxY-minY>(L.dangerY-L.playTop)*.24f
                    && minX>=L.w*.14f && maxX<=L.w*.86f && minY>L.playTop && maxY<L.dangerY);
        }
        check("defeated pose holds before the fall",AlligatorAtmosphere.offset(L,.5f)==0);
        check("fall accelerates into the water",AlligatorAtmosphere.offset(L,1.6f)-AlligatorAtmosphere.offset(L,1.2f)
                >AlligatorAtmosphere.offset(L,1.2f)-AlligatorAtmosphere.offset(L,.8f));
        check("body is submerged before the last splash fades",
                AlligatorAtmosphere.offset(L,AlligatorAtmosphere.SUBMERGED_AT)>AlligatorAtmosphere.waterY(L));
        GameCore c=fight(L),same=fight(L);c.lives=same.lives=1;
        c.takeHit(L.w*.5f,L);same.takeHit(L.w*.5f,L);
        Boss b=c.bossVictory;float tip=b.alligator.tipX,age=b.age,cx=b.body.centreX(),tail=b.alligator.tailPosition;
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);
        c.clock=1;c.deathT=c.deathDuration()-1;
        BossVictory.draw(p,c,L);
        check("victory art leaves the retained fight and gameplay RNG untouched",
                b.alligator.tipX==tip && b.alligator.tailPosition==tail && b.age==age && b.body.centreX()==cx
                && c.rnd.nextLong()==same.rnd.nextLong());
    }
    static GameCore fight(Layout L) {
        GameCore c=new GameCore(TestTimeAttack.store(),168,true);c.startGame();
        c.enemies.clear();c.power=null;c.modeLeft=0;
        c.stage=Lands.firstStage(Lands.SWAMP)+Boss.EVERY-1;
        c.boss.begin(Boss.ALLIGATOR,c.stage,c.rnd,c.playRosterFull());
        c.boss.intro=0;c.boss.update(DT,L,c.rnd);
        return c;
    }
    static void advance(GameCore c,Layout L,float seconds) {
        for(float t=0;t<seconds;t+=DT)c.boss.update(DT,L,c.rnd);
    }
    static void ready(GameCore c,Layout L) {
        for(int n=0;n<360 && (c.boss.alligator.daze>0 || c.boss.alligator.limb<0 || c.boss.alligator.blocked || c.boss.alligator.attack<.3f);n++)c.boss.update(DT,L,c.rnd);
    }
    static void block(GameCore c,Layout L) {
        ready(c,L);Alligator a=c.boss.alligator;
        a.touch(c,L,0,168,a.tipX-L.w*.12f,a.tipY);
        a.touch(c,L,1,168,a.tipX+L.w*.12f,a.tipY);
    }
    static void exchange(GameCore c,Layout L) {block(c,L);c.boss.press(0,c.rnd,L);}
    static void longOpening(GameCore c,Layout L) {
        for(int n=0;n<Alligator.ROUNDS_FOR_DAZE && !c.boss.alligator.longWindow;n++)exchange(c,L);
    }
    static void guardFeedback(Layout L) {
        GameCore c=fight(L);ready(c,L);Alligator a=c.boss.alligator;AlligatorGuard g=a.guard;
        float y=L.playTop+(L.dangerY-L.playTop)*.82f;
        a.touch(c,L,0,19,L.w*.30f,y);a.touch(c,L,2,19,L.w*.31f,y);
        check("tiny movement has no defense cue",!g.active());
        int score=c.score;
        a.touch(c,L,2,19,L.w*.45f,y);
        check("missed swipe shows default shield without awarding defense",g.active() && !g.success && g.primary==AlligatorGuard.DEFAULT && a.roundStreak==0 && c.score==score);
        a.touch(c,L,2,19,L.w*.45f,y+L.w*.02f);
        check("shield follows fine curved movement between swipe thresholds",Math.abs(g.angle-90)<.01f && g.trailY[g.points-1]==y+L.w*.02f);
        a.touch(c,L,2,20,L.w*.8f,y);
        check("other finger cannot move shield",g.trailX[g.points-1]==L.w*.45f);
        a.touch(c,L,3,19,L.w*.45f,y);int count=g.points;
        a.touch(c,L,2,19,L.w*.7f,y);
        check("cancelled gesture cannot add shield trail",g.points==count);
        advance(c,L,1);check("released swipe effect fades out",!g.active());
        c=fight(L);c.companion.begin(6);block(c,L);g=c.boss.alligator.guard;
        check("successful defense strengthens companion-colored effect",g.success && g.duration>AlligatorGuard.SWIPE_TIME
                && g.primary==Collect.BODY[6] && g.secondary==Collect.ACCENT[6]);
        float age=g.age;c.paused=true;c.update(.2f,L);check("pause freezes defense cue",g.age==age);c.paused=false;
        check("similar default colors use yellow",AlligatorGuard.successColor(AlligatorGuard.DEFAULT,false)==Draw.YELLOW
                && AlligatorGuard.successColor(Collect.BODY[7],false)==Draw.YELLOW);
        check("distinct companion colors remain",AlligatorGuard.successColor(0xFFB79BF0,false)==0xFFB79BF0);
        for(int i=0;i<100;i++)g.track(L.w*.25f+i,y);
        check("long swipe trail has bounded storage",g.points==AlligatorGuard.POINTS);
        c.boss.leave();check("leaving clears defense cue and trail",!g.active() && g.points==0);
    }
    static void companionProps(Layout L) {
        group("companion reaction props follow parry pose");
        GameCore c=fight(L);c.companion.begin(6);block(c,L);
        AlligatorGuard g=c.boss.alligator.guard;
        final java.util.ArrayList<float[]> rods=new java.util.ArrayList<>();
        Painter p=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(proxy,method,args)->{
                    if(method.getName().equals("polyline") && ((float[])args[0]).length>6
                            && (Integer)args[1]==Draw.ROSE)rods.add(((float[])args[0]).clone());
                    return null;
                });
        float[] normalized=null;
        for(float at:new float[]{0,.08f,.66f,1.5f}) {
            g.flightAge=at;c.companion.reaction=RunCompanion.BOSS_HIT;
            rods.clear();RunCompanion.draw(p,c,L);
            check("no foam rod remains at home during parry "+at,rods.isEmpty());
            g.drawCompanion(p,c,L);
            check("exactly one carried foam rod "+at,rods.size()==1);
            if(rods.size()==1) {
                float[] rod=rods.get(0);float radius=RunCompanion.radius(c,L)*g.companionScale();
                float[] pose=new float[rod.length];
                for(int n=0;n<rod.length;n+=2) {
                    pose[n]=(rod[n]-g.companionX(L))/radius;
                    pose[n+1]=(rod[n+1]-g.companionY(L))/radius;
                }
                if(normalized==null)normalized=pose;
                boolean follows=true;
                for(int n=0;n<pose.length;n++)follows &= Math.abs(pose[n]-normalized[n])<.001f;
                check("whole foam rod follows companion position and scale "+at,follows);

            }
        }
        c.companion.clock=.12f;rods.clear();g.drawCompanion(p,c,L);
        float[] bent=rods.get(0);int tip=bent.length-2;
        float dx=bent[tip]-bent[0],dy=bent[tip+1]-bent[1];
        float cross=(bent[12]-bent[0])*dy-(bent[13]-bent[1])*dx;
        check("rod visibly bends instead of rotating rigidly",Math.abs(cross)>20);
        c.companion.clock=.32f;rods.clear();g.drawCompanion(p,c,L);
        float[] waved=rods.get(0);
        check("rod waves while its grip stays attached",waved[0]==bent[0] && waved[1]==bent[1]
                && Math.hypot(waved[tip]-bent[tip],waved[tip+1]-bent[tip+1])>10);
        g.reset();rods.clear();RunCompanion.draw(p,c,L);
        check("foam rod returns to ordinary companion rendering",rods.size()==1);
        c.onboarding.companionTravel=1;rods.clear();RunCompanion.draw(p,c,L);
        check("hidden companion leaves no floating props",rods.isEmpty());
    }

    static void parryCompanion(Layout L) {
        group("companion parry travel and hanging arms");
        for(int limb=0;limb<3;limb++) {
            GameCore c=fight(L);c.companion.begin(6);Alligator a=c.boss.alligator;
            a.next=limb;a.timer=0;block(c,L);AlligatorGuard g=a.guard;
            check("parry starts at companion home "+limb,g.flying() && g.companionX(L)==RunCompanion.x(L)
                    && g.companionY(L)==RunCompanion.y(L) && g.pendingImpact);
            check("parry begins at normal companion size "+limb,g.companionScale()==1);
            float x=RunCompanion.x(L)+g.toX*L.w,y=RunCompanion.y(L)+g.toY*L.h;
            g.update(AlligatorGuard.TRAVEL*.5f);
            check("companion grows smoothly during flight "+limb,g.companionScale()>1 && g.companionScale()<1.68f);
            g.update(AlligatorGuard.TRAVEL*.5f);
            check("companion reaches contact pose "+limb,Math.abs(g.companionX(L)-x)<.01f && Math.abs(g.companionY(L)-y)<.01f);
            check("companion edge meets impact without overlapping limb centre "+limb,
                    Math.abs(Math.hypot(x-g.hitX,y-g.hitY)-RunCompanion.radius(c,L)*g.companionScale())<.01f
                    && Math.hypot(x-g.limbX,y-g.limbY)>RunCompanion.radius(c,L)*g.companionScale()+L.w*.04f);
            check("limb stays at contact until companion arrives "+limb,
                    Math.abs(a.drawnX(L,limb)-g.limbX)<.01f && Math.abs(a.drawnY(L,limb)-g.limbY)<.01f);
            check("contact starts companion color pulse "+limb,Math.abs(g.colorPulse()-.65f)<.001f);
            float before=g.flightAge;c.paused=true;c.update(.2f,L);c.paused=false;
            check("pause freezes parry excursion "+limb,g.flightAge==before);
            g.update(.14f);
            check("contact color fades smoothly "+limb,g.colorPulse()>0 && g.colorPulse()<.65f);
            float backX=g.companionX(L)-x,backY=g.companionY(L)-y;
            check("impact bounces companion back along its approach "+limb,
                    backX*(RunCompanion.x(L)-x)+backY*(RunCompanion.y(L)-y)>0
                    && Math.hypot(backX,backY)>L.w*.07f && Math.hypot(backX,backY)<L.w*.16f);
            g.update(.6f);float bobX=g.companionX(L),bobY=g.companionY(L);
            check("companion color restored after half second "+limb,g.colorPulse()==0);
            g.update(.2f);
            check("companion keeps moving after initial recoil "+limb,
                    Math.hypot(g.companionX(L)-bobX,g.companionY(L)-bobY)>L.w*.003f);
            g.update(AlligatorGuard.HOME_AT-g.flightAge);
            float returnY=g.companionY(L);
            check("companion stays near impact until return "+limb,Math.hypot(g.companionX(L)-x,returnY-y)<L.w*.16f);
            check("companion holds its large fighting size "+limb,Math.abs(g.companionScale()-1.68f)<.001f);
            g.update(AlligatorGuard.RETURN*.5f);
            check("return interpolates toward home "+limb,Math.abs(g.companionY(L)-(returnY+RunCompanion.y(L))*.5f)<.01f);
            check("return smoothly shrinks the companion "+limb,Math.abs(g.companionScale()-1.34f)<.001f);
            g.update(AlligatorGuard.RETURN*.5f+.001f);
            check("return finishes at home "+limb,!g.flying() && g.companionY(L)==RunCompanion.y(L));
            check("return restores normal size "+limb,g.companionScale()==1);
        }
        GameCore c=fight(L);c.companion.begin(6);block(c,L);Alligator a=c.boss.alligator;AlligatorGuard g=a.guard;
        g.update(.30f);float x=g.companionX(L),y=g.companionY(L),size=g.companionScale();
        a.tipX=L.w*.8f;a.tipY=L.h*.5f;g.emit(c,L,0,0,1,1,true);
        check("successive parries retarget without jumping home",Math.abs(g.companionX(L)-x)<.01f && Math.abs(g.companionY(L)-y)<.01f);
        check("successive parries preserve current growth",Math.abs(g.companionScale()-size)<.001f);
        g.update(.34f);x=g.companionX(L);y=g.companionY(L);size=g.companionScale();
        a.playerDamaged();check("damage begins a return without jumping",g.flightAge==AlligatorGuard.HOME_AT
                && Math.abs(g.companionX(L)-x)<.01f && Math.abs(g.companionY(L)-y)<.01f);
        check("interrupted growth returns without a size jump",Math.abs(g.companionScale()-size)<.001f);
        check("travel stores a visible trail",g.moves>1);
        g.update(AlligatorGuard.FLIGHT+AlligatorGuard.TRAIL_TIME);
        check("travel trail expires after return",g.moves==0);
        for(int i=0;i<60;i++) {
            a.tipX=(i%2==0?.2f:.8f)*L.w;g.emit(c,L,0,0,1,1,true);g.update(.015f);
        }
        check("rapid parry trails use bounded storage",g.moves>1 && g.moves<=AlligatorGuard.TRAIL_POINTS);
        c.boss.leave();check("leaving clears parry excursion and trail",!g.flying() && g.moves==0);
        c=fight(L);c.companion.begin(6);a=c.boss.alligator;
        for(int i=0;i<3;i++)exchange(c,L);
        block(c,L);g=a.guard;
        check("fourth round parry gets a larger lead-in without skipping its hit",g.impactScale>1.5f && !a.longWindow && a.roundStreak==3);
        c.boss.press(0,c.rnd,L);
        check("confirmed long daze expands the companion impact",a.longWindow && g.dazeImpact && g.impactScale>2 && a.takeShake()>=.85f);
        advance(c,L,.35f);
        float h=L.dangerY-L.playTop;
        for(int limb=0;limb<2;limb++)check("vulnerable arm hangs below shoulder "+limb,a.drawnY(L,limb)>a.rootY(L,limb)+h*.29f);
        float min=a.drawnX(L,0),max=min;
        for(int i=0;i<45;i++) {advance(c,L,DT);x=a.drawnX(L,0);min=Math.min(min,x);max=Math.max(max,x);}
        check("hanging arms swing while dazed",max-min>L.w*.04f);
        c.boss.damage(c.boss.hp);advance(c,L,DT);
        check("boss defeat clears excursion and daze pulse",!g.flying() && !g.dazeImpact);
    }
    static void contactFeedback(Layout L) {
        group("parry contact feedback");
        for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);c.companion.begin(6);Ear ear=new Ear();c.sound=ear;
            ready(c,L);int score=c.score,particles=c.particles.size();c.shake=0;c.bossDeathHaptic=0;
            block(c,L);AlligatorGuard g=c.boss.alligator.guard;
            check("swipe protects and scores immediately but waits for visible contact "+hz,
                    c.score>score && c.boss.alligator.blocked && ear.gatorParries==0 && c.shake==0
                    && c.bossDeathHaptic==0 && c.particles.size()==particles && g.impactAge==g.impactDuration && g.colorPulse()==0);
            for(int tick=0;tick<hz;tick++) {
                g.update(1f/hz);g.feedback(c);
                if(g.flightAge<AlligatorGuard.TRAVEL)check("no feedback before arrival "+hz+"/"+tick,
                        ear.gatorParries==0 && c.particles.size()==particles && g.colorPulse()==0);
                if(g.flightAge>=AlligatorGuard.TRAVEL && g.flightAge<AlligatorGuard.TRAVEL+1f/hz)
                    check("color flash starts on collision frame "+hz,g.colorPulse()>.6f);
                if(g.flightAge>=AlligatorGuard.TRAVEL+AlligatorGuard.CONTACT_FLASH)
                    check("color flash ends after half second "+hz+"/"+tick,g.colorPulse()==0);
            }
            check("contact releases sound shake haptic and particles exactly once "+hz,
                    ear.gatorParries==1 && ear.boltPops==0 && ear.uiBloops==1 && c.shake>=.38f && c.bossDeathHaptic==1
                    && c.particles.size()==particles+12);
            for(boolean damage:new boolean[]{false,true}) {
                c=fight(L);ear=new Ear();c.sound=ear;block(c,L);g=c.boss.alligator.guard;
                if(damage)g.returnHome();else c.boss.leave();
                g.update(1);g.feedback(c);
                check("interrupted flight drops delayed feedback "+damage+"/"+hz,ear.gatorParries==0 && c.particles.isEmpty());
            }
        }
    }
    static void limbRecoil(Layout L) {
        group("limb recoil on companion contact");
        for(int limb=0;limb<3;limb++) {
            GameCore c=fight(L);c.companion.begin(6);Alligator a=c.boss.alligator;
            a.next=limb;a.timer=0;block(c,L);AlligatorGuard g=a.guard;
            float x=g.limbX,y=g.limbY;
            g.update(AlligatorGuard.TRAVEL);
            check("limb recoil waits for contact "+limb,Math.hypot(a.drawnX(L,limb)-x,a.drawnY(L,limb)-y)<.01f);
            float companionX=g.companionX(L),companionY=g.companionY(L);
            g.update(.065f);
            float kickX=a.drawnX(L,limb)-x,kickY=a.drawnY(L,limb)-y;
            check("claw or tail kicks away from incoming companion "+limb,
                    kickX*(companionX-x)+kickY*(companionY-y)<0
                    && Math.hypot(kickX,kickY)>L.w*.10f && Math.hypot(kickX,kickY)<L.w*.16f);
            check("companion and limb separate in opposite directions "+limb,
                    kickX*(g.companionX(L)-companionX)+kickY*(g.companionY(L)-companionY)<0);
            float beforeX=a.drawnX(L,limb),beforeY=a.drawnY(L,limb);
            a.limb=(limb+1)%3;a.tipX=L.w*.5f;a.tipY=L.h*.4f;
            g.emit(c,L,0,0,1,1,true);
            check("next combo parry preserves prior limb recoil "+limb,
                    Math.hypot(a.drawnX(L,limb)-beforeX,a.drawnY(L,limb)-beforeY)<.01f);
            g.update(AlligatorGuard.LIMB_RETURN);
            check("recoil settles exactly to normal pose "+limb,
                    g.drawnLimbX(L,limb,123)==123 && g.drawnLimbY(L,limb,456)==456);
            c.boss.leave();
            boolean clear=true;for(int i=0;i<3;i++)clear &= g.limbAge[i]==AlligatorGuard.LIMB_RETURN;
            check("leaving clears all limb recoil clocks "+limb,clear);
        }
    }
    static void bodyMotion(Layout L) {
        GameCore c=fight(L),idle=fight(L);Alligator a=c.boss.alligator;
        check("slimmer gator body leaves room for wriggle",a.tummy.radiusX()>L.w*.40f && a.tummy.radiusX()<L.w*.55f);
        check("gator body and belly continue below screen",
                a.tummy.centreY()+a.tummy.radiusY()>L.h+L.unit*2 && a.segments[Alligator.SEGMENTS-1].centreY()>L.h);
        float first=a.segments[2].centreX();advance(c,L,.45f);
        check("breathing carries independent tummy segments",a.segments[2].centreX()!=first && a.segments[0]!=a.segments[1]);
        c=fight(L);a=c.boss.alligator;c.boss.damage(1);advance(c,L,.10f);advance(idle,L,.10f);
        boolean jiggle=true;
        for(int i=0;i<Alligator.SEGMENTS;i++)jiggle &= a.segments[i].deform()>idle.boss.alligator.segments[i].deform()+.001f;
        check("boss damage jolts every tummy segment",jiggle);
        for(int side:new int[]{-1,1}) {
            a.tailSide=side;a.tailPosition=side;
            check("tail root enters from side "+side,a.rootX(L,2)<0 || a.rootX(L,2)>L.w);
            check("tail rests above companion in lower playfield "+side,
                    a.restY(L,2)>L.playTop+(L.dangerY-L.playTop)*.65f && a.restY(L,2)<L.dangerY);
            a.limb=2;a.placed=false;a.attack=Alligator.WARN;a.blocked=false;a.target(c,L);a.pose(L);
            int hits=0;for(int i=0;i<100 && hits==0;i++)hits+=a.update(c.boss,DT,L,c.rnd);
            check("either side tail can reach companion "+side,hits==1);
        }
        a.reset();boolean cleared=a.tummy==null;
        for(Softbody segment:a.segments)cleared &= segment==null;
        check("leaving clears every tummy spring",cleared);
    }
    static void waterEntry(Layout L) {
        GameCore c=fight(L);c.boss.begin(Boss.ALLIGATOR,c.stage,c.rnd,c.playRosterFull());
        check("gator introduction lasts three times the normal entrance",c.boss.introDuration()==Boss.INTRO*3 && c.boss.intro==c.boss.introDuration());
        boolean safe=true,rising=true;float previous=AlligatorArrival.offset(L,0);
        for(int i=0;i<Math.round(c.boss.introDuration()/DT)-1;i++) {
            safe &= c.boss.update(DT,L,c.rnd)==0 && c.boss.alligator.limb<0;
            float offset=AlligatorArrival.offset(L,c.boss.introProgress());
            rising &= offset<=previous && offset>=0;previous=offset;
        }
        check("gator rises continuously from below the screen",rising
                && AlligatorArrival.offset(L,0)==L.h && AlligatorArrival.offset(L,1)==0);
        check("gator cannot attack during entrance",safe);
        final int[] calls={0};final boolean[] circlesOnly={true},translucent={true};
        Painter p=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(proxy,method,args)->{
                    calls[0]++;circlesOnly[0] &= method.getName().equals("fillCircle");
                    if(method.getName().equals("fillCircle"))translucent[0] &= ((Integer)args[3]>>>24)<128;
                    return null;
                });
        for(float t:new float[]{0,.2f,.45f,.7f,1}) {
            calls[0]=0;AlligatorArrival.particles(p,L,t,false);AlligatorArrival.particles(p,L,t,true);
            check("entry droplets use two translucent layers "+t,circlesOnly[0]
                    && translucent[0] && calls[0]%2==0 && calls[0]<=AlligatorArrival.PARTICLES*2 && (t==0 || t==1?calls[0]==0:calls[0]>0));
        }
    }

    static void gummyCompanion(Layout L) {
        GameCore c=new GameCore(new Mem(),168);c.collected=Collect.add(c.collected,Collect.ALLIGATOR_FRIEND);
        GatorCompanion a=c.gator;
        for(int i=0;i<30;i++)a.update(c,DT);
        check("collected gator idles on its own springs",a.clock>0 && Math.abs(a.head.centreX())>.001f && a.head!=a.body);
        float[] before=a.head.outline();float clock=a.clock;
        c.paused=true;a.update(c,.5f);c.paused=false;
        c.settingsOpen=true;a.update(c,.5f);c.settingsOpen=false;
        check("paused and settings gator springs stay still",clock==a.clock && java.util.Arrays.equals(before,a.head.outline()));
        a.reset();a.react(.8f);for(int i=0;i<6;i++)a.update(c,DT);
        check("tap squishes head belly and tail",a.head.deform()>.01f && a.body.deform()>.01f && a.tail.deform()>.01f);
        c.companion.begin(Collect.ALLIGATOR_FRIEND);c.companion.react(RunCompanion.WORD,.8f);
        a.update(c,DT);check("run reactions reach the gator springs",a.previousReaction==RunCompanion.WORD);
        for(int i=0;i<600;i++)a.update(c,DT);
        check("gummy idle remains bounded",a.head.deform()<.5f && a.body.deform()<.5f && Math.abs(a.head.centreX())<.2f);
        before=a.head.outline();clock=a.clock;
        RasterPainter normal=new RasterPainter(240,260,1),happy=new RasterPainter(240,260,1);
        Painter p=new OpacityPainter(normal,1) {public GatorCompanion gator(){return a;}};
        BossCollect.draw(p,Boss.ALLIGATOR,120,120,65,1,true,1,0,0,false);
        Painter q=new OpacityPainter(happy,1) {public GatorCompanion gator(){return a;}};
        BossCollect.draw(q,Boss.ALLIGATOR,120,120,65,1,true,1,4,.2f,false);
        check("gator honors companion face reactions",!java.util.Arrays.equals(normal.resolve(),happy.resolve()));
        check("drawing gator never advances its springs",clock==a.clock && java.util.Arrays.equals(before,a.head.outline()));
        check("painter transforms preserve gator simulation",new ColorFadePainter(p,0,0,1).gator()==a
                && new MonochromePainter(p,1).gator()==a);
    }
    static void damagePacing(Layout L) {
        float previousContact=Float.MAX_VALUE,previousCycle=Float.MAX_VALUE;
        for(float health:new float[]{1,.75f,.5f,.25f,.01f}) {
            GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;b.hp=b.hpMax*health;
            a.timer=0;b.update(DT,L,c.rnd);
            check("attack speed follows health "+health,Math.abs(a.tempo-Alligator.speed(b))<.001f);
            if(health==1)check("opening attack keeps original pace",a.tempo==1);
            if(health==.75f)check("first damage quarter accelerates quickly",a.tempo>1.45f);
            if(health<=.75f)check("attack speed caps at the 75-percent-health pace "+health,Math.abs(a.tempo-1.49140625f)<.00001f);
            float contact=0;int hits=0;
            while(hits==0 && contact<3) {hits+=b.update(DT,L,c.rnd);contact+=DT;}
            check("warning-to-contact stops accelerating below 75 percent "+health,hits==1
                    && (health>=.75f?contact<previousContact:Math.abs(contact-previousContact)<=DT));
            if(health>=.5f) {
                float cycle=contact;int first=a.limb;
                while(a.limb==first || a.limb<0) {b.update(DT,L,c.rnd);cycle+=DT;if(cycle>5)break;}
                check("solo attack cycle respects the speed cap "+health,
                        health>=.75f?cycle<previousCycle:Math.abs(cycle-previousCycle)<=DT);
                previousCycle=cycle;
            }
            previousContact=contact;
        }
        GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;
        check("gator needs six times the original damage",b.hpMax==72);
        a.daze=Alligator.DAZE;a.longWindow=true;a.windowHit=true;
        for(int n=0;n<18;n++)b.press(n%Glyph.COUNT,c.rnd,L);
        check("long opening still caps damage at a quarter",b.hp==b.hpMax*.75f && a.daze==0);
        b.press(0,c.rnd,L);
        check("pressing beyond long opening triggers a counter",b.hp==b.hpMax*.75f && a.pendingShots==1);
        c=fight(L);a=c.boss.alligator;ready(c,L);float tempo=a.tempo;
        c.boss.hp=1;c.boss.update(DT,L,c.rnd);
        check("in-flight attack keeps its warning timing",a.tempo==tempo);
        a.reset();check("reset clears damage budget and speed",a.windowDamage==0 && a.tempo==1);
    }
    static void limbMotion(Layout L) {
        GameCore c=fight(L);Alligator a=c.boss.alligator;
        float x=a.restX(L,0),y=a.restY(L,1);advance(c,L,.18f);
        check("idle paws sway independently",x!=a.restX(L,0) && y!=a.restY(L,1));
        float rx=a.rootX(L,0),ry=a.rootY(L,0),tx=a.restX(L,0),ty=a.restY(L,0);
        float cross=(a.elbowX(L,0,tx)-rx)*(ty-ry)-(a.elbowY(L,0,ty)-ry)*(tx-rx);
        check("elbow gives the arm a real bend",Math.abs(cross)>L.w*L.w*.005f);
        check("shoulders attach inside torso",rx>L.w*.07f && a.rootX(L,1)<L.w*.93f);
        check("relaxed shoulders sit lower with paws hanging underneath",ry>L.playTop+(L.dangerY-L.playTop)*.40f && ty>ry);
        check("resting tail shows only its edge tip",a.restX(L,2)>L.w*.85f);
        for(int arm=0;arm<2;arm++) {
            float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
            for(int n=0;n<600;n++) {
                a.motionClock=n*.05f;
                minX=Math.min(minX,a.restX(L,arm));maxX=Math.max(maxX,a.restX(L,arm));
                minY=Math.min(minY,a.restY(L,arm));maxY=Math.max(maxY,a.restY(L,arm));
            }
            check("arm wanders across meaningful attack origins "+arm,maxX-minX>L.w*.22f && maxY-minY>(L.dangerY-L.playTop)*.24f);
            check("wandering palms stay visible and above companion "+arm,minX>L.w*.10f && maxX<L.w*.90f
                    && minY>L.playTop+(L.dangerY-L.playTop)*.34f && maxY<L.playTop+(L.dangerY-L.playTop)*.73f);
        }
        c=fight(L);a=c.boss.alligator;a.motionClock=2;ready(c,L);
        float strikeX=a.strikeX,strikeY=a.strikeY;
        a.motionClock+=2;a.pose(L);
        check("attack commits its wandering start during tell",a.strikeX==strikeX && a.strikeY==strikeY);
        a.tailPosition=-1;
        check("left tail curves down toward its offscreen attachment",a.rootX(L,2)<0 && a.rootY(L,2)>a.restY(L,2));
        a.tailPosition=0;check("tail wraps underneath extended body",a.rootY(L,2)>L.h);
        boolean edge=true;
        for(int i=0;i<=40;i++) {
            a.tailPosition=-1+i*.05f;
            edge &= a.rootX(L,2)<0 || a.rootX(L,2)>L.w || a.rootY(L,2)>L.h;
        }
        check("side-changing tail stays connected beyond screen edge",edge);
        c=fight(L);a=c.boss.alligator;
        check("first defense round has explicit guide",a.showGuide());
        for(int n=0;n<Alligator.ROUNDS_FOR_DAZE-1;n++)exchange(c,L);
        advance(c,L,Alligator.OPEN+.04f);a.next=2;a.timer=0;a.tailPosition=a.tailSide=1;exchange(c,L);
        check("first daze retires the guide",a.dazes==1 && !a.showGuide());
        check("daze keeps blocked tail for its recoil",a.limb==2 && a.blocked && a.recoil>0);
        float before=a.tailPosition;advance(c,L,a.recoil+.04f);
        check("tail changes destination without jumping",a.tailSide==-1 && Math.abs(a.tailPosition-before)<.2f);
        boolean smooth=true,crossed=false;float previous=a.tailPosition;
        for(int n=0;n<65;n++) {
            c.boss.update(DT,L,c.rnd);
            smooth &= Math.abs(a.tailPosition-previous)<=DT*2.5f+.0001f;
            crossed |= Math.abs(a.tailPosition)<.1f;previous=a.tailPosition;
        }
        check("tail sweeps continuously to opposite side",smooth && crossed && a.tailPosition==-1);
        c.paused=true;float clock=a.motionClock,side=a.tailPosition;c.update(.2f,L);
        check("pause freezes idle limbs and tail travel",a.motionClock==clock && a.tailPosition==side);
        a.reset();check("reset restores first-round guide and tail",a.showGuide() && a.tailSide==1 && a.tailPosition==1 && a.motionClock==0);
    }
    static void damageWriggle(Layout L) {
        GameCore c=fight(L),idle=fight(L);Alligator a=c.boss.alligator;
        float[] outline=a.bodyOutline(L);
        float waist=outline[(65-16)*2]-outline[16*2],lower=outline[(65-31)*2]-outline[31*2];
        check("rounded middle gently narrows into the long lower torso",lower>waist*.84f && lower<waist*.97f
                && lower>L.w*.6f && lower<L.w*.8f && outline[31*2+1]>L.h);
        c.boss.damage(1);
        check("damage starts body wriggle",a.wriggle>0);
        advance(c,L,.18f);advance(idle,L,.18f);
        check("damage wriggle moves independent belly layers sideways",
                Math.abs(a.segments[2].centreX()-idle.boss.alligator.segments[2].centreX())>L.w*.003f);
        float wriggle=a.wriggle;c.paused=true;c.update(.2f,L);
        check("pause freezes wriggle",a.wriggle==wriggle);c.paused=false;
        advance(c,L,1);check("wriggle expires",a.wriggle==0);
        longOpening(c,L);
        check("entering daze keeps a strong body wriggle",a.daze>0 && a.wriggle>=.8f);
        c.boss.leave();check("leaving clears wriggle",a.wriggle==0);
    }
    static void expressionFeedback(Layout L) {
        for(int limb=0;limb<3;limb++) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;
            a.next=limb;a.timer=0;c.boss.update(DT,L,c.rnd);
            check("eyes telegraph the selected limb "+limb,a.mood(c.boss)==Alligator.WINDUP_EYES
                    && a.gazeX()==(limb==0?-1:1) && a.gazeY()==(limb==2?1:0));
            advance(c,L,.62f);
            check("eyes focus down during strike "+limb,a.mood(c.boss)==Alligator.STRIKE_EYES && a.gazeY()==1);
        }
        GameCore c=fight(L);Alligator a=c.boss.alligator;block(c,L);
        check("short opening uses the dizzy expression",a.mood(c.boss)==Alligator.DAZED_EYES);
        c.boss.press(0,c.rnd,L);
        for(int n=1;n<Alligator.ROUNDS_FOR_DAZE;n++)exchange(c,L);
        advance(c,L,.4f);
        check("daze changes eyes to dizzy spirals",a.mood(c.boss)==Alligator.DAZED_EYES);
        c.boss.damage(1);check("damage winces over the dazed expression",a.mood(c.boss)==3);
        check("damage immediately flashes red",AlligatorArt.damageFlash(c.boss.hurt)>.5f);
        int first=AlligatorArt.damageColor(c.boss.hurt);advance(c,L,.12f);
        check("damage cycles through red shades",first!=AlligatorArt.damageColor(c.boss.hurt) && AlligatorArt.damageFlash(c.boss.hurt)>.5f);
        advance(c,L,.15f);check("quick damage flash ends",AlligatorArt.damageFlash(c.boss.hurt)==0);
        advance(c,L,.2f);check("eyes recover to the active daze",a.mood(c.boss)==Alligator.DAZED_EYES);
        c=fight(L);a=c.boss.alligator;int contact=0;
        for(int n=0;n<150 && contact==0;n++)contact+=c.boss.update(DT,L,c.rnd);
        check("landing an attack gives cheeky eyes",contact==1 && a.mood(c.boss)==Alligator.CHEEKY_EYES);
        c.boss.damage(c.boss.hp);check("defeat relaxes the face",a.mood(c.boss)==8);
        a.reset();check("reset clears contact expression",!a.hitPlayer);
    }
    static void animatedExpressions(Layout L) {
        Alligator.Eyes eyes=new Alligator.Eyes();
        eyes.update(Alligator.WINDUP_EYES,-1,0,.04f);
        check("gaze and pupil size ease into tell",eyes.x<0 && eyes.x>-1 && eyes.rx>.075f && eyes.rx<.115f);
        float before=eyes.x;eyes.update(Alligator.WINDUP_EYES,1,0,.01f);
        check("interrupted gaze starts from current position",eyes.x>before && eyes.x<0);
        eyes.update(Alligator.DAZED_EYES,0,0,.04f);
        check("dizzy eyes grow in",eyes.dizzy>0 && eyes.dizzy<1);
        float dizzy=eyes.dizzy;eyes.update(3,0,0,.025f);
        check("damage morphs from existing dizzy pose",eyes.squeeze>0 && eyes.squeeze<1 && eyes.dizzy>0 && eyes.dizzy<dizzy);
        eyes.update(Alligator.DAZED_EYES,0,0,.30f);
        check("recovering damage eases back to daze",eyes.squeeze<.01f && eyes.dizzy>.99f);
        Alligator.Eyes slow=new Alligator.Eyes(),fast=new Alligator.Eyes();
        for(int i=0;i<6;i++)slow.update(Alligator.STRIKE_EYES,0,1,1f/30);
        for(int i=0;i<24;i++)fast.update(Alligator.STRIKE_EYES,0,1,1f/120);
        check("eye easing is frame-rate independent",Math.abs(slow.y-fast.y)<.0001f && Math.abs(slow.rx-fast.rx)<.0001f);
        GameCore c=fight(L);advance(c,L,.3f);Alligator a=c.boss.alligator;
        float x=a.eyes.x,clock=a.eyes.clock;c.paused=true;c.update(.2f,L);
        check("pause freezes eye transitions",x==a.eyes.x && clock==a.eyes.clock);
        RasterPainter p=new RasterPainter((int)L.w,(int)L.h,1);
        AlligatorArt.draw(p,c,L,c.boss,1);AlligatorArt.draw(p,c,L,c.boss,1);
        check("drawing never advances eye transitions",x==a.eyes.x && clock==a.eyes.clock);
        a.reset();check("reset clears eye transition state",a.eyes.x==0 && a.eyes.closed()==0 && a.eyes.dizzy==0 && a.eyes.clock==0);
    }
    static void retaliation(Layout L) {
        group("gator crossed-arm retaliation");
        for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;Boss b=c.boss;
            float hp=b.hp;
            check("early key starts block without damage "+hz,b.press(0,c.rnd,L)==Boss.REBUFF && a.retaliation>0 && b.hp==hp);
            float start=a.retaliation;b.press(1,c.rnd,L);
            check("repeated taps do not restart block "+hz,a.retaliation==start);
            float dt=1f/hz,elapsed=0;
            for(;elapsed<.10f;elapsed+=dt)b.update(dt,L,c.rnd);
            check("arms cross before tail fires "+hz,a.crossedArms()>.99f && a.drawnX(L,0)>a.drawnX(L,1) && b.boltCount()==0);
            float clock=a.retaliation,cooldown=a.counterCooldown;c.paused=true;c.update(.2f,L);c.paused=false;
            check("pause freezes retaliation and cooldown "+hz,a.retaliation==clock && a.counterCooldown==cooldown);
            check("crossed arms cannot earn swipe defenses "+hz,!a.swipe(b,L,0,L.h,L.w,0) && a.roundStreak==0);
            for(;b.boltCount()==0 && elapsed<1;elapsed+=dt)b.update(dt,L,c.rnd);
            float firstShot=elapsed;
            check("first counter fires after 0.30 seconds "+hz,Math.abs(firstShot-.30f)<=dt+.001f && b.boltCount()==1);
            for(int n=0;n<hz*2 && a.retaliation>0;n++)b.update(dt,L,c.rnd);
            check("repeated blocked presses emit only one prompt "+hz,a.retaliationShots==1 && b.boltCount()==1 && a.retaliation==0);
            for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i]) {
                check("tail launch stays in playfield "+hz+"/"+i,b.bsx[i]>0 && b.bsx[i]<L.w && b.bsy[i]>L.playTop && b.bsy[i]<L.dangerY);
                check("matching key clears prompt without counterattack "+hz+"/"+i,b.press(b.bglyph[i],c.rnd,L)==Boss.PARRY && a.retaliation==0);
            }
            check("clearing volley leaves boss health unchanged "+hz,b.boltCount()==0 && b.hp==hp);
            b.press(0,c.rnd,L);b.damage(b.hp);b.update(dt,L,c.rnd);
            check("defeat clears retaliation, cooldown and projectiles "+hz,a.retaliation==0 && a.counterCooldown==0 && b.boltCount()==0);
        }
        GameCore burst=fight(L);Alligator queue=burst.boss.alligator;
        for(int n=0;n<24;n++)queue.press(burst.boss,L);
        advance(burst,L,.95f);
        check("burst of blocked inputs produces a single reply",queue.retaliationShots==1 && queue.pendingShots==0 && burst.boss.boltCount()==1);
        float cooldown=queue.counterCooldown,hp=burst.boss.hp;
        for(int n=0;n<24;n++)queue.press(burst.boss,L);
        check("cooldown inputs only block without queuing or extending delay",queue.pendingShots==0
                && queue.retaliation==0 && queue.counterCooldown==cooldown && burst.boss.hp==hp);
        advance(burst,L,cooldown-.05f);queue.press(burst.boss,L);
        check("counter stays suppressed through 1.5 seconds after launch",queue.counterCooldown>0 && queue.pendingShots==0);
        advance(burst,L,.08f);queue.press(burst.boss,L);
        check("a new blocked input can counter after cooldown",queue.pendingShots==1 && queue.retaliation>0);
        advance(burst,L,.32f);
        check("new counter still emits only one prompt",queue.retaliationShots==1 && queue.pendingShots==0);
        GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;
        ready(c,L);float attack=a.attack;b.press(0,c.rnd,L);advance(c,L,.5f);
        check("counter does not cancel or advance the current limb",a.limb>=0 && a.attack==attack);
        advance(c,L,.20f);check("ordinary limb resumes promptly after counter",a.retaliation==0 && a.attack>attack);
        c=fight(L);b=c.boss;b.alligator.timer=10;b.press(0,c.rnd,L);int hits=0;
        for(int n=0;n<270;n++)hits+=b.update(DT,L,c.rnd);
        check("one ignored blocked press returns one damaging prompt",hits==1);
        c=fight(L);c.tapKey(0,L);
        check("early key shot visibly bounces off crossed arms",!c.shots.isEmpty() && c.shots.get(0).shieldBounce);
        Bot defender=new Bot(6f,.2f,.04f,false,168);
        for(int n=0;n<360;n++) {defender.step(c,L,DT);c.update(DT,L);}
        check("bounded hands clear the tail prompts and resume swiping",c.lives==3 && c.boss.boltCount()==0 && c.boss.alligator.roundStreak>0);
        c=fight(L);b=c.boss;a=b.alligator;a.daze=Alligator.DAZE;
        check("dazed key attacks remain damage only",b.press(0,c.rnd,L)==Boss.HIT && a.retaliation==0);
        a.daze=0;b.press(0,c.rnd,L);b.leave();
        check("leaving clears pending tail shot and cooldown",a.retaliation==0 && a.retaliationShots==0 && a.pendingShots==0 && a.counterCooldown==0 && b.boltCount()==0);
    }
    static void windupStrike(Layout L) {
        group("gator wind-up and accelerating strike");
        for(int limb=0;limb<3;limb++)for(float tempo:new float[]{1,1.85f}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;
            a.limb=limb;a.placed=false;a.blocked=false;a.tempo=tempo;a.attack=0;a.pose(L);
            float start=a.tipY;
            a.attack=Alligator.WARN/tempo;a.pose(L);float wound=a.tipY;
            check("limb coils away before striking "+limb+"/"+tempo,wound<start && a.strikeProgress()==0);
            float previous=wound,travel=0;boolean accelerates=true;
            for(int n=1;n<=4;n++) {
                a.attack=(Alligator.WARN+Alligator.REACH*n/4)/tempo;a.pose(L);
                float step=a.tipY-previous;accelerates &= step>travel;
                travel=step;previous=a.tipY;
            }
            check("strike accelerates through contact "+limb+"/"+tempo,accelerates);
            check("full extension keeps original total duration "+limb+"/"+tempo,
                    Math.abs(a.tipX-a.targetX)<.001f && Math.abs(a.tipY-a.targetY)<.001f);
        }
        for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;
            a.limb=0;a.placed=false;a.attack=0;a.pose(L);float dt=1f/hz,time=0;int hits=0;
            while(hits==0 && time<Alligator.WARN+Alligator.REACH+dt) {
                hits+=a.update(c.boss,dt,L,c.rnd);time+=dt;
            }
            check("fast strike still sweeps companion contact "+hz,hits==1 && time>Alligator.WARN
                    && time<=Alligator.WARN+Alligator.REACH+dt);
        }
    }
    static void randomLimbs(Layout L) {
        group("gator randomized attack selection");
        GameCore c=fight(L);Alligator a=c.boss.alligator;int[] counts=new int[3];
        java.util.Random random=new java.util.Random(171);
        int previous=-1;boolean repeats=false,followsCycle=true;
        for(int i=0;i<120;i++) {
            a.limb=-1;a.next=-1;a.timer=.2f;
            a.update(c.boss,.01f,L,random);int choice=a.next;
            a.update(c.boss,.01f,L,random);
            check("pending tell keeps its chosen limb "+i,a.next==choice);
            a.update(c.boss,.2f,L,random);
            counts[a.limb]++;repeats |= a.limb==previous;
            if(previous>=0)followsCycle &= a.limb==(previous+1)%3;
            previous=a.limb;
        }
        check("attacks use all three limbs without immediate repeats",!repeats && counts[0]>20 && counts[1]>20 && counts[2]>20);
        check("limb order no longer follows a fixed cycle",!followsCycle);
        Alligator left=new Alligator(),right=new Alligator();left.reset();right.reset();
        java.util.Random r1=new java.util.Random(168),r2=new java.util.Random(168);
        boolean same=true;
        for(int i=0;i<30;i++) {
            left.limb=right.limb=-1;left.timer=right.timer=0;
            left.update(c.boss,DT,L,r1);right.update(c.boss,DT,L,r2);
            same &= left.limb==right.limb;
        }
        check("same seed reproduces attack order",same);
        a.reset();check("reset clears planned and previous limbs",a.next==-1 && a.lastLimb==-1);
    }
    static int entryDrawCalls(Layout L,float progress) {
        GameCore c=fight(L);c.boss.intro=c.boss.introDuration()*(1-progress);
        final int[] calls={0};
        Painter p=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(proxy,method,args)->{
                    if(method.getName().startsWith("fill") || method.getName().equals("polyline")
                            || method.getName().equals("arc") || method.getName().equals("line"))calls[0]++;
                    return null;
                });
        AlligatorArt.draw(p,c,L,c.boss,1);return calls[0];
    }
    static void parryOpenings(Layout L) {
        group("gator parry-and-punish rhythm");
        GameCore c=fight(L);Alligator a=c.boss.alligator;float hp=c.boss.hp;
        block(c,L);check("one parry opens a short attack window",a.daze==Alligator.OPEN && c.boss.open() && a.roundStreak==0);
        float left=a.daze;c.paused=true;c.update(.2f,L);c.paused=false;
        check("pause preserves the short opening",a.daze==left);
        advance(c,L,Alligator.OPEN+.01f);
        check("unused opening gives no exchange credit",!c.boss.open() && a.roundStreak==0 && c.boss.hp==hp && a.closing>0);
        block(c,L);c.boss.press(0,c.rnd,L);c.boss.press(1,c.rnd,L);c.boss.press(2,c.rnd,L);
        check("extra short-window hits only count one round",a.roundStreak==1 && !a.longWindow && c.boss.hp==hp-3);
        advance(c,L,Alligator.OPEN+.01f);float after=c.boss.hp;
        c.boss.press(0,c.rnd,L);c.boss.press(1,c.rnd,L);
        check("continuing after the window queues one counter",a.retaliation>0 && a.pendingShots==1 && c.boss.hp==after);
        advance(c,L,1.2f);
        check("late hits produce one defendable projectile",c.boss.boltCount()==1);
        c=fight(L);a=c.boss.alligator;
        for(int n=0;n<3;n++) {exchange(c,L);advance(c,L,Alligator.OPEN+.01f);}
        check("three completed exchanges keep their progress",a.roundStreak==3 && !a.longWindow);
        block(c,L);check("fourth parry alone is still short",!a.longWindow && a.roundStreak==3);
        c.boss.press(0,c.rnd,L);
        check("fourth follow-up hit earns the full long opening",a.longWindow && a.daze==Alligator.DAZE && a.dazes==1);
        advance(c,L,Alligator.DAZE+.01f);
        check("finishing long opening starts a fresh exchange cycle",a.roundStreak==0 && !a.longWindow && !c.boss.open());
        c.boss.leave();check("leaving clears window and guard-return state",a.daze==0 && a.closing==0 && !a.windowHit && !a.longWindow);
    }
    static void closingEyes(Layout L) {
        group("gator eyes hint at returning guard");
        GameCore c=fight(L);Alligator a=c.boss.alligator;
        block(c,L);
        check("fresh opening has no guard hint",!a.guardReturning(c.boss));
        advance(c,L,Alligator.OPEN-Alligator.GUARD_TELL+.03f);
        c.boss.press(0,c.rnd,L);
        check("closing eyes remain readable through a last hit",a.guardReturning(c.boss)
                && a.mood(c.boss)==Alligator.GUARD_EYES && c.boss.hurt>0);
        a.eyes.reset();float wide=a.eyes.ry;
        advance(c,L,1f/60);
        check("guard hint narrows eyes smoothly toward companion",a.eyes.ry<wide && a.eyes.ry>.27f
                && a.eyes.y>0 && a.eyes.y<1 && a.eyes.glint>0);
        advance(c,L,Alligator.GUARD_TELL);
        check("guard hint ends with vulnerability",!a.guardReturning(c.boss) && !c.boss.open());
        c=fight(L);a=c.boss.alligator;longOpening(c,L);
        check("long opening starts without closing hint",!a.guardReturning(c.boss));
        for(int n=0;n<15;n++)c.boss.press(n%6,c.rnd,L);
        check("last two allowed long-window hits warn through eyes",a.longWindow && a.daze>Alligator.GUARD_TELL
                && a.guardReturning(c.boss) && a.mood(c.boss)==Alligator.GUARD_EYES);
    }
    static void cleanRounds(Layout L) {
        group("gator damage-free parry-and-hit rounds");
        GameCore c=fight(L);Alligator a=c.boss.alligator;block(c,L);
        float hp=c.boss.hp;
        for(int n=0;n<8;n++)c.boss.press(n%Glyph.COUNT,c.rnd,L);
        check("eight hits in one opening still count only one round",a.roundStreak==1 && !a.longWindow && c.boss.hp==hp-8);
        for(int n=1;n<Alligator.ROUNDS_FOR_DAZE;n++)exchange(c,L);
        check("four distinct parry-and-hit rounds earn long vulnerability",a.roundStreak==4 && a.longWindow && a.daze==Alligator.DAZE);
        c.boss.press(4,c.rnd,L);
        check("long-window hits do not build another streak",a.roundStreak==4 && a.dazes==1);
        c=fight(L);a=c.boss.alligator;block(c,L);advance(c,L,.15f);
        check("short opening eases into its own slumped dizzy pose",a.vulnerablePose>.5f && a.vulnerablePose<.75f && a.eyes.dizzy>.5f);
        c.boss.press(0,c.rnd,L);float pose=a.vulnerablePose;advance(c,L,DT);
        check("taking a boss hit preserves vulnerable body pose",a.vulnerablePose>=pose && a.daze>0);
        c.boss.press(1,c.rnd,L);c.boss.press(2,c.rnd,L);
        c.takeHit(L.w*.5f,L);
        check("actual player damage breaks the streak immediately",a.roundStreak==0 && a.windowDamaged && c.lives==2 && !a.longWindow);
        c.boss.press(3,c.rnd,L);
        check("more hits cannot recover a damaged round",a.roundStreak==0 && !a.longWindow);
        advance(c,L,Alligator.OPEN+.01f);
        c.boss.press(0,c.rnd,L);
        check("a blocked key cannot earn a round",a.roundStreak==0 && a.pendingShots==1);
        // Both unblocked limbs and counter projectiles reach the shared damage hook.
        for(boolean bolt:new boolean[]{false,true}) {
            c=fight(L);a=c.boss.alligator;a.roundStreak=3;
            if(bolt) {a.timer=10;c.boss.press(0,c.rnd,L);}
            for(int n=0;n<300 && c.lives==3;n++)c.update(DT,L);
            check((bolt?"counter projectile":"limb contact")+" resets the round streak",c.lives==2 && a.roundStreak==0);
        }
        c=fight(L);a=c.boss.alligator;
        for(int n=0;n<3;n++)exchange(c,L);
        c.takeHit(L.w*.5f,L);advance(c,L,Alligator.OPEN+.02f);
        block(c,L);c.takeHit(L.w*.5f,L);
        c.boss.press(0,c.rnd,L);
        check("damage before the first hit also invalidates the round",a.roundStreak==0 && !a.longWindow);
        for(int n=1;n<=4;n++) {
            exchange(c,L);
            check("damage requires four fresh clean rounds "+n,a.roundStreak==n && a.longWindow==(n==4));
        }
        c.boss.leave();check("leaving clears round, damage flag and vulnerable pose",a.roundStreak==0 && !a.windowDamaged && a.vulnerablePose==0);
    }
    static void combos(Layout L) {
        group("gator full-combo defense");
        for(float health:new float[]{.51f,.5f,.49f,.25f,.24f}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;
            c.boss.hp=c.boss.hpMax*health;a.timer=0;c.boss.update(DT,L,c.rnd);
            check("combos start strictly below half health "+health,a.comboActive==(health<.5f));
            if(!a.comboActive)continue;
            int count=health<.25f?3:2;
            check("third limb starts strictly below quarter health "+health,a.comboCount==count);
            float clock=a.comboAge;c.paused=true;c.update(.2f,L);c.paused=false;
            check("pause freezes the whole combo",a.comboAge==clock);
            int mask=0,hits=0;float previousParry=0;
            for(int n=0;n<count;n++) {
                for(int frame=0;frame<180 && (a.comboLaunched<=n || a.combo[n].attack<.14f);frame++)
                    hits+=c.boss.update(DT,L,c.rnd);
                Alligator.Strike strike=a.combo[n];mask|=1<<strike.limb;
                float start=a.comboAge-strike.attack;
                if(n>0)check("next strike begins at preceding parry "+count+"/"+n,
                        Math.abs(start-previousParry)<DT*.1f);
                check("each combo limb renders its own tip "+n,Math.abs(a.drawnX(L,strike.limb)-strike.tipX)<.001f
                        && Math.abs(a.drawnY(L,strike.limb)-strike.tipY)<.001f);
                check("each combo strike can be parried "+count+"/"+n,a.swipe(c.boss,L,
                        strike.tipX-L.w*.09f,strike.tipY,strike.tipX+L.w*.09f,strike.tipY));
                previousParry=a.comboAge;
                if(n<count-1) {
                    check("parry immediately launches exactly the next limb "+count+"/"+n,
                            a.comboLaunched==n+2 && a.combo[n+1].limb==a.comboOrder[n+1]
                            && a.combo[n+1].attack==0 && strike.blocked);
                    check("fallback spacing follows accelerated launch",Math.abs(a.comboNextAt-a.comboAge-1.04f)<.001f);
                    int launched=a.comboLaunched;
                    check("same swipe cannot skip another strike",!a.swipe(c.boss,L,
                            strike.tipX-L.w*.09f,strike.tipY,strike.tipX+L.w*.09f,strike.tipY)
                            && a.comboLaunched==launched);
                }
                check("only final parry opens vulnerability "+count+"/"+n,
                        (a.daze>0)==(n==count-1) && a.dazeCue==(n==count-1) && a.comboParried==n+1);
                check("parries alone do not count as attack rounds",a.roundStreak==0);
            }
            check("successful combo uses distinct limbs without damage",Integer.bitCount(mask)==count && hits==0);
            float hp=c.boss.hp;c.boss.press(0,c.rnd,L);
            if(a.showoff()) {
                check("threshold interrupts the newly earned opening",c.boss.hp==hp-1
                        && a.showoffCount==3 && a.daze==0 && a.roundStreak==0);
                c.boss.leave();continue;
            }
            check("completed combo permits damage and counts as one round",c.boss.hp==hp-1 && a.roundStreak==1);
            a.roundStreak=3;a.windowHit=false;c.boss.press(0,c.rnd,L);
            check("fourth clean combo round opens long vulnerability",a.longWindow);
            hits=0;for(int n=0;n<90;n++)hits+=c.boss.update(DT,L,c.rnd);
            check("completed combo stays safe during long opening",hits==0);
            c.boss.leave();check("leaving clears combo progress",!a.comboActive
                    && a.comboLaunched==0 && a.comboParried==0 && a.combo[0].limb==-1);
        }
        for(int count:new int[]{2,3})for(int miss=0;miss<count;miss++)for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;float dt=1f/hz;
            c.boss.hp=c.boss.hpMax*(count==2?.49f:.24f);a.timer=0;c.boss.update(dt,L,c.rnd);
            int hits=0;
            for(int frame=0;frame<hz*5 && hits==0;frame++) {
                for(int n=0;n<miss;n++) {
                    Alligator.Strike strike=a.combo[n];
                    if(strike.limb>=0 && !strike.blocked && strike.attack>=.14f)
                        a.swipe(c.boss,L,strike.tipX-L.w*.09f,strike.tipY,strike.tipX+L.w*.09f,strike.tipY);
                }
                hits+=c.boss.update(dt,L,c.rnd);
            }
            check("miss cancels combo without a daze "+count+"/"+miss+"/"+hz,
                    hits==1 && !a.comboActive && a.comboParried==0 && a.daze==0 && a.hitPlayer);
            boolean cleared=true;for(Alligator.Strike strike:a.combo)cleared &= strike.limb==-1;
            check("miss removes active and pending combo strikes",cleared);
            a.roundStreak=3;a.playerDamaged();check("combo damage resets clean-round streak",a.roundStreak==0);
            float recovery=Alligator.RETURN/Alligator.speed(c.boss)+Alligator.GAP/Alligator.speed(c.boss);
            for(int frame=0;frame<(int)(recovery*hz);frame++)hits+=c.boss.update(dt,L,c.rnd);
            check("cancelled combo cannot deal further damage during recovery",hits==1 && a.daze==0);
            for(int frame=0;frame<hz*4 && hits<2;frame++)hits+=c.boss.update(dt,L,c.rnd);
            check("boss can start a fresh combo after cancellation",hits==2);
            c.boss.damage(c.boss.hp);c.boss.update(dt,L,c.rnd);
            check("defeat clears combo progress",!a.comboActive && a.comboParried==0);
        }
        GameCore c=fight(L);Alligator a=c.boss.alligator;
        c.boss.hp=c.boss.hpMax*.24f;a.timer=0;c.boss.update(DT,L,c.rnd);advance(c,L,.16f);
        a.swipe(c.boss,L,a.tipX-L.w*.12f,a.tipY,a.tipX+L.w*.12f,a.tipY);
        float hp=c.boss.hp;int result=c.boss.press(0,c.rnd,L);
        check("keys remain blocked after a partial combo parry",result==Boss.REBUFF && c.boss.hp==hp && a.daze==0);
    }
    static void laughingTaunt(Layout L) {
        GameCore c=fight(L);Alligator a=c.boss.alligator;a.timer=10;
        c.takeHit(L.w*.5f,L);
        check("actual damage starts a fresh taunt",a.taunt==Alligator.TAUNT_TIME);
        advance(c,L,.15f);
        check("taunt grows into a laughing face",a.laugh()>0 && a.eyes.smile>.5f
                && a.mood(c.boss)==Alligator.CHEEKY_EYES);
        float left=a.taunt;c.paused=true;c.update(.2f,L);c.paused=false;
        check("pause freezes laughter",a.taunt==left);
        a.limb=0;a.blocked=false;a.attack=.2f;
        check("attack eyes stay readable during laughter",a.mood(c.boss)==Alligator.WINDUP_EYES);
        a.limb=-1;advance(c,L,Alligator.TAUNT_TIME);
        check("laughter ends on its own",a.taunt==0 && a.laugh()==0);
        c.takeHit(L.w*.5f,L);c.boss.damage(c.boss.hp);advance(c,L,DT);
        check("boss defeat clears laughter",a.taunt==0);
        a.playerDamaged();c.boss.leave();check("leaving clears laughter",a.taunt==0);
    }
    static void clawGlow(Layout L) {
        GameCore c=fight(L);Alligator a=c.boss.alligator;a.next=0;a.timer=0;
        advance(c,L,.3f);c.boss.age=.1f;
        final int[] strokes={0};final boolean[] attached={true};
        Painter p=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class[]{Painter.class},(proxy,method,args)->{
                    if(method.getName().equals("strokePoly")) {
                        strokes[0]++;
                        float[] points=(float[])args[0];
                        for(int i=0;i<points.length;i+=2)
                            attached[0] &= Math.abs(points[i]-a.drawnX(L,0))<L.w*.18f
                                    && Math.abs(points[i+1]-a.drawnY(L,0))<L.w*.18f;
                    }
                    return null;
                });
        AlligatorArt.draw(p,c,L,c.boss,1);
        check("yellow glow follows the whole attacking claw",strokes[0]==3 && attached[0]);
    }
    static void growlAudio(Layout L) {
        group("recorded daze-cancel growls");
        for(int phase=0;phase<2;phase++)for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;Ear ear=new Ear();c.sound=ear;
            b.hp=b.hpMax*(phase==0?.5f:.25f);a.daze=Alligator.DAZE;a.longWindow=true;b.damage(1);
            check("combo demo waits for final growl tail",a.showoffLead()>=GatorTauntRecording.end(phase)+.17f);
            float dt=1f/hz;int played=0;
            for(int frame=0;frame<hz*4 && a.showoff();frame++) {
                c.update(dt,L);
                if(ear.gatorGrowls.size()>played) {
                    int id=ear.gatorGrowls.get(played);
                    check("growls keep file order and original spacing "+phase+"/"+hz+"/"+played,
                            id==phase*3+played && ear.gatorGrowls.size()==played+1
                            && Math.abs(a.showoffAge-GatorTauntRecording.onset(phase,played))<=dt+.001f);
                    played++;
                }
                if(a.showoff() && a.showoffAge<GatorTauntRecording.end(phase))
                    check("no demonstration strikes over recorded growls",a.showoffLimb()<0 && ear.gatorCues==0);
                if(frame==hz/4) {
                    float age=a.showoffAge,mouth=a.laugh();int cues=ear.gatorGrowls.size();
                    c.paused=true;c.update(.2f,L);c.paused=false;
                    check("pause freezes growl mouth and cue schedule",age==a.showoffAge && mouth==a.laugh() && cues==ear.gatorGrowls.size());
                }
            }
            check("phase growls and demonstration complete once",played==3 && !a.showoff() && a.takeGrowls()==0 && a.laugh()==0);
            for(int beat=0;beat<3;beat++) {
                float start=GatorTauntRecording.onset(phase,beat);
                check("recorded envelope opens mouth on each growl",GatorTauntRecording.mouth(phase,start+.09f)>.2f);
                check("mouth closes before next growl",GatorTauntRecording.mouth(phase,start-.005f)==0);
            }
            b.leave();check("leaving clears growl history",a.growlBeat==-1 && a.takeGrowls()==0);
        }
        GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;
        b.hp=b.hpMax*.5f;b.damage(1);c.sound=null;
        while(a.showoffAge<GatorTauntRecording.onset(0,0)+DT)c.update(DT,L);
        Ear ear=new Ear();c.sound=ear;c.update(DT,L);
        check("muted growls do not play late",ear.gatorGrowls.isEmpty());
        b.damage(b.hp);c.update(DT,L);
        check("defeat cancels growl queue and mouth",a.takeGrowls()==0 && a.growlBeat==-1 && a.growlMouth()==0);
        c=fight(L);b=c.boss;a=b.alligator;b.hp=b.hpMax*.5f;b.damage(1);
        c.update(.2f,L);c.startGame();
        check("restart clears pending growls",a.takeGrowls()==0 && !a.showoff() && a.growlBeat==-1);
    }
    static void recordedAudio(Layout L) {
        group("recorded Gator cues");
        GameCore c=fight(L),same=fight(L);Alligator a=c.boss.alligator;
        int variants=0;
        for(int group=0;group<30;group++) {
            int mask=0;
            for(int i=0;i<3;i++)a.queueAttackCue(i==0);
            int count=a.takeAttackCues();
            check("batched three-hit cues survive until playback",count==3);
            for(int i=0;i<count;i++)mask|=1<<a.attackCues[i];
            check("batched combo draws all three recordings",mask==7);
            variants|=1<<a.attackCues[0];
        }
        check("new attack groups randomly start with all three takes",variants==7);
        check("cue choices leave gameplay randomness untouched",c.rnd.nextLong()==same.rnd.nextLong());
        Ear ear=new Ear();c.sound=ear;
        c.boss.intro=c.boss.introDuration()*(1-AlligatorArrival.SPLASH_AT)+.001f;
        c.paused=true;c.update(DT,L);
        check("pause cannot start the entry splash",ear.gatorEntries==0);
        c.paused=false;c.update(DT,L);
        check("entry crossing plays the slowed splash once",ear.gatorEntries==1 && ear.gatorSplashes==0);
        c.update(DT,L);check("entry splash does not retrigger",ear.gatorEntries==1);
        c.boss.intro=0;c.boss.damage(c.boss.hp);
        c.boss.leaveT=Boss.LEAVE-AlligatorAtmosphere.SPLASH_AT+.001f;
        c.paused=true;c.update(DT,L);
        check("pause holds defeat splash",ear.gatorSplashes==0);
        c.paused=false;c.update(DT,L);
        check("defeat splash coincides with water particles",ear.gatorSplashes==1);
        c.update(DT,L);check("defeat splash does not loop",ear.gatorSplashes==1);
        c=fight(L);c.sound=null;c.boss.alligator.timer=0;c.update(DT,L);
        ear=new Ear();c.sound=ear;c.update(DT,L);
        check("muted attack cue is discarded instead of played late",ear.gatorCues==0);
        c=fight(L);ear=new Ear();c.sound=ear;
        c.boss.intro=c.boss.introDuration()*(1-AlligatorArrival.SPLASH_AT)+.001f;
        c.sound=null;c.update(DT,L);c.sound=ear;c.update(DT,L);
        check("muted splash is never replayed on sound attachment",ear.gatorEntries==0);
    }
    static void comboAudio(Layout L) {
        for(int count:new int[]{2,3}) {
            GameCore c=fight(L);Alligator a=c.boss.alligator;Ear ear=new Ear();c.sound=ear;
            c.boss.hp=c.boss.hpMax*(count==2?.49f:.24f);a.timer=0;c.update(DT,L);
            check("first combo strike sounds once "+count,ear.gatorCues==1);
            for(int i=0;i<count;i++) {
                for(int frame=0;frame<30 && a.combo[i].attack<.15f;frame++)c.update(DT,L);
                Alligator.Strike strike=a.combo[i];
                a.touch(c,L,0,168,strike.tipX-L.w*.12f,strike.tipY);
                a.touch(c,L,1,168,strike.tipX+L.w*.12f,strike.tipY);
                c.update(DT,L);
                check("immediate follow-up strike sounds exactly once "+count+"/"+i,
                        ear.gatorCues==Math.min(count,i+2));
                c.update(DT,L);
                check("attack cue does not repeat on next frame",ear.gatorCues==Math.min(count,i+2));
            }
            check("combo takes never repeat "+count,!ear.gatorCueRepeated && Integer.bitCount(ear.gatorCueMask)==count);
            check("Gator no longer borrows Octopulse rising tone",ear.octoCues==0);
            a.pendingAttackCues=1;c.boss.leave();check("leaving clears pending attack sounds",a.takeAttackCues()==0);
        }
    }
    static void comboShowoff(Layout L) {
        for(int count:new int[]{2,3})for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;
            float threshold=b.hpMax*(count==2?.5f:.25f),dt=1f/hz;
            b.hp=threshold+1;a.daze=Alligator.DAZE;a.longWindow=true;a.roundStreak=4;
            b.press(0,c.rnd,L);
            check("exact combo boundary does not trigger early",!a.showoff());
            b.tailBolt(c.rnd,L.w*.5f,L.playTop);b.press(0,c.rnd,L);
            check("crossing threshold interrupts daze for the matching demo "+count+"/"+hz,
                    a.showoffCount==count && a.daze==0 && !a.longWindow && a.roundStreak==0
                    && !a.comboActive && b.boltCount()==0);
            check("daze cancel flashes and shakes before combo demo",a.cancelFlash()>.8f && a.takeShake()>=.85f && a.showoffLimb()<0);
            float hp=b.hp,clock=a.showoffAge;c.paused=true;c.update(.2f,L);c.paused=false;
            check("pause freezes combo demonstration",a.showoffAge==clock);
            int cueMask=0;int hits=0,cues=0,limbs=0,shakes=0;boolean inputSafe=true,flexed=false;
            for(int frame=0;frame<hz*4 && a.showoff();frame++) {
                int limb=a.showoffLimb();if(limb>=0)limbs|=1<<limb;
                if(a.showoffFlex()>.98f) {
                    flexed=true;
                    inputSafe &= limb<0 && a.drawnY(L,0)<a.rootY(L,0) && a.drawnY(L,1)<a.rootY(L,1)
                            && a.elbowX(L,0,a.drawnX(L,0))<a.drawnX(L,0)
                            && a.elbowX(L,1,a.drawnX(L,1))>a.drawnX(L,1);
                }
                inputSafe &= b.press(0,c.rnd,L)==Boss.REBUFF && b.hp==hp && a.pendingShots==0 && a.retaliation==0;
                inputSafe &= !a.swipe(b,L,0,L.h*.5f,L.w,L.h*.5f);
                hits+=b.update(dt,L,c.rnd);
                int cueCount=a.takeAttackCues();
                for(int i=0;i<cueCount;i++) {
                    check("showoff prompt takes do not repeat",(cueMask&(1<<a.attackCues[i]))==0);
                    cueMask|=1<<a.attackCues[i];
                }
                cues+=cueCount;
                if(a.takeShake()>0)shakes++;
            }
            check("demo ignores attack input without retaliation",inputSafe);
            check("arms flex up before the combo then settle",flexed && a.showoffFlex()==0 && a.cancelFlash()==0);
            check("demo shows ordered claws then optional tail with one cue each",!a.showoff()
                    && limbs==(count==2?3:7) && cues==count && hits==0 && a.daze==0);
            check("each demonstration move shakes once "+count+"/"+hz,shakes==count && a.takeShake()==0);
            b.damage(1);check("threshold demo only happens once",!a.showoff());
            for(int frame=0;frame<hz && !a.comboActive;frame++)b.update(dt,L,c.rnd);
            check("normal combo attacks resume after demo",a.comboActive && a.comboCount==count);
            b.leave();check("leaving clears demonstration history",!a.showoff() && a.shownCombos==0);
        }
        GameCore c=fight(L);Boss b=c.boss;Alligator a=b.alligator;
        b.hp=b.hpMax*.5f;b.damage(1);b.damage(b.hp);b.update(DT,L,c.rnd);
        check("boss defeat cancels demonstration immediately",!a.showoff() && a.takeAttackCues()==0 && a.takeShake()==0);
    }
    static void impactFeedback(Layout L) {
        endingPresentation(L);
        group("gator screen impacts");
        for(int hz:new int[]{30,60,120}) {
            GameCore c=fight(L);c.boss.intro=c.boss.introDuration();c.shake=0;
            int pulses=0;
            for(int frame=0;c.boss.intro>0 && frame<hz*6;frame++) {
                c.shake=0;c.update(1f/hz,L);
                if(c.shake>0)pulses++;
            }
            check("entry shakes at three milestones "+hz,pulses==3);
        }
        GameCore c=fight(L);Alligator a=c.boss.alligator;
        c.boss.hp=c.boss.hpMax*.5f;c.boss.damage(1);
        check("threshold has immediate cancel shake",a.takeShake()>=.85f);
        a.showoffAge=a.showoffLead()+Alligator.SHOWOFF_BEAT*.5f-DT*.5f;
        c.shake=0;c.paused=true;c.update(DT,L);
        check("paused demonstration does not shake",c.shake==0 && a.takeShake()==0);
        c.paused=false;c.update(DT,L);
        check("demo impact reaches the screen and is consumed",c.shake>=.45f && a.takeShake()==0);
        c.shake=0;c.update(DT,L);check("demo impact does not repeat",c.shake==0);
        a.pendingShake=.65f;c.boss.leave();check("leaving clears pending shake",a.takeShake()==0);
        c=fight(L);c.shake=0;
        BossPlay.press(c,0,Boss.HIT,L);
        check("boss damage shakes the screen",c.shake>=.55f);
        c.shake=0;c.takeHit(L.w*.5f,L);
        check("player damage has a stronger shake",c.shake>=1.2f);
    }
    static void all(Layout L) {
        TestGatorRendering.all(L);
        parryCompanion(L);
        companionProps(L);
        contactFeedback(L);
        limbRecoil(L);
        impactFeedback(L);
        comboShowoff(L);
        comboAudio(L);
        recordedAudio(L);
        growlAudio(L);
        clawGlow(L);
        laughingTaunt(L);
        group("gator body follows tail side");
        GameCore curved=fight(L);Alligator curve=curved.boss.alligator;curve.timer=100;
        advance(curved,L,1);
        float right=curve.segments[4].centreX()-curve.segments[0].centreX();
        curve.tailSide=-1;float curveBefore=curve.segments[4].centreX();advance(curved,L,DT);
        check("tail-side switch curves body without a jump",Math.abs(curve.segments[4].centreX()-curveBefore)<L.w*.04f);
        advance(curved,L,1.5f);
        float left=curve.segments[4].centreX()-curve.segments[0].centreX();
        check("lower body bends toward either tail side",right>L.w*.07f && left<-L.w*.07f);
        cleanRounds(L);
        combos(L);
        parryOpenings(L);
        closingEyes(L);
        check("entrance skips offscreen creature detail",entryDrawCalls(L,0)==0);
        check("entry has no layered water passes",entryDrawCalls(L,.56f)<=entryDrawCalls(L,1)+AlligatorArrival.PARTICLES*2);
        randomLimbs(L);
        windupStrike(L);
        retaliation(L);
        animatedExpressions(L);
        expressionFeedback(L);
        damageWriggle(L);
        limbMotion(L);
        damagePacing(L);
        waterEntry(L);
        gummyCompanion(L);
        bodyMotion(L);
        guardFeedback(L);
        group("Swampland progression and stable rewards");
        check("swamp has its own final boss",Boss.kindFor(Lands.firstStage(Lands.SWAMP)+4)==Boss.ALLIGATOR);
        GameCore c=fight(L);
        check("swamp scenery uses the authored land",Lands.forStage(c.stage)==Lands.SWAMP);
        check("mushroom friend opens swamp",LandPicker.unlocked(c,Lands.SWAMP));
        long old=Collect.OLD_TAG|((1L<<60)-1);
        check("existing ducks survive and old tag does not grant a gator",Collect.decode(old)==Collect.OLD_MASK && !Collect.has(Collect.decode(old),60));
        long owned=Collect.add(Collect.decode(old),Collect.ALLIGATOR_FRIEND);
        check("expanded collection round trips",Collect.decode(Collect.encode(owned))==owned);
        check("old boss and duck IDs do not move",Collect.bossPrize(Boss.MUSHROOM)==48 && Collect.DUCK_FIRST==49
                && Collect.bossPrize(Boss.ALLIGATOR)==60 && Collect.bossKind(49)==-1);
        group("alligator contacts and swipe defenses");
        float hp=c.boss.hp;int lives=c.lives;
        check("keys cannot damage during defense",c.boss.press(0,c.rnd,L)==Boss.REBUFF && c.boss.hp==hp);
        c=fight(L);ready(c,L);Alligator a=c.boss.alligator;
        a.touch(c,L,0,4,a.tipX,a.tipY);a.touch(c,L,1,4,a.tipX+L.unit*.05f,a.tipY);
        check("tap and tiny jitter do not block",a.roundStreak==0 && !a.blocked);
        a.touch(c,L,0,4,L.w*.3f,L.playTop);a.touch(c,L,1,4,L.w*.7f,L.playTop);
        check("swipes elsewhere do not block or damage player",a.roundStreak==0 && c.lives==lives);
        int contacts=0;
        for(int i=0;i<100 && contacts==0;i++) {
            int hits=c.boss.update(DT,L,c.rnd);
            if(hits>0)check("damage waits for visible tip contact",Math.hypot(a.tipX-a.targetX,a.tipY-a.targetY)<=a.tipRadius(L)+a.targetR+.01f);
            contacts+=hits;
        }
        check("one incoming limb reports one contact",contacts==1);
        for(int i=0;i<15;i++)contacts+=c.boss.update(DT,L,c.rnd);
        check("retracting limb cannot deal repeated damage",contacts==1);
        c=fight(L);a=c.boss.alligator;
        int tails=0,previous=-1;
        for(int n=0;n<Alligator.ROUNDS_FOR_DAZE;n++) {
            ready(c,L);int limb=a.limb;
            if(limb==2)check("tail alternates sides "+n,a.tailSide==(tails++%2==0?1:-1));
            float before=c.boss.hp;block(c,L);
            check("each parry opens a short window without counting an exchange "+n,
                    c.boss.open() && !a.longWindow && a.roundStreak==n && c.boss.hp==before && a.pointer==-1);
            c.boss.press(n%Glyph.COUNT,c.rnd,L);
            check("first hit after each parry advances the round streak "+n,limb!=previous && a.roundStreak==n+1);previous=limb;
        }
        check("fourth exchange opens a longer vulnerability",a.longWindow && a.daze==Alligator.DAZE && c.boss.hp==hp-4);
        for(int g=0;g<Glyph.COUNT;g++) {
            check("every key can hurt the dazed boss "+g,c.boss.press(g,c.rnd,L)==Boss.HIT);
        }
        check("six extra taps leave long window open",a.daze>0 && c.boss.hp==hp-10);
        for(int n=7;n<18;n++)c.boss.press(n%Glyph.COUNT,c.rnd,L);
        check("long window cap returns to defense and resets streak",a.daze==0 && c.boss.hp==hp-21 && a.roundStreak==0);
        longOpening(c,L);float earned=c.boss.hp;
        advance(c,L,Alligator.DAZE+.1f);
        check("expired window retains earned damage",a.daze==0 && c.boss.hp==earned);
        a.pointer=4;Pause.release(c);check("pause releases swipe ownership",a.pointer==-1);
        c.boss.leave();check("leaving clears limbs, body and gesture",a.limb==-1 && a.roundStreak==0 && a.tummy==null && a.pointer==-1);
        group("alligator game wiring and bounded player");
        c=fight(L);c.lives=1;
        for(int i=0;i<400 && c.state==GameCore.PLAY;i++)c.update(DT,L);
        check("unblocked contact reaches normal game-over flow",c.state!=GameCore.PLAY && c.lives==0);
        for(int hz:new int[]{30,60,120}) {
            c=fight(L);Bot bot=new Bot(6f,.2f,.04f,false,168);float dt=1f/hz,t=0;
            while(c.boss.fighting() && t<120 && c.state==GameCore.PLAY) {bot.step(c,L,dt);c.update(dt,L);t+=dt;}
            check("bounded hands beat alligator at "+hz+"fps",c.boss.beaten && c.lives>0);
            System.out.printf("    alligator %dfps: %.1fs, %d lives%n",hz,t,c.lives);
        }
        c=fight(L);Bot slower=new Bot(4f,.30f,.08f,false,169);float slowerTime=0;
        while(c.boss.fighting() && slowerTime<150 && c.state==GameCore.PLAY) {
            slower.step(c,L,DT);c.update(DT,L);slowerTime+=DT;
        }
        check("slower hands can survive the damaged boss pace",c.boss.beaten && c.lives>0);
        System.out.printf("    alligator slower hands: %.1fs, %d lives%n",slowerTime,c.lives);
        c=fight(L);
        for(int i=0;i<100 && !c.boss.beaten;i++) {
            block(c,L);
            while(c.boss.alligator.daze>0 && !c.boss.beaten) {c.boss.press(0,c.rnd,L);advance(c,L,.16f);}
        }
        c.boss.leaveT=0;BossPlay.endBoss(c,L);
        check("victory awards the mapped boss friend",c.prize==Collect.ALLIGATOR_FRIEND && Collect.has(c.collected,Collect.ALLIGATOR_FRIEND));
        group("Time Attack catalog migration");
        TimeAttack oldRuns=new TimeAttack();StringBuilder legacy=new StringBuilder("1");
        for(int p=0;p<3;p++)for(int i=0;i<5;i++)legacy.append(';').append(i==4?42000:10000+i);
        oldRuns.load(legacy.toString());
        check("legacy solo times preserved and new challenge starts clean",oldRuns.best[0][0]==10000 && oldRuns.best[0][Boss.ALLIGATOR]==0 && oldRuns.best[0][TimeAttack.ALL]==0);
        check("old four-boss records archived",oldRuns.classicBest[0]==42000 && oldRuns.classicHistory[0].runs.get(0).cleared==4);
        TimeAttack again=new TimeAttack();again.load(oldRuns.encode());
        check("archive survives subsequent saves",again.classicBest[0]==42000 && again.best[0][3]==10003);
    }
}
