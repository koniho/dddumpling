package com.dddumpling.game;

/** Extra hats share the existing brim anchor and omit fabric/weave trim at small sizes. */
final class AccessoryHats {
    static float left(int v){return v==4?-1.02f:v==6?-1.36f:-1.15f;}
    static float right(int v){return v==4?1.64f:v==6?1.36f:1.15f;}
    static float top(int v){return v==3?-1.50f:v==4?-.97f:v==5?-1.58f:-1.08f;}
    static float bottom(int v){return v==6?.27f:.25f;}
    private static float[] shape(float x,float y,float r,float... a){for(int i=0;i<a.length;i+=2){a[i]=x+a[i]*r;a[i+1]=y+a[i+1]*r;}return a;}
    static void draw(Painter p,int v,float x,float y,float r,float t,float detail) {
        Painter q=detail>0?new OpacityPainter(p,detail):p;
        if(v==3) {
            p.fillEllipse(x,y+r*.08f,r*1.12f,r*.20f,0xFF292640);
            p.fillEllipse(x,y,r*1.10f,r*.15f,0xFF5B4E83);
            p.fillPoly(shape(x,y,r,-.67f,-1.28f,.67f,-1.28f,.54f,.01f,-.54f,.01f),0xFF2C2948);
            p.fillPoly(shape(x,y,r,-.53f,-1.28f,.26f,-1.28f,.19f,-.03f,-.45f,-.03f),0xFF514570);
            p.fillEllipse(x,y-r*1.28f,r*.67f,r*.18f,0xFF7C699F);
            p.fillEllipse(x,y-r*1.30f,r*.56f,r*.11f,0xFF3B3359);
            p.fillPoly(shape(x,y,r,-.56f,-.29f,.56f,-.29f,.54f,-.03f,-.54f,-.03f),0xFFD686B9);
            if(detail>0) {
                q.fillPoly(shape(x,y,r,-.45f,-1.19f,-.29f,-1.19f,-.26f,-.36f,-.40f,-.36f),0x557F72B1);
                q.arc(x,y-r*1.28f,r*.60f,r*.14f,190,140,0xFFC4ADD8,r*.025f);
                q.arc(x,y,r*.93f,r*.10f,5,160,0xAA9F8CB5,r*.03f);
                AccessoryArt.star(q,x+r*.32f,y-r*.16f,r*.10f,0xFFFFE5A1);
                q.line(x-r*.49f,y-r*.24f,x+r*.47f,y-r*.24f,0xFFFFB7DB,r*.025f);
            }
        } else if(v==4) {
            p.fillPoly(shape(x,y,r,.10f,-.08f,.71f,-.13f,1.37f,-.02f,1.61f,.09f,1.41f,.22f,.83f,.23f,.22f,.11f),0xFF37616C);
            p.fillPoly(shape(x,y,r,.13f,-.10f,.74f,-.15f,1.36f,-.06f,1.57f,.04f,1.36f,.12f,.80f,.13f,.23f,.04f),0xFF82D9D2);
            p.fillPoly(shape(x,y,r,-.94f,.06f,-.94f,-.26f,-.80f,-.58f,-.52f,-.78f,-.16f,-.85f,.20f,-.77f,.49f,-.58f,.65f,-.28f,.64f,.06f),0xFF518FAD);
            p.fillPoly(shape(x,y,r,-.79f,-.25f,-.66f,-.55f,-.41f,-.72f,-.16f,-.77f,.06f,-.71f,.15f,-.27f,.10f,.02f,-.81f,.02f),0xFF88C9D9);
            p.fillRect(x-r*.92f,y-r*.07f,x+r*.63f,y+r*.09f,0xFF4B7998);
            p.fillEllipse(x-r*.15f,y-r*.85f,r*.11f,r*.06f,0xFFC0EFE9);
            if(detail>0) {
                q.polyline(shape(x,y,r,-.16f,-.77f,-.47f,-.49f,-.52f,-.10f),0xAAC8F1ED,r*.025f);
                q.polyline(shape(x,y,r,-.12f,-.77f,.16f,-.47f,.27f,-.10f),0xAA3A7596,r*.025f);
                q.polyline(shape(x,y,r,.62f,-.05f,1.12f,.02f,1.37f,.07f),0xAFC6FFF0,r*.025f);
                q.fillEllipse(x-r*.53f,y-r*.34f,r*.14f,r*.17f,0xFFF4D792);
                q.line(x-r*.60f,y-r*.36f,x-r*.45f,y-r*.31f,0xFF6E9AA1,r*.025f);
            }
        } else if(v==5) {
            p.fillPoly(shape(x,y,r,-.81f,-.06f,-.79f,-.49f,-.63f,-.88f,-.33f,-1.13f,0,-1.22f,.34f,-1.11f,.65f,-.84f,.80f,-.46f,.82f,-.06f),0xFF866493);
            p.fillPoly(shape(x,y,r,-.67f,-.12f,-.64f,-.52f,-.48f,-.87f,-.23f,-1.09f,.02f,-1.14f,.31f,-.91f,.40f,-.12f),0xFFC6A1C7);
            p.fillCircle(x,y-r*1.32f,r*.24f,0xFFD9C6E2);
            p.fillCircle(x-r*.05f,y-r*1.37f,r*.16f,0xFFF3E5ED);
            p.fillEllipse(x,y+r*.05f,r*.87f,r*.17f,0xFF725780);
            p.fillPoly(shape(x,y,r,-.87f,-.19f,-.78f,-.26f,.78f,-.26f,.87f,-.19f,.87f,.06f,.76f,.14f,-.76f,.14f,-.87f,.06f),0xFFDFB7C8);
            if(detail>0) {
                for(int i=-6;i<=6;i++)q.line(x+r*i*.12f,y-r*.18f,x+r*i*.12f,y+r*.06f,0x998E6C98,r*.025f);
                for(int row=0;row<3;row++)for(int col=-2;col<=2;col++) {
                    float xx=x+col*r*.20f,yy=y-r*(.42f+row*.20f);
                    q.polyline(new float[]{xx-r*.055f,yy-r*.03f,xx,yy+r*.025f,xx+r*.055f,yy-r*.03f},0x90EFD8E4,r*.018f);
                }
                for(int i=0;i<8;i++){float a=i*.7854f;q.fillCircle(x+(float)Math.cos(a)*r*.20f,y-r*1.32f+(float)Math.sin(a)*r*.20f,r*.055f,0xDDDFD1E6);}
                q.fillRect(x+r*.40f,y-r*.17f,x+r*.61f,y+r*.02f,0xFFF4E5D1);
            }
        } else {
            p.fillEllipse(x,y+r*.05f,r*1.35f,r*.22f,0xFF8B673F);
            p.fillPoly(shape(x,y,r,-1.32f,.01f,-.16f,-1.01f,-.08f,-1.06f,0,-1.01f,1.31f,.01f,.85f,.13f,0,.19f,-.85f,.13f),0xFFD5AD6D);
            p.fillPoly(shape(x,y,r,-1.26f,.01f,-.10f,-1.02f,-.32f,.16f,-.85f,.11f),0xFFF1D299);
            p.fillPoly(shape(x,y,r,-.08f,-1.02f,1.27f,.01f,.84f,.12f,.41f,.17f),0xFFBD9259);
            if(detail>0) {
                for(int i=1;i<=6;i++){float u=i/7f;q.arc(x-r*.08f*(1-u),y+r*(-1.04f+u*1.10f),r*1.25f*u,r*.14f*u,0,180,0x8B926B3B,r*.018f);}
                for(int i=-6;i<=6;i++){float u=i/6f;q.line(x-r*.08f,y-r*.99f,x+r*u*1.25f,y+r*(.04f+.11f*(1-u*u)),0x66FFF0C3,r*.015f);}
                q.arc(x,y+r*.04f,r*1.28f,r*.17f,0,180,0xFFE8C48A,r*.035f);
            }
        }
    }
}
