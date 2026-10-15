package com.dddumpling.game;

/** One-press runners create an urgent alternative to finishing a longer word. */
final class FastEnemies {
    static final int FIRST_STAGE=16;
    static final float SPEED=2f;
    private FastEnemies() {}

    private static boolean eligible(GameCore c) {
        return c.stage>=FIRST_STAGE && !Boss.isBossStage(c.stage) && !c.boss.active() && !Cave.active(c)
                && !c.survival.active && !c.timeAttack.active;
    }

    static void assign(GameCore c,GameCore.Enemy e) {
        e.fastSingle=false;
        if(!eligible(c) || e.link!=null)return;
        // The first pair gets its introduction before the third spawn becomes a runner.
        boolean chosen=c.powerActive()?e.phase>=6.283f*.60f
                :c.stage==FIRST_STAGE?c.spawnedThisStage==2:c.fastEnemyNext;
        if(!chosen)return;
        e.fastSingle=true;
        e.word=new int[]{e.word[0]};e.need=new int[]{1};
        e.gone=new boolean[1];e.goneT=new float[1];e.goneDx=new float[1];e.goneDy=new float[1];
    }

    /** Decide the next runner when its preceding word arrives, so only that gap shortens. */
    static float nextDelay(GameCore c,Layout L) {
        float normal=Power.spawnDelay(c,L);
        c.fastEnemyNext=false;
        if(!eligible(c) || c.powerActive() || c.spawnedThisStage>=c.stageQuota() || LinkedPairs.due(c)
                || c.enemies.isEmpty())return normal;
        GameCore.Enemy previous=c.enemies.get(c.enemies.size()-1);
        if(previous.link!=null || previous.fastSingle || previous.word.length<2)return normal;
        // Two-thirds of longer solo words yields roughly two runners in five solo arrivals.
        c.fastEnemyNext=c.stage==FIRST_STAGE?c.spawnedThisStage==2:previous.phase>=6.283f/3f;
        return c.fastEnemyNext?normal*.25f:normal;
    }

    static void trail(Painter p,GameCore c,GameCore.Enemy e,Layout L,float x,float y,float r,int color,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int shade=Glyph.mix(color,0xFF25364C,.45f),light=Glyph.mix(color,0xFFFFFFFF,.65f);
        float spacing=Math.min(r*.65f,e.speed*.10f);
        // Fading copies follow the hexagon's path, with lit upper edges and shaded undersides.
        for(int k=6;k>=1;k--) {
            float age=spacing*k/Math.max(1,e.speed);
            float tx=x+e.sway*((float)Math.sin((c.clock-age)*1.1f+e.phase)
                    -(float)Math.sin(c.clock*1.1f+e.phase));
            float ty=y-spacing*k,rr=r*(1-k*.025f);
            if(e.sideEntry)tx=x+EnemyEntry.xAt(e,e.y-spacing*k,L)-e.baseX;
            int alpha=(int)(fade*.38f*(1-k/7f));
            float[] hex=Glyph.hex(tx,ty,rr);
            float angle=c.clock*.9f+e.phase+k*.20f;
            float cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
            for(int i=0;i<hex.length;i+=2) {
                float dx=hex[i]-tx,dy=hex[i+1]-ty;
                hex[i]=tx+dx*cs-dy*sn;hex[i+1]=ty+dx*sn+dy*cs;
            }
            p.fillPoly(hex,Glyph.withAlpha(color,alpha/3));
            p.strokePoly(hex,Glyph.withAlpha(shade,alpha),rr*.16f);
            p.strokePoly(hex,Glyph.withAlpha(color,alpha),rr*.09f);
            p.polyline(new float[]{hex[6],hex[7],hex[8],hex[9],hex[10],hex[11],hex[0],hex[1]},
                    Glyph.withAlpha(light,alpha),rr*.035f);
        }
    }

    static void draw(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int color,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int cap=Glyph.withAlpha(0xFFD3A184,fade),shade=Glyph.withAlpha(0xFF987060,fade);
        int cream=Glyph.withAlpha(0xFFFFEBD0,fade),shine=Glyph.withAlpha(0xFFF6FDFF,fade);
        int lens=Glyph.withAlpha(Glyph.mix(color,0xFF9CDDEB,.75f),fade);
        float bounce=(float)Math.sin(c.clock*12+e.phase);
        float cy=y-r*.04f*bounce;
        // A puffy dome, floppy ear pads and big candy-like goggles keep the face readable.
        p.fillEllipse(x,cy-r*.73f,r*.67f,r*.48f,shade);
        p.fillEllipse(x-r*.015f,cy-r*.76f,r*.63f,r*.43f,cap);
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*(.53f+.025f*bounce),ey=cy-r*.29f+side*r*.035f*bounce;
            p.fillEllipse(ex,ey,r*.17f,r*.31f,shade);
            p.fillEllipse(ex,ey-r*.025f,r*.13f,r*.27f,cap);
        }
        p.line(x-r*.10f,cy-r*.69f,x+r*.10f,cy-r*.69f,cream,r*.10f);
        for(int side=-1;side<=1;side+=2) {
            float gx=x+side*r*.27f,gy=cy-r*.70f;
            p.fillCircle(gx,gy,r*.28f,shade);
            p.fillCircle(gx,gy-r*.015f,r*.245f,cream);
            p.fillCircle(gx,gy-r*.015f,r*.18f,lens);
            p.fillCircle(gx-r*.055f,gy-r*.075f,r*.055f,shine);
        }
    }
}
