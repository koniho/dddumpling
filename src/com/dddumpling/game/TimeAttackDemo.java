package com.dddumpling.game;

/** Playful title sparring, driven entirely by the display clock. */
final class TimeAttackDemo extends Draw {
    private TimeAttackDemo() {}
    static final float LOOP=3.2f;
    private static float hop(float t,float start,float end) {
        return t<=start || t>=end?0f:(float)Math.sin((t-start)/(end-start)*Math.PI);
    }
    private static float radius(Layout L) {return Math.min(L.unit,L.h*.017f);}
    private static float companionX(GameCore c,Layout L) {
        float t=c.clock%LOOP;
        return L.w*.16f+radius(L)*(.35f*hop(t,1f,1.8f)-.25f*hop(t,.08f,.85f));
    }
    private static float companionY(GameCore c,Layout L) {
        float t=c.clock%LOOP;
        return L.h*.677f+radius(L)*(.45f-1.25f*hop(t,.18f,1f)-.45f*hop(t,1f,1.8f)-.75f*hop(t,2.25f,3.05f));
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float travel=panelTravel(c.timeAttack.slide),span=L.w*.85f;
        p.save();p.clipRect(L.w*.25f,L.h*.635f,L.w,TimeAttack.selectY(L)-L.unit*.6f);
        if(c.timeAttack.slide<1)scene(p,c,L,c.timeAttack.previousSelected,-c.timeAttack.slideDirection*span*travel);
        scene(p,c,L,c.timeAttack.selected,c.timeAttack.slideDirection*span*(1-travel));
        p.restore();
        float t=c.clock%LOOP,r=radius(L);
        p.fillEllipse(L.w*.16f,L.h*.677f+r*1.45f,r*1.1f,r*.13f,0x44302045);
        Trinket.drawReacting(p,SurvivalDemo.companion(c),companionX(c,L),companionY(c,L),r,c.clock*3,1,
                hop(t,.18f,1f)>.1f?5:hop(t,1f,1.8f)>.1f?4:8,.7f);
    }
    private static void scene(Painter p,GameCore c,Layout L,int selected,float offset) {
        p.save();p.translate(offset,0);
        if(selected==TimeAttack.ALL)drawAll(p,c,L);
        else drawOne(p,c,L,selected);
        p.restore();
    }
    private static void drawOne(Painter p,GameCore c,Layout L,int boss) {
        boolean known=TimeAttack.unlocked(c,boss);
        float t=c.clock%LOOP,r=radius(L),br=r*2;
        float y=L.h*.677f,right=L.w*.65f;
        float charge=hop(t,.08f,.85f),recoil=hop(t,1.5f,2.12f),cheer=hop(t,2.25f,3.05f);
        float cx=companionX(c,L),cy=companionY(c,L);
        float bx=right-br*.8f*charge+br*.22f*recoil;
        float by=y-br*.14f*Math.abs((float)Math.sin(t*10))-br*.22f*cheer;
        int tint=known?Collect.BODY[Collect.BOSS_FIRST+boss]:INK_DIM;
        p.fillEllipse(right,y+br*.92f,br*.85f,r*.15f,0x44302045);
        // Different boss signatures share one readable dodge/counter rhythm.
        if(known)signature(p,boss,bx,by,br,t,charge,tint);
        if(charge>.05f)for(int i=0;i<3;i++) {
            float yy=by+br*(i-1)*.38f;
            p.line(bx+br*1.1f,yy,bx+br*(1.3f+.4f*charge),yy,
                    fadeBy(tint,charge*.6f),r*.08f);
        }
        if(t>1.08f && t<1.57f) {
            float flight=(t-1.08f)/.49f;
            float px=cx+r+(bx-br-cx-r)*flight,py=cy+(by-cy)*flight;
            p.line(px-r*.7f,py+r*.12f,px,py,fadeBy(GOLD,.5f),r*.22f);
            p.fillPoly(star(px,py,r*.3f,r*.14f,5,t*12),GOLD);
        }
        if(recoil>0) {
            float pop=(t-1.5f)/.62f;
            p.strokeCircle(bx-br*.65f,by,br*(.3f+pop*.75f),fadeBy(GOLD,(1-pop)*.65f),r*.10f);
            for(int i=0;i<7;i++) {
                float a=i*Softbody.TAU/7f,dist=br*(.3f+pop*.75f);
                p.fillPoly(star(bx-br*.65f+(float)Math.cos(a)*dist,by+(float)Math.sin(a)*dist,
                        r*.20f*(1-pop),r*.08f*(1-pop),4,a+t),fadeBy(i%2==0?GOLD:tint,1-pop));
            }
        }
        BossCollect.draw(p,boss,bx,by,br,c.clock*3,known,1,recoil>.2f?5:cheer>.1f?8:1,-.65f);
        if(cheer>0)for(int i=0;i<3;i++) {
            float xx=(cx+bx)*.5f+(i-1)*r*.5f,yy=y-r*(.6f+cheer*.9f+(i%2)*.25f);
            p.fillPoly(star(xx,yy,r*.16f*cheer,r*.07f*cheer,4,t+i),fadeBy(GOLD,cheer));
        }
    }
    private static void drawAll(Painter p,GameCore c,Layout L) {
        float t=c.clock%.8f,beat=t/.8f,r=radius(L),br=r*2;
        float y=L.h*.679f,cx=companionX(c,L),cy=companionY(c,L);
        int turn=(int)(c.clock/.8f)%Boss.COUNT;
        for(int boss=0;boss<Boss.COUNT;boss++) {
            boolean active=boss==turn,known=TimeAttack.unlocked(c,boss);
            float bounce=(float)Math.sin(c.clock*11+boss*1.8f);
            float rush=active?hop(beat,0,.55f):0,recoil=active?hop(beat,.78f,1f):0;
            float bx=L.w*(.36f+boss*.17f)-br*.28f*rush+br*.12f*recoil;
            float by=y-br*.15f*Math.abs(bounce);
            int tint=known?Collect.BODY[Collect.BOSS_FIRST+boss]:INK_DIM;
            p.fillEllipse(L.w*(.36f+boss*.17f),y+br*.9f,br*.72f,r*.12f,0x44302045);
            if(known)signature(p,boss,bx,by,br,c.clock+boss*.4f,rush,tint);
            if(active) {
                boolean returning=beat>=.5f;
                float f=returning?(beat-.5f)*2:beat*2;
                float startX=returning?cx+r:bx-br,endX=returning?bx-br:cx+r;
                float px=startX+(endX-startX)*f;
                float py=cy+(by-cy)*(returning?f:1-f)-r*.5f*(float)Math.sin(f*Math.PI);
                int color=returning?GOLD:tint;
                p.line(px+(returning?-r:r)*.7f,py,px,py,fadeBy(color,.45f),r*.17f);
                p.fillPoly(star(px,py,r*.3f,r*.14f,returning?5:4,c.clock*12),color);
                if(recoil>0)p.strokeCircle(bx-br*.4f,by,br*(.6f+recoil*.3f),fadeBy(GOLD,recoil*.7f),r*.1f);
            }
            BossCollect.draw(p,boss,bx,by,br,c.clock*3,known,1,recoil>.1f?5:8,-.7f);
        }
    }
    private static void signature(Painter p,int boss,float x,float y,float r,float t,float charge,int tint) {
        if(boss==Boss.SLIME) {
            for(int i=0;i<5;i++) {
                float f=(t*1.6f+i*.19f)%1f;
                p.fillEllipse(x-r*(.6f+f*1.4f),y+r*(.7f-.5f*(float)Math.sin(f*Math.PI)),
                        r*.11f*(1-f),r*.16f*(1-f),fadeBy(tint,(1-f)*.65f));
            }
        } else if(boss==Boss.SPLITTER) {
            for(int side=-1;side<=1;side+=2) {
                float spread=r*(.14f+.38f*charge);
                BossCollect.draw(p,boss,x+side*spread,y-r*.08f,r,t*4,true,.22f,1,-.5f);
            }
        } else if(boss==Boss.OCTOPUS) {
            for(int i=0;i<3;i++) {
                float reach=r*(.55f+charge*.9f),yy=y+r*(.5f+i*.13f);
                float[] arm={x,yy,x-reach*.6f,yy-r*(.2f+.25f*i),x-reach,yy-r*(.3f+.2f*(float)Math.sin(t*14+i))};
                p.polyline(arm,tint,r*.12f);
                p.fillCircle(arm[4],arm[5],r*.09f,Collect.ACCENT[Collect.BOSS_FIRST+boss]);
            }
        } else {
            for(int i=0;i<6;i++) {
                float f=(t*1.4f+i*.167f)%1f,xx=x-r*(.3f+f*1.6f);
                float yy=y+r*(.45f-.9f*(float)Math.sin(f*Math.PI));
                p.fillCircle(xx,yy,r*.08f,fadeBy(i%2==0?tint:GOLD,(1-f)*.7f));
            }
        }
    }
}
