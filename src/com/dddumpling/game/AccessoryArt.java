package com.dddumpling.game;

/** Small vector pieces attached by the character renderer, inside its movement/scale transform. */
final class AccessoryArt extends Draw {
    static void outfit(Painter p,int who,float x,float faceY,float faceR,float topY,
            float feetX,float feetY,float r,float clock,float fade,boolean ninja) {
        if(r<=0 || fade<=0)return;
        Painter out=fade==1?p:new OpacityPainter(p,fade);
        for(int slot=0;slot<Accessories.SLOTS;slot++) {
            int item=p.accessory(who,slot);
            // Gameplay hood owns the head/face; the saved outfit is never changed.
            if(item<0 || item>=Accessories.COUNT || ninja && slot!=Accessories.BOOTS)continue;
            if(slot==Accessories.HAT)item(out,item,x,topY,r*.67f,clock);
            else if(slot==Accessories.BOOTS)item(out,item,feetX,feetY,r*.68f,clock);
            else item(out,item,x,faceY,faceR,clock);
        }
    }
    static void item(Painter p,int item,float x,float y,float r,float clock) {
        if(item<0 || item>=Accessories.COUNT)return;
        int slot=Accessories.slot(item),v=item%3,c=Accessories.COLOR[item];
        int shade=Glyph.mix(c,0xFF3E3459,.36f),hi=Glyph.mix(c,0xFFFFFFFF,.5f);
        if(slot==Accessories.HAT) {
            if(v==2) {
                float[] crown={x-r,y+r*.14f,x-r*.92f,y-r*.64f,x-r*.45f,y-r*.26f,x,y-r*.90f,
                    x+r*.45f,y-r*.26f,x+r*.92f,y-r*.64f,x+r,y+r*.14f};
                p.fillPoly(crown,shade);
                p.fillPoly(new float[]{x-r*.86f,y,x-r*.79f,y-r*.4f,x-r*.4f,y-r*.09f,x,y-r*.63f,
                    x+r*.4f,y-r*.09f,x+r*.79f,y-r*.4f,x+r*.86f,y},c);
                for(int i=-1;i<=1;i++)p.fillCircle(x+i*r*.77f,y-r*(i==0?.86f:.61f),r*.1f,hi);
                star(p,x,y-r*.05f,r*.18f,0xFFFFF5C7);
            } else {
                p.fillEllipse(x,y-r*.17f,r*.77f,r*.57f,shade);
                p.fillEllipse(x-r*.09f,y-r*.26f,r*.64f,r*.46f,c);
                p.fillEllipse(x-r*.22f,y-r*.42f,r*.32f,r*.17f,hi);
                p.fillRect(x-r*.74f,y-r*.13f,x+r*.74f,y+r*.07f,v==0?0xFFC07765:0xFF508D79);
                p.fillEllipse(x,y+r*.10f,r*(v==0?1.20f:1.02f),r*.19f,shade);
                p.fillEllipse(x-r*.03f,y+r*.04f,r*(v==0?1.18f:1),r*.13f,c);
                if(v==1) {
                    p.line(x,y-r*.67f,x+r*.08f,y-r*.98f,0xFF52896B,r*.07f);
                    p.fillPoly(new float[]{x+r*.07f,y-r*.88f,x+r*.04f,y-r*1.12f,x-r*.35f,y-r*1.10f,x-r*.21f,y-r*.88f},0xFFC1F1AB);
                    p.fillPoly(new float[]{x+r*.07f,y-r*.88f,x+r*.25f,y-r*1.17f,x+r*.50f,y-r*1.16f,x+r*.36f,y-r*.91f},0xFF69B97C);
                }
            }
        } else if(slot==Accessories.GLASSES) {
            p.line(x-r*.88f,y-r*.03f,x+r*.88f,y-r*.03f,shade,r*.09f);
            for(int side=-1;side<=1;side+=2) {
                float cx=x+side*r*.35f;
                if(v==2) {star(p,cx,y,r*.42f,c);star(p,cx,y,r*.30f,0xD54C477D);}
                else {
                    p.fillEllipse(cx,y,r*.33f,r*(v==0?.29f:.25f),shade);
                    p.fillEllipse(cx,y-r*.025f,r*.29f,r*(v==0?.26f:.22f),c);
                    p.fillEllipse(cx,y-r*.02f,r*.23f,r*(v==0?.20f:.17f),0xC934506D);
                }
                p.line(cx-r*.11f,y-r*.12f,cx+r*.04f,y+r*.04f,0xA9FFFFFF,r*.055f);
            }
        } else if(slot==Accessories.BOOTS) {
            for(int side=-1;side<=1;side+=2) {
                float cx=x+side*r*.47f,yy=y+r*.025f*(float)Math.sin(clock*4+side);
                p.fillEllipse(cx+side*r*.07f,yy+r*.13f,r*.37f,r*.22f,shade);
                p.fillRect(cx-r*.24f,yy-r*.30f,cx+r*.24f,yy+r*.12f,c);
                p.fillEllipse(cx+side*r*.07f,yy+r*.06f,r*.35f,r*.19f,c);
                p.fillEllipse(cx-r*.09f,yy-r*.10f,r*.055f,r*.20f,hi);
                p.line(cx-r*.25f,yy-r*.30f,cx+r*.25f,yy-r*.30f,shade,r*.09f);
                if(v==1)p.fillCircle(cx+side*r*.12f,yy,r*.09f,0xFFF2FBFF);
                if(v==2)star(p,cx+side*r*.12f,yy,r*.12f,0xFFFFFFC1);
            }
        } else {
            // Lower-face party masks stay distinct from the Ninja gameplay hood.
            p.line(x-r*.72f,y+r*.12f,x+r*.72f,y+r*.12f,shade,r*.055f);
            p.fillPoly(new float[]{x-r*.56f,y+r*.08f,x,y+r*.02f,x+r*.56f,y+r*.08f,
                x+r*.47f,y+r*.42f,x,y+r*.57f,x-r*.47f,y+r*.42f},shade);
            p.fillPoly(new float[]{x-r*.50f,y+r*.09f,x,y+r*.05f,x+r*.50f,y+r*.09f,
                x+r*.42f,y+r*.37f,x,y+r*.50f,x-r*.42f,y+r*.37f},c);
            p.line(x-r*.34f,y+r*.17f,x+r*.34f,y+r*.17f,hi,r*.06f);
            if(v==0)for(int i=0;i<5;i++) {
                float a=i*6.283f/5;
                p.fillCircle(x+(float)Math.cos(a)*r*.09f,y+r*.32f+(float)Math.sin(a)*r*.09f,r*.065f,hi);
            }
            else if(v==1) {p.fillCircle(x,y+r*.31f,r*.13f,hi);p.fillCircle(x+r*.065f,y+r*.26f,r*.115f,c);}
            else for(int i=0;i<3;i++)p.fillCircle(x+(i-1)*r*.21f,y+r*(.30f+(i%2)*.08f),r*.045f,i==1?0xFFFFADD9:0xFFFFE796);
        }
    }
    private static void star(Painter p,float x,float y,float r,int color) {
        float[] pts=new float[20];
        for(int i=0;i<10;i++) {float a=-1.5708f+i*.62832f,rr=r*(i%2==0?1:.48f);
            pts[i*2]=x+(float)Math.cos(a)*rr;pts[i*2+1]=y+(float)Math.sin(a)*rr;}
        p.fillPoly(pts,color);
    }
}
