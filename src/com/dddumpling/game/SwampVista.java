package com.dddumpling.game;

/** A low mangrove corridor: scenery approaches from a hazy water-level horizon. */
final class SwampVista extends Draw {
    static final int ROWS=6;
    static final float SPEED=.015f;
    private SwampVista() {}
    static float depth(float t){return t/(3-2*t);}
    static float scale(float t){return .14f+depth(t)*1.55f;}
    static float horizon(Layout L){return L.h*.43f;}
    static float rootY(Layout L,float t){return horizon(L)+depth(t)*(L.deckTop-horizon(L)+L.h*.25f);}
    static float sideX(float t,int side){float d=depth(t);return .5f+side*(.13f+d*.56f+d*d*d*.85f);}
    static float travel(float clock,int row,int side){float phase=clock*SPEED*ROWS+(side>0?.43f:0);return (row+phase-(float)Math.floor(phase))/ROWS;}
    static float arena(GameCore c) {
        if(BossCollect.keepsArena(c,Boss.ALLIGATOR))return 1;
        Boss b=BossVictory.retained(c) && c.bossVictoryKind==Boss.ALLIGATOR?c.bossVictory:c.boss;
        if(!b.active() || b.kind!=Boss.ALLIGATOR)return 0;
        float t=Math.max(0,Math.min(1,(b.introDuration()-b.intro)/.9f));return t*t*(3-2*t);
    }
    private static int ink(int color,float fade){return Glyph.withAlpha(color,(int)(255*Math.max(0,Math.min(1,fade))));}
    private static float reveal(float t){float u=Math.min(1,t/.16f);return u*u*(3-2*u);}

    static void draw(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-BossVictory.backdropDrain(c);
        if(fade<=.001f)return;
        float time=c.cloudUi.reduceMotion?0:c.skyClock;
        backdrop(p,L,fade);
        water(p,c,L,fade);
        banks(p,c,L,fade);
        // Far rows are painted first. The same world tree advances to the next row at wrap.
        for(int row=0;row<ROWS;row++)for(int side=-1;side<=1;side+=2) {
            float phase=c.skyClock*SPEED*ROWS+(side>0?.43f:0);
            int world=row-(int)Math.floor(phase),seed=4391+world*137+side*53;
            float t=travel(c.skyClock,row,side),d=depth(t),s=scale(t);
            float x=L.w*(sideX(t,side)+.025f*(float)Math.sin(world*.9f)*d),y=rootY(L,t);
            float r=L.w*.19f*s,h=L.h*(.62f+hash(seed)*.12f)*s;
            if(x+r*3.2f<0 || x-r*3.2f>L.w)continue;
            float opacity=fade*reveal(t);if(opacity<.008f)continue;
            float clarity=.22f+.78f*Math.min(1,d*2.5f);
            tree(p,x,y,r,h,seed,time,clarity,opacity);
            grass(p,x-side*r*.65f,y+r*.08f,r*.95f,seed+17,time,clarity,opacity);
            grass(p,x+side*r*.85f,y+r*.10f,r*.70f,seed+41,time,clarity,opacity*.85f);
        }
        LandBonusArt.inSwamp(p,c,L,fade);
        // A few closer reeds frame the water without covering the centre or the bonus.
        for(int side=-1;side<=1;side+=2)
            grass(p,L.w*(side<0?-.05f:1.05f),L.deckTop+L.w*.04f,L.w*.20f,713+side*43,time,1,fade*.72f);
        for(int i=0;i<9;i++) {
            int seed=341+i*37;float x=L.w*(.15f+hash(seed)*.70f),y=L.h*(.18f+hash(seed+3)*.45f);
            x+=L.w*.012f*(float)Math.sin(time*.5f+i);y+=L.h*.008f*(float)Math.sin(time*.7f+i*2);
            float glow=.5f+.5f*(float)Math.sin(time*1.3f+i*2.4f),r=L.w*.0018f;
            p.fillCircle(x,y,r*3,ink(0xFFCADA98,fade*glow*.08f));
            p.fillCircle(x,y,r,ink(0xFFDFE7B5,fade*(.18f+glow*.20f)));
        }
    }
    private static void backdrop(Painter p,Layout L,float fade) {
        float horizon=horizon(L);
        for(int i=0;i<80;i++) {
            float t=i/80f,y=i==0?-L.h*.03f:(float)Math.floor(t*L.deckTop),next=(float)Math.floor((i+1)/80f*L.deckTop);
            float h=horizon/L.deckTop;
            int color=t<h?Glyph.mix(0xFF263E3B,0xFF657C68,t/h):Glyph.mix(0xFF657C68,0xFF153E43,(float)(1-Math.exp(-3.7f*(t-h)/(1-h))));
            p.fillRect(-L.w*.06f,y,L.w*1.06f,next,ink(color,fade));
        }
        for(int i=4;i>=0;i--)p.fillEllipse(L.w*.51f,horizon-L.h*.10f,L.w*(.13f+i*.045f),L.h*(.18f+i*.018f),
                ink(0xFFBBBE8F,fade*.025f));
        // Thin pale trunks disappear into the opening beyond the grass.
        for(int i=0;i<16;i++) {
            int seed=919+i*67;float x=L.w*(.04f+hash(seed)*.92f),y=horizon+L.h*(hash(seed+3)*.025f);
            float h=L.h*(.21f+hash(seed+7)*.24f),lean=L.w*(hash(seed+11)-.5f)*.13f,w=L.w*(.004f+hash(seed+13)*.007f);
            int color=ink(0xFF4D6B59,fade*(.15f+hash(seed+17)*.18f));
            p.fillPoly(new float[]{x-w,y,x+w,y,x+lean+w*.3f,y-h,x+lean-w*.3f,y-h},color);
            p.line(x+lean*.45f,y-h*.45f,x+lean-L.w*.07f,y-h*.82f,color,w*.52f);
            p.line(x+lean*.65f,y-h*.65f,x+lean+L.w*.05f,y-h*.94f,color,w*.42f);
        }
    }
    private static void banks(Painter p,GameCore c,Layout L,float fade) {
        float h=horizon(L),range=L.deckTop-h;
        float[] edge=new float[42];
        for(int side=-1;side<=1;side+=2) {
            edge[0]=side<0?0:L.w;edge[1]=h;
            for(int i=0;i<=18;i++) {
                float t=i/18f,width=.10f+.34f*(float)Math.sqrt(t);
                float bend=.018f*(float)Math.sin(t*10+c.skyClock*.025f)*t;
                edge[(i+1)*2]=L.w*(.5f+side*width+bend);edge[(i+1)*2+1]=h+t*range;
            }
            edge[40]=side<0?0:L.w;edge[41]=L.deckTop;
            p.fillPoly(edge,ink(0xFF324F3B,fade*.55f));
            for(int i=0;i<8;i++) {
                int seed=1847+i*53+side*37;
                float x=L.w*(side<0?hash(seed)*.42f:.58f+hash(seed)*.42f);
                grass(p,x,h+L.h*(.007f+hash(seed+3)*.018f),L.w*(.030f+hash(seed+7)*.020f),seed,0,.20f,fade*.65f);
            }
        }
        for(int i=0;i<16;i++) {
            float u=(i+.5f)/16,y=h+L.h*(i/16f-.5f)*.08f,next=h+L.h*((i+1)/16f-.5f)*.08f;
            p.fillRect(0,y,L.w,next,ink(0xFF657C68,fade*.42f*(float)Math.sin(u*Math.PI)));
        }
    }
    private static void water(Painter p,GameCore c,Layout L,float fade) {
        float time=c.cloudUi.reduceMotion?0:c.skyClock;
        float h=horizon(L),range=L.deckTop-h;
        for(int i=0;i<22;i++) {
            float t=(i+(time*.22f)%1)/22,d=t*t,y=h+d*range;
            float width=L.w*(.025f+.18f*d),x=L.w*(.51f+.035f*(float)Math.sin(i*2.7f+time*.16f));
            float a=fade*(.07f+.05f*d)*(float)Math.sin(t*Math.PI);
            p.fillPoly(new float[]{x-width,y,x+width*.8f,y-L.w*.001f,x+width*.48f,y+L.w*(.002f+d*.004f),x-width*.74f,y+L.w*(.003f+d*.004f)},ink(0xFF96B19A,a));
            for(int side=-1;side<=1;side+=2) {
                float xx=L.w*(.5f+side*(.12f+d*.32f));
                p.line(xx-width*.65f,y+L.w*.006f,xx+width*.65f,y+L.w*.006f,ink(0xFF6A9A8D,fade*(.025f+d*.045f)),L.w*(.001f+d*.001f));
            }
        }
    }
    private static int shade(int color,float clarity,float fade){return ink(Glyph.mix(0xFF758579,color,clarity),fade);}
    private static void tree(Painter p,float x,float y,float r,float h,int seed,float time,float clarity,float fade) {
        float lean=(hash(seed+5)-.5f)*r*1.3f,top=x+lean,curve=(hash(seed+19)-.5f)*r*.65f;
        int bark=shade(0xFF354C3D,clarity,fade*.90f),dark=shade(0xFF1E3B31,clarity,fade*.85f),light=shade(0xFF697A50,clarity,fade*.65f);
        p.fillEllipse(x,y+r*.06f,r*1.5f,r*.22f,shade(0xFF203F34,clarity,fade*.35f));
        p.arc(x,y+r*.10f,r*1.7f,r*.28f,0,180,shade(0xFF82AA91,clarity,fade*.10f),r*.018f);
        // Low, kinked reflections break up the tree's contact with the water.
        for(int i=0;i<4;i++)p.fillEllipse(x+lean*.12f,y+r*(.16f+i*.13f),r*(.65f-i*.11f),r*.035f,
                shade(0xFF193B36,clarity,fade*(.18f-i*.035f)));
        float[] trunk=new float[28],shine=new float[14];
        for(int i=0;i<7;i++) {
            float u=i/6f,cx=x+lean*u+curve*(float)Math.sin(u*Math.PI),yy=y-h*(.08f+u*.92f),w=r*(.27f-u*.19f);
            trunk[i*2]=cx-w;trunk[i*2+1]=yy;trunk[(13-i)*2]=cx+w;trunk[(13-i)*2+1]=yy;
            shine[i*2]=cx-r*.055f;shine[i*2+1]=yy;
        }
        p.fillPoly(trunk,bark);p.polyline(shine,light,r*.045f);
        for(int i=0;i<4;i++) {
            float side=i%2==0?-1:1,u=.45f+i*.12f,bx=x+lean*u+curve*(float)Math.sin(u*Math.PI),by=y-h*u;
            float tip=top+side*r*(.85f+hash(seed+i*29)*.8f),ty=y-h*(.83f+hash(seed+i*31)*.19f);
            p.polyline(new float[]{bx,by,bx+side*r*.45f,by-h*.17f,tip,ty},bark,r*(.10f-i*.014f));
        }
        for(int i=0;i<5;i++) {
            float side=i%2==0?-1:1,reach=r*(.48f+hash(seed+i*41)*.95f);
            float end=x+side*reach,foot=y+r*(.025f+hash(seed+i*43)*.13f);
            float[] root={x+side*r*.10f,y-h*(.13f+hash(seed+i*47)*.10f),x+side*reach*.42f,y-h*.09f,
                    end-side*r*.15f,foot-r*.18f,end,foot};
            p.polyline(root,dark,r*.12f);p.polyline(root,bark,r*.082f);
            p.line(root[2]-r*.018f,root[3],root[4]-r*.018f,root[5],light,r*.023f);
        }
        float sway=(float)Math.sin(time*.38f+seed)*r*.018f;
        for(int i=0;i<5;i++) {
            float cx=top+(i-2)*r*.60f+sway,cy=y-h+r*(.06f+hash(seed+i*53)*.35f);
            crown(p,cx,cy,r*(.76f+hash(seed+i*61)*.40f),seed+i*59,shade(i%2==0?0xFF385D3D:0xFF476E46,clarity,fade*.72f));
        }
    }
    private static void crown(Painter p,float x,float y,float r,int seed,int color) {
        float[] shape=new float[24];
        for(int i=0;i<12;i++) {
            float a=i*Softbody.TAU/12,reach=r*(.78f+hash(seed+i*37)*.22f);
            shape[i*2]=x+(float)Math.cos(a)*reach;shape[i*2+1]=y+(float)Math.sin(a)*reach*.58f;
        }
        p.fillPoly(shape,color);
    }
    private static void grass(Painter p,float x,float y,float r,int seed,float time,float clarity,float fade) {
        p.fillEllipse(x,y,r*1.1f,r*.16f,shade(0xFF294A34,clarity,fade*.40f));
        float[] blade=new float[8];
        int count=clarity<.3f?12:25;
        for(int i=0;i<count;i++) {
            int n=seed+i*31;float bx=x+(hash(n)-.5f)*r*1.9f,by=y+(hash(n+3)-.5f)*r*.13f;
            float h=r*(.48f+hash(n+5)*1.20f),lean=(hash(n+7)-.5f)*r*.48f+(float)Math.sin(time*.75f+n)*r*.035f;
            float w=r*(.025f+hash(n+9)*.035f),tip=bx+lean;
            blade[0]=bx-w;blade[1]=by;blade[2]=bx+w;blade[3]=by;blade[4]=tip;blade[5]=by-h;
            blade[6]=bx+lean*.38f-w*.4f;blade[7]=by-h*.52f;
            p.fillPoly(blade,shade(i%3==0?0xFF93A66B:i%3==1?0xFF547C4E:0xFF6F955C,clarity,fade*(.65f+hash(n+11)*.2f)));
            if(i%6==0) {
                p.line(bx,by,tip,by-h,shade(0xFF7C986A,clarity,fade*.7f),r*.018f);
                p.line(tip,by-h,tip+lean*.06f,by-h-r*.22f,shade(0xFF75664A,clarity,fade*.85f),r*.065f);
            }
        }
    }
}
