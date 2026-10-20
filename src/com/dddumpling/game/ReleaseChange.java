package com.dddumpling.game;

/** Shipped highlights and their shared list/popup illustrations. */
final class ReleaseChange extends Draw {
    static final int TRAVEL=0, STARS=1, BUGS=2, SHUFFLE=3, DISGUISE=4, SLIME=5,
            PAIR=6, FLEX=7, TEAM=8, NEWS=9, FLURRY=10, MISC=11, SETTINGS=12, SWIPE=13, SCORES=14, OCTOPULSE=15,
            COMPANION=16, TUTORIAL=17, TOWN=18, SURVIVAL=19, TIME_ATTACK=20, SWAMP=21, RUSH=22, FOG=23, HALLOWEEN=24, RAINBOW=25, BOSS_RETRY=26;
    private static final class FogIcon {
        static final GameCore.Enemy WORD=new GameCore.Enemy();
        static {WORD.word=new int[]{0,1,2};WORD.enterT=1f;WORD.swampKind=SwampEnemies.FOG;}
    }
    static final int[][] ITEMS=ReleaseContent.ITEMS;
    static boolean playable(int id) { return id==TRAVEL || id==SHUFFLE || id==PAIR; }
    static float artUnits(int id) { return id==TRAVEL ? 8f : id==PAIR ? 9f : id==SHUFFLE ? 9f : 5f; }
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
    static void icon(Painter p,int id,float x,float y,float r,float time) {
        if(id==RAINBOW) {
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
            Skits.face(p,Kawaii.DUMPLING,x-r*.12f,y+r*.15f,r*.65f,255,1f,1f);
            for(int i=0;i<3;i++) {
                float sx=x+r*(i==0 ? -.72f : i==1 ? .08f : .72f);
                float sy=y-r*(i==1 ? .82f : .38f);
                float sr=r*(.20f+.025f*(float)Math.sin(time*2f+i));
                p.fillPoly(star(sx,sy,sr,sr*.28f,4,0f),i==1 ? GOLD : 0xFFBCE9DA);
            }
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
            int ink=0xFF574563;
            for(int side=-1;side<=1;side+=2) {
                for(int leg=0;leg<3;leg++)
                    p.line(x+side*r*.48f,y+r*(leg*.27f-.12f),x+side*r*.81f,y+r*(leg*.33f-.19f),ink,r*.08f);
                p.line(x+side*r*.23f,y-r*.53f,x+side*r*.42f,y-r*.87f,ink,r*.065f);
                p.fillCircle(x+side*r*.42f,y-r*.87f,r*.095f,0xFFECAAAD);
            }
            p.fillEllipse(x,y+r*.18f,r*.65f,r*.64f,0xFFED8A9E);
            p.arc(x,y+r*.18f,r*.08f,r*.58f,90,180,0xFFBD6581,r*.06f);
            for(int side=-1;side<=1;side+=2) {
                p.fillCircle(x+side*r*.32f,y+r*.16f,r*.115f,ink);
                p.fillCircle(x+side*r*.26f,y+r*.49f,r*.08f,ink);
            }
            p.fillEllipse(x,y-r*.36f,r*.49f,r*.36f,0xFFC5ABD9);
            for(int side=-1;side<=1;side+=2) {
                p.fillCircle(x+side*r*.17f,y-r*.40f,r*.067f,ink);
                p.fillEllipse(x+side*r*.30f,y-r*.27f,r*.08f,r*.04f,0xFFFFB5B9);
            }
            p.arc(x,y-r*.30f,r*.10f,r*.08f,0,180,ink,r*.035f);
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
