package com.dddumpling.game;

/** Draw-only victory doodle: the retained boss paints a sad face after the player loses. */
final class RainbowCatTaunt extends Draw {
    static final float DURATION=4.4f, NOTE_START=.18f, NOTE_STEP=.46f;
    static final int NOTES=7;
    private static final float[] START={.22f,1.42f,1.88f,2.36f}, END={1.25f,1.75f,2.21f,3.30f};
    static boolean retained(GameCore c) {
        return BossVictory.retained(c) && c.bossVictoryKind==Boss.RAINBOW_CAT;
    }
    static float scrollRate(float age) {return RainbowLand.BATTLE_SKY_RATE*(1+2*ease(age/1.2f));}
    static float starTravel(float age) {
        float u=Math.min(1,Math.max(0,age/1.2f));
        return age+2*(1.2f*(u*u*u-.5f*u*u*u*u)+Math.max(0,age-1.2f));
    }
    static void background(Painter p,GameCore c,Layout L) {
        if(!retained(c))return;
        RainbowCat a=c.bossVictory.cat;
        RainbowCatArrival.stars(new OpacityPainter(p,1-BossVictory.backdropDrain(c)),a,c.bossVictory,L,
                a.clock+starTravel(c.deathDuration()-c.deathT));
    }
    static float pitch(int note) {return (float)Math.pow(2,(4-note)/12.0);}
    static float ink(int stroke,float age) {return Math.max(0,Math.min(1,(age-START[stroke])/(END[stroke]-START[stroke])));}
    static float radius(Layout L) {return Math.min(L.w*.30f,(L.dangerY-L.playTop)*.29f);}
    static float point(Layout L,int stroke,float u,int axis) {
        float r=radius(L),cx=L.w*.5f,cy=L.playTop+(L.dangerY-L.playTop)*.46f;
        if(stroke==0) {float angle=-Softbody.TAU*.25f+Softbody.TAU*u;return axis==0?cx+r*(float)Math.cos(angle):cy+r*(float)Math.sin(angle);}
        if(stroke<3)return axis==0?cx+r*((stroke==1?-.38f:.38f)+.015f*(float)Math.sin(u*Math.PI)):cy+r*(-.36f+.22f*u);
        return axis==0?cx+r*.60f*(2*u-1):cy+r*(.52f-.27f*(float)Math.sin(u*Math.PI));
    }
    private static float flowingPoint(Layout L,int stroke,float u,int axis,float age) {
        float centre=axis==0?L.w*.5f:L.playTop+(L.dangerY-L.playTop)*.46f;
        float ripple=1+.018f*(float)Math.sin(age*2.2f)
                +(stroke==1 || stroke==2?0:.018f*(float)Math.sin(u*Softbody.TAU*3-age*2.7f+stroke));
        float drift=L.w*.008f*(float)Math.sin(age*(axis==0?1.3f:1.7f)+axis);
        return centre+(point(L,stroke,u,axis)-centre)*ripple+drift;
    }
    static float flight(RainbowCat a,Layout L,float age,int axis) {
        if(age<START[0])return mix(axis==0?a.x:a.y,flowingPoint(L,0,0,axis,age),ease(age/START[0]));
        for(int stroke=0;stroke<4;stroke++) {
            if(age<=END[stroke])return flowingPoint(L,stroke,ink(stroke,age),axis,age);
            if(stroke<3 && age<START[stroke+1])return mix(flowingPoint(L,stroke,1,axis,age),flowingPoint(L,stroke+1,0,axis,age),ease((age-END[stroke])/(START[stroke+1]-END[stroke])));
        }
        float target=axis==0?L.w*.5f+radius(L)*.60f:L.playTop+(L.dangerY-L.playTop)*.46f-radius(L)*1.35f;
        return mix(flowingPoint(L,3,1,axis,age),target,ease((age-END[3])/.45f));
    }
    private static float mix(float a,float b,float u) {return a+(b-a)*u;}
    private static float ease(float u) {u=Math.max(0,Math.min(1,u));return u*u*(3-2*u);}
    static float laugh(float age) {
        float beat=(age-NOTE_START)/NOTE_STEP;
        return .45f+.55f*Math.max(0,(float)Math.sin(beat*Softbody.TAU));
    }
    private static final int[] SAMPLES={48,8,8,24};
    static final class Mesh {
        final float[][] centres=new float[4][],strips=new float[4][];
        final float[] gleam=new float[36];
        Mesh() {
            for(int i=0;i<4;i++) {
                centres[i]=new float[(SAMPLES[i]+1)*4];strips[i]=new float[(SAMPLES[i]+1)*4];
            }
        }
    }
    static void ribbons(Painter p,Layout L,float age,Mesh mesh) {
        for(int stroke=0;stroke<4;stroke++) {
            float progress=ink(stroke,age);if(progress<=0)continue;
            int samples=SAMPLES[stroke];float[] centres=mesh.centres[stroke],strip=mesh.strips[stroke];
            for(int n=0;n<=samples;n++) {
                float u=progress*n/samples;
                centres[n*4]=flowingPoint(L,stroke,u,0,age);centres[n*4+1]=flowingPoint(L,stroke,u,1,age);
            }
            for(int n=0;n<=samples;n++) {
                int prev=Math.max(0,n-1)*4,next=Math.min(samples,n+1)*4;
                if(stroke==0 && progress==1 && (n==0 || n==samples)){prev=(samples-1)*4;next=4;}
                float dx=centres[next]-centres[prev],dy=centres[next+1]-centres[prev+1];
                float width=(1+.055f*(float)Math.sin(progress*n/samples*Softbody.TAU*2-age*3+stroke))
                        /Math.max(.001f,(float)Math.hypot(dx,dy));
                centres[n*4+2]=-dy*width;centres[n*4+3]=dx*width;
            }
            // Only the outer edges need feathering; internal bands share their soft colour boundaries.
            for(int feather=2;feather>=1;feather--)for(int band=0;band<=5;band+=5) {
                float spread=feather*L.w*.0035f;
                ribbon(p,centres,strip,samples,(band-3)*L.w*.0195f-spread,
                        (band-3)*L.w*.0195f+L.w*.0198f+spread,
                        Glyph.withAlpha(RainbowCatArt.trailColor(p,band,RainbowCatArt.COLORS[band]),feather==1?40:18));
            }
            for(int band=0;band<6;band++)ribbon(p,centres,strip,samples,
                    (band-3)*L.w*.0195f,(band-3)*L.w*.0195f+L.w*.0198f,
                    Glyph.withAlpha(RainbowCatArt.trailColor(p,band,RainbowCatArt.COLORS[band]),195));
            // One soft sheen crosses all six colours, instead of rebuilding one patch per band.
            float travel=(age*3.5f-stroke*.7f)/(Softbody.TAU*2);
            for(int wave=-1;wave<=2;wave++) {
                float centre=travel-(float)Math.floor(travel*2)*.5f+wave*.5f;
                for(int feather=2;feather>=0;feather--) {
                    float half=.032f+feather*.024f;
                    if(centre+half+.018f<=0 || centre-half-.018f>=progress)continue;
                    for(int n=0;n<=8;n++)for(int edge=0;edge<2;edge++) {
                        float u=Math.max(0,Math.min(progress,centre-half+half*2*n/8+(edge==0?-.018f:.018f)));
                        float at=u/progress*samples;int first=Math.min(samples-1,(int)at);float blend=at-first;
                        int left=first*4,right=left+4;
                        float x=mix(centres[left],centres[right],blend),y=mix(centres[left+1],centres[right+1],blend);
                        float offset=L.w*(edge==0?-.0585f:.0588f),nx=mix(centres[left+2],centres[right+2],blend),ny=mix(centres[left+3],centres[right+3],blend);
                        int index=(edge==0?n:17-n)*2;
                        mesh.gleam[index]=x+nx*offset;mesh.gleam[index+1]=y+ny*offset;
                    }
                    p.fillPoly(mesh.gleam,Glyph.withAlpha(0xFFF6F2FF,feather==0?26:feather==1?16:10));
                }
            }
            for(int spark=0;spark<8;spark++) {
                float u=((spark+.5f)/8+age*.035f)%1;if(u>progress)continue;
                float pulse=.5f+.5f*(float)Math.sin(age*8+spark*2.1f+stroke);
                float x=flowingPoint(L,stroke,u,0,age),y=flowingPoint(L,stroke,u,1,age),r=L.w*(.0025f+.0035f*pulse);
                p.line(x-r,y,x+r,y,0xBBFFFFFF,L.w*.0025f);p.line(x,y-r,x,y+r,0xBBFFFFFF,L.w*.0025f);
            }
        }
    }
    private static void ribbon(Painter p,float[] centres,float[] strip,int samples,float lo,float hi,int color) {
        for(int n=0;n<=samples;n++) {
            float x=centres[n*4],y=centres[n*4+1],nx=centres[n*4+2],ny=centres[n*4+3];
            strip[n*2]=x+nx*lo;strip[n*2+1]=y+ny*lo;
            int back=(samples*2+1-n)*2;strip[back]=x+nx*hi;strip[back+1]=y+ny*hi;
        }
        p.fillPoly(strip,color);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        RainbowCat a=c.bossVictory.cat;float age=c.deathDuration()-c.deathT;
        Painter q=new OpacityPainter(p,Math.min(1,c.deathT/.55f));
        if(a.tauntMesh==null)a.tauntMesh=new Mesh();
        ribbons(q,L,age,a.tauntMesh);
        float x=flight(a,L,age,0),y=flight(a,L,age,1);
        float pulse=laugh(age),scale=.65f+.035f*pulse;
        Painter actor=new BossScreen.PortraitFit(q,a.x,a.y,x,y-L.w*.006f*pulse,scale);
        RainbowCatArt.laughingCat(actor,a,age,pulse);
    }
}
