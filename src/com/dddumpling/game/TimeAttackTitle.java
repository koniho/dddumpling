package com.dddumpling.game;

/** Four letter-weights pass a playful impulse back and forth without touching game state. */
final class TimeAttackTitle extends Draw {
    static final float BEAT=.82f;
    private TimeAttackTitle() {}
    static void draw(Painter p,GameCore c,float x,float y,float s,float scale,int base,float fade) {
        float age=c.clock%BEAT,phase=age/BEAT;
        int beat=(int)(c.clock/BEAT),end=(beat&1)==0?0:3,side=end==0?-1:1;
        int boss=c.timeAttack.selected==TimeAttack.ALL?beat%Boss.COUNT:c.timeAttack.selected;
        float impact=(float)Math.exp(-age*4f),confirm=c.modes.confirmation;
        float pulse=Math.max(impact,confirm*.85f);
        int highlight=Glyph.mix(Collect.BODY[Collect.BOSS_FIRST+boss],INK,.35f);
        int color=fadeBy(Glyph.mix(base,highlight,pulse*.9f),fade);
        float font=type(s*1.74f)*scale,step=font*.70f;
        float swing=(float)Math.sin(phase*Math.PI),boost=1f+.35f*confirm;
        for(int i=0;i<4;i++) {
            float home=x+(i-1.5f)*step,dx=0,dy=0;
            if(i==end) {dx=side*s*.78f*swing*boost;dy=-s*.28f*swing*swing*boost;}
            else {
                float delay=Math.abs(i-end)*.025f,passed=Math.max(0,age-delay);
                if(age>=delay)dx=side*s*.10f*(float)Math.sin(passed*32f)*(float)Math.exp(-passed*18f);
            }
            float baseline=y-s*.9f+dy;
            p.line(home,y-s*2.75f,home+dx,baseline-font*.7f,fadeBy(INK_DIM,fade*.28f),s*.035f);
            p.text("BOSS".substring(i,i+1),home+dx,baseline,font,color,Painter.CENTER,true);
        }
        float bounce=s*.32f*(float)Math.sin(Math.min(1,age/.32f)*Math.PI)*(float)Math.exp(-age*3f)*boost;
        p.text("TIME ATTACK",x,y+s*1.1f-bounce,font,color,Painter.CENTER,true);
    }
}
