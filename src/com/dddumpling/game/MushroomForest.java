package com.dddumpling.game;

/** Muted undergrowth and an overhead canopy; all motion follows the scenery clock. */
final class MushroomForest extends Draw {
    static final int LAND=3;
    static final float REVEAL_TIME=.9f, LEAF_TIME=4.5f;
    private static final int[] GREENS={0xFF71917A,0xFF91A486,0xFF587D70,0xFFACB394};
    private MushroomForest() {}

    static boolean retained(GameCore c) {
        return c.state==GameCore.OVER && c.bossVictoryKind==Boss.MUSHROOM && c.bossVictory!=null;
    }
    private static Boss sceneBoss(GameCore c) {return retained(c)?c.bossVictory:c.boss;}
    static float groveProgress(GameCore c) {
        Boss b=sceneBoss(c);
        if(!b.active() || b.kind!=Boss.MUSHROOM || c.survival.active || c.state==GameCore.TITLE)return 0;
        float t=Math.min(1f,(b.introDuration()-b.intro)/Boss.INTRO);
        return t*t*(3-2*t);
    }

    static float bossReveal(Boss b) {
        if(b.kind!=Boss.MUSHROOM || b.intro<=0)return 1;
        float t=Math.max(0,Math.min(1,(REVEAL_TIME-b.intro)/REVEAL_TIME));
        return t*t*(3-2*t);
    }
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float mix=Lands.blend(c);
        return Math.max(groveProgress(c),(c.landFrom==LAND?1-mix:0)
                +(Lands.forStage(Lands.visualStage(c))==LAND?mix:0));
    }

    static void mist(Painter p,GameCore c,Layout L,int from,int to) {
        if(groveProgress(c)>=1)return;
        float fade=presence(c)*(1-c.drained())*(1-groveProgress(c));
        if(fade<=0)return;
        for(int layer=from;layer<to;layer++)for(int i=0;i<4;i++) {
            int seed=1093+layer*77+i*41;
            float span=L.deckTop+L.h*.2f;
            float y=((hash(seed)*span+c.skyClock*L.h*(.009f+layer*.004f)+scroll(c,L)*.35f)%span)-L.h*.1f;
            float x=L.w*(.08f+hash(seed+3)*.84f),w=L.w*(.35f+hash(seed+7)*.28f);
            for(int veil=0;veil<3;veil++) {
                float[] ribbon=new float[66];
                for(int j=0;j<=16;j++) {
                    float u=j/16f,xx=x+(u-.5f)*w*2;
                    float yy=y+L.h*.016f*(float)Math.sin(u*5+c.clock*.12f+i);
                    float h=L.h*(.007f+veil*.006f)*(float)Math.sin(u*Math.PI);
                    ribbon[j*2]=ribbon[(32-j)*2]=xx;
                    ribbon[j*2+1]=yy-h;ribbon[(32-j)*2+1]=yy+h;
                }
                p.fillPoly(ribbon,Glyph.withAlpha(layer==2?0xFFB3C7AE:0xFF889F92,
                        (int)(fade*(layer==2?4:7))));
            }
        }
    }

    private static void floorTexture(Painter p,GameCore c,Layout L,float fade) {
        float[] shape=new float[66];
        for(int i=0;i<36;i++) {
            int seed=2671+i*59;
            float r=L.w*(.018f+hash(seed)*.035f),span=L.deckTop+r*8;
            float x=L.w*(.16f+hash(seed+3)*.68f);
            float y=(hash(seed+9)*span+c.skyClock*L.h*.016f+scroll(c,L))%span-r*4;
            litter(p,shape,x,y,r,seed,(int)(fade*(32+hash(seed+13)*26)));
        }
    }

    /** Traced curled leaves and broken fern fronds, with no oval ground stamps. */
    private static void litter(Painter p,float[] shape,float x,float y,float r,int seed,int a) {
        if(a<=0)return;
        if(p.stamps()!=null && p.stamps().forest(3+seed%3,x,y,r,hash(seed+21)*Softbody.TAU,0,a))return;
        float turn=hash(seed+21)*Softbody.TAU;
        float dx=(float)Math.cos(turn)*r,dy=(float)Math.sin(turn)*r*.6f;
        if(seed%3==0) {
            for(int j=0;j<3;j++)leaf(p,shape,x+dx*j*.24f,y+dy*j*.24f,
                    dx*(.7f+j*.13f),dy-r*(.25f+j*.15f),r*(.18f+j*.02f),
                    j==1?0xFFA69A71:0xFF7F956E,a);
        } else {
            p.polyline(new float[]{x,y,x+dx*.6f,y+dy*.6f-r*.12f,x+dx*1.6f,y+dy*1.6f},
                    Glyph.withAlpha(0xFF829478,a/2),r*.025f);
            for(int j=0;j<5;j++) {
                float t=j/5f,bx=x+dx*t*1.6f,by=y+dy*t*1.6f;
                for(int side=-1;side<=1;side+=2)leaf(p,shape,bx,by,
                        dx*.23f-dy*side*.45f,dy*.23f+dx*side*.45f,
                        r*.095f*(1-t*.4f),0xFF819B78,a);
            }
        }
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
        Boss b=sceneBoss(c);
        float progress=groveProgress(c),fade=progress*(retained(c)?1:1-c.drained());
        if(fade<=.001f || c.state==GameCore.TITLE)return;
        float floor=groveFloor(b,L);
        float lift=-(1-progress)*L.h*.72f;
        float[] shape=new float[66];
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
                float wind=(float)Math.sin(c.clock*.95f+branch*1.7f+side+layer)*.24f
                        +(float)Math.sin(c.clock*1.8f+branch)*.055f;
                float by=L.playTop+L.h*(.04f+branch*.12f),reach=-side*L.w*(.26f-branch*.055f)*(1+wind*.3f);
                p.polyline(new float[]{x,by+L.h*.12f,x+reach*.38f,by+L.h*.035f,x+reach,by-L.h*.035f},
                        Glyph.withAlpha(lit(0xFF8A7050,c.boss),a*2/3),w*.23f);
                sprig(p,shape,x+reach*.55f,by+L.h*.02f,L.w*(.17f-branch*.025f),-side*(.8f+wind),1,a/2,lit(GREENS[(branch+layer)%4],c.boss));
            }
            sprig(p,shape,x,base,L.w*.18f,-side*(.65f+.23f*(float)Math.sin(c.clock*1.1f+side+layer)),0,a/2,lit(GREENS[1],c.boss));
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

    static float groveFloor(Boss b,Layout L) {return b.baseY(L)+Boss.bodyR(L)*3.55f;}
    static float groundBankY(float floor,Layout L,float x,int layer) {
        float t=(x/L.w+.06f)/1.12f;
        return floor+L.h*(.065f-.052f*(float)Math.sin(t*Math.PI)
                +.007f*(float)Math.sin(t*17f+layer*.5f)+layer*.016f);
    }

    private static void ground(Painter p,GameCore c,Layout L,float floor,float fade) {
        // Uneven moss bank runs under the boss roots and down to the edge of the arena.
        for(int layer=0;layer<3;layer++) {
            float[] bank=new float[46];
            for(int j=0;j<=20;j++) {
                float t=j/20f;
                bank[j*2]=L.w*(-.06f+t*1.12f);
                bank[j*2+1]=groundBankY(floor,L,bank[j*2],layer);
            }
            bank[42]=L.w*1.06f;bank[43]=L.deckTop+L.h;
            bank[44]=-L.w*.06f;bank[45]=L.deckTop+L.h;
            int color=layer==0?0xFF617657:layer==1?0xFF3E5747:0xFF2A4039;
            p.fillPoly(bank,Glyph.withAlpha(lit(color,c.boss),(int)(fade*(layer==0?175:210))));
        }
        float[] shape=new float[66];
        for(int i=0;i<24;i++) {
            float x=L.w*hash(813+i*31),y=floor+L.h*(.09f+.19f*hash(211+i*71));
            litter(p,shape,x,y,L.w*(.016f+.014f*hash(i+42)),i*73+14,(int)(fade*35));
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
            float lean=(float)Math.sin(c.clock*(.75f+hash(i+75)*.35f)+i*2.1f)*r*.58f;
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
        fade*=1-groveProgress(c);
        if(fade<=0)return;
        floorTexture(p,c,L,fade*(1-groveProgress(c)*.6f));
        fade*=1-groveProgress(c)*.65f;
        float[] leaf=new float[66];
        for(int layer=0;layer<2;layer++)for(int i=0;i<10;i++) {
            int seed=901+i*53+layer*317;
            float r=L.w*(.13f+hash(seed)*.10f),side=i%2==0?1:-1;
            float x=side>0?L.w*(-.03f+hash(seed+1)*.11f):L.w*(1.03f-hash(seed+1)*.11f);
            float span=L.deckTop+r*5;
            float y=((i+hash(seed+2))*.1f*span+c.skyClock*L.h*(.012f+layer*.008f)+scroll(c,L))%span-r*2.5f;
            if(y<L.playTop-r*.6f || y-r*2.1f>L.deckTop)continue;
            float sway=(float)Math.sin(c.clock*.43f+i)*r*.10f;
            int alpha=(int)(fade*(layer==0?38:66));
            // Ferns alternate with rounded broad leaves and slim willow sprays.
            sprig(p,leaf,x,y,r,side*(.6f+sway/r),i%3,alpha,GREENS[(i+layer)%GREENS.length]);
        }
    }

    static void canopy(Painter p,GameCore c,Layout L) {
        float forest=presence(c),grove=groveProgress(c);
        if(forest<=.001f)return;
        float fade=forest*(retained(c)?1:1-c.drained());
        p.save();
        p.clipRect(0,L.playTop,L.w,L.deckTop);
        float[] shape=new float[66];
        // The travelling canopy leaves the scene; anchored branches take over at the grove.
        if(grove<1)for(int i=0;i<6;i++) {
            int seed=1703+i*137;
            float side=i%2==0?1:-1,r=L.w*(.42f+hash(seed)*.17f);
            float span=L.deckTop+r*4;
            float y=((i+hash(seed+3))/6f*span+c.skyClock*L.h*.034f+scroll(c,L)*1.35f)%span-r*2;
            if(y<L.playTop-r*.6f || y-r*2.1f>L.deckTop)continue;
            float x=side>0?-r*.12f:L.w+r*.12f;
            float sway=(float)Math.sin(c.clock*.65f+i*2.3f)*.23f;
            sprig(p,shape,x,y,r,side*(1.05f+sway),1,(int)(fade*(1-grove)*20),GREENS[i%4]);
        }
        if(grove>0)for(int i=0;i<2;i++) {
            float side=i%2==0?1:-1,r=L.w*(i<2?.43f:.32f);
            float x=side>0?-L.w*.08f:L.w*1.08f,y=L.playTop+L.h*(i<2?.29f:.64f);
            float sway=(float)Math.sin(c.clock*.8f+i*1.8f)*.23f;
            sprig(p,shape,x,y,r,side*(.86f+sway),1,(int)(fade*grove*12),GREENS[i]);
        }
        fallingLeaves(p,c,L,shape);
        p.restore();
    }

    private static void fallingLeaves(Painter p,GameCore c,Layout L,float[] shape) {
        Boss b=sceneBoss(c);
        if(b.kind!=Boss.MUSHROOM || b.mushroomLeafTime<0)return;
        float time=b.mushroomLeafTime+(retained(c)?c.time:0);
        if(time>=LEAF_TIME)return;
        int count=9+(int)((1-b.health())*9);
        for(int i=0;i<count;i++) {
            int seed=4589+i*97;
            float age=time-hash(seed)*.6f;
            if(age<0)continue;
            float side=i%2==0?1:-1;
            float x=L.w*(side>0?.08f+hash(seed+1)*.28f:.92f-hash(seed+1)*.28f);
            x+=L.w*((float)Math.sin(age*2.5f+i)*.055f+side*age*.026f);
            float y=L.playTop+L.h*(.03f+hash(seed+7)*.26f+age*.075f+age*age*.022f);
            float angle=i*2.4f+age*(1.5f+hash(seed+9)*2);
            float r=L.w*(.014f+hash(seed+11)*.012f);
            float dx=(float)Math.cos(angle)*r,dy=(float)Math.sin(angle)*r;
            float width=r*(.18f+.25f*Math.abs((float)Math.sin(age*4+i)));
            float alpha=Math.min(1,age*7)*Math.min(1,(LEAF_TIME-time)/.9f);
            leaf(p,shape,x,y,dx,dy,width,i%4==0?0xFFB3A171:GREENS[i%4],(int)(alpha*165));
        }
    }

    private static void sprig(Painter p,float[] shape,float x,float y,float r,float lean,int kind,int a,int color) {
        if(a<=0)return;
        if(p.stamps()!=null && p.stamps().forest(kind,x,y,r,lean,color,a))return;
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

    /** Immutable local art for optional host caching. The software painter prevents recursion. */
    static void stampArt(Painter p,int art,float r) {
        float[] shape=new float[66];
        if(art<3)sprig(p,shape,0,0,r,1,art,255,GREENS[0]);
        else {
            int seed=art-3;
            // Cancel the authored seed rotation; the cached draw supplies the instance rotation.
            litter(p,shape,0,0,r,seed,255);
        }
    }
    static float litterAngle(int art) {return hash(art-3+21)*Softbody.TAU;}

    /** One tapered contour, with a translucent half-leaf highlight instead of stacked ovals. */
    private static void leaf(Painter p,float[] shape,float x,float y,float dx,float dy,float width,int color,int a) {
        float length=(float)Math.sqrt(dx*dx+dy*dy),nx=-dy/length,ny=dx/length;
        for(int i=0;i<33;i++) {
            float t=i<=16?i/16f:(32-i)/16f;
            float bulge=(float)Math.sin(t*Math.PI)*width*(i<=16?1:-.82f)
                    *(1+.06f*(float)Math.sin(t*Math.PI*5));
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
