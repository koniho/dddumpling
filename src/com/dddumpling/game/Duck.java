package com.dddumpling.game;

/** Original rubber-duck silhouettes; shared by the case, rainbow rider and companion. */
final class Duck extends Draw {
    private static final int EDGE=0xFF3A2E4F;
    private static final int[] RAINBOW={0xFFFF86AA,0xFFFFB454,0xFFFFE46B,0xFF88DEA6,0xFF83DDEC,0xFFBA9DE9};
    private Duck() {}
    private static final int BILL=2, FOOT=3, SILHOUETTE=4;
    private static final float[] CIRCLE=circle();
    // One traced contour: cheeks, drooping wings, torso and the lifted belly arc.
    private static final float[] PROFILE=trace(new float[]{
        .77f,-.28f,
        .79f,-.08f, .72f,.04f, .62f,.14f,
        .86f,.16f, .83f,.32f, .94f,.49f,
        .99f,.58f, 1.075f,.63f, 1.02f,.68f,
        .91f,.72f, .70f,.64f, .59f,.54f,
        .63f,.72f, .61f,.91f, .47f,1.03f,
        .17f,.945f, -.17f,.945f, -.47f,1.03f,
        -.61f,.91f, -.63f,.72f, -.59f,.54f,
        -.70f,.64f, -.91f,.72f, -1.02f,.68f,
        -1.075f,.63f, -.99f,.58f, -.94f,.49f,
        -.83f,.32f, -.86f,.16f, -.62f,.14f,
        -.72f,.04f, -.79f,-.08f, -.77f,-.28f,
        -.77f,-.75f, -.43f,-1f, 0f,-1f,
        .43f,-1f, .77f,-.75f, .77f,-.28f});
    private static final float[] CHEEK={.715f,.045f, .66f,.17f, .55f,.235f, .43f,.275f};
    private static final float[] WING_FOLD={.52f,.355f, .565f,.475f, .64f,.565f, .735f,.61f};
    private static float[] trace(float[] curves) {
        float[] out=new float[192];
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
    private static float smooth(float value) {
        value=Math.max(0,Math.min(1,value));return value*value*(3-2*value);
    }
    private static float displacement(float[] ring,float u,float v,int axis) {
        float angle=(float)Math.atan2(v,u);
        if(angle<0)angle+=Softbody.TAU;
        float at=angle/Softbody.TAU*(ring.length/2);
        int i=((int)at*2)%ring.length,j=(i+2)%ring.length;
        float t=at-(int)at;
        return (ring[i+axis]-CIRCLE[i+axis])*(1-t)+(ring[j+axis]-CIRCLE[j+axis])*t;
    }
    private static float[] deform(float[] points,DuckBodies bodies,int duck) {
        float[] out=points.clone();
        if(bodies==null)return out;
        float[] body=bodies.skin[duck][0].outline(),head=bodies.skin[duck][1].outline(),wing=bodies.skin[duck][2].outline();
        for(int i=0;i<out.length;i+=2) {
            float x=points[i],y=points[i+1],side=x<0?-1:1;
            float w=smooth((Math.abs(x)-.55f)/.28f)*smooth((y-.04f)/.18f);
            float h=(1-smooth((y-.10f)/.35f))*(1-w),b=1-h-w;
            if(h>0) {
                out[i]+=displacement(head,x/.77f,(y+.28f)/.72f,0)*.77f*h;
                out[i+1]+=displacement(head,x/.77f,(y+.28f)/.72f,1)*.72f*h;
            }
            if(b>0) {
                out[i]+=displacement(body,x/.57f,(y-.52f)/.51f,0)*.57f*b;
                out[i+1]+=displacement(body,x/.57f,(y-.52f)/.51f,1)*.51f*b;
            }
            if(w>0) {
                out[i]+=displacement(wing,(Math.abs(x)-.77f)/.25f,(y-.43f)/.30f,0)*.25f*side*w;
                out[i+1]+=displacement(wing,(Math.abs(x)-.77f)/.25f,(y-.43f)/.30f,1)*.30f*w;
            }
        }
        return out;
    }
    private static float[] sculpt(float[] ring,int form) {
        float[] out=ring.clone();
        for(int i=0;i<out.length;i+=2) {
            float u=ring[i],v=ring[i+1];
            if(form==BILL && v<0)out[i+1]*=.68f+.32f*(float)Math.cos(u*Math.PI);
            else if(form==FOOT && v<-.3f)out[i+1]+=.13f*(float)Math.cos(u*Math.PI*3);
        }
        return out;
    }
    private static void fold(Painter p,int duck,float x,float y,float r,int side,float[] curve,int color) {
        float[] points=new float[34];
        for(int i=0;i<17;i++) {
            float t=i/16f,u=1-t;
            for(int axis=0;axis<2;axis++)points[i*2+axis]=
                    (u*u*u*curve[axis]+3*u*u*t*curve[axis+2]+3*u*t*t*curve[axis+4]+t*t*t*curve[axis+6])*(axis==0?side:1);
        }
        points=deform(points,p.ducks(),duck);
        for(int i=0;i<points.length;i+=2) {points[i]=x+points[i]*r;points[i+1]=y+points[i+1]*r;}
        p.polyline(points,Glyph.withAlpha(Glyph.mix(color,EDGE,.72f),175),r*.034f);
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
        float[] ring=bodies==null?CIRCLE:bodies.skin[duck][layer].outline();
        float[] outline=form==SILHOUETTE?deform(PROFILE,bodies,duck):sculpt(ring,form);
        // The sculpted silhouette and every shading layer follow the same spring ring.
        float[] points=new float[outline.length];
        float cs=(float)Math.cos(tilt),sn=(float)Math.sin(tilt);
        for(int pass=0;pass<5;pass++) {
            if(form==SILHOUETTE && pass==4) {
                float[] gleam=deform(new float[]{-.24f,-.62f},bodies,duck);
                p.fillEllipse(x+rx*gleam[0],y+ry*gleam[1],rx*.20f,ry*.15f,
                        Glyph.withAlpha(Glyph.mix(color,0xFFFFFFFF,.70f),105));
                continue;
            }
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
        int bill=known?(kind==3?0xFFFFB591:kind==5?0xFFFFCB78:0xFFF79867):accent;
        skin(p,kind,0,x,y,r,r,body,r*.045f,SILHOUETTE,0);
        for(int side=-1;side<=1;side+=2) {
            fold(p,kind,x,y,r,side,CHEEK,body);
            fold(p,kind,x,y,r,side,WING_FOLD,body);
            if(known && kind>=9)for(int i=0;i<6;i++)
                p.line(x+side*r*.70f,y+r*(.29f+i*.04f),
                        x+side*r*.83f,y+r*(.34f+i*.04f),RAINBOW[i],r*.035f);
        }
        p.fillEllipse(x-r*.08f,y+r*.52f,r*.36f,r*.29f,Glyph.withAlpha(Glyph.mix(body,accent,.28f),85));
        for(int side=-1;side<=1;side+=2)
            skin(p,kind,0,x+side*r*.48f,y+r*.87f,r*.165f,r*.20f,bill,r*.034f,FOOT,side*.19f);
        float hx=x,hy=y-r*.28f;
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
