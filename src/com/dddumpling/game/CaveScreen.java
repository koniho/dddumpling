package com.dddumpling.game;

/** World and camera share coordinates for drawing, beam tests, junctions and taps. */
final class CaveScreen extends Draw {
    private CaveScreen() {}
    static void draw(Painter p, GameCore c, Layout L) {
        Cave v = c.cave;
        p.fillRect(0, 0, L.w, L.deckTop, CaveArt.DARK);
        p.fillRect(0, L.deckTop, L.w, L.h, CaveArt.ROCK);
        if (v.phase == Cave.CHOOSE) { v.selection.draw(p,c,L); return; }
        p.save(); p.clipRect(0, L.playTop, L.w, L.deckTop);
        terrain(p, c, L);
        beam(p, c, L);
        clues(p, c, L);
        if (v.phase == Cave.SHADOW || v.phase == Cave.FIGHT) enemy(p, c, L);
        if (v.phase == Cave.ROCKS || v.phase == Cave.SAND || v.returnTime > 0f) trap(p, c, L);
        float px = v.playerX() * L.w, py = v.playerY(L), r = L.w * .043f;
        float bounce = v.walker.lift()*r;
        float sink = v.phase == Cave.SAND ? Math.max(0f, (v.traps.age-CaveTraps.WARNING)/CaveTraps.DURATION
                - v.traps.hits/(float)CaveTraps.ESCAPE_PRESSES) : 0f;
        p.fillEllipse(px, py + r * .75f, r * 1.3f, r * .36f, Glyph.withAlpha(0xFF000000, 115));
        CaveDumpling.draw(p,c.caveChoice,px,py+sink*r+bounce,r,v.walker.squash(),c.clock);
        // A little expedition hat belongs to the dumpling, not to the UI.
        p.fillPoly(pill(px, py-r*.76f+sink*r+bounce, r*.67f, r*.35f, 8), CaveArt.MID);
        p.fillEllipse(px, py-r*.55f+sink*r+bounce, r*1.12f, r*.15f, CaveArt.LIGHT);
        CaveArt.lantern(p, px+r*1.05f, py+r*.24f+bounce, r*.32f, 255);
        if (v.phase == Cave.SAND) {
            p.fillEllipse(px, py+r*(.72f-sink*.3f), r*1.4f, r*.24f, CaveArt.FLOOR);
            response(p, c, L, new int[]{v.traps.left,v.traps.right}, v.traps.hits%2, 2, py-L.w*.15f);
        }
        if (v.phase == Cave.FORK) fork(p, c, L);
        if (v.phase == Cave.EXIT) p.text("EXIT REACHED", L.w*.5f, L.playTop+L.w*.18f,
                type(L.unit*.8f), CaveArt.LAMP, Painter.CENTER, true);
        Renderer.particles(p,c);
        p.restore();
        Renderer.keys(p, c, L);
    }
    private static void terrain(Painter p, GameCore c, Layout L) {
        Cave v = c.cave;
        int first = (int)Math.floor(v.cameraZ * 5f) - 5;
        for (int row = first; row < first+23; row++) {
            float at = row / 5f, y = v.screenY(at, L);
            for (int col = 0; col < 7; col++) {
                int seed = row * 73 + col * 127 + 7000;
                float x = (col / 6f + (hash(seed)-.5f)*.10f)*L.w;
                CaveArt.stone(p,x,y,L.w*(.105f+hash(seed+3)*.055f),seed,255);
            }
        }
        // Broad filled ribbons keep every translucent corner from double-blending.
        for (int layer = 0; layer < 3; layer++) {
            int col = layer == 0 ? CaveArt.DARK : layer == 1 ? CaveArt.LIGHT : CaveArt.FLOOR;
            float width = L.w * (layer == 0 ? .098f : layer == 1 ? .078f : .068f);
            float last = 0f;
            for (int i = 0; i < 3; i++) {
                path(p,v,L,last,Cave.FORKS[i],-1,0,width,col);
                for(int side=-1;side<=1;side+=2) path(p,v,L,Cave.FORKS[i],Cave.FORKS[i]+2f,i,side,width,col);
                last = Cave.FORKS[i]+2f;
            }
            path(p,v,L,last,Cave.LENGTH+.4f,-1,0,width,col);
        }
        for (int i = 0; i < 3; i++) if (v.routes[i] != 0) {
            for (int j = 1; j < 14; j++) {
                float at = Cave.FORKS[i] + j*.14f;
                if(at > v.z) continue;
                float x = Cave.branchX(i,v.routes[i],at)*L.w;
                p.fillEllipse(x,v.screenY(at,L),L.w*.006f,L.w*.003f,Glyph.withAlpha(CaveArt.LAMP,65));
            }
        }
        float ey = v.screenY(Cave.LENGTH+.1f,L);
        CaveArt.entrance(p,Cave.centre(Cave.LENGTH)*L.w,ey,L.w*.12f,255,false);
        p.fillEllipse(Cave.centre(Cave.LENGTH)*L.w,ey+L.w*.035f,L.w*.052f,L.w*.09f,
                Glyph.withAlpha(CaveArt.LAMP,160));
    }
    private static void path(Painter p,Cave v,Layout L,float from,float to,int fork,int side,float width,int col) {
        int count = Math.max(2,(int)((to-from)*24));
        float[] pts = new float[(count+1)*4];
        for(int i=0;i<=count;i++) {
            float at=from+(to-from)*i/count;
            float x=(fork<0 ? Cave.centre(at) : Cave.branchX(fork,side,at))*L.w;
            float y=v.screenY(at,L);
            pts[i*2]=x-width;pts[i*2+1]=y;
            int back=(count*2+1-i)*2;pts[back]=x+width;pts[back+1]=y;
        }
        p.fillPoly(pts,col);
    }
    private static void beam(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;float x=v.playerX()*L.w,y=v.playerY(L);
        float aim=v.aim;
        if(v.phase==Cave.FORK && !v.lessonSeen && v.timer<Cave.LESSON)
            aim=v.aim+(.42f-v.aim)*Math.min(1f,v.timer/.65f)
                    * Math.min(1f,(Cave.LESSON-v.timer)/.5f);
        float[] mask=new float[36];mask[0]=x;mask[1]=y+L.w*.08f;
        for(int i=0;i<=16;i++) {
            float angle=aim-.39f+.78f*i/16f;
            mask[2+i*2]=x+(float)Math.sin(angle)*L.w*1.55f;
            mask[3+i*2]=y-(float)Math.cos(angle)*Cave.scale(L)*1.55f;
        }
        p.fillContours(new float[][]{new float[]{0,L.playTop,L.w,L.playTop,L.w,L.deckTop,0,L.deckTop},mask},
                Glyph.withAlpha(CaveArt.DARK,115));
        for(int layer=0;layer<4;layer++) {
            float half=.34f+layer*.035f,reach=1.50f-layer*.10f;
            float[] fan=new float[36];fan[0]=x;fan[1]=y;
            for(int i=0;i<=16;i++) {
                float a=aim-half+2f*half*i/16f;
                fan[2+i*2]=x+(float)Math.sin(a)*L.w*reach;
                fan[3+i*2]=y-(float)Math.cos(a)*Cave.scale(L)*reach;
            }
            p.fillPoly(fan,Glyph.withAlpha(CaveArt.LAMP,13));
        }
        for(int i=4;i>0;i--) p.fillCircle(x,y,L.w*(.06f+i*.035f),Glyph.withAlpha(CaveArt.LAMP,5));
        // Darkness deepens with distance while leaving a readable outline of each nearby branch.
        for(int i=0;i<12;i++) {
            float top=L.playTop+i*L.w*.025f;
            p.fillRect(0,top,L.w,top+L.w*.025f,Glyph.withAlpha(CaveArt.DARK,Math.max(0,155-i*12)));
        }
    }
    private static void clues(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;
        for(int i=0;i<3;i++) for(int side=-1;side<=1;side+=2) {
            float at=Cave.FORKS[i]+.8f,x=Cave.branchX(i,side,at),y=v.screenY(at,L);
            int a=v.illuminated(x,at) ? 245 : 55;
            int kind=v.event(i,side);
            if(!v.met[i] || v.routes[i]!=side) {
            if(kind==Cave.SAND) {
                p.fillEllipse(x*L.w,y,L.w*.055f,L.w*.03f,Glyph.withAlpha(CaveArt.LIGHT,a));
                p.arc(x*L.w,y,L.w*.04f,L.w*.015f,20,280,Glyph.withAlpha(CaveArt.DARK,a),L.w*.004f);
            } else if(kind==Cave.ROCKS) {
                for(int k=0;k<3;k++) CaveArt.stone(p,(x+(k-1)*.032f)*L.w,y+k*L.w*.015f,L.w*.026f,i*23+k,a);
            } else {
                p.fillCircle(x*L.w-L.w*.012f,y,L.w*.006f,Glyph.withAlpha(CaveArt.LAMP,a));
                p.fillCircle(x*L.w+L.w*.012f,y,L.w*.006f,Glyph.withAlpha(CaveArt.LAMP,a));
            }
            }
            if(v.hasHeart(i,side) && (!v.hearts[i] || v.routes[i]!=side)) {
                float hz=Cave.FORKS[i]+CaveRoute.HEART_OFFSET,hx=Cave.branchX(i,side,hz);
                CaveArt.heart(p,hx*L.w,v.screenY(hz,L),L.w*.023f,v.illuminated(hx,hz) ? 255 : 80);
            }
        }
    }
    private static void fork(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;float at=v.z+.35f;
        float x=Cave.branchX(v.fork,v.nearestBranch(),at)*L.w,y=v.screenY(at,L);
        for(int i=0;i<4;i++) p.fillCircle(x,y,L.w*(.018f+i*.009f),Glyph.withAlpha(CaveArt.LAMP,35-i*7));
        float remaining=Cave.FORK_WAIT-Math.max(0f,v.timer-(v.lessonSeen ? 0f : Cave.LESSON));
        for(int i=0;i<3;i++) p.fillCircle(v.playerX()*L.w+(i-1)*L.w*.022f,v.playerY(L)+L.w*.075f,
                L.w*.006f,Glyph.withAlpha(CaveArt.LAMP,remaining>i ? 245 : 40));
        if(!v.lessonSeen && v.timer<Cave.LESSON) {
            float t=v.timer/Cave.LESSON;
            float tx=Cave.branchX(v.fork,1,v.z+.42f)*L.w,ty=v.screenY(v.z+.42f,L);
            p.strokeCircle(tx,ty,L.w*(.025f+.028f*(t*3f%1f)),Glyph.withAlpha(CaveArt.LAMP,180),L.w*.004f);
            for(int i=1;i<=6;i++) {
                float z=v.z+i*.065f;
                p.fillCircle(Cave.branchX(v.fork,1,z)*L.w,v.screenY(z,L),L.w*.004f,
                        Glyph.withAlpha(CaveArt.LAMP,180));
            }
            Renderer.touchHint(p,tx,ty,L.w*.045f,.65f,1f,c.clock);
        }
    }
    private static void enemy(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;float x=v.enemyX*L.w,y=v.screenY(v.enemyZ,L),r=L.w*.055f;
        if(v.phase==Cave.SHADOW) {
            p.fillEllipse(x,y,r,r*.8f,0xFF120C09);
            for(int side=-1;side<=1;side+=2) p.fillEllipse(x+side*r*.30f,y-r*.12f,r*.10f,r*.065f,CaveArt.LAMP);
            if(v.z<1f) Renderer.touchHint(p,x,y,r*.72f,.65f,.7f,c.clock);
            return;
        }
        p.fillEllipse(x,y+r*.7f,r*1.1f,r*.28f,Glyph.withAlpha(0xFF000000,140));
        Kawaii.determined(p,Kawaii.BLOB,x,y,r,Glyph.mix(Glyph.COLOR[v.response[0]],CaveArt.MID,.3f),1f+v.pulse*.15f);
        response(p,c,L,v.response,v.responsePos,v.responseSize,y-r*1.45f);
    }
    private static void response(Painter p,GameCore c,Layout L,int[] glyphs,int pos,int count,float y) {
        float gap=L.w*.073f,r=L.w*.026f;
        float x0=Math.max(r*1.5f,Math.min(L.w-r*1.5f-gap*(count-1),c.cave.playerX()*L.w-gap*(count-1)/2f));
        for(int i=0;i<count;i++) {
            float x=x0+i*gap;int a=i<pos && c.cave.phase!=Cave.SAND ? 65 : 255;
            p.fillCircle(x,y,r*1.3f,CaveArt.DARK);
            Kawaii.draw(p,glyphs[i],x,y,r,Glyph.withAlpha(Glyph.COLOR[glyphs[i]],a),1f,.5f);
            if(i==pos) p.strokeCircle(x,y,r*1.4f,CaveArt.LAMP,L.w*.003f);
        }
    }
    private static void trap(Painter p,GameCore c,Layout L) {
        Cave v=c.cave;CaveTraps t=v.traps;float py=v.playerY(L);
        if(v.phase==Cave.SAND) {
            for(int i=4;i>0;i--) p.fillEllipse(v.playerX()*L.w,py+L.w*.025f,L.w*(.055f+i*.017f),L.w*(.015f+i*.008f),
                    i%2==0 ? CaveArt.LIGHT : CaveArt.FLOOR);
            return;
        }
        p.fillPoly(pill(L.w*.5f,py+L.w*.01f,L.w*.39f,L.w*.115f,16),CaveArt.DARK);
        p.fillPoly(pill(L.w*.5f,py,L.w*.37f,L.w*.10f,16),CaveArt.LIGHT);
        p.fillPoly(pill(L.w*.5f,py-L.w*.006f,L.w*.355f,L.w*.084f,16),CaveArt.FLOOR);
        if(v.phase!=Cave.ROCKS) return;
        for(int i=0;i<CaveTraps.ROCK_COUNT;i++) {
            float fall=t.rockProgress(i);
            if(fall<0f || t.landed[i]) continue;
            float x=t.lanes[i]*L.w;
            p.fillEllipse(x,py,L.w*CaveTraps.ROCK_R,L.w*.024f,Glyph.withAlpha(0xFF100A07,(int)(70+100*Math.min(1f,fall))));
            CaveArt.stone(p,x,py-(1f-fall)*L.w*.70f,L.w*CaveTraps.ROCK_R,i*77,255);
        }
        if(t.age<CaveTraps.WARNING) {
            float x=L.w*(.5f+.20f*(float)Math.sin(t.age*4f));
            Renderer.touchHint(p,x,py,L.w*.04f,1.1f,1f,c.clock);
            for(int i=0;i<6;i++) p.fillCircle(L.w*(.2f+i*.12f),py-L.w*.6f+t.age*L.w*.2f,L.w*.004f,CaveArt.LIGHT);
        }
    }
}
