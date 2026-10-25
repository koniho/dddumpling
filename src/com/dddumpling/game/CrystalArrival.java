package com.dddumpling.game;

import java.util.ArrayList;

/** Bounded edge choreography over the cached land and arena textures. */
final class CrystalArrival extends Draw {
    static final int PER_LAYER=40;
    private float width,height,start,progress;
    Edge[] edges;
    static final class Edge {
        float ax,ay,bx,by,tx,ty,ux,uy;
        int layer;
        boolean matched,departing;
        Edge(float a,float b,float c,float d,int depth){ax=a;ay=b;bx=c;by=d;layer=depth;}
    }
    void reset(){edges=null;}
    static float blend(float t){return panelTravel((t-.12f)/.76f);}
    void prepare(CrystalField field,CrystalArena arena,Layout L,float skyStart,float t) {
        boolean reuse=edges!=null && width==L.w && height==L.deckTop && t>=progress;
        progress=t;if(reuse)return;
        width=L.w;height=L.deckTop;start=skyStart;
        field.prepare(L.w);arena.prepare(L.w,0);
        ArrayList<Edge> motion=new ArrayList<>();
        // The distant layer crossfades softly; only the two near layers need moving highlights.
        for(int layer=1;layer<CrystalField.LAYERS;layer++) {
            ArrayList<Edge> source=visible(field.layers[layer],L,skyStart,layer);
            ArrayList<Edge> target=visible(arena.cells[layer],L,skyStart,layer);
            boolean[] used=new boolean[source.size()];int count=0;
            for(Edge dest:target) {
                if(count>=PER_LAYER-8)break;
                int best=-1;float score=L.w*L.w*.3f;boolean reverse=false;
                for(int i=0;i<source.size();i++)if(!used[i]) {
                    Edge old=source.get(i);
                    float dx=old.bx-old.ax,dy=old.by-old.ay,tx=dest.bx-dest.ax,ty=dest.by-dest.ay;
                    float dot=dx*tx+dy*ty;
                    if(dot*dot<(dx*dx+dy*dy)*(tx*tx+ty*ty)*.5f)continue;
                    float forward=distance(old.ax,old.ay,dest.ax,dest.ay)+distance(old.bx,old.by,dest.bx,dest.by);
                    float backward=distance(old.ax,old.ay,dest.bx,dest.by)+distance(old.bx,old.by,dest.ax,dest.ay);
                    float cost=Math.min(forward,backward);
                    if(cost<score){best=i;score=cost;reverse=backward<forward;}
                }
                Edge edge;
                if(best>=0) {
                    edge=source.get(best);used[best]=true;edge.matched=true;
                } else edge=new Edge(dest.ax,dest.ay,dest.ax,dest.ay,layer);
                edge.tx=reverse?dest.bx:dest.ax;edge.ty=reverse?dest.by:dest.ay;
                edge.ux=reverse?dest.ax:dest.bx;edge.uy=reverse?dest.ay:dest.by;
                motion.add(edge);count++;
            }
            for(int i=0;i<source.size() && count<PER_LAYER;i++)if(!used[i]) {
                Edge edge=source.get(i);edge.departing=true;motion.add(edge);count++;
            }
        }
        edges=motion.toArray(new Edge[0]);
    }
    private static float distance(float x,float y,float xx,float yy){return (x-xx)*(x-xx)+(y-yy)*(y-yy);}
    private static ArrayList<Edge> visible(CrystalField.Cell[] cells,Layout L,float sky,int layer) {
        ArrayList<Edge> result=new ArrayList<>();float period=CrystalField.PERIOD*L.w;
        float scroll=CrystalField.scroll(sky,L.w,layer);
        for(int tile=-1;tile<=(int)(L.deckTop/period)+1;tile++)for(CrystalField.Cell cell:cells) {
            float y=scroll+tile*period;
            if(cell.bottom+y<0 || cell.top+y>L.deckTop)continue;
            for(int k=0;k<cell.rim.length;k+=2) {
                int next=(k+2)%cell.rim.length;
                Edge edge=clipped(cell.rim[k],cell.rim[k+1]+y,cell.rim[next],cell.rim[next+1]+y,L,layer);
                if(edge==null)continue;
                boolean duplicate=false;
                for(Edge other:result)
                    if((distance(edge.ax,edge.ay,other.ax,other.ay)<.04f && distance(edge.bx,edge.by,other.bx,other.by)<.04f)
                            || (distance(edge.ax,edge.ay,other.bx,other.by)<.04f && distance(edge.bx,edge.by,other.ax,other.ay)<.04f)) {duplicate=true;break;}
                if(!duplicate)result.add(edge);
            }
        }
        return result;
    }
    private static Edge clipped(float ax,float ay,float bx,float by,Layout L,int layer) {
        float dx=bx-ax,dy=by-ay,lo=0,hi=1;
        for(int side=0;side<4;side++) {
            float p=side==0?-dx:side==1?dx:side==2?-dy:dy;
            float q=side==0?ax:side==1?L.w-ax:side==2?ay:L.deckTop-ay;
            if(Math.abs(p)<.00001f){if(q<0)return null;continue;}
            float t=q/p;if(p<0)lo=Math.max(lo,t);else hi=Math.min(hi,t);
            if(lo>=hi)return null;
        }
        if((dx*dx+dy*dy)*(hi-lo)*(hi-lo)<4)return null;
        return new Edge(ax+dx*lo,ay+dy*lo,ax+dx*hi,ay+dy*hi,layer);
    }
    void draw(Painter p,Layout L,float t,float sky,float fade) {
        if(edges==null || t<=0 || t>=1)return;
        float move=blend(t),pulse=(float)Math.sin(t*Math.PI);
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(Edge e:edges) {
            float alpha=fade*pulse*(e.layer==1?.55f:1);
            float ax=e.ax,ay=e.ay,bx=e.bx,by=e.by;
            if(e.departing)alpha*=1-panelTravel(t/.55f);
            else {
                ax+=(e.tx-ax)*move;ay+=(e.ty-ay)*move;
                bx+=(e.ux-bx)*move;by+=(e.uy-by)*move;
                if(!e.matched)alpha*=move;
            }
            if(alpha<.015f)continue;
            float drift=(sky-start)*CrystalField.SPEED[e.layer]*L.w;ay+=drift;by+=drift;
            if(Math.min(ay,by)>L.deckTop || Math.max(ay,by)<0)continue;
            p.line(ax,ay,bx,by,Glyph.withAlpha(0xFFBDA2FF,(int)(alpha*28)),L.w*.016f);
            p.line(ax,ay,bx,by,Glyph.withAlpha(0xFFF2DDFF,(int)(alpha*135)),L.w*.003f);
        }
        p.restore();
    }
}
