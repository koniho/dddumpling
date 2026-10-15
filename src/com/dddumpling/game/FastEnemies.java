package com.dddumpling.game;

/** One-press runners create an urgent alternative to finishing a longer word. */
final class FastEnemies {
    static final int FIRST_STAGE=16;
    static final float SPEED=2f;
    private FastEnemies() {}

    static void assign(GameCore c,GameCore.Enemy e) {
        e.fastSingle=false;
        if(c.stage<FIRST_STAGE || Boss.isBossStage(c.stage) || c.boss.active() || Cave.active(c)
                || c.survival.active || c.timeAttack.active || c.powerActive() || e.link!=null) return;
        // The first pair gets its introduction before the third spawn becomes a runner.
        boolean chosen=c.stage==FIRST_STAGE?c.spawnedThisStage==2:e.phase>=6.283f*.75f;
        if(!chosen)return;
        e.fastSingle=true;
        e.word=new int[]{e.word[0]};e.need=new int[]{1};
        e.gone=new boolean[1];e.goneT=new float[1];e.goneDx=new float[1];e.goneDy=new float[1];
    }

    static void draw(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int white=Glyph.withAlpha(0xFFF7FBFF,fade),edge=Glyph.withAlpha(0xFF364B60,fade);
        float bandY=y-r*.36f,wave=(float)Math.sin(c.clock*18+e.phase);
        // A fluttering racing headband leaves the character's eyes and key shape clear.
        p.fillPoly(new float[]{x+r*.36f,bandY,x+r*.95f,bandY-r*(.18f+wave*.08f),
                x+r*.79f,bandY+r*.02f,x+r*.43f,bandY+r*.10f},white);
        p.fillPoly(new float[]{x+r*.38f,bandY+r*.06f,x+r*.77f,bandY+r*(.30f-wave*.08f),
                x+r*.53f,bandY+r*.24f},Glyph.withAlpha(0xFFBCD7E6,fade));
        p.line(x-r*.48f,bandY,x+r*.43f,bandY+r*.05f,edge,r*.18f);
        p.line(x-r*.48f,bandY-r*.025f,x+r*.43f,bandY+r*.025f,white,r*.12f);
        p.fillCircle(x+r*.43f,bandY+r*.025f,r*.10f,white);
        for(int side=-1;side<=1;side+=2)for(int k=0;k<2;k++) {
            float t=(c.clock*2.5f+k*.5f+side*.12f+e.phase)%1;
            if(t<0)t+=1;
            float px=x+side*r*(.9f+k*.25f),py=y-r*(.5f+t*1.2f);
            p.line(px,py,px+side*r*.10f,py-r*.45f,
                    Glyph.withAlpha(0xFFE7F6FF,(int)(fade*(1-t)*.7f)),r*.065f);
        }
    }
}
