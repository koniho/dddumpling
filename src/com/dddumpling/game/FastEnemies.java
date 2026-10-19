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
        // Power-up runners use 26% of solo candidates: 35% fewer than the former 40%.
        boolean rainbow=Lands.forStage(c.stage)==Lands.RAINBOW;
        boolean chosen=c.powerActive()?e.phase>=6.283f*(rainbow?.61f:.74f)
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
        c.fastEnemyNext=c.stage==FIRST_STAGE?c.spawnedThisStage==2:previous.phase>=6.283f*(Lands.forStage(c.stage)==Lands.RAINBOW?.08f:1f/3f);
        return c.fastEnemyNext?normal*.25f:normal;
    }

    static float[] outline(float x,float y,float r) {
        return Draw.star(x,y,r*1.22f,r*.72f,5,-(float)Math.PI*.5f);
    }

    static boolean starTile(GameCore.Enemy e,int i) {
        return e.fastSingle || (e.swampKind==SwampEnemies.DIVE && i==e.word.length-1);
    }

    static void glow(Painter p,float x,float y,float r,int color,int fade,float pulse) {
        int light=Glyph.mix(color,0xFFFFF6C4,.62f);
        p.fillCircle(x,y,r*1.65f,Glyph.withAlpha(color,(int)(fade*.06f*pulse)));
        p.fillCircle(x,y,r*1.40f,Glyph.withAlpha(light,(int)(fade*.09f*pulse)));
        p.strokePoly(outline(x,y,r),Glyph.withAlpha(color,fade/6),r*.32f);
        p.strokePoly(outline(x,y,r),Glyph.withAlpha(light,fade/4),r*.16f);
    }

    private static float pastX(GameCore c,GameCore.Enemy e,Layout L,float x,float distance) {
        if(e.sideEntry)return x+EnemyEntry.xAt(e,e.y-distance,L)-e.baseX;
        float age=distance/Math.max(1,e.speed);
        return x+e.sway*((float)Math.sin((c.clock-age)*1.1f+e.phase)
                -(float)Math.sin(c.clock*1.1f+e.phase));
    }

    static void trail(Painter p,GameCore c,GameCore.Enemy e,Layout L,float x,float y,float r,int color,int fade) {
        if(!e.fastSingle || !e.typeable())return;
        int light=Glyph.mix(color,0xFFFFF6C4,.62f);
        float pulse=.85f+.15f*(float)Math.sin(c.clock*5+e.phase);
        // Layered translucent shapes glow without a full-screen blur or extra particle state.
        glow(p,x,y,r,color,fade,pulse);
        float length=Math.max(r*3.2f,Math.min(r*6,e.speed*.48f));
        for(int lane=-1;lane<=1;lane++) {
            float[] streak=new float[14];
            for(int k=0;k<7;k++) {
                float u=k/6f,distance=length*u;
                streak[k*2]=pastX(c,e,L,x,distance)+lane*r*.25f*(1-u);
                streak[k*2+1]=y-distance;
            }
            p.polyline(streak,Glyph.withAlpha(color,fade/9),r*.24f);
            p.polyline(streak,Glyph.withAlpha(light,fade/5),r*.065f);
        }
        // Staggered lifetimes scatter stars behind the actual top/side-entry trajectory.
        for(int i=0;i<14;i++) {
            float u=(c.clock*.95f+i*.618034f+e.phase*.13f)%1f;
            float distance=r*.75f+length*u;
            float spread=(float)Math.sin(i*17.13f+e.phase)*r*(.25f+u*.65f);
            float tx=pastX(c,e,L,x,distance)+spread,ty=y-distance;
            float twinkle=.6f+.4f*(float)Math.sin(c.clock*8+i*2.4f);
            float rr=r*(.11f+.11f*(.5f+.5f*(float)Math.sin(i*7.4f)))*(1-u*.5f);
            int alpha=(int)(fade*(1-u)*(1-u)*(.60f+.30f*twinkle));
            p.fillCircle(tx,ty,rr*3.1f,Glyph.withAlpha(color,alpha/9));
            p.fillCircle(tx,ty,rr*1.9f,Glyph.withAlpha(light,alpha/5));
            float angle=c.clock*.65f+i*2.4f;
            p.fillPoly(Draw.star(tx,ty,rr,rr*.43f,i%3==0?5:4,angle),Glyph.withAlpha(light,alpha));
            p.fillPoly(Draw.star(tx,ty,rr*.52f,rr*.22f,4,angle),Glyph.withAlpha(0xFFFFFDE6,alpha));
        }
    }

    static void clearBurst(Painter p,GameCore c,GameCore.Enemy e,Layout L) {
        if(e.fastSingle && e.destroyed)
            starBurst(p,e.clearX,e.clearY,L.enemyR,Skins.keyColor(p,e.word[0]),e.phase,e.destroyT/GameCore.DESTROY_TIME,RainbowLand.active(c));
        if(e.swampKind!=SwampEnemies.DIVE)return;
        int last=e.word.length-1;
        // NINJA/MULTI can clear the marked key before the rest of its word.
        if(e.gone[last])
            starBurst(p,c.tileX(e,last,L),e.y,L.enemyR,Skins.keyColor(p,e.word[last]),e.phase,e.goneT[last],RainbowLand.active(c));
        else if(e.destroyed)
            starBurst(p,e.clearX+(last*.5f)*L.enemyStep,e.clearY,L.enemyR,
                    Skins.keyColor(p,e.word[last]),e.phase,e.destroyT/GameCore.DESTROY_TIME,RainbowLand.active(c));
    }

    static void starBurst(Painter p,float x,float y,float r,int color,float phase,float t) {
        starBurst(p,x,y,r,color,phase,t,false,1);
    }

    private static void starBurst(Painter p,float x,float y,float r,int color,float phase,float t,boolean rainbow) {
        if(rainbow)rainbowBurst(p,x,y,r,phase,t);
        else starBurst(p,x,y,r,color,phase,t,false,1);
    }

    static void rainbowBurst(Painter p,float x,float y,float r,float phase,float t) {
        starBurst(p,x,y,r*1.25f,RainbowLand.CLEAR_COLORS[0],phase,t,true,2);
    }

    private static void starBurst(Painter p,float x,float y,float r,int color,float phase,float t,boolean rainbow,int intensity) {
        if(t<0 || t>=1)return;
        float ease=1-(1-t)*(1-t),fade=(1-t)*(1-t);
        int light=Glyph.mix(color,0xFFFFF4C6,.65f);
        float flash=Math.max(0,1-t*4);
        p.fillCircle(x,y,r*(1.5f+ease*2),Glyph.withAlpha(color,(int)(80*flash)));
        p.fillPoly(Draw.star(x,y,r*(1+ease*2.7f),r*(.3f+ease*.6f),5,-1.57f),
                Glyph.withAlpha(light,(int)(200*flash)));
        for(int ring=0;ring<Fx.CLEAR_INTENSITY*intensity;ring++)
            p.strokePoly(outline(x,y,r*(1+ease*(2.5f+ring*.55f))),
                    Glyph.withAlpha(rainbow?RainbowLand.CLEAR_COLORS[ring%RainbowLand.CLEAR_COLORS.length]:ring%2==0?color:light,(int)(155*fade)),r*.16f*(1-t));
        for(int i=0;i<14*Fx.CLEAR_INTENSITY*intensity;i++) {
            float angle=phase+i*2.39996f;
            float reach=r*(.65f+ease*(2.5f+(i%3)*.7f)*Fx.CLEAR_SPREAD);
            float xx=x+(float)Math.cos(angle)*reach,yy=y+(float)Math.sin(angle)*reach;
            float size=r*(.16f+(i%3)*.055f)*(1-t*.6f);
            int alpha=(int)(255*fade);
            int particle=rainbow?RainbowLand.CLEAR_COLORS[i%RainbowLand.CLEAR_COLORS.length]:color;
            int shine=rainbow?Glyph.mix(particle,0xFFFFFFFF,.18f):light;
            p.line(xx-(float)Math.cos(angle)*r*ease*.9f,yy-(float)Math.sin(angle)*r*ease*.9f,
                    xx,yy,Glyph.withAlpha(particle,alpha/3),size*.7f);
            p.fillCircle(xx,yy,size*3,Glyph.withAlpha(particle,alpha/8));
            p.fillCircle(xx,yy,size*1.8f,Glyph.withAlpha(shine,alpha/4));
            p.fillPoly(Draw.star(xx,yy,size,size*.42f,i%2==0?5:4,angle+t*3),Glyph.withAlpha(shine,alpha));
        }
    }

    static void body(Painter p,float x,float y,float r,int color,int fade,boolean cleared) {
        int light=Glyph.mix(color,0xFFFFF0B1,.55f);
        float[] shape=outline(x,y,r);
        p.fillPoly(shape,Glyph.withAlpha(color,(cleared?26:95)*fade/255));
        p.strokePoly(shape,Glyph.withAlpha(color,(cleared?58:230)*fade/255),r*.10f);
        p.strokePoly(shape,Glyph.withAlpha(light,(cleared?40:245)*fade/255),r*.045f);
        if(!cleared) {
            float sx=x-r*.14f,sy=y-r*.92f;
            p.fillPoly(Draw.star(sx,sy,r*.14f,r*.04f,4,0),Glyph.withAlpha(0xFFFFFDE6,fade));
        }
    }
}
