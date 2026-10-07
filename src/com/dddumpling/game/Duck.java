package com.dddumpling.game;

/** Original rubber-duck silhouettes; shared by the case, rainbow rider and companion. */
final class Duck extends Draw {
    private static final int EDGE=0xFF3A2E4F;
    private static final int[] RAINBOW={0xFFFF86AA,0xFFFFB454,0xFFFFE46B,0xFF88DEA6,0xFF83DDEC,0xFFBA9DE9};
    private Duck() {}
    private static void oval(Painter p,float x,float y,float rx,float ry,int color,float edge) {
        p.fillEllipse(x,y,rx+edge,ry+edge,EDGE);
        p.fillEllipse(x,y,rx,ry,color);
    }
    private static void skin(Painter p,int duck,int layer,float x,float y,float rx,float ry,int color,float edge) {
        DuckBodies bodies=p.ducks();
        if(bodies==null) {oval(p,x,y,rx,ry,color,edge);return;}
        float[] outline=bodies.skin[duck][layer].outline();
        // All depth layers follow the very same deformed spring ring.
        float[] points=new float[outline.length];
        for(int pass=0;pass<5;pass++) {
            float scale=pass==0?1f+edge/Math.min(rx,ry):pass==1?1f:pass==2?.91f:pass==3?.74f:.37f;
            float shiftX=pass<2?0:pass==2?-.025f:pass==3?-.12f:-.30f;
            float shiftY=pass<2?0:pass==2?-.04f:pass==3?-.18f:-.43f;
            for(int i=0;i<outline.length;i+=2) {
                points[i]=x+rx*(outline[i]*scale+shiftX);
                points[i+1]=y+ry*(outline[i+1]*scale+shiftY);
            }
            int tint=pass==0?EDGE:pass==1?Glyph.mix(color,EDGE,.22f):pass==2?color:
                    Glyph.mix(color,0xFFFFFFFF,pass==3?.15f:.36f);
            p.fillPoly(points,tint);
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
        // The tail curls up behind the plump body, rather than forming another head.
        oval(p,x+r*.62f,y+r*.24f,r*.32f,r*.24f,accent,r*.045f);
        oval(p,x+r*.83f,y+r*.07f,r*.13f,r*.23f,body,r*.04f);
        skin(p,kind,0,x,y+r*.37f,r*.80f,r*.53f,body,r*.055f);
        p.fillEllipse(x-r*.12f,y+r*.57f,r*.55f,r*.23f,Glyph.mix(body,0xFFFFFFFF,.30f));
        p.fillEllipse(x+r*.30f,y+r*.48f+wing,r*.37f,r*.18f,0x33000000);
        skin(p,kind,2,x+r*.28f,y+r*.39f+wing,r*.36f,r*.21f,accent,r*.035f);
        if(known && kind>=9)for(int i=0;i<6;i++) {
            p.arc(x+r*.30f,y+r*.37f+wing,r*(.30f-i*.036f),r*(.19f-i*.022f),0,175,RAINBOW[i],r*.04f);
        }
        float hx=x-r*.21f,hy=y-r*.33f;
        p.fillEllipse(hx+r*.025f,hy+r*.46f,r*.50f,r*.18f,0x33000000);
        skin(p,kind,1,hx,hy,r*.59f,r*.58f,body,r*.055f);
        p.arc(hx-r*.10f,hy-r*.08f,r*.35f,r*.32f,205,62,known?0x99FFFFFF:accent,r*.07f);
        if(!known) {
            p.text("?",hx,hy+r*.18f,type(r*.62f),INK,Painter.CENTER,true);return;
        }
        if(kind==1)for(int i=0;i<3;i++) {
            float bx=hx+(i-1)*r*.17f,by=hy-r*(.57f+(i%2)*.12f);
            p.fillCircle(bx,by,r*.12f,accent);p.strokeCircle(bx,by,r*.12f,0xAAFFFFFF,r*.025f);
        }
        if(kind==2 || kind==4) {
            p.fillPoly(new float[]{hx-r*.05f,hy-r*.51f,hx-r*.24f,hy-r*.79f,hx+r*.04f,hy-r*.65f,
                    hx+r*.25f,hy-r*.78f,hx+r*.16f,hy-r*.49f},0xFF5CB77A);
        } else if(kind==6) {
            for(int i=0;i<3;i++)p.fillCircle(hx+(i-1)*r*.16f,hy-r*.52f-(i%2)*r*.09f,r*.15f,body);
        } else if(kind==8) {
            p.fillPoly(star(hx,hy-r*.63f,r*.22f,r*.105f,5,-1.57f),accent);
        } else if(kind==10) {
            p.fillPoly(new float[]{hx-r*.36f,hy-r*.49f,hx-r*.40f,hy-r*.88f,hx-r*.14f,hy-r*.69f,
                    hx,hy-r*.99f,hx+r*.15f,hy-r*.69f,hx+r*.39f,hy-r*.88f,hx+r*.34f,hy-r*.49f},0xFFFFC453);
            for(int i=0;i<3;i++)p.fillCircle(hx+(i-1)*r*.22f,hy-r*.60f,r*.055f,RAINBOW[i*2]);
        } else {
            p.arc(hx,hy-r*.54f,r*.15f,r*.15f,170,235,accent,r*.085f);
        }
        if(kind==2 || kind==8)for(int i=0;i<4;i++) {
            float px=x-r*.40f+i*r*.17f,py=y+r*(.55f+(i%2)*.10f);
            if(kind==8)p.fillPoly(star(px,py,r*.067f,r*.03f,5,0),accent);
            else p.fillEllipse(px,py,r*.028f,r*.045f,0xFFFFE9A4);
        }
        if(kind==5)p.arc(x+r*.32f,y+r*.40f+wing,r*.10f,r*.09f,40,230,0xFFEADBFF,r*.045f);
        if(kind==7)for(int i=0;i<3;i++)p.arc(x+r*.18f,y+r*.45f+wing,r*(.13f+i*.065f),r*.12f,185,125,0xFFFFCA69,r*.035f);
        boolean blink=Math.sin(clock*1.7f+kind)>.985f;
        for(int side=-1;side<=1;side+=2) {
            float ex=hx+side*r*.23f+look*r*.10f,ey=hy-r*.02f;
            if(blink || mood==RunCompanion.VICTORY)p.arc(ex,ey,r*.075f,r*.055f,195,150,EDGE,r*.045f);
            else {
                p.fillEllipse(ex,ey,r*.071f,r*.088f,EDGE);
                p.fillCircle(ex-r*.018f,ey-r*.029f,r*.022f,0xFFFFFFFF);
            }
            p.fillEllipse(hx+side*r*.35f,hy+r*.15f,r*.105f,r*.055f,0x88F57D9D);
        }
        skin(p,kind,3,hx,hy+r*.22f,r*.22f,r*.10f,kind==3?0xFFFFC5A4:kind==5?0xFFFFDD7A:0xFFFFA640,r*.028f);
        p.line(hx-r*.12f,hy+r*.23f,hx+r*.12f,hy+r*.23f,0x99764A33,r*.018f);
        if(ninja)Trinket.ninjaMask(p,hx,hy,r*.56f,1);
    }
}
