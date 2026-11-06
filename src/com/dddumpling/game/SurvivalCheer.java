package com.dddumpling.game;

/** Cosmetic milestones follow the active clock and never consume gameplay randomness. */
final class SurvivalCheer extends Draw {
    static final float DURATION=1.2f;
    private static final String[] CHEERS={"NICE!","GREAT!","WOW!","SUPER!","AMAZING!","UNREAL!"};
    private static final int[] COLORS={GOLD,0xFF67E9DF,0xFFB69AFF,0xFF76EBA1,0xFFFFAD63};
    long milestone;
    float age=DURATION;
    String cheer="",cleared="";

    void reset() {milestone=0;age=DURATION;cheer=cleared="";}
    void update(double seconds) {
        long reached=(long)(seconds/30);
        if(reached>milestone) {
            milestone=reached;
            cheer=CHEERS[(int)Math.min(CHEERS.length-1,reached-1)];
            cleared=Survival.time(reached*30000)+" SURVIVED";
        }
        age=milestone==0?DURATION:Math.min(DURATION,(float)(seconds-milestone*30));
    }
    boolean visible(GameCore c) {
        return c.survival.active && !c.survival.finished && c.state==GameCore.PLAY
                && milestone>0 && age<DURATION;
    }
    // Every interval approaches a bounded ceiling without growing the effect across the field.
    float intensity() {return milestone<=0?0:1f-4f/(milestone+3f);}
    int color() {return COLORS[(int)((milestone-1)%COLORS.length)];}
    float opacity() {return Math.min(1,age/.08f)*Math.min(1,(DURATION-age)/.28f);}

    void pulse(Painter p,GameCore c,Layout L) {
        if(!visible(c))return;
        float beat=(float)Math.sin(Math.PI*Math.min(1,age/.8f));
        float strength=c.cloudUi.reduceMotion?.07f:.13f+.12f*intensity();
        p.fillRect(0,0,L.w,L.deckTop,Glyph.withAlpha(color(),(int)(255*strength*beat)));
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!visible(c))return;
        boolean still=c.cloudUi.reduceMotion;
        float level=intensity(),alpha=opacity(),s=L.unit;
        float cx=(L.playLeft+L.playRight)*.5f,cy=(L.playTop+L.dangerY)*.5f;
        float pop=still?1:1f-(float)Math.exp(-age*15)*(float)Math.cos(age*23);
        float scale=Math.min(1,.72f+.28f*pop);
        // The shared .73 advance allowance fits both native fonts and the wider harness font.
        float width=L.playRight-L.playLeft-s*.5f;
        float size=width/(cheer.length()*.73f)*scale;
        float detailSize=width/(cleared.length()*.73f)*scale;
        float gap=s*.42f,height=(size+detailSize)*.72f+gap;
        float titleY=cy-height*.5f+size*.72f;
        float detailY=cy+height*.5f;
        int tint=color(),a=(int)(255*alpha);
        float travel=still?.5f:Math.min(1,age/.65f);
        int count=6+(int)(12*level);
        // Bursts frame the centered lettering; enemies and controls still draw in front.
        for(int i=0;i<count;i++) {
            float angle=(i/(float)count)*(float)(Math.PI*2)+.25f;
            float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float radius=L.w*(.28f+.13f*travel);
            float x=cx+dx*radius,y=cy+dy*(height*.5f+s*(.6f+.6f*travel));
            if(Math.abs(dx)<.58f)continue;
            float r=s*(.13f+.12f*level)*(1-.35f*travel);
            int ink=Glyph.withAlpha(i%2==0?tint:INK,(int)(a*.85f));
            p.fillPoly(star(x,y,r*2,r*.65f,4,still?0:age*2+i),ink);
            if(level>.2f)p.line(x+dx*s*.24f,y+dy*s*.15f,
                    x+dx*s*(.45f+.35f*level),y+dy*s*.28f,ink,s*.08f);
        }
        float bounce=still?0:(float)Math.sin(age*18)* (float)Math.exp(-age*6)*s*.17f;
        outlined(p,cheer,cx,titleY+bounce,size,tint,a,s*.16f);
        outlined(p,cleared,cx,detailY+bounce,detailSize,INK,a,s*.10f);
    }
    private static void outlined(Painter p,String text,float x,float y,float size,int color,int alpha,float edge) {
        int outline=Glyph.withAlpha(BG,alpha);
        p.text(text,x-edge,y,size,outline,Painter.CENTER,true);
        p.text(text,x+edge,y,size,outline,Painter.CENTER,true);
        p.text(text,x,y-edge,size,outline,Painter.CENTER,true);
        p.text(text,x,y+edge,size,outline,Painter.CENTER,true);
        p.text(text,x,y,size,Glyph.withAlpha(color,alpha),Painter.CENTER,true);
    }
}
