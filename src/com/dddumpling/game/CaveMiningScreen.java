package com.dddumpling.game;

/** The lantern's shrinking, fading pool is the clock; a loaded cart replaces the key prompt. */
final class CaveMiningScreen extends Draw {
    static float field(Layout L){return L.deckTop-L.playTop;}
    static float cartY(Layout L){return L.playTop+field(L)*.70f;}
    static boolean inCart(CaveMining m,Layout L,float x,float y) {
        return Math.abs(x-m.cartX*L.w)<L.w*.24f && Math.abs(y-cartY(L))<L.w*.14f
                && y<L.deckTop && y>L.playTop;
    }
    static float light(CaveMining m){return Math.max(0,Math.min(1,m.left/CaveMining.TIME));}
    static void draw(Painter p,GameCore c,Layout L) {
        CaveMining m=c.mining;float w=L.w,top=L.playTop,h=field(L),s=L.unit,light=light(m);
        p.fillRect(0,0,w,L.h,0xFF141321);
        p.fillEllipse(w*.5f,top+h*.43f,w*.61f,h*.52f,0xFF302C3B);
        for(int i=0;i<8;i++) {
            float x=i*w/7;
            p.fillPoly(new float[]{x-w*.13f,0,x+w*.12f,0,x+w*.02f,top+h*(.045f+.05f*(i%3))},0xFF46404B);
        }
        // Warm light loses both reach and intensity; the cave behind it remains visible near the end.
        for(int i=4;i>=0;i--) {
            float radius=w*(.25f+.55f*light)*(1f+i*.12f);
            p.fillEllipse(w*.13f,top+h*.30f,radius,radius*.80f,
                    Glyph.withAlpha(0xFFFFCD86,(int)((5+light*8)*(1f-i*.1f))));
        }
        float ledge=top+h*.47f;
        p.fillPoly(new float[]{w*.14f,ledge,w*.88f,ledge-w*.06f,w*.84f,ledge+w*.10f,w*.20f,ledge+w*.12f},0xFF6C6168);
        p.line(w*.16f,ledge,w*.87f,ledge-w*.06f,0xFFAE9385,w*.015f);
        float px=w*.36f,py=ledge-w*.095f,r=w*.074f;
        CaveDumpling.draw(p,c.caveChoice,px,py,r,1-m.strike*.055f,c.clock);
        float swing=m.strike*m.strike;
        float ax=px+r*.65f,ay=py+r*.35f,bx=px+r*(1.65f+swing*.8f),by=py+r*(-.70f+swing*1.1f);
        p.line(ax,ay,bx,by,0xFFBB8D68,r*.11f);
        p.line(bx-r*.32f,by-r*.19f,bx+r*.37f,by+r*.15f,0xFFB7CADA,r*.15f);
        p.fillCircle(ax,ay,r*.17f,CaveDumpling.COLORS[Math.max(0,c.caveChoice)]);
        rock(p,w*.64f,ledge-w*.07f,w*.09f,0xFF827D91);
        p.polyline(new float[]{w*.62f,ledge-w*.15f,w*.66f,ledge-w*.08f,w*.60f,ledge-w*.04f},0xFF4A415B,w*.009f);
        if(m.strike>.5f)for(int i=0;i<4;i++) {
            float t=1-m.strike;
            rock(p,w*(.60f+(i-1.5f)*t*.12f),ledge-w*(.06f+t*.13f),w*.008f,GOLD);
        }
        float track=cartY(L)+w*.10f;
        for(int i=0;i<12;i++)p.line(i*w/11,track-w*.025f,i*w/11+w*.04f,track+w*.06f,0xFF766052,w*.021f);
        p.line(0,track,w,track,0xFFB5B3BD,w*.011f);
        p.line(0,track+w*.042f,w,track+w*.042f,0xFF6C6C82,w*.011f);
        float cx=m.cartX*w,cy=cartY(L);
        for(int i=0;i<m.loads;i++)if(m.falling[i]>0) {
            float t=1-m.falling[i]/CaveMining.DROP;
            rock(p,w*.64f+(cx-w*.64f)*t,ledge-w*.1f+(cy-ledge)*t*t,w*.027f,0xFFADB3C9);
        }
        cart(p,cx,cy,w*.13f,m.loads,m.falling,c.clock,m.phase==CaveMining.PUSH?m.travel:0);
        helpers(p,c,L,m);
        p.fillRect(0,0,w,L.deckTop,Glyph.withAlpha(0xFF060713,(int)((1-light)*155)));
        lantern(p,w*.13f,top+h*.30f,w*.037f,light,c.clock);
        p.text("DUMPLING MINE",w*.5f,top+s*.5f,type(s*.78f),INK,Painter.CENTER,true);
        if(c.bonusParading()){Parade.draw(p,c,L,Math.min(1,c.paradeTimer/.35f));return;}
        if(m.phase==CaveMining.DIG) {
            float nr=w*.044f,space=w*.13f,sy=top+h*.15f;
            for(int i=0;i<m.length;i++) {
                float x=w*.5f+(i-(m.length-1)*.5f)*space;int g=m.sequence[i];
                int col=i<m.pos?0xFF88B8AC:Glyph.COLOR[g];
                p.fillPoly(Glyph.hex(x,sy,nr*1.18f),Glyph.withAlpha(col,i<m.pos?95:45));
                p.strokePoly(Glyph.hex(x,sy,nr*1.18f),col,w*.004f);
                Kawaii.draw(p,g,x,sy,nr*.85f,col,1,0);
                if(i==m.pos)p.fillPoly(new float[]{x-w*.01f,sy+nr*1.65f,x+w*.01f,sy+nr*1.65f,x,sy+nr*1.40f},m.bad>0?ROSE:GOLD);
            }
            for(int i=0;i<CaveMining.LOADS;i++)rock(p,w*(.36f+i*.07f),top+h*.24f,w*.014f,i<m.loads?GOLD:0xFF595569);
        } else if(m.phase==CaveMining.FULL) {
            // No sequence and no live keyboard while the cart is waiting for a swipe.
            p.text("SEND IT!",w*.5f,top+h*.17f,type(s*.78f),GOLD,Painter.CENTER,true);
            for(int side=-1;side<=1;side+=2) {
                float x=cx+side*w*(.23f+.015f*(float)Math.sin(c.clock*4));
                p.polyline(new float[]{x-side*w*.022f,cy-w*.022f,x,cy,x-side*w*.022f,cy+w*.022f},GOLD,w*.01f);
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
    private static void rock(Painter p,float x,float y,float r,int col) {
        p.fillPoly(new float[]{x-r,y+r*.3f,x-r*.65f,y-r*.6f,x+r*.25f,y-r,x+r,y-r*.2f,x+r*.65f,y+r*.65f,x-r*.3f,y+r},col);
        p.line(x-r*.6f,y-r*.5f,x+r*.15f,y-r*.8f,Glyph.withAlpha(INK,85),r*.14f);
    }
    private static void cart(Painter p,float x,float y,float r,int loads,float[] falling,float clock,float moving) {
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
    private static void helpers(Painter p,GameCore c,Layout L,CaveMining m) {
        if(m.phase!=CaveMining.PUSH && m.input.pointer<0)return;
        int dir=m.phase==CaveMining.PUSH?m.direction:m.cartX<.56f?-1:1;
        float arrival=m.phase==CaveMining.PUSH?Math.min(1,.25f+m.travel*2):Math.min(.3f,Math.abs(m.cartX-.56f)*2);
        for(int i=0;i<4;i++) {
            float r=L.w*.048f,x=m.cartX*L.w-dir*L.w*(.18f+i*.095f+(1-arrival)*.7f);
            float y=cartY(L)+L.w*.05f-r*.09f*(float)Math.sin(c.clock*17+i);
            CaveDumpling.draw(p,(i+1)%CaveDumpling.COUNT,x,y,r,1.05f,c.clock);
            p.line(x+dir*r*.65f,y+r*.25f,x+dir*r*1.15f,y+r*.15f,CaveDumpling.COLORS[(i+1)%CaveDumpling.COUNT],r*.16f);
        }
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
