package com.dddumpling.game;

/** Soft burrow friends and coiled concert companions, including their unknown silhouettes. */
final class CaveCollect extends Draw {
    private static final int FACE=0xFF49374F, PINK=0xFFF391AF;

    static void body(Painter p,int shape,float x,float y,float r,int fill,int trim,boolean known) {
        if(shape==Collect.MOLE) {
            for(int side=-1;side<=1;side+=2) {
                p.fillEllipse(x+side*r*.72f,y+r*.26f,r*.28f,r*.22f,fill);
                p.fillEllipse(x+side*r*.37f,y+r*.73f,r*.27f,r*.16f,fill);
                p.fillCircle(x+side*r*.47f,y-r*.51f,r*.21f,fill);
            }
            p.fillEllipse(x,y+r*.05f,r*.73f,r*.83f,fill);
            if(known) {
                p.fillEllipse(x,y+r*.29f,r*.48f,r*.43f,trim);
                p.fillEllipse(x,y-r*.06f,r*.42f,r*.27f,trim);
                for(int side=-1;side<=1;side+=2)for(int n=0;n<3;n++)
                    p.fillEllipse(x+side*r*(.69f+n*.08f),y+r*.39f,r*.045f,r*.09f,trim);
            }
        } else {
            // The tail peeks around the coil; the raised head makes the silhouette unmistakable.
            p.fillPoly(new float[]{x+r*.42f,y+r*.57f,x+r*.97f,y+r*.16f,
                    x+r*.88f,y+r*.69f,x+r*.54f,y+r*.78f},fill);
            p.fillEllipse(x-r*.09f,y+r*.55f,r*.79f,r*.34f,fill);
            if(known)p.fillEllipse(x-r*.1f,y+r*.60f,r*.55f,r*.12f,trim);
            p.fillEllipse(x-r*.19f,y+r*.25f,r*.51f,r*.35f,fill);
            p.fillEllipse(x+r*.22f,y-r*.03f,r*.28f,r*.63f,fill);
            p.fillEllipse(x+r*.15f,y-r*.48f,r*.55f,r*.38f,fill);
            if(known)p.fillEllipse(x+r*.18f,y-r*.28f,r*.39f,r*.15f,trim);
        }
    }

    static void draw(Painter p,int i,float x,float y,float r,float clock,boolean known,float fade) {
        draw(p,i,x,y,r,clock,known,fade,-1,0f);
    }
    static void draw(Painter p,int i,float x,float y,float r,float clock,boolean known,float fade,int mood,float look) {
        boolean mole=Collect.FAMILY[i]==Collect.MOLES;
        int variant=i-(mole?Collect.MOLE_FIRST:Collect.SNAKE_FIRST);
        int fill=fadeBy(known?Collect.BODY[i]:0xFF302748,fade);
        int trim=fadeBy(known?Collect.ACCENT[i]:0xFF302748,fade);
        body(p,Collect.SHAPE[i],x,y,r*1.055f,fadeBy(known?FACE:0xFF66577D,fade),trim,false);
        body(p,Collect.SHAPE[i],x,y,r,fill,trim,known);
        if(!known) {
            p.text("?",x,y+r*.22f,type(r*.6f),fadeBy(INK_DIM,fade),Painter.CENTER,true);return;
        }
        int ink=fadeBy(FACE,fade),pink=fadeBy(PINK,fade),white=fadeBy(0xFFFFF6E9,fade);
        float fx=x+(mole?0:r*.15f),fy=y-r*(mole?.19f:.48f);
        float eyes=mole?.23f:.24f;
        boolean sleepy=mole && variant==2;
        if(mood>=0) Trinket.reactionFace(p,fx,fy,r*.62f,clock,fade,mood,look);
        else for(int side=-1;side<=1;side+=2) {
            float ex=fx+side*r*eyes;
            if(sleepy || Math.sin(clock*1.2+i)>.996)
                p.polyline(new float[]{ex-r*.075f,fy,ex,fy+r*.035f,ex+r*.075f,fy},ink,r*.035f);
            else {
                p.fillEllipse(ex,fy,r*.058f,r*.085f,ink);
                p.fillCircle(ex-r*.018f,fy-r*.025f,r*.022f,white);
            }
            p.fillEllipse(fx+side*r*(eyes+.13f),fy+r*.12f,r*.11f,r*.06f,pink);
        }
        if(mole) {
            if(variant==3)p.fillPoly(star(fx,fy+r*.14f,r*.13f,r*.065f,6,0),pink);
            else p.fillEllipse(fx,fy+r*.13f,r*.115f,r*.075f,pink);
            if(mood<0) p.polyline(new float[]{fx-r*.09f,fy+r*.27f,fx,fy+r*.31f,fx+r*.09f,fy+r*.27f},ink,r*.035f);
            if(variant==0 || variant==4) {
                int hat=fadeBy(variant==0?0xFF84C6A8:0xFFF6DA80,fade);
                p.fillEllipse(x,y-r*.68f,r*.44f,r*.14f,hat);
                p.fillCircle(x,y-r*.71f,r*.16f,ink);
                p.fillCircle(x,y-r*.72f,r*.12f,white);
            } else if(variant==1) {
                for(int k=0;k<5;k++)p.fillCircle(x-r*.42f+(float)Math.cos(k*1.256f)*r*.12f,
                        y-r*.62f+(float)Math.sin(k*1.256f)*r*.12f,r*.09f,pink);
                p.fillCircle(x-r*.42f,y-r*.62f,r*.07f,white);
            } else if(variant==2) {
                p.fillPoly(new float[]{x-r*.37f,y-r*.62f,x+r*.30f,y-r*.64f,x+r*.39f,y-r*.91f},trim);
                p.fillCircle(x+r*.39f,y-r*.91f,r*.085f,white);
            }
        } else {
            if(mood<0) {
                p.polyline(new float[]{fx-r*.09f,fy+r*.13f,fx,fy+r*.18f,fx+r*.09f,fy+r*.13f},ink,r*.032f);
                p.line(fx,fy+r*.18f,fx,fy+r*.29f,pink,r*.04f);
                p.polyline(new float[]{fx-r*.05f,fy+r*.33f,fx,fy+r*.27f,fx+r*.05f,fy+r*.33f},pink,r*.03f);
            }
            for(int k=0;k<3;k++) {
                float sx=x-r*(.57f-k*.23f),sy=y+r*.43f;
                if(variant==3)p.fillPoly(star(sx,sy,r*.075f,r*.035f,4,clock*.1f),white);
                else p.fillEllipse(sx,sy,r*.065f,r*.085f,trim);
            }
            if(variant==1) {
                p.fillPoly(new float[]{fx-r*.31f,fy-r*.39f,fx-r*.09f,fy-r*.31f,fx-r*.31f,fy-r*.22f},pink);
                p.fillPoly(new float[]{fx+r*.13f,fy-r*.39f,fx-r*.09f,fy-r*.31f,fx+r*.13f,fy-r*.22f},pink);
            } else if(variant==4) {
                p.fillPoly(new float[]{fx-r*.23f,fy-r*.32f,fx-r*.26f,fy-r*.53f,fx-r*.08f,fy-r*.43f,
                        fx,fy-r*.61f,fx+r*.1f,fy-r*.43f,fx+r*.26f,fy-r*.53f,fx+r*.23f,fy-r*.32f},trim);
            }
        }
    }
}
