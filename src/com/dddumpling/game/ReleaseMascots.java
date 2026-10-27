package com.dddumpling.game;

/** Small deterministic reactions shared by every release's improvements and bug-fix cards. */
final class ReleaseMascots extends Draw {
    static boolean active(int icon) { return icon==ReleaseChange.MISC || icon==ReleaseChange.BUGS; }

    static void dumpling(Painter p,float x,float y,float r,float time,float reaction) {
        float t=Math.max(0,reaction),energy=reaction>=0?(float)Math.exp(-t*1.15f):0;
        float bounce=Math.abs((float)Math.sin(t*7.8f))*energy;
        float squash=1+.035f*(float)Math.sin(time*3.2f)
                +energy*.27f*(float)Math.cos(t*15.6f);
        float cy=y+r*(.16f-.06f*(float)Math.sin(time*3.2f)-bounce*.48f);
        p.fillEllipse(x,y+r*.85f,r*(.54f-bounce*.12f),r*.09f,0x30302544);
        Painter body=new RunCompanion.Squash(p,x,cy,squash);
        float br=r*.76f;
        Kawaii.dumplingBody(body,x,cy,br,br,0xFFFFCF61);
        Finish.draw(body,Collect.GLITTER,x,cy,br,0xFFFFF1B2,time,1,212);
        Trinket.reactionFace(body,x,cy+br*.04f,br*.64f,time*2,1,energy>.09f?8:1,0);
        for(int side=-1;side<=1;side+=2)
            body.fillEllipse(x+side*br*.39f,cy+br*.23f,br*.14f,br*.095f,0xB3F5859D);
        for(int i=0;i<3;i++) {
            float a=i*2.3f+time*.24f,twinkle=.55f+.45f*(float)Math.sin(time*4+i*2);
            float sx=x+(float)Math.cos(a)*r*.92f,sy=y+(float)Math.sin(a)*r*.78f;
            float sr=r*(.065f+.045f*twinkle);
            p.fillPoly(star(sx,sy,sr,sr*.28f,4,time*.2f),Glyph.withAlpha(GOLD,(int)(120+110*twinkle)));
        }
        if(reaction<0)return;
        for(int i=0;i<20;i++) {
            float age=t-(i%4)*.09f,u=age/1.35f;
            if(u<0 || u>=1)continue;
            float a=i*2.39996f,d=r*(.35f+u*(.65f+(i%3)*.13f));
            float sx=x+(float)Math.cos(a)*d,sy=y-r*.12f+(float)Math.sin(a)*d*.74f+r*u*u*.28f;
            float sr=r*(.055f+.02f*(i%3))*(1-u*.5f);
            int color=Glyph.withAlpha(i%2==0?0xFFFFECA1:0xFFFFC044,(int)(240*(1-u)));
            p.fillPoly(star(sx,sy,sr,sr*.30f,4,a+u*3),color);
        }
    }
    static void bug(Painter p,float x,float y,float r,float time,float reaction) {
        float squash=1,jump=0;
        if(reaction>=0 && reaction<.28f) {
            float press=(float)Math.sin(Math.PI*Math.min(1,reaction/.28f));
            squash+=1.45f*press;
        } else if(reaction>=.28f && reaction<1.5f) {
            float t=reaction-.28f,energy=(float)Math.exp(-t*2.6f)*Math.max(0,1-t/1.22f);
            jump=Math.abs((float)Math.sin(t*7.5f))*energy*.75f;
            squash=1-.27f*energy*(float)Math.sin(t*15);
        }
        float roam=reaction>=0 && reaction<1.5f?1-.75f*(float)Math.sin(Math.PI*reaction/1.5f):1;
        float cx=x+r*.17f*(float)Math.sin(time*2.3f)*roam;
        float cy=y+r*.07f*(float)Math.sin(time*4.6f)*roam-r*jump;
        p.fillEllipse(cx,y+r*.79f,r*(.48f-jump*.12f),r*.085f,0x35302544);
        Painter legs=new RunCompanion.Squash(p,cx,cy+r*.64f,squash);
        int ink=0xFF574563;
        for(int side=-1;side<=1;side+=2) {
            for(int leg=0;leg<3;leg++) {
                float wiggle=(float)Math.sin(time*15+leg*2+side)*r*.12f;
                float yy=cy+r*(leg*.27f-.12f);
                legs.polyline(new float[]{cx+side*r*.46f,yy,cx+side*r*.70f,yy-r*.10f+wiggle,
                        cx+side*r*.84f,yy+r*.11f+wiggle},ink,r*.075f);
            }
            float antenna=(float)Math.sin(time*5+side)*r*.08f;
            legs.polyline(new float[]{cx+side*r*.23f,cy-r*.53f,cx+side*r*.31f,cy-r*.76f,
                    cx+side*r*.43f+antenna,cy-r*.83f},ink,r*.06f);
            legs.fillCircle(cx+side*r*.43f+antenna,cy-r*.83f,r*.09f,0xFFFFB6C6);
        }
        legs.fillEllipse(cx,cy+r*.18f,r*.65f,r*.64f,0xFFC36886);
        legs.fillEllipse(cx-r*.035f,cy+r*.12f,r*.60f,r*.57f,0xFFF093A8);
        legs.arc(cx,cy+r*.18f,r*.08f,r*.56f,90,180,0xFFBD6581,r*.05f);
        legs.fillEllipse(cx-r*.27f,cy-r*.08f,r*.15f,r*.22f,0x70FFE1D4);
        for(int side=-1;side<=1;side+=2) {
            legs.fillCircle(cx+side*r*.32f,cy+r*.16f,r*.115f,ink);
            legs.fillCircle(cx+side*r*.26f,cy+r*.49f,r*.08f,ink);
        }
        legs.fillEllipse(cx,cy-r*.36f,r*.49f,r*.36f,0xFFC5ABD9);
        for(int side=-1;side<=1;side+=2) {
            float ex=cx+side*r*.17f;
            if(reaction>=0 && reaction<.28f)
                legs.arc(ex,cy-r*.39f,r*.10f,r*.07f,190,160,ink,r*.04f);
            else legs.fillCircle(ex,cy-r*.40f,r*.067f,ink);
            legs.fillEllipse(cx+side*r*.30f,cy-r*.27f,r*.08f,r*.04f,0xFFFFB5B9);
        }
        legs.arc(cx,cy-r*.30f,r*.10f,r*.08f,0,180,ink,r*.035f);
    }
}
