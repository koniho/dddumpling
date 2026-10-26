package com.dddumpling.game;

/** Optional host cache for immutable local artwork. Unsupported painters use vectors. */
interface ArtStamp {
    int PALM=0, PEBBLE=1, COUNT=2;
    /** Alpha is applied to the completed foliage image, avoiding hundreds of translucent paths. */
    default boolean forest(int art,float x,float y,float r,float lean,int color,int alpha) {return false;}
    default boolean waterBand(float[] xy,float radius,int alpha) {return false;}
    default boolean waterGlow(float x,float y,float rx,float ry,int alpha) {return false;}
    default boolean reefVista(int art,float x,float y,float w,float h,int alpha) {return false;}
    default boolean reefRock(int art,float x,float y,float w,float h,int alpha) {return false;}
    default boolean crystal(int layer,float x,float y,float w,float h,int alpha) {return false;}
    default boolean basin(int art,float x,float y,float w,float h,int alpha) {return false;}
    default boolean basinMesh(int art,float x,float y,float w,float h,float[] offsets,int alpha) {return false;}
    boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount);
    default boolean greenStamp(int art,float x,float y,float w,float h,float angle,int tint,float amount,float phase) {
        return false;
    }
}
