package com.dddumpling.game;

import java.util.ArrayList;

/** Mirrored crystal chamber: each completed pinch fractures the tiles symmetrically. */
final class CrystalArena extends Draw {
    static final int MAX_PHASE=Boss.DIVIDE_NODES, START=6;
    private int phase=-1;
    private float width;
    CrystalField.Cell[][] cells;
    static boolean active(GameCore c){return c.boss.active() && c.boss.kind==Boss.SPLITTER;}
    static int phase(Boss b){return Math.min(MAX_PHASE,b.divideSplits+Integer.bitCount(b.divideDead));}
    static int art(int layer,int phase){return CrystalField.LAYERS*(phase+1)+layer;}
    static boolean validArt(int art){return art>=0 && art<CrystalField.LAYERS*(MAX_PHASE+2);}
    void prepare(float w,int next) {
        if(cells!=null && width==w && phase==next)return;
        width=w;phase=next;cells=new CrystalField.Cell[CrystalField.LAYERS][];
        for(int layer=0;layer<cells.length;layer++)cells[layer]=make(w,layer,next);
    }
    private static float area(float[] p) {
        float a=0;
        for(int i=0;i<p.length;i+=2){int j=(i+2)%p.length;a+=p[i]*p[j+1]-p[j]*p[i+1];}
        return Math.abs(a)*.5f;
    }
    static CrystalField.Cell[] make(float w,int layer,int phase) {
        ArrayList<float[]> pieces=new ArrayList<>();
        float[] sx=new float[START],sy=new float[START];
        for(int i=0;i<START;i++) {
            sx[i]=.25f*(i%2+.15f+.7f*hash(1351+layer*113+i*71));
            sy[i]=2f*(i/2+.15f+.7f*hash(872+layer*173+i*47))/(START/2);
        }
        for(int i=0;i<START;i++) {
            float[] p={0,0,.5f,0,.5f,2,0,2};
            for(int j=0;j<START;j++)if(j!=i)
                p=CrystalField.clip(p,sx[j]-sx[i],sy[j]-sy[i],(sx[j]*sx[j]+sy[j]*sy[j]-sx[i]*sx[i]-sy[i]*sy[i])*.5f);
            pieces.add(p);
        }
        for(int split=0;split<phase;split++) {
            int largest=0;
            for(int i=1;i<pieces.size();i++)if(area(pieces.get(i))>area(pieces.get(largest)))largest=i;
            float[] p=pieces.remove(largest);float x=0,y=0;
            for(int i=0;i<p.length;i+=2){x+=p[i];y+=p[i+1];}x/=p.length/2;y/=p.length/2;
            float angle=.55f+hash(split*71+layer*131+420)*2f,nx=(float)Math.cos(angle),ny=(float)Math.sin(angle);
            pieces.add(CrystalField.clip(p,nx,ny,nx*x+ny*y));
            pieces.add(CrystalField.clip(p,-nx,-ny,-nx*x-ny*y));
        }
        ArrayList<CrystalField.Cell> result=new ArrayList<>();
        for(int i=0;i<pieces.size();i++) {
            float[] source=pieces.get(i),joined=joinCenter(source);
            for(int side=0;side<(joined==null?2:1);side++) {
                float[] contour=joined==null?source:joined;
                CrystalField.Cell cell=new CrystalField.Cell();result.add(cell);
                cell.rim=new float[contour.length];cell.face=new float[contour.length];
                cell.top=Float.MAX_VALUE;cell.bottom=-Float.MAX_VALUE;
                cell.phase=hash(i*71+layer*137)*Softbody.TAU;cell.color=0xFF8062B3;
                for(int j=0;j<contour.length;j+=2) {
                    cell.rim[j]=(side==0?contour[j]:1-contour[j])*w;cell.rim[j+1]=contour[j+1]*w;
                    cell.top=Math.min(cell.top,cell.rim[j+1]);cell.bottom=Math.max(cell.bottom,cell.rim[j+1]);
                }
            }
        }
        return result.toArray(new CrystalField.Cell[0]);
    }
    // Join across the mirror axis, following the outside contour instead of stroking a seam.
    private static float[] joinCenter(float[] source) {
        int n=source.length/2;
        for(int edge=0;edge<n;edge++) {
            int next=(edge+1)%n;
            if(Math.abs(source[edge*2]-.5f)>.000001f || Math.abs(source[next*2]-.5f)>.000001f)continue;
            float[] joined=new float[(n*2-2)*2];
            for(int i=0;i<n;i++) {
                int j=(next+i)%n;
                joined[i*2]=source[j*2];joined[i*2+1]=source[j*2+1];
            }
            int out=n;
            for(int i=n-2;i>0;i--,out++) {
                joined[out*2]=1-joined[i*2];joined[out*2+1]=joined[i*2+1];
            }
            return joined;
        }
        return null;
    }
    static void paintLayer(Painter p,int layer,int phase) {
        p.save();p.clipRect(0,0,CrystalField.STAMP_WIDTH,CrystalField.STAMP_HEIGHT);
        for(CrystalField.Cell cell:make(CrystalField.STAMP_WIDTH,layer,phase))
            CrystalField.base(p,cell,layer,CrystalField.STAMP_WIDTH,1);
        p.restore();
    }
    void draw(Painter p,GameCore c,Layout L,float fade) {
        Boss b=c.boss;prepare(L.w,phase(b));
        float burst=b.beaten?Math.max(0,DivideDeath.elapsed(b)-DivideDeath.BURST_AT):0;
        float period=CrystalField.PERIOD*L.w;
        boolean lighting=c.crystalField.hasLights(c);
        // Freeze the pattern at the burst so every visible tile leaves its actual position.
        float clock=c.skyClock-burst;
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int layer=0;layer<cells.length;layer++) {
            float offset=CrystalField.scroll(clock,L.w,layer);
            for(int tile=-1;tile<=(int)(L.deckTop/period);tile++) {
                float y=offset+tile*period;if(y+period<0 || y>L.deckTop)continue;
                ArtStamp cache=p.stamps();
                boolean cached=burst==0 && cache!=null && cache.crystal(art(layer,phase),0,y,L.w,period,(int)(fade*255));
                p.save();p.translate(0,y);
                for(int i=0;i<cells[layer].length;i++) {
                    CrystalField.Cell cell=cells[layer][i];
                    if(cell.bottom+y<0 || cell.top+y>L.deckTop)continue;
                    if(burst>0){shatter(p,cell,layer,y,burst,L,fade);continue;}
                    if(!cached)CrystalField.base(p,cell,layer,L.w,fade);
                    if(lighting)c.crystalField.edgeLights(p,c,L,cell,layer,y,fade);
                    float glint=Math.max(0,(float)Math.sin(c.clock*.8f+cell.phase));
                    if(glint>.65f)p.fillPoly(cell.rim,Glyph.withAlpha(0xFFBB99EE,(int)(fade*(glint-.65f)*(12+layer*8))));
                }
                p.restore();
            }
        }
        p.restore();
    }
    private void shatter(Painter p,CrystalField.Cell cell,int layer,float offset,float t,Layout L,float fade) {
        float cx=0,cy=0;int n=cell.rim.length/2;
        for(int i=0;i<n;i++){cx+=cell.rim[i*2];cy+=cell.rim[i*2+1];}cx/=n;cy/=n;
        // Center-spanning tiles fall straight down, preserving symmetry during the burst.
        float side=Math.abs(cx-L.w*.5f)<.001f?0:Math.signum(cx-L.w*.5f),seed=cell.phase/Softbody.TAU;
        float dx=side*L.w*(.32f+seed*.5f)*t,dy=(cy+offset-L.deckTop*.5f)*t*.9f+L.h*.16f*t*t;
        float angle=side*t*(.6f+seed*1.8f),cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
        float alpha=fade*Math.max(0,1-t/1.25f);
        if(alpha<=0)return;
        // Reused scratch contour; pinching only increases the bounded tile count.
        float[] shape=cell.face;
        if(shape==null || shape.length!=cell.rim.length)shape=cell.face=new float[cell.rim.length];
        for(int i=0;i<n;i++) {
            float x=cell.rim[i*2]-cx,y=cell.rim[i*2+1]-cy;
            shape[i*2]=cx+dx+x*cs-y*sn;shape[i*2+1]=cy+dy+x*sn+y*cs;
        }
        p.fillPoly(shape,Glyph.withAlpha(0xFF9670CD,(int)(alpha*22)));
        p.strokePoly(shape,Glyph.withAlpha(0xFFE3C8FF,(int)(alpha*(48+layer*24))),L.w*(.004f-layer*.0008f));
    }
}
