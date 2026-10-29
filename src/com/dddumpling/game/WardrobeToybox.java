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
        contentsTray(p,c,L,ui,accent);
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
            if(ui.scroll>.05f)chevron(p,L.w*.055f,top+h*.50f,L.unit*.42f,-1,accent,c.clock);
            if(ui.scroll<maxScroll(count)-.05f)chevron(p,L.w*.945f,top+h*.50f,L.unit*.42f,1,accent,c.clock);
        }
        // Category objects stay in the foreground while their contents tumble out.
        for(int k=0;k<Accessories.SLOTS;k++) {
            float x=categoryX(L,k),y=categoryY(L),r=categoryR(L);
            float bounce=k==ui.slot?(float)Math.sin(Math.min(1,ui.pulse/.45f)*Math.PI)*.20f:0;
            float yy=y-r*(.035f*(float)Math.sin(c.clock*2+k)+bounce);
            int item=c.wardrobe.equipped(ui.who,k);if(item<0)item=Accessories.item(k,0);
            int color=Accessories.COLOR[Accessories.item(k,0)];
            p.fillEllipse(x,y+r*.88f,r*.96f,r*.16f,Glyph.withAlpha(color,k==ui.slot?65:25));
            if(k==ui.slot){p.fillCircle(x,yy,r*1.23f,Glyph.withAlpha(color,18));p.arc(x,yy,r*1.24f,r*1.20f,c.clock*27,255,Glyph.withAlpha(color,155),L.unit*.055f);}
            float open=k==ui.slot?travel(Math.min(1,ui.trayT/.32f)):0;
            if(k==ui.lastSlot && k!=ui.slot)open=1-travel(Math.min(1,ui.trayT/.24f));
            Painter q=new LinkedPairArt.TurnPainter(p,x,yy,.035f*(float)Math.sin(c.clock*1.7f+k));
            container(q,c,k,item,r,color,open,ui.trayT);
            if(k==ui.slot && ui.trayT<.65f)for(int spark=0;spark<3;spark++) {
                float age=ui.trayT/.65f,a=spark*2.1f+.4f;
                AccessoryArt.star(q,(float)Math.cos(a)*r*(.8f+age*.45f),-r*(.45f+age*.65f),
                        r*.12f*(1-age),Glyph.withAlpha(0xFFFFE9B0,(int)(210*(1-age))));
            }
        }
    }
    /** A persistent tray joins the category container to the row it owns. */
    private static void contentsTray(Painter p,GameCore c,Layout L,WardrobeScreen ui,int color) {
        float h=WardrobeScreen.trayHeight(L),top=WardrobeScreen.trayTop(L);
        float turn=travel(Math.min(1,ui.trayT/.34f)),anchor=categoryX(L,ui.slot);
        if(ui.lastSlot>=0 && ui.lastSlot!=ui.slot) {
            anchor=categoryX(L,ui.lastSlot)+(anchor-categoryX(L,ui.lastSlot))*turn;
            color=Glyph.mix(Accessories.COLOR[Accessories.item(ui.lastSlot,0)],color,turn);
        }
        float appear=ui.lastSlot<0?travel(Math.min(1,ui.trayT/.40f)):1;
        float rimY=top+h*.32f,neckY=categoryY(L)+categoryR(L)*.72f;
        float[] edge=new float[82],shape=new float[164];
        for(int i=0;i<=40;i++) {
            float x=L.w*(.045f+i*.91f/40),distance=(x-anchor)/(L.w*.065f);
            float rise=(float)Math.exp(-distance*distance*1.7f);
            float wave=(float)Math.sin(i*.22f+c.clock*.8f)*L.unit*.045f*(1-rise);
            float y=rimY-(rimY-neckY)*rise+wave;
            edge[i*2]=shape[i*2]=x;edge[i*2+1]=shape[i*2+1]=y;
            int j=81-i;shape[j*2]=x;
            shape[j*2+1]=settledY(L,x)+itemR(L,Accessories.count(ui.slot))*.76f;
        }
        p.fillPoly(shape,Glyph.withAlpha(color,(int)(19*appear)));
        p.polyline(edge,Glyph.withAlpha(color,(int)(20*appear)),L.unit*.38f);
        p.polyline(edge,Glyph.withAlpha(color,(int)(155*appear)),L.unit*.075f);
        // Edge and container share an accent; tiny travelling glints keep the connection alive.
        for(int side=-1;side<=1;side+=2) {
            float phase=(c.clock*.22f)%1;
            float x=anchor+(L.w*(side<0?.045f:.955f)-anchor)*phase;
            float distance=(x-anchor)/(L.w*.065f),rise=(float)Math.exp(-distance*distance*1.7f);
            float y=rimY-(rimY-neckY)*rise;
            float fade=(float)Math.sin(Math.PI*phase)*appear;
            p.fillCircle(x,y,L.unit*.12f,Glyph.withAlpha(color,(int)(40*fade)));
            p.fillCircle(x,y,L.unit*.045f,Glyph.withAlpha(0xFFFFEBC5,(int)(175*fade)));
        }
    }
    /** Open toy containers keep the category recognizable through its peeking accessory. */
    private static void container(Painter p,GameCore c,int kind,int item,float r,int tint,float open,float age) {
        float clock=c.clock;
        int base=Glyph.mix(tint,0xFF7C699E,.28f),light=Glyph.mix(base,0xFFFFF2D4,.44f);
        int shade=Glyph.mix(base,0xFF302A51,.45f),inside=Glyph.mix(base,0xFF211A38,.67f);
        float top=r*.06f,lift=r*(.30f+open*.38f);
        // Lid is behind both the peeking toy and the front wall.
        Painter lid=new LinkedPairArt.TurnPainter(p,-r*.08f,-lift,-.10f-open*.32f);
        if(kind==Accessories.FLAIR) {
            lid.fillEllipse(0,0,r*.57f,r*.17f,shade);
            lid.fillEllipse(0,-r*.07f,r*.57f,r*.17f,light);
            AccessoryArt.star(lid,0,-r*.26f,r*.22f,0xFFFFE5A2);
        } else if(kind==Accessories.HAT) {
            lid.fillEllipse(0,0,r*1.02f,r*.29f,shade);
            lid.fillEllipse(0,-r*.08f,r*1.02f,r*.29f,light);
            lid.arc(0,-r*.12f,r*.80f,r*.16f,190,135,Glyph.withAlpha(0xFFFFF5DF,150),r*.06f);
        } else {
            float width=kind==Accessories.GLASSES?1.04f:1f;
            lid.fillPoly(new float[]{-r*width,0,-r*.88f,-r*.24f,r*.84f,-r*.24f,r*width,0,r*.88f,r*.12f,-r*.88f,r*.12f},light);
            lid.line(-r*.86f,r*.08f,r*.86f,r*.08f,shade,r*.09f);
            if(kind==Accessories.MASK)lid.arc(0,-r*.22f,r*.30f,r*.18f,180,180,light,r*.12f);
        }
        p.fillEllipse(0,top,r*.96f,r*.27f,shade);
        p.fillEllipse(0,top-r*.02f,r*.81f,r*.17f,inside);
        float toyY=-r*(.28f+open*.20f),toyR=r*(.74f+open*.06f);
        if(kind==Accessories.FLAIR) {
            AccessoryArt.star(p,-r*.24f,toyY,r*.41f,0xFFFFE49D);
            AccessoryArt.star(p,r*.32f,toyY-r*.12f,r*.27f,0xFFDBB8FF);
        } else AccessoryArt.thumbnail(p,item,-toyR,toyY-toyR,toyR,toyY+toyR,clock);
        if(kind==Accessories.HAT) {
            p.fillPoly(new float[]{-r*.96f,top,r*.96f,top,r*.88f,r*.70f,r*.50f,r*.88f,-r*.50f,r*.88f,-r*.88f,r*.70f},base);
            p.fillEllipse(0,r*.65f,r*.88f,r*.25f,base);
            p.polyline(new float[]{-r*.78f,r*.22f,-r*.74f,r*.61f,-r*.53f,r*.70f},light,r*.11f);
            p.line(r*.59f,r*.22f,r*.54f,r*.71f,shade,r*.13f);
            p.arc(0,top,r*.96f,r*.25f,0,180,light,r*.12f);
        } else if(kind==Accessories.FLAIR) {
            // Faceted glass jar; the stars can still be seen through its blue front.
            p.fillPoly(new float[]{-r*.58f,-r*.06f,-r*.62f,r*.16f,-r*.81f,r*.36f,-r*.73f,r*.79f,
                    0,r*.91f,r*.73f,r*.79f,r*.81f,r*.36f,r*.62f,r*.16f,r*.58f,-r*.06f},0xBA788DB9);
            p.polyline(new float[]{-r*.46f,r*.12f,-r*.64f,r*.39f,-r*.57f,r*.66f},0xB4D6F9FF,r*.13f);
            p.polyline(new float[]{r*.60f,r*.28f,r*.67f,r*.44f,r*.60f,r*.71f},0x85705C9B,r*.12f);
            p.fillEllipse(0,-r*.01f,r*.61f,r*.15f,0xB5BECFE8);
            AccessoryArt.star(p,r*.17f,r*.48f,r*.23f,0xFFFFE6AB);
        } else {
            float height=kind==Accessories.GLASSES?.57f:.82f;
            p.fillPoly(new float[]{-r*.98f,top,r*.98f,top,r*.86f,r*height,-r*.84f,r*height},base);
            p.fillPoly(new float[]{r*.67f,top,r*.98f,top,r*.86f,r*height,r*.60f,r*height},shade);
            p.line(-r*.79f,r*.20f,-r*.72f,r*(height-.12f),light,r*.13f);
            p.line(-r*.90f,top+r*.04f,r*.88f,top+r*.04f,light,r*.12f);
            if(kind==Accessories.MASK) {
                p.line(-r*.51f,r*.13f,-r*.46f,r*.75f,light,r*.09f);
                p.line(r*.46f,r*.13f,r*.42f,r*.75f,light,r*.09f);
            } else if(kind==Accessories.BOOTS) {
                p.fillEllipse(r*.37f,r*.37f,r*.16f,r*.07f,inside);
            }
        }
        if(kind!=Accessories.FLAIR && open<1) {
            float yy=r*(kind==Accessories.GLASSES?.36f:.48f);
            Painter badge=new OpacityPainter(p,1-open);
            badge.fillEllipse(-r*.05f,yy,r*.18f,r*.15f,shade);
            AccessoryArt.star(badge,-r*.05f,yy-r*.01f,r*.12f,0xFFFFE3A3);
        }
        if(open>0)contents(p,c,kind,item,r,light,open,age);
    }
    private static void contents(Painter canvas,GameCore c,int kind,int item,float r,int trim,float open,float age) {
        Painter p=new OpacityPainter(canvas,open);
        float cy=r*(kind==Accessories.GLASSES?.34f:.46f),half=r*(kind==Accessories.GLASSES?.19f:.28f)*open;
        float left=-r*.68f,right=r*.58f,top=cy-half,bottom=cy+half;
        // A recessed glass window opens in the front wall. Only three tiny toys draw.
        p.fillRect(left-r*.055f,top-r*.055f,right+r*.055f,bottom+r*.055f,0xFF29233E);
        p.fillRect(left,top,right,bottom,0xFF423957);
        p.fillRect(left,cy,right,bottom,0xFF343049);
        p.save();p.clipRect(left,top,right,bottom);
        int count=Accessories.count(kind);
        float position=Accessories.variant(item)+Math.max(0,age-.55f)*.46f;
        int first=(int)Math.floor(position);float fraction=position-first;
        for(int offset=-1;offset<=1;offset++) {
            int variant=((first+offset)%count+count)%count,peek=Accessories.item(kind,variant);
            float dx=(offset-fraction)*r*.92f,x=(left+right)*.5f+dx;
            float visibility=Math.max(0,1-Math.abs(dx)/(r*1.12f));
            float bob=(float)Math.sin(c.clock*2.7f+variant*1.8f)*r*.025f;
            Painter toy=new LinkedPairArt.TurnPainter(new OpacityPainter(p,visibility*(c.wardrobe.owned[peek]?1:.30f)),x,cy+bob,
                    (float)Math.sin(c.clock*1.9f+variant)*.055f);
            float size=r*(kind==Accessories.GLASSES?.34f:.42f);
            AccessoryArt.thumbnail(toy,peek,-size,-half*.91f,size,half*.91f,c.clock);
        }
        float glint=((c.clock*.22f+kind*.21f)%1.5f-.25f)*r*1.7f;
        p.line(left+glint,top,left+glint-r*.24f,bottom,0x28DFECFF,r*.10f);
        p.restore();
        p.polyline(new float[]{left,bottom,left,top,right,top},Glyph.withAlpha(trim,215),r*.045f);
        p.polyline(new float[]{right,top,right,bottom,left,bottom},0xFF6C648C,r*.045f);
    }
    private static void chevron(Painter p,float x,float y,float r,int dir,int color,float clock) {
        float pulse=.5f-.5f*(float)Math.cos(clock*4.2f);
        x+=dir*r*.50f*pulse;r*=1+.12f*pulse;
        float[] points={x-dir*r*.4f,y-r,x+dir*r*.4f,y,x-dir*r*.4f,y+r};
        p.polyline(points,Glyph.withAlpha(color,(int)(18+25*pulse)),r*.65f);
        p.polyline(points,Glyph.withAlpha(Glyph.mix(color,0xFFFFF1D5,.24f),(int)(180+75*pulse)),r*.25f);
    }
    private static void lock(Painter p,float x,float y,float r,float fade) {
        int ink=Glyph.withAlpha(0xFFD4C8EA,(int)(fade*235));
        p.arc(x,y-r*.25f,r*.55f,r*.64f,180,180,ink,r*.20f);p.fillRect(x-r*.75f,y-r*.16f,x+r*.75f,y+r*.65f,ink);
    }
}
