package com.dddumpling.game;

/** Five bioluminescent reef silhouettes, with local light rather than full-screen blur. */
final class UnderwaterCoral extends Draw {
    private static final int[] COLORS={0xFF5BCFEF,0xFFFFAD70,0xFFB29AF5,0xFF78F1BC,0xFFF18EBC};
    private UnderwaterCoral() {}
    private static int ink(int color,float alpha) {return Glyph.withAlpha(color,(int)alpha);}
    static void draw(Painter p,float x,float y,float r,float t,int kind,float fade) {
        int color=COLORS[kind];float pulse=.82f+.18f*(float)Math.sin(t*1.5f+kind);
        // Light pools spill onto the ledge; the body casts a long, cool shadow below it.
        p.fillPoly(new float[]{x-r*.65f,y-r*.10f,x+r*.55f,y-r*.16f,x+r*1.65f,y+r*.95f,
                x+r*.25f,y+r*.62f,x-r*.65f,y+r*.18f},ink(0xFF010812,fade*90));
        for(int layer=2;layer>=0;layer--)p.fillEllipse(x,y-r*.32f,r*(1.05f+layer*.3f),r*(.68f+layer*.21f),
                ink(color,fade*pulse*(layer==0?13:5)));
        if(kind==0)fan(p,x,y,r,t,color,fade);
        else if(kind==1)tubes(p,x,y,r,color,fade);
        else if(kind==2)plates(p,x,y,r,color,fade);
        else if(kind==3)anemone(p,x,y,r,t,color,fade);
        else lobes(p,x,y,r,t,color,fade);
    }
    private static void fan(Painter p,float x,float y,float r,float t,int col,float a) {
        for(int i=0;i<5;i++) {
            float dx=(i-2)*r*.39f,dy=-r*(.75f+.35f*(float)Math.cos(i-2)),sway=r*.04f*(float)Math.sin(t+i);
            float[] path={x,y,x+dx*.32f,y+dy*.44f,x+dx*.70f+sway,y+dy*.73f,x+dx+sway,y+dy};
            p.polyline(path,ink(Glyph.mix(col,0xFF14253F,.65f),a*230),r*.11f);
            p.polyline(path,ink(col,a*190),r*.035f);
            p.polyline(new float[]{path[4],path[5],path[4]+r*.16f,path[5]-r*.24f},ink(col,a*155),r*.028f);
            p.fillCircle(path[6],path[7],r*.047f,ink(0xFFDAFBFF,a*235));
        }
    }
    private static void tubes(Painter p,float x,float y,float r,int col,float a) {
        for(int i=0;i<4;i++) {
            float xx=x+(i-1.5f)*r*.33f,h=r*(.42f+hash(192+i)*.72f),w=r*(.12f+i%2*.025f);
            p.fillPoly(new float[]{xx-w,y,xx-w*.8f,y-h,xx+w*.8f,y-h,xx+w,y},ink(Glyph.mix(col,0xFF432143,.55f),a*240));
            p.fillPoly(new float[]{xx-w*.48f,y,xx-w*.55f,y-h,xx+w*.1f,y-h,xx+w*.3f,y},ink(col,a*175));
            p.fillEllipse(xx,y-h,w,w*.50f,ink(col,a*240));
            p.fillEllipse(xx,y-h,w*.57f,w*.26f,ink(0xFF2C1B35,a*250));
            p.arc(xx,y-h,w*.85f,w*.40f,175,175,ink(0xFFFFE5B8,a*220),r*.025f);
        }
    }
    private static void plates(Painter p,float x,float y,float r,int col,float a) {
        float[] rim=new float[32];
        for(int level=0;level<3;level++) {
            float yy=y-r*(.18f+level*.31f),rx=r*(.85f-level*.18f);
            for(int j=0;j<16;j++) {
                float angle=j*Softbody.TAU/16,wave=1+.07f*(float)Math.sin(j*2.3f+level);
                rim[j*2]=x+(float)Math.cos(angle)*rx*wave;
                rim[j*2+1]=yy+(float)Math.sin(angle)*rx*.25f*wave;
            }
            p.fillPoly(new float[]{x-r*.22f,y,x-r*.12f,yy,x+r*.12f,yy,x+r*.25f,y},ink(0xFF463750,a*220));
            p.fillPoly(rim,ink(Glyph.mix(col,0xFF2A2445,.37f),a*240));
            p.strokePoly(rim,ink(col,a*205),r*.035f);
            p.arc(x,yy,rx*.65f,rx*.12f,180,160,ink(0xFFE9DFFF,a*160),r*.022f);
        }
    }
    private static void anemone(Painter p,float x,float y,float r,float t,int col,float a) {
        p.fillEllipse(x,y,r*.65f,r*.19f,ink(0xFF21484C,a*230));
        for(int i=0;i<7;i++) {
            float dx=(i-3)*r*.21f,h=r*(.45f+hash(i+671)*.45f),sway=r*.15f*(float)Math.sin(t*1.7f+i*.9f);
            float[] tentacle={x+dx*.5f,y,x+dx*.75f-sway*.4f,y-h*.45f,x+dx+sway,y-h};
            p.polyline(tentacle,ink(Glyph.mix(col,0xFF183B48,.55f),a*230),r*.08f);
            p.polyline(tentacle,ink(col,a*175),r*.026f);
            p.fillCircle(x+dx+sway,y-h,r*.065f,ink(0xFFD3FFE0,a*230));
        }
    }
    private static void lobes(Painter p,float x,float y,float r,float t,int col,float a) {
        for(int i=0;i<3;i++) {
            float xx=x+(i-1)*r*.43f,yy=y-r*(i==1?.44f:.24f),rr=r*(i==1?.48f:.35f);
            p.fillEllipse(xx,yy,rr,rr*.8f,ink(Glyph.mix(col,0xFF39213F,.58f),a*240));
            p.fillEllipse(xx-rr*.13f,yy-rr*.22f,rr*.74f,rr*.48f,ink(col,a*120));
            float[] fold=new float[18];
            for(int j=0;j<9;j++) {
                float u=j/8f;
                fold[j*2]=xx+(u-.5f)*rr*1.5f;
                fold[j*2+1]=yy-rr*.18f+rr*.22f*(float)Math.sin(u*17+i);
            }
            p.polyline(fold,ink(0xFFFFD4EB,a*200),r*.035f);
        }
    }
}
