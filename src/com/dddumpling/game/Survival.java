package com.dddumpling.game;

/** Endless combat and its local records; Adventure's stage and reward flow stays separate. */
final class Survival extends Draw {
    static final float RAMP_SECONDS=300f, RESCUE_SECONDS=30f;
    static final float ENTRANCE_SECONDS=2f;
    final long[] bestTime=new long[3];
    final int[] bestScore=new int[3];
    boolean active,finished,newBest;
    double seconds;
    float rescueLeft,skyPhase,rescueReadyFlash;
    int profile,adventureLand;
    int usedPowers;
    private final java.util.Random effects=new java.util.Random(149);
    // At minimum width and spacing, at most 201 bands cover the screen.
    private final float[] bandX=new float[202];
    private final int[] bandOrder=new int[202];

    static int profile(boolean kids,boolean full) { return kids?2:full?1:0; }
    static String profileName(int profile) { return profile==2?"KIDS":profile==1?"6 KEYS":"4 KEYS"; }
    int titleProfile(GameCore c) { return profile(c.preferences.kids,c.fullRoster); }
    long millis() { return (long)(seconds*1000); }
    static String time(long millis) {
        long total=Math.max(0,millis)/1000;
        return total/60+":"+(total%60<10?"0":"")+total%60;
    }
    String title(GameCore c) { return "BEST "+time(bestTime[titleProfile(c)])+" / "+bestScore[titleProfile(c)]; }
    void begin(GameCore c) {
        active=true;finished=newBest=false;seconds=0;rescueLeft=skyPhase=rescueReadyFlash=0;
        usedPowers=0;
        effects.setSeed(149);
        profile=profile(c.kidsRun,c.runFullRoster);adventureLand=c.landChoice;
        c.best=bestScore[profile];
    }
    void update(GameCore c,float elapsed) {
        if(!active || finished || c.state!=GameCore.PLAY)return;
        if(elapsed<=0 || Float.isNaN(elapsed) || Float.isInfinite(elapsed))return;
        seconds+=elapsed;
        skyPhase+=elapsed*(1f+3f*ramp());
        rescueReadyFlash=Math.max(0,rescueReadyFlash-elapsed/1.2f);
        // Advance difficulty without stage endings, bosses, banners or clearing live enemies.
        c.stage=1+Math.min(18,(int)(seconds/20));
        if(c.pushUsed) {
            rescueReadyFlash=0;
            rescueLeft+=elapsed;
            if(rescueLeft>=RESCUE_SECONDS) {
                c.pushUsed=false;rescueLeft=0;rescueReadyFlash=1;
                c.companion.react(RunCompanion.WORD,1f);
            }
        } else rescueLeft=0;
    }
    float ramp() { return Math.min(1f,(float)(seconds/RAMP_SECONDS)); }
    float travel(GameCore c) { return c.kidsRun?12f:12f-4f*ramp(); }
    float spawn(GameCore c) { return c.kidsRun?1.65f:1.65f-.6f*ramp(); }
    int crowd(GameCore c) { return c.kidsRun?3:seconds<60?3:seconds<180?4:5; }
    float rescueCharge(GameCore c) { return c.pushUsed?Math.min(1,rescueLeft/RESCUE_SECONDS):1f; }
    void drawRescue(Painter p,GameCore c,Layout L) {
        if(!active || finished || c.state!=GameCore.PLAY || c.companion.who<0
                || c.onboarding.companionAway(c))return;
        float x=RunCompanion.x(L),y=RunCompanion.y(L)+c.companion.rescueLift(L)+c.companion.damagePush(L);
        float rx=RunCompanion.halfWidth(L)*1.17f,ry=RunCompanion.halfHeight(L)*1.17f;
        float charge=rescueCharge(c),stroke=L.unit*.10f;
        int color=Glyph.COLOR[4];
        p.arc(x,y,rx,ry,0,360,Glyph.withAlpha(INK_DIM,65),stroke);
        if(charge>0)p.arc(x,y,rx,ry,-90,charge*360,Glyph.withAlpha(color,c.pushUsed?215:160),stroke);
        float tip=y-ry-L.unit*.12f;
        p.polyline(new float[]{x-L.unit*.16f,tip+L.unit*.14f,x,tip,x+L.unit*.16f,tip+L.unit*.14f},
                Glyph.withAlpha(color,c.pushUsed?70:220),stroke);
        if(rescueReadyFlash>0) {
            float spread=1f+.28f*(1-rescueReadyFlash);
            p.arc(x,y,rx*spread,ry*spread,0,360,Glyph.withAlpha(color,(int)(210*rescueReadyFlash)),stroke*1.5f);
        }
    }
    static float rackX(Layout L) { return L.padL+L.unit*1.5f; }
    static float rackY(Layout L,int slot) { return (L.playTop+L.deckTop)*.5f+(slot-1)*L.unit*3.4f; }
    boolean powerUsed(int effect) { return (usedPowers&(1<<effect))!=0; }
    boolean tapPower(GameCore c,Layout L,float x,float y) {
        if(!active || finished || c.state!=GameCore.PLAY || c.paused || c.settingsOpen
                || c.onboarding.briefing || c.onboarding.practice!=null || c.returnFade>0)return false;
        float grab=L.unit*1.4f;
        for(int slot=0;slot<Power.OFFERED.length;slot++) {
            float dx=x-rackX(L),dy=y-rackY(L,slot);
            if(dx*dx+dy*dy>grab*grab)continue;
            // Spent and temporarily unavailable buttons still own the touch.
            c.endStroke();
            int effect=Power.offeredAt(slot);
            if(powerUsed(effect) || c.powerActive() || c.debuffLeft>0)return true;
            c.startFrenzy(effect,L);
            if(c.powerActive() && c.mode==effect) {
                usedPowers|=1<<effect;
                c.powerBurstX=rackX(L);c.powerBurstY=rackY(L,slot);
            }
            return true;
        }
        return false;
    }
    void drawPowers(Painter p,GameCore c,Layout L) {
        if(!active || finished || c.state!=GameCore.PLAY)return;
        float x=rackX(L),r=L.unit;
        for(int slot=0;slot<Power.OFFERED.length;slot++) {
            int effect=Power.offeredAt(slot);float y=rackY(L,slot);
            boolean spent=powerUsed(effect);
            float alpha=spent?.22f:c.powerActive() || c.debuffLeft>0?.6f:1f;
            p.fillPoly(Glyph.hex(x,y,r*1.2f),Glyph.withAlpha(BG,spent?65:170));
            Renderer.summaryPowerIcon(new OpacityPainter(p,alpha),effect,x,y,r,spent?0:c.clock);
            if(c.powerActive() && c.mode==effect)
                p.arc(x,y,r*1.27f,r*1.27f,-90,360*c.modeLeft/Power.DURATION,GOLD,r*.07f);
        }
    }
    float entrance() {
        float t=Math.min(1f,(float)(seconds/ENTRANCE_SECONDS));
        return t*t*(3f-2f*t);
    }
    int background() {return Glyph.mix(BG,Glyph.hsv(3.7f,.50f+.25f*ramp(),.18f),entrance());}
    private static float scatter(int index,int salt) {
        int hash=(index+1)*0x45d9f3b+salt*0x119de1f3;
        hash=(hash^(hash>>>16))*0x45d9f3b;
        return (hash&0xffff)/65535f;
    }
    void wordBurst(GameCore c,float x,float y,float spread,int count,int color) {
        Fx.explode(c,c.rnd,x,y,spread,count,color);
        if(!active)return;
        if(c.particles.size()>4096)c.particles.subList(0,c.particles.size()-4096).clear();
        // Extra spectacle has its own RNG so difficulty does not change with particle density.
        int extra=Math.min(Math.round(count*9*ramp()),Math.max(0,4096-c.particles.size()));
        Fx.explode(c,effects,x,y,spread*(1f+.8f*ramp()),extra,Glyph.withAlpha(color,175));
    }
    void scenery(Painter p,GameCore c,Layout L) {
        float intensity=ramp(),width=L.w/(3f+21f*intensity),radius=width*.5f;
        float length=L.h,pitch=length-width;
        float saturation=.50f+.25f*intensity,value=.30f+.08f*intensity;
        float x=0;
        int count=0;
        for(int column=0;;column++) {
            bandX[column]=x;
            // Stable random depth keeps existing overlaps unchanged as new bands appear.
            int layer=count++;
            while(layer>0 && scatter(bandOrder[layer-1],6)>scatter(column,6)) {
                bandOrder[layer]=bandOrder[layer-1];layer--;
            }
            bandOrder[layer]=column;
            if(x>=L.w)break;
            // Leave room for each incoming stripe to shift without opening a gap.
            x=Math.min(L.w,x+width*(.12f+.36f*scatter(column,1)));
        }
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int layer=0;layer<count;layer++) {
            int column=bandOrder[layer];x=bandX[column];
            // The first cap starts above the title sky; later stripes follow continuously.
            float travel=skyPhase*L.h*.08f*(.8f+.4f*scatter(column,2))
                    +L.h*(1.15f*entrance()-.12f*scatter(column,3))-pitch*.5f-radius;
            int cycle=(int)Math.floor(travel/pitch);
            float offset=travel-cycle*pitch;
            // Central rectangles touch vertically; adjacent columns overlap even between caps.
            for(int row=-1;row<=Math.ceil(L.h/pitch)+1;row++) {
                if(row-cycle>0)continue;
                // Each vertical repeat gets a new position, held for its whole scroll.
                float stripeX=x+width*.24f*(2f*scatter(column,7+(row-cycle)*31)-1f);
                if(stripeX<0)stripeX=-stripeX;
                if(stripeX>L.w)stripeX=2f*L.w-stripeX;
                float cy=offset+row*pitch,top=cy-pitch*.5f,bottom=cy+pitch*.5f;
                if(bottom+radius<0 || top-radius>L.deckTop)continue;
                float hue=(scatter(column,4)+(row-cycle)*.137f)%1f;
                if(hue<0)hue+=1f;
                int color=Glyph.mix(Glyph.hsv(hue*6f,saturation,value),BG_DEATH,c.drained());
                p.fillRect(stripeX-radius,top,stripeX+radius,bottom,color);
                p.fillCircle(stripeX,top,radius,color);
                p.fillCircle(stripeX,bottom,radius,color);
                float shine=1f-c.drained();
                stripeRim(p,stripeX,top,bottom,radius*.78f,radius*.18f,
                        Glyph.mix(color,0xFF000000,.24f*shine),false);
                stripeRim(p,stripeX,top,bottom,radius*.72f,radius*.28f,
                        Glyph.mix(color,0xFFFFFFFF,.12f*shine),true);
                stripeRim(p,stripeX,top,bottom,radius*.80f,radius*.08f,
                        Glyph.mix(color,0xFFFFFFFF,.27f*shine),true);
            }
        }
        p.restore();
    }
    private static void stripeRim(Painter p,float x,float top,float bottom,float radius,float width,int color,boolean right) {
        float edge=x+(right?radius:-radius);
        p.line(edge,top,edge,bottom,color,width);
        p.arc(x,top,radius,radius,right?270:180,90,color,width);
        p.arc(x,bottom,radius,radius,right?0:90,90,color,width);
    }
    void finish(GameCore c) {
        if(!active || finished)return;
        finished=true;usedPowers=0;rescueReadyFlash=0;
        if(c.scoresSuppressed)return;
        long duration=millis();
        newBest=duration>bestTime[profile];
        bestTime[profile]=Math.max(bestTime[profile],duration);
        bestScore[profile]=Math.max(bestScore[profile],c.score);
        if(c.store!=null)c.store.saveSurvival(encode());
    }
    void leave(GameCore c) {
        if(!active)return;
        finish(c);c.landChoice=adventureLand;
        c.best=c.landChoice==LandPicker.TOWN?0:c.landBests[c.landChoice];
        active=false;rescueLeft=rescueReadyFlash=0;
    }
    void clearRecords() {java.util.Arrays.fill(bestTime,0);java.util.Arrays.fill(bestScore,0);newBest=false;}
    String encode() {
        StringBuilder s=new StringBuilder("1");
        for(int i=0;i<3;i++)s.append(';').append(bestTime[i]).append(',').append(bestScore[i]);
        return s.toString();
    }
    void load(String saved) {
        clearRecords();if(saved==null || saved.length()>256)return;
        try {
            String[] rows=saved.split(";",-1);if(rows.length!=4 || !rows[0].equals("1"))return;
            long[] times=new long[3];int[] scores=new int[3];
            for(int i=0;i<3;i++) {
                String[] fields=rows[i+1].split(",",-1);if(fields.length!=2)return;
                times[i]=Long.parseLong(fields[0]);scores[i]=Integer.parseInt(fields[1]);
                if(times[i]<0 || scores[i]<0)return;
            }
            System.arraycopy(times,0,bestTime,0,3);System.arraycopy(scores,0,bestScore,0,3);
        } catch(NumberFormatException ignored) {clearRecords();}
    }
    static float resultY(Layout L,boolean retry) {return L.h*(retry?.73f:.80f);}
    void resultTap(GameCore c,Layout L,float x,float y) {
        if(!active || !c.overReady() || c.returnFade>0 || Math.abs(x-L.w*.5f)>L.w*.35f)return;
        if(Math.abs(y-resultY(L,true))<L.unit*1.1f) {
            c.toTitle();c.beginStart();
        } else if(Math.abs(y-resultY(L,false))<L.unit*1.1f)c.dismissGameOver();
    }
    void result(Painter p,GameCore c,Layout L,float fade) {
        float s=L.unit,cx=L.w*.5f;
        p.text("SURVIVAL",cx,L.h*.24f,type(s*1.6f),fadeBy(YELLOW,fade),Painter.CENTER,true);
        p.text(time(millis()),cx,L.h*.345f,type(s*2f),fadeBy(INK,fade),Painter.CENTER,true);
        p.text("SCORE "+c.score+" / "+profileName(profile),cx,L.h*.40f,type(s*.68f),fadeBy(INK_DIM,fade),Painter.CENTER,true);
        Screens.accuracy(p,c,L,L.h*.51f,fade);
        p.text(newBest?"NEW LONGEST RUN!":"LONGEST "+time(bestTime[profile]),cx,L.h*.63f,
                type(s*.75f),fadeBy(newBest?GOLD:INK,fade),Painter.CENTER,true);
        p.text("BEST SCORE "+bestScore[profile],cx,L.h*.67f,type(s*.58f),fadeBy(INK_DIM,fade),Painter.CENTER,false);
        p.text("RETRY",cx,resultY(L,true),type(s*.9f),fadeBy(GOLD,fade),Painter.CENTER,true);
        p.text("TITLE",cx,resultY(L,false),type(s*.7f),fadeBy(INK,fade),Painter.CENTER,true);
    }
}
