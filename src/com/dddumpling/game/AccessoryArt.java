package com.dddumpling.game;

/** Small vector pieces attached by the character renderer, inside its movement/scale transform. */
final class AccessoryArt extends Draw {
    private static final int[] DRAW_ORDER={Accessories.MASK,Accessories.GLASSES,Accessories.HAT,Accessories.BOOTS};
    static void outfit(Painter p,int who,float x,float faceY,float faceR,float topY,
            float feetX,float feetY,float r,float clock,float fade,boolean ninja) {
        outfit(p,who,x,faceY,faceR,topY,feetX,feetY,r,clock,fade,ninja,1f);
    }
    static void outfit(Painter p,int who,float x,float faceY,float faceR,float topY,
            float feetX,float feetY,float r,float clock,float fade,boolean ninja,float bootScale) {
        if(r<=0 || fade<=0)return;
        Painter out=fade==1?p:new OpacityPainter(p,fade);
        int flair=p.accessory(who,Accessories.FLAIR);
        if(flair>=0 && flair<Accessories.COUNT && Accessories.slot(flair)==Accessories.FLAIR)
            AccessoryFlair.draw(out,Accessories.variant(flair),feetX,(topY+feetY)*.5f,r,clock);
        for(int slot:DRAW_ORDER) {
            int item=p.accessory(who,slot);
            // Gameplay hood owns the head/face; the saved outfit is never changed.
            if(item<0 || item>=Accessories.COUNT || ninja && slot!=Accessories.BOOTS)continue;
            if(slot==Accessories.HAT)item(out,item,x,topY,r*.67f,clock);
            else if(slot==Accessories.BOOTS)item(out,item,feetX,feetY,r*.68f*bootScale,clock);
            else {
                float fit=slot==Accessories.MASK && Accessories.variant(item)==0
                        && who>=Collect.DUCK_FIRST && who<Collect.DUCK_FIRST+Collect.DUCK_COUNT?.74f:1f;
                item(out,item,x,faceY,faceR*fit,clock);
            }
        }
    }
    static void item(Painter p,int item,float x,float y,float r,float clock) {
        if(item<0 || item>=Accessories.COUNT)return;
        int slot=Accessories.slot(item),v=Accessories.variant(item),c=Accessories.COLOR[item];
        int shade=Glyph.mix(c,0xFF3E3459,.36f),hi=Glyph.mix(c,0xFFFFFFFF,.5f);
        if(slot==Accessories.FLAIR) {AccessoryFlair.draw(p,v,x,y,r*.70f,clock);return;}
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
            footwear(p,v,x,y,r,clock,c,shade,hi);
        } else mask(p,v,x,y,r);
    }
    private static void footwear(Painter p,int v,float x,float y,float r,float clock,int c,int shade,int hi) {
        for(int side=-1;side<=1;side+=2) {
            float cx=x+side*r*.49f,yy=y+r*.025f*(float)Math.sin(clock*4+side);
            if(v==0) {
                // Chunky foam clog: wide rounded toe, ventilation holes and a heel strap.
                p.fillEllipse(cx+side*r*.06f,yy+r*.10f,r*.39f,r*.22f,shade);
                p.fillEllipse(cx+side*r*.06f,yy+r*.035f,r*.39f,r*.22f,c);
                p.fillEllipse(cx-side*r*.08f,yy-r*.09f,r*.19f,r*.10f,shade);
                p.arc(cx-side*r*.04f,yy-r*.05f,r*.25f,r*.16f,190,165,hi,r*.065f);
                for(int hole=0;hole<3;hole++)p.fillEllipse(cx+side*r*(.08f+hole*.08f),yy+r*(.01f+(hole%2)*.035f),r*.027f,r*.022f,shade);
                p.arc(cx+side*r*.05f,yy+r*.06f,r*.31f,r*.09f,20,140,hi,r*.035f);
            } else if(v==1) {
                // Low-cut glitter shoes, with a contrasting cushioned sole and bow.
                p.fillEllipse(cx+side*r*.06f,yy+r*.12f,r*.36f,r*.17f,shade);
                p.fillEllipse(cx+side*r*.06f,yy+r*.055f,r*.36f,r*.20f,c);
                p.fillEllipse(cx-side*r*.09f,yy-r*.075f,r*.15f,r*.085f,shade);
                for(int i=0;i<5;i++) {
                    float xx=cx+r*(-.20f+i*.10f),sy=yy+r*(.035f+(i%2)*.06f);
                    float flash=.65f+.35f*(float)Math.sin(clock*4+i*1.7f+side);
                    star(p,xx,sy,r*(.035f+.025f*flash),hi);
                }
                p.fillPoly(new float[]{cx-r*.16f,yy-r*.13f,cx,yy-r*.07f,cx-r*.13f,yy+r*.01f},hi);
                p.fillPoly(new float[]{cx+r*.16f,yy-r*.13f,cx,yy-r*.07f,cx+r*.13f,yy+r*.01f},hi);
                p.fillCircle(cx,yy-r*.065f,r*.045f,c);
            } else {
                // Rounded dress heel: raised heel, sloping arch and upturned toe.
                float[] sole={-.27f,-.10f,-.05f,-.06f,.15f,.09f,.39f,.06f,.40f,.19f,.12f,.24f,-.12f,.05f,-.17f,.30f,-.28f,.30f};
                float[] upper={-.27f,-.21f,-.10f,-.13f,.10f,.01f,.31f,-.03f,.42f,.04f,.38f,.13f,.14f,.16f,-.08f,-.01f,-.25f,-.04f};
                p.fillPoly(shoePoints(sole,cx,yy,r,side),shade);
                p.fillPoly(shoePoints(upper,cx,yy,r,side),c);
                p.line(cx-side*r*.20f,yy-r*.23f,cx-side*r*.14f,yy-r*.03f,hi,r*.07f);
                p.fillEllipse(cx+side*r*.25f,yy+r*.015f,r*.11f,r*.04f,hi);
                p.fillCircle(cx-side*r*.18f,yy-r*.17f,r*.045f,0xFFFFE8A3);
            }
        }
    }
    private static float[] shoePoints(float[] a,float x,float y,float r,int side) {
        for(int i=0;i<a.length;i+=2){a[i]=x+a[i]*r*side;a[i+1]=y+a[i+1]*r;}return a;
    }
    private static float[] oval(float x,float y,float rx,float ry) {
        float[] a=new float[48];for(int i=0;i<24;i++){float t=i*Softbody.TAU/24;
            a[i*2]=x+rx*(float)Math.cos(t);a[i*2+1]=y+ry*(float)Math.sin(t);}return a;
    }
    private static void mask(Painter p,int v,float x,float y,float r) {
        if(v==0) {
            Trinket.ninjaMask(p,x,y,r*1.10f,1);
        } else if(v==1) {
            float[] head=oval(x,y,r*.97f,r*.90f),left=oval(x-r*.35f,y-r*.01f,r*.26f,r*.30f),right=oval(x+r*.35f,y-r*.01f,r*.26f,r*.30f);
            p.fillContours(new float[][]{head,left,right},0xFFFFD667);
            p.arc(x-r*.07f,y-r*.22f,r*.72f,r*.53f,205,120,0xFFFFEDAD,r*.11f);
            p.strokePoly(left,0xFFE7AF44,r*.045f);p.strokePoly(right,0xFFE7AF44,r*.045f);
            p.fillEllipse(x,y+r*.42f,r*.70f,r*.27f,0xFFD98A38);
            p.fillEllipse(x,y+r*.34f,r*.70f,r*.24f,0xFFFFB557);
            p.arc(x,y+r*.32f,r*.55f,r*.09f,10,160,0xFFD17F39,r*.045f);
            for(int side=-1;side<=1;side+=2)p.fillEllipse(x+side*r*.18f,y+r*.26f,r*.045f,r*.025f,0xFFC28742);
        } else if(v==2) {
            // Soft menpo cheek armor and a playful grin; no scary horns or sharp fangs.
            float[] shape={x-r*.91f,y+r*.02f,x-r*.62f,y+r*.07f,x-r*.34f,y+r*.22f,x,y+r*.10f,
                x+r*.34f,y+r*.22f,x+r*.62f,y+r*.07f,x+r*.91f,y+r*.02f,x+r*.76f,y+r*.52f,
                x+r*.44f,y+r*.71f,x,y+r*.84f,x-r*.44f,y+r*.71f,x-r*.76f,y+r*.52f};
            p.fillPoly(shape,0xFF8E3654);
            for(int side=-1;side<=1;side+=2) {
                p.fillEllipse(x+side*r*.55f,y+r*.33f,r*.29f,r*.25f,0xFFDF686B);
                p.arc(x+side*r*.53f,y+r*.27f,r*.21f,r*.13f,200,130,0xFFFFB1A0,r*.055f);
                p.line(x+side*r*.78f,y+r*.07f,x+side*r*.68f,y+r*.19f,0xFFFFD38D,r*.055f);
            }
            p.fillEllipse(x,y+r*.32f,r*.20f,r*.17f,0xFFF28C7C);
            p.fillEllipse(x,y+r*.56f,r*.36f,r*.15f,0xFF642F4C);
            for(int tooth=0;tooth<4;tooth++)p.fillRect(x+r*(-.27f+tooth*.14f),y+r*.47f,x+r*(-.16f+tooth*.14f),y+r*.57f,0xFFFFE5BD);
            p.arc(x,y+r*.61f,r*.39f,r*.14f,25,130,0xFFEB8A83,r*.055f);
        } else {
            float[] head=oval(x,y,r*.98f,r*1.02f);
            float[] left=heroEye(x-r*.35f,y,r,-1),right=heroEye(x+r*.35f,y,r,1);
            p.fillContours(new float[][]{head,left,right},0xFF56518C);
            p.arc(x,y-r*.14f,r*.84f,r*.78f,205,130,0xFF8177B4,r*.10f);
            p.strokePoly(left,0xFF78E4DD,r*.10f);p.strokePoly(right,0xFF78E4DD,r*.10f);
            p.arc(x,y+r*.49f,r*.38f,r*.20f,25,130,0xFF78CECE,r*.075f);
            p.line(x-r*.12f,y-r*.73f,x+r*.13f,y-r*.88f,0xFFB1F6EA,r*.055f);
            star(p,x-r*.12f,y-r*.70f,r*.12f,0xFFFFEAB4);
        }
    }

    private static float[] heroEye(float x,float y,float r,int side) {
        return shoePoints(new float[]{-.25f,-.19f,.04f,-.31f,.29f,-.36f,.27f,.13f,.07f,.30f,-.17f,.24f,-.27f,.06f},x,y,r,side);
    }
    static void star(Painter p,float x,float y,float r,int color) {
        float[] pts=new float[20];
        for(int i=0;i<10;i++) {float a=-1.5708f+i*.62832f,rr=r*(i%2==0?1:.48f);
            pts[i*2]=x+(float)Math.cos(a)*rr;pts[i*2+1]=y+(float)Math.sin(a)*rr;}
        p.fillPoly(pts,color);
    }
}
