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
        backdrop(p,L,fade);
        nightSky(p,L,fade);
        horizonTrees(p,L,fade);
        riverHaze(p,L,fade);
        banks(p,c,L,fade);
        horizonGrass(p,L,time,fade);
        ripples(p,c,L,fade);
        flotsam(p,c,L,fade);
        // Keep tall trailing tufts until their tips leave view; world seeds survive row rollover.
        for(int row=0;row<DRAW_ROWS;row++)for(int side=-1;side<=1;side+=2)for(int lane=LANES-1;lane>=0;lane--) {
            int seed=grassSeed(c.skyClock,row,side,lane);
            float t=travel(c.skyClock,row,side,lane),d=depth(t),s=scale(t);
            float x=L.w*grassX(L,c.skyClock,t,side,lane,seed),y=rootY(L,t,x);
            float r=L.w*SwampGrass.RADIUS*s*(.8f+hash(seed)*.25f);
            if(x+r*1.4f<0 || x-r*1.4f>L.w || y-r*SwampGrass.TOP>L.deckTop)continue;
            float opacity=fade*reveal(t);if(opacity<.008f)continue;
            SwampGrass.draw(p,x,y,r,seed,time,.25f+.75f*Math.min(1,d*2.5f),opacity);
        }
        shoreReeds(p,c,L,time,fade);
        horizonHaze(p,L,fade);
        LandBonusArt.inSwamp(p,c,L,fade);
        c.swampFireflies.draw(p,c,L,fade);
    }
    private static void backdrop(Painter p,Layout L,float fade) {
        float h=horizon(L)/L.deckTop;
        for(int i=0;i<80;i++) {
            float t=i/80f,y=i==0?-L.h*.03f:(float)Math.floor(t*L.deckTop),next=(float)Math.floor((i+1)/80f*L.deckTop);
            int color=t<h?Glyph.mix(0xFF142E3B,0xFF657C68,(float)Math.pow(t/h,2.2)):Glyph.mix(0xFF657C68,0xFF153E43,(float)(1-Math.exp(-3.7f*(t-h)/(1-h))));
            p.fillRect(-L.w*.06f,y,L.w*1.06f,next,ink(color,fade));
        }
    }
    private static void nightSky(Painter p,Layout L,float fade) {
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
    private static void horizonTrees(Painter p,Layout L,float fade) {
        float[] tree=new float[78];
        for(int layer=0;layer<3;layer++)for(int i=0;i<12;i++) {
            int seed=919+layer*317+i*67;
            float x=L.w*((i+.2f+hash(seed)*.6f)/12),y=horizonAt(L,x)+L.h*(.032f+layer*.004f);
            float h=L.h*(.045f+layer*.025f+hash(seed+3)*.05f),r=L.w*(.055f+layer*.016f+hash(seed+5)*.045f);
            float lean=L.w*(hash(seed+7)-.5f)*.04f;
            int color=ink(layer==0?0xFF4F6A60:layer==1?0xFF3C5A4E:0xFF2E4A40,fade);
            // A single solid outline avoids darker trunk/canopy intersections and tree overlaps.
            tree[0]=x-r*.50f;tree[1]=y;tree[2]=x-r*.12f;tree[3]=y-h*.21f;
            for(int n=0;n<32;n++) {
                float angle=Softbody.TAU*.25f+.14f+n*(Softbody.TAU-.28f)/31;
                float reach=r*(.90f+.07f*(float)Math.sin(angle*5+seed)+.03f*(float)Math.sin(angle*9-seed));
                tree[(n+2)*2]=x+lean+(float)Math.cos(angle)*reach;
                tree[(n+2)*2+1]=y-h+(float)Math.sin(angle)*reach*.42f;
            }
            tree[68]=x+r*.13f;tree[69]=y-h*.20f;tree[70]=x+r*.55f;tree[71]=y;
            tree[72]=x+r*.28f;tree[73]=y-h*.10f;tree[74]=x;tree[75]=y-h*.13f;
            tree[76]=x-r*.25f;tree[77]=y-h*.10f;
            // Sink the roots behind the curved distant bank instead of exposing a separate row.
            for(int n=0;n<tree.length;n+=2)tree[n+1]=Math.min(tree[n+1],horizonAt(L,tree[n]));
            p.fillPoly(tree,color);
        }
    }
    private static float bankY(Layout L,float d,float x) {
        return horizon(L)+d*(L.deckTop-horizon(L)+L.h*.35f)+(horizonAt(L,x)-horizon(L))*Math.max(0,1-d*2);
    }
    private static void banks(Painter p,GameCore c,Layout L,float fade) {
        float range=L.deckTop-horizon(L);float[] xs=new float[49],ds=new float[49],widths=new float[49];
        float[] core=new float[120],strip=new float[196];
        float[] offsets={-.8f,-.25f,.3f,.85f,1.4f};
        int[] colors={0xFF3D6557,0xFF285348,0xFF2B4A3B,0xFF324F3B};float[] alpha={.12f,.28f,.45f,.58f};
        for(int side=-1;side<=1;side+=2) {
            float outer=side<0?-L.w*.06f:L.w*1.06f;
            for(int i=0;i<=48;i++) {
                float v=i/48f;ds[i]=waterDepth(L,horizon(L)+v*range);
                xs[i]=L.w*waterEdgeX(L,c.skyClock,ds[i],side);
                float material=3*ds[i]/(1+2*ds[i])-c.skyClock*SPEED;
                widths[i]=L.w*(.006f+.016f*v)*(.85f+.20f*(float)Math.sin(material*19+side));
            }
            float top=xs[0]+side*widths[0]*1.4f;
            for(int i=0;i<=10;i++){float x=outer+(top-outer)*i/10f;core[i*2]=x;core[i*2+1]=horizonAt(L,x);}
            for(int i=1;i<=48;i++) {
                float x=xs[i]+side*widths[i]*1.4f;core[(i+10)*2]=x;core[(i+10)*2+1]=bankY(L,ds[i],x);
            }
            core[118]=outer;core[119]=L.deckTop;p.fillPoly(core,ink(0xFF324F3B,fade*.65f));
            // Narrow non-overlapping wet margins soften the bank without full-screen overdraw.
            for(int band=0;band<4;band++) {
                for(int i=0;i<=48;i++) {
                    float x=xs[i]+side*widths[i]*offsets[band],xx=xs[i]+side*widths[i]*offsets[band+1];
                    strip[i*2]=x;strip[i*2+1]=bankY(L,ds[i],x);
                    strip[(97-i)*2]=xx;strip[(97-i)*2+1]=bankY(L,ds[i],xx);
                }
                p.fillPoly(strip,ink(colors[band],fade*alpha[band]));
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
                if(x+r*1.4f<0 || x-r*1.4f>L.w || y-r*SwampGrass.TOP>L.deckTop || a<.01f)continue;
                p.fillEllipse(x,y,r*1.7f,r*.35f,ink(0xFF385D45,a*.18f));
                SwampGrass.draw(p,x,y,r,seed,time,.35f+d*.55f,a*.70f);
            }
        }
    }
    private static void horizonGrass(Painter p,Layout L,float time,float fade) {
        // Distant reeds close the horizon below the trees, including the far river bend.
        for(int i=0;i<36;i++) {
            int seed=12400+i*28+(i%2==0?0:3);
            float x=L.w*i/35f,r=L.w*(.015f+.001f*hash(seed+7));
            float y=horizonAt(L,x)+r*7.4f;
            SwampGrass.draw(p,x,y,r,seed,time,.95f,fade);
        }
    }
    private static void riverHaze(Painter p,Layout L,float fade) {
        float x=L.w*.5f,y=horizonAt(L,x)-L.h*.022f;
        // A local mist pocket marks the river opening behind the stronger foreground reeds.
        for(int i=0;i<8;i++) {
            float u=(i+.5f)/8,s=1.2f-i*.11f;
            p.fillEllipse(x,y,L.w*.19f*s,L.h*.032f*s,
                    ink(0xFF94A68F,fade*(.018f+.028f*(float)Math.sin(u*Math.PI))));
        }
    }
    private static void horizonHaze(Painter p,Layout L,float fade) {
        float[] band=new float[100];
        for(int layer=0;layer<18;layer++) {
            float u=(layer+.5f)/18,top=L.h*(-.060f+layer*.007f),bottom=top+L.h*.007f;
            for(int i=0;i<25;i++) {
                float x=L.w*(-.06f+i/24f*1.12f),y=horizonAt(L,x);
                band[i*2]=x;band[i*2+1]=y+top;band[(49-i)*2]=x;band[(49-i)*2+1]=y+bottom;
            }
            // Match the distant sky at the seam, then feather into the visible banks.
            float softness=(float)Math.sin(u*Math.PI);
            p.fillPoly(band,ink(0xFF657C68,fade*.60f*softness*softness));
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
            if(y-r*.25f>L.deckTop || a<.008f)continue;
            p.arc(x,y,r,r*.24f,15+hash(seed+7)*50,150+hash(seed+11)*130,
                    ink(0xFF9EBBA1,a),Math.max(.55f,L.w*(.0012f+.001f*d)));
        }
    }
    private static void flotsam(Painter p,GameCore c,Layout L,float fade) {
        float phase=c.skyClock*SPEED*8,time=c.cloudUi.reduceMotion?0:c.skyClock;
        for(int i=0;i<8;i++) {
            int seed=6211+(i-(int)Math.floor(phase))*139;
            float t=(i+phase-(float)Math.floor(phase))/8,d=depth(t);
            float x=L.w*(.5f+bend(c.skyClock,d)+(hash(seed)-.5f)*(.07f+.17f*d));
            float y=rootY(L,t,x),r=L.w*(.004f+.020f*d),a=fade*.60f*reveal(t);
            if(y-r>L.deckTop || a<.01f)continue;
            float bob=(float)Math.sin(time*.6f+seed)*r*.12f;
            p.fillEllipse(x,y+r*.55f,r*2,r*.25f,ink(0xFF153F3A,a*.24f));
            int kind=Math.floorMod(seed,3);
            if(kind==0) {
                p.fillEllipse(x,y+bob,r*1.1f,r*.45f,ink(0xFF658C6B,a));
                p.fillEllipse(x+r*1.1f,y+r*.4f+bob,r*.7f,r*.30f,ink(0xFF71966F,a*.85f));
            } else if(kind==1) {
                p.line(x-r*1.8f,y-r*.2f+bob,x+r*1.8f,y+r*.3f+bob,ink(0xFF827C5E,a),r*.45f);
                p.line(x,y+bob,x+r*.55f,y-r*.75f+bob,ink(0xFF827C5E,a),r*.23f);
            } else {
                p.fillPoly(new float[]{x-r*1.1f,y+bob,x-r*.25f,y-r*.55f+bob,x+r*1.2f,y+r*.2f+bob,x+r*.1f,y+r*.45f+bob},ink(0xFF8A9C69,a));
            }
        }
        for(int i=0;i<5;i++) {
            float d=.025f+i*.015f,x=L.w*(.5f+bend(c.skyClock,d)),y=bankY(L,d,x),w=L.w*(.018f+i*.005f);
            p.fillEllipse(x,y,w,L.h*.0012f,ink(0xFFB0C3A4,fade*.08f));
        }
    }
}
