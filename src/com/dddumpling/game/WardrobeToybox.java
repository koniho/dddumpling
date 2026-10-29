package com.dddumpling.game;

/** The same curved ribbon geometry drives rendering, taps and swipes. Only visible toys draw. */
final class WardrobeToybox extends Draw {
    static int visible(int count) {return Math.min(3,count);}
    static float maxScroll(int count) {return Math.max(0,count-visible(count));}
    static float spacing(Layout L,int count) {return L.w*(count<=3?.29f:.25f);}
    static float categoryR(Layout L) {return Math.min(L.w*.054f,WardrobeScreen.trayHeight(L)*.095f);}
    static float categoryX(Layout L,int slot) {return L.w*(.14f+slot*.18f);}
    static float categoryY(Layout L) {return WardrobeScreen.trayTop(L)+categoryR(L)*1.45f;}
    static float itemR(Layout L,int count) {return Math.min(L.w*(count<=3?.125f:.115f),WardrobeScreen.trayHeight(L)*.225f);}
    static float settledX(Layout L,int count,int index,float scroll) {
        return L.w*.5f+(index-scroll-(visible(count)-1)*.5f)*spacing(L,count);
    }
    static float settledY(Layout L,float x) {
        float u=(x-L.w*.5f)/(L.w*.43f);
        return WardrobeScreen.trayTop(L)+WardrobeScreen.trayHeight(L)*(.50f+.05f*(1-u*u));
    }
    static float arrival(WardrobeScreen ui,int index) {
        float delay=Math.max(0,Math.min(3,index-(int)ui.scroll))*.055f;
        return Math.max(0,Math.min(1,(ui.trayT-delay)/.48f));
    }
    static float travel(float t) {return 1-(1-t)*(1-t)*(1-t);}
    static float itemX(WardrobeScreen ui,Layout L,int count,int index) {
        float t=travel(arrival(ui,index));
        return categoryX(L,ui.slot)+(settledX(L,count,index,ui.scroll)-categoryX(L,ui.slot))*t;
    }
    static float itemY(WardrobeScreen ui,Layout L,int count,int index) {
        float t=arrival(ui,index),end=settledY(L,settledX(L,count,index,ui.scroll));
        return categoryY(L)+(end-categoryY(L))*travel(t)-itemR(L,count)*(float)Math.sin(Math.PI*t)*.55f;
    }
    static float bottom(Layout L,boolean details) {
        return WardrobeScreen.trayTop(L)+WardrobeScreen.trayHeight(L)*(details?1.02f:.80f);
    }
    static boolean inRibbon(Layout L,float x,float y) {
        float t=WardrobeScreen.trayTop(L),h=WardrobeScreen.trayHeight(L);
        return x>=L.w*.04f && x<=L.w*.96f && y>=t+h*.25f && y<=t+h*.80f;
    }
    static int hit(WardrobeScreen ui,Layout L,int count,float x,float y) {
        float r=categoryR(L)*1.24f,dy=y-categoryY(L);
        for(int k=0;k<Accessories.SLOTS;k++)if(Math.hypot(x-categoryX(L,k),dy)<=r)return 10+k;
        if(!inRibbon(L,x,y))return 0;
        for(int i=Math.max(0,(int)Math.floor(ui.scroll)-1);i<Math.min(count,(int)Math.floor(ui.scroll)+5);i++) {
            if(arrival(ui,i)<.40f)continue;
            float xx=itemX(ui,L,count,i),yy=itemY(ui,L,count,i);
            if(Math.hypot(x-xx,y-yy)<=itemR(L,count)*1.05f)return 20+i;
        }
        return 0;
    }
    static void draw(Painter p,GameCore c,Layout L,WardrobeScreen ui) {
        int count=Accessories.count(ui.slot);float h=WardrobeScreen.trayHeight(L),top=WardrobeScreen.trayTop(L);
        int accent=Accessories.COLOR[Accessories.item(ui.slot,0)];
        float[] ribbon=new float[26];
        for(int i=0;i<13;i++){float x=L.w*(.06f+i*.88f/12);ribbon[i*2]=x;ribbon[i*2+1]=settledY(L,x)+itemR(L,count)*.76f;}
        p.polyline(ribbon,Glyph.withAlpha(accent,24),L.unit*.45f);
        p.polyline(ribbon,Glyph.withAlpha(accent,65),L.unit*.045f);
        p.save();p.clipRect(L.w*.035f,top-L.unit*.2f,L.w*.965f,top+h*.81f);
        if(ui.lastSlot>=0 && ui.lastSlot!=ui.slot && ui.trayT<.28f) {
            float leave=ui.trayT/.28f;int oldCount=Accessories.count(ui.lastSlot);
            for(int i=Math.max(0,(int)ui.lastScroll-1);i<Math.min(oldCount,(int)ui.lastScroll+5);i++) {
                float x=settledX(L,oldCount,i,ui.lastScroll)+L.w*leave*.75f,y=settledY(L,x)+h*leave*.08f,r=itemR(L,oldCount)*(1-leave*.40f);
                AccessoryArt.thumbnail(new OpacityPainter(p,(1-leave)*.45f),Accessories.item(ui.lastSlot,i),x-r,y-r,x+r,y+r,c.clock);
            }
        }
        for(int i=Math.max(0,(int)Math.floor(ui.scroll)-1);i<Math.min(count,(int)Math.floor(ui.scroll)+5);i++) {
            int item=Accessories.item(ui.slot,i);float t=arrival(ui,i),x=itemX(ui,L,count,i),y=itemY(ui,L,count,i),r=itemR(L,count);
            float scale=.20f+.80f*travel(t)+.10f*(float)Math.sin(t*Math.PI*2)*(1-t);
            boolean owned=c.wardrobe.owned[item],wearing=c.wardrobe.equipped(ui.who,ui.slot)==item;
            float edge=Math.min(1,Math.max(0,(Math.min(x,L.w-x)-L.w*.015f)/(L.w*.09f)));
            float a=edge*Math.min(1,t*4);
            p.fillEllipse(x,y+r*.72f,r*.78f,r*.12f,Glyph.withAlpha(0xFF000000,(int)(48*a)));
            if(wearing) {
                p.fillEllipse(x,y,r*.99f,r*.88f,Glyph.withAlpha(Accessories.COLOR[item],(int)(30*a)));
                p.arc(x,y,r*.97f,r*.89f,c.clock*24,245,Glyph.withAlpha(Accessories.COLOR[item],(int)(170*a)),L.unit*.07f);
                AccessoryArt.star(p,x+r*.74f,y-r*.63f,r*.12f,Glyph.withAlpha(0xFFFFE9A3,(int)(255*a)));
            }
            float angle=(1-t)*(-.55f+i*.23f)+.025f*(float)Math.sin(c.clock*2+i);
            Painter q=new LinkedPairArt.TurnPainter(new OpacityPainter(p,a*(owned?1:.30f)),x,y,angle);
            float rr=r*scale*(1+.018f*(float)Math.sin(c.clock*2.4f+i));
            AccessoryArt.thumbnail(q,item,-rr,-rr,rr,rr,c.clock);
            if(!owned)lock(p,x+r*.62f,y+r*.55f,r*.19f,a);
            if(t>.65f && t<1) {
                float pop=(t-.65f)/.35f;
                p.strokeCircle(x,y,r*(.65f+pop*.5f),Glyph.withAlpha(Accessories.COLOR[item],(int)(120*(1-pop)*a)),L.unit*.075f);
            }
        }
        if(ui.swoosh>0) {
            for(int i=0;i<7;i++) {
                float x=ui.dragX-ui.swipeDirection*L.unit*(i*.55f+ui.swooshAge*5);
                float y=settledY(L,x)+itemR(L,count)*.76f;
                AccessoryArt.star(p,x,y,L.unit*(.10f+i*.012f),Glyph.withAlpha(accent,(int)(125*ui.swoosh*(1-i/8f))));
            }
        }
        p.restore();
        if(count>visible(count)) {
            float y=top+h*.78f,r=L.unit*.10f,track=L.w*.18f;
            p.line(L.w*.5f-track,y,L.w*.5f+track,y,0x554E4469,r);
            float fraction=Math.max(0,Math.min(1,ui.scroll/maxScroll(count)));
            p.fillCircle(L.w*.5f-track+track*2*fraction,y,r*1.8f,accent);
            if(ui.scroll>.05f)chevron(p,L.w*.055f,top+h*.50f,L.unit*.32f,-1,accent);
            if(ui.scroll<maxScroll(count)-.05f)chevron(p,L.w*.945f,top+h*.50f,L.unit*.32f,1,accent);
        }
        // Category objects stay in the foreground while their contents tumble out.
        for(int k=0;k<Accessories.SLOTS;k++) {
            float x=categoryX(L,k),y=categoryY(L),r=categoryR(L);
            float bounce=k==ui.slot?(float)Math.sin(Math.min(1,ui.pulse/.45f)*Math.PI)*.20f:0;
            float yy=y-r*(.035f*(float)Math.sin(c.clock*2+k)+bounce);
            int item=c.wardrobe.equipped(ui.who,k);if(item<0)item=Accessories.item(k,0);
            int color=Accessories.COLOR[item];
            p.fillEllipse(x,y+r*.78f,r*.72f,r*.12f,Glyph.withAlpha(color,k==ui.slot?90:30));
            if(k==ui.slot){p.fillCircle(x,yy,r*1.16f,Glyph.withAlpha(color,22));p.arc(x,yy,r*1.12f,r*1.12f,c.clock*27,255,Glyph.withAlpha(color,155),L.unit*.055f);}
            Painter q=new LinkedPairArt.TurnPainter(p,x,yy,.055f*(float)Math.sin(c.clock*1.7f+k));
            if(k==Accessories.FLAIR){AccessoryArt.star(q,0,0,r*.82f,color);AccessoryArt.star(q,-r*.12f,-r*.1f,r*.35f,0xFFFFF5CB);}
            else AccessoryArt.thumbnail(q,item,-r,-r,r,r,c.clock);
        }
    }
    private static void chevron(Painter p,float x,float y,float r,int dir,int color) {
        p.polyline(new float[]{x-dir*r*.4f,y-r,x+dir*r*.4f,y,x-dir*r*.4f,y+r},Glyph.withAlpha(color,160),r*.20f);
    }
    private static void lock(Painter p,float x,float y,float r,float fade) {
        int ink=Glyph.withAlpha(0xFFD4C8EA,(int)(fade*235));
        p.arc(x,y-r*.25f,r*.55f,r*.64f,180,180,ink,r*.20f);p.fillRect(x-r*.75f,y-r*.16f,x+r*.75f,y+r*.65f,ink);
    }
}
