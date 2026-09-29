package com.dddumpling.game;

/** Crystal toys react to each landing, nudge, and rainbow-making bounce. */
final class CrystalSkits extends Draw {
    private static final int[] COLORS={0xFFFFA8C5,0xFFFFDB9C,0xFFABEDCF,0xFF9DDDFF,0xFFD8B9FF};
    private CrystalSkits() {}

    private static float phase(float t,float start,float end) {
        return Math.max(0f,Math.min(1f,(t-start)/(end-start)));
    }
    private static float hop(float t,float start,float end) {
        return (float)Math.sin(phase(t,start,end)*Math.PI);
    }
    static void draw(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=2 || r<=0)return;
        t=Math.max(0f,Math.min(1f,t));
        if(variant==0)hopscotch(p,x,y,r,t,a);
        else if(variant==1)sneeze(p,x,y,r,t,a);
        else prism(p,x,y,r,t,a);
    }

    private static void sparkle(Painter p,float x,float y,float r,int color,int a) {
        p.fillPoly(new float[]{x,y-r,x+r*.24f,y-r*.24f,x+r,y,x+r*.24f,y+r*.24f,
                x,y+r,x-r*.24f,y+r*.24f,x-r,y,x-r*.24f,y-r*.24f},Glyph.withAlpha(color,a));
    }
    private static void burst(Painter p,float x,float y,float r,float age,int a) {
        if(age<=0f || age>=1f)return;
        for(int i=0;i<9;i++) {
            float side=(i-4)/4f;
            float lift=4f*age*(1f-age)*(.75f+.16f*(i%3));
            sparkle(p,x+side*r*1.75f*age,y-r*lift,r*(.075f+.02f*(i%2)),
                    COLORS[i%COLORS.length],(int)(a*(1f-age)));
        }
    }
    private static void ring(Painter p,float x,float y,float r,float age,int color,int a) {
        if(age<=0f || age>=1f)return;
        p.arc(x,y,r*(.25f+age*.8f),r*(.1f+age*.24f),0,360,
                Glyph.withAlpha(color,(int)(a*(1f-age))),r*.05f);
    }

    private static void hopscotch(Painter p,float x,float y,float r,float t,int a) {
        float base=y+r*.9f;
        for(int i=0;i<3;i++) {
            float px=x+(i-1)*r*1.1f,height=r*(.65f+i*.18f),hit=.18f+i*.26f;
            float glow=t<hit?0f:1f-phase(t,hit,hit+.22f);
            p.fillEllipse(px,base-height*.5f,r*.5f,height*.75f,
                    Glyph.withAlpha(COLORS[i],(int)(a*.32f*glow)));
            Lands.crystal(p,px,base,r*.34f,height,a,t*5f+i,false,true);
            ring(p,px,base-height,r,(t-hit)/.24f,COLORS[i],a);
            burst(p,px,base-height,r*.55f,(t-hit)/.25f,a);
        }
        float step,top,lift,squash;
        if(t<.18f) {
            float u=phase(t,0,.18f);step=-1.65f+u*.55f;top=.25f;
            lift=hop(t,0,.18f)*.65f;squash=1f-lift*.18f;
        } else if(t<.70f) {
            int leg=t<.44f?0:1;
            float start=.18f+leg*.26f,u=phase(t,start,start+.26f);
            step=-1.1f+(leg+u)*1.1f;top=.25f-(leg+u)*.18f;
            lift=(float)Math.sin(u*Math.PI)*.8f;squash=1f+.18f*(1f-u)-lift*.25f;
        } else {
            step=1.1f;top=-.11f;lift=0f;squash=1f+.25f*(1f-phase(t,.70f,.86f));
        }
        float radius=r*.39f;
        Skits.face(p,Kawaii.SQUISHY,x+r*step,y+r*(top-lift)-radius/squash,
                radius,a,squash,1f);
    }

    private static void sneeze(Painter p,float x,float y,float r,float t,int a) {
        float approach=phase(t,0,.28f),away=phase(t,.46f,.72f);
        float charge=hop(t,.28f,.46f);
        float wobble=(float)Math.sin(phase(t,.28f,.46f)*Math.PI*6f)*charge*.10f;
        float cx=x+r*(.55f+wobble),base=y+r*.78f;
        float height=r*(1.35f-charge*.17f);
        Lands.crystal(p,cx,base,r*(.46f+charge*.08f),height,a,t*5f,false,true);
        int ink=Glyph.withAlpha(INK,a);
        for(int side=-1;side<=1;side+=2)
            p.arc(cx+side*r*.16f,y+r*.05f,r*.075f,r*.06f,180,180,ink,r*.035f);
        p.fillEllipse(cx,y+r*.24f,r*.08f,r*(t>.46f?.12f:.045f),ink);
        float px=x+r*(-1.35f+approach*.99f-away*.78f);
        float jump=hop(t,.46f,.72f)*.65f;
        Skits.face(p,Kawaii.CAT,px,y+r*(.3f-jump),r*.45f,a,
                1f+charge*.15f-jump*.2f,t>.46f?1f:.3f);
        burst(p,cx,y-r*.38f,r*1.05f,(t-.46f)/.40f,a);
        ring(p,cx,y+r*.35f,r,(t-.46f)/.3f,COLORS[3],a);
    }

    private static void prism(Painter p,float x,float y,float r,float t,int a) {
        float glow=phase(t,.35f,.56f),settle=phase(t,.64f,.76f);
        float px=x-r*.55f,top=y+r*.13f;
        // Light fans out only when the dumpling touches the prism.
        for(int i=0;i<COLORS.length;i++) {
            float endX=x+r*(.05f+i*.39f),endY=y-r*(.95f-i*.18f);
            p.fillPoly(new float[]{px,top,endX-r*.17f,endY,endX+r*.17f,endY+r*.13f},
                    Glyph.withAlpha(COLORS[i],(int)(a*.50f*glow)));
        }
        Lands.crystal(p,px,y+r*.83f,r*.46f,r*.7f,a,t*5f,false,true);
        float u=phase(t,0,.35f),jump=hop(t,0,.35f)*.85f;
        float squash=1f+.24f*(1f-phase(t,.35f,.49f))*glow-jump*.18f;
        Skits.face(p,Kawaii.DUMPLING,px-r*.7f*(1f-u),top-r*.4f/squash-r*jump,
                r*.4f,a,squash,1f);
        float friendX=x+r*1.05f,friendY=y+r*(.23f-hop(t,.50f,.72f)*.4f);
        Skits.face(p,Kawaii.STRAWBERRY,friendX,friendY,r*.45f,a,1f,1f);
        ring(p,px,top,r,(t-.35f)/.3f,COLORS[2],a);
        burst(p,friendX,y-r*.35f,r*.7f,(t-.54f)/.44f,a);
        if(t>.60f) {
            int alpha=(int)(a*phase(t,.60f,.66f));
            float crownY=friendY-r*(.38f+(1f-settle)*.45f);
            for(int i=-1;i<=1;i++)
                Lands.crystal(p,friendX+i*r*.17f,crownY,r*.10f,r*(i==0?.34f:.23f),
                        alpha,t*5f+i,false,true);
        }
    }
}
