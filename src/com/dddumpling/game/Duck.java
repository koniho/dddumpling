package com.dddumpling.game;

/** Original rubber-duck silhouettes; shared by the case, rainbow rider and companion. */
final class Duck extends Draw {
    private static final int EDGE=0xFF3A2E4F;
    private static final int[] RAINBOW={0xFFFF86AA,0xFFFFB454,0xFFFFE46B,0xFF88DEA6,0xFF83DDEC,0xFFBA9DE9};
    private Duck() {}
    private static final int ROUND=0, WING=1, BILL=2, FOOT=3, BODY=4;
    private static final float[] CIRCLE=circle();
    // Rounded shoulder, outward sweep, soft tip and concave inner return from the reference.
    private static final float[] WING_PROFILE=trace(new float[]{
        1f,.73f,
        1.10f,.91f, .65f,1.03f, .17f,.88f,
        -.45f,.71f, -.83f,.08f, -.92f,-.31f,
        -1.09f,-.74f, -.85f,-1.09f, -.49f,-.95f,
        .32f,-.82f, .27f,-.15f, .65f,.36f,
        .80f,.56f, 1f,.64f, 1f,.73f});
    // The belly rises gently between the feet, rather than ending in an oval's low point.
    private static final float[] BODY_PROFILE=trace(new float[]{
        1f,0f,
        1.08f,.47f, 1.02f,.84f, .83f,1f,
        .30f,.84f, -.30f,.84f, -.83f,1f,
        -1.02f,.84f, -1.08f,.47f, -1f,0f,
        -.98f,-1.25f, .98f,-1.25f, 1f,0f});
    private static float[] trace(float[] curves) {
        float[] out=new float[CIRCLE.length];
        int segments=(curves.length-2)/6;
        for(int i=0;i<out.length;i+=2) {
            float position=(float)i/out.length*segments;
            int segment=(int)position,base=segment*6;
            float t=position-segment,u=1-t;
            for(int axis=0;axis<2;axis++)out[i+axis]=u*u*u*curves[base+axis]
                    +3*u*u*t*curves[base+2+axis]+3*u*t*t*curves[base+4+axis]
                    +t*t*t*curves[base+6+axis];
        }
        return out;
    }
    private static float[] sculpt(float[] ring,int form) {
        if(form==ROUND)return ring;
        float[] out=ring.clone();
        float[] profile=form==WING?WING_PROFILE:form==BODY?BODY_PROFILE:null;
        for(int i=0;i<out.length;i+=2) {
            float u=ring[i],v=ring[i+1];
            if(profile!=null) {
                // Carry spring displacement onto the traced contour, including all depth layers.
                out[i]=profile[i]+u-CIRCLE[i];
                out[i+1]=profile[i+1]+v-CIRCLE[i+1];
            } else if(form==BILL && v<0)out[i+1]*=.68f+.32f*(float)Math.cos(u*Math.PI);
            else if(form==FOOT && v<-.3f)out[i+1]+=.13f*(float)Math.cos(u*Math.PI*3);
        }
        return out;
    }
    private static float[] circle() {
        float[] out=new float[96];
        for(int i=0;i<48;i++) {
            out[i*2]=(float)Math.cos(i*Softbody.TAU/48);
            out[i*2+1]=(float)Math.sin(i*Softbody.TAU/48);
        }
        return out;
    }
    private static void skin(Painter p,int duck,int layer,float x,float y,float rx,float ry,
            int color,float edge,int form,float tilt) {
        DuckBodies bodies=p.ducks();
        float[] outline=sculpt(bodies==null?CIRCLE:bodies.skin[duck][layer].outline(),form);
        // The sculpted silhouette and every shading layer follow the same spring ring.
        float[] points=new float[outline.length];
        float cs=(float)Math.cos(tilt),sn=(float)Math.sin(tilt);
        for(int pass=0;pass<5;pass++) {
            float scale=pass<=1?1f:pass==2?.92f:pass==3?.76f:.32f;
            float shiftX=pass<2?0:pass==2?-.02f:pass==3?-.10f:-.29f;
            float shiftY=pass<2?0:pass==2?-.04f:pass==3?-.16f:-.47f;
            for(int i=0;i<outline.length;i+=2) {
                float u=outline[i],v=outline[i+1];
                float dx=rx*(u*scale+shiftX),dy=ry*(v*scale+shiftY);
                points[i]=x+dx*cs-dy*sn;
                points[i+1]=y+dx*sn+dy*cs;
            }
            if(pass==0) {
                p.strokePoly(points,Glyph.withAlpha(Glyph.mix(color,EDGE,.55f),185),edge*2);
            } else {
                int tint=pass==1?Glyph.mix(color,EDGE,.07f):pass==2?color:
                        Glyph.mix(color,0xFFFFFFFF,pass==3?.22f:.70f);
                p.fillPoly(points,Glyph.withAlpha(tint,pass==1?155:pass==2?92:pass==3?62:105));
            }
        }
        // Light catches both the rear lower rim and the thin front membrane.
        for(int band=0;band<2;band++) {
            int first=band==0?2:outline.length/2+4;
            int samples=outline.length/4-6;
            float[] rim=new float[samples*2];
            for(int j=0;j<samples;j++) {
                int i=(first+j*2)%outline.length;
                float u=outline[i],v=outline[i+1];
                float dx=rx*u*.90f,dy=ry*v*.90f;
                rim[j*2]=x+dx*cs-dy*sn;rim[j*2+1]=y+dx*sn+dy*cs;
            }
            p.polyline(rim,Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.70f),band==0?105:155),edge*.65f);
        }
    }
    static void draw(Painter out,int who,float x,float y,float r,float clock,boolean known,
            float fade,int mood,float look,boolean ninja) {
        Painter p=new OpacityPainter(out,fade);
        int kind=who-Collect.DUCK_FIRST;
        int body=known?Collect.BODY[who]:0xFF393056,accent=known?Collect.ACCENT[who]:0xFF55476F;
        float bob=(float)Math.sin(clock*3+kind)*r*.035f;
        y+=bob;
        float wing=(float)Math.sin(clock*(mood==RunCompanion.VICTORY?15:4)+kind)*r*.055f;
        int bill=known?(kind==3?0xFFFFB591:kind==5?0xFFFFCB78:0xFFF79867):accent;
        // Seated, front-facing silhouette: small pear body under an oversized round head.
        skin(p,kind,0,x,y+r*.52f,r*.57f,r*.51f,body,r*.045f,BODY,0);
        for(int side=-1;side<=1;side+=2) {
            float wx=x+side*r*.77f,wy=y+r*.43f+wing;
            skin(p,kind,2,wx,wy,side*r*.25f,r*.30f,body,r*.040f,WING,0);
            if(known && kind>=9)for(int i=0;i<6;i++)
                p.line(wx-side*r*.07f,wy-r*(.17f-i*.051f),
                        wx+side*r*.065f,wy-r*(.12f-i*.051f),RAINBOW[i],r*.035f);
        }
        p.fillEllipse(x-r*.08f,y+r*.52f,r*.36f,r*.29f,Glyph.withAlpha(Glyph.mix(body,accent,.28f),85));
        for(int side=-1;side<=1;side+=2)
            skin(p,kind,0,x+side*r*.38f,y+r*.85f,r*.165f,r*.20f,bill,r*.034f,FOOT,side*.19f);
        float hx=x,hy=y-r*.28f;
        p.fillEllipse(hx,hy+r*.62f,r*.47f,r*.11f,0x22000000);
        skin(p,kind,1,hx,hy,r*.77f,r*.72f,body,r*.05f,ROUND,0);
        p.arc(hx-r*.13f,hy-r*.11f,r*.47f,r*.40f,210,49,known?0x66FFFFFF:accent,r*.06f);
        if(known)for(int i=0;i<3;i++) {
            float bx=hx+r*(-.33f+i*.24f),by=hy-r*(.41f+(i%2)*.10f);
            float br=r*(.045f+(i%2)*.025f);
            p.fillCircle(bx,by,br,0x22FFFFFF);
            p.arc(bx,by,br,br,205,135,0x77FFFFFF,r*.012f);
        }
        if(!known) {
            p.text("?",hx,hy+r*.18f,type(r*.62f),INK,Painter.CENTER,true);return;
        }
        if(kind==1)for(int i=0;i<3;i++) {
            float bx=hx+(i-1)*r*.17f,by=hy-r*(.71f+(i%2)*.09f);
            p.fillCircle(bx,by,r*.12f,accent);p.strokeCircle(bx,by,r*.12f,0xAAFFFFFF,r*.025f);
        }
        if(kind==2 || kind==4) {
            p.fillPoly(new float[]{hx-r*.05f,hy-r*.64f,hx-r*.24f,hy-r*.91f,hx+r*.04f,hy-r*.79f,
                    hx+r*.25f,hy-r*.90f,hx+r*.16f,hy-r*.63f},0xFF5CB77A);
        } else if(kind==6) {
            for(int i=0;i<3;i++)p.fillCircle(hx+(i-1)*r*.16f,hy-r*.67f-(i%2)*r*.09f,r*.15f,body);
        } else if(kind==8) {
            p.fillPoly(star(hx,hy-r*.78f,r*.20f,r*.105f,5,-1.57f),accent);
        } else if(kind==10) {
            p.fillPoly(new float[]{hx-r*.36f,hy-r*.63f,hx-r*.40f,hy-r*1.00f,hx-r*.14f,hy-r*.83f,
                    hx,hy-r*1.12f,hx+r*.15f,hy-r*.83f,hx+r*.39f,hy-r*1.00f,hx+r*.34f,hy-r*.63f},0xFFFFC453);
            for(int i=0;i<3;i++)p.fillCircle(hx+(i-1)*r*.22f,hy-r*.75f,r*.055f,RAINBOW[i*2]);
        } else {
            p.arc(hx,hy-r*.70f,r*.15f,r*.15f,170,235,accent,r*.085f);
        }
        if(kind==2 || kind==8)for(int i=0;i<4;i++) {
            float px=x-r*.23f+i*r*.15f,py=y+r*(.58f+(i%2)*.09f);
            if(kind==8)p.fillPoly(star(px,py,r*.064f,r*.028f,5,0),accent);
            else p.fillEllipse(px,py,r*.024f,r*.039f,0xFFFFE9A4);
        }
        if(kind==5)p.arc(x,y+r*.56f,r*.115f,r*.10f,40,230,accent,r*.045f);
        if(kind==7)for(int i=0;i<3;i++)p.arc(x,y+r*.67f,r*(.12f+i*.06f),r*(.09f+i*.04f),185,170,accent,r*.037f);
        if(kind>=9)for(int i=0;i<6;i++)p.arc(x,y+r*.61f,r*(.25f-i*.033f),r*(.17f-i*.022f),180,180,RAINBOW[i],r*.035f);
        boolean blink=Math.sin(clock*1.7f+kind)>.985f;
        for(int side=-1;side<=1;side+=2) {
            float ex=hx+side*r*.34f+look*r*.10f,ey=hy-r*.01f;
            if(blink || mood==RunCompanion.VICTORY)p.arc(ex,ey,r*.12f,r*.09f,195,150,EDGE,r*.048f);
            else {
                p.fillEllipse(ex,ey,r*.125f,r*.137f,EDGE);
                p.fillCircle(ex-r*.030f,ey-r*.045f,r*.038f,0xFFFFFFFF);
            }
            p.fillEllipse(hx+side*r*.49f,hy+r*.21f,r*.11f,r*.058f,0x88F57D9D);
        }
        skin(p,kind,3,hx,hy+r*.25f,r*.30f,r*.17f,bill,r*.032f,BILL,0);
        for(int side=-1;side<=1;side+=2)
            p.fillEllipse(hx+side*r*.058f,hy+r*.18f,r*.019f,r*.026f,EDGE);
        if(ninja)Trinket.ninjaMask(p,hx,hy,r*.73f,1);
    }
}
