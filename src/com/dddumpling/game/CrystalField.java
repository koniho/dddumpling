package com.dddumpling.game;

import java.util.ArrayList;

/** Periodic Voronoi gemstone floor. Geometry is rebuilt only when the viewport changes. */
final class CrystalField extends Draw {
    static final int LAND=1, COUNT=18;
    static final float PERIOD=2f, SPEED=.025f;
    private float width;
    Cell[] cells;
    static final class Cell {
        float[] rim,face;
        float[][] bevel;
        int[] bevelColor;
        float top,bottom,phase;
        int color;
    }
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float blend=Lands.blend(c);
        return (c.landFrom==LAND?1-blend:0)+(Lands.forStage(Lands.visualStage(c))==LAND?blend:0);
    }
    static float scroll(float clock,float width) {return (clock*SPEED%PERIOD)*width;}

    void prepare(float w) {
        if(cells!=null && width==w)return;
        width=w;cells=new Cell[COUNT];
        float[] sx=new float[COUNT],sy=new float[COUNT];
        for(int i=0;i<COUNT;i++) {
            sx[i]=(i%3+.12f+.76f*hash(4103+i*47))/3;
            sy[i]=(i/3+.12f+.76f*hash(5231+i*61))/3;
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
            cell.bevel=new float[poly.length/2][];cell.bevelColor=new int[poly.length/2];
            cell.top=Float.MAX_VALUE;cell.bottom=-Float.MAX_VALUE;
            cell.phase=hash(891+i*37)*Softbody.TAU;
            cell.color=Glyph.mix(0xFF43315F,0xFF71588C,hash(1831+i*53));
            float inset=.70f+hash(i*31+71)*.13f;
            for(int k=0;k<poly.length;k+=2) {
                float x=poly[k],y=poly[k+1];
                cell.face[k]=(sx[i]+(x-sx[i])*inset)*w;
                cell.face[k+1]=(sy[i]+(y-sy[i])*inset)*w;
                poly[k]*=w;poly[k+1]*=w;
                cell.top=Math.min(cell.top,poly[k+1]);cell.bottom=Math.max(cell.bottom,poly[k+1]);
            }
            for(int k=0;k<poly.length;k+=2) {
                int next=(k+2)%poly.length;
                cell.bevel[k/2]=new float[]{poly[k],poly[k+1],poly[next],poly[next+1],
                        cell.face[next],cell.face[next+1],cell.face[k],cell.face[k+1]};
                float dx=poly[next]-poly[k],dy=poly[next+1]-poly[k+1];
                float facing=(dx-dy)/Math.max(1,(float)Math.sqrt(dx*dx+dy*dy));
                cell.bevelColor[k/2]=facing>0?Glyph.mix(cell.color,0xFFB8A1DE,facing*.37f)
                        :Glyph.mix(cell.color,0xFF20152F,-facing*.42f);
            }
        }
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
        float offset=scroll(c.skyClock,L.w),period=PERIOD*L.w;
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        for(int tile=-1;tile<=(int)(L.deckTop/period)+1;tile++) {
            float y=offset+tile*period;
            p.save();p.translate(0,y);
            for(Cell cell:cells) {
                if(cell.bottom+y<0 || cell.top+y>L.deckTop)continue;
                float shimmer=.5f+.5f*(float)Math.sin(c.clock*.72f+cell.phase);
                int face=Glyph.mix(cell.color,0xFFAC9ACF,.055f+shimmer*.10f);
                p.fillPoly(cell.rim,Glyph.withAlpha(0xFF261B39,(int)(fade*255)));
                for(int k=0;k<cell.bevel.length;k++) {
                    float glint=Math.max(0,(float)Math.sin(c.clock*.62f+cell.phase+k*.9f));
                    int bevel=Glyph.mix(cell.bevelColor[k],0xFFCEB9ED,glint*glint*glint*.12f);
                    p.fillPoly(cell.bevel[k],Glyph.withAlpha(bevel,(int)(fade*255)));
                }
                p.fillPoly(cell.face,Glyph.withAlpha(face,(int)(fade*255)));
                p.strokePoly(cell.face,Glyph.withAlpha(0xFFC5ABE7,(int)(fade*(10+shimmer*14))),L.w*.0018f);
                p.strokePoly(cell.rim,Glyph.withAlpha(0xFF251A37,(int)(fade*95)),L.w*.0022f);
            }
            p.restore();
        }
        p.restore();
    }
}
