package com.dddumpling.game;

/** Optional host cache for immutable local artwork. Unsupported painters use vectors. */
interface ArtStamp {
    int PALM=0, PEBBLE=1, COUNT=2;
    boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount);
    default boolean greenStamp(int art,float x,float y,float w,float h,float angle,int tint,float amount,float phase) {
        return false;
    }
}
