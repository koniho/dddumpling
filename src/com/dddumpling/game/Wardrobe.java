package com.dddumpling.game;

/** Earn once, wear on any squishy. Each character's slots are independent saved registers. */
final class Wardrobe {
    static final int ITEM_BITS=10, ITEM_MASK=(1<<ITEM_BITS)-1;
    private static final String[] SLOT_ID={"hat","glasses","boots","mask","flair"};
    final boolean[] owned=new boolean[Accessories.COUNT];
    final long[][] outfits=new long[Collect.COUNT][Accessories.SLOTS];
    long revision=-1;
    final WardrobeScreen screen=new WardrobeScreen();
    static String outfitKey(int who,int slot) {
        return "accessory_outfit_"+Collect.progressKey(who)+"_"+SLOT_ID[slot];
    }
    void refresh(GameCore c) {
        if(revision==c.progress.revision)return;
        java.util.ArrayList<String> keys=new java.util.ArrayList<>();
        java.util.ArrayList<Long> values=new java.util.ArrayList<>();
        for(int i=0;i<Accessories.COUNT;i++) {
            int a=Accessories.achievement(i);
            if(c.progress.maximum(Accessories.key("owned",i))==0
                    && Achievements.steps(c.progress,a)>=Achievements.target(a)) {
                keys.add(Accessories.key("owned",i));values.add(1L);
            }
        }
        for(int who=0;who<Collect.COUNT;who++)for(int slot=0;slot<Accessories.SLOTS;slot++) {
            long record=c.progress.maximum(outfitKey(who,slot));
            if(record==0) {
                record=legacyOutfit(c,who,slot);
                if(record>0) {keys.add(outfitKey(who,slot));values.add(record);}
            }
            outfits[who][slot]=record;
        }
        if(!keys.isEmpty()) {
            long[] out=new long[values.size()];for(int i=0;i<out.length;i++)out[i]=values.get(i);
            c.progress.accessories(keys.toArray(new String[0]),out,false);
        }
        for(int i=0;i<Accessories.COUNT;i++)owned[i]=c.progress.maximum(Accessories.key("owned",i))>0;
        revision=c.progress.revision;
    }
    /** Import the old exclusive wearer once per slot; a saved removal also fences old cloud data. */
    private long legacyOutfit(GameCore c,int who,int slot) {
        long best=0;int item=-1;
        for(int i=0;i<Accessories.COUNT;i++)if(Accessories.slot(i)==slot) {
            long old=c.progress.maximum(Accessories.key("wearer",i));
            if((old&ITEM_MASK)==who+1 && old>=best) {best=old;item=i;}
        }
        return item<0?0:(best&~(long)ITEM_MASK)|(item+1L);
    }
    int equipped(int who,int slot) {
        if(who<0 || who>=Collect.COUNT || slot<0 || slot>=Accessories.SLOTS)return -1;
        int item=(int)(outfits[who][slot]&ITEM_MASK)-1;
        return item>=0 && item<Accessories.COUNT && owned[item] && Accessories.slot(item)==slot?item:-1;
    }
    boolean equip(GameCore c,int item,int who) {
        refresh(c);
        if(item<0 || item>=Accessories.COUNT || !owned[item])return false;
        return saveSlot(c,who,Accessories.slot(item),item);
    }
    boolean remove(GameCore c,int who,int slot) {
        refresh(c);return saveSlot(c,who,slot,-1);
    }
    private boolean saveSlot(GameCore c,int who,int slot,int item) {
        if(who<0 || who>=Collect.COUNT || !Collect.has(c.collected,who) || slot<0 || slot>=Accessories.SLOTS)return false;
        long clock=outfits[who][slot]>>>ITEM_BITS;
        if(clock>=(Long.MAX_VALUE>>>ITEM_BITS)-1)return false;
        long record=((clock+1)<<ITEM_BITS)|(item+1L);
        boolean ok=c.progress.accessories(new String[]{outfitKey(who,slot)},new long[]{record},false);
        refresh(c);return ok;
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
