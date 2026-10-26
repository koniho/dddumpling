package com.dddumpling.game;

/** Shaded, irregular colonies. Bioluminescence stays inside the reef's subdued palette. */
final class UnderwaterCoral extends Draw {
    private static final int[] COLORS={0xFF396C7A,0xFF825C50,0xFF625878,0xFF426D60,0xFF79556C};
    private UnderwaterCoral() {}
    private static int ink(int color,float alpha) {return Glyph.withAlpha(color,(int)alpha);}
    private static int shade(int color) {return Glyph.mix(color,0xFF111A2B,.58f);}
    private static int light(int color) {return Glyph.mix(color,0xFF93A6B0,.23f);}
    static void draw(Painter p,float x,float y,float r,float t,int kind,float fade) {
        draw(p,x,y,r,t,kind,fade,137+kind*71);
    }
    static void draw(Painter p,float x,float y,float r,float t,int kind,float fade,int seed) {
        int color=COLORS[kind];float pulse=.8f+.2f*(float)Math.sin(t*.7f+kind);
        // Uneven, nested contact shadows keep colonies attached to their rock ledges.
        for(int layer=2;layer>=0;layer--)p.fillEllipse(x+r*.12f,y+r*.08f,r*(.65f+layer*.13f),r*(.13f+layer*.045f),ink(0xFF030916,fade*(layer==0?95:26)));
        p.fillEllipse(x,y-r*.30f,r*1.10f,r*.77f,ink(color,fade*pulse*8));
        if(kind==0)fan(p,x,y,r,t,color,fade,seed);
        else if(kind==1)tubes(p,x,y,r,color,fade,seed);
        else if(kind==2)plates(p,x,y,r,color,fade,seed);
        else if(kind==3)anemone(p,x,y,r,t,color,fade,seed);
        else lobes(p,x,y,r,color,fade,seed);
    }
    private static void fan(Painter p,float x,float y,float r,float t,int col,float a,int seed) {
        for(int i=0;i<5;i++) {
            float dx=(i-2)*r*(.27f+hash(seed+i)*.18f),dy=-r*(.48f+hash(seed+i+10)*.65f);
            float sway=r*.035f*(float)Math.sin(t*.8f+i),base=x+(hash(seed+i+3)-.5f)*r*.17f;
            float[] path={base,y,base+dx*.26f,y+dy*.38f,base+dx*.64f+sway,y+dy*.72f,base+dx+sway,y+dy};
            p.polyline(path,ink(shade(col),a*235),r*.115f);
            for(int j=0;j<path.length;j+=2){path[j]-=r*.018f;path[j+1]-=r*.012f;}
            p.polyline(path,ink(col,a*210),r*.063f);
            float[] twig={path[4],path[5],path[4]+r*(.09f+hash(seed+i+5)*.12f),path[5]-r*.22f};
            p.polyline(twig,ink(col,a*190),r*.042f);
            p.line(path[2],path[3],path[4],path[5],ink(light(col),a*115),r*.022f);
        }
    }
    private static void tubes(Painter p,float x,float y,float r,int col,float a,int seed) {
        float[] body=new float[36],ridge=new float[36];
        for(int i=0;i<4;i++) {
            float xx=x+(i-1.5f)*r*.31f,base=y+r*(hash(seed+i+8)-.5f)*.19f;
            float h=r*(.42f+hash(seed+i)*.75f),w=r*(.12f+hash(seed+i+2)*.055f),lean=r*(hash(seed+i+5)-.5f)*.30f;
            for(int j=0;j<9;j++) {
                float u=j/8f,centre=xx+lean*u*u,yy=base-u*h;
                float width=w*(.76f+.17f*(float)Math.sin(u*8+seed+i)+.12f*(1-u));
                body[j*2]=centre-width;body[j*2+1]=yy;
                body[(17-j)*2]=centre+width;body[(17-j)*2+1]=yy;
                ridge[j*2]=centre-width*.54f;ridge[j*2+1]=yy;
                ridge[(17-j)*2]=centre+width*.03f;ridge[(17-j)*2+1]=yy;
            }
            p.fillPoly(body,ink(shade(col),a*245));
            p.fillPoly(ridge,ink(col,a*210));
            p.fillEllipse(xx+lean,base-h,w*.85f,w*.43f,ink(col,a*230));
            p.fillEllipse(xx+lean+w*.08f,base-h+w*.04f,w*.60f,w*.29f,ink(0xFF101522,a*245));
            p.arc(xx+lean,base-h,w*.78f,w*.37f,190,140,ink(light(col),a*140),r*.027f);
        }
    }
    private static void plates(Painter p,float x,float y,float r,int col,float a,int seed) {
        float[] rim=new float[40],top=new float[40];
        for(int level=0;level<3;level++) {
            float xx=x+r*(hash(seed+level)-.5f)*.32f,yy=y-r*(.12f+level*.25f);
            float rx=r*(.83f-level*.16f)*( .86f+hash(seed+level+9)*.24f);
            for(int j=0;j<20;j++) {
                float angle=j*Softbody.TAU/20,wave=1+.12f*(float)Math.sin(j*1.9f+seed)+.06f*(float)Math.cos(j*3.2f+level);
                rim[j*2]=xx+(float)Math.cos(angle)*rx*wave;
                rim[j*2+1]=yy+(float)Math.sin(angle)*rx*.31f*wave;
                top[j*2]=xx+(rim[j*2]-xx)*.92f-r*.025f;
                top[j*2+1]=yy+(rim[j*2+1]-yy)*.75f-r*.065f;
            }
            p.fillEllipse(xx,yy+r*.15f,rx*.52f,r*.18f,ink(shade(col),a*235));
            p.fillPoly(rim,ink(shade(col),a*245));
            p.fillPoly(top,ink(col,a*225));
            p.arc(xx-r*.02f,yy-r*.07f,rx*.64f,rx*.16f,190,120,ink(light(col),a*105),r*.025f);
        }
    }
    private static void anemone(Painter p,float x,float y,float r,float t,int col,float a,int seed) {
        p.fillEllipse(x,y-r*.07f,r*.51f,r*.25f,ink(shade(col),a*235));
        float[] strand=new float[12];
        for(int i=0;i<7;i++) {
            float dx=(i-3)*r*.18f,h=r*(.36f+hash(seed+i)*.53f),base=y+r*(hash(seed+i+7)-.5f)*.14f;
            for(int j=0;j<6;j++) {
                float u=j/5f;
                strand[j*2]=x+dx*(.35f+u*.65f)+r*.12f*u*u*(float)Math.sin(t*1.2f+i-u*3);
                strand[j*2+1]=base-u*h;
            }
            p.polyline(strand,ink(shade(col),a*235),r*.10f);
            for(int j=0;j<6;j++)strand[j*2]-=r*.02f;
            p.polyline(strand,ink(col,a*200),r*.055f);
            p.fillEllipse(strand[10],strand[11],r*.035f,r*.052f,ink(light(col),a*145));
        }
    }
    private static void lobes(Painter p,float x,float y,float r,int col,float a,int seed) {
        float[] shape=new float[40],fold=new float[26];
        for(int i=0;i<4;i++) {
            float xx=x+(i-1.5f)*r*.31f,yy=y-r*(.18f+hash(seed+i)*.29f),rr=r*(.28f+hash(seed+i+3)*.19f);
            for(int layer=0;layer<3;layer++) {
                for(int j=0;j<20;j++) {
                    float angle=j*Softbody.TAU/20,edge=1+.07f*(float)Math.sin(j*2.1f+seed+i);
                    shape[j*2]=xx-rr*layer*.08f+(float)Math.cos(angle)*rr*(1-layer*.20f)*edge;
                    shape[j*2+1]=yy-rr*layer*.13f+(float)Math.sin(angle)*rr*(.80f-layer*.19f)*edge;
                }
                p.fillPoly(shape,ink(layer==0?shade(col):layer==1?col:light(col),a*(layer==2?70:230)));
            }
            for(int j=0;j<13;j++) {
                float u=j/12f;
                fold[j*2]=xx+(u-.5f)*rr*1.4f;
                fold[j*2+1]=yy-rr*.10f+rr*.19f*(float)Math.sin(u*15+i);
            }
            p.polyline(fold,ink(shade(col),a*160),r*.045f);
            for(int j=0;j<13;j++)fold[j*2+1]-=r*.023f;
            p.polyline(fold,ink(light(col),a*110),r*.02f);
        }
    }
}
