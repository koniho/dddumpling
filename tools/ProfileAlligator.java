package com.dddumpling.game;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Drawing preparation only: no rasterization, Android Canvas, or GPU timing.
 * Compile with javac -cp build/harness -d build/harness tools/ProfileAlligator.java
 * Run with java -cp build/harness com.dddumpling.game.ProfileAlligator
 * Pass cached to count the optional bitmap-stamp path (still no GPU measurement).
 */
final class ProfileAlligator {
    private static volatile double consumed;
    public static void main(String[] args) {
        Layout l=new Layout();l.compute(393,852,0,0,0,0);
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        boolean allocations=bean.isThreadAllocatedMemorySupported();
        if(allocations)bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().getId();
        boolean cached=args.length>0 && args[0].equals("cached");
        System.out.println("Gator drawing preparation; excludes rasterization/GPU. Portrait 393x852. Cached="+cached);
        for(String state:new String[]{"idle","entry","claw strike","combo demo"}) {
            GameCore c=TestAlligator.fight(l);Alligator a=c.boss.alligator;
            if(state.equals("entry"))c.boss.intro=c.boss.introDuration()*.45f;
            if(state.equals("claw strike")) {a.next=0;a.timer=0;TestAlligator.advance(c,l,.3f);}
            if(state.equals("combo demo")) {
                c.boss.hp=c.boss.hpMax*.25f;c.boss.damage(1);
                TestAlligator.advance(c,l,Alligator.SHOWOFF_LEAD+Alligator.SHOWOFF_BEAT*2.5f);
            }
            Counter p=new Counter(cached);
            long warmUntil=System.nanoTime()+1_000_000_000L;
            while(System.nanoTime()<warmUntil)AlligatorArt.draw(p,c,l,c.boss,1);
            long[] batches=new long[100];int frames=0;
            long bytes=allocations?bean.getThreadAllocatedBytes(thread):0;
            for(int batch=0;batch<batches.length;batch++) {
                long start=System.nanoTime();
                for(int f=0;f<50;f++){AlligatorArt.draw(p,c,l,c.boss,1);frames++;}
                batches[batch]=(System.nanoTime()-start)/50;
            }
            bytes=allocations?bean.getThreadAllocatedBytes(thread)-bytes:-1;
            consumed=p.checksum;Arrays.sort(batches);
            p.calls=p.vertices=0;AlligatorArt.draw(p,c,l,c.boss,1);
            System.out.printf(java.util.Locale.ROOT,
                    "%s: median %.3f ms, p95 batch %.3f ms, %.1f KiB/frame, %d draws, %d vertices%n",
                    state,batches[50]/1e6,batches[95]/1e6,bytes<0?-1:bytes/(1024.0*frames),p.calls,p.vertices);
        }
    }
    private static final class Counter implements Painter,ArtStamp {
        final boolean cached;
        Counter(boolean cached) {this.cached=cached;}
        public ArtStamp stamps(){return cached?this:null;}
        public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount) {
            point(x,y,tint);vertices+=4;return true;
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
