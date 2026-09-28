package com.dddumpling.game;

/** Side-heavy power entrances vary their landing lane and arrive farther down the field. */
final class PowerRush {
    private PowerRush() {}

    static boolean pressured(GameCore c,Layout L) {
        float line=L.playTop+(L.dangerY-L.playTop)*.60f;
        for(GameCore.Enemy e:c.enemies)
            if(!e.destroyed && !e.dying && (e.attacking || e.y>=line))return true;
        return false;
    }

    static int pattern(GameCore c) { return (c.powerSpawnedEnemies/4)%3; }

    static float spawnSpeed(GameCore c,GameCore.Enemy e,Layout L) {
        // Lower entrances shorten the journey, never the descent speed.
        if(e.sideEntry || c.ninja())return (L.dangerY+L.enemyR*2.2f)/c.travelSeconds();
        return (L.dangerY-e.y)/c.travelSeconds();
    }

    static void arrange(GameCore c,GameCore.Enemy e,Layout L,float lo,float hi) {
        if(!c.powerActive())return;
        boolean danger=pressured(c,L);
        int slot=c.powerSpawnedEnemies%4,pattern=pattern(c);
        float lane=pattern==0 ? .18f+slot*.2133f
                : pattern==1 ? (slot%2==0?.55f:.45f)
                : slot<2 ? .42f+slot*.16f : slot==2?.22f:.78f;
        e.baseX=hi>lo ? lo+(hi-lo)*lane : (L.playLeft+L.playRight)*.5f;
        e.sway=0f;
        boolean side=hi>lo && (pattern==1 || slot!=0);
        if(side) {
            boolean left=slot%2==0;
            float half=L.wordWidth(e.word.length)*.5f;
            e.sideEntry=true;
            e.pathStartX=left ? L.playLeft-half-L.enemyR : L.playRight+half+L.enemyR;
            e.pathEndX=e.baseX;
            float depth=danger ? .20f+slot*.025f : .24f+pattern*.04f+slot*.055f;
            e.pathStartY=L.playTop+(L.dangerY-L.playTop)*depth;
            e.baseX=e.pathStartX;e.y=e.pathStartY;
        }
        if(danger)return;
        float depth=side ? Math.min(.55f,(e.y-L.playTop)/(L.dangerY-L.playTop)+.08f)
                : .26f+slot*.04f;
        rush(c,e,L,depth);
    }

    /** A blocked preferred lane must not stall Ninja's whole stream. */
    static void ninjaAlternative(GameCore c,GameCore.Enemy e,Layout L,int alternative) {
        boolean danger=pressured(c,L),left=c.powerSpawnedEnemies%2==0;
        float depth=danger ? .12f+alternative*.05f
                : .18f+((alternative+c.powerSpawnedEnemies)%3)*.16f;
        float half=L.wordWidth(e.word.length)*.5f;
        e.sideEntry=true;e.sway=0;
        e.pathStartX=left ? L.playLeft-half-L.enemyR : L.playRight+half+L.enemyR;
        e.pathStartY=L.playTop+(L.dangerY-L.playTop)*depth;
        e.y=e.pathStartY;e.baseX=e.pathStartX;e.rushSpan=0;
        if(!danger)rush(c,e,L,Math.min(.55f,depth+.08f));
    }

    static void pair(GameCore c,GameCore.Enemy e,Layout L) {
        if(c.powerActive() && !pressured(c,L))rush(c,e,L,.30f);
    }

    private static void rush(GameCore c,GameCore.Enemy e,Layout L,float depth) {
        if(c.powerRefillBurst>0)depth=Math.min(.55f,depth+.04f);
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
