package com.dddumpling.game;

/** A lit reef corridor: immutable scenery passes through a small perspective field. */
final class UnderwaterVista extends Draw {
    static final int COUNT=7,WIDTH=256;
    static int height(int art) {return art==0?512:352;}
    private UnderwaterVista() {}
    private static int ink(int color,float alpha) {return Glyph.withAlpha(color,(int)alpha);}
    private static float smooth(float v) {v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    static float depth(float travel) {return travel/(4-3*travel);}
    static float visibility(float travel) {return smooth(travel/.22f);}
    static float rootY(float travel) {return .19f+depth(travel)*1.71f;}
    static float scale(float travel) {return .15f+depth(travel)*1.35f;}

    static void draw(Painter p,GameCore c,Layout L,float fade) {
        if(fade<.004f)return;
        if(p instanceof WaterPainter)p=((WaterPainter)p).anchored();
        image(p,0,0,0,L.w,L.h,fade);
        surface(p,c,L,fade);
        // Far to near, with the same projection for placement, size, and movement.
        // New scenery is invisible at the far plane; recycling happens below the screen.
        for(int row=0;row<4;row++)for(int side=0;side<2;side++) {
            float clock=c.skyClock*.065f+side*.21f;
            int cycle=(int)Math.floor(clock);
            float travel=(row+clock-cycle)/4f,d=depth(travel),s=scale(travel);
            int art=1+Math.floorMod(row-cycle+side*3,6);
            float x=L.w*(.5f+(side==0?-1:1)*(.14f+d*.60f));
            float y=L.h*rootY(travel),w=L.w*.65f*s,h=w*352/256;
            if(y-h>L.h || x+w*.5f<0 || x-w*.5f>L.w)continue;
            float alpha=fade*visibility(travel)*(.45f+.55f*smooth(d*2.4f));
            image(p,art,x-w*.5f,y-h*.86f,w,h,alpha);
        }
        // Living fronds remain vectors, while the detailed colonies travel as cached images.
        for(int side=0;side<2;side++) {
            float t=c.skyClock*.016f+side*.5f;t-=(int)t;
            float d=depth(t),s=scale(t),x=L.w*(.5f+(side==0?-1:1)*(.13f+d*.59f));
            float y=L.h*rootY(t),r=L.w*.035f*s;
            Underwater.kelp(p,c,x,y,r,fade*visibility(t)*.65f,791+side*71,2);
        }
        for(int i=0;i<18;i++) {
            float t=(hash(727+i)*1.0f+c.skyClock*.018f)%1,d=depth(t);
            float x=L.w*(.5f+(hash(913+i)-.5f)*(.35f+d*1.5f));
            float y=L.h*(.13f+d*1.13f),r=L.w*(.0008f+.002f*d);
            p.fillCircle(x,y,r*2.6f,ink(0xFF70D8DB,fade*visibility(t)*10));
            p.fillCircle(x,y,r,ink(0xFFACDBDA,fade*visibility(t)*(28+d*35)));
        }
    }

    private static void image(Painter p,int art,float x,float y,float w,float h,float fade) {
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.reefVista(art,x,y,w,h,(int)(fade*255)))return;
        artwork(p,art,x,y,w,h,fade);
    }
    static void paint(Painter p,int art) {artwork(p,art,0,0,WIDTH,height(art),1);}
    private static void artwork(Painter p,int art,float x,float y,float w,float h,float fade) {
        if(art==0){backdrop(p,x,y,w,h,fade);return;}
        float r=w*.34f,cx=x+w*.5f,root=y+h*.86f;
        int seed=419+art*83;
        // Tall sea fans and grasses sit behind a rounded, shaded rock foot.
        fan(p,cx-r*.58f,root-r*.28f,r*.77f,seed,0xFF578E97,fade);
        fan(p,cx+r*.53f,root-r*.13f,r*.60f,seed+41,0xFF666A9C,fade);
        stone(p,cx,root-r*.12f,r*.95f,r*.60f,seed,fade);
        stone(p,cx-r*.68f,root+r*.10f,r*.43f,r*.31f,seed+7,fade);
        stone(p,cx+r*.64f,root+r*.17f,r*.44f,r*.34f,seed+11,fade);
        UnderwaterCoral.colony(p,cx-r*.30f,root-r*.36f,r*.58f,seed,art%5,fade,seed,
                art%3==0?0xFF77ABA1:art%3==1?0xFFA77B9F:0xFF8A8CB7);
        plume(p,cx+r*.48f,root-r*.19f,r*.62f,seed+17,
                art%3==0?0xFF76A798:art%3==1?0xFFB688A6:0xFF7799BF,fade);
        plume(p,cx-r*.57f,root+r*.13f,r*.40f,seed+37,
                art%2==0?0xFF7299B3:0xFFC49C7C,fade);
        for(int i=0;i<3;i++) {
            float xx=cx+(hash(seed+i+92)-.5f)*r*1.30f,yy=root+r*(.16f+hash(seed+i+85)*.20f);
            stone(p,xx,yy,r*.16f,r*.11f,seed+i+19,fade);
        }
    }
    private static void backdrop(Painter p,float x,float y,float w,float h,float a) {
        // Smooth water/light falloff is baked once on each native host.
        for(int row=0;row<80;row++) {
            float t=(row+.5f)/80;
            int col=Glyph.mix(0xFF17546F,0xFF0B203E,smooth(t));
            p.fillRect(x,y+h*row/80,x+w,y+h*(row+1)/80,ink(col,a*255));
        }
        for(int layer=63;layer>=0;layer--) {
            float t=layer/63f;
            p.fillEllipse(x+w*.48f,y-h*.06f,w*(.13f+t*.52f),h*(.15f+t*.54f),ink(0xFF75DFDD,a*1.8f));
        }
        // An irregular, distant sandy channel broadens gently toward the foreground.
        float[] path=new float[84];
        for(int layer=0;layer<12;layer++) {
            for(int i=0;i<21;i++) {
                float t=i/20f,yy=y+h*(.61f+t*.52f);
                float half=w*(.015f*t+t*t*(.34f-layer*.015f));
                float mid=x+w*(.50f+.025f*(float)Math.sin(t*8));
                float ripple=w*.014f*t*(float)Math.sin(t*34+layer);
                path[i*2]=mid-half+ripple;path[i*2+1]=yy;
                path[(41-i)*2]=mid+half+ripple;path[(41-i)*2+1]=yy;
            }
            p.fillPoly(path,ink(0xFFA2ADA0,a*3));
        }
        // Receding walls frame open water; foreground colonies add the moving relief.
        for(int side=0;side<2;side++)for(int layer=0;layer<3;layer++) {
            float[] wall=new float[132];
            wall[0]=x+w*side;wall[1]=y-h*.05f;
            for(int i=0;i<64;i++) {
                float t=i/63f;
                float edge=.065f+.060f*(float)Math.cos(t*5)+.025f*(float)Math.sin(t*18+side*2)
                        +.012f*(float)Math.cos(t*31+side)+(.045f-layer*.035f);
                wall[2+i*2]=x+w*(side==0?edge:1-edge);wall[3+i*2]=y+h*(t*1.1f-.05f);
            }
            wall[130]=x+w*side;wall[131]=y+h*1.05f;
            p.fillPoly(wall,ink(layer==0?0xFF28617A:layer==1?0xFF1B405E:0xFF112C4B,a*(layer==0?70:layer==1?150:205)));
        }
        // Rounded relief and reef life live inside the cached wall painting.
        for(int side=0;side<2;side++)for(int i=0;i<8;i++) {
            int seed=1709+side*317+i*83;
            float yy=y+h*(.07f+i*.135f+hash(seed+29)*.04f);
            float inset=-.025f+hash(seed+19)*.065f,xx=x+w*(side==0?inset:1-inset);
            float rx=w*(.09f+hash(seed)*.07f),ry=rx*(.75f+hash(seed+13)*.35f);
            stone(p,xx,yy,rx,ry,seed,a*.75f);
            float inward=side==0?1:-1;
            float plantX=xx+inward*rx*.45f,plantY=yy-ry*.15f;
            fan(p,plantX,plantY,w*(.040f+i*.004f),seed,0xFF6D9EA8,a*.30f);
            if(i%2==0)plume(p,xx+inward*rx*.60f,yy+ry*.35f,w*(.043f+i*.004f),seed+51,
                    i%4==0?0xFF779EAE:0xFF8D7DA1,a*.55f);
        }
    }
    private static void surface(Painter p,GameCore c,Layout L,float a) {
        float[] line=new float[22];
        for(int i=0;i<9;i++) {
            float r=L.w*(.12f+i*.062f),yy=L.h*(.015f+i*.014f);
            for(int j=0;j<11;j++) {
                float u=j/10f;
                line[j*2]=L.w*.48f+(u-.5f)*r*2;
                line[j*2+1]=yy+L.h*.009f*(float)Math.sin(u*8+c.clock*.3f+i*1.7f)
                        +L.h*.025f*(u-.5f)*(u-.5f);
            }
            p.polyline(line,ink(0xFF9DE3DD,a*(20-i*1.5f)),L.w*(.003f+i*.0003f));
        }
        for(int i=0;i<3;i++) {
            float xx=L.w*(.31f+i*.17f),sway=L.w*.035f*(float)Math.sin(c.clock*.18f+i);
            p.fillPoly(new float[]{xx-L.w*.025f,0,xx+L.w*.025f,0,
                    xx+sway+L.w*.15f,L.h*.80f,xx+sway-L.w*.10f,L.h*.80f},ink(0xFF98DDD5,a*6));
        }
    }
    private static void stone(Painter p,float x,float y,float rx,float ry,int seed,float a) {
        float[] rim=new float[56];
        for(int layer=0;layer<3;layer++) {
            for(int j=0;j<28;j++) {
                float angle=j*Softbody.TAU/28;
                float edge=.91f+.06f*(float)Math.sin(angle*3+seed)+.035f*(float)Math.cos(angle*5+seed*.7f);
                rim[j*2]=x-rx*layer*.055f+(float)Math.cos(angle)*rx*edge*(1-layer*.14f);
                rim[j*2+1]=y-ry*layer*.15f+(float)Math.sin(angle)*ry*edge*(1-layer*.21f);
            }
            p.fillPoly(rim,ink(layer==0?0xFF102B46:layer==1?0xFF2C5068:0xFF53798B,a*(layer==2?140:245)));
        }
        for(int i=0;i<3;i++) {
            float xx=x+rx*(hash(seed+i)-.5f),yy=y-ry*(.26f+hash(seed+i+5)*.35f);
            p.line(xx,yy,xx+rx*.24f,yy-ry*.06f,ink(0xFF8FB0B5,a*43),rx*.035f);
        }
    }
    private static void fan(Painter p,float x,float y,float r,int seed,int color,float a) {
        for(int i=0;i<5;i++) {
            float dx=(i-2)*r*.32f,hh=r*(.75f+hash(seed+i)*.70f);
            float[] branch={x,y,x+dx*.20f,y-hh*.36f,x+dx*.72f,y-hh*.72f,x+dx,y-hh};
            p.polyline(branch,ink(Glyph.mix(color,0xFF132D49,.40f),a*230),r*.054f);
            p.polyline(branch,ink(color,a*150),r*.023f);
            for(int j=1;j<3;j++) {
                float xx=branch[j*2],yy=branch[j*2+1],sign=i%2==0?-1:1;
                p.polyline(new float[]{xx,yy,xx+sign*r*.15f,yy-r*.15f,xx+sign*r*.21f,yy-r*.32f},ink(color,a*155),r*.025f);
            }
        }
    }
    private static void plume(Painter p,float x,float y,float r,int seed,int color,float a) {
        float[] leaf=new float[24];
        for(int i=0;i<9;i++) {
            float angle=-2.9f+i*.32f,rr=r*(.55f+hash(seed+i)*.48f);
            float dx=(float)Math.cos(angle)*rr,dy=(float)Math.sin(angle)*rr;
            for(int layer=0;layer<2;layer++) {
                for(int j=0;j<6;j++) {
                    float t=j/5f,width=r*.09f*(float)Math.sin(t*Math.PI)*(layer==0?1:.52f);
                    float xx=x+dx*t*t,yy=y+dy*t;
                    leaf[j*2]=xx-width;leaf[j*2+1]=yy;
                    leaf[(11-j)*2]=xx+width;leaf[(11-j)*2+1]=yy;
                }
                p.fillPoly(leaf,ink(layer==0?Glyph.mix(color,0xFF122D48,.30f):color,a*(layer==0?240:185)));
            }
        }
    }
}
