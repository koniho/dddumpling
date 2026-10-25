package com.dddumpling.game;

import java.util.ArrayList;

/** Periodic Voronoi gemstone floor. Geometry is rebuilt only when the viewport changes. */
final class CrystalField extends Draw {
    static final int LAND=1, COUNT=18;
    static final float PERIOD=2f;
    static final float[] SPEED={.012f,.023f,.041f};
    static final int LAYERS=3;
    private float width;
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
    private static float[] clip(float[] p,float nx,float ny,float d) {
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
    void draw(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=.001f)return;
        prepare(L.w);
        float period=PERIOD*L.w;
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int layer=0;layer<LAYERS;layer++) {
            float offset=scroll(c.skyClock,L.w,layer);
            int edgeColor=layer==0?0xFF9983BE:layer==1?0xFFB79BD8:0xFFDCC4FF;
            for(int tile=-1;tile<=(int)(L.deckTop/period)+1;tile++) {
                float y=offset+tile*period;
                p.save();p.translate(0,y);
                for(Cell cell:layers[layer]) {
                    if(cell.bottom+y<0 || cell.top+y>L.deckTop)continue;
                    float shimmer=.5f+.5f*(float)Math.sin(c.clock*.72f+cell.phase);
                    p.fillPoly(cell.rim,Glyph.withAlpha(cell.color,(int)(fade*(4+layer*3))));
                    // Broad low-alpha strokes soften distant layers without blur surfaces.
                    float softness=layer==0?.030f:layer==1?.017f:.009f;
                    p.strokePoly(cell.rim,Glyph.withAlpha(edgeColor,(int)(fade*(3+layer*2))),L.w*softness);
                    p.strokePoly(cell.rim,Glyph.withAlpha(edgeColor,(int)(fade*(5+layer*3))),L.w*softness*.48f);
                    p.strokePoly(cell.rim,Glyph.withAlpha(edgeColor,(int)(fade*(layer==0?12:layer==1?28:53)*( .8f+shimmer*.2f))),
                            L.w*(layer==0?.006f:layer==1?.0035f:.0022f));
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
