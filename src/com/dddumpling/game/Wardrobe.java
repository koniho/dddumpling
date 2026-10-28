package com.dddumpling.game;

/** One global copy of each style; logical assignment clocks resolve concurrent cloud edits. */
final class Wardrobe {
    // Collect indices are existing append-only prize IDs. Zero means no wearer.
    static final int OWNER_BITS=10, OWNER_MASK=(1<<OWNER_BITS)-1;
    final boolean[] owned=new boolean[Accessories.COUNT];
    final long[] assignment=new long[Accessories.COUNT];
    long revision=-1;
    final WardrobeScreen screen=new WardrobeScreen();
    void refresh(GameCore c) {
        if(revision==c.progress.revision)return;
        java.util.ArrayList<String> keys=new java.util.ArrayList<>();
        for(int i=0;i<Accessories.COUNT;i++) {
            int a=Accessories.achievement(i);
            if(c.progress.maximum(Accessories.key("owned",i))==0
                    && Achievements.steps(c.progress,a)>=Achievements.target(a))keys.add(Accessories.key("owned",i));
        }
        if(!keys.isEmpty()) {
            long[] values=new long[keys.size()];java.util.Arrays.fill(values,1);
            c.progress.accessories(keys.toArray(new String[0]),values,false);
        }
        for(int i=0;i<Accessories.COUNT;i++) {
            owned[i]=c.progress.maximum(Accessories.key("owned",i))>0;
            assignment[i]=c.progress.maximum(Accessories.key("wearer",i));
        }
        revision=c.progress.revision;
    }
    int rawOwner(int item) {return (int)(assignment[item]&OWNER_MASK)-1;}
    int equipped(int who,int slot) {
        if(who<0 || who>=Collect.COUNT)return -1;
        int best=-1;
        for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==slot && owned[i] && rawOwner(i)==who
                && (best<0 || assignment[i]>assignment[best] || assignment[i]==assignment[best] && i>best))best=i;
        return best;
    }
    int wearer(int item) {
        int who=rawOwner(item);
        return equipped(who,Accessories.slot(item))==item?who:-1;
    }
    boolean equip(GameCore c,int item,int who) {
        refresh(c);
        if(item<0 || item>=Accessories.COUNT || !owned[item] || who>=Collect.COUNT || who< -1
                || who>=0 && !Collect.has(c.collected,who))return false;
        long clock=0;
        for(long v:assignment)clock=Math.max(clock,v>>>OWNER_BITS);
        if(clock>=(Long.MAX_VALUE>>>OWNER_BITS)-1)return false;
        long base=(clock+1)<<OWNER_BITS;
        java.util.ArrayList<String> keys=new java.util.ArrayList<>();
        java.util.ArrayList<Long> values=new java.util.ArrayList<>();
        // Retire all competing claims, including hidden losers from cloud merges.
        for(int i=0;i<Accessories.COUNT;i++)if(i==item || who>=0 && rawOwner(i)==who && Accessories.slot(i)==Accessories.slot(item)) {
            keys.add(Accessories.key("wearer",i));values.add(base+(i==item?who+1:0));
        }
        long[] out=new long[values.size()];for(int i=0;i<out.length;i++)out[i]=values.get(i);
        boolean ok=c.progress.accessories(keys.toArray(new String[0]),out,false);refresh(c);return ok;
    }
    int pending(GameCore c) {
        for(int i=0;i<Accessories.COUNT;i++)if(owned[i] && c.progress.maximum(Accessories.key("seen",i))==0)return i;
        return -1;
    }
    boolean acknowledge(GameCore c) {
        String[] keys=new String[Accessories.COUNT];long[] values=new long[keys.length];
        for(int i=0;i<keys.length;i++){keys[i]=Accessories.key("seen",i);values[i]=owned[i]?1:0;}
        return c.progress.accessories(keys,values,false);
    }
    boolean categoryOwned(int category) {
        for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==category && !owned[i])return false;
        return category>=0 && category<Accessories.SLOTS;
    }
    void developer(GameCore c,int action) {
        if(!BuildFlags.DEVELOPER || action<0 || action>=3+Accessories.SLOTS)return;
        refresh(c);
        if(action==2) {
            c.progress.accessories(new String[0],new long[0],true);screen.close();
        } else {
            java.util.ArrayList<String> keys=new java.util.ArrayList<>();
            for(int i=0;i<Accessories.COUNT;i++) {
                if(action>=3 && Accessories.slot(i)!=action-3)continue;
                if(action==0 && owned[i])continue;
                keys.add(Accessories.key("owned",i));
                if(action==0)break;
            }
            long[] values=new long[keys.size()];java.util.Arrays.fill(values,1);
            c.progress.accessories(keys.toArray(new String[0]),values,false);
        }
        revision=-1;refresh(c);
    }
}
