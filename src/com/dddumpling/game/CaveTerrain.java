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
        float nx=dy/length,ny=-dx/length,px=x(v,fork,side,at),py=y(v,fork,side,at);
        float left=width+roughness(v,at,-1)*width*.22f,right=width+roughness(v,at,1)*width*.22f;
        return new float[]{v.screenX(px+nx*right,L),v.worldScreenY(py+ny*right,L),
                v.screenX(px-nx*left,L),v.worldScreenY(py-ny*left,L)};
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
                if(layer==2 && i%5==0) {
                    p.fillEllipse(px,py,L.w*.013f*v.zoom(),L.w*.004f*v.zoom(),Glyph.mix(color,CaveArt.LAMP,light*.17f));
                }
            }
            last=next;
        }
        if(layer==1) {
            int rocks=Math.max(2,(int)((to-from)/.14f));
            for(int i=0;i<rocks;i++)for(int wall=-1;wall<=1;wall+=2)
                wallRock(p,v,L,from+(to-from)*(i+.5f)/rocks,(to-from)/rocks,fork,side,wall);
        }
    }
    // Fixed world-space noise keeps the silhouette still while the camera and lantern move.
    private static float roughness(Cave v,float at,int side) {
        float cell=at/.12f;int i=(int)cell,seed=(int)v.runSeed+side*771;
        float t=cell-i;return (hash(seed+i*91)*(1-t)+hash(seed+(i+1)*91)*t)*2-1;
    }
    private static float[] point(Cave v,Layout L,int fork,int side,float at,float offset) {
        float dx=x(v,fork,side,at+.015f)-x(v,fork,side,at-.015f);
        float dy=y(v,fork,side,at+.015f)-y(v,fork,side,at-.015f);
        float length=Math.max(.0001f,(float)Math.hypot(dx,dy));
        return new float[]{v.screenX(x(v,fork,side,at)+dy/length*offset,L),
                v.worldScreenY(y(v,fork,side,at)-dx/length*offset,L)};
    }
    private static void wallRock(Painter p,Cave v,Layout L,float at,float span,int fork,int side,int wall) {
        int seed=(int)v.runSeed+(int)(at*913)+wall*173+side*317;
        float width=fork>=0?.82f:1,depth=(.06f+hash(seed+8)*.055f)*width;
        float offset=(.205f+roughness(v,at,wall)*.024f)*width;
        float[] center=point(v,L,fork,side,at,wall*offset);
        if(center[0]<-L.w*.2f||center[0]>L.w*1.2f||center[1]<L.playTop-L.w*.2f||center[1]>L.deckTop+L.w*.2f)return;
        float[] pts=new float[14];
        for(int i=0;i<7;i++) {
            float angle=i*6.283185f/7,reach=.78f+hash(seed+i*43)*.30f;
            float along=at+(float)Math.cos(angle)*span*.70f*reach;
            float out=offset+(float)Math.sin(angle)*depth*reach;
            float[] vertex=point(v,L,fork,side,Math.max(0,Math.min(Cave.LENGTH,along)),wall*out);
            pts[i*2]=vertex[0];pts[i*2+1]=vertex[1];
        }
        float dx=x(v,fork,side,at+.015f)-x(v,fork,side,at-.015f),dy=y(v,fork,side,at+.015f)-y(v,fork,side,at-.015f);
        float length=Math.max(.0001f,(float)Math.hypot(dx,dy));
        float light=v.light(x(v,fork,side,at)+wall*dy/length*offset,y(v,fork,side,at)-wall*dx/length*offset);
        int seam=Glyph.mix(CaveArt.DARK,CaveArt.ROCK,light*.28f);
        int face=Glyph.mix(CaveArt.DARK,CaveArt.MID,.04f+light*(.60f+hash(seed+4)*.32f));
        int lit=Glyph.mix(CaveArt.DARK,CaveArt.LIGHT,.04f+light*.9f);
        p.fillPoly(pts,seam);
        for(int i=0;i<7;i++){pts[i*2]=center[0]+(pts[i*2]-center[0])*.91f;pts[i*2+1]=center[1]+(pts[i*2+1]-center[1])*.91f;}
        p.fillPoly(pts,face);
        p.fillPoly(new float[]{pts[0],pts[1],pts[2],pts[3],pts[4],pts[5],center[0],center[1]},lit);
        p.fillPoly(new float[]{pts[6],pts[7],pts[8],pts[9],pts[10],pts[11],center[0],center[1]},
                Glyph.mix(seam,face,.48f));
        if(hash(seed+12)>.48f)p.polyline(new float[]{pts[12],pts[13],center[0],center[1],
                center[0]+(pts[4]-center[0])*.36f,center[1]+(pts[5]-center[1])*.36f},seam,L.w*.002f*v.zoom());
    }
}
