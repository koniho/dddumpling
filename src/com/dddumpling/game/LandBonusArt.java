package com.dddumpling.game;

/** Small scenery discoveries, with a short collection burst behind the falling prompts. */
final class LandBonusArt extends Draw {
    static final float SQUIRREL_SCALE=.76f;
    private static final int[] COLORS={SlimeHills.MINT,CrystalField.edgeColor(2),0xFFFFBCE8,MushroomForest.WOOD_LIGHT,0,0xFFFF9EBF,0xFFFFE57B};
    private static final int[] RAINBOW={0xFFFF91B7,0xFFFFC26D,0xFFFFEC83,0xFF92EDBC,0xFF85DAFF,0xFFC5ACFF};
    static void releaseIcon(Painter p,float x,float y,float r,float time,float hit) {
        float size=r*.72f,baseY=y+r*.12f;
        float reaction=hit<0?0:Math.min(1,hit/.2f);
        float pop=hit<0?0:(float)(Math.sin(hit*18)*Math.exp(-hit*4));
        float lift=hit<0?0:size*.55f*(float)Math.sin(Math.min(1,hit/.85f)*Math.PI);
        flamingo(p,x,baseY-lift,size*(1+pop*.22f),time,reaction,false,baseY+size*.86f,size);
        if(hit<0 || hit>=LandBonus.BURST)return;
        float fade=1-hit/LandBonus.BURST;
        for(int i=0;i<8;i++) {
            float a=i*.7854f,travel=r*(.65f+hit*.8f);
            mote(p,Lands.SWAMP,x+(float)Math.cos(a)*travel,
                    y+(float)Math.sin(a)*travel,r*.07f*fade,a,Glyph.withAlpha(GOLD,(int)(230*fade)));
        }
        p.text("+"+LandBonus.POINTS,x,y-r*(.85f+hit*.2f),r*.40f,
                Glyph.withAlpha(GOLD,(int)(255*fade)),Painter.CENTER,true);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        int land=Lands.forStage(c.stage);
        if(land!=MushroomForest.LAND && land!=Lands.SWAMP && ((land!=CrystalField.LAND && land!=Underwater.LAND) || c.landBonus.burst>=0))drawBonus(p,c,L);
    }
    static void inSwamp(Painter p,GameCore c,Layout L,float fade) {
        if(Lands.forStage(c.stage)==Lands.SWAMP)drawBonus(new OpacityPainter(p,fade),c,L);
    }
    static void beneathForest(Painter p,GameCore c,Layout L,float fade) {
        if(Lands.forStage(c.stage)==MushroomForest.LAND)drawBonus(new OpacityPainter(p,fade),c,L);
    }
    static void beneathCrystal(Painter p,GameCore c,Layout L) {
        if(Lands.forStage(c.stage)==CrystalField.LAND && c.landBonus.burst<0)drawBonus(p,c,L);
    }
    static void onSeabed(Painter p,GameCore c,Layout L,float fade) {
        if(Lands.forStage(c.stage)==Underwater.LAND && c.landBonus.burst<0)drawBonus(new OpacityPainter(p,fade),c,L);
    }
    private static void drawBonus(Painter p,GameCore c,Layout L) {
        LandBonus b=c.landBonus;
        if(!LandBonus.scene(c) || !b.visible(c) && b.burst<0)return;
        int land=Lands.forStage(c.stage),color=COLORS[land];
        boolean still=c.cloudUi.reduceMotion;
        float x=LandBonus.x(c,L),y=LandBonus.y(c,L),r=LandBonus.size(c,L);
        float t=still?0:b.age,hit=b.burst;
        if(land==Underwater.LAND && hit>=0 && !still) {
            float u=Math.min(1,hit/.55f),ease=1-(1-u)*(1-u)*(1-u);
            x+=(L.w*.5f-x)*ease;
            y+=(L.playTop+(L.deckTop-L.playTop)*.58f-y)*ease;
            r+=(L.w*.13f-r)*ease;
        }
        float rewardX=x,rewardY=y;
        float fade=hit<0?Math.min(1,(b.age-b.delay)/.28f):Math.min(1,(LandBonus.BURST-hit)/.35f);
        fade*=Math.max(0,Math.min(1,(L.deckTop+r-y)/(r*2)));
        if(fade<=0)return;
        p.save();p.clipRect(0,0,L.w,L.deckTop);
        Painter q=new OpacityPainter(p,fade*(hit<0 && land!=0?.68f:1));
        if(land==Underwater.LAND && hit<0) {
            final float clarity=.25f+.75f*Math.min(1,UnderwaterVista.depth(LandBonus.clamTravel(c))*2.8f);
            q=new OpacityPainter(q,1) {int color(int color) {return UnderwaterVista.depthColor(color,clarity);}};
        }
        float glow=hit<0?.08f:1;
        for(int i=3;i>0;i--)q.fillCircle(x,y,r*(1+i*.12f),Glyph.withAlpha(color,(int)(8*glow)));
        float pop=hit<0 || still?0:(float)(Math.sin(hit*18)*Math.exp(-hit*4));
        float scale=1+pop*.22f;
        float reaction=hit<0?0:Math.min(1,hit/.2f);
        if(hit<0 && land>2)ambient(new OpacityPainter(q,.25f),land,x,y,r,t,still);
        float lift=hit<0 || still?0:r*.55f*(float)Math.sin(Math.min(1,hit/.85f)*Math.PI);
        if(land<=1 && hit>=0 && !still) {
            lift=r*1.2f*Math.abs((float)Math.sin(hit*Math.PI/.42f))*settle(hit);
            x+=r*sway(hit);
        }
        if(land==MushroomForest.LAND)lift=0;
        y-=lift;
        switch(land) {
            case 0:bubble(q,c,L,x,y,r*scale,t,reaction);break;
            case 1:
                if(hit>=0 && !still)jewel(new LinkedPairArt.TurnPainter(q,x,y,sway(hit)),0,0,r*scale,t,reaction,still,true);
                else jewel(q,x,y,r*scale,t,reaction,still,hit>=0);
                break;
            case 2:clam(q,x,y,r*scale,t,reaction,still);break;
            case 3:squirrel(q,x,y,r,t,hit,still);break;
            case Lands.SWAMP:flamingo(q,x,y,r*scale,t,reaction,still,rewardY+r*.86f,r);break;
            case Lands.RAINBOW:coin(q,x,y,r*scale,t,hit,still);break;
        }
        if(hit<0 && land>2) {
            float twinkle=still?.8f:.55f+.45f*(float)Math.sin(t*4);
            q.fillPoly(star(x+r*.84f,y-r*.72f,r*(.045f+.025f*twinkle),r*.02f,4,0),0x99FFF3C9);
        } else if(hit>=0)celebrate(p,L,land,rewardX,rewardY,Math.max(r,LandBonus.radius(L)*.72f),color,hit,still);
        p.restore();
    }
    private static float settle(float hit) {
        return (float)Math.exp(-hit*1.8f)*Math.max(0,1-hit/LandBonus.BURST);
    }
    private static float sway(float hit) {return .30f*(float)Math.sin(hit*13)*settle(hit);}
    private static void celebrate(Painter p,Layout L,int land,float x,float y,float r,int color,float t,boolean still) {
        float u=t/LandBonus.BURST,fade=1-u;
        if(t<.2f)p.fillRect(0,0,L.w,L.deckTop,
                Glyph.withAlpha(color,(int)((still?14:38)*(1-t/.2f))));
        float spread=still?1.2f:.5f+2.4f*(float)Math.sqrt(u);
        p.strokeCircle(x,y,r*spread,Glyph.withAlpha(color,(int)(180*fade)),r*.09f*fade);
        for(int i=0;i<18;i++) {
            float a=i*2.39996f,travel=spread*(.65f+(i%3)*.19f);
            float px=x+(float)Math.cos(a)*r*travel,py=y+(float)Math.sin(a)*r*travel;
            int tint=land==Lands.RAINBOW?RAINBOW[i%6]:land==Lands.SWAMP?(i%2==0?0xFFFFCBE3:color):i%3==0?0xFFFFF4C9:i%3==1?color:GOLD;
            float size=r*(i%3==0?.12f:.065f)*fade;
            p.fillCircle(px,py,size*2.2f,Glyph.withAlpha(tint,(int)(45*fade)));
            mote(p,land,px,py,size*1.5f,still?0:a+t*2,Glyph.withAlpha(tint,(int)(245*fade)));
        }
        if(land==Lands.RAINBOW)for(int i=0;i<6;i++)
            p.arc(x,y,r*(spread+i*.08f),r*(spread+i*.08f),190,160,
                    Glyph.withAlpha(RAINBOW[i],(int)(160*fade)),r*.06f);
        float yy=Math.max(L.playTop+r*.6f,y-r*(1.5f+(still?0:u*.65f)));
        p.text("+"+LandBonus.POINTS,x,yy+r*.035f,r*.54f,Glyph.withAlpha(0xFF302440,(int)(255*fade)),Painter.CENTER,true);
        p.text("+"+LandBonus.POINTS,x,yy,r*.54f,Glyph.withAlpha(0xFFFFF3C9,(int)(255*fade)),Painter.CENTER,true);
    }
    private static void ambient(Painter p,int land,float x,float y,float r,float time,boolean still) {
        for(int i=0;i<4;i++) {
            float cycle=still?.5f:(time*.33f+i*.25f)%1;
            float a=i*2.39996f+time*.25f;
            float px=x+(float)Math.cos(a)*r*(.85f+cycle*.35f);
            float py=y-r*(.3f+cycle*1.1f);
            int color=land==Lands.RAINBOW?RAINBOW[i]:COLORS[land];
            int alpha=(int)(150*Math.sin(cycle*Math.PI));
            mote(p,land,px,py,r*.08f,a,Glyph.withAlpha(color,alpha));
        }
    }
    private static void mote(Painter p,int land,float x,float y,float size,float angle,int color) {
        if(land==0 || land==2) {
            p.fillCircle(x,y,size,Glyph.withAlpha(color,((color>>>24)*35)/100));
            p.strokeCircle(x,y,size,color,size*.20f);
            p.fillCircle(x-size*.30f,y-size*.30f,size*.22f,color);
        } else if(land==1) {
            p.fillPoly(new float[]{x,y-size*1.5f,x+size*.65f,y,x,y+size*1.5f,x-size*.65f,y},color);
            p.line(x,y-size,x,y+size,0x66FFFFFF,size*.13f);
        } else if(land==3 || land==Lands.SWAMP) {
            float dx=(float)Math.cos(angle)*size,dy=(float)Math.sin(angle)*size;
            p.fillPoly(new float[]{x-dx,y-dy,x+dy*.55f,y-dx*.55f,x+dx,y+dy,x-dy*.55f,y+dx*.55f},color);
            p.line(x-dx*1.2f,y-dy*1.2f,x+dx*.8f,y+dy*.8f,color,size*.13f);
        } else p.fillPoly(star(x,y,size,size*.36f,4,angle),color);
    }
    private static void eyes(Painter p,float x,float y,float r,boolean happy) {
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.24f;
            if(happy)p.arc(ex,y,r*.10f,r*.07f,190,160,0xFF49344F,r*.045f);
            else {
                p.fillEllipse(ex,y,r*.075f,r*.105f,0xFF49344F);
                p.fillCircle(ex-r*.02f,y-r*.035f,r*.025f,0xFFFFFFFF);
            }
            p.fillEllipse(x+side*r*.39f,y+r*.13f,r*.10f,r*.055f,0xAFFF9CB6);
        }
        p.arc(x,y+r*.12f,r*.10f,r*.08f,0,180,0xFF49344F,r*.035f);
    }
    private static void bubble(Painter p,GameCore c,Layout L,float x,float y,float r,float time,float hit) {
        LandBonus bonus=c.landBonus;float surface=LandBonus.surfaceY(c,L),rise=bonus.bubbleRise(c);
        boolean emerging=hit==0 && rise<1;
        if(!emerging)p.fillEllipse(x,surface,r*.75f,r*.12f,0x25213B26);
        p.save();
        if(emerging)p.clipRect(0,0,L.w,surface);
        if(bonus.burst>=0 && !c.cloudUi.reduceMotion)
            zucchini(new LinkedPairArt.TurnPainter(p,x,y,sway(bonus.burst)),0,0,r,time,hit);
        else zucchini(p,x,y,r,time,hit);
        if(hit<.8f) {
            SlimeBubbles.Bubble shell=new SlimeBubbles.Bubble();
            shell.age=c.cloudUi.reduceMotion?0:Math.max(0,bonus.age-bonus.delay);
            shell.seed=bonus.bubbleSeed();shell.r=r;
            c.slimeBubbles.bubbleShell(p,shell,x,y,1);
        }
        p.restore();
        if(emerging) {
            float chord=r*(float)Math.sin(Math.PI*rise);
            p.arc(x,surface,chord,r*.11f,0,180,Glyph.withAlpha(SlimeHills.MINT,40),r*.023f);
        }
        if(hit>0)for(int i=0;i<6;i++) {
            float a=i*1.0472f,rr=r*(.6f+hit*.6f);
            p.fillCircle(x+(float)Math.cos(a)*rr,y+(float)Math.sin(a)*rr,r*.075f*(1-hit*.6f),Glyph.withAlpha(SlimeHills.MINT,150));
        }
    }
    private static void zucchini(Painter p,float x,float y,float r,float time,float hit) {
        // Rounded blossom end left; the curved neck and fluted stem rise on the right.
        float bow=.36f+.025f*(float)Math.sin(time*2.4f),tipY=y+r*(.16f-bow);
        float[] stem={x+r*.47f,tipY+r*.04f,x+r*.55f,tipY-r*.16f,
                x+r*.60f,tipY-r*.37f,x+r*.72f,tipY-r*.31f,
                x+r*.70f,tipY-r*.10f,x+r*.70f,tipY+r*.08f};
        p.fillPoly(stem,0xFF83A744);p.strokePoly(stem,0xFF243D20,r*.042f);
        p.polyline(new float[]{x+r*.54f,tipY,x+r*.63f,tipY-r*.17f,x+r*.64f,tipY-r*.30f},0xFFD0D982,r*.045f);
        p.line(x+r*.66f,tipY-r*.02f,x+r*.68f,tipY-r*.25f,0xFF496B2C,r*.028f);
        float[] skin=zucchiniBand(x,y,r,bow,-1,1);
        p.fillPoly(skin,0xFF69A13D);
        p.fillPoly(zucchiniBand(x,y,r,bow,.35f,1),0xFF497A30);
        p.fillPoly(zucchiniBand(x,y,r,bow,-.90f,-.28f),0xFF8ABB53);
        for(float rib:new float[]{-.65f,-.05f,.52f}) {
            float[] stripe=new float[34];
            for(int i=0;i<17;i++) {
                float u=i/16f,half=(.30f-.09f*u)*(float)Math.sqrt(Math.max(0,1-(2*u-1)*(2*u-1)));
                stripe[i*2]=x+r*(-.72f+1.30f*u);
                stripe[i*2+1]=y+r*(.22f-.06f*u-bow*u*u*u+rib*half);
            }
            p.polyline(stripe,rib<0?0xFF628E37:0xFF365F2B,r*.026f);
        }
        for(int i=0;i<30;i++) {
            float u=.07f+.86f*hash(251+i*19),v=-.80f+1.60f*hash(433+i*31);
            float half=(.30f-.09f*u)*(float)Math.sqrt(1-(2*u-1)*(2*u-1));
            float xx=x+r*(-.72f+1.30f*u),yy=y+r*(.22f-.06f*u-bow*u*u*u+v*half);
            p.fillEllipse(xx,yy,r*(.012f+.009f*hash(719+i)),r*.018f,0xB6B6D66B);
        }
        p.strokePoly(skin,0xFF243D20,r*.042f);
        p.fillEllipse(x-r*.70f,y+r*.22f,r*.035f,r*.066f,0xFF263D20);
        p.fillEllipse(x-r*.704f,y+r*.21f,r*.015f,r*.037f,0xFFE4BA69);
        p.fillEllipse(x-r*.46f,y-r*.015f,r*.05f,r*.019f,0xD5F2F4CC);
        p.fillEllipse(x-r*.34f,y-r*.035f,r*.026f,r*.014f,0xC2F2F4CC);
    }
    private static float[] zucchiniBand(float x,float y,float r,float bow,float top,float bottom) {
        float[] shape=new float[68];
        for(int i=0;i<17;i++) {
            float u=i/16f,half=(.30f-.09f*u)*(float)Math.sqrt(Math.max(0,1-(2*u-1)*(2*u-1)));
            float xx=x+r*(-.72f+1.30f*u),cy=y+r*(.22f-.06f*u-bow*u*u*u);
            shape[i*2]=xx;shape[i*2+1]=cy+r*half*top;
            shape[(33-i)*2]=xx;shape[(33-i)*2+1]=cy+r*half*bottom;
        }
        return shape;
    }
    private static void jewel(Painter p,float x,float y,float r,float time,float hit,boolean still,boolean collected) {
        float cy=y,pulse=still?.5f:.5f+.5f*(float)Math.sin(time*1.7f);
        int shade=CrystalField.edgeColor(0),mid=CrystalField.edgeColor(1),light=CrystalField.edgeColor(2);
        int ink=Glyph.mix(shade,Lands.BG[CrystalField.LAND],.80f);
        float[] outline={0,-.82f,.62f,-.10f,.40f,.59f,0,.80f,-.40f,.59f,-.62f,-.10f};
        float[] face={0,-.44f,.45f,-.06f,.27f,.48f,0,.64f,-.27f,.48f,-.45f,-.06f};
        for(int i=0;i<outline.length;i+=2) {
            outline[i]=x+outline[i]*r;outline[i+1]=cy+outline[i+1]*r;
            face[i]=x+face[i]*r;face[i+1]=cy+face[i+1]*r;
        }
        for(int i=4;i>0;i--) {
            p.fillEllipse(x,cy,r*(.5f+i*.15f),r*(.75f+i*.12f),
                    Glyph.withAlpha(light,(int)(3+4*pulse+hit*8)));
            p.strokePoly(outline,Glyph.withAlpha(light,(int)(7+4*pulse)),r*.07f*i*(1-hit*.8f));
        }
        // The friendly prism stays translucent beneath the floor until collected.
        Painter q=new OpacityPainter(p,.44f+.08f*pulse+hit*.48f);
        q.fillPoly(outline,shade);
        int[] facets={light,mid,Glyph.mix(shade,ink,.25f),Glyph.mix(shade,ink,.45f),shade,Glyph.mix(mid,light,.4f)};
        for(int i=0;i<6;i++) {
            int j=(i+1)%6,a=i*2,b=j*2;
            q.fillPoly(new float[]{outline[a],outline[a+1],outline[b],outline[b+1],
                    face[b],face[b+1],face[a],face[a+1]},facets[i]);
            q.line(outline[a],outline[a+1],face[a],face[a+1],ink,r*.025f);
        }
        q.fillPoly(face,mid);
        q.strokePoly(face,ink,r*.025f);q.strokePoly(outline,ink,r*.058f);
        q.fillPoly(new float[]{x,cy-r*.78f,x+r*.49f,cy-r*.13f,x+r*.26f,cy-r*.25f},Glyph.withAlpha(Glyph.mix(light,0xFFFFFFFF,.4f),108));
        q.fillPoly(new float[]{x-r*.56f,cy-r*.10f,x-r*.02f,cy-r*.76f,x-r*.24f,cy-r*.22f},Glyph.withAlpha(light,69));
        if(!collected)return;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.21f,ey=cy+r*.13f;
            q.fillCircle(ex,ey,r*(.105f+hit*.008f),ink);
            q.fillCircle(ex+r*.014f,ey+r*.020f,r*.065f,Glyph.mix(shade,ink,.45f));
            q.fillCircle(ex-r*.024f,ey-r*.029f,r*.043f,0xFFF4FFFF);
            q.fillCircle(ex+r*.038f,ey+r*.043f,r*.016f,light);
            q.fillEllipse(x+side*r*.31f,cy+r*.28f,r*.058f,r*.027f,0xFFF0B8D0);
        }
        float[] smile=new float[18];
        for(int i=0;i<9;i++) {
            float a=(float)Math.PI*i/8;
            smile[i*2]=x+(float)Math.cos(a)*r*.145f;
            smile[i*2+1]=cy+r*.31f+(float)Math.sin(a)*r*(.13f+hit*.035f);
        }
        q.fillPoly(smile,0xFFFFD879);q.strokePoly(smile,ink,r*.033f);
    }
    private static void clam(Painter p,float x,float y,float r,float time,float hit,boolean still) {
        float open=.40f+.19f*(float)Math.sin(time*2.5f)+hit*.30f;
        for(int i=3;i>0;i--)p.fillEllipse(x,y+r*.32f,r*(.80f+i*.05f),r*(.35f+i*.04f),0x08BD8BD3);
        p.fillEllipse(x,y+r*.80f,r*.85f,r*.10f,0x302A4660);
        p.fillEllipse(x,y-r*open,r*.88f,r*.43f,0xFFBD8BD3);
        for(int i=0;i<7;i++) {
            float a=(float)Math.PI*(1.10f+i*.80f/6f);
            float ex=x+(float)Math.cos(a)*r*.86f,ey=y-r*open+(float)Math.sin(a)*r*.42f;
            p.line(x,y-r*.12f,ex,ey,0xFFE8B8EC,r*.07f);
        }
        p.fillEllipse(x,y+r*.35f,r*.88f,r*.38f,0xFFB57AD0);
        p.fillEllipse(x,y+r*.25f,r*.85f,r*.24f,0xFFFFC6E3);
        p.fillEllipse(x,y+r*.23f,r*.65f,r*.14f,0xFFBA7CBB);
        float py=y-r*.01f-(still?0:hit*r*.62f);
        p.fillCircle(x,py,r*.32f,0xFFFFE0EC);
        p.fillCircle(x-r*.04f,py-r*.04f,r*.25f,0xFFFFF6F2);
        p.fillCircle(x-r*.12f,py-r*.14f,r*.075f,0xFFFFFFFF);
        eyes(p,x,y+r*.46f,r*.45f,hit>0 || time%4>3.8f);
    }
    private static void squirrel(Painter p,float x,float y,float r,float time,float elapsed,boolean still) {
        // Forest-colored haze softens the silhouette; collection gently clears it.
        float reveal=elapsed<0?0:Math.min(1,elapsed/.22f);
        final float haze=.44f-reveal*.32f;
        p=new OpacityPainter(p,.78f+reveal*.22f) {
            int color(int color) {
                return super.color(Glyph.withAlpha(Glyph.mix(color,Lands.BG[MushroomForest.LAND],haze),color>>>24));
            }
        };
        // The hollow opening faces left, with split bark and a little branch on the right.
        p.fillEllipse(x,y+r*.78f,r*1.04f,r*.13f,0x26332925);
        float[] bark=forestShape(x,y,r,-.68f,-.13f,.38f,-.23f,.83f,-.32f,.96f,-.20f,
                .82f,-.09f,1.02f,-.05f,1.06f,.34f,.86f,.43f,1.01f,.47f,.91f,.65f,-.66f,.82f);
        p.fillPoly(bark,MushroomForest.WOOD_DARK);p.strokePoly(bark,0xFF3C3E32,r*.035f);
        p.fillPoly(forestShape(x,y,r,.18f,-.18f,.46f,-.38f,.52f,-.66f,.58f,-.67f,
                .57f,-.43f,.75f,-.54f,.79f,-.49f,.58f,-.27f,.45f,-.13f),MushroomForest.WOOD);
        p.polyline(forestShape(x,y,r,.28f,-.14f,.48f,-.33f,.54f,-.54f),MushroomForest.WOOD_DARK,r*.045f);
        for(int i=0;i<5;i++) {
            float yy=-.04f+i*.15f,xx=-.42f+(i%2)*.18f;
            p.polyline(forestShape(x,y,r,xx,yy,xx+.25f,yy+.035f,xx+.55f,yy+.01f,xx+.86f,yy-.045f),
                    i%2==0?MushroomForest.WOOD:0xFF4B493A,r*(i%2==0?.024f:.035f));
        }
        p.fillEllipse(x-r*.66f,y+r*.33f,r*.40f,r*.50f,0xFF88754F);
        p.fillEllipse(x-r*.68f,y+r*.32f,r*.33f,r*.405f,0xFF172922);
        p.fillPoly(forestShape(x,y,r,-.91f,.16f,-.65f,.24f,-.79f,.29f),MushroomForest.WOOD_DARK);
        p.fillPoly(forestShape(x,y,r,-.78f,.77f,-.38f,.65f,-.27f,.70f,-.64f,.84f),MushroomForest.WOOD_LIGHT);

        float logR=r;r*=SQUIRREL_SCALE;
        boolean collected=elapsed>=0;
        float u=collected?Math.min(1,elapsed/.38f):0;
        float raise=collected?(still?1:u*u*(3-2*u)):0;
        float bounce=collected && !still?Math.abs((float)Math.sin(elapsed*Math.PI/.40f))*settle(elapsed):0;
        float cx=x-logR*.68f,cy=y+logR*.25f-r*(.02f+raise*.26f+bounce*.48f);
        if(!still && !collected)cy+=r*.035f*(float)Math.sin(time*2.3f);
        int fur=MushroomForest.WOOD,light=MushroomForest.WOOD_LIGHT,cream=0xFFC1C6A4,ink=MushroomForest.WOOD_DARK;
        // A compact curl peeks over the far side, clear of the log's branch.
        forestOval(p,cx+r*.40f,cy-r*.23f,r*.25f,r*.32f,fur);
        p.fillEllipse(cx+r*.46f,cy-r*.30f,r*.16f,r*.17f,cream);
        p.arc(cx+r*.47f,cy-r*.29f,r*.09f,r*.10f,5,300,ink,r*.03f);
        forestOval(p,cx,cy+r*.30f,r*.32f,r*.39f,fur);
        p.fillEllipse(cx,cy+r*.35f,r*.23f,r*.29f,cream);
        for(int side=-1;side<=1;side+=2) {
            forestOval(p,cx+side*r*.29f,cy-r*.49f,r*.14f,r*.23f,fur);
            p.fillEllipse(cx+side*r*.29f,cy-r*.50f,r*.082f,r*.16f,0xFFAB8C7C);
        }
        forestOval(p,cx,cy-r*.12f,r*.43f,r*.39f,light);
        for(int side=-1;side<=1;side+=2) {
            float ex=cx+side*r*.205f;
            p.fillEllipse(ex,cy-r*.10f,r*.18f,r*.27f,cream);
            p.fillEllipse(cx+side*r*.28f,cy+r*.10f,r*.18f,r*.13f,cream);
            boolean blink=!collected && !still && time%4.8f>4.65f;
            if(blink)p.arc(ex,cy-r*.08f,r*.11f,r*.045f,190,160,ink,r*.035f);
            else {
                p.fillEllipse(ex,cy-r*.09f,r*.125f,r*.16f,0xFF30382F);
                p.fillEllipse(ex-r*.035f,cy-r*.15f,r*.051f,r*.050f,0xFFE0DDC1);
            }
            p.fillEllipse(cx+side*r*.31f,cy+r*.08f,r*.068f,r*.036f,0xFFAB8C7C);
        }
        p.fillEllipse(cx,cy+r*.10f,r*.17f,r*.095f,cream);
        p.fillEllipse(cx,cy+r*.015f,r*.045f,r*.032f,0xFF30382F);
        for(int side=-1;side<=1;side+=2)p.arc(cx+side*r*.055f,cy+r*.04f,r*.055f,r*.045f,0,180,ink,r*.022f);
        // The solid front lip occludes the lower body; paws and acorn rest in front.
        float[] lip=forestShape(x,y,logR,-1.04f,.39f,-.90f,.46f,-.68f,.49f,-.46f,.46f,-.28f,.39f,
                -.31f,.59f,-.45f,.76f,-.66f,.83f,-.89f,.74f,-1.03f,.57f);
        p.fillPoly(lip,MushroomForest.WOOD_DARK);
        p.polyline(forestShape(x,y,logR,-1.04f,.39f,-.90f,.46f,-.68f,.49f,-.46f,.46f,-.28f,.39f),
                0xFF88754F,logR*.065f);
        p.polyline(forestShape(x,y,logR,-.93f,.57f,-.76f,.63f,-.52f,.61f),0xFF88754F,logR*.026f);
        p.line(x-logR*.43f,y+logR*.57f,x-logR*.48f,y+logR*.72f,0xFF3C3E32,logR*.025f);
        // Both hands stay attached to the acorn as it rises above the ears.
        float acornY=cy+r*(.31f-raise*1.23f);
        for(int side=-1;side<=1;side+=2) {
            float shoulderX=cx+side*r*.26f,shoulderY=cy+r*.24f;
            float handX=cx+side*r*.16f,handY=acornY+r*.015f;
            float elbowX=cx+side*r*.55f,elbowY=cy+r*(.42f-raise*.88f);
            forestArm(p,shoulderX,shoulderY,elbowX,elbowY,handX,handY,r,fur);
        }
        forestOval(p,cx,acornY+r*.07f,r*.135f,r*.19f,0xFF88754F);
        p.fillEllipse(cx-r*.025f,acornY+r*.08f,r*.072f,r*.12f,MushroomForest.WOOD_LIGHT);
        p.line(cx,acornY-r*.14f,cx+r*.015f,acornY-r*.23f,ink,r*.055f);
        forestOval(p,cx,acornY-r*.065f,r*.165f,r*.11f,MushroomForest.WOOD_DARK);
        for(int side=-1;side<=1;side+=2)
            forestOval(p,cx+side*r*.19f,acornY+r*.025f,r*.11f,r*.10f,light);
    }
    private static void forestArm(Painter p,float ax,float ay,float bx,float by,float cx,float cy,float r,int fur) {
        // A plump curved sleeve with rounded ends, instead of a hinged elbow.
        float[] shape=new float[52];
        for(int layer=0;layer<2;layer++) {
            float width=r*(layer==0?.12f:.085f);
            for(int i=0;i<=12;i++) {
                float t=i/12f,u=1-t,x=u*u*ax+2*u*t*bx+t*t*cx,y=u*u*ay+2*u*t*by+t*t*cy;
                float dx=2*(u*(bx-ax)+t*(cx-bx)),dy=2*(u*(by-ay)+t*(cy-by));
                float length=(float)Math.sqrt(dx*dx+dy*dy),bulge=width*(1+.18f*(float)Math.sin(t*Math.PI));
                float nx=-dy/length*bulge,ny=dx/length*bulge;
                shape[i*2]=x+nx;shape[i*2+1]=y+ny;
                shape[(25-i)*2]=x-nx;shape[(25-i)*2+1]=y-ny;
            }
            int color=layer==0?MushroomForest.WOOD_DARK:fur;
            p.fillPoly(shape,color);p.fillCircle(ax,ay,width,color);p.fillCircle(cx,cy,width,color);
        }
    }
    private static float[] forestShape(float x,float y,float r,float... points) {
        for(int i=0;i<points.length;i+=2){points[i]=x+points[i]*r;points[i+1]=y+points[i+1]*r;}
        return points;
    }
    private static void forestOval(Painter p,float x,float y,float rx,float ry,int color) {
        float line=Math.min(rx,ry)*.13f;
        p.fillEllipse(x,y,rx+line,ry+line,MushroomForest.WOOD_DARK);p.fillEllipse(x,y,rx,ry,color);
    }
    private static void flamingo(Painter p,float x,float y,float r,float time,float hit,boolean still,float waterY,float groundR) {
        float flap=(float)Math.sin(time*(hit>0?14:1.65f)),hop=still?0:hit*r*.13f;
        final float haze=.32f-hit*.26f;
        p=new OpacityPainter(p,.88f+hit*.12f) {
            int color(int color) {return super.color(Glyph.mix(color,Lands.BG[Lands.SWAMP],haze));}
        };
        y-=hop;
        // Water belongs to the scenery, independent of the bird's hop and scale pop.
        float lift=Math.max(0,waterY-groundR*.86f-y),extension=still?0:Math.min(1,lift/(groundR*.35f));
        float leftWater=x-groundR*.11f,rightWater=x+groundR*.16f;
        for(float foot:new float[]{leftWater,rightWater}) {
            p.fillEllipse(foot,waterY,groundR*.14f,groundR*.035f,0x223C8C95);
            p.arc(foot,waterY,groundR*.14f,groundR*.04f,180,180,0x6669CED1,groundR*.018f);
        }
        float footY=waterY-lift*.70f;
        float leftFoot=leftWater-groundR*.05f*extension,rightFoot=rightWater+groundR*.07f*extension;
        float idle=still?0:1-hit;
        // Slow weight shifts and breathing flex the legs without lifting either foot.
        x+=idle*r*.022f*(float)Math.sin(time*.8f+.4f);
        y+=idle*r*.025f*(float)Math.sin(time*1.65f);
        float headDX=idle*r*(.075f*(float)Math.sin(time*.75f)+.025f*(float)Math.sin(time*1.7f));
        float headDY=idle*r*.04f*(float)Math.sin(time*.75f+.6f);
        p.line(x-r*.11f,y+r*.26f,leftFoot,footY,0xFFE7A0A9,r*.055f);
        p.polyline(new float[]{x+r*.10f,y+r*.25f,x+groundR*(.30f-.13f*extension),
                (y+r*.25f+footY)*.5f,rightFoot,footY},0xFFF6A6B6,r*.055f);
        if(extension>0) {
            p.line(leftFoot,footY,leftFoot+groundR*.13f*extension,footY-groundR*.025f*extension,0xFFE7A0A9,r*.045f);
            p.line(rightFoot,footY,rightFoot+groundR*.13f*extension,footY-groundR*.025f*extension,0xFFF6A6B6,r*.045f);
        }
        p.fillEllipse(x-r*.10f,y+r*.03f,r*.51f,r*.32f,0xFFF58AB5);
        // A shaded belly and soft upper light give the resting silhouette volume.
        for(int i=0;i<3;i++)p.fillEllipse(x-r*.12f,y+r*(.10f+i*.035f),r*(.44f-i*.045f),r*(.20f-i*.035f),0x28A75E89);
        p.fillEllipse(x-r*.17f,y-r*.10f,r*.31f,r*.11f,0x55FFD5DF);
        p.fillPoly(new float[]{x-r*.45f,y+r*.02f,x-r*.72f,y-r*.15f,x-r*.58f,y+r*.19f},0xFFFFB4D0);
        float[] neck=new float[50];
        for(int i=0;i<=24;i++) {
            float t=i/24f,u=1-t;
            neck[i*2]=x+r*(u*u*u*.18f+3*u*u*t*.65f-3*u*t*t*.05f+t*t*t*.43f)+headDX*t*t;
            neck[i*2+1]=y+r*(u*u*u*.03f-3*u*u*t*.20f-3*u*t*t*.67f-t*t*t*.64f)+headDY*t*t;
        }
        p.polyline(neck,0xFFC5789D,r*.20f);
        p.polyline(neck,0xFFF58AB5,r*.15f);
        for(int i=0;i<neck.length;i+=2)neck[i]-=r*.035f;
        p.polyline(neck,0x66FFD4DF,r*.045f);
        float hx=x+headDX,hy=y+headDY;
        p.fillCircle(hx+r*.40f,hy-r*.65f,r*.20f,0xFFFFABD0);
        p.fillEllipse(hx+r*.42f,hy-r*.57f,r*.15f,r*.09f,0x44BA739C);
        p.fillEllipse(hx+r*.34f,hy-r*.74f,r*.075f,r*.035f,0x77FFE4EA);
        p.fillPoly(new float[]{hx+r*.55f,hy-r*.65f,hx+r*.79f,hy-r*.58f,hx+r*.68f,hy-r*.38f,
                hx+r*.52f,hy-r*.51f},0xFFFFE6CB);
        p.fillPoly(new float[]{hx+r*.73f,hy-r*.54f,hx+r*.79f,hy-r*.58f,hx+r*.68f,hy-r*.38f,
                hx+r*.62f,hy-r*.44f},0xFF514050);
        boolean blink=!still && hit==0 && time%5.6f>5.44f;
        if(hit>0 || blink)p.arc(hx+r*.44f,hy-r*.69f,r*.06f,r*.04f,190,160,0xFF49344F,r*.035f);
        else {
            p.fillCircle(hx+r*.44f,hy-r*.69f,r*.038f,0xFF49344F);
            p.fillCircle(hx+r*.43f,hy-r*.70f,r*.013f,0xFFFFFFFF);
        }
        if(hit>0) {
            for(int side=-1;side<=1;side+=2) {
                float angle=side==1?-.35f-.65f*flap:(float)Math.PI+.35f+.65f*flap;
                float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                float cx=x-r*.10f+dx*r*.35f,cy=y+dy*r*.35f;
                float[] wing=new float[32];
                for(int i=0;i<16;i++) {
                    float a=i*Softbody.TAU/16,along=(float)Math.cos(a)*r*.48f,across=(float)Math.sin(a)*r*.19f;
                    wing[i*2]=cx+dx*along-dy*across;wing[i*2+1]=cy+dy*along+dx*across;
                }
                p.fillPoly(wing,0xFFFFB7D6);
                for(int i=0;i<3;i++) {
                    float offset=(i-1)*r*.10f;
                    p.line(cx-dy*offset,cy+dx*offset,cx+dx*r*.35f-dy*offset,
                            cy+dy*r*.35f+dx*offset,0xFFFFD3E6,r*.045f);
                }
            }
        } else {
            float cy=y-r*(.025f+(still?0:.018f*flap));
            float[] folded=new float[48];
            for(int i=0;i<24;i++) {
                float a=i*Softbody.TAU/24,across=(float)Math.cos(a);
                folded[i*2]=x-r*.17f+across*r*.34f;
                folded[i*2+1]=cy+(float)Math.sin(a)*r*.20f*(.75f+.25f*across);
            }
            p.fillEllipse(x-r*.16f,cy+r*.075f,r*.32f,r*.14f,0x669D5C85);
            p.fillPoly(folded,0xFFE79ABA);
            p.fillEllipse(x-r*.13f,cy-r*.055f,r*.24f,r*.075f,0x77FFD4E1);
            for(int i=0;i<2;i++)p.arc(x-r*(.23f-i*.12f),cy+r*.015f,r*.15f,r*.095f,
                    5,115,0x77AF698D,r*.023f);
        }
        for(float foot:new float[]{leftWater,rightWater}) {
            p.arc(foot,waterY,groundR*.14f,groundR*.04f,0,180,0x8869CED1,groundR*.018f);
            if(hit>0 && !still)for(int side=-1;side<=1;side+=2)
                p.fillEllipse(foot+side*groundR*.13f,waterY-groundR*(.06f+hit*.12f),groundR*.022f,groundR*.04f,0x779EEDFF);
        }
    }
    private static void coin(Painter p,float x,float y,float r,float time,float hit,boolean still) {
        float reveal=hit<0?0:Math.min(1,hit/.22f);
        p=new OpacityPainter(p,.27f+.73f*reveal);
        float spin=still?1:Math.abs((float)Math.cos(time*2.6f+(hit<0?0:hit*13)));
        float rx=r*(.16f+.57f*spin),cy=y;
        p.fillEllipse(x,y+r*.87f,r*.60f,r*.08f,0x35372B5B);
        p.strokeCircle(x,cy,r*.94f,Glyph.withAlpha(0xFFE8A9FF,(int)(54*reveal)),r*.07f);
        // Feather the idle silhouette and mute fine detail; collection restores crisp edges.
        for(int i=3;i>0;i--) {
            float feather=r*.035f*i*(1-reveal);
            p.fillEllipse(x,cy,rx+feather,r*.76f+feather,Glyph.withAlpha(0xFFFFDC67,(int)(18*(1-reveal))));
        }
        Painter detail=new OpacityPainter(p,.38f+.62f*reveal);
        detail.fillEllipse(x+r*.06f,cy,rx,r*.76f,0xFFCA813A);
        p.fillEllipse(x,cy,rx,r*.76f,0xFFFFDC67);
        detail.arc(x,cy,rx*.78f,r*.61f,0,360,0xFFFFF1AC,r*.035f);
        if(spin>.20f)detail.fillPoly(star(x,cy,r*.34f*spin,r*.15f*spin,5,0),0xFFD99837);
        detail.arc(x,cy,rx*.88f,r*.66f,210,80,0xFFFFF6CF,r*.065f);
        float phase=still?.3f:time*(hit<0?1.8f:3.5f)+Math.max(0,hit)*6;
        float sweep=(float)Math.sin(phase)*1.3f;
        float[] face=new float[48];
        for(int i=0;i<24;i++) {
            float angle=i*Softbody.TAU/24;
            face[i*2]=(float)Math.cos(angle);face[i*2+1]=(float)Math.sin(angle);
        }
        for(int layer=0;layer<2;layer++) {
            float width=layer==0?.22f:.075f;
            float[] shine=CrystalField.clip(CrystalField.clip(face,1,.35f,sweep+width),-1,-.35f,-sweep+width);
            for(int i=0;i<shine.length;i+=2){shine[i]=x+shine[i]*rx;shine[i+1]=cy+shine[i+1]*r*.76f;}
            if(shine.length>=6)p.fillPoly(shine,Glyph.withAlpha(0xFFFFF8D9,layer==0?45:90));
        }
        float gleam=still?.65f:.35f+.65f*(float)Math.pow(Math.sin(phase*.8f),2);
        p.fillPoly(star(x+rx*.66f,cy-r*.43f,r*(.04f+.035f*gleam),r*.014f,4,0),
                Glyph.withAlpha(0xFFFFF8D9,(int)(120+100*gleam)));
    }
}
