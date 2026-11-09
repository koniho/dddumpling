package com.dddumpling.game;

/** Four immutable reed patches, shared by native image caches and vector previews. */
final class SwampGrass extends Draw {
    static final int COUNT=4,BLADES=18,WIDTH=128,HEIGHT=512;
    static final float RADIUS=.09f,UNIT=48,ANCHOR_X=64,ANCHOR_Y=480,TOP=9.2f;
    private static final float[][][] BLADES_XY=new float[COUNT][BLADES][8];
    private static final int[] COLORS={0xFF3D856B,0xFF5F913F,0xFF8DAA63,0xFF78913F,
            0xFF468F88,0xFFB2A75E,0xFF899D80,0xFF638B58};
    private static final int[][] BLADE_COLORS=new int[COUNT][BLADES];
    static {
        for(int art=0;art<COUNT;art++)for(int i=0;i<BLADES;i++) {
            int seed=811+art*317+i*37;
            float x=(hash(seed)-.5f)*2.1f;
            float y=-.20f+(hash(seed+3)-.5f)*1.4f*(float)Math.sqrt(Math.max(.1f,1-x*x/1.21f));
            float h=2*(1.9f+hash(seed+5)*1.5f),lean=(hash(seed+7)-.5f)*.50f,w=.018f+hash(seed+9)*.024f;
            float[] b=BLADES_XY[art][i];
            b[0]=x-w;b[1]=y;b[2]=x+w;b[3]=y;b[4]=x+lean;b[5]=y-h;b[6]=x+lean*.38f-w*.4f;b[7]=y-h*.52f;
        }
        for(float[][] blades:BLADES_XY)java.util.Arrays.sort(blades,(a,b)->Float.compare(a[1],b[1]));
        // Bake cooler rear shadows and varied greens into the existing native stamps.
        for(int art=0;art<COUNT;art++)for(int i=0;i<BLADES;i++) {
            int seed=2831+art*131+i*67;
            int color=COLORS[Math.min(COLORS.length-1,(int)(hash(seed)*COLORS.length))];
            color=Glyph.mix(color,0xFFB0AE79,hash(seed+3)*.08f);
            float front=Math.max(0,Math.min(1,(BLADES_XY[art][i][1]+.9f)/1.4f));
            BLADE_COLORS[art][i]=Glyph.mix(color,0xFF243F3E,.06f+.62f*(1-front));
        }
    }
    static int cattails(int art){return art==0 || art==3?0:art+1;}
    static void draw(Painter p,float x,float y,float r,int seed,float time,float clarity,float fade) {
        int art=Math.floorMod(seed,COUNT);float sway=.006f*(float)Math.sin(time*.65f+seed);
        int alpha=(int)(255*fade*(.45f+.45f*clarity));ArtStamp cache=p.stamps();
        if(cache!=null && cache.swampGrass(art,x,y,r,sway,alpha,0))return;
        paint(p,art,x,y,r,sway,alpha/255f);
    }
    static void paint(Painter p,int art,float x,float y,float r,float sway,float fade) {
        for(int i=0;i<cattails(art);i++) {
            int seed=1723+art*71+i*43;
            float bx=(hash(seed)-.5f)*1.6f,by=-.8f+hash(seed+3)*1.2f,h=2*(3.1f+hash(seed+5)*.65f),tip=bx+(h-by)*sway;
            int stem=Glyph.withAlpha(0xFF8A9C69,(int)(255*fade)),head=Glyph.withAlpha(0xFF70543E,(int)(255*fade));
            p.line(x+(bx-by*sway)*r,y+by*r,x+tip*r,y+(by-h)*r,stem,r*.026f);
            p.line(x+tip*r,y+(by-h)*r,x+(tip+sway*.32f)*r,y+(by-h-.32f)*r,head,r*.11f);
            p.line(x+(tip+sway*.32f)*r,y+(by-h-.32f)*r,x+(tip+sway*.43f)*r,y+(by-h-.43f)*r,stem,r*.016f);
        }
        float[] out=new float[8];
        for(int i=0;i<BLADES;i++) {
            float[] b=BLADES_XY[art][i];
            for(int j=0;j<8;j+=2){out[j]=x+r*(b[j]-b[j+1]*sway);out[j+1]=y+r*b[j+1];}
            p.fillPoly(out,Glyph.withAlpha(BLADE_COLORS[art][i],(int)(255*fade)));
        }
    }
}
