package com.dddumpling.game;

/** Tight follow camera, instant encounter staging, and a steady response row above the player. */
final class CaveScreen extends Draw {
    private CaveScreen() {}
    static void draw(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;
        p.fillRect(0,0,L.w,L.deckTop,CaveArt.DARK);p.fillRect(0,L.deckTop,L.w,L.h,CaveArt.ROCK);
        if(v.phase==Cave.CHOOSE){v.selection.draw(p,c,L);return;}
        p.save();p.clipRect(0,L.playTop,L.w,L.deckTop);
        float rumble=shakeStrength(v);
        p.translate((float)Math.sin(v.effects.clock*83)*L.w*.006f*rumble,
                (float)Math.sin(v.effects.clock*107)*L.w*.004f*rumble);
        CaveTerrain.draw(p,v,L);
        if(v.phase==Cave.ROCKS||v.phase==Cave.SAND)floor(p,c,L);
        if(v.phase==Cave.SHADOW||v.phase==Cave.FIGHT)enemy(p,c,L);
        float px=v.playerX()*L.w,py=v.playerY(L),r=L.w*(.043f+(v.phase==Cave.SAND?.022f*v.focus:0))*v.zoom();
        float bounce=v.walker.lift()*r;
        boolean sand=v.phase==Cave.SAND;
        float sink=sand?sink(v):0;
        float shake=sand?(float)Math.sin(v.traps.age*52)*r*.045f:0;
        p.fillEllipse(px,py+r*.75f,r*1.3f,r*.36f,Glyph.withAlpha(0xFF000000,115));
        p.save();if(sand)p.clipRect(0,L.playTop,L.w,py+r*.88f);
        float bodyY=py+bounce+sink*r*.95f;
        CaveDumpling.draw(p,c.caveChoice,px+shake,bodyY,r,v.walker.squash(),c.clock,sand);
        p.fillPoly(pill(px+shake,bodyY-r*.76f,r*.67f,r*.35f,8),CaveArt.MID);
        p.fillEllipse(px+shake,bodyY-r*.55f,r*1.12f,r*.15f,CaveArt.LIGHT);
        CaveArt.lantern(p,px+r*1.05f,bodyY+r*.24f,r*.32f,255);
        if(sand)for(int side=-1;side<=1;side+=2) {
            float handY=py+r*(.10f+.15f*(float)Math.sin(c.clock*23+side));
            p.line(px+side*r*.6f,bodyY+r*.12f,px+side*r*1.18f,handY,CaveDumpling.COLORS[Math.max(0,c.caveChoice)],r*.17f);
            p.fillCircle(px+side*r*1.18f,handY,r*.16f,CaveArt.LAMP);
        }
        p.restore();
        if(sand) {
            p.fillEllipse(px,py+r*.92f,r*1.35f,r*.13f,CaveArt.FLOOR);
            response(p,c,L,new int[]{v.traps.left,v.traps.right},v.traps.hits%2,2,py-L.w*.19f);
            for(int i=0;i<CaveTraps.ESCAPE_PRESSES;i++)p.fillCircle(px+(i-3.5f)*L.w*.018f,py-L.w*.135f,L.w*.005f,
                    i<v.traps.hits?CaveArt.LAMP:CaveArt.ROCK);
        }
        if(v.phase==Cave.ROCKS)rocks(p,c,L);
        v.effects.draw(p,v,L);
        if(v.phase==Cave.FORK)fork(p,c,L);
        if(v.phase==Cave.EXIT)p.text("EXIT REACHED",L.w*.5f,L.playTop+L.w*.18f,type(L.unit*.8f),CaveArt.LAMP,Painter.CENTER,true);
        Renderer.particles(p,c);p.restore();Renderer.keys(p,c,L);
    }
    static float shakeStrength(Cave v) {
        float sustained=v.phase==Cave.ROCKS?.38f+.12f*(float)Math.sin(v.traps.age*19):0;
        return Math.max(v.effects.rumble,sustained);
    }
    static float rockFloorY(Cave v,Layout L) {
        float t=Math.min(1,v.zoomAge/Cave.HAZARD_ZOOM),ease=t*t*(3-2*t);
        float below=L.deckTop+L.w*.18f;
        return below+(v.playerY(L)-below)*ease;
    }
    static float sink(Cave v){return Math.max(.12f,Math.min(.94f,.12f+1.0f*v.traps.age/CaveTraps.DURATION-v.traps.hits*.035f));}
    private static void floor(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;float x=L.w*(.5f+v.hazardOffset()),y=v.playerY(L),s=L.w;CaveTraps t=v.traps;
        if(v.phase==Cave.SAND) {
            for(int i=7;i>0;i--) {
                float curl=t.age*5+i*.8f;
                p.fillEllipse(x+(float)Math.sin(curl)*s*.006f,y+s*.035f,s*(.055f+i*.025f),s*(.02f+i*.012f),
                        i%2==0?CaveArt.LIGHT:CaveArt.FLOOR);
            }
            for(int i=0;i<9;i++) {
                float a=i*2.4f-t.age*4,rr=s*(.05f+.018f*i);
                p.fillCircle(x+(float)Math.cos(a)*rr,y+s*.035f+(float)Math.sin(a)*rr*.45f,s*.006f,CaveArt.MID);
            }
        } else {
            y=rockFloorY(v,L);
            float rumble=(float)Math.sin(t.age*63)*s*.002f;
            p.fillEllipse(x,y+s*.025f,s*.42f,s*.16f,CaveArt.ROCK);
            p.fillEllipse(x,y+s*.015f,s*.40f,s*.145f,CaveArt.LIGHT);
            p.fillEllipse(x,y,s*.38f,s*.13f,CaveArt.FLOOR);
            for(int i=0;i<7;i++) {
                float xx=x+(i-3)*s*.095f+rumble;
                p.polyline(new float[]{xx-s*.016f,y-s*.06f,xx+s*.02f,y-s*.01f,xx,y+s*.045f,xx+s*.035f,y+s*.09f},
                        CaveArt.DARK,s*.003f);
            }
        }
    }
    private static void rocks(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;CaveTraps t=v.traps;float py=v.playerY(L);
        for(int i=0;i<CaveTraps.ROCK_COUNT;i++) {
            float progress=t.rockProgress(i),x=(t.lanes[i]+v.hazardOffset())*L.w;
            if(progress<0)continue;
            if(t.landed[i])continue;
            float fall=.12f+.88f*progress*progress;
            p.fillEllipse(x,py,L.w*CaveTraps.ROCK_R*(.55f+progress*.45f),L.w*.025f,Glyph.withAlpha(CaveArt.DARK,180));
            p.strokeCircle(x,py,L.w*.075f,CaveArt.LAMP,L.w*.002f);
            CaveArt.tumbling(p,(t.rockX(i,progress)+v.hazardOffset())*L.w,py-(1-fall)*L.w*.60f,L.w*CaveTraps.ROCK_R,i*77,255,
                    i+progress*(i%2==0?5f:-7f));
            for(int j=0;j<3;j++)p.fillCircle((t.rockX(i,progress)+v.hazardOffset())*L.w+(j-1)*L.w*.024f,py-(1-fall)*L.w*.6f-L.w*(.08f+j*.022f),L.w*.006f,CaveArt.LIGHT);
        }
        if(t.age<.45f)Renderer.touchHint(p,L.w*(.5f+.2f*t.age/.45f),py+L.w*.09f,L.w*.035f,1.1f,.75f,c.clock);
    }
    private static void enemy(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;float x=v.screenX(v.enemyX,L),y=v.worldScreenY(v.enemyY,L);
        float pop=Math.min(1,v.timer/Cave.REVEAL),r=L.w*(.043f+.023f*Math.min(1,v.timer/Cave.APPROACH));
        float bx=v.screenX(v.enemyStartX,L),by=v.worldScreenY(v.enemyStartY,L);
        if(pop>=1)CaveArt.stone(p,bx,by+L.w*.026f,L.w*.09f,81,255);
        p.fillEllipse(x,y+r*.7f,r*1.1f,r*.28f,Glyph.withAlpha(0xFF000000,140));
        Kawaii.determined(p,Kawaii.BLOB,x,y-r*.5f*(float)Math.sin(pop*Math.PI),r,Glyph.mix(Glyph.COLOR[v.response[0]],CaveArt.MID,.2f),1f+v.pulse*.15f);
        if(pop<1)CaveArt.stone(p,bx,by+L.w*(.026f+pop*.055f),L.w*.09f*(1-pop*.35f),81,255);
        if(pop>=1)for(int i=0;i<3;i++) {
            float dx=(v.enemyStartX-v.enemyX),dy=(v.enemyStartY-v.enemyY);
            p.line(x+dx*L.w*(.1f+i*.10f),y-dy*L.w*(.1f+i*.10f),x+dx*L.w*(.16f+i*.10f),y-dy*L.w*(.16f+i*.10f),CaveArt.LIGHT,L.w*.003f);
        }
        response(p,c,L,v.response,v.responsePos,v.responseSize,v.playerY(L)-L.w*.20f);
    }
    private static void response(Painter p,GameCore c,Layout L,int[] glyphs,int pos,int count,float y) {
        float gap=L.w*.090f,r=L.w*.032f;
        float x0=Math.max(r*1.5f,Math.min(L.w-r*1.5f-gap*(count-1),c.cave.playerX()*L.w-gap*(count-1)/2));
        for(int i=0;i<count;i++) {
            float x=x0+i*gap;int a=i<pos&&c.cave.phase!=Cave.SAND?65:255;
            p.fillCircle(x,y,r*1.3f,CaveArt.DARK);Kawaii.draw(p,glyphs[i],x,y,r,Glyph.withAlpha(Glyph.COLOR[glyphs[i]],a),1f,.5f);
            if(i==pos)p.strokeCircle(x,y,r*1.4f,CaveArt.LAMP,L.w*.003f);
        }
    }
    private static void fork(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;
        for(int side=-1;side<=1;side+=2) {
            float x=v.branchScreenX(side,L),y=v.branchScreenY(side,L);
            p.strokeCircle(x,y,L.w*.028f,Glyph.withAlpha(CaveArt.LAMP,side==v.nearestBranch()?240:95),L.w*.004f);
        }
        float t=Math.min(1,v.timer/Cave.FORK_WAIT);
        p.arc(v.playerX()*L.w,v.playerY(L),L.w*.068f,L.w*.068f,-90,360*(1-t),CaveArt.LAMP,L.w*.004f);
        if(!v.lessonSeen)Renderer.touchHint(p,v.branchScreenX(1,L),v.branchScreenY(1,L),L.w*.035f,.65f,.8f,c.clock);
    }
}
