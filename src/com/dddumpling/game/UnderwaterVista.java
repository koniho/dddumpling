package com.dddumpling.game;

/** A lit reef corridor: immutable scenery passes through a small perspective field. */
final class UnderwaterVista extends Draw {
    static final int COUNT=7,WIDTH=256,ROWS=5;
    static int height(int art) {return art==0?512:768;}
    private UnderwaterVista() {}
    private static int ink(int color,float alpha) {return Glyph.withAlpha(color,(int)alpha);}
    private static float smooth(float v) {v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    static float depth(float travel) {return travel/(4-3*travel);}
    static float visibility(float travel) {return smooth(travel/.22f);}
    static float rootY(float travel) {return .50f+depth(travel)*.82f;}
    static float scale(float travel) {return .16f+depth(travel)*1.50f;}

    static float columnHeight(float travel,float width) {
        return width*height(1)/WIDTH*(1+.95f*smooth(depth(travel)));
    }
    static float columnX(float travel,int side) {
        return .5f+(side==0?-1:1)*(.14f+depth(travel)*.90f);
    }
    static float travel(float clock,int row,int side) {
        float phase=clock*.10f+side*.43f;
        return (row+phase-(float)Math.floor(phase))/ROWS;
    }

    static void draw(Painter p,GameCore c,Layout L,float fade) {
        if(fade<.004f)return;
        if(p instanceof WaterPainter)p=((WaterPainter)p).anchored();
        image(p,0,0,0,L.w,L.h,fade);
        surface(p,c,L,fade);
        ground(p,c,L,fade);
        // Each column and all its decorations share one cached image and projection.
        // The two rows of five emerge at the horizon and leave through the side edges.
        for(int row=0;row<ROWS;row++)for(int side=0;side<2;side++) {
            float clock=c.skyClock*.10f+side*.43f;
            int cycle=(int)Math.floor(clock);
            float t=travel(c.skyClock,row,side),d=depth(t),s=scale(t);
            int art=1+Math.floorMod(row-cycle+side*3,6);
            float x=L.w*columnX(t,side),y=L.h*rootY(t),w=L.w*.60f*s,h=columnHeight(t,w);
            if(x+w*.5f<0 || x-w*.5f>L.w)continue;
            float alpha=fade*visibility(t)*(.38f+.62f*smooth(d*2.4f));
            image(p,art,x-w*.5f,y-h*.90f,w,h,alpha);
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
        float r=w*.34f,cx=x+w*.5f,root=y+h*.90f;
        int seed=419+art*83;
        // Crown and ledge plants are painted with the rock, so nothing floats independently.
        fan(p,cx-r*.12f,y+h*.22f,r*.63f,seed,0xFF578E97,fade*.65f);
        column(p,x,y,w,h,seed,fade);
        for(int ledge=0;ledge<3;ledge++) {
            float sign=(ledge+art)%2==0?-1:1;
            float yy=y+h*(.30f+ledge*.23f),xx=cx+sign*r*(.48f+hash(seed+ledge)*.13f);
            stone(p,xx,yy,r*.42f,r*.24f,seed+ledge*19,fade);
            if(ledge==1)UnderwaterCoral.colony(p,xx,yy-r*.10f,r*.46f,seed,art%5,fade*.80f,seed,
                    art%3==0?0xFF77ABA1:art%3==1?0xFFA77B9F:0xFF8A8CB7);
            else plume(p,xx,yy-r*.10f,r*(.38f+ledge*.04f),seed+ledge*31,
                    art%2==0?0xFF7299B3:0xFFAD87A0,fade*.85f);
        }
        // Broad attached footing and loose toe stones meet the sand without a flat cut.
        stone(p,cx,root-r*.025f,r*1.12f,r*.26f,seed+7,fade);
        stone(p,cx-r*.76f,root+r*.025f,r*.35f,r*.18f,seed+29,fade);
        stone(p,cx+r*.72f,root+r*.05f,r*.30f,r*.17f,seed+53,fade);
        plume(p,cx+r*.35f,root-r*.08f,r*.55f,seed+37,0xFF719B9D,fade);
    }
    private static void column(Painter p,float x,float y,float w,float h,int seed,float a) {
        float[] contour=new float[72],rim=new float[72];
        float crown=.09f+hash(seed+61)*.12f;
        for(int j=0;j<13;j++) {
            float t=j/12f,yy=crown+.025f+t*(.875f-crown);
            float flare=.12f*smooth((t-.70f)/.30f);
            float taper=.45f+.55f*(float)Math.sin(Math.PI*(.12f+t*.78f));
            float left=(.25f+.055f*(float)Math.sin(j*1.9f+seed)+.025f*hash(seed+j))*taper+flare;
            float right=(.27f+.055f*(float)Math.sin(j*1.7f+seed+2))*taper+flare;
            float mid=.48f+.025f*(float)Math.sin(t*7+seed);
            contour[j*2]=mid-left;contour[j*2+1]=yy;
            contour[(30-j)*2]=mid+right;
            contour[(30-j)*2+1]=yy+.010f*(float)Math.sin(j*1.3f+seed);
        }
        for(int i=0;i<5;i++) {
            float u=(i+1)/6f;
            contour[(13+i)*2]=contour[24]+(contour[36]-contour[24])*u;
            contour[(13+i)*2+1]=.90f+.025f*(float)Math.sin(u*Math.PI)+hash(seed+i+19)*.007f;
            contour[(31+i)*2]=contour[60]+(contour[0]-contour[60])*u;
            contour[(31+i)*2+1]=crown-.020f*(float)Math.sin(u*Math.PI)
                    +.012f*(float)Math.sin(u*13+seed)+hash(seed+i+31)*.008f;
        }
        for(int layer=0;layer<3;layer++) {
            for(int j=0;j<contour.length;j+=2) {
                rim[j]=x+w*(.48f+(contour[j]-.48f)*(1-layer*.19f)-layer*.018f);
                rim[j+1]=y+h*(contour[j+1]-layer*.006f);
            }
            p.fillPoly(rim,ink(layer==0?0xFF102B46:layer==1?0xFF2C5068:0xFF53798B,a*(layer==2?130:245)));
        }
        for(int i=0;i<7;i++) {
            float yy=y+h*(.25f+i*.085f),xx=x+w*(.33f+hash(seed+i)*.10f);
            p.polyline(new float[]{xx,yy,xx+w*.12f,yy+h*.012f,xx+w*.27f,yy-h*.01f},
                    ink(0xFF112C43,a*60),w*.012f);
            p.line(xx,yy-h*.008f,xx+w*.16f,yy-h*.005f,ink(0xFF8BB0B6,a*36),w*.009f);
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
    }
    private static void ground(Painter p,GameCore c,Layout L,float a) {
        float[] line=new float[26];
        // Fixed rows in depth, projected into accelerating, widening sand ripples.
        for(int row=0;row<12;row++) {
            float phase=c.skyClock*.20f;
            int cycle=(int)Math.floor(phase);
            float t=(row+phase-cycle)/12f,d=depth(t),yy=L.h*rootY(t);
            float alpha=a*visibility(t)*smooth((1.12f-yy/L.h)/.12f);
            if(alpha<=.001f || yy>L.h*1.04f)continue;
            int seed=Math.floorMod(row-cycle,12);
            float span=L.w*(.08f+d*1.75f);
            for(int j=0;j<13;j++) {
                float u=j/12f;
                line[j*2]=L.w*.5f+(u-.5f)*span;
                line[j*2+1]=yy+L.h*d*(.012f*(float)Math.sin(u*8+seed*1.3f)+.024f*(u-.5f)*(u-.5f));
            }
            float width=L.w*(.001f+d*.014f);
            p.polyline(line,ink(0xFF9BAEAD,alpha*(7+d*9)),width*2.4f);
            p.polyline(line,ink(0xFF9BAEAD,alpha*(10+d*10)),width);
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
