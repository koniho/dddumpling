package com.dddumpling.game;

/** Shipped highlights and their shared list/popup illustrations. */
final class ReleaseChange extends Draw {
    static final int TRAVEL=0, STARS=1, BUGS=2, SHUFFLE=3, DISGUISE=4, SLIME=5,
            PAIR=6, FLEX=7, TEAM=8, NEWS=9, FLURRY=10, MISC=11, SETTINGS=12, SWIPE=13, SCORES=14, OCTOPULSE=15,
            COMPANION=16, TUTORIAL=17, TOWN=18, SURVIVAL=19, TIME_ATTACK=20, SWAMP=21, RUSH=22, FOG=23, HALLOWEEN=24, RAINBOW=25, BOSS_RETRY=26,
            ACHIEVEMENTS=27, PROFESSOR=28, LAND_HOMES=29, ARENA_HOMES=30, WARDROBE=31;
    private static final class FogIcon {
        static final GameCore.Enemy WORD=new GameCore.Enemy();
        static {WORD.word=new int[]{0,1,2};WORD.enterT=1f;WORD.swampKind=SwampEnemies.FOG;}
    }
    static final int[][] ITEMS=ReleaseContent.ITEMS;
    static boolean homes(int id) { return id==LAND_HOMES || id==ARENA_HOMES; }
    static boolean playable(int id) { return id==TRAVEL || id==SHUFFLE || id==PAIR; }
    static float artUnits(int id) { return homes(id) ? 8f : id==TRAVEL ? 8f : id==PAIR ? 9f : id==SHUFFLE ? 9f : id==ACHIEVEMENTS ? 7f : 5f; }
    private static void halloween(Painter p,float x,float y,float r,float time) {
        p=Skins.painter(p,Skins.HALLOWEEN);
        // Real costume art stays recognizable in both the small list and the roomy popup.
        p.fillEllipse(x,y+r*.12f,r*.94f,r*.70f,0x224E397F);
        for(int side=-1;side<=1;side+=2) {
            float twinkle=.75f+.25f*(float)Math.sin(time*3+side);
            p.fillPoly(star(x+side*r*.73f,y-r*.55f,r*.14f*twinkle,r*.045f,4,.15f*side),
                    Glyph.withAlpha(side<0?0xFFFFBE70:0xFFC3A1F0,(int)(210*twinkle)));
        }
        float bob=(float)Math.sin(time*3),hop=Math.max(0,(float)Math.sin(time*3+.9f));
        HalloweenKeys.draw(p,Kawaii.STRAWBERRY,x+r*.15f*(float)Math.sin(time*1.6f),
                y-r*(.64f+.07f*bob),r*.28f,Skins.HALLOWEEN_COLORS[1],1,.4f,0,time);
        HalloweenKeys.draw(p,Kawaii.DUMPLING,x-r*.54f,y-r*.02f+r*.10f*bob,
                r*.47f,Skins.HALLOWEEN_COLORS[0],1+.04f*bob,.8f,0,time);
        HalloweenKeys.draw(p,Kawaii.BLOB,x+r*.53f,y+r*.02f-r*.08f*bob,
                r*.47f,Skins.HALLOWEEN_COLORS[5],1-.04f*bob,.3f,0,time);
        HalloweenKeys.draw(p,Kawaii.CAT,x,y+r*(.32f-.10f*hop),r*.55f,
                Skins.HALLOWEEN_COLORS[2],1+.07f*bob,.5f,0,time);
    }
    private static void wardrobe(Painter p,float x,float y,float r,float time) {
        final int outfit=(int)(time/4.8f)%3;
        final float phase=time%4.8f,landing=1.25f;
        final int hat=Accessories.item(Accessories.HAT,outfit);
        float bx=x-r*.70f,by=y+r*.40f,bodyR=r*.46f;
        float u=Math.max(0,Math.min(1,(phase-.35f)/.90f));u=u*u*(3-2*u);
        float dance=Math.max(0,phase-landing),energy=Math.max(0,1-dance/1.6f);
        float wiggle=(float)Math.sin(dance*13)*energy;
        float cx=x+r*.35f+r*.08f*wiggle,cy=y+r*.26f-r*.16f*Math.abs(wiggle);
        Painter box=new LinkedPairArt.TurnPainter(p,bx,by,-.06f*(float)Math.sin(time*3));
        WardrobeToybox.container(box,Accessories.HAT,phase<.35f?hat:-1,r*.32f,Accessories.COLOR[hat],1f,time);
        Painter dressed=new OpacityPainter(p,1f) {
            public int accessory(int who,int slot) {
                if(slot==Accessories.HAT)return phase>=landing?hat:-1;
                if(slot==Accessories.GLASSES || slot==Accessories.BOOTS)return Accessories.item(slot,outfit);
                return -1;
            }
        };
        Painter dancer=new LinkedPairArt.TurnPainter(dressed,cx,cy,wiggle*.18f);
        Trinket.drawReacting(dancer,0,0,0,bodyR*(1+.06f*wiggle),time,1f,phase>=landing?1:0,0);
        if(phase>=.35f && phase<landing) {
            float targetY=cy+bodyR*AccessoryAnchors.TOP[Collect.SHAPE[0]];
            for(int i=11;i>=0;i--) {
                float t=u-i*.025f;if(t<0)continue;
                float tx=bx+(cx-bx)*t,ty=by-r*.15f+(targetY-by+r*.15f)*t-r*.70f*4*t*(1-t);
                float sparkle=r*.042f*(1-i/13f);
                p.fillCircle(tx,ty,sparkle*2.5f,0x24FFE6A3);
                AccessoryArt.star(p,tx,ty,sparkle,Glyph.withAlpha(GOLD,220-i*14));
            }
            float hx=bx+(cx-bx)*u,hy=by-r*.15f+(targetY-by+r*.15f)*u-r*.70f*4*u*(1-u);
            AccessoryArt.item(p,hat,hx,hy,r*(.22f+u*(.46f*.67f-.22f)),time);
        }
        if(phase>=landing && dance<1.6f) {
            float burst=Math.min(1,dance/.55f);
            for(int i=0;i<10;i++) {
                float a=i*.6283185f,rr=r*(.30f+burst*.60f);
                float sx=cx+(float)Math.cos(a)*rr,sy=cy+(float)Math.sin(a)*rr;
                AccessoryArt.star(p,sx,sy,r*.065f*energy,Glyph.withAlpha(i%2==0?GOLD:0xFFBFA5FF,(int)(220*energy)));
            }
        }
    }
    static void icon(Painter p,int id,float x,float y,float r,float time) {
        icon(p,id,x,y,r,time,-1f);
    }
    static void icon(Painter p,int id,float x,float y,float r,float time,float celebration) {
        if(homes(id)) {
            ReleaseHomes.icon(p,id==ARENA_HOMES,x,y,r);return;
        }
        if(id==WARDROBE) {
            wardrobe(p,x,y,r,time);
        } else if(id==ACHIEVEMENTS) {
            float jump=0f;
            if(celebration>=0f && celebration<2.8f) {
                float t=celebration;
                jump=(float)Math.sin(Math.min(1f,t/.9f)*Math.PI)*r*.85f;
                float fade=Math.min(1f,(2.8f-t)/.8f);
                for(int i=0;i<18;i++) {
                    float a=i*2.39996f,flight=Math.max(0f,t-.10f);
                    float speed=r*(1.4f+(i%3)*.35f);
                    float xx=x+(float)Math.cos(a)*speed*flight;
                    float yy=y-r*.3f+(float)Math.sin(a)*speed*flight+r*.75f*flight*flight;
                    float sr=r*(.08f+.025f*(i%3));
                    int color=i%3==0?0xFFFFF2B3:i%3==1?GOLD:0xFFE8A4CF;
                    p.fillPoly(star(xx,yy,sr,sr*.4f,5,a+t*3),Glyph.withAlpha(color,(int)(255*fade)));
                }
                if(t<.7f)p.strokeCircle(x,y-r*.2f,r*(.6f+t*2.3f),
                        Glyph.withAlpha(GOLD,(int)(150*(1f-t/.7f))),r*.045f);
                y-=r*.12f*(float)Math.sin(t*17)*Math.max(0f,1-t/1.4f);
            }
            float bob=r*.05f*(float)Math.sin(time*3);
            int gold=0xFFFFCC63,shade=0xFFD88E3F,shine=0xFFFFEDAC;
            for(int side=-1;side<=1;side+=2) {
                p.strokeCircle(x+side*r*.54f,y+r*.12f,r*.27f,shade,r*.13f);
                p.strokeCircle(x+side*r*.54f,y+r*.09f,r*.27f,gold,r*.08f);
                float sparkle=r*(.12f+.035f*(float)Math.sin(time*4+side));
                p.fillPoly(star(x+side*r*.78f,y-r*.59f,sparkle,sparkle*.28f,4,0),shine);
            }
            p.line(x,y+r*.34f,x,y+r*.77f,shade,r*.20f);
            p.fillPoly(pill(x,y+r*.80f,r*.44f,r*.13f,10),shade);
            p.fillPoly(pill(x,y+r*.75f,r*.36f,r*.09f,10),gold);
            Kawaii.moodDumpling(p,x,y-r*.29f+bob-jump,r*.57f,gold,1f,
                    1f+.035f*(float)Math.sin(time*3)-jump/r*.13f);
            p.fillPoly(new float[]{x-r*.60f,y,x+r*.60f,y,x+r*.43f,y+r*.37f,
                    x+r*.22f,y+r*.49f,x-r*.22f,y+r*.49f,x-r*.43f,y+r*.37f},gold);
            p.line(x-r*.56f,y+r*.025f,x+r*.56f,y+r*.025f,shine,r*.08f);
            p.fillPoly(star(x,y+r*.24f,r*.17f,r*.08f,5,0),shine);
            p.line(x-r*.39f,y+r*.12f,x-r*.31f,y+r*.28f,shine,r*.055f);
        } else if(id==PROFESSOR) {
            float cy=y+r*.15f+r*.045f*(float)Math.sin(time*2.5f),rr=r*.69f;
            int ink=0xFF594367;
            Kawaii.draw(p,Kawaii.CAT,x-r*.10f,cy,rr,0xFFC4B3E9,1f,.25f);
            for(int side=-1;side<=1;side+=2)
                p.strokeCircle(x-r*.10f+side*r*.22f,cy-r*.06f,r*.19f,ink,r*.045f);
            p.line(x-r*.13f,cy-r*.06f,x-r*.07f,cy-r*.06f,ink,r*.045f);
            p.fillPoly(new float[]{x-r*.10f,cy+r*.39f,x-r*.32f,cy+r*.28f,x-r*.32f,cy+r*.51f},0xFFDB8299);
            p.fillPoly(new float[]{x-r*.10f,cy+r*.39f,x+r*.12f,cy+r*.28f,x+r*.12f,cy+r*.51f},0xFFDB8299);
            p.fillCircle(x-r*.10f,cy+r*.39f,r*.065f,ink);
            p.fillPoly(pill(x-r*.10f,cy-r*.51f,r*.33f,r*.13f,8),ink);
            p.fillPoly(new float[]{x-r*.69f,cy-r*.69f,x-r*.10f,cy-r*.90f,
                    x+r*.49f,cy-r*.69f,x-r*.10f,cy-r*.48f},0xFF82669B);
            p.polyline(new float[]{x-r*.10f,cy-r*.70f,x+r*.39f,cy-r*.66f,
                    x+r*.42f,cy-r*.30f},GOLD,r*.045f);
            p.fillEllipse(x+r*.42f,cy-r*.27f,r*.055f,r*.10f,GOLD);
            p.fillEllipse(x+r*.65f,y+r*.38f,r*.30f,r*.22f,0xFFF7F2E8);
            p.fillPoly(new float[]{x+r*.42f,y+r*.45f,x+r*.30f,y+r*.61f,x+r*.58f,y+r*.54f},0xFFF7F2E8);
            for(int dot=0;dot<3;dot++)p.fillCircle(x+r*(.50f+dot*.15f),y+r*.38f,r*.032f,ink);
        } else if(id==RAINBOW) {
            p.save();p.clipRect(x-r,y-r,x+r,y+r);
            RainbowCatArt.portrait(p,x-r*.12f,y,r*.8f,time,true,1f,-1);
            p.restore();
        } else if(id==BOSS_RETRY) {
            float bob=(float)Math.sin(time*3f);
            Trinket.draw(p,14,x-r*.46f,y+r*(.22f+.06f*bob),r*.48f,time,true,1f);
            BossCollect.draw(p,Boss.SLIME,x+r*.37f,y+r*(.10f-.06f*bob),r*.57f,time,true,1f);
            float[] arc=new float[34];
            for(int i=0;i<17;i++) {
                float a=(float)Math.PI*(1.12f+i*.76f/16f);
                arc[i*2]=x+(float)Math.cos(a)*r*.86f;
                arc[i*2+1]=y+(float)Math.sin(a)*r*.86f;
            }
            int color=Glyph.cycle(time*.5f);
            p.polyline(arc,color,r*.10f);
            p.fillPoly(new float[]{x+r*.55f,y-r*.44f,x+r*.94f,y-r*.13f,
                    x+r*.94f,y-r*.65f},color);
        } else if(id==HALLOWEEN) {
            halloween(p,x,y,r,time);
        } else if(id==FOG) {
            p.save();p.translate(x,y);
            float rr=r*.28f,step=r*.6f;
            SwampEnemies.fog(p,FogIcon.WORD,rr,step,0,time,0);
            for(int i=1;i<3;i++)SwampEnemies.silhouette(p,i,(i-1)*step,0,rr,255);
            SwampEnemies.fog(p,FogIcon.WORD,rr,step,0,time,1);
            p.fillPoly(Glyph.hex(-step,0,rr),Glyph.COLOR[0]);
            Skits.face(p,Kawaii.DUMPLING,-step,0,rr*.7f,255,1f,1f);
            SwampEnemies.fog(p,FogIcon.WORD,rr,step,0,time,2);
            p.restore();
        } else if(id==SWAMP) {
            Swamp.prop(p,x,y+r*.28f,r*.8f,255,time,false);
            BossCollect.draw(p,Boss.ALLIGATOR,x-r*.12f,y-r*.15f,r*.78f,time,true,1f);
        } else if(id==RUSH) {
            float bob=r*.08f*(float)Math.sin(time*4f);
            for(int i=0;i<5;i++) {
                float t=(time*.8f+i*.2f)%1f;
                float xx=x-r*.38f+t*r*.75f,yy=y-r*.25f-t*r*.65f;
                p.fillPoly(star(xx,yy,r*.13f*(1-t*.5f),r*.05f,5,time+i),Glyph.withAlpha(GOLD,(int)(180*(1-t))));
            }
            FastEnemies.glow(p,x,y+bob,r*.65f,Glyph.COLOR[0],255,1f);
            FastEnemies.body(p,x,y+bob,r*.65f,Glyph.COLOR[0],255,false);
            Skits.face(p,Kawaii.DUMPLING,x,y+bob,r*.42f,255,1f,1f);
        } else if(id==SURVIVAL) {
            for(int i=0;i<7;i++) {
                float xx=x+(i-3)*r*.26f,yy=y-r*.8f+r*.25f*(float)Math.sin(time*3+i);
                int color=Glyph.cycle(i*.8f);
                p.line(xx,yy,xx,yy+r*1.4f,color,r*.21f);
                p.fillCircle(xx,yy,r*.105f,color);p.fillCircle(xx,yy+r*1.4f,r*.105f,color);
            }
            Duck.draw(p,Collect.DUCK_FIRST,x,y+r*.18f,r*.68f,time,true,1f,8,0,false);
        } else if(id==TIME_ATTACK) {
            BossScreen.slimePortrait(p,x-r*.18f,y+r*.12f,r*.48f,time);
            float tx=x+r*.53f,ty=y-r*.42f,hand=time*3;
            p.fillCircle(tx,ty,r*.46f,BG);p.strokeCircle(tx,ty,r*.43f,GOLD,r*.07f);
            p.line(tx,ty,tx+(float)Math.sin(hand)*r*.29f,ty-(float)Math.cos(hand)*r*.29f,INK,r*.06f);
            p.line(tx-r*.12f,ty-r*.54f,tx+r*.12f,ty-r*.54f,GOLD,r*.08f);
        } else if(id==COMPANION || id==TUTORIAL) {
            float hop=(float)Math.sin(time*3f)*r*.10f;
            Trinket.draw(p,14,x-r*.15f,y+r*.2f+hop,r*.75f,time,true,1f);
            if(id==TUTORIAL) {
                p.fillEllipse(x+r*.45f,y-r*.55f,r*.55f,r*.34f,0xFFF7F2E8);
                p.fillPoly(new float[]{x+r*.15f,y-r*.38f,x+r*.15f,y-r*.12f,x+r*.4f,y-r*.35f},0xFFF7F2E8);
                for(int dot=0;dot<3;dot++)
                    p.fillCircle(x+r*(.22f+dot*.22f),y-r*.55f,r*.055f,INK);
            }
        } else if(id==TOWN) {
            LandPicker.townIcon(p,x,y,r,255);
        } else if(id==SCORES) {
            powerHalo(p,x,y,r*.7f,time,GOLD,1f);
            for(int row=0;row<3;row++) {
                float yy=y+r*(row*.5f-.5f);
                p.line(x-r*.65f,yy,x+r*.65f,yy,row==0?GOLD:INK_DIM,r*.12f);
            }
            p.fillPoly(star(x+r*.55f,y-r*.55f,r*.38f,r*.18f,5,0f),GOLD);
        } else if(id==OCTOPULSE) {
            BossCollect.draw(p,Boss.OCTOPUS,x,y,r,time,true,1f);
        } else if(id==SWIPE) {
            float lift=(float)(.5-.5*Math.cos(time*2.5f));
            p.line(x-r*.85f,y+r*.7f,x+r*.85f,y+r*.7f,Glyph.cycle(time),r*.16f);
            p.polyline(new float[]{x-r*.3f,y-r*.45f,x,y-r*.8f,x+r*.3f,y-r*.45f},GOLD,r*.12f);
            Renderer.touchHint(p,x,y+r*(.45f-lift*.65f),r*.42f,(float)Math.PI/2f,1f,time);
        } else if(id==SETTINGS) {
            p.strokePoly(Glyph.hex(x,y,r),INK_DIM,r*.117f);
            for(int line=-1;line<=1;line++)
                p.line(x-r*.467f,y+line*r*.3f,x+r*.467f,y+line*r*.3f,INK_DIM,r*.117f);
        } else if(id==MISC) {
            ReleaseMascots.dumpling(p,x,y,r,time,celebration);
        } else if(id==NEWS) {
            powerHalo(p,x,y,r*.6f,time,Glyph.cycle(time*.7f),1f);
            ReleaseMascot.steamer(p,x,y,r*.85f,time);
        } else if(id==FLURRY) {
            for(int band=0;band<7;band++)
                p.strokeCircle(x,y,r*(.95f-band*.09f),Glyph.cycle(band*.85f),r*.08f);
        } else if(id==TRAVEL) {
            Lands.logo(p,0,x,y-r*.4f,r,255,time);
        } else if(id==STARS) {
            p.polyline(new float[]{x-r*.9f,y+r*.8f,x-r*.4f,y+r*.35f,x+r*.15f,y+r*.6f,x+r*.6f,y-r*.6f},0xFFAE9ADA,r*.14f);
            p.fillPoly(star(x+r*.35f,y-r*.35f,r*.65f,r*.32f,5,.1f),GOLD);
        } else if(id==BUGS) {
            ReleaseMascots.bug(p,x,y,r,time,celebration);
        } else if(id==SHUFFLE) {
            p.fillPoly(Glyph.hex(x,y,r*.94f),0xFFAE9ADA);
            p.text("?",x,y+r*.48f,r*1.5f,GOLD,Painter.CENTER,true);
        } else if(id==DISGUISE) {
            Kawaii.incognito(p,Kawaii.STRAWBERRY,x-r*.42f,y,r*.63f,Glyph.COLOR[1],1f);
            Kawaii.incognito(p,Kawaii.CAT,x+r*.42f,y,r*.63f,Glyph.COLOR[2],1f);
        } else if(id==SLIME) {
            BossScreen.slimePortrait(p,x-r*.08f,y,r*.58f,time);
            p.fillPoly(Glyph.hex(x+r*.45f,y+r*.42f,r*.42f),Glyph.COLOR[0]);
            Skits.face(p,Kawaii.DUMPLING,x+r*.45f,y+r*.42f,r*.26f,255,1f,0f);
        } else if(id==FLEX) {
            float[] arm={x+r*.7f,y+r*.5f,x-r*.45f,y+r*.5f,x-r*.65f,y+r*.05f,x-r*.12f,y-r*.6f};
            p.polyline(arm,0xFFE3AB4D,r*.40f);
            p.fillEllipse(x+r*.1f,y+r*.35f,r*.38f,r*.32f,GOLD);
            p.fillCircle(x-r*.12f,y-r*.6f,r*.27f,GOLD);
            p.line(x-r*.20f,y-r*.64f,x+r*.01f,y-r*.51f,0xFFE3AB4D,r*.055f);
        } else if(id==TEAM) {
            Skits.face(p,Kawaii.SQUISHY,x,y,r*.78f,255,1f,1f);
            for(int side=-1;side<=1;side+=2)
                p.polyline(new float[]{x+side*r*.8f,y-r*.7f,x+side*r*.63f,y-r*.28f,
                        x+side*r*.93f,y-r*.31f,x+side*r*.8f,y+r*.13f},GOLD,r*.10f);
        } else {
            p.line(x-r*.53f,y,x+r*.53f,y,GOLD,r*.14f);
            Skits.face(p,Kawaii.DUMPLING,x-r*.62f,y,r*.49f,255,1f,1f);
            Skits.face(p,Kawaii.CAT,x+r*.62f,y,r*.49f,255,1f,1f);
        }
    }
}
