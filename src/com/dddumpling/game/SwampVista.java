package com.dddumpling.game;

/** Quiet grass banks and fireflies, projected from the same moving water-level landscape. */
final class SwampVista extends Draw {
    static final int ROWS=32,LANES=10, DRAW_ROWS=ROWS+ROWS*2/5;
    static final float SPEED=.030f;
    private static final float[] BANK_OFFSETS={.10f,.24f,.38f,.52f,.68f,.92f,1.28f,1.82f,2.63f,3.845f};
    private SwampVista() {}
    static float depth(float t){return t/(3-2*t);}
    static float scale(float t){return .14f+depth(t)*1.55f;}
    static float horizon(Layout L){return L.h*.43f;}
    static float horizonAt(Layout L,float x) {
        float u=Math.max(-1,Math.min(1,x/L.w*2-1));return horizon(L)+L.h*.014f*u*u;
    }
    static float rootY(Layout L,float t,float x){return rootY(L,t)+(horizonAt(L,x)-horizon(L))*Math.max(0,1-depth(t)*2);}
    static float rootY(Layout L,float t){return horizon(L)+depth(t)*(L.deckTop-horizon(L)+L.h*.35f);}
    static float waterDepth(Layout L,float y){return Math.max(0,(y-horizon(L))/(L.deckTop-horizon(L)+L.h*.35f));}
    static float bend(float clock,float d) {
        d=Math.max(0,Math.min(1,d));float t=3*d/(1+2*d);
        return .19f*(float)Math.sin(Math.PI*Math.sqrt(d))*(float)Math.sin(t*12-clock*SPEED*12+.6f);
    }
    private static float phase(float clock,int side,int lane){return clock*SPEED*ROWS+(side>0?.43f:0)+lane*.37f;}
    static float travel(float clock,int row,int side){return travel(clock,row,side,0);}
    static float travel(float clock,int row,int side,int lane){float p=phase(clock,side,lane);return (row+p-(float)Math.floor(p))/ROWS;}
    static int grassSeed(float clock,int row,int side,int lane){return 4391+(row-(int)Math.floor(phase(clock,side,lane)))*137+side*53+lane*701;}
    static float shoreX(Layout L,float clock,float d,int side) {
        float v=d*(L.deckTop-horizon(L)+L.h*.35f)/(L.deckTop-horizon(L));
        float material=3*d/(1+2*d)-clock*SPEED;
        float fringe=.010f*(.14f+d*1.55f)*((float)Math.sin(material*24+side)*.65f+(float)Math.sin(material*47+side*3)*.35f);
        return .5f+bend(clock,d)+side*(.065f+.28f*(float)Math.sqrt(Math.max(0,v))+fringe);
    }
    // Recede the soil through the nearest clump without moving its planted roots.
    static float waterEdgeX(Layout L,float clock,float d,int side) {
        return shoreX(L,clock,d,side)+side*.106f*(.14f+d*1.55f);
    }
    static float presence(GameCore c) {
        if(c.survival.active || SurvivalEntrance.active(c) || c.state==GameCore.TITLE)return 0;
        float mix=Lands.blend(c);
        return ((c.landFrom==Lands.SWAMP?1-mix:0)+(Lands.forStage(Lands.visualStage(c))==Lands.SWAMP?mix:0))*(1-arena(c));
    }
    static float bankOffset(int lane) {
        return BANK_OFFSETS[lane];
    }
    static float grassX(Layout L,float clock,float t,int side,int lane,int seed) {
        return shoreX(L,clock,depth(t),side)+side*(bankOffset(lane)+hash(seed+7)*.012f)*scale(t);
    }
    static float arena(GameCore c) {
        if(BossCollect.keepsArena(c,Boss.ALLIGATOR))return 1;
        Boss b=BossVictory.retained(c) && c.bossVictoryKind==Boss.ALLIGATOR?c.bossVictory:c.boss;
        if(!b.active() || b.kind!=Boss.ALLIGATOR)return 0;
        float t=Math.max(0,Math.min(1,(b.introDuration()-b.intro)/.9f));return t*t*(3-2*t);
    }
    private static int ink(int color,float fade){return Glyph.withAlpha(color,(int)(255*Math.max(0,Math.min(1,fade))));}
    private static float reveal(float t){float u=Math.min(1,t/.16f);return u*u*(3-2*u);}
    static void draw(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-BossVictory.backdropDrain(c);if(fade<=.001f)return;
        float time=c.cloudUi.reduceMotion?0:c.skyClock;
        c.swampGeometry.prepare(c,L);
        if(!SwampLayers.draw(p,SwampLayers.SKY,L,0,fade)) {backdrop(p,L,fade);nightSky(p,L,fade);}
        horizonTrees(p,c,L,fade);
        banks(p,c,L,fade);
        riverTint(p,c,L,fade);
        underwaterLife(p,c,L,fade);
        riverHaze(p,c,L,fade);
        if(!SwampLayers.draw(p,SwampLayers.HAZE,L,0,fade))horizonHaze(p,L,fade);
        horizonGrass(p,c,L,time,fade);
        ripples(p,c,L,fade);
        flotsam(p,c,L,fade);
        // Keep tall trailing tufts until their tips leave view; world seeds survive row rollover.
        for(int row=0;row<DRAW_ROWS;row++)for(int side=-1;side<=1;side+=2)for(int lane=LANES-1;lane>=0;lane--) {
            int seed=grassSeed(c.skyClock,row,side,lane);
            float t=travel(c.skyClock,row,side,lane),d=depth(t),s=scale(t);
            float x=L.w*grassX(L,c.skyClock,t,side,lane,seed),y=rootY(L,t,x);
            float r=L.w*SwampGrass.RADIUS*s*(.8f+hash(seed)*.25f);
            if(x+r*1.4f<0 || x-r*1.4f>L.w || y-r*SwampGrass.TOP>L.h)continue;
            float opacity=fade*reveal(t);if(opacity<.008f)continue;
            SwampGrass.draw(p,x,y,r,seed,time,.25f+.75f*Math.min(1,d*2.5f),opacity);
        }
        shoreReeds(p,c,L,time,fade);
        submergedEdges(p,c,L,fade);
        LandBonusArt.inSwamp(p,c,L,fade);
        c.swampFireflies.draw(p,c,L,fade);
    }
    static void backdrop(Painter p,Layout L,float fade) {
        // A single ramp avoids antialias seams between adjacent translucent rectangles.
        final int half=32;int[] colors=new int[half*2+3];float[] stops=new float[colors.length];
        float top=-L.h*.03f,range=L.h-top;
        colors[0]=ink(0xFF142E3B,fade);stops[0]=0;
        for(int i=0;i<=half;i++) {
            float u=i/(float)half;
            colors[i+1]=ink(Glyph.mix(0xFF142E3B,0xFF657C68,(float)Math.pow(u,2.2)),fade);
            stops[i+1]=(horizon(L)*u-top)/range;
        }
        for(int i=1;i<=half;i++) {
            float u=i/(float)half;int index=half+1+i;
            colors[index]=ink(Glyph.mix(0xFF657C68,0xFF153E43,(float)(1-Math.exp(-3.7f*u))),fade);
            stops[index]=(horizon(L)+(L.deckTop-horizon(L))*u-top)/range;
        }
        colors[colors.length-1]=colors[colors.length-2];stops[stops.length-1]=1;
        p.verticalGradient(-L.w*.06f,top,L.w*1.06f,L.h,colors,stops);
    }
    static void nightSky(Painter p,Layout L,float fade) {
        for(int i=0;i<18;i++) {
            int seed=7301+i*193;
            float x=L.w*(.05f+.90f*hash(seed)),y=L.h*(.025f+.26f*hash(seed+7));
            float r=L.w*(.0012f+.0012f*hash(seed+13));
            p.fillCircle(x,y,r,ink(0xFFD6E2CF,fade*(.20f+.32f*hash(seed+19))));
        }
        float x=L.w*.76f,y=L.h*.19f,r=L.w*.041f;
        // Thin, soft halo layers keep the moon hazy without clouding the open sky.
        for(int i=5;i>=1;i--)p.fillCircle(x,y,r*(1+i*.20f),ink(0xFFD0DFC1,fade*.025f));
        p.fillCircle(x,y,r,ink(0xFFD9E3C8,fade*.48f));
        p.fillCircle(x-r*.14f,y-r*.12f,r*.76f,ink(0xFFE1E8D1,fade*.10f));
    }
    static void horizonTrees(Painter p,GameCore c,Layout L,float fade) {
        float parallax=c.cloudUi.reduceMotion?0:L.w*.006f*(float)Math.sin(c.skyClock*SPEED*12);
        for(int layer=0;layer<3;layer++) {
            float shift=parallax*(.4f+layer*.3f);
            if(!SwampLayers.draw(p,SwampLayers.TREES+layer,L,shift,fade))treeLayer(p,L,layer,shift,fade);
        }
    }
    static void treeLayer(Painter p,Layout L,int layer,float shift,float fade) {
        float[] tree=new float[78];
        for(int side=-1;side<=1;side+=2)for(int i=0;i<6;i++) {
            int seed=919+layer*317+i*67+side*149;
            float u=i/5f,spread=(float)Math.sqrt(u);
            float d=.003f+layer*.009f+hash(seed)*.008f+u*.006f;
            float h=L.h*(.022f+.060f*spread+layer*.009f+hash(seed+3)*.015f+.016f*(1-u));
            float r=L.w*(.018f+.050f*spread+layer*.008f+hash(seed+5)*.012f+.005f*(1-u));
            // Anchor outside the widest bend; a few pixels of layered parallax keep the distance alive.
            float bankReach=Math.abs(waterEdgeX(L,0,d,side)-.5f-bend(0,d))
                    +.19f*(float)Math.sin(Math.PI*Math.sqrt(d))+.020f*(.14f+d*1.55f);
            float bank=L.w*(.5f+side*bankReach)+side*(r*1.1f+L.w*.025f);
            float outer=side<0?-L.w*.045f:L.w*1.045f,x=bank+(outer-bank)*u+shift;
            float root=bankY(L,d,x)+L.h*.002f*hash(seed+9),rootH=Math.min(L.h*.016f,h*.20f);
            float lean=L.w*(hash(seed+7)-.5f)*.018f;
            int base=layer==0?0xFF3F5D52:layer==1?0xFF304E43:0xFF264338;
            int color=ink(Glyph.mix(base,0xFF657C68,(1-u)*.16f+(2-layer)*.04f),fade);
            // Each bank recedes into smaller, paler trees; every root stays outside the channel.
            tree[0]=x-r*.50f;tree[1]=root;tree[2]=x-r*.12f;tree[3]=root-rootH;
            for(int n=0;n<32;n++) {
                float angle=Softbody.TAU*.25f+.14f+n*(Softbody.TAU-.28f)/31;
                float reach=r*(.90f+.07f*(float)Math.sin(angle*5+seed)+.03f*(float)Math.sin(angle*9-seed));
                tree[(n+2)*2]=x+lean+(float)Math.cos(angle)*reach;
                tree[(n+2)*2+1]=root-h+(float)Math.sin(angle)*reach*.42f;
            }
            tree[68]=x+r*.13f;tree[69]=root-rootH;tree[70]=x+r*.55f;tree[71]=root;
            tree[72]=x+r*.28f;tree[73]=root-rootH*.5f;tree[74]=x;tree[75]=root-rootH*.65f;
            tree[76]=x-r*.25f;tree[77]=root-rootH*.5f;
            p.fillPoly(tree,color);
        }
    }
    static float bankY(Layout L,float d,float x) {
        return horizon(L)+d*(L.deckTop-horizon(L)+L.h*.35f)+(horizonAt(L,x)-horizon(L))*Math.max(0,1-d*2);
    }
    private static void banks(Painter p,GameCore c,Layout L,float fade) {
        SwampGeometry g=c.swampGeometry;float[] ds=g.depth,core=g.core,strip=g.strip;
        for(int side=-1;side<=1;side+=2) {
            float outer=side<0?-L.w*.06f:L.w*1.06f;
            int slot=(side+1)/2;float[] xs=g.edge[slot],widths=g.bankWidth[slot];
            float top=xs[0]+side*widths[0]*2f;
            for(int i=0;i<=10;i++){float x=outer+(top-outer)*i/10f;core[i*2]=x;core[i*2+1]=horizonAt(L,x);}
            for(int i=1;i<=48;i++) {
                float x=xs[i]+side*widths[i]*2f;core[(i+10)*2]=x;core[(i+10)*2+1]=bankY(L,ds[i],x);
            }
            core[118]=outer;core[119]=L.h;p.fillPoly(core,ink(0xFF324F3B,fade*.65f));
            // Feather the wet bank into the water with a smooth, shared-color falloff.
            for(int band=0;band<8;band++) {
                float inner=-1.6f+band*.45f,outerEdge=inner+.45f;
                float u=(band+.5f)/8,softness=u*u*(3-2*u);
                for(int i=0;i<=48;i++) {
                    float x=xs[i]+side*widths[i]*inner,xx=xs[i]+side*widths[i]*outerEdge;
                    strip[i*2]=x;strip[i*2+1]=bankY(L,ds[i],x);
                    strip[(97-i)*2]=xx;strip[(97-i)*2+1]=bankY(L,ds[i],xx);
                }
                p.fillPoly(strip,ink(Glyph.mix(0xFF285348,0xFF324F3B,softness),fade*.65f*softness));
            }
        }
    }
    private static void riverTint(Painter p,GameCore c,Layout L,float fade) {
        float[] strip=c.swampGeometry.quad;
        for(int band=0;band<12;band++) {
            float near=band/12f*.28f,far=(band+1)/12f*.28f,u=(band+.5f)/12;
            float left=L.w*waterEdgeX(L,c.skyClock,near,-1),right=L.w*waterEdgeX(L,c.skyClock,near,1);
            float nextLeft=L.w*waterEdgeX(L,c.skyClock,far,-1),nextRight=L.w*waterEdgeX(L,c.skyClock,far,1);
            strip[0]=left;strip[1]=bankY(L,near,left);strip[2]=right;strip[3]=bankY(L,near,right);
            strip[4]=nextRight;strip[5]=bankY(L,far,nextRight);strip[6]=nextLeft;strip[7]=bankY(L,far,nextLeft);
            p.fillPoly(strip,ink(0xFF3F7784,fade*.62f*(1-u)*(1-u)));
        }
    }
    private static void submergedEdges(Painter p,GameCore c,Layout L,float fade) {
        SwampGeometry g=c.swampGeometry;float[] strip=g.strip;
        for(int side=-1;side<=1;side+=2) {
            int slot=(side+1)/2;float[] xs=g.edge[slot],ys=g.y[slot],widths=g.waterWidth[slot];
            // Water is painted over the reeds, tinting their bases through a soft translucent edge.
            for(int band=0;band<8;band++) {
                // Reach across the full root footprint, biased slightly toward the water.
                float inner=-3.5f+band*.75f,outer=inner+.75f,u=(band+.5f)/8;
                for(int i=0;i<=48;i++) {
                    strip[i*2]=xs[i]+side*widths[i]*inner;strip[i*2+1]=ys[i];
                    strip[(97-i)*2]=xs[i]+side*widths[i]*outer;strip[(97-i)*2+1]=ys[i];
                }
                float softness=(float)Math.sin(u*Math.PI);
                p.fillPoly(strip,ink(0xFF487E83,fade*.38f*softness*softness));
            }
        }
    }
    private static void shoreReeds(Painter p,GameCore c,Layout L,float time,float fade) {
        for(int side=-1;side<=1;side+=2) {
            float phase=c.skyClock*SPEED*6+(side>0?.5f:0);
            for(int i=0;i<6;i++) {
                int seed=8923+(i-(int)Math.floor(phase))*191+side*73;
                float t=(i+phase-(float)Math.floor(phase))/6,d=depth(t),r=L.w*(.003f+.011f*d);
                float x=L.w*(waterEdgeX(L,c.skyClock,d,side)+side*.003f*scale(t)),y=rootY(L,t,x),a=fade*reveal(t);
                if(x+r*1.4f<0 || x-r*1.4f>L.w || y-r*SwampGrass.TOP>L.h || a<.01f)continue;
                p.fillEllipse(x,y,r*1.7f,r*.35f,ink(0xFF385D45,a*.18f));
                SwampGrass.draw(p,x,y,r,seed,time,.35f+d*.55f,a*.70f);
            }
        }
    }
    private static void horizonGrass(Painter p,GameCore c,Layout L,float time,float fade) {
        // Keep the whole distant river opening clear, from the blade tips to their roots.
        float left=L.w,right=0,farDepth=waterDepth(L,horizon(L)+L.w*.030f*3f);
        for(int i=0;i<=4;i++) {
            float d=farDepth*i/4;
            left=Math.min(left,L.w*waterEdgeX(L,c.skyClock,d,-1));
            right=Math.max(right,L.w*waterEdgeX(L,c.skyClock,d,1));
        }
        for(int i=0;i<36;i++) {
            if(i%3==0 && fade==1 && horizonBank(L,i,left,right)==1
                    && horizonBank(L,i+1,left,right)==1 && horizonBank(L,i+2,left,right)==1
                    && SwampLayers.draw(p,SwampLayers.REEDS+i/3,L,0,1)) {i+=2;continue;}
            int seed=12400+i*28+(i%2==0?0:3);
            float x=L.w*i/35f,r=L.w*(.028f+.002f*hash(seed+7));
            // Keep roots below the horizon and a clear band of blade tips above it.
            float y=horizonAt(L,x)+r*3f;
            float bank=Math.max(0,Math.min(1,(Math.max(left-x,x-right)-r*1.1f)/(r*1.5f)));
            if(bank<=.01f)continue;
            SwampGrass.draw(p,x,y,r,seed,time,1,fade*bank*bank*(3-2*bank));
        }
    }
    static float horizonBank(Layout L,int i,float left,float right) {
        int seed=12400+i*28+(i%2==0?0:3);
        float x=L.w*i/35f,r=L.w*(.028f+.002f*hash(seed+7));
        return Math.max(0,Math.min(1,(Math.max(left-x,x-right)-r*1.1f)/(r*1.5f)));
    }
    static void reedGroup(Painter p,Layout L,int group) {
        for(int i=group*3;i<group*3+3;i++) {
            int seed=12400+i*28+(i%2==0?0:3);
            float x=L.w*i/35f,r=L.w*(.028f+.002f*hash(seed+7));
            SwampGrass.draw(p,x,horizonAt(L,x)+r*3,r,seed,0,1,1);
        }
    }
    private static void riverHaze(Painter p,GameCore c,Layout L,float fade) {
        float x=L.w*(.5f+bend(c.skyClock,.012f)*.6f),y=horizonAt(L,x)-L.h*.007f;
        // Carry the water into the distant opening with mist that follows the same bend.
        for(int i=0;i<8;i++) {
            float u=(i+.5f)/8,s=1.2f-i*.11f;
            p.fillEllipse(x,y,L.w*.22f*s,L.h*.022f*s,
                    ink(0xFF94A68F,fade*(.010f+.018f*(float)Math.sin(u*Math.PI))));
        }
    }
    static void horizonHaze(Painter p,Layout L,float fade) {
        float[] band=new float[100];
        for(int layer=0;layer<18;layer++) {
            float u=(layer+.5f)/18,top=L.h*(-.060f+layer*.007f),bottom=top+L.h*.007f;
            for(int i=0;i<25;i++) {
                float x=L.w*(-.06f+i/24f*1.12f),y=horizonAt(L,x);
                band[i*2]=x;band[i*2+1]=y+top;band[(49-i)*2]=x;band[(49-i)*2+1]=y+bottom;
            }
            // Match the distant sky at the seam, then feather into the visible banks.
            float softness=(float)Math.sin(u*Math.PI);
            p.fillPoly(band,ink(0xFF657C68,fade*.38f*softness*softness));
        }
    }
    private static void ripples(Painter p,GameCore c,Layout L,float fade) {
        float phase=c.skyClock*SPEED*12,time=c.cloudUi.reduceMotion?0:c.skyClock;
        for(int i=0;i<12;i++) {
            int seed=10091+(i-(int)Math.floor(phase))*173;
            float t=(i+phase-(float)Math.floor(phase))/12,d=depth(t);
            float x=L.w*(.5f+bend(c.skyClock,d)+(hash(seed)-.5f)*(.08f+.28f*d)),y=rootY(L,t,x);
            float cycle=time*.34f+hash(seed+3),u=cycle-(float)Math.floor(cycle);
            float r=L.w*(.006f+.028f*d)*(.45f+u),a=fade*reveal(t)*.27f*(float)Math.sin(u*Math.PI);
            if(y-r*.25f>L.h || a<.008f)continue;
            p.arc(x,y,r,r*.24f,15+hash(seed+7)*50,150+hash(seed+11)*130,
                    ink(0xFF9EBBA1,a),Math.max(.55f,L.w*(.0012f+.001f*d)));
        }
    }
    private static void underwaterLife(Painter p,GameCore c,Layout L,float fade) {
        float phase=c.skyClock*SPEED*6,time=c.cloudUi.reduceMotion?0:c.skyClock;float[] points=c.swampGeometry.quad;
        for(int i=0;i<6;i++) {
            int seed=16321+(i-(int)Math.floor(phase))*197;
            float t=(i+phase-(float)Math.floor(phase))/6,d=depth(t);
            float x=L.w*(.5f+bend(c.skyClock,d)+(hash(seed)-.5f)*(.08f+.22f*d));
            float y=rootY(L,t,x),r=L.w*(.006f+.025f*d),a=fade*reveal(t)*(.14f+.10f*d);
            if(y-r*3>L.h || a<.008f)continue;
            int kind=Math.floorMod(seed,3);
            if(kind==0) {
                p.fillEllipse(x,y,r*1.4f,r*.62f,ink(0xFF71846C,a));
                p.fillEllipse(x+r*1.6f,y+r*.65f,r*.75f,r*.40f,ink(0xFF587A70,a));
                p.fillEllipse(x-r*1.4f,y+r*.8f,r*.55f,r*.32f,ink(0xFF8A9678,a*.75f));
            } else if(kind==1) {
                for(int blade=0;blade<3;blade++) {
                    float h=r*(1.6f+blade*.5f),lean=r*(blade-1)*.65f;
                    for(int n=0;n<4;n++) {
                        float u=n/3f;
                        points[n*2]=x+lean*u+r*.20f*(float)Math.sin(time*.7f+seed+u*3)*u;
                        points[n*2+1]=y-h*u;
                    }
                    p.polyline(points,ink(0xFF69987D,a),Math.max(.55f,r*.18f));
                }
            } else {
                for(int fish=0;fish<2;fish++) {
                    float dir=seed%2==0?1:-1;
                    float xx=x+fish*r*2.4f+r*.4f*(float)Math.sin(time*.65f+seed),yy=y+fish*r*.7f;
                    p.fillEllipse(xx,yy,r*.85f,r*.27f,ink(0xFF709E9A,a));
                    points[0]=xx-dir*r*.7f;points[1]=yy;points[2]=xx-dir*r*1.35f;points[3]=yy-r*.40f;
                    points[4]=xx-dir*r*1.18f;points[5]=yy;points[6]=xx-dir*r*1.35f;points[7]=yy+r*.40f;
                    p.fillPoly(points,ink(0xFF709E9A,a));
                }
            }
        }
    }
    private static void lilyPad(Painter p,SwampGeometry g,float x,float y,float r,int seed,float time,float fade,boolean still) {
        float[] pad=g.pad;pad[0]=x;pad[1]=y;
        for(int i=0;i<12;i++) {
            float angle=.28f+i*(Softbody.TAU-.56f)/11;
            pad[(i+1)*2]=x+(float)Math.cos(angle)*r*1.65f;
            pad[(i+1)*2+1]=y+(float)Math.sin(angle)*r*.65f;
        }
        p.fillEllipse(x,y+r*.24f,r*1.8f,r*.55f,ink(0xFF153F3A,fade*.20f));
        p.fillPoly(pad,ink(0xFF6A965E,fade));
        p.line(x-r*1.2f,y+r*.1f,x+r*.45f,y-r*.12f,ink(0xFFA3B77D,fade*.35f),Math.max(.5f,r*.07f));
        float breath=still?0:(float)Math.sin(time*2+seed)*r*.025f;
        int green=ink(0xFF80A85F,fade),dark=ink(0xFF284D3C,fade);
        p.fillEllipse(x-r*.53f,y-r*.12f,r*.32f,r*.22f,green);
        p.fillEllipse(x+r*.53f,y-r*.12f,r*.32f,r*.22f,green);
        p.fillEllipse(x,y-r*.32f,r*.64f,r*.46f+breath,green);
        p.fillEllipse(x,y-r*.23f,r*.36f,r*.22f,ink(0xFFBDCB83,fade*.65f));
        float blink=(time+hash(seed+5)*5)%5.2f;
        for(int side=-1;side<=1;side+=2) {
            float eyeX=x+side*r*.29f,eyeY=y-r*.67f;
            p.fillEllipse(eyeX,eyeY,r*.20f,r*.22f,ink(0xFFD1D99A,fade));
            if(!still && blink>5.06f)p.line(eyeX-r*.10f,eyeY,eyeX+r*.10f,eyeY,dark,Math.max(.5f,r*.07f));
            else p.fillCircle(eyeX,eyeY,r*.10f,dark);
        }
        p.arc(x,y-r*.36f,r*.24f,r*.13f,10,160,dark,Math.max(.5f,r*.06f));
    }
    private static void flotsam(Painter p,GameCore c,Layout L,float fade) {
        float phase=c.skyClock*SPEED*8,time=c.cloudUi.reduceMotion?0:c.skyClock;
        for(int i=0;i<8;i++) {
            int seed=6211+(i-(int)Math.floor(phase))*139;
            float t=(i+phase-(float)Math.floor(phase))/8,d=depth(t);
            float x=L.w*(.5f+bend(c.skyClock,d)+(hash(seed)-.5f)*(.07f+.17f*d));
            float y=rootY(L,t,x),r=L.w*(.004f+.020f*d),a=fade*.60f*reveal(t);
            if(y-r>L.h || a<.01f)continue;
            float bob=(float)Math.sin(time*.6f+seed)*r*.12f;
            p.fillEllipse(x,y+r*.55f,r*2,r*.25f,ink(0xFF153F3A,a*.24f));
            int kind=Math.floorMod(seed,3);
            if(kind==0) {
                lilyPad(p,c.swampGeometry,x,y+bob,r*2,seed,time,a*1.25f,c.cloudUi.reduceMotion);
            } else if(kind==1) {
                p.line(x-r*1.8f,y-r*.2f+bob,x+r*1.8f,y+r*.3f+bob,ink(0xFF827C5E,a),r*.45f);
                p.line(x,y+bob,x+r*.55f,y-r*.75f+bob,ink(0xFF827C5E,a),r*.23f);
            } else {
                float[] leaf=c.swampGeometry.quad;
                leaf[0]=x-r*1.1f;leaf[1]=y+bob;leaf[2]=x-r*.25f;leaf[3]=y-r*.55f+bob;
                leaf[4]=x+r*1.2f;leaf[5]=y+r*.2f+bob;leaf[6]=x+r*.1f;leaf[7]=y+r*.45f+bob;
                p.fillPoly(leaf,ink(0xFF8A9C69,a));
            }
        }
        for(int i=0;i<5;i++) {
            float d=.025f+i*.015f,x=L.w*(.5f+bend(c.skyClock,d)),y=bankY(L,d,x),w=L.w*(.018f+i*.005f);
            p.fillEllipse(x,y,w,L.h*.0012f,ink(0xFFB0C3A4,fade*.08f));
        }
    }
}
