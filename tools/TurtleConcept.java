package com.dddumpling.game;

/** #150 review-only concepts. Deliberately not included in the shipped catalogue. */
final class TurtleConcept extends Draw {
    static final String[] NAMES={"LIME LULLABY","LILAC LAP","PEACH PADDLE","LAGOON LOAF","RAINBOW RAMBLE"};
    static final int EDGE=0xFF252047;
    static final int[] SKIN={0xFFD2F589,0xFFD7B1F2,0xFFFFCD9C,0xFF98E3D6,0xFFB9E8FF};
    static final int[] SHELL={0xFF459E71,0xFF9466B8,0xFFD28572,0xFF5185AA,0xFFAC81DA};
    private static void oval(Painter p,float x,float y,float rx,float ry,int color) {
        p.fillEllipse(x,y,rx,ry,EDGE);p.fillEllipse(x,y,rx*.91f,ry*.91f,color);
    }
    static void draw(Painter p,int variant,float x,float y,float r) {
        int skin=SKIN[variant],shell=SHELL[variant];
        // The flippers and small layered shell sit below the oversized head.
        oval(p,x-r*.55f,y+r*.63f,r*.37f,r*.19f,shell);
        oval(p,x+r*.55f,y+r*.63f,r*.37f,r*.19f,shell);
        oval(p,x,y+r*.43f,r*.70f,r*.51f,shell);
        p.fillEllipse(x-r*.16f,y+r*.26f,r*.47f,r*.29f,Glyph.mix(shell,0xFFFFFFFF,.22f));
        p.strokePoly(Glyph.hex(x,y+r*.40f,r*.28f),Glyph.mix(shell,EDGE,.28f),r*.045f);
        oval(p,x,y+r*.63f,r*.55f,r*.27f,0xFFE1AE75);
        p.fillEllipse(x,y+r*.57f,r*.43f,r*.14f,0xFFFFE7A2);
        p.line(x,y+r*.52f,x,y+r*.83f,0xFFC28C66,r*.03f);
        for(int d=-1;d<=1;d+=2) {
            float[] flipper=new float[48];
            for(int i=0;i<24;i++) {
                float a=i*Softbody.TAU/24;
                float u=(float)Math.cos(a)*r*.24f,v=(float)Math.sin(a)*r*.46f;
                flipper[i*2]=x+d*r*.52f+u*.88f+d*v*.47f;
                flipper[i*2+1]=y+r*.68f+v*.88f-d*u*.47f;
            }
            p.fillPoly(flipper,skin);p.strokePoly(flipper,EDGE,r*.045f);
            p.fillEllipse(x+d*r*.49f,y+r*.62f,r*.12f,r*.20f,Glyph.mix(skin,0xFFFFFFFF,.18f));
            for(int i=0;i<3;i++)p.fillEllipse(x+d*r*(.62f+i*.06f),y+r*(.70f+i*.075f),r*.035f,r*.06f,shell);
        }
        // Broad cheek contour grows out of the head, with no neck seam through the face.
        float[] head=new float[96];
        for(int i=0;i<48;i++) {
            double a=i*Softbody.TAU/48;
            float cheek=(float)Math.sin(a)>.3f?1.03f:1;
            head[i*2]=x+(float)Math.cos(a)*r*.91f*cheek;
            head[i*2+1]=y-r*.25f+(float)Math.sin(a)*r*.82f;
        }
        p.fillPoly(head,skin);p.strokePoly(head,EDGE,r*.055f);
        p.arc(x,y-r*.25f,r*.85f,r*.76f,205,105,Glyph.mix(skin,0xFFFFFFFF,.63f),r*.04f);
        p.fillEllipse(x-r*.34f,y-r*.84f,r*.13f,r*.055f,0xBBFFFFFF);
        p.fillEllipse(x-r*.50f,y-r*.70f,r*.055f,r*.033f,0xBBFFFFFF);
        for(int d=-1;d<=1;d+=2) {
            float ex=x+d*r*.43f,ey=y-r*.23f;
            oval(p,ex,ey,r*.235f,r*.305f,0xFFF1EEFF);
            p.fillEllipse(ex+d*r*.018f,ey,r*.183f,r*.265f,0xFF7752B6);
            p.fillEllipse(ex,ey-r*.060f,r*.158f,r*.204f,0xFF15132C);
            p.fillEllipse(ex+d*r*.022f,ey+r*.172f,r*.105f,r*.060f,0xFFC384E9);
            p.fillCircle(ex-r*.062f,ey-r*.12f,r*.068f,0xFFFFFFFF);
            p.fillCircle(ex+r*.048f,ey-r*.012f,r*.025f,0xFFE1DAFF);
            p.fillEllipse(x+d*r*.59f,y+r*.10f,r*.165f,r*.092f,0xFFED9BA9);
            p.fillCircle(x+d*r*.61f,y+r*.08f,r*.028f,0x99FFFFFF);
            p.arc(ex,y-r*.27f,r*.23f,r*.29f,200,135,EDGE,r*.04f);
        }
        p.fillPoly(new float[]{x-r*.18f,y+r*.10f,x,y+r*.15f,x+r*.18f,y+r*.10f,
                x+r*.12f,y+r*.28f,x,y+r*.32f,x-r*.12f,y+r*.28f},EDGE);
        p.fillEllipse(x,y+r*.26f,r*.096f,r*.049f,0xFFF2A7B5);
        if(variant==4)for(int i=0;i<5;i++) {
            float sx=x+(i-2)*r*.13f,sy=y-r*.93f-Math.abs(i-2)*r*.025f;
            p.fillCircle(sx,sy,r*.055f,Glyph.cycle(i*.2f));
        }
    }
}
