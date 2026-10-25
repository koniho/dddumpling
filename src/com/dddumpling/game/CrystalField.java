package com.dddumpling.game;

import java.util.ArrayList;

/** Periodic Voronoi gemstone floor. Geometry is rebuilt only when the viewport changes. */
final class CrystalField extends Draw {
    static final int LAND=1, COUNT=18;
    static final float PERIOD=2f;
    static final float[] SPEED={.012f,.023f,.041f};
    static final int LAYERS=3, STAMP_WIDTH=480, STAMP_HEIGHT=960;
    static final float CLEAR_TIME=1.05f;
    static final int MAX_LIGHTS=6;
    private final Pulse[] lights=new Pulse[MAX_LIGHTS+Boss.DIVIDE_PIECES*2];
    private int nextLight,arenaLightCount;
    final CrystalArrival arrival=new CrystalArrival();
    private float width;
    static final class Pulse {
        float born=-100,sky,x,y,rx,held=-1;
        int color;
    }
    CrystalField(){for(int i=0;i<lights.length;i++)lights[i]=new Pulse();}
    void reset(){for(Pulse light:lights){light.born=-100;light.held=-1;}nextLight=arenaLightCount=0;arrival.reset();}
    void cleared(GameCore c,Layout L,float x,float y,int length,int color) {
        if(c.state!=GameCore.PLAY || c.survival.active || Lands.forStage(c.stage)!=LAND)return;
        flash(c,L,x,y,length,color);
    }
    void flash(GameCore c,Layout L,float x,float y,int length,int color) {
        Pulse light=lights[nextLight];nextLight=(nextLight+1)%MAX_LIGHTS;
        light.born=c.clock;light.sky=c.skyClock;light.x=x/L.w;light.y=y/L.w;
        light.rx=Math.min(.46f,Math.max(.23f,length*L.enemyStep/L.w*.55f));
        light.color=Glyph.mix(0xFFE2CEFF,color,.28f);
    }
    void arenaFlash(GameCore c,Layout L,float x,float y,int length,int color) {
        flash(c,L,x,y,length,color);flash(c,L,L.w-x,y,length,color);
    }
    void prepareArenaLights(GameCore c,Layout L) {
        arenaLightCount=0;
        Boss b=c.boss;
        if(c.state!=GameCore.PLAY || !b.fighting() || b.kind!=Boss.SPLITTER)return;
        float power=BossScreen.vulnerabilityPulse(c.clock)*.70f;
        for(int i=0;i<b.pieceCount();i++)if(BossScreen.divideVulnerable(b,i)) {
            Softbody body=b.pieceBody(i);if(body==null)continue;
            for(int side=0;side<2;side++) {
                Pulse light=lights[MAX_LIGHTS+arenaLightCount++];
                light.x=(side==0?body.centreX():L.w-body.centreX())/L.w;light.y=body.centreY()/L.w;
                light.rx=Math.max(.18f,Math.min(.42f,b.pieceR(i,L)*1.8f/L.w));
                light.sky=c.skyClock;light.held=power;light.color=Draw.YELLOW;
            }
        }
    }
    private static float strength(Pulse light,float clock) {
        if(light.held>=0)return light.held;
        float age=clock-light.born;if(age<0 || age>=CLEAR_TIME)return 0;
        float fade=1-age/CLEAR_TIME;return fade*fade;
    }
    boolean hasLights(GameCore c) {
        for(int i=0;i<MAX_LIGHTS+arenaLightCount;i++)if(strength(lights[i],c.clock)>.015f)return true;
        return false;
    }
    private static float lightY(Pulse light,GameCore c,int layer) {
        // Follow the lit crystal rather than leaving the light fixed on the screen.
        return light.y+(c.skyClock-light.sky)*SPEED[layer];
    }
    float lightAt(GameCore c,Layout L,int layer,float x,float y) {
        float best=0;
        for(int lightIndex=0;lightIndex<MAX_LIGHTS+arenaLightCount;lightIndex++) {
            Pulse light=lights[lightIndex];
            if(strength(light,c.clock)<=0)continue;
            float dx=(x/L.w-light.x)/light.rx,dy=(y/L.w-lightY(light,c,layer))/.27f;
            best=Math.max(best,strength(light,c.clock)*Math.max(0,1-dx*dx-dy*dy));
        }
        return best;
    }
    Cell[][] layers;
    static final class Cell {
        float[] rim,face;
        float[][] sheen;
        float top,bottom,phase;
        int color;
    }
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float blend=Lands.blend(c);
        return (c.landFrom==LAND?1-blend:0)+(Lands.forStage(Lands.visualStage(c))==LAND?blend:0);
    }
    static float scroll(float clock,float width,int layer) {return ((clock*SPEED[layer]+layer*.37f)%PERIOD)*width;}

    void prepare(float w) {
        if(layers!=null && width==w)return;
        width=w;layers=new Cell[LAYERS][];
        for(int layer=0;layer<LAYERS;layer++)layers[layer]=make(w,layer);
    }
    private static Cell[] make(float w,int layer) {
        Cell[] cells=new Cell[COUNT];
        int seed=layer*7103;
        float[] sx=new float[COUNT],sy=new float[COUNT];
        for(int i=0;i<COUNT;i++) {
            sx[i]=(i%3+.12f+.76f*hash(seed+4103+i*47))/3;
            sy[i]=(i/3+.12f+.76f*hash(seed+5231+i*61))/3;
        }
        for(int i=0;i<COUNT;i++) {
            float[] poly={-.08f,sy[i]-1,1.08f,sy[i]-1,1.08f,sy[i]+1,-.08f,sy[i]+1};
            for(int repeat=-1;repeat<=1;repeat++)for(int j=0;j<COUNT;j++) {
                if(i==j && repeat==0)continue;
                float x=sx[j],y=sy[j]+repeat*PERIOD;
                poly=clip(poly,x-sx[i],y-sy[i],(x*x+y*y-sx[i]*sx[i]-sy[i]*sy[i])*.5f);
            }
            Cell cell=cells[i]=new Cell();
            cell.rim=poly;cell.face=new float[poly.length];
            cell.sheen=new float[8][];
            cell.top=Float.MAX_VALUE;cell.bottom=-Float.MAX_VALUE;
            cell.phase=hash(seed+891+i*37)*Softbody.TAU;
            cell.color=Glyph.mix(0xFF43315F,0xFF71588C,hash(1831+i*53));
            float inset=.94f;
            for(int k=0;k<poly.length;k+=2) {
                float x=poly[k],y=poly[k+1];
                cell.face[k]=(sx[i]+(x-sx[i])*inset)*w;
                cell.face[k+1]=(sy[i]+(y-sy[i])*inset)*w;
                poly[k]*=w;poly[k+1]*=w;
                cell.top=Math.min(cell.top,poly[k+1]);cell.bottom=Math.max(cell.bottom,poly[k+1]);
            }
            float lo=Float.MAX_VALUE,hi=-Float.MAX_VALUE;
            for(int k=0;k<cell.face.length;k+=2) {
                float diagonal=cell.face[k]+cell.face[k+1]*.65f;
                lo=Math.min(lo,diagonal);hi=Math.max(hi,diagonal);
            }
            // Preclip eight broad diagonal strips: animated sheen never rebuilds paths.
            for(int band=0;band<cell.sheen.length;band++) {
                float start=lo+(hi-lo)*band/cell.sheen.length,end=lo+(hi-lo)*(band+1)/cell.sheen.length;
                cell.sheen[band]=clip(clip(cell.face,1,.65f,end),-1,-.65f,-start);
            }
        }
        return cells;
    }
    /** Clip to the half-plane closest to this seed; no gameplay RNG is consumed. */
    static float[] clip(float[] p,float nx,float ny,float d) {
        ArrayList<Float> out=new ArrayList<>();
        for(int k=0;k<p.length;k+=2) {
            int prev=(k+p.length-2)%p.length;
            float ax=p[prev],ay=p[prev+1],bx=p[k],by=p[k+1];
            float a=ax*nx+ay*ny-d,b=bx*nx+by*ny-d;
            if((a<=0)!=(b<=0)) {
                float t=a/(a-b);out.add(ax+(bx-ax)*t);out.add(ay+(by-ay)*t);
            }
            if(b<=0){out.add(bx);out.add(by);}
        }
        float[] result=new float[out.size()];
        for(int k=0;k<result.length;k++)result[k]=out.get(k);
        return result;
    }
    void edgeLights(Painter p,GameCore c,Layout L,Cell cell,int layer,float y,float fade) {
        for(int k=0;k<cell.rim.length;k+=2) {
            int n=(k+2)%cell.rim.length;
            float ax=cell.rim[k],ay=cell.rim[k+1],bx=cell.rim[n],by=cell.rim[n+1];
            float best=0,from=0,to=0;int color=0;
            for(int lightIndex=0;lightIndex<MAX_LIGHTS+arenaLightCount;lightIndex++) {
                Pulse light=lights[lightIndex];
                float power=strength(light,c.clock);if(power<=.015f)continue;
                float rx=light.rx*L.w,ry=L.w*.27f;
                float lx=light.x*L.w,ly=lightY(light,c,layer)*L.w-y;
                if(Math.max(ax,bx)<lx-rx || Math.min(ax,bx)>lx+rx
                        || Math.max(ay,by)<ly-ry || Math.min(ay,by)>ly+ry)continue;
                float dx=(bx-ax)/rx,dy=(by-ay)/ry;
                float px=(ax-lx)/rx,py=(ay-ly)/ry;
                float len=dx*dx+dy*dy;if(len<.00001f)continue;
                float projection=-(px*dx+py*dy)/len,t=Math.max(0,Math.min(1,projection));
                float nx=px+dx*t,ny=py+dy*t,near=nx*nx+ny*ny;
                float amount=power*Math.max(0,1-near);if(amount<=best)continue;
                float ex=px+dx*projection,ey=py+dy*projection;
                float half=(float)Math.sqrt(Math.max(0,(1-ex*ex-ey*ey)/len));
                best=amount;from=Math.max(0,projection-half);to=Math.min(1,projection+half);color=light.color;
            }
            if(best<=.025f)continue;
            float dx=bx-ax,dy=by-ay,scale=fade*best;
            float x1=ax+dx*from,y1=ay+dy*from,x2=ax+dx*to,y2=ay+dy*to;
            float soft=layer==0?.036f:layer==1?.025f:.016f;
            p.line(x1,y1,x2,y2,Glyph.withAlpha(color,(int)(scale*24)),L.w*soft);
            p.line(x1,y1,x2,y2,Glyph.withAlpha(color,(int)(scale*57)),L.w*soft*.48f);
            p.line(x1,y1,x2,y2,Glyph.withAlpha(color,(int)(scale*(layer==0?105:layer==1?150:205))),
                    L.w*(layer==0?.008f:layer==1?.005f:.003f));
        }
    }
    private static int edgeColor(int layer) {
        return layer==0?0xFF9983BE:layer==1?0xFFB79BD8:0xFFDCC4FF;
    }
    static void base(Painter p,Cell cell,int layer,float w,float fade) {
        base(p,cell,layer,w,fade,1);
    }
    static void base(Painter p,Cell cell,int layer,float w,float fade,float edgeGain) {
        int color=edgeColor(layer);
        p.fillPoly(cell.rim,Glyph.withAlpha(cell.color,(int)(fade*(4+layer*3))));
        fade*=edgeGain;
        float softness=layer==0?.030f:layer==1?.017f:.009f;
        p.strokePoly(cell.rim,Glyph.withAlpha(color,(int)(fade*(3+layer*2))),w*softness);
        p.strokePoly(cell.rim,Glyph.withAlpha(color,(int)(fade*(5+layer*3))),w*softness*.48f);
        p.strokePoly(cell.rim,Glyph.withAlpha(color,(int)(fade*(layer==0?12:layer==1?28:53)*.9f)),
                w*(layer==0?.006f:layer==1?.0035f:.0022f));
    }
    /** One immutable repeating tile per depth, including wrapped edge halos. */
    static void paintLayer(Painter p,int layer) {
        if(!CrystalArena.validArt(layer))return;
        if(layer>=LAYERS){CrystalArena.paintLayer(p,layer%LAYERS,layer/LAYERS-1);return;}
        Cell[] cells=make(STAMP_WIDTH,layer);
        p.save();p.clipRect(0,0,STAMP_WIDTH,STAMP_HEIGHT);
        for(int tile=-1;tile<=1;tile++) {
            p.save();p.translate(0,tile*STAMP_HEIGHT);
            for(Cell cell:cells)if(cell.bottom+tile*STAMP_HEIGHT>=-STAMP_WIDTH*.04f
                    && cell.top+tile*STAMP_HEIGHT<=STAMP_HEIGHT+STAMP_WIDTH*.04f)
                base(p,cell,layer,STAMP_WIDTH,1);
            p.restore();
        }
        p.restore();
    }
    void draw(Painter p,GameCore c,Layout L,float fade) {
        prepareArenaLights(c,L);
        if(fade<=.001f)return;
        if(CrystalArena.active(c)) {
            Boss b=BossVictory.sceneBoss(c);
            if(b.intro>0) {
                float t=b.introProgress(),mix=CrystalArrival.blend(t);
                arrival.prepare(this,c.crystalArena,L,c.skyClock-(b.introDuration()-b.intro),t);
                if(mix<.999f)drawStage(p,c,L,fade*(1-mix),false);
                if(mix>.001f)c.crystalArena.draw(p,c,L,fade*mix);
                arrival.draw(p,L,t,c.skyClock,fade);
            } else {arrival.reset();c.crystalArena.draw(p,c,L,fade);}
            return;
        }
        arrival.reset();drawStage(p,c,L,fade,true);
    }
    private void drawStage(Painter p,GameCore c,Layout L,float fade,boolean animate) {
        prepare(L.w);
        float period=PERIOD*L.w;
        boolean lighting=hasLights(c);
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int layer=0;layer<LAYERS;layer++) {
            float offset=scroll(c.skyClock,L.w,layer);
            int edgeColor=edgeColor(layer);
            for(int tile=-1;tile<=(int)(L.deckTop/period)+1;tile++) {
                float y=offset+tile*period;
                ArtStamp cache=p.stamps();
                boolean cached=cache!=null && (y+period<0 || y>L.deckTop || cache.crystal(layer,0,y,L.w,period,(int)(fade*255)));
                p.save();p.translate(0,y);
                for(Cell cell:layers[layer]) {
                    if(cell.bottom+y<0 || cell.top+y>L.deckTop)continue;
                    if(!cached)base(p,cell,layer,L.w,fade);
                    if(animate && lighting)edgeLights(p,c,L,cell,layer,y,fade);
                    if(!animate)continue;
                    float sweep=((c.clock*.16f+cell.phase/Softbody.TAU)%1)*13-2.5f;
                    for(int band=0;band<cell.sheen.length;band++) {
                        float distance=Math.abs(band-sweep)/1.8f;
                        if(distance>=1 || cell.sheen[band].length<6)continue;
                        float glow=(1-distance*distance);glow*=glow;
                        p.fillPoly(cell.sheen[band],Glyph.withAlpha(edgeColor,(int)(fade*glow*(5+layer*5))));
                    }
                }
                p.restore();
            }
        }
        p.restore();
    }
}
