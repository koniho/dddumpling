package com.dddumpling.game;

/** Four distinct shoe profiles. Close-up stitching never changes the small-view silhouette. */
final class AccessoryFootwear {
    static float top(int v){return v==3?-.78f:v==4?-.64f:v==5?-.54f:-.39f;}
    private static float[] points(float x,float y,float r,int side,float... a){for(int i=0;i<a.length;i+=2){a[i]=x+a[i]*r*side;a[i+1]=y+a[i+1]*r;}return a;}
    private static void poly(Painter p,float x,float y,float r,int side,int c,float... a){p.fillPoly(points(x,y,r,side,a),c);}
    static void draw(Painter p,int v,float x,float y,float r,int s,float t,float detail) {
        Painter q=detail>0?new OpacityPainter(p,detail):p;
        int sole=v==3?0xFF596C89:v==4?0xFF7C555B:v==5?0xFF4B493D:0xFF527A80;
        int foam=v==3?0xFFDFEFF2:v==4?0xFFFFEEDB:v==5?0xFFB6A071:0xFFF3F7DD;
        poly(p,x,y,r,s,sole,-.34f,.02f,-.36f,.17f,-.27f,.28f,.23f,.30f,.46f,.23f,.52f,.14f,.48f,.04f,.23f,-.02f,-.26f,-.07f);
        poly(p,x,y-r*.05f,r,s,foam,-.33f,.03f,-.32f,.17f,-.25f,.22f,.24f,.24f,.45f,.17f,.49f,.09f,.44f,.02f,.24f,-.02f,-.26f,-.05f);
        if(v==3) {
            poly(p,x,y,r,s,0xFF678DA6,-.29f,.07f,-.28f,-.54f,-.17f,-.67f,.13f,-.67f,.23f,-.53f,.20f,-.16f,.40f,-.10f,.49f,.03f,.43f,.13f,.23f,.18f,-.25f,.13f);
            poly(p,x,y,r,s,0xFFA5D0DE,-.17f,-.53f,.09f,-.53f,.11f,-.13f,.32f,-.07f,.39f,.06f,-.19f,.07f);
            p.fillEllipse(x-s*r*.02f,y-r*.57f,r*.28f,r*.105f,0xFFDAE7E9);
            p.fillEllipse(x-s*r*.02f,y-r*.61f,r*.24f,r*.07f,0xFFF7F4EC);
            if(detail>0) {
                for(int i=0;i<3;i++)q.arc(x-s*r*.02f,y-r*(.18f+i*.12f),r*.22f,r*.05f,0,170,0x956583A5,r*.025f);
                for(int i=-2;i<=2;i++)q.fillCircle(x+r*i*.10f,y-r*.62f,r*.04f,0xFFF8F5E9);
                q.line(x+s*r*.17f,y-r*.49f,x+s*r*.17f,y-r*.17f,0xCCEDF5EF,r*.025f);
            }
        } else if(v==4) {
            poly(p,x,y,r,s,0xFF9F4D65,-.30f,.09f,-.29f,-.46f,-.16f,-.57f,.11f,-.52f,.18f,-.28f,.35f,-.13f,.47f,-.04f,.46f,.13f,-.23f,.14f);
            poly(p,x,y,r,s,0xFFDF8B99,-.19f,-.43f,-.08f,-.50f,.06f,-.44f,.13f,-.20f,.29f,-.08f,.19f,.08f,-.18f,.08f);
            p.fillEllipse(x+s*r*.34f,y+r*.025f,r*.19f,r*.115f,0xFFFFEBD3);
            p.fillEllipse(x-s*r*.13f,y-r*.28f,r*.105f,r*.12f,0xFFF5E4D5);
            if(detail>0) {
                q.polyline(points(x,y,r,s,-.25f,-.42f,-.23f,-.04f,.13f,.06f),0xDFFFF4DD,r*.021f);
                for(int i=0;i<3;i++){float yy=y-r*(.12f+i*.105f);q.line(x-s*r*.015f,yy-r*.045f,x+s*r*.13f,yy,0xFFFFF3D8,r*.03f);}
                q.arc(x+s*r*.34f,y+r*.025f,r*.16f,r*.085f,210,125,0xFFC6BBAF,r*.023f);
                q.line(x-r*.23f,y+r*.16f,x+r*.36f,y+r*.16f,0xFF896A79,r*.022f);
            }
        } else if(v==5) {
            poly(p,x,y,r,s,0xFF88613F,-.31f,.08f,-.29f,-.33f,-.19f,-.46f,.09f,-.46f,.21f,-.29f,.27f,-.13f,.43f,-.08f,.51f,.05f,.43f,.15f,-.25f,.15f);
            poly(p,x,y,r,s,0xFFC79B63,-.18f,-.32f,.05f,-.36f,.15f,-.20f,.22f,-.05f,.05f,.10f,-.22f,.06f);
            poly(p,x,y,r,s,0xFF625B48,.24f,-.09f,.43f,-.06f,.49f,.04f,.42f,.13f,.24f,.15f,.17f,.06f);
            p.fillEllipse(x-s*r*.045f,y-r*.38f,r*.23f,r*.08f,0xFF585D4B);
            if(detail>0) {
                for(int i=0;i<3;i++) {
                    float yy=y-r*(.06f+i*.10f);
                    q.line(x-s*r*.05f,yy-r*.04f,x+s*r*.14f,yy,0xFFFFDEA1,r*.025f);
                    q.fillCircle(x-s*r*.075f,yy-r*.04f,r*.032f,0xFFD5BA7D);
                    q.fillCircle(x+s*r*.16f,yy,r*.032f,0xFFD5BA7D);
                }
                q.polyline(points(x,y,r,s,-.25f,-.29f,-.24f,.04f,-.12f,.08f),0xA7E5C395,r*.02f);
                for(int i=0;i<5;i++)q.line(x+r*(-.25f+i*.15f),y+r*.20f,x+r*(-.25f+i*.15f),y+r*.27f,0xFF4B493D,r*.035f);
            }
        } else {
            poly(p,x,y,r,s,0xFF68A29C,-.34f,.06f,-.32f,-.23f,-.19f,-.33f,-.03f,-.22f,.13f,-.13f,.38f,-.07f,.54f,.04f,.46f,.13f,.17f,.16f,-.25f,.11f);
            poly(p,x,y,r,s,0xFFB9E38D,-.21f,-.20f,-.08f,-.21f,.12f,-.08f,.34f,-.03f,.41f,.06f,.15f,.10f,-.22f,.03f);
            poly(p,x,y,r,s,0xFFF0998C,-.32f,-.21f,-.20f,-.29f,-.15f,-.17f,-.17f,.04f,-.31f,.02f);
            poly(p,x,y,r,s,0xFFE8F7CE,.03f,-.08f,.12f,-.11f,.26f,.04f,.20f,.07f);
            if(detail>0) {
                for(int i=0;i<7;i++)q.fillCircle(x+s*r*(.02f+i*.048f),y+r*(-.035f+(i%2)*.06f),r*.013f,0x8876A585);
                for(int i=0;i<3;i++)q.line(x+s*r*(-.10f+i*.075f),y-r*(.16f-i*.035f),x+s*r*(-.015f+i*.075f),y-r*(.10f-i*.035f),0xFFF5F9E5,r*.021f);
                q.line(x-r*.22f,y+r*.17f,x+r*.36f,y+r*.17f,0x99B6D0C2,r*.025f);
            }
        }
        if(detail>0)q.arc(x+s*r*.26f,y+r*.06f,r*.18f,r*.065f,200,125,0x70FFFFFF,r*.024f);
    }
}
