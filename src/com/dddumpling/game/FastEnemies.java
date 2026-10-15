package com.dddumpling.game;

/** One-press runners create an urgent alternative to finishing a longer word. */
final class FastEnemies {
    static final int FIRST_STAGE=16;
    static final float SPEED=2f;
    private FastEnemies() {}

    private static boolean eligible(GameCore c) {
        return c.stage>=FIRST_STAGE && !Boss.isBossStage(c.stage) && !c.boss.active() && !Cave.active(c)
                && !c.survival.active && !c.timeAttack.active && !c.powerActive();
    }

    static void assign(GameCore c,GameCore.Enemy e) {
        e.fastSingle=false;
        if(!eligible(c) || e.link!=null)return;
        // The first pair gets its introduction before the third spawn becomes a runner.
        boolean chosen=c.stage==FIRST_STAGE?c.spawnedThisStage==2:c.fastEnemyNext;
        if(!chosen)return;
        e.fastSingle=true;
        e.word=new int[]{e.word[0]};e.need=new int[]{1};
        e.gone=new boolean[1];e.goneT=new float[1];e.goneDx=new float[1];e.goneDy=new float[1];
    }

    /** Decide the next runner when its preceding word arrives, so only that gap shortens. */
    static float nextDelay(GameCore c,Layout L) {
        float normal=Power.spawnDelay(c,L);
        c.fastEnemyNext=false;
        if(!eligible(c) || c.spawnedThisStage>=c.stageQuota() || LinkedPairs.due(c)
                || c.enemies.isEmpty())return normal;
        GameCore.Enemy previous=c.enemies.get(c.enemies.size()-1);
        if(previous.link!=null || previous.fastSingle || previous.word.length<2)return normal;
        // One-third of longer solo words yields roughly one runner in four solo arrivals.
        c.fastEnemyNext=c.stage==FIRST_STAGE?c.spawnedThisStage==2:previous.phase>=6.283f*2f/3f;
        return c.fastEnemyNext?normal*.25f:normal;
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
        int leather=Glyph.withAlpha(0xFFA97450,fade),shade=Glyph.withAlpha(0xFF624734,fade);
        int trim=Glyph.withAlpha(0xFFEACDA4,fade),shine=Glyph.withAlpha(0xFFF1FCFF,fade);
        float cy=y-r*.035f*(float)Math.sin(c.clock*16+e.phase);
        // Soft leather cap with rounded ear flaps; the goggles sit above the character's eyes.
        float[] cap={x-r*.66f,cy-r*.26f,x-r*.61f,cy-r*.78f,
            x-r*.45f,cy-r*1.02f,x-r*.19f,cy-r*1.14f,x+r*.17f,cy-r*1.14f,
            x+r*.44f,cy-r*1.02f,x+r*.60f,cy-r*.78f,x+r*.66f,cy-r*.26f,
            x+r*.62f,cy+r*.10f,x+r*.48f,cy+r*.15f,x+r*.40f,cy-r*.35f,
            x+r*.23f,cy-r*.45f,x-r*.23f,cy-r*.45f,x-r*.40f,cy-r*.35f,
            x-r*.48f,cy+r*.15f,x-r*.62f,cy+r*.10f};
        p.fillPoly(cap,leather);p.strokePoly(cap,shade,r*.06f);
        p.polyline(new float[]{x-r*.17f,cy-r*1.06f,x-r*.07f,cy-r*.91f,x-r*.05f,cy-r*.78f},trim,r*.045f);
        p.line(x-r*.55f,cy-r*.29f,x-r*.55f,cy+r*.02f,trim,r*.07f);
        p.line(x+r*.55f,cy-r*.29f,x+r*.55f,cy+r*.02f,trim,r*.07f);
        p.line(x-r*.54f,cy-r*.67f,x+r*.54f,cy-r*.67f,shade,r*.15f);
        for(int side=-1;side<=1;side+=2) {
            float gx=x+side*r*.25f,gy=cy-r*.67f;
            p.fillEllipse(gx,gy,r*.255f,r*.215f,shade);
            p.fillEllipse(gx,gy-r*.015f,r*.22f,r*.18f,trim);
            p.fillEllipse(gx,gy-r*.01f,r*.17f,r*.135f,Glyph.withAlpha(Glyph.mix(color,0xFF73BDDA,.7f),fade));
            p.fillEllipse(gx,gy+r*.04f,r*.15f,r*.065f,Glyph.withAlpha(0xFF397994,fade/2));
            p.line(gx-r*.09f,gy-r*.055f,gx+r*.035f,gy-r*.08f,shine,r*.045f);
        }
        p.line(x-r*.035f,cy-r*.68f,x+r*.035f,cy-r*.68f,trim,r*.075f);
    }
}
