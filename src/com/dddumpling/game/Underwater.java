package com.dddumpling.game;

/** Broken reef walls, quiet sea life and a sheltered cave. Cosmetic motion never uses game RNG. */
final class Underwater extends Draw {
    static final int LAND=2;
    private static final int WATER=0xFF123E50, LIGHT=0xFF98E7DB;
    private Underwater() {}

    static float caveProgress(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        if(BossCollect.keepsArena(c,Boss.OCTOPUS))return 1;
        Boss b=BossVictory.sceneBoss(c);
        if(!b.active() || b.kind!=Boss.OCTOPUS)return 0;
        return smooth(Math.min(1,b.introProgress()/0.70f));
    }
    static float bossReveal(Boss b) {return smooth((b.introProgress()-.55f)/.45f);}
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float mix=Lands.blend(c);
        return Math.max(caveProgress(c),(c.landFrom==LAND?1-mix:0)
                +(Lands.forStage(Lands.visualStage(c))==LAND?mix:0));
    }
    private static float smooth(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static int ink(int col,float alpha) {return Glyph.withAlpha(col,(int)alpha);}

    static void backdrop(Painter p,GameCore c,Layout L) {
        float fade=presence(c)*(1-BossVictory.backdropDrain(c));
        if(fade<=.001f)return;
        float cave=caveProgress(c);
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        p.fillRect(0,0,L.w,L.deckTop,ink(WATER,245*fade));
        // A few nested pools of blue give depth without blur passes or full-screen distortion.
        for(int layer=3;layer>=0;layer--)p.fillEllipse(L.w*.54f,L.h*.40f,
                L.w*(.38f+layer*.15f),L.h*(.23f+layer*.12f),ink(0xFF367F86,fade*(8+layer*2)*(1-cave*.65f)));
        rays(p,c,L,fade*(1-cave*.70f));
        if(cave<1)reefs(p,c,L,fade*(1-cave),cave*L.h*.36f);
        if(cave>0)cave(p,c,L,fade,cave);
        caustics(p,c,L,fade*(1-cave*.45f),false);
        fish(p,c,L,fade);
        p.restore();
    }

    private static void rays(Painter p,GameCore c,Layout L,float fade) {
        for(int i=0;i<4;i++) {
            float top=L.w*(-.4f+i*.31f),sway=L.w*.06f*(float)Math.sin(c.clock*.23f+i);
            for(int layer=0;layer<2;layer++) {
                float width=L.w*(.035f+layer*.025f);
                p.fillPoly(new float[]{top-width,-L.h*.1f,top+width,-L.h*.1f,
                        top+L.w*.66f+sway+width*2,L.deckTop,top+L.w*.46f+sway-width*2,L.deckTop},
                        ink(LIGHT,fade*(layer==0?8:3)));
            }
        }
    }

    private static void reefs(Painter p,GameCore c,Layout L,float fade,float arrival) {
        for(int depth=0;depth<2;depth++)for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            int seed=1253+depth*103+side*43+i*67;
            float span=L.h*1.65f,slot=span/3;
            // Each outcrop occupies less than half a slot: open water visibly separates cliffs.
            float y=(i*slot+side*slot*.43f+hash(seed)*slot*.12f+c.skyClock*L.h*(.015f+depth*.012f)+arrival)%span-L.h*.30f;
            float h=slot*(.20f+hash(seed+1)*.045f),w=L.w*(.12f+hash(seed+2)*.11f)*(depth==0?.75f:1);
            if(y+h<-20 || y-h>L.deckTop+20)continue;
            cliff(p,L.w*side,y,w,h,side==0?1:-1,fade*(depth==0?.42f:.88f),seed);
            if(depth==1)garden(p,c,L,L.w*side+(side==0?1:-1)*w*.58f,y-h*.18f,w*.65f,fade,seed);
        }
    }

    private static void cliff(Painter p,float x,float y,float w,float h,int side,float fade,int seed) {
        float[] outline={0,-1,.38f,-.91f,.67f,-.66f,.62f,-.39f,.94f,-.16f,1,.06f,
                .87f,.30f,.55f,.36f,.67f,.58f,.38f,.84f,0,1,-.4f,1,-.4f,-1};
        float[] shape=new float[outline.length];
        for(int layer=0;layer<3;layer++) {
            float inset=layer*.13f;
            for(int j=0;j<shape.length;j+=2) {
                shape[j]=x+side*w*(outline[j]*(1-inset)-inset*.3f);
                shape[j+1]=y+h*(outline[j+1]*(1-layer*.06f)-layer*.045f);
            }
            p.fillPoly(shape,ink(layer==0?0xFF0A273B:layer==1?0xFF285464:0xFF3C6C72,fade*(layer==0?240:190)));
        }
        // Broken mineral shelves and lit edges follow the rock instead of outlining every facet.
        for(int j=0;j<3;j++) {
            float yy=y+h*(-.65f+j*.51f),reach=w*(.49f+hash(seed+j)*.23f);
            p.fillPoly(new float[]{x,yy-h*.10f,x+side*reach*.74f,yy-h*.04f,
                    x+side*reach,yy+h*.03f,x+side*reach*.42f,yy+h*.055f,x,yy+h*.01f},ink(0xFF79A39A,fade*45));
        }
    }

    private static void cave(Painter p,GameCore c,Layout L,float fade,float progress) {
        p.save();p.translate(0,-(1-progress)*L.h*.68f);
        // An irregular roof and receding rock arches enclose a dark, open central chamber.
        float[] roof=new float[50];
        for(int layer=0;layer<3;layer++) {
            float top=L.h*(.24f-layer*.042f);
            roof[0]=-L.w*.1f;roof[1]=-L.h;
            for(int j=0;j<=21;j++) {
                float x=(j/21f*1.2f-.1f)*L.w,u=Math.abs(x/L.w-.51f)*2;
                roof[2+j*2]=x;
                roof[3+j*2]=top+L.h*(.20f*u*u+.017f*(float)Math.sin(j*2.3f+layer));
            }
            roof[46]=L.w*1.1f;roof[47]=-L.h;roof[48]=-L.w*.1f;roof[49]=-L.h;
            p.fillPoly(roof,ink(layer==0?0xFF275060:layer==1?0xFF1B3C51:0xFF102839,fade*(135+layer*45)));
        }
        for(int side=0;side<2;side++) {
            cliff(p,L.w*side,L.h*.48f,L.w*.18f,L.h*.36f,side==0?1:-1,fade,991+side);
            cliff(p,L.w*side,L.deckTop-L.h*.06f,L.w*.28f,L.h*.13f,side==0?1:-1,fade,953+side);
            garden(p,c,L,L.w*(side==0?.13f:.87f),L.deckTop-L.h*.065f,L.w*.12f,fade,373+side);
        }
        // Sand-lit hollow under Octopulse; rim is textured rather than a hard horizon.
        for(int layer=2;layer>=0;layer--)p.fillEllipse(L.w*.5f,L.deckTop-L.h*.08f,L.w*(.34f+layer*.07f),L.h*(.034f+layer*.018f),ink(0xFF599899,fade*(9+layer*3)));
        for(int i=0;i<12;i++) {
            float x=L.w*(.10f+i*.073f),y=L.h*(.22f+.18f*(float)Math.pow(Math.abs(x/L.w-.51f)*2,2));
            float r=L.w*(.003f+hash(i+71)*.004f),pulse=.65f+.35f*(float)Math.sin(c.clock*1.3f+i*2.1f);
            p.fillCircle(x,y,r*2.9f,ink(LIGHT,fade*pulse*12));
            p.fillCircle(x,y,r,ink(LIGHT,fade*pulse*105));
        }
        p.restore();
    }

    private static void garden(Painter p,GameCore c,Layout L,float x,float y,float r,float fade,int seed) {
        float[] ribbon=new float[28];
        for(int stem=0;stem<4;stem++) {
            float base=x+(stem-1.5f)*r*.25f,height=r*(1.25f+hash(seed+stem)*1.0f);
            for(int j=0;j<7;j++) {
                float u=j/6f,xx=base+u*u*r*.28f*(float)Math.sin(c.clock*1.35f+seed+u*2.1f);
                float yy=y-u*height,width=r*.075f*(1-u*.88f);
                ribbon[j*2]=xx-width;ribbon[j*2+1]=yy;
                ribbon[(13-j)*2]=xx+width;ribbon[(13-j)*2+1]=yy;
            }
            p.fillPoly(ribbon,ink(stem%2==0?0xFF448E81:0xFF75AA91,fade*(stem%2==0?135:115)));
        }
        float cx=x+r*.40f,cy=y+r*.11f;
        for(int branch=0;branch<5;branch++) {
            float dx=(branch-2)*r*.23f,dy=-r*(.36f+hash(seed+branch+20)*.42f);
            int col=ink(branch%2==0?0xFFC07C9E:0xFFA288BE,fade*145);
            p.polyline(new float[]{cx,cy,cx+dx*.33f,cy+dy*.55f,cx+dx,cy+dy},col,r*.075f);
            p.polyline(new float[]{cx+dx*.33f,cy+dy*.55f,cx+dx-r*.14f,cy+dy*.9f},col,r*.05f);
            p.fillCircle(cx+dx,cy+dy,r*.047f,ink(0xFFE9B5BC,fade*155));
        }
    }

    private static void fish(Painter p,GameCore c,Layout L,float fade) {
        for(int school=0;school<3;school++) {
            int direction=school%2==0?1:-1;
            float travel=(c.clock*(.022f+school*.005f)+school*.37f)%1;
            float x=L.w*(-.20f+travel*1.4f);if(direction<0)x=L.w-x;
            for(int i=0;i<4;i++) {
                float xx=x-direction*i*L.w*.035f,yy=L.h*(.29f+school*.19f)+L.h*.014f*(float)Math.sin(c.clock*1.2f+school+i*.8f);
                float r=L.w*(.009f+(i%2)*.002f),tail=.65f+.22f*(float)Math.sin(c.clock*10+i);
                int col=ink(school==1?0xFFBCA2BA:0xFF88BEB6,fade*95);
                p.fillPoly(new float[]{xx+r*direction,yy,xx,yy-r*.5f,xx-r*direction,yy-r*.20f,
                        xx-r*1.65f*direction,yy-r*tail,xx-r*1.65f*direction,yy+r*tail,
                        xx-r*direction,yy+r*.20f,xx,yy+r*.5f},col);
                p.fillCircle(xx+r*.5f*direction,yy-r*.1f,r*.11f,ink(0xFF16394C,fade*160));
            }
        }
    }

    /** Sparse refracted light continues in front of words, with the same phase as their sheen. */
    static void veil(Painter p,GameCore c,Layout L) {
        float fade=presence(c)*(1-BossVictory.backdropDrain(c));if(fade<=.001f)return;
        p.save();p.clipRect(0,L.playTop,L.w,L.dangerY);
        caustics(p,c,L,fade*.40f,true);
        for(int i=0;i<14;i++) {
            float span=L.deckTop+40,y=span-(hash(234+i)*span+c.clock*(8+i%3*3))%span;
            float x=L.w*hash(877+i)+(float)Math.sin(c.clock*.8f+i)*L.w*.013f,r=L.w*(.002f+hash(57+i)*.003f);
            p.arc(x,y,r,r*1.2f,195,230,ink(LIGHT,fade*42),Math.max(.7f,r*.20f));
        }
        p.restore();
    }
    private static void caustics(Painter p,GameCore c,Layout L,float fade,boolean front) {
        float[] ribbon=new float[36];
        for(int i=0;i<(front?6:14);i++) {
            float span=L.deckTop+L.h*.2f,y=(hash(i+571)*span+c.skyClock*L.h*.016f)%span-L.h*.10f;
            float x=L.w*hash(i+813),r=L.w*(.09f+hash(i+21)*.09f),phase=c.clock*.63f+i;
            float pulse=.4f+.6f*(float)Math.pow(Math.sin(phase),2);
            for(int j=0;j<9;j++) {
                float u=j/8f;
                float xx=x+(u-.5f)*r*2,yy=y+r*.27f*(float)Math.sin(u*5+phase*.6f);
                float width=r*.025f*(float)Math.sin(u*Math.PI);
                ribbon[j*2]=ribbon[(17-j)*2]=xx;
                ribbon[j*2+1]=yy-width;ribbon[(17-j)*2+1]=yy+width;
            }
            p.fillPoly(ribbon,ink(LIGHT,fade*pulse*24));
        }
    }

    static int enemyColor(GameCore c,int color,float y,Layout L) {
        float amount=presence(c)*(.08f+.045f*(1+(float)Math.sin(c.clock*1.5f-y/L.h*11)));
        return Glyph.mix(color,LIGHT,amount);
    }
    static void tileLight(Painter p,GameCore c,Layout L,float x,float y,float r,int fade) {
        float water=presence(c);if(water<=0 || fade<=0)return;
        float t=(c.clock*.29f+y/L.h*.71f+x/L.w*.13f)%1;
        float yy=(t*1.54f-.77f)*r,half=r-Math.abs(yy)*.57735f;
        float width=r*.065f,edge=Math.max(0,half-r*.11f);
        // Fitted inside the hexagon, under the character: no distortion of the input target.
        p.fillPoly(new float[]{x-edge,y+yy-width,x+edge*.85f,y+yy-width*.4f,
                x+edge,y+yy+width*.55f,x-edge*.86f,y+yy+width},ink(LIGHT,water*fade*.22f*(float)Math.sin(t*Math.PI)));
    }
}
