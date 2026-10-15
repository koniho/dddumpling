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

    static void trail(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int color,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int shade=Glyph.mix(color,0xFF25364C,.48f),light=Glyph.mix(color,0xFFFFFFFF,.65f);
        // Tapered, shaded ribbons stream upward behind the descending key.
        for(int lane=-1;lane<=1;lane++) {
            float length=r*(lane==0?3.6f:2.8f);
            float rootY=y-r*(lane==0?.65f:.20f),rootX=x+lane*r*.83f;
            for(int j=0;j<7;j++) {
                float t=j/7f,u=(j+1)/7f;
                float ax=rootX+lane*r*t*.40f+r*.13f*t*(float)Math.sin(c.clock*13+e.phase+t*7+lane);
                float bx=rootX+lane*r*u*.40f+r*.13f*u*(float)Math.sin(c.clock*13+e.phase+u*7+lane);
                float ay=rootY-length*t,by=rootY-length*u;
                float aw=r*.22f*(1-t),bw=r*.22f*(1-u);
                int alpha=(int)(fade*.65f*(1-t)*(1-t));
                p.fillPoly(new float[]{ax-aw,ay,ax+aw,ay,bx+bw,by,bx-bw,by},Glyph.withAlpha(shade,alpha));
                p.fillPoly(new float[]{ax-aw*.7f,ay,ax+aw*.65f,ay,bx+bw*.65f,by,bx-bw*.7f,by},Glyph.withAlpha(color,alpha));
                p.fillPoly(new float[]{ax-aw*.55f,ay,ax-aw*.15f,ay,bx-bw*.15f,by,bx-bw*.55f,by},Glyph.withAlpha(light,alpha));
            }
        }
    }

    static void draw(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int color,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int cap=Glyph.withAlpha(Glyph.mix(color,0xFF324663,.28f),fade);
        int shadow=Glyph.withAlpha(Glyph.mix(color,0xFF25364C,.58f),fade);
        int light=Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.50f),fade);
        float cy=y-r*(.59f+.035f*(float)Math.sin(c.clock*16+e.phase));
        // A tilted backward cap: the brim sticks out to the side, clear of the face.
        float[] brim={x+r*.20f,cy-r*.13f,x+r*.61f,cy-r*.28f,
            x+r*.92f,cy-r*.22f,x+r*.99f,cy-r*.12f,x+r*.82f,cy-r*.05f,x+r*.30f,cy+r*.02f};
        p.fillPoly(brim,shadow);
        p.line(x+r*.51f,cy-r*.20f,x+r*.86f,cy-r*.18f,cap,r*.10f);
        float[] crown={x-r*.55f,cy+r*.05f,x-r*.55f,cy-r*.16f,
            x-r*.44f,cy-r*.40f,x-r*.24f,cy-r*.54f,x+r*.02f,cy-r*.56f,
            x+r*.27f,cy-r*.43f,x+r*.43f,cy-r*.22f,x+r*.47f,cy-r*.02f};
        p.fillPoly(crown,cap);p.strokePoly(crown,shadow,r*.055f);
        p.polyline(new float[]{x-r*.24f,cy-r*.48f,x-r*.10f,cy-r*.30f,x-r*.07f,cy-r*.08f},light,r*.045f);
        p.line(x-r*.47f,cy,x+r*.38f,cy-r*.06f,shadow,r*.075f);
        p.fillEllipse(x+r*.02f,cy-r*.57f,r*.09f,r*.045f,light);
    }
}
