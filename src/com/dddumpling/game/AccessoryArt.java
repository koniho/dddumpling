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
        for(int slot:DRAW_ORDER) {
            int item=p.accessory(who,slot);
            // Gameplay hood owns the head/face; the saved outfit is never changed.
            if(item<0 || item>=Accessories.COUNT || ninja && slot!=Accessories.BOOTS)continue;
            if(slot==Accessories.HAT)item(out,item,x,topY,who==Collect.ALLIGATOR_FRIEND?faceR*.81f:r*.67f,clock);
            else if(slot==Accessories.BOOTS) {
                // The cat's four independently stepping paws are dressed by its portrait renderer.
                if(who!=Collect.RAINBOW_CAT_FRIEND)item(out,item,feetX,feetY,r*.68f*bootScale,clock,
                        who>=Collect.DUCK_FIRST && who<Collect.DUCK_FIRST+Collect.DUCK_COUNT?.88f:.49f);
            }
            else {
                int variant=Accessories.variant(item);
                float fit=slot==Accessories.MASK?AccessoryAnchors.maskWidth(who,variant):1f;
                float height=slot==Accessories.MASK?AccessoryAnchors.maskHeight(who,variant):1f;
                item(height==1?out:new MaskFit(out,faceY,height),item,x,faceY,faceR*fit,clock);
            }
        }
    }
    /** Final character pass: clothing, paws and gameplay masks must already be drawn. */
    static void flair(Painter p,int who,float feetX,float topY,float feetY,float r,float clock,float fade) {
        if(r<=0 || fade<=0)return;
        Painter out=fade==1?p:new OpacityPainter(p,fade);
        int flair=p.accessory(who,Accessories.FLAIR);
        if(flair>=0 && flair<Accessories.COUNT && Accessories.slot(flair)==Accessories.FLAIR) {
            float cy=(topY+feetY)*.5f;
            target(out,Accessories.FLAIR,feetX-r*1.65f,cy-r*1.40f,feetX+r*1.65f,cy+r*1.40f);
            if(flair!=AccessoryGlitter.ITEM)AccessoryFlair.draw(out,Accessories.variant(flair),feetX,cy,r,clock);
        }
    }
    static float detail(Painter p,float radius) {
        float t=Math.max(0,Math.min(1,(p.accessorySize(radius)-.065f)/.055f));
        return t*t*(3-2*t);
    }
    static void target(Painter p,int slot,float l,float t,float r,float b) {
        if(p.accessoryTargets())p.accessoryTarget(slot,new float[]{l,t,r,t,r,b,l,b});
    }
    /** Fit the visible artwork, rather than its attachment origin, inside a picker tile. */
    static void thumbnail(Painter p,int item,float l,float t,float right,float bottom,float clock) {
        int slot=Accessories.slot(item),v=Accessories.variant(item);
        float left=-1,top=-1,r=1,b=1;
        if(slot==Accessories.HAT) {
            left=v==0?-1.20f:v==1?-1.02f:-1.08f;r=-left;
            top=v==0?-.75f:v==1?-1.17f:-1.25f;b=.29f;
            if(v>=3){left=AccessoryHats.left(v);r=AccessoryHats.right(v);top=AccessoryHats.top(v);b=AccessoryHats.bottom(v);}
        } else if(slot==Accessories.GLASSES) {left=-.93f;r=.93f;top=v==2?-.46f:v==4?-.49f:-.39f;b=v==0?.42f:-top;if(v==4){left=-1.03f;r=1.03f;}}
        else if(slot==Accessories.BOOTS) {left=v>=3?-1.05f:-.96f;r=-left;top=v>=3?AccessoryFootwear.top(v):-.30f;b=.35f;}
        else if(slot==Accessories.MASK) {
            if(v==0){left=-1.08f;r=1.55f;top=-1.05f;b=1.27f;}
            else if(v==1){left=-.98f;r=.98f;top=-.91f;b=.91f;}
            else if(v==2){left=-.92f;r=.92f;top=0;b=.85f;}
            else {left=-.99f;r=.99f;top=-1.03f;b=1.03f;}
        } else {
            r=v==0?1.26f:v==1?1.20f:v==2?1.03f:.60f;
            b=v==0?1.12f:v==1?1.0f:v==2?.91f:.55f;left=-r;top=-b;
        }
        float radius=Math.min((right-l)/(r-left),(bottom-t)/(b-top))*.89f;
        item(p,item,(l+right)*.5f-(left+r)*.5f*radius,(t+bottom)*.5f-(top+b)*.5f*radius,radius,clock);
    }
    static void item(Painter p,int item,float x,float y,float r,float clock) {item(p,item,x,y,r,clock,.49f);}
    private static void item(Painter p,int item,float x,float y,float r,float clock,float footSpan) {
        if(item<0 || item>=Accessories.COUNT)return;
        int slot=Accessories.slot(item),v=Accessories.variant(item),c=Accessories.COLOR[item];
        int shade=Glyph.mix(c,0xFF3E3459,.36f),hi=Glyph.mix(c,0xFFFFFFFF,.5f);
        float detail=detail(p,r);Painter trim=detail>0?new OpacityPainter(p,detail):p;
        if(slot==Accessories.HAT)target(p,slot,x+r*(v>=3?AccessoryHats.left(v):-1.3f),y+r*(v>=3?AccessoryHats.top(v):-1.25f),x+r*(v>=3?AccessoryHats.right(v):1.3f),y+r*.35f);
        else if(slot==Accessories.GLASSES)target(p,slot,x-r,y-r*.46f,x+r,y+r*.46f);
        else if(slot==Accessories.BOOTS)target(p,slot,x-r*(footSpan+.56f),y+r*(v>=3?AccessoryFootwear.top(v):-.40f),x+r*(footSpan+.56f),y+r*.44f);
        else if(slot==Accessories.MASK)target(p,slot,x-r*1.25f,y-r*1.15f,x+r*1.25f,y+r*1.15f);
        if(slot==Accessories.FLAIR) {AccessoryFlair.draw(p,v,x,y,r*.70f,clock);return;}
        if(slot==Accessories.HAT) {
            if(v>=3){AccessoryHats.draw(p,v,x,y,r,clock,detail);return;}
            if(v==2) {
                AccessoryDetail.crown(p,x,y,r,clock,detail);
            } else {
                p.fillEllipse(x,y-r*.17f,r*.77f,r*.57f,shade);
                p.fillEllipse(x-r*.09f,y-r*.26f,r*.64f,r*.46f,c);
                if(detail>0)trim.fillEllipse(x-r*.22f,y-r*.42f,r*.32f,r*.17f,hi);
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
            if(v==3){googly(p,x,y,r,clock,detail);return;}
            AccessoryEyewear.draw(p,v,x,y,r,clock,detail,c);return;
        } else if(slot==Accessories.BOOTS) {
            for(int side=-1;side<=1;side+=2)shoe(p,v,x+side*r*footSpan,y+r*.025f*(float)Math.sin(clock*4+side),r,side,clock,c,shade,hi,detail);
        } else mask(p,v,x,y,r,detail);
        if(detail>0 && slot!=Accessories.BOOTS && !(slot==Accessories.HAT && v==2))AccessoryDetail.trim(trim,slot,v,x,y,r,clock);
    }
    /** Loose pupils wobble independently inside soft white domes; no physics allocation. */
    private static void googly(Painter p,float x,float y,float r,float clock,float detail) {
        p.line(x-r*.82f,y-r*.04f,x+r*.82f,y-r*.04f,0xFF8F8DB2,r*.075f);
        Painter trim=detail>0?new OpacityPainter(p,detail):p;
        for(int side=-1;side<=1;side+=2) {
            float cx=x+side*r*.39f,cy=y-r*.04f;
            p.fillEllipse(cx,cy+r*.035f,r*.39f,r*.375f,0xFF8F8DB2);
            p.fillEllipse(cx,cy,r*.365f,r*.35f,0xFFE3EDF8);
            p.fillEllipse(cx-r*.025f,cy-r*.035f,r*.32f,r*.30f,0xFFFFFFFF);
            float phase=clock*3.7f+side*.85f;
            float px=cx+r*(.105f*(float)Math.sin(phase)+.028f*(float)Math.sin(phase*2.3f));
            float py=cy+r*(.095f*(float)Math.cos(phase*.87f)+.025f*(float)Math.sin(phase*2.1f));
            p.fillCircle(px,py,r*.155f,0xFF302B45);
            p.fillCircle(px-r*.045f,py-r*.055f,r*.045f,0xFFFFFFFF);
            if(detail>0) {
                trim.arc(cx,cy,r*.325f,r*.31f,20,125,0xFFBDCEDF,r*.032f);
                trim.arc(cx-r*.025f,cy-r*.025f,r*.28f,r*.26f,210,95,0xAFFFFFFF,r*.05f);
                trim.fillEllipse(cx-r*.17f,cy-r*.20f,r*.066f,r*.035f,0xFFFFFFFF);
                trim.fillCircle(px+r*.058f,py+r*.052f,r*.022f,0xFFB9CDE5);
            }
        }
    }
    static void foot(Painter p,int item,float x,float y,float r,float clock,int side) {
        int c=Accessories.COLOR[item];
        shoe(p,Accessories.variant(item),x,y,r,side,clock,c,Glyph.mix(c,0xFF3E3459,.36f),Glyph.mix(c,0xFFFFFFFF,.5f),detail(p,r));
    }
    private static void shoe(Painter p,int v,float cx,float yy,float r,int side,float clock,int c,int shade,int hi,float detail) {
        if(v>=3){AccessoryFootwear.draw(p,v,cx,yy,r,side,clock,detail);return;}
        Painter trim=detail>0?new OpacityPainter(p,detail):p;
            if(v==0) {
                // Chunky foam clog: wide rounded toe, ventilation holes and a heel strap.
                p.fillEllipse(cx+side*r*.06f,yy+r*.10f,r*.39f,r*.22f,shade);
                p.fillEllipse(cx+side*r*.06f,yy+r*.035f,r*.39f,r*.22f,c);
                p.fillEllipse(cx-side*r*.08f,yy-r*.09f,r*.19f,r*.10f,shade);
                p.arc(cx-side*r*.04f,yy-r*.05f,r*.25f,r*.16f,190,165,hi,r*.065f);
                if(detail>0)for(int hole=0;hole<3;hole++)trim.fillEllipse(cx+side*r*(.08f+hole*.08f),yy+r*(.01f+(hole%2)*.035f),r*.027f,r*.022f,shade);
                if(detail>0)trim.arc(cx+side*r*.05f,yy+r*.06f,r*.31f,r*.09f,20,140,hi,r*.035f);
            } else if(v==1) {
                // Low-cut glitter shoes, with a contrasting cushioned sole and bow.
                p.fillEllipse(cx+side*r*.06f,yy+r*.12f,r*.36f,r*.17f,shade);
                p.fillEllipse(cx+side*r*.06f,yy+r*.055f,r*.36f,r*.20f,c);
                p.fillEllipse(cx-side*r*.09f,yy-r*.075f,r*.15f,r*.085f,shade);
                star(p,cx,yy+r*.055f,r*.06f,hi);
                if(detail>0)for(int i=0;i<5;i++) {
                    float xx=cx+r*(-.20f+i*.10f),sy=yy+r*(.035f+(i%2)*.06f);
                    float flash=.65f+.35f*(float)Math.sin(clock*4+i*1.7f+side);
                    star(trim,xx,sy,r*(.035f+.025f*flash),hi);
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
                if(detail>0)trim.fillEllipse(cx+side*r*.25f,yy+r*.015f,r*.11f,r*.04f,hi);
                p.fillCircle(cx-side*r*.18f,yy-r*.17f,r*.045f,0xFFFFE8A3);
            }
        if(detail>0)AccessoryDetail.footTrim(trim,v,cx,yy,r,side);
    }
    private static final class MaskFit extends OpacityPainter {
        final float cy,sy;
        MaskFit(Painter p,float y,float sy){super(p,1);cy=y;this.sy=sy;}
        float y(float y){return cy+(y-cy)*sy;}
        float[] points(float[] a){float[] b=a.clone();for(int i=1;i<b.length;i+=2)b[i]=y(b[i]);return b;}
        public void accessoryTarget(int slot,float[] a){p.accessoryTarget(slot,points(a));}
        public void fillPoly(float[] a,int c){p.fillPoly(points(a),c);}
        public void fillContours(float[][] a,int c){float[][] b=new float[a.length][];for(int i=0;i<a.length;i++)b[i]=points(a[i]);p.fillContours(b,c);}
        public void strokePoly(float[] a,int c,float w){p.strokePoly(points(a),c,w*sy);}
        public void polyline(float[] a,int c,float w){p.polyline(points(a),c,w*sy);}
        public void fillCircle(float x,float y,float r,int c){p.fillEllipse(x,y(y),r,r*sy,c);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){p.fillEllipse(x,y(y),rx,ry*sy,c);}
        public void arc(float x,float y,float rx,float ry,float a,float sweep,int c,float w){p.arc(x,y(y),rx,ry*sy,a,sweep,c,w*sy);}
        public void fillRect(float l,float t,float r,float b,int c){p.fillRect(l,y(t),r,y(b),c);}
        public void line(float x,float y,float xx,float yy,int c,float w){p.line(x,y(y),xx,y(yy),c,w*sy);}
    }
    private static float[] shoePoints(float[] a,float x,float y,float r,int side) {
        for(int i=0;i<a.length;i+=2){a[i]=x+a[i]*r*side;a[i+1]=y+a[i+1]*r;}return a;
    }
    private static float[] oval(float x,float y,float rx,float ry,int n) {
        float[] a=new float[n*2];for(int i=0;i<n;i++){float t=i*Softbody.TAU/n;
            a[i*2]=x+rx*(float)Math.cos(t);a[i*2+1]=y+ry*(float)Math.sin(t);}return a;
    }
    private static void mask(Painter p,int v,float x,float y,float r,float detail) {
        int n=detail>0?24:12;Painter trim=detail>0?new OpacityPainter(p,detail):p;
        if(v==0) {
            Trinket.ninjaMask(p,x,y,r*1.10f,1,detail>0);
        } else if(v==1) {
            float[] head=oval(x,y,r*.97f,r*.90f,n),left=oval(x-r*.35f,y-r*.01f,r*.26f,r*.30f,n),right=oval(x+r*.35f,y-r*.01f,r*.26f,r*.30f,n);
            p.fillContours(new float[][]{head,left,right},0xFFFFD667);
            if(detail>0)trim.arc(x-r*.07f,y-r*.22f,r*.72f,r*.53f,205,120,0xFFFFEDAD,r*.11f);
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
                if(detail>0)trim.arc(x+side*r*.53f,y+r*.27f,r*.21f,r*.13f,200,130,0xFFFFB1A0,r*.055f);
                p.line(x+side*r*.78f,y+r*.07f,x+side*r*.68f,y+r*.19f,0xFFFFD38D,r*.055f);
            }
            p.fillEllipse(x,y+r*.32f,r*.20f,r*.17f,0xFFF28C7C);
            p.fillEllipse(x,y+r*.56f,r*.36f,r*.15f,0xFF642F4C);
            for(int tooth=0;tooth<4;tooth++)p.fillRect(x+r*(-.27f+tooth*.14f),y+r*.47f,x+r*(-.16f+tooth*.14f),y+r*.57f,0xFFFFE5BD);
            p.arc(x,y+r*.61f,r*.39f,r*.14f,25,130,0xFFEB8A83,r*.055f);
        } else {
            float[] head=oval(x,y,r*.98f,r*1.02f,n);
            float[] left=heroEye(x-r*.35f,y,r,-1),right=heroEye(x+r*.35f,y,r,1);
            p.fillContours(new float[][]{head,left,right},0xFF56518C);
            if(detail>0)trim.arc(x,y-r*.14f,r*.84f,r*.78f,205,130,0xFF8177B4,r*.10f);
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
