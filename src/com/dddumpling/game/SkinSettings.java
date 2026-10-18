package com.dddumpling.game;

/** The skin gallery changes only a saved visual preference, including during a paused run. */
final class SkinSettings extends Draw {
    static final int OPEN=16, DEFAULT=17, HALLOWEEN=18, BACK=19;
    static float buttonY(Layout L) {return PlayerSettings.row(L,2)+PlayerSettings.unit(L)*4f;}
    static float cardHeight(Layout L) {
        float s=PlayerSettings.unit(L);
        return (PlayerSettings.bottom(L)-PlayerSettings.top(L)-s*11f)*.5f;
    }
    static float cardTop(Layout L,int skin) {return PlayerSettings.top(L)+PlayerSettings.unit(L)*5.8f+skin*(cardHeight(L)+PlayerSettings.unit(L)*1.2f);}
    static float backY(Layout L) {return PlayerSettings.bottom(L)-PlayerSettings.unit(L)*1.8f;}
    static int hit(Layout L,float x,float y) {
        float s=PlayerSettings.unit(L);
        for(int i=0;i<Skins.COUNT;i++)if(x>=PlayerSettings.contentL(L) && x<=PlayerSettings.contentR(L)
                && y>=cardTop(L,i) && y<=cardTop(L,i)+cardHeight(L))return DEFAULT+i;
        if(Math.abs(y-backY(L))<s*1.3f)return BACK;
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
    private static void card(Painter p,float l,float t,float r,float b,float s,int skin,boolean selected) {
        int color=skin==Skins.HALLOWEEN?(selected?0x665E497F:0x335E497F)
                :(selected?0x554DCEAA:0x334DCEAA);
        p.fillRect(l,t,r,b,color);
    }
    static void chip(Painter p,GameCore c,Layout L) {
        float s=PlayerSettings.unit(L),y=buttonY(L),l=PlayerSettings.contentL(L),r=PlayerSettings.contentR(L);
        int skin=c.preferences.skin;
        card(p,l,y-s*2.25f,r,y+s*2.25f,s,skin,false);
        p.text("SKINS",l+s,y-s*.52f,type(s*.72f),INK,Painter.LEFT,true);
        p.text(Skins.NAMES[skin],l+s,y+s*.65f,type(s*.46f),skin==1?0xFFFFCB9A:0xFF9AE9C0,Painter.LEFT,true);
        for(int i=0;i<3;i++) {
            int g=new int[]{0,1,5}[i];float x=r-s*(7.2f-i*2.4f);
            float hop=(float)Math.sin(c.clock*3-i*.85f);
            Kawaii.draw(p,g,x,y-s*.15f*hop,s*1.05f,Skins.keyColor(p,g),1+hop*.025f,.4f);
        }
        p.polyline(new float[]{r-s*.95f,y-s*.36f,r-s*.58f,y,r-s*.95f,y+s*.36f},INK,s*.1f);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float s=PlayerSettings.unit(L),l=PlayerSettings.contentL(L),r=PlayerSettings.contentR(L);
        p.text("DRESS UP YOUR KEYS",L.w*.5f,PlayerSettings.top(L)+s*4.3f,type(s*.68f),GOLD,Painter.CENTER,true);
        for(int i=0;i<Skins.COUNT;i++) {
            float top=cardTop(L,i),height=cardHeight(L),bottom=top+height;
            boolean selected=c.preferences.skin==i;
            card(p,l,top,r,bottom,s,i,selected);
            // Small drifting sparkles sit behind the cast, without changing game RNG.
            for(int k=0;k<9;k++) {
                float x=l+(r-l)*(.07f+hash(k*17+i*41)*.86f);
                float y=top+height*(.23f+hash(k*31+i*67)*.60f);
                float glow=.5f+.5f*(float)Math.sin(c.clock*2+k);
                int color=Glyph.withAlpha(i==1?0xFFFFC17F:0xFFB4F7DB,(int)(35+glow*65));
                p.line(x-s*.13f,y,x+s*.13f,y,color,s*.045f);
                p.line(x,y-s*.13f,x,y+s*.13f,color,s*.045f);
            }
            p.text(Skins.NAMES[i],l+s,top+s*1.8f,type(s*.80f),INK,Painter.LEFT,true);
            if(selected) {
                float cx=r-s*1.5f,cy=top+s*1.4f;
                p.fillRect(cx-s*.70f,cy-s*.70f,cx+s*.70f,cy+s*.70f,i==1?0xFFFFBF78:0xFF9AE9C0);
                p.polyline(new float[]{cx-s*.32f,cy,cx-s*.08f,cy+s*.25f,cx+s*.35f,cy-s*.28f},0xFF302442,s*.12f);
            }
            Painter preview=Skins.painter(p,i);
            for(int g=0;g<Glyph.COUNT;g++) {
                float x=l+(r-l)*(g%3+.5f)/3f,y=top+height*(g<3?.39f:.72f);
                float wave=(float)Math.sin(c.clock*3+g*.8f),tap=Math.max(0,1-(c.clock-c.preferences.skinAt)/.45f);
                float bounce=.13f*wave;
                float radius=Math.min(s*1.65f,(r-l)/10f)*(1+tap*.13f*(selected?1:0));
                p.fillEllipse(x,y+s*1.35f,s*1.25f,s*.18f,0x30201830);
                Kawaii.draw(preview,g,x,y-s*bounce,radius,Skins.keyColor(preview,g),1+bounce*.20f,.45f);
            }
            p.text(selected?"WEARING THIS!":"TAP TO WEAR",L.w*.5f,bottom-s*.65f,type(s*.48f),selected?(i==1?0xFFFFCB9A:0xFF9AE9C0):INK_DIM,Painter.CENTER,true);
        }
        float by=backY(L);
        p.polyline(new float[]{L.w*.5f-s*2.7f,by-s*.3f,L.w*.5f-s*3f,by,L.w*.5f-s*2.7f,by+s*.3f},INK,s*.09f);
        p.text("BACK",L.w*.5f,by+s*.25f,type(s*.65f),INK,Painter.CENTER,true);
    }
}
