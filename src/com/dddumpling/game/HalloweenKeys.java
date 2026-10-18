package com.dddumpling.game;

/** Approved soft silhouettes with simple geometric costume details and shared expressions. */
final class HalloweenKeys {
    private static final int INK=0xFF392C4F;
    private static final float[][] FORMS={
        {-.85f,.48f,-.79f,-.22f,-.51f,-.73f,0,-.87f,.53f,-.67f,.80f,-.13f,.80f,.55f,.53f,.76f,.30f,.58f,.04f,.82f,-.22f,.62f,-.55f,.80f},
        {-.72f,-.26f,-.61f,-.84f,-.25f,-.61f,.24f,-.62f,.61f,-.85f,.73f,-.27f,.71f,.41f,.35f,.72f,-.34f,.72f,-.73f,.37f},
        {-.85f,-.31f,-.47f,-.74f,0,-.72f,.49f,-.70f,.88f,-.24f,.86f,.38f,.45f,.77f,0,.80f,-.48f,.74f,-.88f,.27f},
        {-.70f,-.27f,-.67f,-.32f,.67f,-.32f,.70f,-.27f,.62f,.17f,.29f,.63f,0,.76f,-.29f,.63f,-.62f,.17f},
        {-.79f,-.47f,-.58f,-.79f,.55f,-.78f,.79f,-.48f,.82f,.32f,.50f,.74f,-.50f,.75f,-.81f,.33f},
        {-.72f,-.26f,-.61f,-.84f,-.25f,-.61f,.24f,-.62f,.61f,-.85f,.73f,-.27f,.71f,.41f,.35f,.72f,-.34f,.72f,-.73f,.37f}
    };
    // Catmull-Rom contours keep the shape coherent instead of overlapping primitive lobes.
    static float[] shape(float x,float y,float rx,float ry,float[] nodes) {
        int n=nodes.length/2,steps=5;float[] out=new float[n*steps*2];
        for(int i=0;i<n;i++)for(int j=0;j<steps;j++) {
            float t=j/(float)steps,t2=t*t,t3=t2*t;
            for(int a=0;a<2;a++) {
                float p0=nodes[((i+n-1)%n)*2+a],p1=nodes[i*2+a],p2=nodes[((i+1)%n)*2+a],p3=nodes[((i+2)%n)*2+a];
                float v=.5f*((2*p1)+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t2+(-p0+3*p1-3*p2+p3)*t3);
                out[(i*steps+j)*2+a]=(a==0?x:y)+v*(a==0?rx:ry);
            }
        }
        return out;
    }
    private static float[] points(float x,float y,float rx,float ry,float... coords) {
        float[] out=coords.clone();
        for(int i=0;i<out.length;i+=2){out[i]=x+out[i]*rx;out[i+1]=y+out[i+1]*ry;}
        return out;
    }
    private static float[] body(int g,float x,float y,float rx,float ry) {
        if(g==Kawaii.SQUISHY)return points(x,y,rx,ry,
                -.52f,-.73f,.52f,-.73f,.76f,-.48f,.76f,.48f,.52f,.71f,-.52f,.71f,-.76f,.48f,-.76f,-.48f);
        if(g==Kawaii.BLOB) {
            // Sharp triangular ears join straight temples and one soft semicircular jaw.
            float[] outline=new float[(5+17)*2];
            float[] top={-.74f,-.78f,-.26f,-.47f,.26f,-.47f,.74f,-.78f,.74f,.18f};
            System.arraycopy(top,0,outline,0,top.length);
            for(int i=0;i<=16;i++) {
                float a=i*(float)Math.PI/16;
                outline[10+i*2]=.74f*(float)Math.cos(a);
                outline[11+i*2]=.18f+.52f*(float)Math.sin(a);
            }
            return points(x,y,rx,ry,outline);
        }
        return shape(x,y,rx,ry,FORMS[g]);
    }
    static void draw(Painter p,int g,float x,float y,float r,int incoming,float squash,float happy,int mood,float clock) {
        if(g<0 || g>=Glyph.COUNT)return;
        float rx=r*squash,ry=r/squash;
        // Center the whole silhouette: crown at -1.30, chin at +.76.
        if(g==Kawaii.GRAPES)y+=ry*.27f;
        int base=Skins.HALLOWEEN_COLORS[g],alpha=incoming>>>24;
        if((incoming&0xFFFFFF)!=(Glyph.COLOR[g]&0xFFFFFF) && (incoming&0xFFFFFF)!=(base&0xFFFFFF))base=Glyph.mix(base,incoming,.22f);
        Painter q=new OpacityPainter(p,alpha/255f);
        if(g==Kawaii.STRAWBERRY)for(int side=-1;side<=1;side+=2) {
            float flutter=.05f*(float)Math.sin(clock*7+happy*3);
            float[] wing=shape(x,y,rx*side,ry,new float[]{.45f,-.15f,1.08f,-.51f,1.30f,-.36f,1.19f,.06f,.97f,-.01f,.89f,.29f,.64f,.17f,.46f,.34f});
            for(int i=1;i<wing.length;i+=2)wing[i]+=ry*flutter;
            q.fillPoly(wing,Glyph.mix(base,0xFF792B55,.30f));
        }
        if(g==Kawaii.SQUISHY)for(int side=-1;side<=1;side+=2) {
            q.line(x+rx*side*.67f,y+ry*.19f,x+rx*side*.95f,y+ry*.19f,0xFFB4A7C9,r*.15f);
            q.line(x+rx*side*.95f,y+ry*.08f,x+rx*side*.95f,y+ry*.30f,0xFFECDFEE,r*.11f);
        }
        float[] body=body(g,x,y,rx,ry);
        q.fillPoly(body,base);
        q.fillPoly(body(g,x-rx*.055f,y-ry*.10f,rx*.86f,ry*.79f),Glyph.mix(base,0xFFFFFFFF,g==Kawaii.BLOB?.20f:.13f));
        if(g!=Kawaii.GRAPES)q.fillEllipse(x-rx*.36f,y-ry*.43f,rx*.15f,ry*.075f,0xA6FFFFFF);
        if(g==Kawaii.CAT) {
            q.fillPoly(points(x,y,rx,ry,-.08f,-.68f,-.13f,-.98f,.09f,-1.03f,.05f,-.68f),0xFF68945A);
        }
        if(g==Kawaii.BLOB) {
            for(int side=-1;side<=1;side+=2) {
                q.fillPoly(points(x,y,rx*side,ry,.39f,-.43f,.62f,-.59f,.62f,-.31f),0xFFD79FBE);
                q.line(x+side*rx*.53f,y+ry*.23f,x+side*rx*.89f,y+ry*.27f,0xFFE4D6EA,r*.055f);
            }
        }
        if(g==Kawaii.GRAPES) {
            q.fillPoly(points(x,y,rx,ry,-.94f,-.24f,-.42f,-.61f,-.13f,-1.30f,.35f,-1.19f,.13f,-1.03f,.43f,-.61f,.94f,-.24f),0xFF5B3D7E);
            q.fillPoly(points(x,y,rx,ry,-.36f,-.70f,.35f,-.70f,.43f,-.57f,-.42f,-.57f),0xFFF2AE67);
        }
        if(g==Kawaii.SQUISHY) {
            q.fillPoly(points(x,y,rx,ry,-.76f,-.19f,-.76f,-.48f,-.52f,-.73f,.52f,-.73f,.76f,-.48f,.76f,-.19f,.57f,-.27f,.35f,-.35f,.16f,-.20f,-.02f,-.35f,-.28f,-.22f,-.45f,-.35f),0xFF454554);
        }
        face(q,g,x,y,rx,ry,r,happy,mood,clock);
    }
    private static void face(Painter p,int g,float x,float y,float rx,float ry,float r,float happy,int mood,float clock) {
        int ink=g==Kawaii.BLOB?0xFFFFE9AF:INK;
        int eyes=g==Kawaii.BLOB?0xFFCCEFA3:ink;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*rx*.29f,ey=y+ry*.035f;
            if(mood==2 || happy>.7f)p.arc(ex,ey+ry*.07f,rx*.15f,ry*.14f,200,140,eyes,r*.07f);
            else if(g==Kawaii.CAT && mood==0) {
                p.fillPoly(shape(ex,ey,rx*.19f,ry*.21f,new float[]{-1,.6f,-.2f,-1,1,.6f}),ink);
                p.fillCircle(ex,ey-ry*.025f,r*.025f,0xFFFFE4A3);
            } else {p.fillEllipse(ex,ey,rx*.145f,ry*(mood==1?.22f:.185f),eyes);p.fillCircle(ex-rx*.025f,ey-ry*.055f,r*.045f,g==Kawaii.BLOB?INK:0xFFFFFFFF);}
            if(mood==2)p.line(ex-side*rx*.14f,ey-ry*.20f,ex+side*rx*.07f,ey-ry*.12f,ink,r*.045f);
            p.fillEllipse(x+side*rx*(g==Kawaii.GRAPES?.41f:.48f),y+ry*.26f,rx*(g==Kawaii.GRAPES?.10f:.13f),ry*.08f,0x99F278A2);
        }
        if(g==Kawaii.CAT && mood==0) {
            p.fillPoly(shape(x,y+ry*.24f,rx*.055f,ry*.065f,new float[]{0,-1,1,1,-1,1}),ink);
            p.fillPoly(shape(x,y+ry*.40f,rx*.25f,ry*.12f,new float[]{-1,-.4f,-.3f,.1f,.1f,-.05f,1,-.4f,.55f,.9f,-.35f,1}),ink);
            p.fillRect(x-rx*.06f,y+ry*.38f,x+rx*.035f,y+ry*.45f,0xFFFFC772);
        } else if(mood==1)p.fillEllipse(x,y+ry*.35f,rx*.08f,ry*.12f,ink);
        else if(g!=Kawaii.BLOB || mood==3)p.arc(x,y+ry*(mood==3?.46f:.30f),rx*.15f,ry*.12f,mood==3?205:20,140,ink,r*.045f);
        if(g==Kawaii.STRAWBERRY && mood!=3)for(int side=-1;side<=1;side+=2)p.fillPoly(new float[]{x+side*rx*.1f,y+ry*.30f,x+side*rx*.04f,y+ry*.44f,x+side*rx*.16f,y+ry*.34f},0xFFF6EDF9);
        if(g==Kawaii.BLOB && mood!=1 && mood!=3) {
            p.fillPoly(shape(x,y+ry*.24f,rx*.09f,ry*.06f,new float[]{-1,-.5f,1,-.5f,0,1}),0xFFF4B1CB);
            p.arc(x-rx*.09f,y+ry*.31f,rx*.10f,ry*.08f,0,155,ink,r*.03f);
            p.arc(x+rx*.09f,y+ry*.31f,rx*.10f,ry*.08f,25,155,ink,r*.03f);
        }
        if(mood==3)Kawaii.cryTears(p,x,y,rx*.29f,rx,ry,clock,1);
    }
}
