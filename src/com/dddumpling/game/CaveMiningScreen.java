package com.dddumpling.game;

/** The lantern's shrinking, fading pool is the clock; a full rock pile replaces the key prompt. */
final class CaveMiningScreen extends Draw {
    static float field(Layout L){return L.deckTop-L.playTop;}
    static float cartY(Layout L){return CaveMiningScene.ground(L)-L.w*.07f;}
    static boolean inCart(CaveMining m,Layout L,float x,float y) {
        return Math.abs(x-m.cartX*L.w)<L.w*.24f && Math.abs(y-cartY(L))<L.w*.23f
                && y<L.deckTop && y>L.playTop;
    }
    static float light(CaveMining m){return Math.max(0,Math.min(1,m.left/CaveMining.TIME));}
    static void draw(Painter p,GameCore c,Layout L) {
        CaveMining m=c.mining;float w=L.w,top=L.playTop,h=field(L),s=L.unit,light=light(m);
        p.fillRect(0,0,w,L.h,0xFF141321);
        p.save();p.clipRect(0,top,w,L.deckTop);
        float shake=m.scene.shake;
        p.translate((float)Math.sin(m.scene.clock*91)*w*.008f*shake,(float)Math.sin(m.scene.clock*113)*w*.005f*shake);
        m.scene.draw(p,c,L);
        p.restore();
        p.fillRect(0,0,w,L.deckTop,Glyph.withAlpha(0xFF060713,(int)((1-light)*155)));
        lantern(p,w*.13f,top+h*.30f,w*.037f,light,c.clock);
        p.text("DUMPLING MINE",w*.5f,top+s*.5f,type(s*.78f),INK,Painter.CENTER,true);
        if(c.bonusParading()){Parade.draw(p,c,L,Math.min(1,c.paradeTimer/.35f));return;}
        if(m.phase==CaveMining.DIG) {
            p.save();p.translate((float)Math.sin(m.scene.clock*91)*w*.008f*shake,(float)Math.sin(m.scene.clock*113)*w*.005f*shake);
            float nr=w*.048f,x=CaveMiningScene.wallX(m)*w,y=CaveMiningScene.promptY(m,L,m.pos/(float)(m.length-1));
            int g=m.sequence[m.pos];
            p.fillCircle(x,y,nr*1.28f,0xFF241A24);
            p.strokeCircle(x,y,nr*1.28f,m.bad>0?ROSE:GOLD,w*.005f);
            Kawaii.draw(p,g,x,y,nr,Glyph.COLOR[g],1,0);p.restore();
        } else if(m.phase==CaveMining.FULL) {
            // No sequence and no live keyboard while the cart is waiting for a swipe.
            p.text("SWIPE THE PILE!",w*.5f,top+h*.17f,type(s*.78f),GOLD,Painter.CENTER,true);
            for(int side=-1;side<=1;side+=2) {
                float x=m.cartX*w+side*w*(.23f+.015f*(float)Math.sin(c.clock*4));
                p.polyline(new float[]{x-side*w*.022f,cartY(L)-w*.022f,x,cartY(L),x-side*w*.022f,cartY(L)+w*.022f},GOLD,w*.01f);
            }
        } else if(m.phase==CaveMining.REPORT) {
            p.text(m.won?"A PRECIOUS FIND!":"MORE NEXT TIME",w*.5f,top+h*.17f,type(s*.67f),m.won?GOLD:INK,Painter.CENTER,true);
            if(m.won)Trinket.draw(p,c.prize,w*.5f,top+h*.58f,w*.085f,c.clock,true,1);
        }
        int delivered=m.won?CaveMining.CARTS:m.carts;
        for(int i=0;i<CaveMining.CARTS;i++)miniCart(p,w*(.25f+i*.125f),top+h*.86f,w*.035f,i<delivered);
        p.fillRect(0,L.deckTop,w,L.h,0xFF221E31);
        Renderer.keys(m.phase==CaveMining.DIG?p:new OpacityPainter(p,.20f),c,L);
    }
    static void rock(Painter p,float x,float y,float r,int col) {
        p.fillPoly(new float[]{x-r,y+r*.3f,x-r*.65f,y-r*.6f,x+r*.25f,y-r,x+r,y-r*.2f,x+r*.65f,y+r*.65f,x-r*.3f,y+r},col);
        p.line(x-r*.6f,y-r*.5f,x+r*.15f,y-r*.8f,Glyph.withAlpha(INK,85),r*.14f);
    }
    static void cart(Painter p,float x,float y,float r,int loads,float[] falling,float clock,float moving) {
        for(int i=0;i<loads;i++)if(falling==null || falling[i]<=0) {
            float rx=x+r*((i%3)-1)*.58f,ry=y-r*(.32f+(i/3)*.38f);
            rock(p,rx,ry,r*.39f,0xFF9697AB);
        }
        p.fillPoly(new float[]{x-r,y-r*.30f,x+r,y-r*.30f,x+r*.77f,y+r*.42f,x-r*.77f,y+r*.42f},0xFF9B6375);
        p.line(x-r,y-r*.30f,x+r,y-r*.30f,0xFFDAA0AB,r*.10f);
        for(int side=-1;side<=1;side+=2) {
            float wx=x+side*r*.58f,wy=y+r*.53f;
            p.fillCircle(wx,wy,r*.22f,0xFF252331);p.strokeCircle(wx,wy,r*.22f,0xFFBAB8C7,r*.055f);
            float angle=moving*14;
            p.line(wx,wy,wx+(float)Math.cos(angle)*r*.17f,wy+(float)Math.sin(angle)*r*.17f,0xFFBAB8C7,r*.055f);
            p.fillCircle(x+side*r*.65f,y+r*.02f,r*.045f,0xFFE0B5A8);
        }
    }
    private static void miniCart(Painter p,float x,float y,float r,boolean full) {
        cart(new OpacityPainter(p,full?1f:.28f),x,y,r,full?5:0,null,0,0);
    }
    private static void lantern(Painter p,float x,float y,float r,float light,float clock) {
        p.line(x,y-r*2.5f,x,y-r*1.35f,0xFFBDA77D,r*.12f);
        p.strokeCircle(x,y-r*1.05f,r*.40f,0xFFE0BE80,r*.12f);
        p.fillPoly(new float[]{x-r*.7f,y-r*.7f,x+r*.7f,y-r*.7f,x+r*.85f,y+r*.7f,x-r*.85f,y+r*.7f},0xFF4B3540);
        p.fillRect(x-r*.54f,y-r*.52f,x+r*.54f,y+r*.50f,Glyph.mix(0xFF392C38,0xFFFFC76A,light));
        if(light>0) {
            float f=r*(.18f+.55f*light)*(1+.05f*(float)Math.sin(clock*11));
            p.fillEllipse(x,y,f*.47f,f,0xFFFFEDB0);
            p.fillCircle(x,y+f*.30f,f*.25f,INK);
        }
        p.line(x-r*.9f,y+r*.7f,x+r*.9f,y+r*.7f,0xFFCFAD78,r*.18f);
    }
}
