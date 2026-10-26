package com.dddumpling.game;

/** Broken reef walls, quiet sea life and a sheltered cave. Cosmetic motion never uses game RNG. */
final class Underwater extends Draw {
    static final int LAND=2,ROCK_COUNT=12,ROCK_WIDTH=248,ROCK_HEIGHT=236;
    static final float SCROLL=.016f;
    static final int WATER_TEXTURE=128;
    private static final int WATER=0xFF080F24, LIGHT=0xFF8BCFDF;
    private static final float[][] ROCK_RIMS=rockRims();
    private Underwater() {}
    private static float[][] rockRims() {
        float[][] shapes=new float[ROCK_COUNT][40];
        for(int seed=0;seed<shapes.length;seed++)for(int j=0;j<20;j++) {
            float angle=j*Softbody.TAU/20;
            float edge=1+.065f*(float)Math.sin(angle*3+seed*.7f)+.045f*(float)Math.cos(angle*5+seed);
            float cx=(float)Math.cos(angle),sy=(float)Math.sin(angle);
            // Worn, blunt shoulders and uneven ledges instead of an ellipse stretched into a plate.
            shapes[seed][j*2]=Math.copySign((float)Math.pow(Math.abs(cx),.62f),cx)*edge;
            shapes[seed][j*2+1]=Math.copySign((float)Math.pow(Math.abs(sy),.72f),sy)*edge
                    +.055f*(float)Math.sin(angle*2+seed);
        }
        return shapes;
    }

    static float caveProgress(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        if(BossCollect.keepsArena(c,Boss.OCTOPUS))return 1;
        Boss b=BossVictory.sceneBoss(c);
        if(!b.active() || b.kind!=Boss.OCTOPUS)return 0;
        return smooth(Math.min(1,b.introProgress()/0.70f));
    }
    static float bossReveal(Boss b) {return smooth((b.introProgress()-.55f)/.45f);}
    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float mix=Lands.blend(c);
        return Math.max(caveProgress(c),(c.landFrom==LAND?1-mix:0)
                +(Lands.forStage(Lands.visualStage(c))==LAND?mix:0));
    }
    private static float smooth(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static int ink(int col,float alpha) {return Glyph.withAlpha(col,(int)alpha);}

    static void backdrop(Painter p,GameCore c,Layout L) {
        float fade=presence(c)*(1-BossVictory.backdropDrain(c));
        if(fade<=.001f)return;
        float cave=caveProgress(c);
        p=new WaterPainter(p,c,L,1.8f);
        p.save();p.clipRect(0,0,L.w,L.h);
        p.fillRect(0,0,L.w,L.h,ink(WATER,245*fade));
        if(cave<1)UnderwaterVista.draw(p,c,L,fade*(1-cave));
        if(cave>0)UnderwaterCave.draw(p,c,L,fade*cave,cave);
        fish(p,c,L,fade*(1-cave*.60f));
        // Scenery continues behind the keys; a gradual darkening replaces the cut-off horizon.
        float start=L.deckTop-L.h*.045f;
        for(int band=0;band<14;band++) {
            float top=start+(L.h-start)*band/14f,bottom=start+(L.h-start)*(band+1)/14f;
            float t=(band+.5f)/14f;
            p.fillRect(0,top,L.w,bottom,ink(WATER,fade*180*t*t*(3-2*t)));
        }
        p.restore();
    }

    private static float scrollY(GameCore c,Layout L,float depth) {
        return c.skyClock*L.h*SCROLL*(1+depth*.03f);
    }

    static void paintRock(Painter p,int art) {rockVector(p,104,108,80,80,1,art);}
    private static void rockVector(Painter p,float x,float y,float w,float h,float fade,int seed) {
        float[] shape=new float[40],rim=ROCK_RIMS[seed%ROCK_RIMS.length];
        for(int layer=0;layer<4;layer++) {
            float scale=layer==0?1.20f:layer==1?1.06f:layer==2?1.07f:1;
            float dx=layer<2?w*(layer==0?.40f:.29f):0;
            float dy=layer<2?h*(layer==0?.24f:.17f):0;
            for(int j=0;j<20;j++) {
                shape[j*2]=x+dx+rim[j*2]*w*scale;
                shape[j*2+1]=y+dy+rim[j*2+1]*h*scale;
            }
            int col=layer<2?0xFF020714:layer==2?0xFF1C2534:0xFF303743;
            int alpha=layer==0?19:layer==1?45:layer==2?28:220;
            p.fillPoly(shape,ink(col,fade*alpha));
        }
        // A broad lit top meets an uneven darker front face, not nested copies of the outline.
        float[] top=new float[28];
        for(int j=0;j<11;j++) {
            int at=(j+10)%20;
            top[j*2]=x+rim[at*2]*w;top[j*2+1]=y+rim[at*2+1]*h;
        }
        top[22]=x+w*.46f;top[23]=y-h*.10f;
        top[24]=x+w*.08f;top[25]=y+h*.17f;
        top[26]=x-w*.46f;top[27]=y-h*.02f;
        p.fillPoly(top,ink(0xFF69717A,fade*83));
        float[] facet=new float[16];
        for(int j=0;j<5;j++) {
            int at=j+12;
            facet[j*2]=x+rim[at*2]*w;facet[j*2+1]=y+rim[at*2+1]*h;
        }
        facet[10]=x+w*.25f;facet[11]=y-h*.45f;
        facet[12]=x-w*.10f;facet[13]=y-h*.20f;
        facet[14]=x-w*.58f;facet[15]=y-h*.30f;
        p.fillPoly(facet,ink(0xFF8A9094,fade*35));
    }

    static void kelp(Painter p,GameCore c,float x,float y,float r,float fade,int seed,int stems) {
        float[] blade=c.waterRipples.kelpBlade,shadow=c.waterRipples.kelpShadow,edge=c.waterRipples.kelpEdge;
        for(int stem=0;stem<stems;stem++) {
            float spread=seed%3==0?.8f:seed%3==1?.3f:.55f;
            float base=x+(stem-(stems-1)*.5f)*r*spread;
            float root=y+r*(hash(seed+stem+19)-.5f)*.45f;
            float height=r*(1.8f+hash(seed+stem)*4.1f);
            for(int j=0;j<9;j++) {
                float u=j/8f,phase=c.clock*(.9f+stem*.11f)+seed;
                float bend=u*r*(.32f*(float)Math.sin(phase-u*4.5f)
                        +.18f*u*(float)Math.sin(phase*1.4f-u*9+stem));
                float lean=(stem-(stems-1)*.5f)*r*.25f*u;
                float xx=base+bend+lean,yy=root-u*height;
                float wave=(float)Math.sin(u*13+seed);
                float width=r*(.045f+.035f*wave*wave)*(1-u*.94f);
                int left=j*2,right=(17-j)*2;
                blade[left]=xx-width;blade[right]=xx+width;blade[left+1]=blade[right+1]=yy;
                edge[left]=xx-width*.45f;edge[right]=xx-width*.10f;edge[left+1]=edge[right+1]=yy;
                // Upper-left light projects each waving blade down-right onto the seabed.
                float sx=xx+u*height*.62f,sy=root+u*height*.30f;
                shadow[left]=sx-width*1.8f;shadow[right]=sx+width*1.8f;
                shadow[left+1]=shadow[right+1]=sy;
            }
            p.fillPoly(shadow,ink(0xFF010714,fade*75));
            p.fillPoly(blade,ink(0xFF274C51,fade*210));
            if(stems>=3)p.fillPoly(edge,ink(0xFF669280,fade*90));
        }
    }

    private static void fish(Painter p,GameCore c,Layout L,float fade) {
        for(int school=0;school<3;school++) {
            int direction=school%2==0?1:-1;
            float travel=(c.clock*(.022f+school*.005f)+school*.37f)%1;
            float x=L.w*(.30f+travel*.46f);if(direction<0)x=L.w-x;
            float schoolFade=fade*smooth(travel/.16f)*smooth((1-travel)/.16f);
            for(int i=0;i<4;i++) {
                float xx=x-direction*i*L.w*.022f,yy=L.h*(.29f+school*.16f)+L.h*.014f*(float)Math.sin(c.clock*1.2f+school+i*.8f);
                float r=L.w*(.009f+(i%2)*.002f),tail=.65f+.22f*(float)Math.sin(c.clock*10+i);
                int col=ink(school==1?0xFFBCA2BA:0xFF88BEB6,schoolFade*115);
                p.fillPoly(new float[]{xx+r*direction,yy,xx,yy-r*.5f,xx-r*direction,yy-r*.20f,
                        xx-r*1.65f*direction,yy-r*tail,xx-r*1.65f*direction,yy+r*tail,
                        xx-r*direction,yy+r*.20f,xx,yy+r*.5f},col);
                p.fillCircle(xx+r*.5f*direction,yy-r*.1f,r*.11f,ink(0xFF16394C,schoolFade*160));
            }
        }
    }

    /** Broad, layered haze passes over all playfield artwork; the HUD stays crisp above it. */
    static void veil(Painter p,GameCore c,Layout L) {
        float fade=presence(c)*(1-BossVictory.backdropDrain(c));if(fade<=.001f)return;
        p.save();p.clipRect(0,0,L.w,L.h);
        p.fillRect(0,0,L.w,L.h,ink(0xFF538BA0,fade*(10-7*caveProgress(c))));
        c.waterRipples.drawGlows(p,c,L,fade);
        haze(p,c,L,fade*(1-.60f*caveProgress(c)));
        for(int i=0;i<14;i++) {
            float span=L.deckTop+40,y=span-(hash(234+i)*span+c.clock*(8+i%3*3))%span;
            float x=L.w*hash(877+i)+(float)Math.sin(c.clock*.8f+i)*L.w*.013f,r=L.w*(.002f+hash(57+i)*.003f);
            p.arc(x,y,r,r*1.2f,195,230,ink(LIGHT,fade*42),Math.max(.7f,r*.20f));
        }
        p.restore();
    }
    private static void haze(Painter p,GameCore c,Layout L,float fade) {
        float[] ribbon=c.waterRipples.hazeRibbon,centre=c.waterRipples.hazeCentre;
        for(int i=0;i<3;i++) {
            float span=L.h*1.5f,y=(i*span/3+hash(i+571)*L.h*.12f+scrollY(c,L,0))%span-L.h*.25f;
            float phase=c.clock*.40f+i*2.1f,r=L.h*(.070f+hash(i+21)*.025f);
            float pulse=.85f+.15f*(float)Math.sin(phase);
            for(int j=0;j<9;j++) {
                float u=j/8f;
                // The whole strip crosses the viewport: its ends never taper inside the scene.
                ribbon[j*2]=ribbon[(17-j)*2]=L.w*(-.25f+u*1.5f);
                centre[j*2]=ribbon[j*2];
                centre[j*2+1]=y+L.w*(.085f*(float)Math.sin(u*4+phase)
                        +.025f*(float)Math.sin(u*7-phase*.7f));
            }
            ArtStamp cache=p.stamps();
            if(cache!=null && cache.waterBand(centre,r*3,Math.round(25*fade*pulse)))continue;
            float previous=0;
            // Tiny Gaussian opacity increments replace the four visibly stepped bands.
            for(int layer=0;layer<16;layer++) {
                float distance=3*(1-layer/16f),width=r*distance;
                float opacity=25*fade*pulse*(float)Math.exp(-.5f*distance*distance);
                int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
                if(alpha<=0)continue;
                for(int j=0;j<9;j++) {
                    ribbon[j*2+1]=centre[j*2+1]-width;
                    ribbon[(17-j)*2+1]=centre[j*2+1]+width;
                }
                p.fillPoly(ribbon,ink(LIGHT,alpha));
            }
        }
    }

    /** Shared soft textures, generated once by either host; no frame-sized blur buffers. */
    static int waterPixel(int x,int y,boolean band) {
        float yy=(y/(WATER_TEXTURE-1f)*2-1)*3;
        float xx=band?0:(x/(WATER_TEXTURE-1f)*2-1)*3;
        float d=(float)Math.sqrt(xx*xx+yy*yy);
        float falloff=Math.max(0,((float)Math.exp(-.5f*d*d)-.011109f)/.988891f);
        int color=band?LIGHT:Glyph.mix(0xFF49B5CC,0xFFBBF5EB,Math.max(0,1-d/3));
        return Glyph.withAlpha(color,Math.round(255*falloff));
    }

    static float enemyShift(GameCore c,float x,float y,Layout L) {
        float water=presence(c);if(water<=0)return 0;
        c.waterRipples.prepare(c,L);
        return c.waterRipples.refract(x,y)*water*.24f;
    }
    static int enemyColor(GameCore c,int color,float y,Layout L) {
        float amount=presence(c)*(.08f+.045f*(1+(float)Math.sin(c.clock*1.5f-y/L.h*11)));
        return Glyph.mix(color,LIGHT,amount);
    }
    static void tileLight(Painter p,GameCore c,Layout L,float x,float y,float r,int fade) {
        float water=presence(c);if(water<=0 || fade<=0)return;
        p.strokePoly(Glyph.hex(x,y,r*1.015f),ink(LIGHT,water*fade*.065f),r*.17f);
        float t=(c.clock*.29f+y/L.h*.71f+x/L.w*.13f)%1;
        float yy=(t*1.54f-.77f)*r,half=r-Math.abs(yy)*.57735f;
        float width=r*.065f,edge=Math.max(0,half-r*.11f);
        // Fitted inside the hexagon, under the character: no distortion of the input target.
        p.fillPoly(new float[]{x-edge,y+yy-width,x+edge*.85f,y+yy-width*.4f,
                x+edge,y+yy+width*.55f,x-edge*.86f,y+yy+width},ink(LIGHT,water*fade*.22f*(float)Math.sin(t*Math.PI)));
    }
}
