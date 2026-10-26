package com.dddumpling.game;

/** Broken reef walls, quiet sea life and a sheltered cave. Cosmetic motion never uses game RNG. */
final class Underwater extends Draw {
    static final int LAND=2,ROCK_COUNT=12,ROCK_WIDTH=248,ROCK_HEIGHT=236;
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
        // A few nested pools of blue give depth without blur passes or full-screen distortion.
        for(int layer=3;layer>=0;layer--)p.fillEllipse(L.w*.54f,L.h*.40f,
                L.w*(.38f+layer*.15f),L.h*(.23f+layer*.12f),ink(0xFF243B69,fade*(6+layer*2)*(1-cave*.65f)));
        rays(p,c,L,fade*(1-cave*.70f));
        sandDunes(p,c,L,fade);
        scatteredRocks(p,c,L,fade);
        if(cave<1)reefs(p,c,L,fade*(1-cave),cave*L.h*.36f);
        if(cave>0)cave(p,c,L,fade,cave);
        seabedGardens(p,c,L,fade,cave);
        fish(p,c,L,fade);
        // Scenery continues behind the keys; a gradual darkening replaces the cut-off horizon.
        float start=L.deckTop-L.h*.045f;
        for(int band=0;band<14;band++) {
            float top=start+(L.h-start)*band/14f,bottom=start+(L.h-start)*(band+1)/14f;
            float t=(band+.5f)/14f;
            p.fillRect(0,top,L.w,bottom,ink(WATER,fade*180*t*t*(3-2*t)));
        }
        p.restore();
    }

    private static void rays(Painter p,GameCore c,Layout L,float fade) {
        for(int i=0;i<4;i++) {
            float top=L.w*(-.4f+i*.31f),sway=L.w*.06f*(float)Math.sin(c.clock*.23f+i);
            for(int layer=0;layer<2;layer++) {
                float width=L.w*(.035f+layer*.025f);
                p.fillPoly(new float[]{top-width,-L.h*.1f,top+width,-L.h*.1f,
                        top+L.w*.66f+sway+width*2,L.deckTop,top+L.w*.46f+sway-width*2,L.deckTop},
                        ink(LIGHT,fade*(layer==0?3:1)));
            }
        }
    }

    private static void reefs(Painter p,GameCore c,Layout L,float fade,float arrival) {
        for(int depth=0;depth<2;depth++)for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float depthFade=depth==0?fade*fade*.42f:fade*.88f;
            if(depthFade<.025f)continue;
            int seed=1253+depth*103+side*43+i*67;
            // Wide, low outcrops leave open water between each shelf.
            float y=reefY(c,L,depth,side,i,arrival);
            float w=L.w*(.23f+hash(seed+2)*.04f)*(depth==0?.80f:1);
            float h=w*(.34f+hash(seed+1)*.06f);
            // Occasional low, broad shelves reach beyond the middle, alternating banks.
            if(depth==1 && i==side){w=L.w*(.60f+hash(seed+2)*.06f);h=w*.21f;}
            if(y+h<-20 || y-h>L.h+20)continue;
            cliff(p,L.w*side,y,w,h,side==0?1:-1,depthFade,seed);
        }
    }

    private static float reefY(GameCore c,Layout L,int depth,int side,int row,float arrival) {
        int seed=1253+depth*103+side*43+row*67;
        float span=L.h*1.65f,slot=span/3;
        return (row*slot+side*slot*.43f+hash(seed)*slot*.12f
                +c.skyClock*L.h*(.015f+depth*.012f)+arrival)%span-L.h*.30f;
    }
    private static float shelfPlantFade(GameCore c,Layout L,float y,float radius,float cave) {
        float clear=1;
        for(int side=0;side<2;side++) {
            float shelfY=reefY(c,L,1,side,side,cave*L.h*.36f);
            float distance=Math.abs(y-shelfY)-L.w*.165f-radius;
            clear=Math.min(clear,smooth(distance/(L.h*.045f)));
        }
        return clear;
    }

    private static void cliff(Painter p,float x,float y,float w,float h,int side,float fade,int seed) {
        rock(p,x+side*w*.02f,y,w,h,fade,seed);
    }

    /** Rounded erosion contours with a broad upper-left highlight and a down-right cast shadow. */
    private static void rock(Painter p,float x,float y,float w,float h,float fade,int seed) {
        if(p.stamps()!=null && p.stamps().reefRock(seed%ROCK_COUNT,x,y,w,h,(int)(fade*255)))return;
        rockVector(p,x,y,w,h,fade,seed);
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

    private static void sandDunes(Painter p,GameCore c,Layout L,float fade) {
        float[] dune=new float[44];
        for(int i=0;i<6;i++) {
            int seed=3809+i*83,depth=i%2;
            float r=L.w*(.16f+hash(seed)*.22f),height=r*(.12f+hash(seed+5)*.13f);
            float span=L.h+height*8,x=L.w*(.15f+hash(seed+9)*.70f);
            float y=(hash(seed+13)*span+c.skyClock*L.h*(depth==0?.009f:.019f))%span-height*4;
            float slope=(hash(seed+17)-.5f)*.38f;
            for(int layer=0;layer<3;layer++) {
                for(int j=0;j<11;j++) {
                    float u=j/10f,edge=(float)Math.sin(u*Math.PI);
                    float xx=x+(u-.5f)*r*2;
                    float crest=y+height*((float)Math.sin(u*4.1f+hash(seed+3)*2)*.5f+slope*u*5);
                    // A steep lit ridge rolls into a wider, down-right lee slope; ends taper away.
                    float above=height*edge*(layer==0?.45f:layer==1?.20f:.035f);
                    float below=height*edge*(layer==0?1.55f:layer==1?.70f:.12f);
                    dune[j*2]=xx+(layer==0?height*.25f:0);dune[j*2+1]=crest-above;
                    dune[(21-j)*2]=dune[j*2];dune[(21-j)*2+1]=crest+below;
                }
                int color=layer==0?0xFF080F1D:layer==1?0xFF40515B:0xFF78827D;
                float alpha=layer==0?52:layer==1?24:25;
                p.fillPoly(dune,ink(color,fade*alpha*(depth==0?.55f:.90f)));
            }
        }
    }

    private static void scatteredRocks(Painter p,GameCore c,Layout L,float fade) {
        for(int i=0;i<10;i++) {
            int seed=2803+i*53;
            float r=L.w*(.025f+hash(seed)*.045f),span=L.h+r*6;
            float y=(hash(seed+11)*span+c.skyClock*L.h*(.010f+i%2*.006f))%span-r*3;
            float x=L.w*scatterX(seed,r/L.w);
            rock(p,x,y,r,r*(.55f+hash(seed+19)*.20f),fade*(.22f+i%3*.065f),seed);
        }
    }

    private static void cave(Painter p,GameCore c,Layout L,float fade,float progress) {
        p.save();p.translate(0,-(1-progress)*L.h*.68f);
        // An irregular roof and receding rock arches enclose a dark, open central chamber.
        float[] roof=new float[50];
        for(int layer=0;layer<3;layer++) {
            float top=L.h*(.24f-layer*.042f);
            roof[0]=-L.w*.1f;roof[1]=-L.h;
            for(int j=0;j<=21;j++) {
                float x=(j/21f*1.2f-.1f)*L.w,u=Math.abs(x/L.w-.51f)*2;
                roof[2+j*2]=x;
                roof[3+j*2]=top+L.h*(.20f*u*u+.011f*(float)Math.sin(j*.72f+layer));
            }
            roof[46]=L.w*1.1f;roof[47]=-L.h;roof[48]=-L.w*.1f;roof[49]=-L.h;
            p.fillPoly(roof,ink(layer==0?0xFF3B3F49:layer==1?0xFF252A35:0xFF121822,fade*(135+layer*45)));
        }
        for(int side=0;side<2;side++) {
            cliff(p,L.w*side,L.h*.43f,L.w*.26f,L.w*.090f,side==0?1:-1,fade,991+side);
            cliff(p,L.w*side,L.deckTop-L.h*.06f,L.w*.32f,L.w*.100f,side==0?1:-1,fade,953+side);

        }
        // Sand-lit hollow under Octopulse; rim is textured rather than a hard horizon.
        for(int layer=2;layer>=0;layer--)p.fillEllipse(L.w*.5f,L.deckTop-L.h*.08f,L.w*(.34f+layer*.07f),L.h*(.034f+layer*.018f),ink(0xFF435783,fade*(8+layer*2)));
        for(int i=0;i<12;i++) {
            float x=L.w*(.10f+i*.073f),y=L.h*(.22f+.18f*(float)Math.pow(Math.abs(x/L.w-.51f)*2,2));
            float r=L.w*(.003f+hash(i+71)*.004f),pulse=.65f+.35f*(float)Math.sin(c.clock*1.3f+i*2.1f);
            p.fillCircle(x,y,r*2.9f,ink(LIGHT,fade*pulse*12));
            p.fillCircle(x,y,r,ink(LIGHT,fade*pulse*105));
        }
        p.restore();
    }

    static float scatterX(int seed,float radius) {
        for(int tryX=0;tryX<12;tryX++) {
            float x=.07f+hash(seed+7+tryX*73)*.86f;
            if(Math.abs(x-.35f)>radius*1.1f+.07f && Math.abs(x-.65f)>radius*1.1f+.07f)return x;
        }
        return .5f;
    }
    private static void seabedGardens(Painter p,GameCore c,Layout L,float fade,float cave) {
        float stageFade=fade*smooth((1-cave-.55f)/.45f);
        if(stageFade>.025f)for(int i=0;i<5;i++) {
            int seed=4103+i*97;
            float span=L.h*1.5f,y=(i*span/5+hash(seed)*L.h*.08f+c.skyClock*L.h*.016f)%span-L.h*.20f;
            float r=L.w*(.039f+hash(seed+3)*.008f);
            if(y+r<0 || y-r*5>L.h)continue;
            float plants=stageFade*shelfPlantFade(c,L,y,r,cave);
            if(plants>.025f)garden(p,c,L,L.w*(i%2==0?.35f:.65f),y,r,plants,seed,i);
        }
        float caveFade=fade*smooth((cave-.45f)/.55f);
        if(caveFade>.025f)for(int i=0;i<2;i++)garden(p,c,L,L.w*(i==0?.35f:.65f),
                L.deckTop-L.h*.055f,L.w*.043f,caveFade,4261+i*79,i==0?2:3);
    }

    private static void garden(Painter p,GameCore c,Layout L,float x,float y,float r,float fade,int seed,int kind) {
        float[] ribbon=new float[44];
        for(int stem=0;stem<3;stem++) {
            float base=x+(stem-1)*r*.35f,height=r*(2.4f+hash(seed+stem)*2.0f);
            // Travelling bends grow from a fixed holdfast; uneven widths twist along each blade.
            for(int layer=0;layer<2;layer++) {
                for(int j=0;j<11;j++) {
                    float u=j/10f,phase=c.clock*(.9f+stem*.11f)+seed;
                    float bend=u*r*(.32f*(float)Math.sin(phase-u*4.5f)
                            +.18f*u*(float)Math.sin(phase*1.4f-u*9+stem));
                    float xx=base+bend+(stem-1)*r*.13f*u,yy=y-u*height;
                    float width=r*(.035f+.030f*(float)Math.pow(Math.sin(u*13+seed),2))*(1-u*.94f);
                    if(layer==1){xx-=width*.25f;width*=.23f;}
                    ribbon[j*2]=xx-width;ribbon[j*2+1]=yy;
                    ribbon[(21-j)*2]=xx+width;ribbon[(21-j)*2+1]=yy;
                }
                p.fillPoly(ribbon,ink(layer==0?0xFF274C51:0xFF669280,fade*(layer==0?210:100)));
            }
        }
        UnderwaterCoral.draw(p,x+r*.25f,y+r*.04f,r*.85f,c.clock+seed,kind,fade,seed);
    }

    private static void fish(Painter p,GameCore c,Layout L,float fade) {
        for(int school=0;school<3;school++) {
            int direction=school%2==0?1:-1;
            float travel=(c.clock*(.022f+school*.005f)+school*.37f)%1;
            float x=L.w*(-.20f+travel*1.4f);if(direction<0)x=L.w-x;
            for(int i=0;i<4;i++) {
                float xx=x-direction*i*L.w*.035f,yy=L.h*(.29f+school*.19f)+L.h*.014f*(float)Math.sin(c.clock*1.2f+school+i*.8f);
                float r=L.w*(.009f+(i%2)*.002f),tail=.65f+.22f*(float)Math.sin(c.clock*10+i);
                int col=ink(school==1?0xFFBCA2BA:0xFF88BEB6,fade*95);
                p.fillPoly(new float[]{xx+r*direction,yy,xx,yy-r*.5f,xx-r*direction,yy-r*.20f,
                        xx-r*1.65f*direction,yy-r*tail,xx-r*1.65f*direction,yy+r*tail,
                        xx-r*direction,yy+r*.20f,xx,yy+r*.5f},col);
                p.fillCircle(xx+r*.5f*direction,yy-r*.1f,r*.11f,ink(0xFF16394C,fade*160));
            }
        }
    }

    /** Broad, layered haze passes over all playfield artwork; the HUD stays crisp above it. */
    static void veil(Painter p,GameCore c,Layout L) {
        float fade=presence(c)*(1-BossVictory.backdropDrain(c));if(fade<=.001f)return;
        p=new WaterPainter(p,c,L,.6f);
        p.save();p.clipRect(0,0,L.w,L.h);
        p.fillRect(0,0,L.w,L.h,ink(0xFF538BA0,fade*10));
        haze(p,c,L,fade);
        for(int i=0;i<14;i++) {
            float span=L.deckTop+40,y=span-(hash(234+i)*span+c.clock*(8+i%3*3))%span;
            float x=L.w*hash(877+i)+(float)Math.sin(c.clock*.8f+i)*L.w*.013f,r=L.w*(.002f+hash(57+i)*.003f);
            p.arc(x,y,r,r*1.2f,195,230,ink(LIGHT,fade*42),Math.max(.7f,r*.20f));
        }
        p.restore();
    }
    private static void haze(Painter p,GameCore c,Layout L,float fade) {
        float[] ribbon=new float[36],centre=new float[9];
        for(int i=0;i<3;i++) {
            float span=L.h*1.5f,y=(i*span/3+hash(i+571)*L.h*.12f+c.skyClock*L.h*.016f)%span-L.h*.25f;
            float phase=c.clock*.40f+i*2.1f,r=L.h*(.070f+hash(i+21)*.025f);
            float pulse=.85f+.15f*(float)Math.sin(phase);
            for(int j=0;j<9;j++) {
                float u=j/8f;
                // The whole strip crosses the viewport: its ends never taper inside the scene.
                ribbon[j*2]=ribbon[(17-j)*2]=L.w*(-.25f+u*1.5f);
                centre[j]=y+L.w*(.085f*(float)Math.sin(u*4+phase)
                        +.025f*(float)Math.sin(u*7-phase*.7f));
            }
            float previous=0;
            // Tiny Gaussian opacity increments replace the four visibly stepped bands.
            for(int layer=0;layer<16;layer++) {
                float distance=3*(1-layer/16f),width=r*distance;
                float opacity=25*fade*pulse*(float)Math.exp(-.5f*distance*distance);
                int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
                if(alpha<=0)continue;
                for(int j=0;j<9;j++) {
                    ribbon[j*2+1]=centre[j]-width;
                    ribbon[(17-j)*2+1]=centre[j]+width;
                }
                p.fillPoly(ribbon,ink(LIGHT,alpha));
            }
        }
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
