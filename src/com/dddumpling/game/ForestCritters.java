package com.dddumpling.game;

/** Small bank animals startled by cleared words; cosmetic state never uses combat RNG. */
final class ForestCritters extends Draw {
    static final int PER_WORD=3,CAPACITY=24,RESIDENTS=9;
    static final class Critter {
        float age=-1,delay,duration,z,phase,startAlpha,bankOffset=.037f;
        int side,kind;
    }
    final Critter[] animals=new Critter[CAPACITY];
    final Critter[] residents=new Critter[RESIDENTS];
    private int next,sequence;
    private boolean settled;
    ForestCritters(){for(int i=0;i<CAPACITY;i++)animals[i]=new Critter();for(int i=0;i<RESIDENTS;i++)residents[i]=new Critter();}
    void reset(){for(Critter a:animals)a.age=-1;for(Critter a:residents)a.age=-1;next=sequence=0;settled=false;}
    static boolean enabled(GameCore c) {
        return LandBonus.scene(c) && Lands.forStage(c.stage)==MushroomForest.LAND;
    }
    void clean(GameCore c){if(!enabled(c))reset();}
    private void settle(GameCore c,Layout L) {
        if(settled)return;
        settled=true;
        for(int i=0;i<RESIDENTS;i++) {
            Critter a=residents[i];a.kind=i%3==2?1:0;a.side=hash(719+i*67)>.5f?-1:1;a.bankOffset=.037f+hash(421+i*91)*.025f;
            a.phase=hash(173+i*83)*Softbody.TAU;a.age=.7f;
            a.z=(L.playTop+(L.deckTop-L.playTop)*(.08f+.84f*(i+.15f+.70f*hash(519+i*83))/RESIDENTS)-ForestTerrain.travel(c,L))/L.w;
        }
    }
    void update(GameCore c,Layout L,float dt) {
        clean(c);if(!enabled(c))return;
        settle(c,L);
        for(Critter a:animals)if(a.age>=0) {
            a.age+=dt;if(a.age>=a.delay+a.duration)a.age=-1;
        }
        for(Critter a:residents) {
            a.age+=dt;
            if(ForestTerrain.screenY(a.z,ForestTerrain.travel(c,L),L)>L.deckTop+L.w*.05f) {
                a.z=(-L.w*.04f-ForestTerrain.travel(c,L))/L.w;a.age=0;
            }
        }
    }
    void cleared(GameCore c,Layout L,float x,float y) {
        if(!enabled(c))return;
        settle(c,L);int chosen=0;
        for(int i=0;i<PER_WORD;i++) {
            int best=-1;float nearest=Float.MAX_VALUE;
            for(int j=0;j<RESIDENTS;j++) {
                Critter a=residents[j];if((chosen&(1<<j))!=0 || a.kind!=(i==2?1:0))continue;
                float yy=ForestTerrain.screenY(a.z,ForestTerrain.travel(c,L),L);
                float distance=Math.abs(yy-y)+Math.abs(x(a,0)*L.w-x)*.25f;
                if(a.age<.3f || yy<L.playTop || yy>L.deckTop)distance+=L.h*4;
                if(distance<nearest){nearest=distance;best=j;}
            }
            chosen|=1<<best;Critter source=residents[best];
            int seed=++sequence*137+913;
            Critter a=animals[next];next=(next+1)%CAPACITY;
            a.age=0;a.delay=i*.075f+hash(seed)*.05f;a.duration=1.65f+hash(seed+7)*.65f;
            a.z=source.z;a.side=source.side;a.phase=source.phase;a.kind=source.kind;a.bankOffset=source.bankOffset;
            a.startAlpha=.72f*Math.max(0,Math.min(1,source.age/.65f));
            // Replacement residents emerge quietly from foliage after the fleeing group has passed.
            source.z=(L.playTop+(L.deckTop-L.playTop)*(.12f+hash(seed+3)*.74f)-ForestTerrain.travel(c,L))/L.w;
            source.phase=hash(seed+11)*Softbody.TAU;source.bankOffset=.037f+hash(seed+13)*.025f;source.age=-1.3f-hash(seed+7)*.7f;
        }
    }
    static float progress(Critter a){return Math.max(0,Math.min(1,(a.age-a.delay)/a.duration));}
    static float z(Critter a,float t) {
        return a.z+.065f*((float)Math.sin(a.phase+t*12)-(float)Math.sin(a.phase))
                +.10f*t*(float)Math.cos(a.phase);
    }
    static float x(Critter a,float t) {
        float zz=z(a,t),run=t*t/(.18f+.82f*t);
        return ForestTerrain.streamX(zz)+a.side*(ForestTerrain.streamWidth(zz)+a.bankOffset+.53f*run);
    }
    void draw(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0 || !enabled(c))return;
        boolean still=c.cloudUi.reduceMotion;
        if(settled)for(Critter a:residents)if(a.age>=0) {
            float time=still?0:a.age,angle=(a.side<0?(float)Math.PI:0)+.18f*(float)Math.sin(time*1.4f+a.phase);
            float r=L.w*(a.kind==1?.0125f:.0135f)*(1+.035f*(float)Math.sin(time*2+a.phase));
            float xx=x(a,0)*L.w,yy=ForestTerrain.screenY(a.z,ForestTerrain.travel(c,L),L);
            float alpha=fade*.72f*Math.min(1,a.age/.65f);
            p.fillEllipse(xx,yy+r*.5f,r*1.2f,r*.64f,Glyph.withAlpha(0xFF152D23,(int)(alpha*80)));
            animal(p,xx,yy,r,(float)Math.cos(angle),(float)Math.sin(angle),
                    still?0:.09f*(float)Math.sin(time*3+a.phase),a.kind,alpha);
        }
        for(Critter a:animals) {
            float age=a.age-a.delay;if(a.age<0)continue;
            float t=progress(a),move=still?0:t;
            float opacity=fade*Math.max(a.startAlpha,Math.max(0,Math.min(1,age/.10f)))*Math.min(1,(a.duration-age)/.30f);
            if(opacity<=0)continue;
            float xx=x(a,move)*L.w,yy=ForestTerrain.screenY(z(a,move),ForestTerrain.travel(c,L),L);
            float ahead=Math.min(1,move+.008f),behind=Math.max(0,move-.008f);
            float dx=x(a,ahead)-x(a,behind),dy=z(a,ahead)-z(a,behind);
            float length=(float)Math.sqrt(dx*dx+dy*dy),ux=length>0?dx/length:a.side,uy=length>0?dy/length:0;
            float r=L.w*(a.kind==1?.0125f:.0135f),stride=still || age<0?0:(float)Math.sin(age*(a.kind==1?20:35)+a.phase);
            float hop=a.kind==1 && !still?Math.max(0,stride)*r*.5f:0;
            p.fillEllipse(xx,yy+r*.5f,r*1.2f,r*.64f,Glyph.withAlpha(0xFF152D23,(int)(opacity*80)));
            animal(p,xx,yy-hop,r,ux,uy,stride,a.kind,opacity);
        }
    }
    private static void animal(Painter p,float x,float y,float r,float ux,float uy,float stride,int kind,float fade) {
        int body=Glyph.withAlpha(kind==1?0xFF7F9A68:0xFFA3957A,(int)(fade*210));
        int shade=Glyph.withAlpha(kind==1?0xFF526F4B:0xFF6C6756,(int)(fade*210));
        int light=Glyph.withAlpha(kind==1?0xFFB1BC87:0xFFC4B79A,(int)(fade*150));
        if(kind==0) {
            float bend=stride*.35f;
            p.polyline(new float[]{x-ux*r*.6f,y-uy*r*.6f,
                    x-ux*r*1.6f-uy*r*bend,y-uy*r*1.6f+ux*r*bend,
                    x-ux*r*2.3f-uy*r*(bend+.4f),y-uy*r*2.3f+ux*r*(bend+.4f)},shade,r*.21f);
        }
        for(int side=-1;side<=1;side+=2) {
            float kick=stride*side;
            dot(p,x,y,r,ux,uy,-.35f+kick*.3f,side*(kind==1?.85f:.67f),kind==1?.43f:.24f,shade);
            dot(p,x,y,r,ux,uy,.6f-kick*.23f,side*.58f,.20f,body);
        }
        dot(p,x,y,r,ux,uy,-.15f,0,.80f,body);
        dot(p,x,y,r,ux,uy,-.35f,-.18f,.47f,light);
        dot(p,x,y,r,ux,uy,.62f,0,.57f,body);
        for(int side=-1;side<=1;side+=2) {
            dot(p,x,y,r,ux,uy,kind==1?.80f:.28f,side*.50f,kind==1?.28f:.33f,body);
            if(kind==0)dot(p,x,y,r,ux,uy,.28f,side*.50f,.18f,light);
            dot(p,x,y,r,ux,uy,.92f,side*.29f,.11f,Glyph.withAlpha(0xFF23362D,(int)(fade*235)));
        }
        if(kind==0)dot(p,x,y,r,ux,uy,1.20f,0,.13f,shade);
    }
    private static void dot(Painter p,float x,float y,float r,float ux,float uy,float along,float across,float size,int color) {
        p.fillCircle(x+r*(ux*along-uy*across),y+r*(uy*along+ux*across),r*size,color);
    }
}
