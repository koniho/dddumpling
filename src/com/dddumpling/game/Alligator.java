package com.dddumpling.game;

/** Swipe defense owns its timing, collision geometry and finger; Boss owns health and lifecycle. */
final class Alligator {
    static final int BLOCKS=9, WINDOW_HITS=6;
    static final float WARN=.60f, REACH=.85f, RETURN=.38f, GAP=.25f, DAZE=3.4f;
    int limb=-1, next, blocks, windowHits, pointer=-1;
    float timer=GAP, attack, daze, recoil, flash;
    float tipX,tipY,previousX,previousY,targetX,targetY,targetR;
    float fingerX,fingerY,anchorX,anchorY,strokeX,strokeY,strokeEndX,strokeEndY,strokeFade;
    boolean placed, blocked, warningCue, dazeCue;
    Softbody tummy;

    void reset() {
        limb=pointer=-1;next=blocks=windowHits=0;
        timer=GAP;attack=daze=recoil=flash=strokeFade=0;
        tipX=tipY=previousX=previousY=targetX=targetY=targetR=0;
        placed=blocked=warningCue=dazeCue=false;tummy=null;
    }
    void target(GameCore c,Layout L) {
        targetX=RunCompanion.x(L);
        targetY=RunCompanion.y(L)+c.companion.damagePush(L);
        targetR=RunCompanion.halfHeight(L)*.85f;
    }
    float rootX(Layout L,int which) {return which==2?L.w*.72f:which==0?L.w*.06f:L.w*.94f;}
    float rootY(Layout L,int which) {return which==2?L.h+L.unit*2:L.playTop+(L.dangerY-L.playTop)*.27f;}
    float restX(Layout L,int which) {return which==2?L.w*.67f:which==0?L.w*.19f:L.w*.81f;}
    float restY(Layout L,int which) {return which==2?L.dangerY+L.unit*.9f:L.playTop+(L.dangerY-L.playTop)*.44f;}
    float tipRadius(Layout L) {return L.w*(limb==2?.045f:.052f);}
    void pose(Layout L) {
        if(limb<0)return;
        float u=Math.max(0,Math.min(1,(attack-WARN)/REACH));
        u=u*u*(3-2*u);
        if(blocked)u*=Math.max(0,recoil/RETURN);
        tipX=restX(L,limb)+(targetX-restX(L,limb))*u;
        tipY=restY(L,limb)+(targetY-restY(L,limb))*u;
    }
    void animate(Boss b,float dt,Layout L) {
        if(targetR==0) {targetX=RunCompanion.x(L);targetY=RunCompanion.y(L);targetR=RunCompanion.halfHeight(L)*.85f;}
        float top=b.bodyY(L)+Boss.bodyR(L)*.15f,bottom=L.dangerY+L.unit*1.5f;
        float y=(top+bottom)*.5f,r=(bottom-top)*.5f;
        if(tummy==null) {tummy=new Softbody(Softbody.NODES,168);tummy.reset(L.w*.5f,y,r,L.w*.48f/r);tummy.jiggle=1.6f;}
        tummy.moveTo(L.w*.5f,y);
        tummy.update(dt);
        flash=Math.max(0,flash-dt*3);strokeFade=Math.max(0,strokeFade-dt);
        if(b.beaten) {limb=pointer=-1;daze=0;}
        pose(L);
    }
    int update(Boss b,float dt,Layout L) {
        warningCue=dazeCue=false;
        if(daze>0) {
            daze=Math.max(0,daze-dt);
            if(daze==0) {blocks=windowHits=0;timer=.7f;}
            return 0;
        }
        if(limb<0) {
            timer-=dt;
            if(timer<=0) {limb=next++%3;attack=0;blocked=false;warningCue=true;pose(L);}
            return 0;
        }
        if(blocked) {
            recoil=Math.max(0,recoil-dt);pose(L);
            if(recoil==0) {limb=-1;timer=GAP;}
            return 0;
        }
        previousX=tipX;previousY=tipY;
        attack+=dt;pose(L);
        // Sweep the actual tip to avoid tunnelling past the companion on a slow frame.
        if(distance(targetX,targetY,previousX,previousY,tipX,tipY)<=targetR+tipRadius(L)) {
            blocked=true;recoil=RETURN;pointer=-1;
            return 1;
        }
        return 0;
    }
    boolean swipe(Boss b,Layout L,float x0,float y0,float x1,float y1) {
        if(!b.fighting() || daze>0 || limb<0 || blocked || attack<.12f)return false;
        if(distance(tipX,tipY,x0,y0,x1,y1)>tipRadius(L)+L.w*.07f)return false;
        blocked=true;recoil=RETURN;blocks++;flash=1;
        strokeX=x0;strokeY=y0;strokeEndX=x1;strokeEndY=y1;strokeFade=.3f;
        if(tummy!=null)tummy.squash(.23f);
        if(b.body!=null)b.body.squash(.35f);
        if(blocks>=BLOCKS) {daze=DAZE;windowHits=0;limb=-1;dazeCue=true;}
        return true;
    }
    int press(Boss b,Layout L) {
        if(daze<=0)return Boss.REBUFF;
        windowHits++;
        b.hitX=b.bodyX(L);b.hitY=b.bodyY(L);
        int verdict=b.damage(1f);
        if(windowHits>=WINDOW_HITS && !b.beaten) {daze=0;blocks=windowHits=0;timer=.7f;}
        return verdict;
    }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(c.state!=GameCore.PLAY || c.boss.kind!=Boss.ALLIGATOR || !c.boss.fighting()
                || c.paused || c.settingsOpen || c.onboarding.briefing) {pointer=-1;return false;}
        if(action==3) {boolean had=pointer>=0;pointer=-1;return had;}
        if(action==0 || action==5) {
            if(pointer>=0 || c.keyAt(x,y,L)>=0 || y<L.playTop)return false;
            pointer=id;fingerX=anchorX=x;fingerY=anchorY=y;return true;
        }
        if(pointer!=id)return false;
        if(action==2 || action==1 || action==6) {
            float dx=x-anchorX,dy=y-anchorY;
            if(dx*dx+dy*dy>=L.w*L.w*.0064f) {
                if(swipe(c.boss,L,anchorX,anchorY,x,y)) {
                    c.timeAttack.defended();c.score+=GameCore.BOSS_HIT;
                    c.shake=Math.max(c.shake,.22f);c.bossDeathHaptic=1;
                    Fx.explode(c,c.rnd,tipX,tipY,L.enemyR,12,0xFF9FEFD0);
                    if(c.sound!=null) {c.sound.boltPop();if(dazeCue)c.sound.uiBloop();}
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
