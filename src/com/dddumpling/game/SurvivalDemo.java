package com.dddumpling.game;

/** A clock-driven title skit; no combat state, audio, or gameplay randomness. */
final class SurvivalDemo extends Draw {
    private SurvivalDemo() {}
    static int companion(GameCore c) {
        if(Collect.has(c.collected,c.caseIndex))return c.caseIndex;
        for(int i=0;i<Collect.COUNT;i++)if(Collect.has(c.collected,i))return i;
        return 0;
    }
    static void draw(Painter painter,GameCore c,Layout L,float fade) {
        if(fade<=.004f)return;
        Painter p=new OpacityPainter(painter,fade);
        float clock=c.clock%8f,beat=clock%2f;
        int turn=(int)(clock/2f),side=turn%2==0?-1:1,g=Roster.at(c.fullRoster,turn%Roster.count(c.fullRoster));
        boolean ninja=turn<2;
        float s=L.unit,x=L.w*.5f,y=LandPicker.cardY(L)+s*1.3f,r=Math.min(s*1.35f,L.h*.029f);
        float attack=Math.max(0,Math.min(1,(beat-.7f)/.5f));
        float spring=(float)Math.sin(attack*Math.PI);
        float actorX=x+side*(ninja?L.w*.095f*spring:-r*.12f*spring);
        float actorY=y-r*.25f*Math.abs((float)Math.sin(beat*Math.PI))-r*.25f*spring;
        float hitX=x+side*L.w*.24f,hitY=y-s*.12f;
        float approach=Math.min(1,beat/.85f);
        float enemyX=x+side*L.w*(.42f-.18f*panelTravel(approach));
        float impact=ninja?.95f:1.12f;
        if(beat<impact) {
            GameCore.Enemy e=new GameCore.Enemy();
            e.word=new int[]{g};e.need=new int[]{1};e.gone=new boolean[1];
            e.baseX=enemyX;e.y=hitY;e.enterT=Math.min(1,beat/.2f);
            Renderer.enemy(p,c,L,e);
        } else {
            float pop=Math.min(1,(beat-impact)/.55f);
            for(int i=0;i<8;i++) {
                float a=i*(float)Math.PI/4f,dist=s*(.3f+pop*1.5f);
                p.fillCircle(hitX+(float)Math.cos(a)*dist,hitY+(float)Math.sin(a)*dist,
                        s*.12f*(1-pop),fadeBy(Glyph.COLOR[g],1-pop));
            }
        }
        if(ninja && beat>=.7f && beat<1.25f) {
            float sweep=Math.min(1,(beat-.7f)/.28f),alpha=1f-Math.max(0,(beat-.98f)/.27f);
            float[] path=new float[18];
            for(int i=0;i<9;i++) {
                float t=sweep*i/8f;
                path[i*2]=x+side*L.w*(.035f+.29f*t);
                path[i*2+1]=y+s*(.48f-.85f*t-.3f*(float)Math.sin(t*Math.PI));
            }
            p.polyline(path,fadeBy(Glyph.COLOR[g],alpha*.45f),s*.35f);
            p.polyline(path,fadeBy(INK,alpha),s*.09f);
        } else if(!ninja && beat>=.7f && beat<impact) {
            Renderer.bullet(p,c,L,x+side*r*.6f,actorY,hitX,hitY,(beat-.7f)/(impact-.7f),g,1);
        }
        p.fillEllipse(x,y+r*.72f,r*.8f,r*.13f,0x44302045);
        Trinket.drawReacting(p,companion(c),actorX,actorY,r*(1f+.08f*spring),c.clock,1f,
                spring>.3f?4:1,side*.5f,ninja);
        if(turn%2==0 && beat<.3f) {
            float pop=beat/.3f;
            p.arc(x,y,r*(.8f+pop*.7f),r*(.8f+pop*.7f),0,360,
                    fadeBy(ninja?Glyph.COLOR[3]:GOLD,(1-pop)*.6f),s*.06f);
        }
    }
}
