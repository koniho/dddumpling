package com.dddumpling.game;

/** Muted undergrowth and an overhead canopy; all motion follows the scenery clock. */
final class MushroomForest extends Draw {
    static final int LAND=3;
    private static final int[] GREENS={0xFF71917A,0xFF91A486,0xFF587D70,0xFFACB394};
    private MushroomForest() {}

    static float groveProgress(GameCore c) {
        if(!c.boss.active() || c.boss.kind!=Boss.MUSHROOM || c.survival.active)return 0;
        float t=c.boss.introProgress();
        return t*t*(3-2*t);
    }

    static float light(Boss b) {
        if(!b.active() || b.kind!=Boss.MUSHROOM || b.intro>0)return 0;
        float pip=Math.min(1,b.mushroomSweepFlash/.28f);
        float spore=Math.min(1,b.launchT/Boss.LAUNCH_TIME);
        return Math.max(b.hurt*.95f,Math.max(pip*.30f,spore*spore*.50f));
    }
    private static int lightColor(Boss b) {
        if(b.hurt>0)return 0xFFFFD3A0;
        return b.launchT>0?0xFFC5E8C0:0xFFF0E5AF;
    }
    private static int lit(int color,Boss b) {return Glyph.mix(color,lightColor(b),light(b)*.65f);}

    // Looking upward sends the nearby foliage down, then settles into the clearing.
    private static float scroll(GameCore c,Layout L) {return groveProgress(c)*L.h*.72f;}

    static void grove(Painter p,GameCore c,Layout L) {
        float progress=groveProgress(c),fade=progress*(1-c.drained());
        if(fade<=.001f || c.state==GameCore.TITLE)return;
        float floor=c.boss.baseY(L)+Boss.bodyR(L)*3.55f;
        float lift=-(1-progress)*L.h*.72f;
        float[] shape=new float[34];
        p.save();p.clipRect(0,L.playTop,L.w,L.deckTop);p.translate(0,lift);
        // A softly lit clearing joins the boss's own living roots, without a hard horizon.
        for(int i=3;i>=0;i--)p.fillEllipse(L.w*.5f,floor,L.w*(.38f+i*.055f),L.h*(.035f+i*.018f),
                Glyph.withAlpha(0xFF9BAE86,(int)(fade*9)));
        for(int i=0;i<5;i++) {
            float x=L.w*(.13f+i*.18f),sway=(float)Math.sin(c.clock*.17f+i)*L.w*.025f;
            p.fillPoly(new float[]{x,L.playTop-L.h*.3f,x+L.w*.04f,L.playTop-L.h*.3f,
                    x+sway+L.w*.17f,floor,x+sway-L.w*.06f,floor},Glyph.withAlpha(0xFFCCD6A7,(int)(fade*8)));
        }
        ground(p,c,L,floor,fade);
        for(int layer=0;layer<2;layer++)for(int side=-1;side<=1;side+=2) {
            float x=L.w*(side<0?.08f:.92f),w=L.w*(layer==0?.055f:.09f);
            if(layer==0)x-=side*L.w*.13f;
            float base=floor+L.h*(layer==0?-.12f:.035f);
            float top=L.playTop-L.h*.30f,lean=-side*L.w*.11f;
            int a=(int)(fade*(layer==0?60:135));
            p.fillPoly(new float[]{x-w*1.8f,base,x-w*.68f,base-L.h*.14f,
                    x-w*.48f+lean,top,x+w*.47f+lean,top,x+w*.76f,base-L.h*.17f,x+w*1.9f,base},
                    Glyph.withAlpha(lit(layer==0?0xFF655F50:0xFF604D3C,c.boss),a));
            p.fillPoly(new float[]{x-w*.60f,base-L.h*.07f,x-w*.18f+lean,top,
                    x+w*.13f+lean,top,x+w*.20f,base-L.h*.13f},Glyph.withAlpha(lit(0xFFB29770,c.boss),a/3));
            for(int root=0;root<3;root++) {
                float reach=-side*L.w*(.11f+root*.065f);
                p.polyline(new float[]{x,base-L.h*.16f,x+reach*.24f,base-L.h*.025f,
                        x+reach,base+L.h*(.015f+root*.012f)},Glyph.withAlpha(0xFF88754F,a/2),w*(.20f-root*.045f));
            }
            for(int branch=0;branch<3;branch++) {
                float by=L.playTop+L.h*(.04f+branch*.12f),reach=-side*L.w*(.26f-branch*.055f);
                p.polyline(new float[]{x,by+L.h*.12f,x+reach*.38f,by+L.h*.035f,x+reach,by-L.h*.035f},
                        Glyph.withAlpha(lit(0xFF8A7050,c.boss),a*2/3),w*.23f);
                sprig(p,shape,x+reach*.55f,by+L.h*.02f,L.w*(.17f-branch*.025f),-side*.8f,1,a/2,lit(GREENS[(branch+layer)%4],c.boss));
            }
            sprig(p,shape,x,base,L.w*.18f,-side*.65f,0,a/2,lit(GREENS[1],c.boss));
        }
        groundProps(p,c,L,floor,fade);
        float light=light(c.boss);
        if(light>0) {
            int color=lightColor(c.boss);
            float cx=c.boss.bodyX(L),cy=c.boss.bodyY(L)+Boss.bodyR(L)*.65f;
            // Broad, cheap light pools illuminate the environment behind the boss and prompts.
            for(int glow=3;glow>=0;glow--) {
                p.fillEllipse(cx,cy,L.w*(.31f+glow*.095f),L.h*(.16f+glow*.055f),
                        Glyph.withAlpha(color,(int)(fade*light*14)));
                p.fillEllipse(cx,floor+L.h*.035f,L.w*(.23f+glow*.09f),L.h*(.025f+glow*.014f),
                        Glyph.withAlpha(color,(int)(fade*light*18)));
            }
            for(int side=-1;side<=1;side+=2)p.fillPoly(new float[]{cx,cy,
                    cx+side*L.w*.58f,L.playTop-L.h*.10f,cx+side*L.w*.33f,L.playTop-L.h*.10f},
                    Glyph.withAlpha(color,(int)(fade*light*16)));
        }
        // Warm spores drift around the clearing; the combat centre stays open.
        for(int i=0;i<16;i++) {
            float x=L.w*(.06f+hash(403+i*19)*.88f);
            float y=L.playTop+(floor-L.playTop)*hash(603+i*43)-(float)Math.sin(c.clock*.4f+i)*L.h*.025f;
            float glow=.5f+.5f*(float)Math.sin(c.clock*1.2f+i*2.1f);
            p.fillCircle(x,y,L.unit*.11f,Glyph.withAlpha(0xFFD9DEAE,(int)(fade*glow*14)));
            p.fillCircle(x,y,L.unit*.035f,Glyph.withAlpha(0xFFE6EAC2,(int)(fade*glow*100)));
        }
        p.restore();
    }

    private static void ground(Painter p,GameCore c,Layout L,float floor,float fade) {
        // Uneven moss bank runs under the boss roots and down to the edge of the arena.
        for(int layer=0;layer<3;layer++) {
            float[] bank=new float[46];
            for(int j=0;j<=20;j++) {
                float t=j/20f;
                bank[j*2]=L.w*(-.06f+t*1.12f);
                bank[j*2+1]=floor+L.h*(.065f-.052f*(float)Math.sin(t*Math.PI)
                        +.007f*(float)Math.sin(t*17f+layer*.5f)+layer*.016f);
            }
            bank[42]=L.w*1.06f;bank[43]=L.deckTop+L.h;
            bank[44]=-L.w*.06f;bank[45]=L.deckTop+L.h;
            int color=layer==0?0xFF617657:layer==1?0xFF3E5747:0xFF2A4039;
            p.fillPoly(bank,Glyph.withAlpha(lit(color,c.boss),(int)(fade*(layer==0?175:210))));
        }
        for(int i=0;i<24;i++) {
            float x=L.w*hash(813+i*31),y=floor+L.h*(.09f+.19f*hash(211+i*71));
            p.fillEllipse(x,y,L.w*(.012f+.018f*hash(i+42)),L.h*.0025f,
                    Glyph.withAlpha(0xFF91A777,(int)(fade*(12+hash(i+19)*12))));
        }
    }

    private static void groundProps(Painter p,GameCore c,Layout L,float floor,float fade) {
        for(int i=0;i<8;i++) {
            float side=i%2==0?1:-1;
            float x=L.w*(side>0?.04f+hash(i*43+9)*.23f:.96f-hash(i*43+9)*.23f);
            float y=floor+L.h*(.055f+hash(i*79+17)*.095f);
            float r=L.w*(.022f+hash(i*37+6)*.035f);
            p.fillEllipse(x,y+r*.20f,r*1.3f,r*.25f,Glyph.withAlpha(0xFF142B25,(int)(fade*80)));
            p.fillPoly(new float[]{x-r,y,x-r*.89f,y-r*.55f,x-r*.4f,y-r*.88f,
                    x+r*.42f,y-r*.78f,x+r*.87f,y-r*.37f,x+r,y+r*.04f,x+r*.2f,y+r*.2f},
                    Glyph.withAlpha(lit(0xFF6C7B70,c.boss),(int)(fade*190)));
            p.fillPoly(new float[]{x-r*.89f,y-r*.55f,x-r*.4f,y-r*.88f,
                    x+r*.42f,y-r*.78f,x+r*.14f,y-r*.38f,x-r*.55f,y-r*.29f},
                    Glyph.withAlpha(0xFFA5AD90,(int)(fade*85)));
            p.fillEllipse(x-r*.18f,y-r*.63f,r*.53f,r*.16f,Glyph.withAlpha(0xFF829869,(int)(fade*150)));
        }
        for(int i=0;i<10;i++) {
            float side=i%2==0?1:-1;
            float x=L.w*(side>0?.025f+hash(i*47+61)*.25f:.975f-hash(i*47+61)*.25f);
            float y=floor+L.h*(.055f+hash(i*83+7)*.105f);
            float r=L.w*(.018f+hash(i*29+14)*.023f);
            float lean=(float)Math.sin(c.clock*(.75f+hash(i+75)*.35f)+i*2.1f)*r*.25f;
            float h=r*(1.1f+hash(i+91)*.8f),tipX=x+lean,tipY=y-h;
            p.fillEllipse(x,y+r*.06f,r*.85f,r*.2f,Glyph.withAlpha(0xFF162F27,(int)(fade*65)));
            p.polyline(new float[]{x,y,x+lean*.3f,y-h*.52f,tipX,tipY},
                    Glyph.withAlpha(0xFFC1C6A4,(int)(fade*180)),r*.28f);
            p.line(x-r*.035f,y-r*.08f,tipX-r*.035f,tipY,Glyph.withAlpha(0xFFE0DDC1,(int)(fade*100)),r*.075f);
            int color=i%3==0?0xFFAB8C7C:i%3==1?0xFF9DA078:0xFFBBABA0;
            Lands.mushroomCap(p,tipX,tipY,r,r*(.35f+hash(i+11)*.37f),c.clock+i,
                    Glyph.withAlpha(lit(color,c.boss),(int)(fade*200)),Glyph.withAlpha(0xFFF0E4C5,(int)(fade*140)));
        }
    }

    static void undergrowth(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-groveProgress(c)*.65f;
        float[] leaf=new float[34];
        for(int layer=0;layer<2;layer++)for(int i=0;i<10;i++) {
            int seed=901+i*53+layer*317;
            float r=L.w*(.13f+hash(seed)*.10f),side=i%2==0?1:-1;
            float x=side>0?L.w*(-.03f+hash(seed+1)*.11f):L.w*(1.03f-hash(seed+1)*.11f);
            float span=L.deckTop+r*5;
            float y=((i+hash(seed+2))*.1f*span+c.skyClock*L.h*(.012f+layer*.008f)+scroll(c,L))%span-r*2.5f;
            float sway=(float)Math.sin(c.clock*.43f+i)*r*.10f;
            int alpha=(int)(fade*(layer==0?38:66));
            // Ferns alternate with rounded broad leaves and slim willow sprays.
            sprig(p,leaf,x,y,r,side*(.6f+sway/r),i%3,alpha,GREENS[(i+layer)%GREENS.length]);
        }
    }

    static void canopy(Painter p,GameCore c,Layout L) {
        if(c.survival.active || c.state==GameCore.TITLE)return;
        float mix=Lands.blend(c);
        float fade=((c.landFrom==LAND?1-mix:0)+(Lands.forStage(Lands.visualStage(c))==LAND?mix:0))*(1-c.drained());
        fade=Math.max(fade,groveProgress(c)*(1-c.drained()));
        if(fade<=.001f)return;
        p.save();
        float margin=Renderer.shakeMargin(c,L);
        p.clipRect(-margin,L.playTop-margin,L.w+margin,L.deckTop);
        float[] leaf=new float[34];
        for(int i=0;i<9;i++) {
            int seed=1703+i*137;
            float side=i%2==0?1:-1;
            float r=L.w*(.23f+hash(seed)*.12f);
            float span=L.deckTop+r*4;
            float y=((i+hash(seed+3))/9f*span+c.skyClock*L.h*.034f+scroll(c,L)*1.35f)%span-r*2;
            float x=side>0?-r*.12f:L.w+r*.12f;
            float sway=(float)Math.sin(c.clock*.36f+i*2.3f)*.16f;
            sprig(p,leaf,x,y,r,side*(.95f+sway),i%3==0?2:1,(int)(fade*(31-14*groveProgress(c))),GREENS[i%4]);
        }
        p.restore();
    }

    private static void sprig(Painter p,float[] shape,float x,float y,float r,float lean,int kind,int a,int color) {
        int count=kind==0?6:3;
        float tipX=x+r*lean,tipY=y-r*1.45f;
        float bend=r*.16f;
        p.polyline(new float[]{x,y,x+r*lean*.35f,y-r*.65f,tipX,tipY},
                Glyph.withAlpha(0xFF91A58A,a/2),r*.018f);
        for(int j=0;j<count;j++) {
            float t=(j+.45f)/(count+.3f);
            float bx=x+(tipX-x)*t-bend*(float)Math.sin(t*Math.PI);
            float by=y+(tipY-y)*t;
            float size=r*(kind==0?.38f:.64f)*(1-t*.48f);
            for(int side=-1;side<=1;side+=2) {
                float dx=side*size*(kind==2?.57f:.91f)+lean*size*.32f;
                float dy=-size*(kind==2?1.02f:.55f);
                leaf(p,shape,bx,by,dx,dy,size*(kind==0?.17f:kind==1?.38f:.19f),color,a);
            }
        }
        leaf(p,shape,tipX-r*lean*.16f,tipY+r*.20f,r*lean*.27f,-r*.42f,r*.14f,color,a);
    }

    /** One tapered contour, with a translucent half-leaf highlight instead of stacked ovals. */
    private static void leaf(Painter p,float[] shape,float x,float y,float dx,float dy,float width,int color,int a) {
        float length=(float)Math.sqrt(dx*dx+dy*dy),nx=-dy/length,ny=dx/length;
        for(int i=0;i<17;i++) {
            float t=i<=8?i/8f:(16-i)/8f;
            float bulge=(float)Math.sin(t*Math.PI)*width*(i<=8?1:-.82f);
            shape[i*2]=x+dx*t+nx*bulge;
            shape[i*2+1]=y+dy*t+ny*bulge;
        }
        p.fillPoly(shape,Glyph.withAlpha(color,a));
        p.fillPoly(new float[]{x,y,x+dx*.40f+nx*width*.65f,y+dy*.40f+ny*width*.65f,
                x+dx*.78f+nx*width*.40f,y+dy*.78f+ny*width*.40f,x+dx,y+dy},
                Glyph.withAlpha(0xFFD0D8B2,a/3));
        p.line(x,y,x+dx*.86f,y+dy*.86f,Glyph.withAlpha(0xFFBDD0AB,a/3),width*.035f);
    }
}
