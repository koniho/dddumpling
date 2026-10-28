package com.dddumpling.game;

/** Close-up ornament fades away before it becomes subpixel work in gameplay. */
final class AccessoryDetail {
    private static float[] points(float x,float y,float r,float... a) {
        for(int i=0;i<a.length;i+=2){a[i]=x+a[i]*r;a[i+1]=y+a[i+1]*r;}return a;
    }
    static void crown(Painter p,float x,float y,float r,float clock,float detail) {
        int gold=0xFFF1BC48,shadow=0xFFA56A29,light=0xFFFFEAA0;
        p.fillEllipse(x,y-r*.29f,r*.78f,r*.48f,0xFF763D69);
        p.fillPoly(points(x,y,r,-.94f,.12f,-1,-.76f,-.63f,-.43f,-.51f,-1.02f,
                -.23f,-.54f,0,-1.16f,.23f,-.54f,.51f,-1.02f,.63f,-.43f,1,-.76f,.94f,.12f),shadow);
        p.fillPoly(points(x,y,r,-.87f,.04f,-.93f,-.64f,-.62f,-.32f,-.50f,-.89f,
                -.23f,-.40f,0,-1.04f,.23f,-.40f,.50f,-.89f,.62f,-.32f,.93f,-.64f,.87f,.04f),gold);
        p.fillEllipse(x,y+r*.06f,r*.96f,r*.22f,shadow);
        p.fillEllipse(x,y,r*.94f,r*.17f,gold);
        for(int i=-2;i<=2;i++) {
            float xx=x+r*(i==0?0:i==1?.51f:i==-1?-.51f:i*.5f);
            float yy=y-r*(i==0?1.14f:Math.abs(i)==1?1.01f:.75f);
            p.fillCircle(xx,yy,r*.075f,light);
        }
        gem(p,x,y-r*.19f,r*.18f,0xFFB6436C,0xFFF38AA7);
        if(detail<=0)return;
        Painter q=new OpacityPainter(p,detail);
        q.arc(x,y-r*.02f,r*.86f,r*.105f,8,164,light,r*.045f);
        q.arc(x,y+r*.07f,r*.86f,r*.115f,12,156,0xFFCE8D30,r*.035f);
        q.fillEllipse(x-r*.19f,y-r*.39f,r*.11f,r*.17f,0x38FFFFFF);
        for(int side=-1;side<=1;side+=2) {
            gem(q,x+side*r*.57f,y-r*.17f,r*.105f,0xFF348D87,0xFF91F0CD);
            q.line(x+side*r*.49f,y-r*.81f,x+side*r*.39f,y-r*.42f,light,r*.035f);
            q.line(x+side*r*.84f,y-r*.55f,x+side*r*.78f,y-r*.22f,light,r*.035f);
            for(int bead=0;bead<3;bead++)q.fillCircle(x+side*r*(.30f+bead*.22f),y+r*.09f,r*.028f,light);
        }
        q.line(x,y-r*.95f,x-r*.085f,y-r*.58f,light,r*.04f);
        AccessoryArt.star(q,x,y-r*1.13f,r*.105f,0xFFFFF3BC);
        float pulse=.5f+.5f*(float)Math.sin(clock*2.2f);
        AccessoryArt.star(q,x-r*.56f,y-r*.22f,r*(.025f+.035f*pulse),0xFFFFFFFF);
    }
    private static void gem(Painter p,float x,float y,float r,int dark,int bright) {
        p.fillEllipse(x,y,r*1.18f,r*1.30f,0xFFFFE5A1);
        p.fillPoly(points(x,y,r,0,-1.1f,.78f,-.35f,.62f,.65f,0,1.04f,-.62f,.65f,-.78f,-.35f),dark);
        p.fillPoly(points(x,y,r,0,-1.1f,.20f,-.20f,-.62f,.65f,-.78f,-.35f),bright);
    }
    static void trim(Painter p,int slot,int v,float x,float y,float r,float clock) {
        if(slot==Accessories.HAT) {
            p.arc(x-r*.05f,y-r*.20f,r*.53f,r*.44f,210,100,0x65FFFFFF,r*.028f);
            for(int i=-3;i<=3;i++)p.line(x+r*i*.20f,y+r*.04f,x+r*(i*.20f+.06f),y+r*.055f,0x80685140,r*.018f);
            if(v==0)gem(p,x+r*.46f,y-r*.06f,r*.08f,0xFF627A66,0xFFC3E7B2);
            else {p.line(x+r*.12f,y-r*.94f,x+r*.40f,y-r*1.10f,0xFFB6EAAA,r*.025f);
                p.line(x-r*.02f,y-r*.95f,x-r*.26f,y-r*1.05f,0xFF78B588,r*.025f);}
        } else if(slot==Accessories.GLASSES) {
            for(int s=-1;s<=1;s+=2) {
                p.fillCircle(x+s*r*.78f,y-r*.025f,r*.036f,0xFFFFE1A4);
                p.line(x+s*r*.35f-r*.13f,y-r*.045f,x+s*r*.35f-r*.04f,y+r*.065f,0x63FFFFFF,r*.025f);
                p.arc(x+s*r*.35f,y-r*.02f,r*.21f,r*.16f,210,80,0x69B7EFF7,r*.025f);
            }
        } else if(slot==Accessories.BOOTS) {
            for(int s=-1;s<=1;s+=2) {
                float cx=x+s*r*.49f,yy=y+r*.025f*(float)Math.sin(clock*4+s);
                if(v==0){p.fillCircle(cx-s*r*.18f,yy-r*.035f,r*.045f,0xFFFFE6AD);p.fillCircle(cx-s*r*.18f,yy-r*.035f,r*.021f,0xFF9DBED4);}
                else if(v==1)p.arc(cx+s*r*.06f,yy+r*.08f,r*.30f,r*.13f,5,160,0x9FFFFFFF,r*.025f);
                else gem(p,cx-s*r*.18f,yy-r*.17f,r*.04f,0xFFA05A9B,0xFFFFC5EB);
            }
        } else if(slot==Accessories.MASK) {
            if(v==0){p.arc(x,y+r*.05f,r*.93f,r*.95f,218,105,0xFF42394F,r*.05f);
                p.line(x+r*1.04f,y-r*.47f,x+r*1.32f,y-r*.68f,0xFF615370,r*.035f);}
            else if(v==1){p.fillEllipse(x-r*.18f,y+r*.25f,r*.27f,r*.055f,0xAFFFF0AF);
                p.arc(x,y+r*.43f,r*.51f,r*.14f,15,145,0xFFECA04B,r*.035f);}
            else if(v==2)for(int s=-1;s<=1;s+=2){p.fillCircle(x+s*r*.72f,y+r*.34f,r*.038f,0xFFFFDB91);
                p.arc(x+s*r*.53f,y+r*.40f,r*.17f,r*.12f,15,140,0xFFAB415F,r*.03f);}
            else for(int s=-1;s<=1;s+=2)p.arc(x+s*r*.37f,y+r*.12f,r*.39f,r*.70f,30,80,0xFF7169A6,r*.03f);
        }
    }
}
