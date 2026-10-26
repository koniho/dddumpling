package com.dddumpling.game;

/** A stationary reef chamber: shaded stone and its relighting are cached separately. */
final class UnderwaterCave extends Draw {
    static final int KELP_PATCHES=18;
    private UnderwaterCave() {}
    private static int ink(int color,float alpha){return Glyph.withAlpha(color,(int)alpha);}

    static void draw(Painter p,GameCore c,Layout L,float fade,float arrival) {
        if(p instanceof WaterPainter)p=((WaterPainter)p).anchored();
        Boss b=BossVictory.sceneBoss(c);
        float movement=motion(b,L),damage=damagePulse(b);
        float bx=b.body==null?b.bodyX(L):b.body.centreX();
        float by=b.body==null?b.bodyY(L):b.body.centreY();
        UnderwaterVista.image(p,UnderwaterVista.CAVE,0,0,L.w,L.h,fade);
        kelp(p,c,L,b,fade,0,KELP_PATCHES);
        glow(p,bx,by+L.h*.13f,L.w*(.36f+damage*.14f),L.h*(.27f+damage*.10f),fade*(12+movement*24+damage*90));
        for(int i=0;i<3;i++) {
            float pulse=c.waterRipples.beamPulse(c.clock,.24f+i*.25f);
            float drift=(float)Math.sin(c.clock*.17f+i*2.4f)*L.w*.045f+(bx-L.w*.5f)*.28f;
            float light=.20f+UnderwaterVista.beamIdle(c.clock*.7f,i)*.65f+pulse*.30f+movement*.10f+damage*.48f;
            UnderwaterVista.image(p,UnderwaterVista.CAVE+3,L.w*(-.22f+i*.30f)+drift,
                    -L.h*(.08f+i*.025f),L.w*(.85f-i*.08f),L.h*(.94f-i*.04f),fade*light);
        }
        float y=-(1-arrival)*L.h*.16f;
        UnderwaterVista.image(p,UnderwaterVista.CAVE+1,0,y,L.w,L.h,fade);
        for(int side=0;side<2;side++) {
            int arm=side==0?1:6;
            float reachY=b.octoPlaced && (b.octoArms&(1<<arm))!=0?b.octoY[arm][Boss.OCTO_NODES-1]:by+L.h*.15f;
            float gy=Math.max(L.h*.25f,Math.min(L.h*.79f,reachY));
            glow(p,L.w*(side==0?.12f:.88f),gy,L.w*(.21f+damage*.10f),L.h*(.24f+damage*.09f),
                    fade*(9+movement*26+damage*110));
        }
        float pulse=c.waterRipples.beamPulse(c.clock,.5f);
        float shimmer=.055f+.045f*(.5f+.5f*(float)Math.sin(c.clock*.47f));
        UnderwaterVista.image(p,UnderwaterVista.CAVE+2,0,y,L.w,L.h,fade*Math.min(1,shimmer+pulse*.85f+movement*.22f+damage*1.45f));
    }

    static float damagePulse(Boss b) {
        if(b.kind!=Boss.OCTOPUS)return 0;
        return Math.max(b.hurt,b.octoDeath>0?Math.max(0,1-b.octoDeath):0);
    }
    static float motion(Boss b,Layout L) {
        if(b.kind!=Boss.OCTOPUS || !b.octoPlaced)return 0;
        float energy=0;
        for(int arm=0;arm<Boss.OCTO_ARMS;arm++)if((b.octoArms&(1<<arm))!=0) {
            int tip=Boss.OCTO_NODES-1;
            energy+=Math.abs(b.octoVX[arm][tip])+Math.abs(b.octoVY[arm][tip]);
        }
        return Math.min(1,energy/(L.w*3));
    }
    static float current(Boss b,Layout L,float x,float y) {
        if(b.kind!=Boss.OCTOPUS || !b.octoPlaced)return 0;
        float force=0;
        for(int arm=0;arm<Boss.OCTO_ARMS;arm++)if((b.octoArms&(1<<arm))!=0) {
            int tip=Boss.OCTO_NODES-1;
            float dx=(x-b.octoX[arm][tip])/L.w,dy=(y-b.octoY[arm][tip])/L.h;
            float weight=1/(1+dx*dx*22+dy*dy*28);
            force+=Math.max(-1,Math.min(1,b.octoVX[arm][tip]/(L.w*.6f)))*weight*.50f;
        }
        return Math.max(-.8f,Math.min(.8f,force));
    }
    private static void glow(Painter p,float x,float y,float rx,float ry,float alpha) {
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.waterGlow(x,y,rx,ry,Math.round(alpha)))return;
        float previous=0;
        for(int layer=0;layer<10;layer++) {
            float radius=3*(1-layer/10f),opacity=alpha*(float)Math.exp(-radius*radius*.5f);
            int a=Math.round(opacity)-Math.round(previous);previous=opacity;
            p.fillEllipse(x,y,rx*radius/3,ry*radius/3,ink(0xFF80DCD2,a));
        }
    }
    private static void kelp(Painter p,GameCore c,Layout L,Boss b,float fade,int first,int end) {
        float damage=damagePulse(b);
        float bx=b.body==null?L.w*.5f:b.body.centreX();
        float[] blade=c.waterRipples.kelpBlade,edge=c.waterRipples.kelpEdge,shadow=c.waterRipples.kelpShadow;
        for(int i=first;i<end;i++) {
            int seed=721+i*97;boolean far=i<8;
            float x=kelpX(i),y=kelpY(i);
            float depth=Math.max(0,Math.min(1,(y-.70f)/.18f));
            float height=L.h*Math.min(y-.675f,(.025f+depth*.04f)*(1+hash(seed+7)*1.1f));
            float flow=c.waterRipples.caveCurrent.flow[i],direction=x*L.w>=bx?1:-1;
            float localDamage=c.waterRipples.caveCurrent.damage[i],age=c.waterRipples.caveCurrent.age[i];
            float flash=Math.min(1,damage*.35f+localDamage*.90f);
            float dx=x-b.hitX/L.w,dy=y-b.hitY/L.h;
            float distance=(float)Math.sqrt(dx*dx+dy*dy);
            float surge=localDamage*(.68f*direction+.52f*(float)Math.sin(age*13-distance*3));
            float strength=fade*(far?.42f:.90f);
            for(int stem=0;stem<2;stem++) {
                float rootX=x*L.w+(stem-.5f)*L.w*.008f,rootY=y*L.h+stem*L.h*.003f;
                float tall=height*(stem==0?1:.73f),phase=c.clock*.85f+i*2.1f+stem*.7f;
                for(int j=0;j<9;j++) {
                    float u=j/8f;
                    float sway=.14f*(float)Math.sin(phase-u*3.4f)+.06f*(float)Math.sin(phase*.7f+u*6);
                    float bend=tall*u*u*(sway+flow*.60f+surge*.72f);
                    float xx=rootX+bend+(stem-.5f)*tall*u*.16f;
                    float yy=rootY-tall*u+Math.abs(bend)*.22f*u;
                    float width=L.w*(far?.0040f:.0075f)*(1+.32f*(float)Math.sin(u*13+seed))*(1-u*.94f);
                    int left=j*2,right=(17-j)*2;
                    blade[left]=xx-width;blade[right]=xx+width;blade[left+1]=blade[right+1]=yy;
                    edge[left]=xx-width*.42f;edge[right]=xx-width*.10f;edge[left+1]=edge[right+1]=yy;
                    shadow[left]=xx-width*1.6f+tall*u*.40f;shadow[right]=xx+width*1.6f+tall*u*.40f;
                    shadow[left+1]=shadow[right+1]=rootY+tall*u*.18f;
                }
                p.fillPoly(shadow,ink(0xFF01050C,strength*70));
                p.fillPoly(blade,ink(Glyph.mix(i%3==0?0xFF285C58:0xFF285C4A,0xFFAFF5CF,flash*.85f),strength*240));
                p.fillPoly(edge,ink(Glyph.mix(0xFF8BCDB1,0xFFE3FFF2,flash),strength*(48+flash*180+Math.abs(flow)*25)));
            }
        }
    }

    static float kelpX(int i) {
        int seed=721+i*97;
        float depth=Math.max(0,Math.min(1,(kelpY(i)-.70f)/.18f));
        return .5f+(hash(seed)-.5f)*(.20f+depth*.25f);
    }
    static float kelpY(int i) {
        int seed=721+i*97;
        return i<8?.71f+hash(seed+3)*.06f:i<14?.77f+hash(seed+3)*.07f:.84f+hash(seed+3)*.04f;
    }
    static float travelDelay(float dx,float dy) {
        return Math.min(.75f,.045f+.75f*(float)Math.sqrt(dx*dx+dy*dy));
    }
    static final class CurrentHistory {
        static final int SAMPLES=64;
        final float[] flow=new float[KELP_PATCHES],damage=new float[KELP_PATCHES],age=new float[KELP_PATCHES];
        private final float[][] samples=new float[SAMPLES][KELP_PATCHES+2];
        private final float[] times=new float[SAMPLES];
        private int next,count;
        private float lastTime=-100,lastAge=-1;
        void reset() {
            next=count=0;lastTime=-100;lastAge=-1;
            java.util.Arrays.fill(flow,0);java.util.Arrays.fill(damage,0);java.util.Arrays.fill(age,0);
        }
        void update(GameCore c,Layout L) {
            Boss b=c.boss;
            if(c.state!=GameCore.PLAY || b.kind!=Boss.OCTOPUS || !b.active()) {reset();return;}
            if(b.age<lastAge || c.clock<lastTime)reset();
            lastAge=b.age;
            if(c.clock-lastTime>=1/60f-.00001f) {
                float[] sample=samples[next];times[next]=lastTime=c.clock;
                for(int i=0;i<KELP_PATCHES;i++)sample[i]=current(b,L,kelpX(i)*L.w,kelpY(i)*L.h);
                sample[KELP_PATCHES]=damagePulse(b);
                sample[KELP_PATCHES+1]=b.octoDeath>0?b.octoDeath:(1-b.hurt)*.38f;
                next=(next+1)%SAMPLES;count=Math.min(SAMPLES,count+1);
            }
            float bx=(b.body==null?b.bodyX(L):b.body.centreX())/L.w;
            float by=(b.body==null?b.bodyY(L):b.body.centreY())/L.h;
            for(int i=0;i<KELP_PATCHES;i++) {
                float target=c.clock-travelDelay(kelpX(i)-bx,kelpY(i)-by);
                flow[i]=damage[i]=age[i]=0;
                for(int n=0;n<count;n++) {
                    int older=(next-1-n+SAMPLES)%SAMPLES;
                    if(times[older]>target)continue;
                    int newer=n==0?older:(older+1)%SAMPLES;
                    float span=times[newer]-times[older],t=span<=0?0:Math.min(1,(target-times[older])/span);
                    flow[i]=samples[older][i]+(samples[newer][i]-samples[older][i])*t;
                    damage[i]=samples[older][KELP_PATCHES]+(samples[newer][KELP_PATCHES]-samples[older][KELP_PATCHES])*t;
                    age[i]=samples[older][KELP_PATCHES+1]+(samples[newer][KELP_PATCHES+1]-samples[older][KELP_PATCHES+1])*t;
                    break;
                }
            }
        }
    }

    static void paint(Painter p,int layer,float x,float y,float w,float h,float fade) {
        if(layer==0){chamber(p,x,y,w,h,fade);return;}
        if(layer==3){mist(p,x,y,w,h,fade);return;}
        Painter stone=layer==2?new LitStone(p):p;
        // Broken buttresses have independent silhouettes, leaving an off-centre ceiling fissure.
        for(int plane=0;plane<3;plane++)for(int side=0;side<2;side++)
            buttress(stone,x,y,w,h,side,plane,fade);
        shard(stone,x,y,w,h,new float[]{.32f,-.06f,.52f,-.05f,.47f,.025f,.48f,.07f,.43f,.155f,.41f,.08f,.35f,.10f},
                0xFF081928,0xFF386B7A,fade);
        shard(stone,x,y,w,h,new float[]{.65f,-.05f,.86f,-.04f,.82f,.11f,.76f,.18f,.75f,.09f,.69f,.13f,.67f,.06f},
                0xFF030D18,0xFF284D60,fade);
        // Fractured shelves interrupt the cave floor instead of forming a neat arch base.
        shard(stone,x,y,w,h,new float[]{-.06f,.80f,.10f,.77f,.19f,.81f,.24f,.80f,.29f,.86f,.18f,.87f,.13f,.92f,-.06f,.96f},
                0xFF06131D,0xFF2B535D,fade);
        shard(stone,x,y,w,h,new float[]{1.05f,.75f,.91f,.78f,.87f,.81f,.77f,.82f,.72f,.87f,.85f,.89f,.88f,.95f,1.05f,.93f},
                0xFF030A12,0xFF294A5C,fade);
        for(int side=0;side<2;side++)
            UnderwaterCoral.colony(stone,x+w*(side==0?.17f:.84f),y+h*(side==0?.84f:.86f),w*.055f,
                    419+side*93,side==0?2:4,fade*.60f,419,side==0?0xFF6AACA8:0xFF777FAA);
    }

    private static final float[] LEFT={-.12f,-.1f,.36f,-.1f,.33f,.06f,.26f,.11f,.29f,.17f,
        .23f,.20f,.27f,.28f,.17f,.25f,.13f,.34f,.18f,.38f,.12f,.45f,.17f,.51f,
        .10f,.56f,.18f,.62f,.14f,.68f,.22f,.73f,.13f,.78f,.16f,.87f,.21f,1.08f,-.12f,1.08f};
    private static final float[] RIGHT={1.12f,-.1f,.68f,-.1f,.72f,.06f,.64f,.12f,.71f,.17f,
        .66f,.24f,.79f,.21f,.83f,.28f,.77f,.33f,.86f,.39f,.80f,.45f,.87f,.51f,
        .81f,.59f,.87f,.64f,.78f,.70f,.85f,.75f,.80f,.84f,.84f,.91f,.77f,1.08f,1.12f,1.08f};

    private static void buttress(Painter p,float x,float y,float w,float h,int side,int plane,float fade) {
        float[] original=side==0?LEFT:RIGHT,edge=new float[original.length];
        float sign=side==0?1:-1,shift=(1-plane)*.047f;
        for(int j=0;j<edge.length;j+=2) {
            edge[j]=x+w*(original[j]+sign*shift+(hash(911+j*7+side*83+plane*39)-.5f)*.025f);
            edge[j+1]=y+h*(original[j+1]+(hash(559+j+plane*23+side*71)-.5f)*.026f);
        }
        int body=plane==0?0xFF123344:plane==1?0xFF091B29:0xFF020913;
        p.fillPoly(edge,ink(body,fade*255));
        // Split rock faces catch cool light on only one side of each fractured edge.
        for(int j=2;j<edge.length-4;j+=2) {
            float ax=edge[j],ay=edge[j+1],bx=edge[j+2],by=edge[j+3];
            float deep=(.07f+hash(j+plane*61+side*41)*.11f)*w;
            float[] face={ax,ay,bx,by,bx-sign*deep,by+h*.02f,ax-sign*deep*.65f,ay-h*.015f};
            int col=(j/2+plane+side)%3==0?0xFF31596B:(j/2+side)%2==0?0xFF19394B:0xFF0C2434;
            p.fillPoly(face,ink(col,fade*(plane==2?190:235)));
            if(j%4==0) {
                p.polyline(new float[]{ax-sign*w*.008f,ay,bx-sign*w*.008f,by,bx-sign*deep*.48f,by+h*.014f},
                        ink(plane==0?0xFF5F999E:0xFF406C80,fade*(plane==2?90:145)),w*(plane==0?.003f:.002f));
            }
            p.line(ax-sign*deep*.4f,ay+h*.012f,bx-sign*deep*.6f,by,ink(0xFF01050E,fade*160),w*.004f);
        }
    }

    private static void shard(Painter p,float x,float y,float w,float h,float[] points,int body,int rim,float fade) {
        float[] shape=points.clone();
        for(int i=0;i<shape.length;i+=2){shape[i]=x+w*shape[i];shape[i+1]=y+h*shape[i+1];}
        p.fillPoly(shape,ink(body,fade*255));
        float[] facet=new float[shape.length];
        for(int i=0;i<shape.length;i+=2){facet[i]=shape[i]+w*.009f;facet[i+1]=shape[i+1]-h*.009f;}
        p.fillPoly(facet,ink(rim,fade*80));
        p.polyline(shape,ink(rim,fade*110),w*.0025f);
    }

    private static void mist(Painter p,float x,float y,float w,float h,float fade) {
        float[] ribbon=new float[164];
        float previous=0;
        // Continuous feathered ribbons avoid horizontal strip edges in the light shafts.
        for(int layer=0;layer<28;layer++) {
            float radius=3*(1-layer/28f),opacity=180*(float)Math.exp(-radius*radius*.5f);
            int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
            for(int j=0;j<=40;j++) {
                float u=j/40f,start=(3-radius)*.035f,end=1-(3-radius)*.08f;
                float t=start+(end-start)*u,centre=.28f+.30f*t+.10f*(float)Math.sin(t*5.5f);
                float spread=(.025f+.11f*t)*radius*(float)Math.pow(Math.sin(u*Math.PI),.65f);
                ribbon[j*2]=x+w*(centre-spread);ribbon[j*2+1]=y+h*t;
                ribbon[(81-j)*2]=x+w*(centre+spread);ribbon[(81-j)*2+1]=y+h*t;
            }
            p.fillPoly(ribbon,ink(layer<14?0xFF477FA8:0xFFABF2DB,fade*alpha));
        }
    }

    private static void chamber(Painter p,float x,float y,float w,float h,float fade) {
        // Tiny baked dithering avoids broad color bands in the near-black gradient.
        for(int row=0;row<256;row++) {
            float t=(row+.5f)/256f;
            float light=(float)Math.exp(-Math.pow((t-.46f)/.24f,2));
            for(int col=0;col<32;col++) {
                float noise=hash(935+row*37+col);
                int r=(int)(2+18*light+noise),g=(int)(8+50*light+noise),b=(int)(19+58*light+noise);
                p.fillRect(x+w*col/32,y+(float)Math.floor(h*row/256),
                        x+w*(col+1)/32,y+(float)Math.floor(h*(row+1)/256),
                        ink(0xFF000000|(r<<16)|(g<<8)|b,fade*255));
            }
        }
        for(int plane=0;plane<2;plane++) {
            float inset=plane==0?.24f:.13f;
            Painter distant=new DepthStone(p,plane==0?.60f:.36f);
            for(int side=0;side<2;side++)
                buttress(distant,x+w*inset,y+h*(plane==0?.17f:.08f),w*(1-inset*2),h*(plane==0?.70f:.88f),side,0,fade);
        }
        // Dark sand fills the hollow; a soft, uneven lip tucks beneath the distant walls.
        float[] floor=new float[44];
        for(int layer=0;layer<24;layer++) {
            for(int j=0;j<20;j++) {
                float u=j/19f;
                floor[j*2]=x+w*u;
                floor[j*2+1]=y+h*(.61f+layer*.0018f+.022f*(u-.5f)*(u-.5f)
                        +.008f*(float)Math.sin(u*11)+.004f*(float)Math.sin(u*29));
            }
            floor[40]=x+w;floor[41]=y+h*1.1f;floor[42]=x;floor[43]=y+h*1.1f;
            p.fillPoly(floor,ink(0xFF363E37,fade*15));
        }
        // Wider, farther-apart ripples establish the slope toward the foreground.
        for(int row=0;row<17;row++) {
            float t=(float)Math.pow(row/16f,1.65),yy=y+h*(.65f+t*.45f),span=w*(.19f+t*1.10f);
            float[] ridge=new float[26],lee=new float[26];
            for(int j=0;j<13;j++) {
                float u=j/12f;
                ridge[j*2]=x+w*(.5f+.022f*(float)Math.sin(row*1.8f))+(u-.5f)*span;
                ridge[j*2+1]=yy+h*(.002f+t*.006f)*(float)Math.sin(u*7+row*1.9f);
                lee[j*2]=ridge[j*2];lee[j*2+1]=ridge[j*2+1]+h*(.001f+t*.002f);
            }
            p.polyline(lee,ink(0xFF07121C,fade*(28+t*18)),w*(.002f+t*.004f));
            p.polyline(ridge,ink(0xFF8B9582,fade*(15+t*12)),w*(.001f+t*.002f));
        }
        // Fine grain is baked once with the floor, rather than animated particles.
        for(int i=0;i<210;i++) {
            float t=hash(591+i*17),xx=x+w*(.5f+(hash(887+i*29)-.5f)*(.18f+t*1.08f));
            float yy=y+h*(.655f+t*.44f),length=w*(.001f+t*.004f)*(1+hash(i+811));
            p.line(xx,yy,xx+length,yy-h*.0005f,
                    ink(i%3==0?0xFF060F18:0xFF9B9B7D,fade*(i%3==0?28:14)),w*(.0006f+t*.0008f));
        }

    }

    private static final class DepthStone extends OpacityPainter {
        final float haze;
        DepthStone(Painter p,float haze){super(p,1);this.haze=haze;}
        int color(int c){return (c&0xFF000000)|(Glyph.mix(c,0xFF143846,haze)&0xFFFFFF);}
        public ArtStamp stamps(){return null;}
    }
    private static final class LitStone extends OpacityPainter {
        LitStone(Painter p){super(p,1);}
        int color(int c) {
            float light=(((c>>>16)&255)*.21f+((c>>>8)&255)*.72f+(c&255)*.07f)/255;
            light=Math.max(0,(light-.08f)*1.8f);
            return ink(0xFF9CE3DB,(c>>>24)*Math.min(1,light*light*3));
        }
        public ArtStamp stamps(){return null;}
    }
}
