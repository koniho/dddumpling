package com.dddumpling.game;

/** The lantern colors physical floor and wall facets; empty rock stays dark. */
final class CaveTerrain extends Draw {
    private CaveTerrain() {}
    static void draw(Painter p,Cave v,Layout L) {
        for(int layer=0;layer<3;layer++) {
            float from=0;
            for(int fork=0;fork<3;fork++) {
                passage(p,v,L,from,Cave.FORKS[fork],-1,0,layer);
                for(int side=-1;side<=1;side+=2)passage(p,v,L,Cave.FORKS[fork],Cave.FORKS[fork]+1.4f,fork,side,layer);
                from=Cave.FORKS[fork]+1.4f;
            }
            passage(p,v,L,from,Cave.LENGTH,-1,0,layer);
        }
        float x=v.screenX(v.pathX(Cave.LENGTH),L),y=v.screenY(Cave.LENGTH,L);
        if(Cave.LENGTH-v.z<1.5f && Math.abs(x-L.w*.5f)<L.w && y>L.playTop-L.w*.2f && y<L.deckTop+L.w*.2f) {
            CaveArt.entrance(p,x,y,L.w*.105f*v.zoom(),255,false);
            p.fillEllipse(x,y+L.w*.025f,L.w*.04f,L.w*.075f,Glyph.withAlpha(CaveArt.LAMP,175));
        }
    }
    private static float x(Cave v,int fork,int side,float at){return fork<0?v.centre(at):v.branchX(fork,side,at);}
    private static float y(Cave v,int fork,int side,float at){return fork<0?v.route.y(at):v.route.branchY(fork,side,at);}
    private static float[] edge(Cave v,Layout L,int fork,int side,float at,float width) {
        float dx=x(v,fork,side,at+.012f)-x(v,fork,side,at-.012f);
        float dy=y(v,fork,side,at+.012f)-y(v,fork,side,at-.012f);
        float length=Math.max(.0001f,(float)Math.hypot(dx,dy));
        float nx=dy/length*width,ny=-dx/length*width,px=x(v,fork,side,at),py=y(v,fork,side,at);
        return new float[]{v.screenX(px+nx,L),v.worldScreenY(py+ny,L),v.screenX(px-nx,L),v.worldScreenY(py-ny,L)};
    }
    private static void passage(Painter p,Cave v,Layout L,float from,float to,int fork,int side,int layer) {
        int count=Math.max(2,(int)((to-from)*30));
        float width=layer==0?.25f:layer==1?.205f:.145f;
        if(fork>=0)width*=.82f;
        float[] last=edge(v,L,fork,side,from,width);
        for(int i=1;i<=count;i++) {
            float at=from+(to-from)*i/count,mid=at-(to-from)/count*.5f;
            float[] next=edge(v,L,fork,side,at,width);
            float px=v.screenX(x(v,fork,side,mid),L),py=v.worldScreenY(y(v,fork,side,mid),L);
            if(px>-L.w*.4f && px<L.w*1.4f && py>L.playTop-L.w*.4f && py<L.deckTop+L.w*.4f) {
                float light=v.light(x(v,fork,side,mid),y(v,fork,side,mid));
                float rough=hash((int)(mid*97)+layer*123)*.13f;
                int surface=layer==0?CaveArt.ROCK:layer==1?CaveArt.LIGHT:CaveArt.FLOOR;
                int color=Glyph.mix(CaveArt.DARK,surface,Math.min(1,.08f+light*(.85f+rough)));
                p.fillPoly(new float[]{last[0],last[1],next[0],next[1],next[2],next[3],last[2],last[3]},color);
                if(layer==1 && i%3==0) {
                    int rim=Glyph.mix(CaveArt.DARK,CaveArt.LAMP,light*.65f);
                    p.line(last[0],last[1],next[0],next[1],rim,L.w*.004f);
                    p.line(last[2],last[3],next[2],next[3],rim,L.w*.004f);
                }
                if(layer==2 && i%5==0) {
                    p.fillEllipse(px,py,L.w*.013f*v.zoom(),L.w*.004f*v.zoom(),Glyph.mix(color,CaveArt.LAMP,light*.17f));
                }
            }
            last=next;
        }
    }
}
