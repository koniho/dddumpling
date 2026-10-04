package com.dddumpling.game;

/** Endless combat and its local records; Adventure's stage and reward flow stays separate. */
final class Survival extends Draw {
    static final float RAMP_SECONDS=300f, RESCUE_SECONDS=30f;
    final long[] bestTime=new long[3];
    final int[] bestScore=new int[3];
    boolean active,finished,newBest;
    double seconds;
    float rescueLeft,skyPhase;
    int profile,adventureLand;
    private final java.util.Random effects=new java.util.Random(149);

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
        active=true;finished=newBest=false;seconds=0;rescueLeft=skyPhase=0;
        effects.setSeed(149);
        profile=profile(c.kidsRun,c.runFullRoster);adventureLand=c.landChoice;
        c.best=bestScore[profile];
    }
    void update(GameCore c,float elapsed) {
        if(!active || finished || c.state!=GameCore.PLAY)return;
        if(elapsed<=0 || Float.isNaN(elapsed) || Float.isInfinite(elapsed))return;
        seconds+=elapsed;
        skyPhase+=elapsed*(1f+3f*ramp());
        // Advance difficulty without stage endings, bosses, banners or clearing live enemies.
        c.stage=1+Math.min(18,(int)(seconds/20));
        if(c.pushUsed) {
            rescueLeft+=elapsed;
            if(rescueLeft>=RESCUE_SECONDS) {c.pushUsed=false;rescueLeft=0;}
        } else rescueLeft=0;
    }
    float ramp() { return Math.min(1f,(float)(seconds/RAMP_SECONDS)); }
    float travel(GameCore c) { return c.kidsRun?12f:12f-4f*ramp(); }
    float spawn(GameCore c) { return c.kidsRun?1.65f:1.65f-.6f*ramp(); }
    int crowd(GameCore c) { return c.kidsRun?3:seconds<60?3:seconds<180?4:5; }
    int background() {
        float intensity=ramp();
        return Glyph.mix(Glyph.mix(0xFF101E32,0xFF32153F,intensity),
                Glyph.mix(0xFF17182F,Glyph.cycle(skyPhase*.008f),.18f),intensity*intensity);
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
        float intensity=ramp(),phase=skyPhase,fade=1f-c.drained();
        int cool=Glyph.mix(0xFF53DECC,0xFFFF74BE,intensity),warm=Glyph.mix(0xFF719CFA,0xFFFFC577,intensity);
        p.save();p.clipRect(0,L.playTop,L.w,L.deckTop);
        // The late run becomes a slowly turning rainbow tunnel, behind every enemy and cue.
        float vivid=intensity*intensity;
        if(vivid>.001f) {
            float cx=L.w*.5f,cy=(L.playTop+L.deckTop)*.5f;
            for(int ring=0;ring<12;ring++) {
                float t=(ring/12f+phase*.009f)%1f;
                int color=Glyph.withAlpha(Glyph.cycle(phase*.012f+ring*.0833f),(int)(65*vivid*fade*Math.sin(t*Math.PI)));
                p.arc(cx,cy,L.w*(.08f+t*.8f),L.h*(.04f+t*.55f),0,360,color,L.unit*(.25f+intensity*.6f));
            }
            for(int arm=0;arm<8;arm++) {
                float[] points=new float[64];
                for(int k=0;k<32;k++) {
                    float t=k/31f,a=arm*.785398f+t*3.4f+phase*.055f;
                    float ripple=1f+.09f*(float)Math.sin(t*18-phase*.09f);
                    points[k*2]=cx+(float)Math.cos(a)*t*L.w*.8f*ripple;
                    points[k*2+1]=cy+(float)Math.sin(a)*t*L.h*.53f*ripple;
                }
                p.polyline(points,Glyph.withAlpha(Glyph.cycle(arm/8f+phase*.01f),(int)(38*vivid*fade)),L.unit*.45f);
            }
        }
        // Aurora stays near the edges, leaving the letter lanes dark at every intensity.
        for(int side=0;side<2;side++)for(int band=0;band<6;band++) {
            float x=side==0?-L.w*.08f:L.w*1.08f;
            float y=L.h*(.35f+.09f*(float)Math.sin(phase*.07f+side*2+band*.35f));
            p.fillEllipse(x,y,L.w*(.23f+band*.028f),L.h*(.22f+band*.035f),
                    Glyph.withAlpha(side==0?cool:warm,(int)((3+intensity*5)*fade)));
        }
        int count=12+(int)(24*intensity);
        for(int i=0;i<count;i++) {
            float t=(phase*.012f+i*.618034f)%1f;
            float edge=.035f+.15f*(.5f+.5f*(float)Math.sin(i*2.4f+phase*.04f));
            float x=L.w*(i%2==0?edge:1f-edge),y=L.deckTop-t*(L.deckTop-L.playTop);
            int color=Glyph.withAlpha(i%2==0?cool:warm,(int)((25+intensity*65)*Math.sin(t*Math.PI)*fade));
            p.line(x,y,x,y+L.unit*(.3f+intensity*.9f),color,L.unit*.06f);
            p.fillCircle(x,y,L.unit*(.045f+intensity*.035f),color);
        }
        p.restore();
    }
    void finish(GameCore c) {
        if(!active || finished)return;
        finished=true;
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
        active=false;rescueLeft=0;
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
