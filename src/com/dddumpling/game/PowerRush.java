package com.dddumpling.game;

/** Four arrivals make a phrase: diagonal rain, alternating sides, then a center fan. */
final class PowerRush {
    private PowerRush() {}

    static boolean pressured(GameCore c,Layout L) {
        float line=L.playTop+(L.dangerY-L.playTop)*.60f;
        for(GameCore.Enemy e:c.enemies)
            if(!e.destroyed && !e.dying && (e.attacking || e.y>=line))return true;
        return false;
    }

    static int pattern(GameCore c) { return (c.powerSpawnedEnemies/4)%3; }

    static float phraseDelay(GameCore c,float normal,float refill,Layout L) {
        if(pressured(c,L))return normal;
        if(c.powerSpawnedEnemies>0 && c.powerSpawnedEnemies%4==0)return normal*1.25f;
        return refill;
    }

    static void arrange(GameCore c,GameCore.Enemy e,Layout L,float lo,float hi) {
        if(!c.powerActive() || pressured(c,L))return;
        int slot=c.powerSpawnedEnemies%4,pattern=pattern(c);
        float lane=pattern==0 ? .18f+slot*.2133f
                : pattern==1 ? (slot%2==0?.55f:.45f)
                : slot<2 ? .42f+slot*.16f : slot==2?.22f:.78f;
        e.baseX=hi>lo ? lo+(hi-lo)*lane : (L.playLeft+L.playRight)*.5f;
        e.sway=0f;
        boolean side=hi>lo && (pattern==1 || (pattern==2 && slot>=2));
        if(side) {
            boolean left=slot%2==0;
            float half=L.wordWidth(e.word.length)*.5f;
            e.sideEntry=true;
            e.pathStartX=left ? L.playLeft-half-L.enemyR : L.playRight+half+L.enemyR;
            e.pathEndX=e.baseX;
            e.pathStartY=L.playTop+(L.dangerY-L.playTop)*(.12f+slot*.035f);
            e.baseX=e.pathStartX;e.y=e.pathStartY;
        }
        float depth=pattern==0?.26f+slot*.04f:.34f+slot*.015f;
        rush(c,e,L,depth);
    }

    static void pair(GameCore c,GameCore.Enemy e,Layout L) {
        if(c.powerActive() && !pressured(c,L))rush(c,e,L,.30f);
    }

    private static void rush(GameCore c,GameCore.Enemy e,Layout L,float depth) {
        if(c.powerRefillBurst>0)depth=Math.min(.42f,depth+.04f);
        e.rushEndY=L.playTop+(L.dangerY-L.playTop)*depth;
        e.rushSpan=Math.max(1f,e.rushEndY-e.y);
    }

    /** Integrate the entrance boost exactly; velocity settles continuously to normal at the join. */
    static float yAfter(GameCore.Enemy e,float distance) {
        if(e.rushSpan<=0f || e.y>=e.rushEndY)return e.y+distance;
        float k=4f/e.rushSpan;
        float toJoin=(float)Math.log1p(k*(e.rushEndY-e.y))/k;
        if(distance>=toJoin)return e.rushEndY+distance-toJoin;
        return e.y+(e.rushEndY+1f/k-e.y)*(float)-Math.expm1(-k*distance);
    }
}
