package com.dddumpling.game;

/** The skin gallery changes only a saved visual preference, including during a paused run. */
final class SkinSettings extends Draw {
    static final int OPEN=16, DEFAULT=17, HALLOWEEN=18, BACK=19;
    static float buttonY(Layout L) {return PlayerSettings.row(L,2)+PlayerSettings.unit(L)*2.3f;}
    static float cardTop(Layout L,int skin) {return PlayerSettings.top(L)+PlayerSettings.unit(L)*(6f+skin*14f);}
    static int hit(Layout L,float x,float y) {
        float s=PlayerSettings.unit(L),t=PlayerSettings.top(L);
        if(y<t+s*3f && x<PlayerSettings.left(L)+s*4f)return BACK;
        for(int i=0;i<Skins.COUNT;i++)if(x>=PlayerSettings.left(L)+s && x<=PlayerSettings.right(L)-s
                && y>=cardTop(L,i) && y<=cardTop(L,i)+s*12f)return DEFAULT+i;
        if(Math.abs(y-(t+s*37f))<s*1.4f)return BACK;
        return 0;
    }
    static void choose(GameCore c,int skin) {
        skin=Skins.valid(skin);
        if(c.preferences.skin==skin)return;
        c.preferences.skin=skin;c.preferences.save(c);
        if(c.onboarding.practice!=null)c.onboarding.practice.preferences.skin=skin;
        c.preferences.skinAt=c.clock;
        if(c.sound!=null)c.sound.uiBloop();
    }
    static void chip(Painter p,GameCore c,Layout L) {
        float s=PlayerSettings.unit(L),y=buttonY(L),l=PlayerSettings.left(L)+s,r=PlayerSettings.right(L)-s;
        p.fillRect(l,y-s*.8f,r,y+s*.8f,0x555D4D80);
        Kawaii.draw(p,0,l+s,y,s*.65f,Skins.keyColor(p,0),1,.5f);
        p.text("SKINS: "+Skins.NAMES[c.preferences.skin],L.w*.5f,y+s*.2f,type(s*.46f),INK,Painter.CENTER,true);
        p.polyline(new float[]{r-s*1.2f,y-s*.30f,r-s*.85f,y,r-s*1.2f,y+s*.30f},INK,s*.08f);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float s=PlayerSettings.unit(L),l=PlayerSettings.left(L)+s,r=PlayerSettings.right(L)-s;
        p.text("SKINS",L.w*.5f,PlayerSettings.top(L)+s*4.4f,type(s*.8f),GOLD,Painter.CENTER,true);
        for(int i=0;i<Skins.COUNT;i++) {
            float top=cardTop(L,i),bottom=top+s*12f;
            boolean selected=c.preferences.skin==i;
            p.fillRect(l,top,r,bottom,selected?0x554DCEAA:0x225E567A);
            if(selected)p.strokePoly(new float[]{l,top,r,top,r,bottom,l,bottom},0xFF9AE9C0,s*.07f);
            p.text(Skins.NAMES[i],L.w*.5f,top+s*1.8f,type(s*.70f),INK,Painter.CENTER,true);
            Painter preview=Skins.painter(p,i);
            for(int g=0;g<Glyph.COUNT;g++) {
                float x=l+(r-l)*(g%3+.5f)/3f,y=top+s*(4.4f+(g/3)*3.5f);
                float wave=(float)Math.sin(c.clock*3+g*.8f),tap=Math.max(0,1-(c.clock-c.preferences.skinAt)/.45f);
                float bounce=selected?.12f*wave:0;
                Kawaii.draw(preview,g,x,y-s*bounce,s*1.25f*(1+tap*.07f*(selected?1:0)),Skins.keyColor(preview,g),1+bounce*.20f,selected?.8f:.25f);
            }
            p.text(selected?"SELECTED":"TAP TO SELECT",L.w*.5f,bottom-s*.75f,type(s*.44f),selected?0xFF9AE9C0:INK_DIM,Painter.CENTER,true);
        }
        p.text("BACK",L.w*.5f,PlayerSettings.top(L)+s*37.2f,type(s*.65f),INK,Painter.CENTER,true);
    }
}
