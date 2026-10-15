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
        // Streaming air beside the key signals speed without covering the character.
        for(int side=-1;side<=1;side+=2)for(int k=0;k<3;k++) {
            float t=(c.clock*2.5f+k/3f+side*.12f+e.phase)%1;
            if(t<0)t+=1;
            float px=x+side*r*(1.12f+k*.22f),py=y+r*(.25f-t*1.3f);
            float endX=px+side*r*.08f,endY=py-r*(.8f-k*.12f);
            int alpha=(int)(fade*(1-t)*.85f);
            p.line(px,py,endX,endY,Glyph.withAlpha(0xFF496C85,alpha/2),r*.17f);
            p.line(px,py,endX,endY,Glyph.withAlpha(0xFFF1FAFF,alpha),r*.09f);
        }
    }
}
