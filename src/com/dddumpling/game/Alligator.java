package com.dddumpling.game;

/** Swipe defense owns its timing, collision geometry and finger; Boss owns health and lifecycle. */
final class Alligator {
    static final int ROUNDS_FOR_DAZE=4;
    static final float WARN=.60f, REACH=.85f, RETURN=.38f, GAP=.25f, OPEN=.85f, DAZE=3.4f, GUARD_TELL=.28f;
    static final int WINDUP_EYES=10, STRIKE_EYES=11, DAZED_EYES=12, CHEEKY_EYES=13, GUARD_EYES=14;
    static final int SEGMENTS=6;
    static final float COUNTER_SPEED=3f, COUNTER_WINDUP=.30f, COUNTER_COOLDOWN=1.5f;
    static final float RETALIATE=1.95f/COUNTER_SPEED;
    float retaliation;
    int retaliationShots,pendingShots;
    float shotDelay,counterCooldown;
    static final float TAUNT_TIME=1.05f;
    float wriggle,taunt;
    final Softbody[] segments=new Softbody[SEGMENTS];
    final AlligatorGuard guard=new AlligatorGuard();
    final Eyes eyes=new Eyes();
    int limb=-1, next=-1, lastLimb=-1, roundStreak, pointer=-1, tailSide=1, dazes;
    float timer=GAP, attack, daze, recoil, flash, tempo=1, windowDamage, motionClock, tailPosition=1;
    float tipX,tipY,previousX,previousY,targetX,targetY,targetR,strikeX,strikeY;
    float fingerX,fingerY,anchorX,anchorY,strokeX,strokeY,strokeEndX,strokeEndY,strokeFade;
    int pendingAttackCues;
    // Audio randomness never advances the gameplay generator.
    final java.util.Random cueRandom=new java.util.Random(0);
    final int[] attackCues=new int[3];
    int usedCueMask;
    void queueAttackCue(boolean first) {
        if(first)usedCueMask=0;
        int pick=cueRandom.nextInt(3-Integer.bitCount(usedCueMask));
        for(int variant=0;variant<3;variant++)if((usedCueMask&(1<<variant))==0 && pick--==0) {
            usedCueMask|=1<<variant;
            if(pendingAttackCues<attackCues.length)attackCues[pendingAttackCues++]=variant;
            break;
        }
    }
    boolean placed, blocked, dazeCue, hitPlayer, longWindow, windowHit, windowDamaged;
    float closing,vulnerablePose;
    Softbody tummy;
    static final float COMBO_GAP=1.04f;
    final Strike[] combo={new Strike(),new Strike(),new Strike()};
    final int[] comboOrder=new int[3];
    boolean comboActive;
    int comboLaunched,comboCount,comboParried;
    float comboAge,comboNextAt;
    static final float SHOWOFF_LEAD=1.05f, SHOWOFF_BEAT=.55f, SHOWOFF_HOLD=.35f;
    int showoffCount,showoffBeat=-1,showoffImpact=-1,shownCombos;
    float showoffAge,pendingShake;
    float takeShake() {float amount=pendingShake;pendingShake=0;return amount;}
    boolean showoff() {return showoffCount>0;}
    float showoffFlex() {
        if(!showoff())return 0;
        return AlligatorArrival.ramp(showoffAge,0,.24f)*(1-AlligatorArrival.ramp(showoffAge,.68f,SHOWOFF_LEAD));
    }
    float cancelFlash() {
        if(!showoff())return 0;
        float pulse=showoffAge<.10f?.85f:showoffAge<.18f?.30f:.72f;
        return pulse*(1-AlligatorArrival.ramp(showoffAge,.24f,.44f));
    }
    int showoffLimb() {
        int beat=(int)((showoffAge-SHOWOFF_LEAD)/SHOWOFF_BEAT);
        return showoffAge<SHOWOFF_LEAD || beat>=showoffCount?-1:beat;
    }
    float showoffReach(int which) {
        if(!showoff() || showoffLimb()!=which)return 0;
        float u=(showoffAge-SHOWOFF_LEAD)/SHOWOFF_BEAT-which;
        float reach=(float)Math.sin(Math.PI*u);
        return reach*reach;
    }
    boolean damageThreshold(Boss b,float before) {
        if(b.hp<=0)return false;
        int count=before>=b.hpMax*.25f && b.hp<b.hpMax*.25f?3
                :before>=b.hpMax*.5f && b.hp<b.hpMax*.5f?2:0;
        if(count==0 || (shownCombos&(1<<count))!=0)return false;
        shownCombos|=count==3?12:4;
        showoffCount=count;showoffBeat=showoffImpact=-1;showoffAge=0;
        clearCombo();limb=next=pointer=-1;placed=blocked=hitPlayer=false;
        daze=recoil=closing=windowDamage=retaliation=shotDelay=counterCooldown=taunt=0;
        longWindow=windowHit=windowDamaged=dazeCue=false;roundStreak=0;
        pendingShots=retaliationShots=pendingAttackCues=0;guard.reset();
        pendingShake=Math.max(pendingShake,.85f);jolt(.6f);
        return true;
    }
    void updateShowoff(Boss b,float dt) {
        showoffAge+=dt;
        int beat=Math.min(showoffCount-1,(int)((showoffAge-SHOWOFF_LEAD)/SHOWOFF_BEAT));
        if(showoffAge>=SHOWOFF_LEAD)while(showoffBeat<beat) {
            showoffBeat++;queueAttackCue(showoffBeat==0);jolt(.15f);
            if(b.body!=null)b.body.squash(.16f);
        }
        float impacts=(showoffAge-SHOWOFF_LEAD-SHOWOFF_BEAT*.5f)/SHOWOFF_BEAT;
        if(impacts>=0) {
            int impact=Math.min(showoffCount-1,(int)impacts);
            if(impact>showoffImpact) {showoffImpact=impact;pendingShake=Math.max(pendingShake,.45f+.10f*impact);}
        }
        if(showoffAge>=SHOWOFF_LEAD+showoffCount*SHOWOFF_BEAT+SHOWOFF_HOLD) {
            showoffCount=0;showoffAge=0;showoffBeat=-1;timer=.55f;limb=next=-1;
        }
    }

    /** Each overlapping limb keeps the same wind-up, swept contact and recoil as a solo strike. */
    static final class Strike {
        int limb=-1;
        float attack,recoil,tempo,tipX,tipY,previousX,previousY,strikeX,strikeY;
        boolean placed,blocked,hitPlayer;
        void save(Alligator a) {
            limb=a.limb;attack=a.attack;recoil=a.recoil;tempo=a.tempo;
            tipX=a.tipX;tipY=a.tipY;previousX=a.previousX;previousY=a.previousY;
            strikeX=a.strikeX;strikeY=a.strikeY;placed=a.placed;blocked=a.blocked;hitPlayer=a.hitPlayer;
        }
        void load(Alligator a) {
            a.limb=limb;a.attack=attack;a.recoil=recoil;a.tempo=tempo;
            a.tipX=tipX;a.tipY=tipY;a.previousX=previousX;a.previousY=previousY;
            a.strikeX=strikeX;a.strikeY=strikeY;a.placed=placed;a.blocked=blocked;a.hitPlayer=hitPlayer;
        }
    }
    void clearCombo() {
        comboActive=false;comboLaunched=comboCount=comboParried=0;comboAge=comboNextAt=0;
        for(Strike s:combo)s.limb=-1;
    }
    Strike strikeFor(int which) {
        if(comboActive)for(Strike s:combo)if(s.limb==which)return s;
        return null;
    }
    boolean attacking(int which) {
        if(showoff())return showoffLimb()==which;
        Strike s=strikeFor(which);
        return s!=null?!s.blocked:!comboActive && limb==which && !blocked;
    }
    float windup(int which) {
        Strike s=strikeFor(which);
        return s!=null?s.attack*s.tempo:attack*tempo;
    }
    void selectComboLead() {
        for(Strike s:combo)if(s.limb>=0 && !s.blocked) {s.load(this);return;}
        for(Strike s:combo)if(s.limb>=0) {s.load(this);return;}
        limb=-1;placed=false;
    }

    void reset() {
        clearCombo();showoffCount=shownCombos=0;showoffBeat=showoffImpact=-1;showoffAge=pendingShake=0;
        guard.reset();eyes.reset();wriggle=retaliation=taunt=0;retaliationShots=pendingShots=0;shotDelay=counterCooldown=0;
        limb=pointer=next=lastLimb=-1;roundStreak=dazes=0;
        timer=GAP;attack=daze=recoil=flash=strokeFade=windowDamage=0;tempo=1;motionClock=0;tailPosition=1;
        tipX=tipY=previousX=previousY=targetX=targetY=targetR=strikeX=strikeY=0;
        pendingAttackCues=0;placed=blocked=dazeCue=hitPlayer=longWindow=windowHit=windowDamaged=false;closing=vulnerablePose=0;tummy=null;tailSide=1;
        for(int i=0;i<SEGMENTS;i++)segments[i]=null;
    }
    /** Continuous channels can change destination mid-expression without restarting a pose. */
    static final class Eyes {
        float x,y,rx=.115f,ry=.34f,smile,squeeze,dizzy,brow,tilt,glint,blink,clock;
        void reset() {x=y=smile=squeeze=dizzy=brow=tilt=glint=blink=clock=0;rx=.115f;ry=.34f;}
        void update(int mood,float look,float down,float dt) {
            clock+=dt;
            float k=1-(float)Math.exp(-dt*(mood==3?30:20));
            float blinkTarget=mood==0?Math.max(0,1-Math.abs(clock%4.2f-3.8f)/.12f):0;
            blink+=(blinkTarget-blink)*k;
            boolean tell=mood==WINDUP_EYES || mood==GUARD_EYES,strike=mood==STRIKE_EYES;
            boolean happy=mood==CHEEKY_EYES || mood==8;
            boolean closed=happy || mood==3 || mood==DAZED_EYES;
            x+=((closed?0:look)-x)*k;y+=((closed?0:down)-y)*k;
            rx+=((tell?.075f:strike?.10f:.115f)-rx)*k;
            ry+=((tell?.27f:mood==2?.42f:.34f)-ry)*k;
            smile+=((happy?1:0)-smile)*k;
            squeeze+=((mood==3?1:0)-squeeze)*k;
            dizzy+=((mood==DAZED_EYES?1:0)-dizzy)*k;
            brow+=((tell || strike || mood==2 || happy?1:0)-brow)*k;
            tilt+=((tell?.12f:strike?.08f:happy?-.10f:0)-tilt)*k;
            glint+=((tell?1:0)-glint)*k;
        }
        float closed() {return Math.max(blink,Math.max(smile,squeeze));}
    }
    // Reach the fastest pace at 75% health; later difficulty comes from limb combos.
    static float speed(Boss b) {
        float health=b.hpMax<=0?1:Math.max(.75f,Math.min(1,b.hp/b.hpMax));
        return 1+.85f*(1-health*health*health);
    }
    void endDaze(Boss b) {
        if(longWindow)roundStreak=0;
        timer=(longWindow?.7f:GAP)/speed(b);
        daze=windowDamage=0;longWindow=windowHit=windowDamaged=false;closing=.35f;
    }
    void playerDamaged() {
        roundStreak=0;if(daze>0)windowDamaged=true;
        guard.returnHome();
        taunt=TAUNT_TIME;jolt(.20f);
    }
    float laugh() {
        if(taunt<=0)return 0;
        float age=TAUNT_TIME-taunt;
        float envelope=Math.min(1,age/.10f)*Math.min(1,taunt/.22f);
        return envelope*(.4f+.6f*(float)Math.pow(Math.sin(age*Math.PI*4),2));
    }
    void target(GameCore c,Layout L) {
        targetX=RunCompanion.x(L);
        targetY=RunCompanion.y(L)+c.companion.damagePush(L);
        targetR=RunCompanion.halfHeight(L)*.85f;
    }
    float rootX(Layout L,int which) {
        if(which==2)return L.w*(.5f+tailPosition*.63f);
        return L.w*(which==0?.23f:.77f)+(tummy==null?0:(tummy.centreX()-L.w*.5f)*.7f);
    }
    float rootY(Layout L,int which) {
        float h=L.dangerY-L.playTop;
        if(which==2) {
            float side=L.playTop+h*(.73f+.20f*Math.max(0,-tailPosition));
            return side+(L.h+L.unit*3-side)*(1-AlligatorArrival.ramp(Math.abs(tailPosition),.78f,1));
        }
        return L.playTop+h*(.41f+.025f*vulnerablePose-.035f*showoffFlex())+L.unit*.18f*(float)Math.sin(motionClock*2.1f+which);
    }
    float restX(Layout L,int which) {
        float idle=L.w*(which==2?.5f+tailPosition*.37f:(which==0?.27f:.73f)
                +.105f*(float)Math.sin(motionClock*.80f+which*2.2f)
                +.035f*(float)Math.sin(motionClock*1.7f+which*.9f));
        float hang=rootX(L,which)+L.w*.085f*(float)Math.sin(motionClock*3.3f+which*.7f);
        return which==2?idle:idle+(hang-idle)*vulnerablePose;
    }
    float restY(Layout L,int which) {
        float h=L.dangerY-L.playTop;
        float idle=L.playTop+h*(which==2?.79f+.065f*(1-tailPosition*tailPosition)
                :.56f+.11f*(float)Math.sin(motionClock*.72f+which*1.8f)
                +.035f*(float)Math.sin(motionClock*1.27f+which*.6f));
        float swing=(float)Math.sin(motionClock*3.3f+which*.7f);
        float hang=rootY(L,which)+h*(.35f-.025f*swing*swing);
        return which==2?idle:idle+(hang-idle)*vulnerablePose;
    }
    float elbowX(Layout L,int which,float tx) {
        Strike s=strikeFor(which);
        float extend=s!=null?strikeProgress(s.attack,s.tempo):limb==which?strikeProgress():0;
        float rest=(rootX(L,which)+tx)*.5f+(which==0?-1:1)*L.w*.085f*(1-extend*.6f)*(1-.8f*vulnerablePose);
        return rest+(L.w*(which==0?.10f:.90f)-rest)*showoffFlex();
    }
    float elbowY(Layout L,int which,float ty) {
        float rest=(rootY(L,which)+ty)*.5f+(L.dangerY-L.playTop)*.035f;
        return rest+(rootY(L,which)+(L.dangerY-L.playTop)*.065f-rest)*showoffFlex();
    }
    int mood(Boss b) {
        if(b.beaten)return 8;
        if(guardReturning(b))return GUARD_EYES;
        if(b.hurt>.04f)return 3;
        if(showoff())return CHEEKY_EYES;
        if(daze>0)return DAZED_EYES;
        if(retaliation>0)return WINDUP_EYES;
        if(taunt>0 && (limb<0 || blocked))return CHEEKY_EYES;
        if(blocked && recoil>0)return hitPlayer?CHEEKY_EYES:2;
        if(limb>=0)return attack*tempo<WARN?WINDUP_EYES:STRIKE_EYES;
        return b.intro>0?2:0;
    }
    boolean guardReturning(Boss b) {
        return daze>0 && (daze<=GUARD_TELL || (longWindow && windowDamage>=b.hpMax*.25f-2));
    }
    float gazeX() {
        int which=limb<0?next:limb;
        if((blocked && recoil>0) || daze>0)return 0;
        if(limb>=0 && attack*tempo>=WARN)return 0;
        return which<0?0:which==2?tailSide:which==0?-1:1;
    }
    float gazeY() {
        if(daze>0)return daze<=GUARD_TELL?1:0;
        if(blocked && recoil>0)return 0;
        return (limb>=0 && attack*tempo>=WARN) || (limb<0?next:limb)==2?1:0;
    }
    float crossedArms() {
        float guard=!comboActive && closing>0?(float)Math.sin(Math.PI*(1-closing/.35f)):0;
        float phase=retaliation*COUNTER_SPEED;
        float counter=retaliation<=0?0:AlligatorArrival.ramp(phase,0,.22f)
                *(1-AlligatorArrival.ramp(phase,1.6f,RETALIATE*COUNTER_SPEED));
        return Math.max(guard,counter);
    }
    float tailFlick() {
        if(retaliation<=0)return 0;
        float phase=retaliation*COUNTER_SPEED;
        return AlligatorArrival.ramp(phase,.35f,.55f)*(1-AlligatorArrival.ramp(phase,1.50f,1.80f));
    }
    float tailSwing() {
        float phase=retaliation*COUNTER_SPEED;
        return AlligatorArrival.ramp(phase,.55f,.95f)-AlligatorArrival.ramp(phase,1.10f,1.50f);
    }
    float counterTailX(Layout L) {
        float u=tailSwing();
        return L.w*(.5f+tailSide*(.32f-.64f*u));
    }
    float counterTailY(Layout L) {
        float u=tailSwing();
        return L.playTop+(L.dangerY-L.playTop)*(.64f-.08f*4*u*(1-u));
    }
    float drawnX(Layout L,int which) {
        if(showoff()) {
            float rest=restX(L,which),aim=L.w*(which==0?.43f:which==1?.57f:.5f);
            if(which<2)rest+=(L.w*(which==0?.17f:.83f)-rest)*showoffFlex();
            return rest+(aim-rest)*showoffReach(which);
        }
        Strike s=strikeFor(which);
        float x=s!=null?s.tipX:limb==which?tipX:restX(L,which);
        if(which<2)x+=(restX(L,which)-x)*vulnerablePose;
        float blend=which==2?tailFlick():crossedArms();
        float target=which==2?counterTailX(L):L.w*(which==0?.65f:.35f);
        x+=(target-x)*blend;
        return guard.drawnLimbX(L,which,x);
    }
    float drawnY(Layout L,int which) {
        if(showoff()) {
            float rest=restY(L,which),aim=L.playTop+(L.dangerY-L.playTop)*(which==2?.60f:.45f);
            if(which<2)rest+=(L.playTop+(L.dangerY-L.playTop)*(.20f+.012f*(float)Math.sin(showoffAge*18))-rest)*showoffFlex();
            return rest+(aim-rest)*showoffReach(which);
        }
        Strike s=strikeFor(which);
        float y=s!=null?s.tipY:limb==which?tipY:restY(L,which);
        if(which<2)y+=(restY(L,which)-y)*vulnerablePose;
        float blend=which==2?tailFlick():crossedArms();
        float target=which==2?counterTailY(L):L.playTop+(L.dangerY-L.playTop)*(which==0?.38f:.42f);
        y+=(target-y)*blend;
        return guard.drawnLimbY(L,which,y);
    }
    boolean showGuide() {return dazes==0;}
    void retireLimb() {if(limb==2)tailSide=-tailSide;limb=-1;placed=false;}
    float tipRadius(Layout L) {return L.w*(limb==2?.055f:.085f);}
    float centreAt(Layout L,float y) {
        if(segments[0]==null)return L.w*.5f;
        for(int i=1;i<SEGMENTS;i++)if(segments[i]!=null && y<segments[i].centreY()) {
            Softbody a=segments[i-1],b=segments[i];
            float u=Math.max(0,Math.min(1,(y-a.centreY())/Math.max(1,b.centreY()-a.centreY())));
            u=u*u*(3-2*u);return a.centreX()+(b.centreX()-a.centreX())*u;
        }
        return segments[SEGMENTS-1]==null?segments[0].centreX():segments[SEGMENTS-1].centreX();
    }
    float[] bodyOutline(Layout L) {
        float top=L.playTop+(L.dangerY-L.playTop)*.235f,bottom=L.h+(L.h-L.playTop)*.28f;
        float[] shape=new float[132];
        for(int i=0;i<=32;i++) {
            float t=i/32f,y=top+(bottom-top)*t;
            float round=(float)Math.sin(Math.PI*Math.max(0,Math.min(1,(t-.08f)/.92f)));
            float x=centreAt(L,y),w=L.w*(.18f+.15f*AlligatorArrival.ramp(t,0,.18f)
                    +.045f*round+.009f*(float)Math.sin(motionClock*2.1f-t*9));
            shape[i*2]=x-w;shape[i*2+1]=y;
            shape[(65-i)*2]=x+w;shape[(65-i)*2+1]=y;
        }
        return shape;
    }
    void jolt(float strength) {
        if(strength>=.3f)wriggle=Math.max(wriggle,strength);
        if(tummy!=null)tummy.squash(strength*.6f);
        for(int i=0;i<SEGMENTS;i++)if(segments[i]!=null) {
            segments[i].squash(strength*(.75f+i*.10f));
            segments[i].impulse(segments[i].centreX()+segments[i].radiusX()*.35f,
                    segments[i].centreY()-segments[i].radiusY(),strength*.3f);
        }
    }
    float strikeProgress() {
        return strikeProgress(attack,tempo);
    }
    static float strikeProgress(float attack,float tempo) {
        float u=Math.max(0,Math.min(1,(attack*tempo-WARN)/REACH));
        return u*u*u;
    }
    void pose(Layout L) {
        if(limb<0)return;
        float height=L.dangerY-L.playTop;
        if(!placed) {
            strikeX=restX(L,limb)/L.w;strikeY=(restY(L,limb)-L.playTop)/height;placed=true;
        }
        float u=strikeProgress();
        if(blocked)u*=Math.max(0,recoil*tempo/RETURN);
        // Coil through the tell, then launch from that held pose without easing off near impact.
        float wind=Math.min(1,attack*tempo/WARN);
        wind=wind*wind*(3-2*wind);
        if(blocked)wind*=Math.max(0,recoil*tempo/RETURN);
        // Commit the wandering paw at the tell, then blend back into idle during recoil.
        float settle=blocked?1-Math.max(0,recoil*tempo/RETURN):0;
        settle=settle*settle*(3-2*settle);
        float x=strikeX*L.w+(restX(L,limb)-strikeX*L.w)*settle;
        float y=L.playTop+strikeY*height+(restY(L,limb)-L.playTop-strikeY*height)*settle;
        x+=(limb==2?tailSide:limb==0?-1:1)*L.w*.060f*wind;
        y-=height*.040f*wind;
        tipX=x+(targetX-x)*u;tipY=y+(targetY-y)*u;
    }
    void animate(Boss b,float dt,Layout L) {
        taunt=b.beaten?0:Math.max(0,taunt-dt);
        float slump=daze>0 && !b.beaten?(longWindow?1f:.75f)*Math.min(1,daze/GUARD_TELL):0;
        vulnerablePose+=(slump-vulnerablePose)*(1-(float)Math.exp(-dt*18));
        eyes.update(mood(b),gazeX(),guardReturning(b)?1:gazeY(),dt);
        motionClock+=dt;wriggle=Math.max(0,wriggle-dt*1.25f);
        if(limb!=2 && strikeFor(2)==null) {
            float step=dt*2.5f;
            tailPosition+=Math.max(-step,Math.min(step,tailSide-tailPosition));
        }
        if(targetR==0) {targetX=RunCompanion.x(L);targetY=RunCompanion.y(L);targetR=RunCompanion.halfHeight(L)*.85f;}
        float height=L.dangerY-L.playTop;
        float top=b.bodyY(L)+Boss.bodyR(L)*.18f,bottom=L.h+(L.h-L.playTop)*.28f;
        float y=(top+bottom)*.5f,r=(bottom-top)*.5f;
        float reach=limb<0?0:(float)Math.sin(Math.min(1,attack*tempo/(WARN+REACH))*Math.PI);
        float sway=(float)Math.sin(motionClock*1.8f)*L.w*.018f
                +(float)Math.sin(motionClock*11f)*L.w*.045f*wriggle+(limb==0?-1:1)*reach*L.w*.012f;
        if(tummy==null) {tummy=new Softbody(Softbody.NODES,168);tummy.reset(L.w*.5f,y,r,L.w*.46f/r);tummy.jiggle=1.6f;}
        tummy.moveTo(L.w*.5f+sway,y+(float)Math.sin(b.age*2.1f)*L.unit*.16f-L.unit*.55f*laugh());
        tummy.update(dt);
        float stretch=Math.max(.88f,Math.min(1.12f,tummy.radiusY()/r));
        for(int i=0;i<SEGMENTS;i++) {
            float first=L.playTop+height*.35f;
            float spacing=(L.h+L.unit*2-first)/(SEGMENTS-1);
            float sy=first+i*spacing,ry=spacing*.60f;
            float rx=L.w*(.267f-.045f*Math.abs(i-2.5f)/2.5f);
            float wave=(float)Math.sin(b.age*2.1f-i*.52f);
            if(segments[i]==null) {
                segments[i]=new Softbody(Softbody.NODES,169+i);segments[i].reset(L.w*.5f,sy,ry,rx/ry);
                segments[i].jiggle=1.6f+i*.12f;
            }
            float down=i/(float)(SEGMENTS-1);
            segments[i].moveTo(tummy.centreX()+tailPosition*L.w*.18f*down*down+wave*L.w*.018f
                    +(float)Math.sin(motionClock*11-i*.82f)*L.w*.055f*wriggle,
                    y+(sy-y)*stretch+wave*L.unit*.16f+reach*L.unit*.3f*(i%2==0?1:-1)
                    -L.unit*.35f*laugh()*(1-i*.08f));
            segments[i].update(dt);
        }
        closing=Math.max(0,closing-dt);
        guard.update(dt);
        if(b.beaten)guard.reset();
        flash=Math.max(0,flash-dt*3);strokeFade=Math.max(0,strokeFade-dt);
        if(b.beaten) {pendingShake=0;showoffCount=0;showoffAge=0;showoffBeat=showoffImpact=-1;pendingAttackCues=0;clearCombo();limb=pointer=-1;daze=retaliation=closing=vulnerablePose=0;longWindow=windowHit=windowDamaged=false;roundStreak=0;retaliationShots=pendingShots=0;shotDelay=counterCooldown=0;}
        if(comboActive) {
            for(Strike s:combo)if(s.limb>=0) {s.load(this);pose(L);s.save(this);}
            selectComboLead();
        } else pose(L);
    }
    int takeAttackCues() {int count=pendingAttackCues;pendingAttackCues=0;return count;}
    void beginStrike(Boss b,Layout L,int which) {
        limb=lastLimb=which;tempo=speed(b);attack=0;blocked=false;placed=false;hitPlayer=false;
        queueAttackCue(!comboActive);jolt(.075f);pose(L);
    }
    void beginCombo(Boss b,java.util.Random rnd) {
        clearCombo();comboActive=true;comboCount=b.hp<b.hpMax*.25f?3:2;
        comboLaunched=1;comboNextAt=COMBO_GAP;comboOrder[0]=limb;combo[0].save(this);
        int second=rnd.nextInt(2);if(second>=limb)second++;
        comboOrder[1]=second;comboOrder[2]=3-limb-second;
    }
    int advanceStrike(Boss b,float dt,Layout L) {
        if(blocked) {
            recoil=Math.max(0,recoil-dt);pose(L);
            if(recoil==0) {retireLimb();timer=GAP/speed(b);}
            return 0;
        }
        previousX=tipX;previousY=tipY;attack+=dt;pose(L);
        if(distance(targetX,targetY,previousX,previousY,tipX,tipY)<=targetR+tipRadius(L)) {
            blocked=true;hitPlayer=true;recoil=RETURN/tempo;pointer=-1;return 1;
        }
        return 0;
    }
    int updateCombo(Boss b,float dt,Layout L) {
        float before=comboAge;comboAge+=dt;
        for(int i=0;i<comboCount;i++) {
            Strike s=combo[i];float step=dt;
            if(i>=comboLaunched) {
                if(comboAge<comboNextAt)break;
                beginStrike(b,L,comboOrder[i]);s.save(this);comboLaunched++;
                step=comboAge-Math.max(before,comboNextAt);comboNextAt+=COMBO_GAP;
            }
            if(s.limb<0)continue;
            s.load(this);int contact=advanceStrike(b,step,L);s.save(this);
            if(contact>0) {
                // Keep the contacting limb's recoil, but cancel all remaining strikes.
                clearCombo();pointer=-1;return 1;
            }
        }
        selectComboLead();
        if(comboLaunched==comboCount && limb<0) {comboActive=false;timer=GAP/speed(b);}
        return 0;
    }
    void staggerCombo() {
        if(!comboActive)return;
        comboLaunched=comboCount;
        for(Strike s:combo)if(s.limb>=0 && !s.blocked) {s.blocked=true;s.recoil=RETURN/s.tempo;}
        selectComboLead();
    }
    int update(Boss b,float dt,Layout L,java.util.Random rnd) {
        dazeCue=false;
        if(showoff()) {updateShowoff(b,dt);return 0;}
        counterCooldown=Math.max(0,counterCooldown-dt);
        if(retaliation>0) {
            retaliation+=dt;
            shotDelay-=dt;
            if(pendingShots>0 && shotDelay<=0 && b.tailBolt(rnd,drawnX(L,2),drawnY(L,2))>=0) {
                pendingShots=0;retaliationShots++;counterCooldown=COUNTER_COOLDOWN;jolt(.18f);
            }
            shotDelay=Math.max(0,shotDelay);
            if(retaliation>=1.5f/COUNTER_SPEED && pendingShots>0)retaliation=.55f/COUNTER_SPEED;
            if(retaliation>=RETALIATE)retaliation=0;
            return 0;
        }
        if(comboActive) {
            if(daze>0) {daze=Math.max(0,daze-dt);if(daze==0)endDaze(b);}
            return updateCombo(b,dt,L);
        }
        if(daze>0) {
            if(limb>=0 && blocked) {
                recoil=Math.max(0,recoil-dt);pose(L);
                if(recoil==0)retireLimb();
            }
            daze=Math.max(0,daze-dt);
            if(daze==0)endDaze(b);
            return 0;
        }
        if(limb<0) {
            if(next<0) {
                int pick=rnd.nextInt(lastLimb<0?3:2);
                next=lastLimb>=0 && pick>=lastLimb?pick+1:pick;
            }
            timer-=dt;
            if(timer<=0) {beginStrike(b,L,next);next=-1;if(b.hp<b.hpMax*.5f)beginCombo(b,rnd);}
            return 0;
        }
        return advanceStrike(b,dt,L);
    }
    boolean swipe(Boss b,Layout L,float x0,float y0,float x1,float y1) {
        if(!b.fighting() || showoff() || retaliation>0 || longWindow)return false;
        if(comboActive) {
            for(Strike s:combo)if(s.limb>=0 && !s.blocked) {
                s.load(this);
                if(parry(b,L,x0,y0,x1,y1)) {
                    s.save(this);
                    if(comboLaunched<comboCount) {
                        beginStrike(b,L,comboOrder[comboLaunched]);combo[comboLaunched++].save(this);
                        comboNextAt=comboAge+COMBO_GAP;
                        // Keep this swipe's impact effects anchored to the parried limb.
                        s.load(this);
                    }
                    return true;
                }
            }
            selectComboLead();return false;
        }
        return daze<=0 && parry(b,L,x0,y0,x1,y1);
    }
    boolean parry(Boss b,Layout L,float x0,float y0,float x1,float y1) {
        if(limb<0 || blocked || attack<.12f)return false;
        if(distance(tipX,tipY,x0,y0,x1,y1)>tipRadius(L)+L.w*.07f)return false;
        blocked=true;recoil=RETURN/tempo;pointer=-1;
        if(!comboActive || ++comboParried==comboCount) {
            daze=OPEN;longWindow=windowHit=windowDamaged=false;windowDamage=closing=0;dazeCue=true;
        }
        strokeX=x0;strokeY=y0;strokeEndX=x1;strokeEndY=y1;strokeFade=.3f;
        return true;
    }
    int press(Boss b,Layout L) {
        if(showoff())return Boss.REBUFF;
        if(daze<=0) {
            if(retaliation==0 && counterCooldown<=0) {
                retaliation=.001f;retaliationShots=0;shotDelay=COUNTER_WINDUP;pointer=-1;jolt(.18f);
                pendingShots=1;counterCooldown=COUNTER_COOLDOWN;
            }
            b.hitX=L.w*.5f;b.hitY=L.playTop+(L.dangerY-L.playTop)*.40f;
            return Boss.REBUFF;
        }
        float damage=longWindow?Math.min(1f,b.hpMax*.25f-windowDamage):1f;
        if(damage<=0) {endDaze(b);return Boss.REBUFF;}
        windowDamage+=damage;
        b.hitX=b.bodyX(L);b.hitY=b.bodyY(L);
        int verdict=b.damage(damage);
        if(showoff())return verdict;
        if(!windowHit && !longWindow && !b.beaten) {
            windowHit=true;
            if(!windowDamaged)roundStreak++;
            if(roundStreak>=ROUNDS_FOR_DAZE) {
                longWindow=true;daze=DAZE;windowDamage=damage;dazes++;dazeCue=true;jolt(.8f);staggerCombo();
                guard.dazeBurst();pendingShake=Math.max(pendingShake,.85f);
            }
        }
        if(longWindow && windowDamage>=b.hpMax*.25f && !b.beaten)endDaze(b);
        return verdict;
    }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(c.state!=GameCore.PLAY || c.boss.kind!=Boss.ALLIGATOR || !c.boss.fighting()
                || c.paused || c.settingsOpen || c.onboarding.briefing) {pointer=-1;return false;}
        if(showoff()) {pointer=-1;return true;}
        if(action==3) {boolean had=pointer>=0;pointer=-1;return had;}
        if(action==0 || action==5) {
            if(pointer>=0 || c.keyAt(x,y,L)>=0 || y<L.playTop)return false;
            pointer=id;fingerX=anchorX=x;fingerY=anchorY=y;guard.beginGesture();return true;
        }
        if(pointer!=id)return false;
        if(action==2 || action==1 || action==6) {
            guard.track(x,y);
            float dx=x-anchorX,dy=y-anchorY;
            if(dx*dx+dy*dy>=L.w*L.w*.0064f) {
                boolean defended=swipe(c.boss,L,anchorX,anchorY,x,y);
                guard.emit(c,L,anchorX,anchorY,x,y,defended);
                if(defended) {
                    c.timeAttack.defended();c.score+=GameCore.BOSS_HIT;
                    guard.prepareImpact(c,L);
                }
                anchorX=x;anchorY=y;
            }
            fingerX=x;fingerY=y;
            if(action!=2)pointer=-1;
        }
        return true;
    }
    static float distance(float x,float y,float ax,float ay,float bx,float by) {
        float dx=bx-ax,dy=by-ay,d=dx*dx+dy*dy;
        float t=d==0?0:Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/d));
        return (float)Math.hypot(x-ax-dx*t,y-ay-dy*t);
    }
}
