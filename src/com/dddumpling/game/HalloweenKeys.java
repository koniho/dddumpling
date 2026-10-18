package com.dddumpling.game;

/** One softly shaded, layered silhouette per seasonal key; all poses share the same face. */
final class HalloweenKeys {
    private static final int INK=0xFF392C4F;
    private static final float[][] FORMS={
        {-.85f,.48f,-.79f,-.22f,-.51f,-.73f,0,-.87f,.53f,-.67f,.80f,-.13f,.80f,.55f,.53f,.76f,.30f,.58f,.04f,.82f,-.22f,.62f,-.55f,.80f},
        {-.72f,-.26f,-.61f,-.84f,-.25f,-.61f,.24f,-.62f,.61f,-.85f,.73f,-.27f,.71f,.41f,.35f,.72f,-.34f,.72f,-.73f,.37f},
        {-.85f,-.31f,-.47f,-.74f,0,-.72f,.49f,-.70f,.88f,-.24f,.86f,.38f,.45f,.77f,0,.80f,-.48f,.74f,-.88f,.27f},
        {-.75f,-.35f,-.40f,-.66f,.12f,-.67f,.65f,-.38f,.79f,.14f,.58f,.66f,0,.82f,-.57f,.64f,-.79f,.13f},
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
    static void draw(Painter p,int g,float x,float y,float r,int incoming,float squash,float happy,int mood,float clock) {
        if(g<0 || g>=Glyph.COUNT)return;
        float rx=r*squash,ry=r/squash;
        int base=Skins.HALLOWEEN_COLORS[g],alpha=incoming>>>24;
        if((incoming&0xFFFFFF)!=(Glyph.COLOR[g]&0xFFFFFF) && (incoming&0xFFFFFF)!=(base&0xFFFFFF))base=Glyph.mix(base,incoming,.22f);
        Painter q=new OpacityPainter(p,alpha/255f);
        if(g==Kawaii.STRAWBERRY)for(int side=-1;side<=1;side+=2) {
            float flutter=.05f*(float)Math.sin(clock*7+happy*3);
            float[] wing=shape(x,y,rx*side,ry,new float[]{.45f,-.15f,1.08f,-.51f,1.30f,-.36f,1.19f,.06f,.97f,-.01f,.89f,.29f,.64f,.17f,.46f,.34f});
            for(int i=1;i<wing.length;i+=2)wing[i]+=ry*flutter;
            q.fillPoly(wing,Glyph.mix(base,0xFF792B55,.30f));
            q.polyline(new float[]{x+rx*side*.5f,y,x+rx*side*.96f,y-ry*.2f,x+rx*side*1.2f,y-ry*.32f},0xFFFF8DAB,r*.035f);
        }
        if(g==Kawaii.SQUISHY)for(int side=-1;side<=1;side+=2) {
            q.line(x+rx*side*.67f,y+ry*.19f,x+rx*side*.95f,y+ry*.19f,0xFFB4A7C9,r*.15f);
            q.line(x+rx*side*.95f,y+ry*.08f,x+rx*side*.95f,y+ry*.30f,0xFFECDFEE,r*.11f);
        }
        float[] body=shape(x,y,rx,ry,FORMS[g]);
        q.fillPoly(body,Glyph.mix(base,INK,.26f));
        q.fillPoly(shape(x-rx*.025f,y-ry*.045f,rx*.95f,ry*.94f,FORMS[g]),base);
        q.fillPoly(shape(x-rx*.10f,y-ry*.14f,rx*.77f,ry*.70f,FORMS[g]),Glyph.mix(base,0xFFFFFFFF,.20f));
        q.fillEllipse(x-rx*.36f,y-ry*.43f,rx*.15f,ry*.075f,0xA6FFFFFF);
        q.fillCircle(x-rx*.53f,y-ry*.25f,r*.047f,0x90FFFFFF);
        if(g==Kawaii.CAT) {
            for(int side=-1;side<=1;side+=2)q.arc(x+side*rx*.15f,y,rx*.42f,ry*.72f,side<0?110:290,140,0x48863E23,r*.035f);
            q.polyline(new float[]{x-rx*.04f,y-ry*.70f,x-rx*.09f,y-ry*.93f,x+rx*.07f,y-ry*1.02f},0xFF68945A,r*.13f);
        }
        if(g==Kawaii.BLOB) {
            for(int side=-1;side<=1;side+=2) {
                q.fillPoly(shape(x+side*rx*.46f,y-ry*.55f,rx*.13f,ry*.16f,new float[]{-.7f,.6f,0,-1,.8f,.6f}),0xFFC68EA9);
                for(int i=0;i<2;i++)q.line(x+side*rx*.48f,y+ry*(.13f+i*.13f),x+side*rx*.91f,y+ry*(.07f+i*.24f),0xFFC3B0D3,r*.027f);
            }
        }
        if(g==Kawaii.GRAPES) {
            float[] hat=shape(x,y,rx,ry,new float[]{-.80f,-.48f,-.48f,-.63f,-.28f,-1.08f,.06f,-1.38f,.32f,-1.27f,.09f,-1.08f,.50f,-.64f,.78f,-.48f,.12f,-.36f});
            q.fillPoly(hat,0xFF5B3D7E);
            q.polyline(new float[]{x-rx*.42f,y-ry*.69f,x,y-ry*.61f,x+rx*.39f,y-ry*.69f},0xFFF3AE62,r*.11f);
            q.fillCircle(x+rx*.08f,y-ry*.66f,r*.09f,0xFFFFE5A1);
        }
        if(g==Kawaii.SQUISHY) {
            q.fillPoly(shape(x,y,rx,ry,new float[]{-.77f,-.43f,-.58f,-.78f,.56f,-.77f,.77f,-.42f,.42f,-.45f,.28f,-.31f,.10f,-.48f,-.15f,-.33f,-.33f,-.47f}),0xFF454554);
            q.line(x+rx*.28f,y-ry*.11f,x+rx*.61f,y-ry*.21f,0xFF527B5C,r*.035f);
            for(int i=0;i<3;i++)q.line(x+rx*(.33f+i*.10f),y-ry*.22f,x+rx*(.36f+i*.10f),y-ry*.08f,0xFF527B5C,r*.035f);
        }
        face(q,g,x,y,rx,ry,r,happy,mood,clock);
    }
    private static void face(Painter p,int g,float x,float y,float rx,float ry,float r,float happy,int mood,float clock) {
        int ink=g==Kawaii.BLOB?0xFFF6EDF9:INK;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*rx*.29f,ey=y+ry*.035f;
            if(mood==2 || happy>.7f)p.arc(ex,ey+ry*.07f,rx*.11f,ry*.10f,200,140,ink,r*.055f);
            else if(g==Kawaii.CAT && mood==0) {
                p.fillPoly(shape(ex,ey,rx*.15f,ry*.16f,new float[]{-1,.6f,-.2f,-1,1,.6f}),ink);
                p.fillCircle(ex,ey-ry*.025f,r*.025f,0xFFFFE4A3);
            } else {p.fillEllipse(ex,ey,rx*.105f,ry*(mood==1?.17f:.135f),ink);p.fillCircle(ex-rx*.025f,ey-ry*.04f,r*.03f,g==Kawaii.BLOB?INK:0xFFFFFFFF);}
            if(mood==2)p.line(ex-side*rx*.14f,ey-ry*.20f,ex+side*rx*.07f,ey-ry*.12f,ink,r*.045f);
            p.fillEllipse(x+side*rx*.48f,y+ry*.26f,rx*.13f,ry*.08f,0x99F278A2);
        }
        if(g==Kawaii.CAT && mood==0) {
            p.fillPoly(shape(x,y+ry*.24f,rx*.055f,ry*.065f,new float[]{0,-1,1,1,-1,1}),ink);
            p.fillPoly(shape(x,y+ry*.40f,rx*.25f,ry*.12f,new float[]{-1,-.4f,-.3f,.1f,.1f,-.05f,1,-.4f,.55f,.9f,-.35f,1}),ink);
            p.fillRect(x-rx*.06f,y+ry*.38f,x+rx*.035f,y+ry*.45f,0xFFFFC772);
        } else if(mood==1)p.fillEllipse(x,y+ry*.35f,rx*.08f,ry*.12f,ink);
        else p.arc(x,y+ry*(mood==3?.46f:.30f),rx*.15f,ry*.12f,mood==3?205:20,140,ink,r*.045f);
        if(g==Kawaii.STRAWBERRY && mood!=3)for(int side=-1;side<=1;side+=2)p.fillPoly(new float[]{x+side*rx*.1f,y+ry*.30f,x+side*rx*.04f,y+ry*.44f,x+side*rx*.16f,y+ry*.34f},0xFFF6EDF9);
        if(g==Kawaii.BLOB && mood!=1) {
            p.fillPoly(shape(x,y+ry*.24f,rx*.09f,ry*.06f,new float[]{-1,-.5f,1,-.5f,0,1}),0xFFF4B1CB);
            p.arc(x-rx*.09f,y+ry*.31f,rx*.10f,ry*.08f,0,155,ink,r*.03f);
            p.arc(x+rx*.09f,y+ry*.31f,rx*.10f,ry*.08f,25,155,ink,r*.03f);
        }
        if(mood==3)Kawaii.cryTears(p,x,y,rx*.29f,rx,ry,clock,1);
    }
}
