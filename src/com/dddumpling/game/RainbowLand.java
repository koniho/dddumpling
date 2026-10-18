package com.dddumpling.game;

/** Floating sherbet islands and layered rainbow arches. */
final class RainbowLand extends Draw {
    static boolean active(GameCore c) {
        return !c.survival.active && !c.timeAttack.active && Lands.forStage(c.stage)==Lands.RAINBOW;
    }
    static void enemyTrail(Painter p,GameCore c,GameCore.Enemy e,Layout L,int fade) {
        if(!active(c) || fade<=0)return;
        float t=e.destroyed?Math.min(1,e.destroyT/GameCore.DESTROY_TIME):0;
        if(t>=1)return;
        float spread=1-(1-t)*(1-t),clock=e.destroyed?e.clearClock:c.clock;
        float x=e.destroyed?e.clearX:c.enemyCentreX(e),y=(e.destroyed?e.clearY:e.y)-rim(L);
        fade=(int)(fade*(1-t)*(1-t));
        float width=Math.min(L.w*.72f,L.wordWidth(e.word.length)*.85f)*(1+2.1f*spread);
        float length=L.enemyR*(e.diving?6.5f:e.fastSingle?5f:3.6f);
        length=Math.min(length,Math.max(0,(e.destroyed?e.clearY:e.y)+L.enemyR));
        float[] shape=new float[8];
        int bands=RainbowCatArt.COLORS.length;
        for(int band=0;band<bands;band++)for(int part=0;part<3;part++) {
            float a=part/3f,b=(part+1)/3f;
            float wa=width*(1-a*.38f),wb=width*(1-b*.38f);
            float ax=x+(float)Math.sin(clock*2+e.phase-a*2)*L.enemyR*a*.32f;
            float bx=x+(float)Math.sin(clock*2+e.phase-b*2)*L.enemyR*b*.32f;
            shape[0]=ax+wa*(band/(float)bands-.5f);shape[1]=y-a*length;
            shape[2]=ax+wa*((band+1f)/bands-.5f);shape[3]=shape[1];
            shape[4]=bx+wb*((band+1f)/bands-.5f);shape[5]=y-b*length;
            shape[6]=bx+wb*(band/(float)bands-.5f);shape[7]=shape[5];
            p.fillPoly(shape,Glyph.withAlpha(RainbowCatArt.COLORS[band],(int)(fade*.29f*(1-b*.78f))));
        }
    }
    private static float rim(Layout L) {return L.enemyR*.25f;}
    static void prop(Painter p,float x,float y,float r,int alpha,float t,boolean silhouette) {
        float bob=(float)Math.sin(t*1.8f)*r*.10f;
        int cloud=Glyph.withAlpha(silhouette?0xFF443B64:0xFFECE1FF,alpha);
        for(int i=0;i<RainbowCatArt.COLORS.length;i++)RainbowCatArt.arch(p,x,y+bob,r*(1.1f-i*.10f),r*(1.0f-i*.10f),
                Glyph.withAlpha(silhouette?0xFF443B64:RainbowCatArt.COLORS[i],alpha),r*.13f);
        for(int side=-1;side<=1;side+=2) {
            float xx=x+side*r*.84f;
            p.fillEllipse(xx,y+r*.04f+bob,r*.46f,r*.20f,cloud);
            p.fillCircle(xx-r*.15f,y-r*.08f+bob,r*.23f,cloud);
            p.fillCircle(xx+r*.15f,y-r*.12f+bob,r*.27f,cloud);
            p.fillEllipse(xx-r*.05f,y-r*.15f+bob,r*.25f,r*.07f,Glyph.withAlpha(0xFFFFFFFF,alpha/2));
        }
    }
    static void skit(Painter p,int variant,float x,float y,float r,float t,float a) {
        prop(p,x,y+r*.3f,r,(int)(a*220),t,false);
        float loop=t*2,hop=Math.abs((float)Math.sin(loop));
        RainbowCatArt.portrait(p,x+(float)Math.sin(loop*.6f)*r*.6f,y-r*(.4f+hop*.5f),r*.54f,t,true,a);
        RainbowCatArt.yarn(new OpacityPainter(p,a),x+r*(float)Math.cos(loop),y+r*.12f,r*.22f,t,false);
    }
    static void title(Painter p,float x,float y,float s,float span,float rise,float clock,float fade,boolean front) {
        for(int i=0;i<3;i++) {
            float xx=x+span*(i-1)*.68f,yy=y+s*(front?.45f:-.50f)+(1-rise)*s*1.6f;
            prop(p,xx,yy,s*(front?.63f:.78f),(int)(fade*(front?175:110)),clock+i,false);
        }
    }
}
