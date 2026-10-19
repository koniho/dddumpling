package com.dddumpling.game;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Drawing preparation only: no rasterization, Android Canvas, or GPU timing.
 * Compile with javac -cp build/harness -d build/harness tools/ProfileRainbowCatTaunt.java
 * Run with java -cp build/harness com.dddumpling.game.ProfileRainbowCatTaunt
 */
final class ProfileRainbowCatTaunt {
    private static volatile double consumed;
    public static void main(String[] args) {
        Layout l=new Layout();l.compute(393,852,0,0,0,0);
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        boolean allocations=bean.isThreadAllocatedMemorySupported();
        if(allocations)bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().getId();
        System.out.println("Rainbow Cat taunt drawing preparation; excludes rasterization/GPU. Portrait 393x852.");
        for(float age:new float[]{2.3f,2.8f,3.6f}) {
            GameCore c=TestRainbowCat.fight(l);c.lives=1;c.takeHit(c.boss.cat.x,l);
            c.deathT=RainbowCatTaunt.DURATION-age;
            String state=age<2.36f?"before mouth":age<3.3f?"drawing mouth":"complete face";
            Counter p=new Counter();
            long warmUntil=System.nanoTime()+1_000_000_000L;
            while(System.nanoTime()<warmUntil)RainbowCatTaunt.draw(p,c,l);
            long[] batches=new long[100];int frames=0;
            long bytes=allocations?bean.getThreadAllocatedBytes(thread):0;
            for(int batch=0;batch<batches.length;batch++) {
                long start=System.nanoTime();
                for(int f=0;f<50;f++){RainbowCatTaunt.draw(p,c,l);frames++;}
                batches[batch]=(System.nanoTime()-start)/50;
            }
            bytes=allocations?bean.getThreadAllocatedBytes(thread)-bytes:-1;
            consumed=p.checksum;Arrays.sort(batches);
            p.calls=p.vertices=0;RainbowCatTaunt.draw(p,c,l);
            System.out.printf(java.util.Locale.ROOT,
                    "%s: median %.3f ms, p95 batch %.3f ms, %.1f KiB/frame, %d draws, %d vertices%n",
                    state,batches[50]/1e6,batches[95]/1e6,bytes<0?-1:bytes/(1024.0*frames),p.calls,p.vertices);
        }
    }
    private static final class Counter implements Painter {
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
