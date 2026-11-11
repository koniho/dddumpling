package com.dddumpling.game;

/** Bounded, immutable scenery tiles. Foreground reeds and wildlife remain live. */
final class SwampLayers {
    static final int SKY=0,HAZE=1,TREES=2,REEDS=5,COUNT=17;
    static boolean draw(Painter p,int art,Layout l,float shift,float fade) {
        // Retain per-primitive alpha while entering or leaving the land.
        ArtStamp cache=p.stamps();
        return fade==1 && cache!=null && cache.swampLayer(art,l.w,l.h,l.deckTop,shift,255,0);
    }
    static float left(int art,float w) {return art<REEDS?-w*(art>=TREES?.16f:.06f):w*((art-REEDS)*3/35f-.055f);}
    static float top(int art,float h,float w) {
        return art==SKY?-h*.03f:art==HAZE?h*.365f:art<REEDS?h*.15f:h*.43f-w*.19f;
    }
    static float width(int art,float w) {return w*(art<REEDS?(art>=TREES?1.32f:1.12f):3/35f+.11f);}
    static float bottom(int art,float h,float w,float deck) {
        return art==SKY?h:art==HAZE?h*.515f:art<REEDS?h*.50f:h*.45f+w*.115f;
    }
    static void paint(Painter p,int art,float w,float h,float deck) {
        Layout l=new Layout();l.w=w;l.h=h;l.deckTop=deck;
        if(art==SKY){SwampVista.backdrop(p,l,1);SwampVista.nightSky(p,l,1);}
        else if(art==HAZE)SwampVista.horizonHaze(p,l,1);
        else if(art<REEDS)SwampVista.treeLayer(p,l,art-TREES,0,1);
        else SwampVista.reedGroup(p,l,art-REEDS);
    }
}
