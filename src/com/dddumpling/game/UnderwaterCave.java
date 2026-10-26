package com.dddumpling.game;

/** A stationary reef chamber: shaded stone and its relighting are cached separately. */
final class UnderwaterCave extends Draw {
    private UnderwaterCave() {}
    private static int ink(int color,float alpha){return Glyph.withAlpha(color,(int)alpha);}

    static void draw(Painter p,GameCore c,Layout L,float fade,float arrival) {
        if(p instanceof WaterPainter)p=((WaterPainter)p).anchored();
        UnderwaterVista.image(p,UnderwaterVista.CAVE,0,0,L.w,L.h,fade);
        UnderwaterVista.beams(p,c,L,fade*1.15f);
        float y=-(1-arrival)*L.h*.16f;
        UnderwaterVista.image(p,UnderwaterVista.CAVE+1,0,y,L.w,L.h,fade);
        float pulse=c.waterRipples.beamPulse(c.clock,.5f);
        float shimmer=.055f+.045f*(.5f+.5f*(float)Math.sin(c.clock*.47f));
        UnderwaterVista.image(p,UnderwaterVista.CAVE+2,0,y,L.w,L.h,fade*Math.min(1,shimmer+pulse*.70f));
    }

    static void paint(Painter p,int layer,float x,float y,float w,float h,float fade) {
        if(layer==0){chamber(p,x,y,w,h,fade);return;}
        Painter stone=layer==2?new LitStone(p):new DarkStone(p);
        // Three overlapping planes frame the battle without crowding its centre.
        for(int plane=0;plane<3;plane++)for(int side=0;side<2;side++) {
            float width=w*(.40f+plane*.16f),height=h*(.96f+plane*.19f);
            float left=side==0?x-width*(.55f+plane*.04f):x+w-width*(.45f-plane*.04f);
            UnderwaterVista.artwork(stone,1+plane*2+side,left,y-h*(.02f+plane*.15f),width,height,fade);
        }
        // A broad eroded arch closes the roof; lit ledges follow the same contour.
        float[] arch=new float[56];
        for(int band=0;band<4;band++) {
            arch[0]=x-w*.1f;arch[1]=y-h*.1f;
            for(int j=0;j<25;j++) {
                float u=j/24f,edge=Math.abs(u-.5f)*2;
                float roof=.13f+.26f*edge*edge-band*.025f;
                roof+=.013f*(float)Math.sin(u*23+band*.4f)+.007f*(float)Math.sin(u*49);
                arch[2+j*2]=x+w*(u*1.2f-.1f);arch[3+j*2]=y+h*roof;
            }
            arch[52]=x+w*1.1f;arch[53]=y-h*.1f;arch[54]=arch[0];arch[55]=arch[1];
            int color=band==0?0xFF467A85:band==1?0xFF173344:band==2?0xFF0A1B2A:0xFF030A16;
            stone.fillPoly(arch,ink(color,fade*255));
        }
        // Small luminous colonies sit on the floor, with dark stone around them.
        for(int side=0;side<2;side++) {
            float cx=x+w*(side==0?.20f:.80f),cy=y+h*(side==0?.79f:.82f);
            UnderwaterCoral.colony(stone,cx,cy,w*.065f,419+side*93,side==0?2:4,fade,419,
                    side==0?0xFF6AACAB:0xFF848FBD);
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

    private static final class DarkStone extends OpacityPainter {
        DarkStone(Painter p){super(p,1);}
        int color(int c){return UnderwaterVista.shadeColor(c,.72f);}
        public ArtStamp stamps(){return null;}
    }
    private static final class LitStone extends OpacityPainter {
        LitStone(Painter p){super(p,1);}
        int color(int c) {
            float light=(((c>>>16)&255)*.21f+((c>>>8)&255)*.72f+(c&255)*.07f)/255;
            return ink(0xFFA2DBD7,(c>>>24)*Math.min(1,light*light*4));
        }
        public ArtStamp stamps(){return null;}
    }
}
