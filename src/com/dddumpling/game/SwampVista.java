package com.dddumpling.game;

/** A low mangrove corridor: scenery approaches from a hazy water-level horizon. */
final class SwampVista extends Draw {
    static final int ROWS=6;
    static final float SPEED=.030f;
    private SwampVista() {}
    static float depth(float t){return t/(3-2*t);}
    static float scale(float t){return .14f+depth(t)*1.55f;}
    static float horizon(Layout L){return L.h*.43f;}
    static float rootY(Layout L,float t){return horizon(L)+depth(t)*(L.deckTop-horizon(L)+L.h*.25f);}
    static float sideX(float t,int side){float d=depth(t);return .5f+side*(.13f+d*.56f+d*d*d*1.30f);}
    static float bend(float clock,float d) {
        d=Math.max(0,Math.min(1,d));float t=3*d/(1+2*d);
        return .19f*(float)Math.sin(Math.PI*Math.sqrt(d))*(float)Math.sin(t*12-clock*SPEED*12+.6f);
    }
    static float waterDepth(Layout L,float y){return Math.max(0,(y-horizon(L))/(L.deckTop-horizon(L)+L.h*.25f));}
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
        magicMist(p,L,time,fade);
        water(p,c,L,fade);
        waterShimmer(p,c,L,time,fade);
        banks(p,c,L,fade);
        // Far rows are painted first. The same world tree advances to the next row at wrap.
        for(int row=0;row<ROWS;row++)for(int side=-1;side<=1;side+=2) {
            float phase=c.skyClock*SPEED*ROWS+(side>0?.43f:0);
            int world=row-(int)Math.floor(phase),seed=4391+world*137+side*53;
            float t=travel(c.skyClock,row,side),d=depth(t),s=scale(t);
            float x=L.w*(sideX(t,side)+bend(c.skyClock,d)+.025f*(float)Math.sin(world*.9f)*d),y=rootY(L,t);
            float r=L.w*.19f*s,h=L.h*(.62f+hash(seed)*.12f)*s;
            if(x+r*4.4f<0 || x-r*4.4f>L.w)continue;
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
        fireflies(p,L,time,fade);
    }
    private static final int[] MAGIC={0xFF75E8C7,0xFFC399E4,0xFFA9D987};
    private static void glow(Painter p,float x,float y,float rx,float ry,int color,float strength) {
        for(int i=3;i>=1;i--) {
            float spread=1+i*.55f;
            p.fillEllipse(x,y,rx*spread,ry*spread,ink(color,strength*(4-i)*.04f));
        }
    }
    private static void magicMist(Painter p,Layout L,float time,float fade) {
        // Broad translucent ribbons sit behind trunks, giving the opening luminous depth.
        for(int band=0;band<3;band++) {
            float[] ribbon=new float[100];
            for(int veil=4;veil>=1;veil--) {
                for(int i=0;i<25;i++) {
                    float u=i/24f,x=L.w*(-.12f+u*1.24f);
                    float y=L.h*(.18f+band*.072f+.046f*(float)Math.sin(u*5.4f+band*1.8f+time*.10f));
                    float width=L.h*(.018f+.016f*(float)Math.sin(u*Math.PI))*veil/4;
                    ribbon[i*2]=x;ribbon[i*2+1]=y-width;
                    ribbon[(49-i)*2]=x;ribbon[(49-i)*2+1]=y+width;
                }
                p.fillPoly(ribbon,ink(MAGIC[band],fade*.024f));
            }
            glow(p,L.w*(.27f+band*.24f),L.h*(.28f+band*.045f),L.w*.11f,L.h*.055f,MAGIC[band],fade*.48f);
        }
    }
    private static void waterShimmer(Painter p,GameCore c,Layout L,float time,float fade) {
        // Iridescent currents follow the water's perspective, with no bright centre overlay.
        float[] ribbon=new float[100];
        for(int band=0;band<7;band++) {
            float d=(band+.5f)/7,y=horizon(L)+d*d*(L.deckTop-horizon(L));
            float span=L.w*(.10f+d*.34f);
            int color=MAGIC[band%3];
            for(int veil=3;veil>=1;veil--) {
                for(int i=0;i<25;i++) {
                    float u=i/24f,x=L.w*(.5f+bend(c.skyClock,waterDepth(L,y)))+(u*2-1)*span;
                    float yy=y+L.h*(.003f+d*.009f)*(float)Math.sin(u*9+time*.35f+band*1.7f);
                    float width=L.w*(.0015f+d*.002f)*veil*(float)Math.sin(u*Math.PI);
                    ribbon[i*2]=x;ribbon[i*2+1]=yy-width;
                    ribbon[(49-i)*2]=x;ribbon[(49-i)*2+1]=yy+width;
                }
                p.fillPoly(ribbon,ink(color,fade*(.018f+.025f*d)));
            }
        }
    }
    private static void fireflies(Painter p,Layout L,float time,float fade) {
        if(fade<=.001f)return;
        // Three depths of wandering insects: small distant specks and warm nearby lanterns.
        for(int i=0;i<27;i++) {
            int seed=341+i*37,layer=i%3;float d=(layer+1)/3f;
            float side=i%2==0?-1:1;
            float x=L.w*(.5f+side*(.10f+hash(seed)*.32f));
            float y=L.h*(.23f+hash(seed+3)*(.20f+d*.27f));
            x+=L.w*(.008f+d*.016f)*(float)Math.sin(time*(.23f+d*.17f)+i*2.1f);
            y+=L.h*(.004f+d*.010f)*(float)Math.sin(time*.31f+i*1.7f);
            float pulse=.5f+.5f*(float)Math.sin(time*(.75f+hash(seed+7)*.45f)+i*2.4f);
            float light=fade*(.32f+pulse*.68f),r=L.w*(.0013f+d*.0021f);
            int color=i%5==0?MAGIC[0]:0xFFE3ED91;
            glow(p,x,y,r*2.7f,r*2.7f,color,light*(.50f+d*.45f));
            p.fillCircle(x,y,r*1.65f,ink(color,light*.22f));
            p.fillCircle(x,y,r,ink(0xFFFFF6C5,light*(.5f+d*.40f)));
            if(layer==2) {
                float wing=r*(1.4f+.45f*(float)Math.sin(time*9+i));
                p.fillEllipse(x-r*1.2f,y-r*.6f,wing,r*.55f,ink(0xFFD2F5D7,light*.20f));
                p.fillEllipse(x+r*1.2f,y-r*.6f,wing,r*.55f,ink(0xFFD2F5D7,light*.20f));
                if(y>horizon(L))p.fillEllipse(x,y+L.h*.045f,r*4,r*.6f,ink(color,light*.09f));
            }
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
                float t=i/18f,width=.065f+.28f*(float)Math.sqrt(t);
                float bend=bend(c.skyClock,waterDepth(L,h+t*range));
                edge[(i+1)*2]=L.w*(.5f+side*width+bend);edge[(i+1)*2+1]=h+t*range;
            }
            edge[40]=side<0?0:L.w;edge[41]=L.deckTop;
            p.fillPoly(edge,ink(0xFF324F3B,fade*.72f));
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
            float width=L.w*(.025f+.18f*d),x=L.w*(.5f+bend(c.skyClock,waterDepth(L,y))+.025f*(float)Math.sin(i*2.7f+time*.16f));
            float a=fade*(.07f+.05f*d)*(float)Math.sin(t*Math.PI);
            p.fillPoly(new float[]{x-width,y,x+width*.8f,y-L.w*.001f,x+width*.48f,y+L.w*(.002f+d*.004f),x-width*.74f,y+L.w*(.003f+d*.004f)},ink(0xFF96B19A,a));
            for(int side=-1;side<=1;side+=2) {
                float xx=L.w*(.5f+bend(c.skyClock,waterDepth(L,y))+side*(.10f+d*.25f));
                p.line(xx-width*.65f,y+L.w*.006f,xx+width*.65f,y+L.w*.006f,ink(0xFF6A9A8D,fade*(.025f+d*.045f)),L.w*(.001f+d*.001f));
            }
        }
    }
    private static int shade(int color,float clarity,float fade){return ink(Glyph.mix(0xFF758579,color,clarity),fade);}
    private static void tree(Painter p,float x,float y,float r,float h,int seed,float time,float clarity,float fade) {
        float lean=(hash(seed+5)-.5f)*r*1.3f,top=x+lean,curve=(hash(seed+19)-.5f)*r*.65f;
        int bark=shade(0xFF354C3D,clarity,fade*.90f),dark=shade(0xFF1E3B31,clarity,fade*.85f),light=shade(0xFF79AD91,clarity,fade*.65f);
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
            float side=i%2==0?-1:1,u=.28f+i*.13f,bx=x+lean*u+curve*(float)Math.sin(u*Math.PI),by=y-h*u;
            float tip=top+side*r*(1.75f+hash(seed+i*29)*1.2f),ty=y-h*(.62f+i*.085f+hash(seed+i*31)*.06f);
            limb(p,bx,by,bx+side*r*1.35f,by-h*.08f,tip,ty,r*(.17f-i*.025f),bark,light);
            if(clarity>.45f)p.polyline(new float[]{bx+side*r*.75f,by-h*.09f,tip-side*r*.25f,ty+h*.12f,tip+side*r*.55f,ty+h*.09f},bark,r*.045f);
            if(i<2)crown(p,tip,ty,r*.75f,seed+i*83,shade(0xFF3B6654,clarity,fade*.82f),clarity);
        }
        for(int i=0;i<6;i++) {
            float side=i%2==0?-1:1,reach=r*(1.35f+hash(seed+i*41)*1.4f);
            float end=x+side*reach,foot=y+r*(.025f+hash(seed+i*43)*.16f);
            float ax=x+side*r*.10f,ay=y-h*(.13f+hash(seed+i*47)*.10f),bx=x+side*reach*.72f,by=y-h*.13f;
            limb(p,ax,ay,bx,by,end,foot,r*.15f,dark,light);
            if(i%2==0 && clarity>.45f) {
                float u=.58f,v=1-u,fx=v*v*ax+2*v*u*bx+u*u*end,fy=v*v*ay+2*v*u*by+u*u*foot;
                p.polyline(new float[]{fx,fy,fx+side*r*.36f,fy+r*.28f,end+side*r*.42f,foot+r*.07f},bark,r*.055f);
            }
        }
        if(clarity>.55f) {
            // Small luminous shelf fungi tie the coloured glow to the living scenery.
            int color=MAGIC[(seed&0x7fffffff)%MAGIC.length];
            for(int i=0;i<3;i++) {
                float u=.14f+i*.085f,xx=x+lean*u+curve*(float)Math.sin(u*Math.PI)+(i%2==0?-1:1)*r*.16f;
                float yy=y-h*u,cap=r*(.12f+hash(seed+i*71)*.06f);
                float luma=fade*(.65f+.15f*(float)Math.sin(time*.6f+seed+i));
                glow(p,xx,yy,cap*1.2f,cap*.65f,color,luma*.65f);
                p.line(xx,yy,xx,yy+cap*.65f,shade(0xFF9FBCA8,clarity,fade*.65f),cap*.22f);
                p.fillEllipse(xx,yy,cap,cap*.40f,ink(color,luma*.70f));
                p.fillEllipse(xx,yy-cap*.09f,cap*.65f,cap*.13f,ink(0xFFE1FFE7,luma*.43f));
            }
        }
        float sway=(float)Math.sin(time*.38f+seed)*r*.018f;
        for(int i=0;i<3;i++) {
            float cx=top+(i-1)*r*1.35f+sway,cy=y-h+r*(.06f+hash(seed+i*53)*.45f);
            crown(p,cx,cy,r*(1.18f+hash(seed+i*61)*.35f),seed+i*59,shade(i%2==0?0xFF315B4B:0xFF45694E,clarity,fade*.90f),clarity);
        }
    }
    private static void limb(Painter p,float ax,float ay,float bx,float by,float cx,float cy,float width,int bark,int light) {
        float[] shape=new float[28],shine=new float[14];
        for(int i=0;i<7;i++) {
            float u=i/6f,v=1-u,x=v*v*ax+2*v*u*bx+u*u*cx,y=v*v*ay+2*v*u*by+u*u*cy;
            float dx=v*(bx-ax)+u*(cx-bx),dy=v*(by-ay)+u*(cy-by),length=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy));
            float nx=-dy/length,ny=dx/length,w=width*(1-u*.80f);
            shape[i*2]=x+nx*w;shape[i*2+1]=y+ny*w;
            shape[(13-i)*2]=x-nx*w;shape[(13-i)*2+1]=y-ny*w;
            shine[i*2]=x+nx*w*.45f;shine[i*2+1]=y+ny*w*.45f;
        }
        p.fillPoly(shape,bark);p.polyline(shine,light,width*.16f);
    }
    private static void crown(Painter p,float x,float y,float r,int seed,int color,float clarity) {
        // Scalloped leaf masses overlap a dark underside, with light catching the upper lobes.
        float[] shape=new float[128];float phase=hash(seed)*Softbody.TAU;
        int layers=clarity>.45f?4:1;
        for(int layer=0;layer<layers;layer++) {
            float size=layer==0?1:layer==1?.69f:layer==2?.64f:.48f;
            float cx=x+r*(layer==0?0:layer==1?-.35f:layer==2?.32f:-.12f);
            float cy=y-r*(layer==0?0:layer==1?.10f:layer==2?.08f:.27f);
            for(int i=0;i<64;i++) {
                float a=i*Softbody.TAU/64;
                float lobes=.88f+.07f*(float)Math.sin(a*7+phase+layer)+.045f*(float)Math.sin(a*11-phase-layer);
                float reach=r*size*lobes;
                shape[i*2]=cx+(float)Math.cos(a)*reach;
                shape[i*2+1]=cy+(float)Math.sin(a)*reach*(layer==0?.57f:.72f);
            }
            int tone=layer==0?Glyph.mix(color,0xFF183C36,clarity*.28f):layer==1?color:Glyph.mix(color,0xFF83A97A,clarity*(layer==2?.09f:.23f));
            p.fillPoly(shape,tone);
        }
        if(clarity>.65f)for(int i=0;i<2;i++) {
            float xx=x+r*(i==0?-.28f:.25f),yy=y-r*(i==0?.19f:.06f);
            float side=i==0?-1:1;
            p.fillPoly(new float[]{xx-r*.18f,yy+r*.025f,xx-r*.10f,yy-r*.06f,xx+r*.04f,yy-r*.045f,
                    xx+r*.20f,yy+side*r*.045f,xx+r*.04f,yy+r*.055f,xx-r*.10f,yy+r*.07f},
                    Glyph.mix(color,0xFFA3BC81,clarity*.20f));
        }
    }
    private static void grass(Painter p,float x,float y,float r,int seed,float time,float clarity,float fade) {
        p.fillEllipse(x,y,r*1.1f,r*.16f,shade(0xFF294A34,clarity,fade*.40f));
        float[] blade=new float[8];
        int count=clarity<.3f?6:clarity<.55f?10:18;
        for(int i=0;i<count;i++) {
            int n=seed+i*31;float bx=x+(hash(n)-.5f)*r*1.9f,by=y+(hash(n+3)-.5f)*r*.13f;
            float h=r*(.48f+hash(n+5)*1.20f),lean=(hash(n+7)-.5f)*r*.48f+(float)Math.sin(time*.75f+n)*r*.035f;
            float w=r*(.030f+hash(n+9)*.040f),tip=bx+lean;
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
