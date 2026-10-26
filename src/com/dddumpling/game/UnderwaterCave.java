package com.dddumpling.game;

/** A stationary reef chamber: shaded stone and its relighting are cached separately. */
final class UnderwaterCave extends Draw {
    private UnderwaterCave() {}
    private static int ink(int color,float alpha){return Glyph.withAlpha(color,(int)alpha);}

    static void draw(Painter p,GameCore c,Layout L,float fade,float arrival) {
        if(p instanceof WaterPainter)p=((WaterPainter)p).anchored();
        UnderwaterVista.image(p,UnderwaterVista.CAVE,0,0,L.w,L.h,fade);
        for(int i=0;i<3;i++) {
            float pulse=c.waterRipples.beamPulse(c.clock,.24f+i*.25f);
            float drift=(float)Math.sin(c.clock*.17f+i*2.4f)*L.w*.045f;
            float light=.20f+UnderwaterVista.beamIdle(c.clock*.7f,i)*.65f+pulse*.30f;
            UnderwaterVista.image(p,UnderwaterVista.CAVE+3,L.w*(-.22f+i*.30f)+drift,
                    -L.h*(.08f+i*.025f),L.w*(.85f-i*.08f),L.h*(.94f-i*.04f),fade*light);
        }
        float y=-(1-arrival)*L.h*.16f;
        UnderwaterVista.image(p,UnderwaterVista.CAVE+1,0,y,L.w,L.h,fade);
        float pulse=c.waterRipples.beamPulse(c.clock,.5f);
        float shimmer=.055f+.045f*(.5f+.5f*(float)Math.sin(c.clock*.47f));
        UnderwaterVista.image(p,UnderwaterVista.CAVE+2,0,y,L.w,L.h,fade*Math.min(1,shimmer+pulse*.85f));
    }

    static void paint(Painter p,int layer,float x,float y,float w,float h,float fade) {
        if(layer==0){chamber(p,x,y,w,h,fade);return;}
        if(layer==3){mist(p,x,y,w,h,fade);return;}
        Painter stone=layer==2?new LitStone(p):p;
        // Broken buttresses have independent silhouettes, leaving an off-centre ceiling fissure.
        for(int plane=0;plane<3;plane++)for(int side=0;side<2;side++)
            buttress(stone,x,y,w,h,side,plane,fade);
        shard(stone,x,y,w,h,new float[]{.32f,-.06f,.52f,-.05f,.47f,.025f,.48f,.07f,.43f,.155f,.41f,.08f,.35f,.10f},
                0xFF081928,0xFF386B7A,fade);
        shard(stone,x,y,w,h,new float[]{.65f,-.05f,.86f,-.04f,.82f,.11f,.76f,.18f,.75f,.09f,.69f,.13f,.67f,.06f},
                0xFF030D18,0xFF284D60,fade);
        // Fractured shelves interrupt the cave floor instead of forming a neat arch base.
        shard(stone,x,y,w,h,new float[]{-.06f,.80f,.10f,.77f,.19f,.81f,.24f,.80f,.29f,.86f,.18f,.87f,.13f,.92f,-.06f,.96f},
                0xFF06131D,0xFF2B535D,fade);
        shard(stone,x,y,w,h,new float[]{1.05f,.75f,.91f,.78f,.87f,.81f,.77f,.82f,.72f,.87f,.85f,.89f,.88f,.95f,1.05f,.93f},
                0xFF030A12,0xFF294A5C,fade);
        for(int side=0;side<2;side++)
            UnderwaterCoral.colony(stone,x+w*(side==0?.17f:.84f),y+h*(side==0?.84f:.86f),w*.055f,
                    419+side*93,side==0?2:4,fade*.60f,419,side==0?0xFF6AACA8:0xFF777FAA);
    }

    private static final float[] LEFT={-.12f,-.1f,.36f,-.1f,.33f,.06f,.26f,.11f,.29f,.17f,
        .23f,.20f,.27f,.28f,.17f,.25f,.13f,.34f,.18f,.38f,.12f,.45f,.17f,.51f,
        .10f,.56f,.18f,.62f,.14f,.68f,.22f,.73f,.13f,.78f,.16f,.87f,.21f,1.08f,-.12f,1.08f};
    private static final float[] RIGHT={1.12f,-.1f,.68f,-.1f,.72f,.06f,.64f,.12f,.71f,.17f,
        .66f,.24f,.79f,.21f,.83f,.28f,.77f,.33f,.86f,.39f,.80f,.45f,.87f,.51f,
        .81f,.59f,.87f,.64f,.78f,.70f,.85f,.75f,.80f,.84f,.84f,.91f,.77f,1.08f,1.12f,1.08f};

    private static void buttress(Painter p,float x,float y,float w,float h,int side,int plane,float fade) {
        float[] original=side==0?LEFT:RIGHT,edge=new float[original.length];
        float sign=side==0?1:-1,shift=(1-plane)*.047f;
        for(int j=0;j<edge.length;j+=2) {
            edge[j]=x+w*(original[j]+sign*shift+(hash(911+j*7+side*83+plane*39)-.5f)*.025f);
            edge[j+1]=y+h*(original[j+1]+(hash(559+j+plane*23+side*71)-.5f)*.026f);
        }
        int body=plane==0?0xFF123344:plane==1?0xFF091B29:0xFF020913;
        p.fillPoly(edge,ink(body,fade*255));
        // Split rock faces catch cool light on only one side of each fractured edge.
        for(int j=2;j<edge.length-4;j+=2) {
            float ax=edge[j],ay=edge[j+1],bx=edge[j+2],by=edge[j+3];
            float deep=(.07f+hash(j+plane*61+side*41)*.11f)*w;
            float[] face={ax,ay,bx,by,bx-sign*deep,by+h*.02f,ax-sign*deep*.65f,ay-h*.015f};
            int col=(j/2+plane+side)%3==0?0xFF31596B:(j/2+side)%2==0?0xFF19394B:0xFF0C2434;
            p.fillPoly(face,ink(col,fade*(plane==2?190:235)));
            if(j%4==0) {
                p.polyline(new float[]{ax-sign*w*.008f,ay,bx-sign*w*.008f,by,bx-sign*deep*.48f,by+h*.014f},
                        ink(plane==0?0xFF5F999E:0xFF406C80,fade*(plane==2?90:145)),w*(plane==0?.003f:.002f));
            }
            p.line(ax-sign*deep*.4f,ay+h*.012f,bx-sign*deep*.6f,by,ink(0xFF01050E,fade*160),w*.004f);
        }
    }

    private static void shard(Painter p,float x,float y,float w,float h,float[] points,int body,int rim,float fade) {
        float[] shape=points.clone();
        for(int i=0;i<shape.length;i+=2){shape[i]=x+w*shape[i];shape[i+1]=y+h*shape[i+1];}
        p.fillPoly(shape,ink(body,fade*255));
        float[] facet=new float[shape.length];
        for(int i=0;i<shape.length;i+=2){facet[i]=shape[i]+w*.009f;facet[i+1]=shape[i+1]-h*.009f;}
        p.fillPoly(facet,ink(rim,fade*80));
        p.polyline(shape,ink(rim,fade*110),w*.0025f);
    }

    private static void mist(Painter p,float x,float y,float w,float h,float fade) {
        float[] ribbon=new float[164];
        float previous=0;
        // Continuous feathered ribbons avoid horizontal strip edges in the light shafts.
        for(int layer=0;layer<28;layer++) {
            float radius=3*(1-layer/28f),opacity=180*(float)Math.exp(-radius*radius*.5f);
            int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
            for(int j=0;j<=40;j++) {
                float u=j/40f,start=(3-radius)*.035f,end=1-(3-radius)*.08f;
                float t=start+(end-start)*u,centre=.28f+.30f*t+.10f*(float)Math.sin(t*5.5f);
                float spread=(.025f+.11f*t)*radius*(float)Math.pow(Math.sin(u*Math.PI),.65f);
                ribbon[j*2]=x+w*(centre-spread);ribbon[j*2+1]=y+h*t;
                ribbon[(81-j)*2]=x+w*(centre+spread);ribbon[(81-j)*2+1]=y+h*t;
            }
            p.fillPoly(ribbon,ink(layer<14?0xFF477FA8:0xFFABF2DB,fade*alpha));
        }
    }

    private static void chamber(Painter p,float x,float y,float w,float h,float fade) {
        // Tiny baked dithering avoids broad color bands in the near-black gradient.
        for(int row=0;row<256;row++) {
            float t=(row+.5f)/256f;
            float light=(float)Math.exp(-Math.pow((t-.46f)/.24f,2));
            for(int col=0;col<32;col++) {
                float noise=hash(935+row*37+col);
                int r=(int)(2+18*light+noise),g=(int)(8+50*light+noise),b=(int)(19+58*light+noise);
                p.fillRect(x+w*col/32,y+(float)Math.floor(h*row/256),
                        x+w*(col+1)/32,y+(float)Math.floor(h*(row+1)/256),
                        ink(0xFF000000|(r<<16)|(g<<8)|b,fade*255));
            }
        }
        // A softly lit, irregular floor continues under the keys, without a horizon seam.
        for(int layer=0;layer<18;layer++) {
            float[] floor=new float[44];float inset=layer*.008f;
            for(int j=0;j<20;j++) {
                float u=j/19f;
                floor[j*2]=x+w*(u*(1-inset*2)+inset);
                floor[j*2+1]=y+h*(.69f+inset*.65f+.10f*(u-.5f)*(u-.5f)
                        +.008f*(float)Math.sin(u*11)+.004f*(float)Math.sin(u*29));
            }
            floor[40]=x+w;floor[41]=y+h*1.1f;floor[42]=x;floor[43]=y+h*1.1f;
            p.fillPoly(floor,ink(0xFF59716F,fade*5));
        }
        for(int row=0;row<15;row++) {
            float t=row/14f,yy=y+h*(.72f+t*.34f),span=w*(.18f+t*.80f);
            float[] ridge=new float[26];
            for(int j=0;j<13;j++) {
                float u=j/12f;
                ridge[j*2]=x+w*.5f+(u-.5f)*span;
                ridge[j*2+1]=yy+h*(.003f+t*.006f)*(float)Math.sin(u*7+row*1.9f);
            }
            p.polyline(ridge,ink(0xFF8CA6A0,fade*(9+t*9)),w*(.001f+t*.004f));
        }
    }

    private static final class LitStone extends OpacityPainter {
        LitStone(Painter p){super(p,1);}
        int color(int c) {
            float light=(((c>>>16)&255)*.21f+((c>>>8)&255)*.72f+(c&255)*.07f)/255;
            light=Math.max(0,(light-.08f)*1.8f);
            return ink(0xFF9CE3DB,(c>>>24)*Math.min(1,light*light*3));
        }
        public ArtStamp stamps(){return null;}
    }
}
