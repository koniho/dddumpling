package com.dddumpling.game;

/** Slime climbs over the playfield, then drains away to unveil Super Slime. */
final class SlimeArrival extends Draw {
    static final float DURATION=2.8f, COVER=.85f, RELEASE=1.12f, DRAIN=2.35f;
    private SlimeArrival() {}
    private static float smooth(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    static float elapsed(Boss b) {return DURATION-b.intro;}
    static float reveal(Boss b) {
        if(b.kind!=Boss.SLIME || b.intro<=0)return 1;
        return smooth((elapsed(b)-COVER)/.25f);
    }
    static float titleProgress(Boss b) {return Math.max(0,(elapsed(b)-1.55f)/(DURATION-1.55f));}
    static float surface(Boss b,Layout L) {
        float t=elapsed(b),low=L.deckTop+L.w*.23f,high=L.playTop-L.w*.23f;
        return t<RELEASE?low+(high-low)*smooth(t/COVER)
                :high+(low-high)*smooth((t-RELEASE)/(DRAIN-RELEASE));
    }
    static boolean active(GameCore c) {
        return c.state!=GameCore.TITLE && c.state!=GameCore.OVER && c.boss.active()
                && c.boss.kind==Boss.SLIME && c.boss.intro>0 && !c.boss.beaten;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!active(c))return;
        Boss b=c.boss;float t=elapsed(b),y=surface(b,L),width=L.w;
        float[] sheet=new float[102],edge=new float[98],blob=new float[34];
        p.save();p.clipRect(0,L.playTop,L.w,L.deckTop);
        if(t<DRAIN) {
            float falling=smooth((t-RELEASE)/.45f);
            for(int layer=0;layer<3;layer++) {
                for(int j=0;j<=48;j++) {
                    float u=j/48f,x=width*(-.05f+u*1.1f);
                    float ripple=(float)Math.sin(u*14+t*4)*width*.035f
                            +(float)Math.sin(u*27-t*3)*width*.018f;
                    // Long crests stretch upward as the heavy curtain falls away.
                    float crest=(float)Math.pow(.5f+.5f*Math.sin(u*31+1),6)*width*.20f*falling;
                    edge[j*2]=sheet[j*2]=x;
                    edge[j*2+1]=sheet[j*2+1]=y+ripple-crest+layer*width*.036f;
                }
                sheet[98]=width*1.05f;sheet[99]=L.deckTop+width;
                sheet[100]=-width*.05f;sheet[101]=L.deckTop+width;
                p.fillPoly(sheet,layer==0?SlimeHills.DEEP:layer==1?SlimeHills.GREEN:0xFF80CD89);
                p.polyline(edge,Glyph.withAlpha(SlimeHills.MINT,layer==1?155:55),width*.014f);
            }
            for(int i=0;i<9;i++) {
                float x=width*(.05f+i*.115f),by=y+width*(.15f+hash(i+79)*.7f);
                float r=width*(.013f+hash(i+52)*.019f);
                p.arc(x,by,r,r*1.15f,190,250,0x779FE9AF,width*.004f);
            }
        }
        // A finite spray rises from the reveal, then falls below the field with the sheet.
        float burst=(t-1.46f)/1.34f;
        if(burst>0 && burst<1) {
            float a=(1-burst)*(1-burst),cx=b.bodyX(L),cy=b.bodyY(L);
            for(int i=0;i<28;i++) {
                float angle=Softbody.TAU*i/28f+.09f*(float)Math.sin(i*7);
                float speed=width*(.35f+hash(i+317)*.55f);
                float x=cx+(float)Math.cos(angle)*speed*burst;
                float yy=cy+(float)Math.sin(angle)*speed*burst+width*burst*burst*.63f;
                float r=width*(.013f+hash(i+89)*.021f)*(1-burst*.35f);
                SlimeHills.jelly(p,blob,x,yy,r,r*(1+burst*1.7f),i,
                        Glyph.withAlpha(i%3==0?SlimeHills.MINT:SlimeHills.GREEN,(int)(210*a)));
                p.fillEllipse(x-r*.24f,yy-r*.35f,r*.26f,r*.43f,Glyph.withAlpha(INK,(int)(100*a)));
            }
        }
        p.restore();
    }
}
