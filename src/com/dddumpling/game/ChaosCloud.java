package com.dddumpling.game;

import java.util.Arrays;
import java.util.Random;

/** A finite fast volley, then alternating full strokes; skins never affect these rules. */
final class ChaosCloud {
    static final int[] VOLLEYS={16,16,18};
    static final float MORPH=.28f, LINK_WINDOW=.24f;
    static final int SLASHES=4;
    int phase,fired,total,slashes,lastDirection,pointer=-1,defendedExtra;
    boolean vulnerable;
    float next=.2f,anchorX,anchorY,strokeX,strokeY,slashFlash;
    final int[] mate=new int[Boss.MAX_BOLTS];
    final float[] wait=new float[Boss.MAX_BOLTS];
    final int[] source=new int[Boss.MAX_BOLTS];
    final float[] sourceX=new float[Boss.MAX_BOLTS],sourceY=new float[Boss.MAX_BOLTS];
    ChaosCloud() {reset();}
    void reset() {
        phase=fired=total=slashes=lastDirection=defendedExtra=0;vulnerable=false;next=.2f;slashFlash=0;
        release();Arrays.fill(mate,-1);Arrays.fill(wait,0);Arrays.fill(source,-1);
    }
    void release() {pointer=-1;}
    float flight() {return phase==0?1.65f:phase==1?2.65f:2.35f;}
    float gap() {return phase==0?.34f:phase==1?.56f:.64f;}
    void update(Boss b,float dt,Layout L,Random rnd) {
        slashFlash=Math.max(0,slashFlash-dt);
        if(vulnerable)return;
        if(fired>=VOLLEYS[phase]) {
            if(b.boltCount()==0) {vulnerable=true;slashes=lastDirection=0;release();}
            return;
        }
        next-=dt;
        int count=phase==2?2:1;
        if(next>0 || b.boltCount()>Boss.MAX_BOLTS-count)return;
        int first=-1;
        for(int j=0;j<count;j++) {
            int particle=total%30;
            float radius=Boss.bodyR(L)*1.55f;
            float sx=b.bodyX(L)+ChaosCloudArt.orbitX(particle,b.age)*radius;
            float sy=b.bodyY(L)+ChaosCloudArt.orbitY(particle,b.age)*radius;
            int slot=b.tailBolt(rnd,sx+(sx-b.bodyX(L))*.22f,sy+radius*.28f);
            if(slot<0)return;
            mate[slot]=-1;wait[slot]=0;source[slot]=particle;sourceX[slot]=sx;sourceY[slot]=sy;
            b.bt[slot]=-MORPH/flight();b.bfast[slot]=phase==0;
            b.bhp[slot]=b.bhpMax[slot]=phase==1?3:1;
            if(phase==2) {
                int half=Roster.count(b.rosterFull)/2;
                b.bglyph[slot]=Roster.at(b.rosterFull,j*half+rnd.nextInt(half));
                if(first>=0) {mate[first]=slot;mate[slot]=first;}
                first=slot;
            }
            fired++;total++;
        }
        next+=gap();
        if(b.body!=null)b.body.squash(.12f);
    }
    float morph(Boss b,int i) {return Math.max(0,Math.min(1,1+b.bt[i]*flight()/MORPH));}
    float particleOpacity(Boss b,int particle) {
        for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i] && source[i]==particle && b.bt[i]<0)
            return morph(b,i);
        return 1;
    }
    int age(Boss b,float dt) {
        int hits=0;
        for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i]) {
            if(wait[i]>0) {
                wait[i]-=dt;
                if(wait[i]<=0)b.bhp[i]=1;
            }
            b.bt[i]+=dt/flight();
            if(b.bt[i]<1)continue;
            b.blive[i]=false;wait[i]=0;
            int other=mate[i];if(other>=0) {b.blive[other]=false;wait[other]=0;mate[other]=-1;}
            mate[i]=-1;hits++;
        }
        return hits;
    }
    boolean targetable(Boss b,int i) {return b.bt[i]>=0 && wait[i]<=0;}
    int swat(Boss b,int i) {
        defendedExtra=0;int other=mate[i];
        b.bhp[i]--;b.boltDestroyed=b.bhp[i]<=0;
        if(!b.boltDestroyed)return Boss.PARRY;
        if(other<0) {b.blive[i]=false;return Boss.PARRY;}
        if(wait[other]>0) {
            defendedExtra=1;
            b.blive[i]=b.blive[other]=false;wait[i]=wait[other]=0;mate[i]=mate[other]=-1;
        } else {wait[i]=LINK_WINDOW;b.boltDestroyed=false;}
        return Boss.PARRY;
    }
    boolean slash(Boss b,Layout L,float x1,float y1,float x2,float y2) {
        if(!b.fighting() || !vulnerable)return false;
        float r=Boss.bodyR(L),cx=b.bodyX(L),cy=b.bodyY(L);
        int direction=x2>x1?1:-1;
        if(direction==lastDirection || Math.abs(x2-x1)<r*1.35f)return false;
        if(Math.min(x1,x2)>cx-r*.55f || Math.max(x1,x2)<cx+r*.55f)return false;
        float at=(cx-x1)/(x2-x1),y=y1+(y2-y1)*at;
        if(Math.abs(y-cy)>r*.85f)return false;
        lastDirection=direction;slashes++;slashFlash=.32f;strokeX=x2;strokeY=y;
        b.hitX=cx;b.hitY=y;b.damage(1);
        if(slashes>=SLASHES && !b.beaten) {
            phase++;fired=slashes=0;vulnerable=false;next=.35f;release();
        }
        return true;
    }
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        Boss b=c.boss;
        if(action==3) {release();return false;}
        if(c.state!=GameCore.PLAY || c.paused || c.settingsOpen || b.kind!=Boss.CHAOS_CLOUD
                || !b.fighting() || !vulnerable) {release();return false;}
        if(action==0 || action==5) {
            if(pointer>=0 || y>=L.deckTop)return false;
            pointer=id;anchorX=x;anchorY=y;return true;
        }
        if(pointer!=id)return false;
        float before=b.hp;
        if(slash(b,L,anchorX,anchorY,x,y)) {
            c.progress.bossDamage(b.kind,before,b.hp);c.timeAttack.bossDamaged(before,b.hp);
            c.onboarding.bossDamaged(c);c.companion.react(RunCompanion.BOSS_HIT,1);
            c.shake=Math.max(c.shake,.38f);c.flash=Math.max(c.flash,.18f);
            c.bossDeathHaptic=1;c.score+=40;
            Fx.explode(c,c.rnd,b.hitX,b.hitY,L.enemyR*2,20,Glyph.COLOR[slashes%Glyph.COUNT]);
            if(c.sound!=null)c.sound.ninjaSwish(slashes);
            anchorX=x;anchorY=y;
        }
        if(action==1 || action==6)release();
        return true;
    }
}
