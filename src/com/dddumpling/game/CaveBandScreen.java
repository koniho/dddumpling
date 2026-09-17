package com.dddumpling.game;

/** The remaining bars are hanging crystals; the strike line is the guitar's glowing pick. */
final class CaveBandScreen extends Draw {
    static float strike(Layout L) { return L.w*.20f; }
    static float lane(Layout L) { return L.playTop+(L.deckTop-L.playTop)*.65f; }
    static float noteX(Layout L,float due,float now) { return strike(L)+(due-now)*L.w*.43f; }
    static void draw(Painter p,GameCore c,Layout L) {
        CaveBand b=c.band;float w=L.w,top=L.playTop,field=L.deckTop-top,s=L.unit;
        float beat=CaveSong.beat(b.song,b.position),bounce=(float)Math.sin(beat*Math.PI*2);
        p.fillRect(0,0,w,L.h,0xFF151124);
        p.fillEllipse(w*.5f,top+field*.38f,w*.60f,field*.43f,0xFF242037);
        for(int i=0;i<9;i++) {
            float x=i*w/8,tip=top+field*(.04f+.07f*(i%3));
            p.fillPoly(new float[]{x-w*.13f,0,x+w*.11f,0,x+w*.035f,tip},0xFF342740);
        }
        p.text(CaveSong.NAMES[b.song],w*.5f,top+s*.5f,type(s*.78f),INK,Painter.CENTER,true);
        for(int i=0;i<8;i++) {
            float x=w*(.17f+i*.095f),y=top+s*2.2f;
            boolean pending=beat<i*4+4;
            int col=pending?Glyph.COLOR[(i+b.song)%6]:0xFF403650;
            crystal(p,x,y,w*.018f,col);
            if(pending && beat>=i*4)p.strokeCircle(x,y,w*.028f,Glyph.withAlpha(col,100),w*.006f);
        }
        float backY=top+field*.28f,frontY=top+field*.40f,r=w*.066f;
        p.fillEllipse(w*.5f,frontY+r*.9f,w*.40f,r*1.1f,0xFF382D4B);
        // Drummer sits behind the kit, while the other three instruments keep distinct silhouettes.
        musician(p,4,w*.49f,backY,r,bounce);
        drums(p,w*.49f,backY+r*.55f,r,bounce);
        musician(p,2,w*.19f,backY+r*.55f,r,bounce);
        SettingsArt.guitar(p,w*.19f,backY+r*.55f,r,.65f,beat*1.5f,Glyph.COLOR[2]);
        p.line(w*.19f+r*.45f,backY+r*.85f,w*.19f+r*1.2f,backY+r*.18f,0xFFDFC395,r*.13f);
        musician(p,5,w*.81f,backY+r*.55f,r,bounce);
        keyboard(p,w*.81f,backY+r*1.1f,r,bounce);
        float gy=frontY-r*.055f*bounce;
        CaveDumpling.draw(p,c.caveChoice,w*.49f,gy,r*1.25f,1f+.025f*bounce,b.position);
        SettingsArt.guitar(p,w*.49f,gy,r*1.25f,1f,b.pulse>0?b.position*4:0,CaveDumpling.COLORS[Math.max(0,c.caveChoice)]);
        if(b.pulse>0)for(int i=0;i<4;i++) {
            float a=i*1.57f+b.position;
            crystal(p,w*.49f+(float)Math.cos(a)*r*1.7f,gy+(float)Math.sin(a)*r*1.3f,r*.12f*b.pulse,GOLD);
        }
        if(c.bonusParading()) { Parade.draw(p,c,L,Math.min(1,c.paradeTimer/.35f));return; }
        float y=lane(L),radius=w*.040f;
        p.fillRect(w*.05f,y-radius*1.7f,w*.95f,y+radius*1.7f,0xFF0C1221);
        p.line(w*.05f,y,w*.95f,y,0xFF514B66,w*.003f);
        p.fillRect(strike(L)-w*.018f,y-radius*1.6f,strike(L)+w*.018f,y+radius*1.6f,
                Glyph.withAlpha(b.badPulse>0?ROSE:GOLD,70+(int)(b.pulse*100)));
        p.line(strike(L),y-radius*1.9f,strike(L),y+radius*1.9f,b.badPulse>0?ROSE:GOLD,w*.006f);
        p.save();p.clipRect(w*.05f,y-radius*1.7f,w*.95f,y+radius*1.7f);
        if(!b.finished)for(int i=0;i<b.glyph.length;i++) {
            if(b.result[i]==2 || b.result[i]==1)continue;
            float x=noteX(L,CaveSong.at(b.song,i),b.position);
            if(x<-radius || x>w+radius)continue;
            int g=b.glyph[i];
            if(b.result[i]<0) { p.strokeCircle(x,y,radius*.5f,0xFF694255,w*.004f);continue; }
            p.fillPoly(Glyph.hex(x,y,radius*1.15f),0xFF373149);
            p.strokePoly(Glyph.hex(x,y,radius*1.15f),Glyph.COLOR[g],w*.003f);
            Kawaii.draw(p,g,x,y,radius*.72f,Glyph.COLOR[g],1,0);
        }
        p.restore();
        if(beat<0) {
            // Four drumstick flashes count in before the first playable note.
            p.text("PLAY AT THE LIGHT",w*.5f,y-radius*2.8f,type(s*.52f),INK,Painter.CENTER,true);
            for(int i=0;i<4;i++)p.fillCircle(w*(.40f+i*.065f),y+radius*2.5f,w*.009f,
                    i<beat+4?GOLD:0xFF514B66);
        }
        float cy=top+field*.83f;
        for(int i=0;i<10;i++) {
            float x=w*(.18f+i*.071f);
            int charge=b.won && b.finished?CaveBand.GOAL:b.charge;
            crystal(p,x,cy,w*.019f,charge>i*4?Glyph.COLOR[(i+b.song)%6]:0xFF44364F);
        }
        String label=b.finished?(b.won?"ENCORE!":"KEEP THE SPARK!")
                :Math.min(CaveBand.GOAL,b.charge)+" / "+CaveBand.GOAL;
        p.text(label,w*.5f,cy+s*1.8f,type(s*.63f),b.won?GOLD:INK,Painter.CENTER,true);
        if(b.finished && b.won)Trinket.draw(p,c.prize,w*.5f,y,w*.065f,c.clock,true,1f);
        p.fillRect(0,L.deckTop,w,L.h,0xFF1C172B);
        Renderer.keys(p,c,L);
    }
    private static void crystal(Painter p,float x,float y,float r,int color) {
        p.fillPoly(new float[]{x,y-r*1.7f,x+r,y-r*.3f,x+r*.7f,y+r,x,y+r*1.5f,x-r*.7f,y+r,x-r,y-r*.3f},color);
        p.line(x,y-r*1.3f,x-r*.2f,y+r*.7f,Glyph.withAlpha(INK,100),r*.18f);
    }
    private static void musician(Painter p,int glyph,float x,float y,float r,float bounce) {
        Kawaii.draw(p,glyph,x,y-r*.045f*bounce,r,Glyph.COLOR[glyph],1+.03f*bounce,.6f);
    }
    private static void drums(Painter p,float x,float y,float r,float beat) {
        p.fillCircle(x,y+r*.35f,r*.62f,0xFFBD6EAC);p.fillCircle(x,y+r*.35f,r*.47f,0xFFE5CCDA);
        p.fillPoly(star(x,y+r*.35f,r*.25f,r*.12f,5,0f),0xFF745781);
        for(int side=-1;side<=1;side+=2) {
            p.line(x+side*r,y-r*.5f,x+side*r,y+r*.75f,0xFF8D90A8,r*.06f);
            p.fillEllipse(x+side*r,y-r*.5f,r*.55f,r*.12f,GOLD);
            p.line(x+side*r*.3f,y-r*.4f,x+side*r*.82f,y-r*(.7f+side*beat*.18f),0xFFF6D7A4,r*.075f);
        }
    }
    private static void keyboard(Painter p,float x,float y,float r,float beat) {
        p.line(x-r*.65f,y,x+r*.5f,y+r*.7f,0xFF9893B1,r*.075f);
        p.line(x+r*.65f,y,x-r*.5f,y+r*.7f,0xFF9893B1,r*.075f);
        p.fillRect(x-r,y-r*.2f,x+r,y+r*.22f,0xFFF3E8D4);
        for(int i=1;i<8;i++) {
            float k=x-r+i*r*.25f;
            p.line(k,y-r*.2f,k,y+r*.22f,0xFF3A314A,r*.035f);
            if(i%3!=0)p.fillRect(k-r*.045f,y-r*.2f,k+r*.045f,y+r*.05f,0xFF332940);
        }
        for(int side=-1;side<=1;side+=2)p.fillCircle(x+side*r*.45f,y-r*(.16f+.1f*side*beat),r*.15f,Glyph.COLOR[5]);
    }
}
