package com.dddumpling.game;

/** Little goo playgrounds, paced to the existing stage-intro beat. */
final class SlimeSkits extends Draw {
    private static final int GOO=0xFF85D6A2, LIGHT=0xFFD2F7B2, SHADE=0xFF57A990;
    private SlimeSkits() {}

    private static float phase(float t,float start,float end) {
        return Math.max(0f,Math.min(1f,(t-start)/(end-start)));
    }
    private static float hop(float t,float start,float end) {
        return (float)Math.sin(phase(t,start,end)*Math.PI);
    }
    private static float impact(float t,float at) {
        if(t<at)return 0f;
        float v=1f-phase(t,at,at+.16f);return v*v;
    }
    static void draw(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=0 || r<=0)return;
        t=Math.max(0f,Math.min(1f,t));
        if(variant==0)puddleHop(p,x,y,r,t,a);
        else if(variant==1)trampoline(p,x,y,r,t,a);
        else bubblePop(p,x,y,r,t,a);
    }

    private static void puddle(Painter p,float x,float y,float r,float t,float squash,int a) {
        p.fillEllipse(x,y+r*.12f,r*1.95f,r*.24f,Glyph.withAlpha(SHADE,a/3));
        float[] rim=new float[48];
        for(int i=0;i<24;i++) {
            float angle=i*Softbody.TAU/24f;
            float wobble=1f+.045f*(float)Math.sin(angle*5f+t*18f);
            rim[i*2]=x+(float)Math.cos(angle)*r*(1.8f+squash*.16f)*wobble;
            rim[i*2+1]=y+(float)Math.sin(angle)*r*.24f*wobble;
        }
        p.fillPoly(rim,Glyph.withAlpha(GOO,a));
        p.fillEllipse(x-r*.5f,y-r*.055f,r*.63f,r*.065f,Glyph.withAlpha(LIGHT,a*2/3));
        p.arc(x+r*.28f,y,r*(.3f+squash*.75f),r*.105f,5,170,
                Glyph.withAlpha(LIGHT,a*3/4),r*.045f);
    }

    /** The spray spreads and falls back into the puddle, without fresh randomness. */
    private static void splash(Painter p,float x,float y,float r,float age,int a) {
        if(age<=0f || age>=1f)return;
        int alpha=(int)(a*(1f-age));
        for(int drop=0;drop<9;drop++) {
            float side=(drop-4)/4f;
            float dx=side*r*1.9f*age;
            float lift=4f*age*(1f-age)*(1.05f-.35f*Math.abs(side)+.09f*(drop%3));
            float size=r*(.09f+.018f*(drop%3))*(1f-age*.45f);
            float dy=y-r*lift;
            p.fillEllipse(x+dx,dy,size,size*(1.35f-age*.5f),
                    Glyph.withAlpha(drop%2==0?LIGHT:GOO,alpha));
            p.fillCircle(x+dx-size*.25f,dy-size*.35f,size*.25f,Glyph.withAlpha(0xFFFFFFFF,alpha*2/3));
        }
        p.arc(x,y,r*(.3f+age*1.2f),r*(.05f+age*.13f),0,180,
                Glyph.withAlpha(LIGHT,alpha),r*.045f);
    }

    /** A strawberry discovers that a puddle makes an excellent jumping game. */
    private static void puddleHop(Painter p,float x,float y,float r,float t,int a) {
        float hit=impact(t,.33f)+impact(t,.72f);
        puddle(p,x,y+r*.62f,r,t,hit,a);
        float u=phase(t,0,.33f),v=phase(t,.33f,.72f);
        float px=x+r*(-1.45f+u*1.45f+v*1.2f);
        float lift=hop(t,0,.33f)*1.05f+hop(t,.33f,.72f)*1.45f;
        float squash=1f+hit*.3f-lift*.12f;
        Skits.face(p,Kawaii.STRAWBERRY,px,y+r*(.13f-lift+hit*.10f),r*.48f,a,squash,1f);
        splash(p,x,y+r*.53f,r,(t-.33f)/.34f,a);
        splash(p,x+r*1.2f,y+r*.53f,r*.8f,(t-.72f)/.28f,a);
        // The puddle grins back after the first splash.
        if(t>.33f) {
            p.arc(x-r*.35f,y+r*.68f,r*.07f,r*.06f,180,180,Glyph.withAlpha(INK,a),r*.025f);
            p.arc(x+r*.12f,y+r*.68f,r*.07f,r*.06f,180,180,Glyph.withAlpha(INK,a),r*.025f);
            p.arc(x-r*.1f,y+r*.71f,r*.10f,r*.065f,0,180,Glyph.withAlpha(INK,a),r*.025f);
        }
    }

    /** A willing slime squashes down, springs up, and catches its dumpling friend. */
    private static void trampoline(Painter p,float x,float y,float r,float t,int a) {
        float press=hop(t,.05f,.32f),landing=impact(t,.78f);
        float spring=hop(t,.32f,.48f);
        puddle(p,x,y+r*.83f,r*.78f,t,press+landing,a);
        float squash=1.18f+press*.6f+landing*.55f-spring*.3f;
        float base=y+r*.75f,bodyR=r*.64f;
        Skits.face(p,Kawaii.SQUISHY,x,base-bodyR/squash,bodyR,a,squash,1f);
        float leap=hop(t,.32f,.78f);
        float py=y+r*(-.48f+press*.27f-leap*1.25f+landing*.18f);
        Skits.face(p,Kawaii.DUMPLING,x+r*.2f*(float)Math.sin(t*Softbody.TAU),py,
                r*.43f,a,1f+press*.2f+landing*.25f-leap*.12f,1f);
        splash(p,x,base-r*.12f,r*.8f,(t-.32f)/.28f,a);
        splash(p,x,base-r*.12f,r,(t-.78f)/.22f,a);
    }

    /** Two friends squeeze a goo bubble until it pops into tiny slime hats. */
    private static void bubblePop(Painter p,float x,float y,float r,float t,int a) {
        float grow=phase(t,0,.55f),pop=phase(t,.55f,.9f);
        puddle(p,x,y+r*.65f,r,t,impact(t,.55f),a);
        for(int side=-1;side<=1;side+=2) {
            float jump=hop(t,.55f+(side+1)*.025f,.86f+(side+1)*.025f)*.48f;
            float px=x+side*r*(1.25f-grow*.3f+pop*.3f),py=y+r*(.12f-jump);
            Skits.face(p,side<0?Kawaii.CAT:Kawaii.STRAWBERRY,px,py,r*.47f,a,
                    1f-jump*.2f+impact(t,.88f)*.15f,t>.55f?1f:.55f);
            if(t>.66f) {
                float hat=phase(t,.66f,.74f);
                p.fillEllipse(px,py-r*.38f,r*.32f*hat,r*.10f*hat,Glyph.withAlpha(GOO,a));
                p.fillEllipse(px-side*r*.12f,py-r*.3f,r*.07f*hat,r*.12f*hat,Glyph.withAlpha(GOO,a));
            }
        }
        float bubbleY=y-r*.3f;
        if(t<.55f) {
            float radius=r*(.12f+grow*.67f);
            p.fillCircle(x,bubbleY,radius,Glyph.withAlpha(GOO,a/3));
            p.strokeCircle(x,bubbleY,radius,Glyph.withAlpha(LIGHT,a),r*.055f);
            p.arc(x,bubbleY,radius*.72f,radius*.72f,205,75,Glyph.withAlpha(0xFFFFFFFF,a*3/4),r*.055f);
        } else {
            p.strokeCircle(x,bubbleY,r*(.8f+pop*.55f),Glyph.withAlpha(LIGHT,(int)(a*(1f-pop))),r*.035f);
            splash(p,x,bubbleY,r*1.25f,(t-.55f)/.45f,a);
        }
    }
}
