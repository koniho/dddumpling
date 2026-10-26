package com.dddumpling.game;

/** Draw-command census, not GPU timings. Compare vector and cached crystal paths. */
final class ProfileCrystalField {
    public static void main(String[] args) {
        Layout l=new Layout();l.compute(393,852,0,0,0,0);
        GameCore c=TestBoss.enterBoss(l,Boss.SPLITTER,248);c.stage=10;c.landBlend=1;c.clock=c.skyClock=6;
        for(boolean cached:new boolean[]{false,true}) {
            Counter p=new Counter(cached);
            c.crystalField.draw(p,c,l,1);
            System.out.println((cached?"cached":"vector")+" background: "+p.calls+" draws / "+p.vertices+" vertices");
        }
        Counter boss=new Counter(false);BossScreen.body(boss,c,l);
        System.out.println("boss body: "+boss.calls+" draws / "+boss.vertices+" vertices");
    }
    static final class Counter implements Painter,ArtStamp {
        final boolean cached;
        Counter(boolean cached){this.cached=cached;}
        public ArtStamp stamps(){return cached?this:null;}
        public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
        public boolean crystal(int layer,float x,float y,float w,float h,int alpha) {if(!cached)return false;point(x,y,alpha);vertices+=4;return true;}
        public boolean basin(int art,float x,float y,float w,float h,int alpha){point(x,y,alpha);vertices+=4;return true;}
        public boolean basinMesh(int art,float x,float y,float w,float h,float[] offsets,int alpha) {
            point(x,y,alpha);vertices+=(SlimeBanks.ROWS+1)*2;return true;
        }
        long calls,vertices;double checksum;
        void path(float[] pts,int color) {
            calls++;vertices+=pts.length/2;checksum+=color;
            for(float v:pts)checksum+=v;
        }
        void point(float x,float y,int color) {calls++;checksum+=x+y+color;}
        public void fillPoly(float[] p,int c){path(p,c);}
        public void fillContours(float[][] p,int c){for(float[] q:p)path(q,c);}
        public void strokePoly(float[] p,int c,float w){path(p,c);}
        public void polyline(float[] p,int c,float w){path(p,c);}
        public void fillCircle(float x,float y,float r,int c){point(x,y,c);}
        public void strokeCircle(float x,float y,float r,int c,float w){point(x,y,c);}
        public void arc(float x,float y,float rx,float ry,float a,float s,int c,float w){point(x,y,c);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){point(x,y,c);}
        public void fillRect(float l,float t,float r,float b,int c){point(l,t,c);}
        public void line(float x,float y,float a,float b,int c,float w){point(x,y,c);}
        public void text(String s,float x,float y,float size,int c,int align,boolean bold){point(x,y,c);}
        public void clipRect(float l,float t,float r,float b){}
        public void clipOutCircle(float x,float y,float r){}
        public void save(){}
        public void restore(){}
        public void translate(float x,float y){}
    }
}
