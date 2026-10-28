package com.dddumpling.game;

/** A lit reef corridor: immutable scenery passes through a small perspective field. */
final class UnderwaterVista extends Draw {
    static final int COUNT=30,WIDTH=256,ROWS=5,RAY=11,SOFT=12,CAVE=22;
    static final float COLUMN_WIDTH=1.08f;
    static int width(int art) {return art>=SOFT && art<CAVE?128:WIDTH;}
    static int height(int art) {return art>=CAVE?512:art>=SOFT?384:art==0 || art==RAY?512:768;}
    private UnderwaterVista() {}
    private static int ink(int color,float alpha) {return Glyph.withAlpha(color,(int)alpha);}
    private static float smooth(float v) {v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    static float depth(float travel) {return travel/(4-3*travel);}
    static float visibility(float travel) {return smooth(travel/.22f);}
    static float rootY(float travel) {return .50f+depth(travel)*.82f;}
    static float depthShade(float travel) {return .12f+.88f*smooth(depth(travel)*1.8f);}
    static float edgeSoftness(float travel) {return 1-smooth((depth(travel)-.04f)/.40f);}
    static float scale(float travel) {return .16f+depth(travel)*1.50f;}

    static float columnHeight(float travel,float width) {
        return width*height(1)/WIDTH*(1+.95f*smooth(depth(travel)));
    }
    static float columnX(float travel,int side) {
        float d=depth(travel);
        // Carry the wider foreground pieces fully past the sides before recycling.
        return .5f+(side==0?-1:1)*(.14f+d*.90f+.45f*d*d*d*d);
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
            int art=1+2*Math.floorMod(row-cycle+side*3,ROWS)+side;
            float x=L.w*columnX(t,side),y=L.h*rootY(t),w=L.w*COLUMN_WIDTH*s,h=columnHeight(t,L.w*.60f*s);
            if(x+w*.5f<0 || x-w*.5f>L.w)continue;
            float alpha=fade*visibility(t);
            float soft=alpha*edgeSoftness(t);
            if(soft>.004f)shadedImage(p,SOFT+art-1,x-w*.5f,y-h*.90f,w,h,soft,depthShade(t));
            shadedImage(p,art,x-w*.5f,y-h*.90f,w,h,alpha,depthShade(t));
        }
        for(int i=0;i<18;i++) {
            float t=(hash(727+i)*1.0f+c.skyClock*.018f)%1,d=depth(t);
            float x=L.w*(.5f+(hash(913+i)-.5f)*(.35f+d*1.5f));
            float y=L.h*(.13f+d*1.13f),r=L.w*(.0008f+.002f*d);
            p.fillCircle(x,y,r*2.6f,ink(0xFF70D8DB,fade*visibility(t)*10));
            p.fillCircle(x,y,r,ink(0xFFACDBDA,fade*visibility(t)*(28+d*35)));
        }
    }

    static void image(Painter p,int art,float x,float y,float w,float h,float fade) {
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.reefVista(art,x,y,w,h,(int)(fade*255)))return;
        artwork(p,art,x,y,w,h,fade);
    }
    private static void shadedImage(Painter p,int art,float x,float y,float w,float h,float fade,float shade) {
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.reefVistaShaded(art,x,y,w,h,(int)(fade*255),shade,0))return;
        artwork(new DimPainter(p,shade),art,x,y,w,h,fade);
    }
    static int shadeColor(int color,float shade) {
        return (color&0xFF000000)|((int)(((color>>>16)&255)*shade)<<16)
                |((int)(((color>>>8)&255)*shade)<<8)|(int)((color&255)*shade);
    }
    static int waterColor(float mono) {
        int color=0xFF17445C;
        int gray=(23*54+68*183+92*19)/256;
        return Glyph.mix(color,0xFF000000|(gray<<16)|(gray<<8)|gray,mono);
    }
    static int depthColor(int color,float clarity) {
        return (color&0xFF000000)|(Glyph.mix(waterColor(0),color,clarity)&0xFFFFFF);
    }
    private static final class RockPalette extends OpacityPainter {
        RockPalette(Painter p){super(p,1);}
        int color(int c){return shadeColor(c,.76f);}
        public ArtStamp stamps(){return null;}
    }
    private static final class DimPainter extends OpacityPainter {
        final float shade;
        DimPainter(Painter p,float shade){super(p,1);this.shade=shade;}
        int color(int c){return depthColor(c,shade);}
        public ArtStamp stamps(){return null;}
    }
    static void paint(Painter p,int art) {artwork(p,art,0,0,width(art),height(art),1);}
    static void artwork(Painter p,int art,float x,float y,float w,float h,float fade) {
        if(art>=CAVE){UnderwaterCave.paint(p,art-CAVE,x,y,w,h,fade);return;}
        if(art==0){backdrop(p,x,y,w,h,fade);return;}
        if(art==RAY){sunbeam(p,x,y,w,h,fade);return;}
        boolean soft=art>=SOFT;
        if(soft)art=art-SOFT+1;
        p=new CurvedColumn(new RockPalette(p),art,y,w,h);
        float r=w*.34f,cx=x+w*.5f,root=y+h*.90f;
        int seed=419+art*83;
        // Crown and ledge plants are painted with the rock, so nothing floats independently.
        if(!soft)fan(p,cx-r*.12f,y+h*.22f,r*.63f,seed,0xFF578E97,fade*.65f);
        column(p,x,y,w,h,seed,fade,soft);
        for(int ledge=0;ledge<3;ledge++) {
            float sign=(ledge+art)%2==0?-1:1;
            float yy=y+h*(.30f+ledge*.23f),xx=cx+sign*r*(.48f+hash(seed+ledge)*.13f);
            stone(p,xx,yy,r*.42f,r*.24f,seed+ledge*19,fade,soft);
            if(soft)continue;
            if(ledge==1)UnderwaterCoral.colony(p,xx,yy-r*.10f,r*.46f,seed,art%5,fade*.80f,seed,
                    art%3==0?0xFF77ABA1:art%3==1?0xFFA77B9F:0xFF8A8CB7);
            else plume(p,xx,yy-r*.10f,r*(.38f+ledge*.04f),seed+ledge*31,
                    art%2==0?0xFF7299B3:0xFFAD87A0,fade*.85f);
        }
        // Broad attached footing and loose toe stones meet the sand without a flat cut.
        stone(p,cx,root-r*.025f,r*1.12f,r*.26f,seed+7,fade,soft);
        stone(p,cx-r*.76f,root+r*.025f,r*.35f,r*.18f,seed+29,fade,soft);
        stone(p,cx+r*.72f,root+r*.05f,r*.30f,r*.17f,seed+53,fade,soft);
        if(!soft)plume(p,cx+r*.35f,root-r*.08f,r*.55f,seed+37,0xFF719B9D,fade);
    }
    static float bow(int art,float y) {
        float t=Math.max(0,Math.min(1,(.91f-y)/.82f));
        int variant=(art-1)/2;
        float amount=.025f+.12f*variant/4f;
        // Four of five profiles lean at the crown; one keeps a lower bulge.
        float curve=variant==3?(float)Math.pow(Math.sin(t*Math.PI),1.95):t*t;
        return (art%2==1?1:-1)*amount*curve;
    }
    // Warp the whole painting once, including its plants, seams, crown and footing.
    private static final class CurvedColumn extends OpacityPainter {
        final int art;final float y,w,h;
        CurvedColumn(Painter p,int art,float y,float w,float h){super(p,1);this.art=art;this.y=y;this.w=w;this.h=h;}
        private float dx(float yy){return w*bow(art,(yy-y)/h);}
        private float[] curve(float[] xy) {
            float[] q=xy.clone();for(int i=0;i<q.length;i+=2)q[i]+=dx(q[i+1]);return q;
        }
        public ArtStamp stamps(){return null;}
        public void fillPoly(float[] xy,int c){p.fillPoly(curve(xy),c);}
        public void strokePoly(float[] xy,int c,float r){p.strokePoly(curve(xy),c,r);}
        public void polyline(float[] xy,int c,float r){p.polyline(curve(xy),c,r);}
        public void fillCircle(float x,float y,float r,int c){p.fillCircle(x+dx(y),y,r,c);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){p.fillEllipse(x+dx(y),y,rx,ry,c);}
        public void strokeCircle(float x,float y,float r,int c,float s){p.strokeCircle(x+dx(y),y,r,c,s);}
        public void line(float x,float y,float xx,float yy,int c,float r){p.line(x+dx(y),y,xx+dx(yy),yy,c,r);}
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float r){p.arc(x+dx(y),y,rx,ry,start,sweep,c,r);}
    }
    private static void sunbeam(Painter p,float x,float y,float w,float h,float a) {
        float[] quad=new float[8];
        // Soft edges and length falloff are baked into one shared small texture.
        for(int row=0;row<32;row++) {
            float t=row/32f,u=(row+1)/32f,fade=(float)Math.pow(1-(t+u)*.5f,1.5);
            float previous=0;
            for(int layer=0;layer<24;layer++) {
                float radius=3*(1-layer/24f),opacity=255*fade*(float)Math.exp(-.5f*radius*radius);
                int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
                if(alpha<=0)continue;
                float top=(.06f+t*.23f)*radius/3,bottom=(.06f+u*.23f)*radius/3;
                float tx=.30f+t*.34f,bx=.30f+u*.34f;
                quad[0]=x+w*(tx-top);quad[1]=y+h*t;quad[2]=x+w*(tx+top);quad[3]=y+h*t;
                quad[4]=x+w*(bx+bottom);quad[5]=y+h*u;quad[6]=x+w*(bx-bottom);quad[7]=y+h*u;
                p.fillPoly(quad,ink(0xFFFFF2CE,a*alpha));
            }
        }
    }
    // Feathered silhouette is rasterized once at half resolution, never blurred live.
    private static void softEdge(Painter p,float[] contour,float w,float alpha) {
        for(int i=0;i<10;i++) {
            float radius=3*(1-i/10f),outer=radius+.3f;
            float opacity=(float)(Math.exp(-radius*radius*.5)-Math.exp(-outer*outer*.5));
            p.strokePoly(contour,ink(0xFF102B46,alpha*255*opacity),w*.050f*radius);
        }
    }
    private static void column(Painter p,float x,float y,float w,float h,int seed,float a,boolean soft) {
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
        for(int layer=0;layer<(soft?1:3);layer++) {
            for(int j=0;j<contour.length;j+=2) {
                rim[j]=x+w*(.48f+(contour[j]-.48f)*(1-layer*.19f)-layer*.018f);
                rim[j+1]=y+h*(contour[j+1]-layer*.006f);
            }
            if(soft){softEdge(p,rim,w,a);return;}
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
            // Integer texel boundaries avoid antialiased gaps between adjacent strips.
            p.fillRect(x,y+(float)Math.floor(h*row/80),x+w,y+(float)Math.floor(h*(row+1)/80),ink(col,a*255));
        }
        for(int layer=63;layer>=0;layer--) {
            float t=layer/63f;
            p.fillEllipse(x+w*.48f,y-h*.06f,w*(.13f+t*.52f),h*(.15f+t*.54f),ink(0xFF75DFDD,a*1.8f));
        }
        // Continuous polygons remove strip seams; the wider floor tucks under both banks.
        float[] path=new float[100];
        for(int layer=0;layer<6;layer++) {
            int at=0;
            for(int j=0;j<=16;j++) {
                float xx=.5f+(.20f-layer*.003f)*(j/8f-1);
                path[at++]=x+w*xx;path[at++]=y+h*(horizon(xx)+layer*.002f);
            }
            for(int j=1;j<=16;j++) {
                float t=j/16f,xx=floorEdge(t,1,layer);
                path[at++]=x+w*xx;path[at++]=y+h*floorY(t,xx,layer);
            }
            float xx=floorEdge(1,-1,layer);
            path[at++]=x+w*xx;path[at++]=y+h*floorY(1,xx,layer);
            for(int j=15;j>=0;j--) {
                float t=j/16f;xx=floorEdge(t,-1,layer);
                path[at++]=x+w*xx;path[at++]=y+h*floorY(t,xx,layer);
            }
            p.fillPoly(path,ink(Glyph.mix(0xFF84958C,0xFFA99B83,layer/5f),a*(18+layer)));
        }
    }
    static float horizon(float x) {
        return .48f+.003f*(float)Math.sin(x*14+.8f)+.0015f*(float)Math.sin(x*29);
    }
    static float floorEdge(float t,int side,int layer) {
        return .5f+.035f*(float)Math.sin(t*7)+side*(.20f-layer*.003f+t*(.82f-layer*.018f));
    }
    private static float floorY(float t,float x,int layer) {
        return .48f+t*.60f+(horizon(x)-.48f+layer*.002f)*(1-t)*(1-t);
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
            p.polyline(line,ink(0xFFC3C8B2,alpha*(9+d*10)),width*2.4f);
            p.polyline(line,ink(0xFFC3C8B2,alpha*(14+d*12)),width);
        }
    }
    static float beamIdle(float clock,int beam) {
        float wave=.5f+.5f*(float)Math.sin(clock*(.38f+beam*.035f)+beam*2.1f);
        return .035f+.44f*wave*wave;
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
            p.polyline(line,ink(0xFFFFF4D9,a*(20-i*1.5f)),L.w*(.003f+i*.0003f));
        }
        beams(p,c,L,a);
    }
    static void beams(Painter p,GameCore c,Layout L,float a) {
        for(int i=0;i<5;i++) {
            float sway=L.w*.035f*(float)Math.sin(c.clock*.18f+i*1.7f);
            float x=L.w*(-.13f+i*.23f)+sway;
            float pulse=c.waterRipples.beamPulse(c.clock,x/L.w+.22f);
            float alpha=a*Math.min(1,beamIdle(c.clock,i)+pulse*.72f);
            float w=L.w*(.58f+i%2*.16f),h=L.h*(.51f+i%3*.07f);
            image(p,RAY,x-w*.18f,-L.h*.035f,w*1.6f,h*1.1f,alpha*.40f);
            image(p,RAY,x,-L.h*.035f,w,h,alpha);
        }
    }
    private static void stone(Painter p,float x,float y,float rx,float ry,int seed,float a,boolean soft) {
        float[] rim=new float[56];
        for(int layer=0;layer<(soft?1:3);layer++) {
            for(int j=0;j<28;j++) {
                float angle=j*Softbody.TAU/28;
                float edge=.91f+.06f*(float)Math.sin(angle*3+seed)+.035f*(float)Math.cos(angle*5+seed*.7f);
                rim[j*2]=x-rx*layer*.055f+(float)Math.cos(angle)*rx*edge*(1-layer*.14f);
                rim[j*2+1]=y-ry*layer*.15f+(float)Math.sin(angle)*ry*edge*(1-layer*.21f);
            }
            if(soft){softEdge(p,rim,rx*3,a);return;}
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
