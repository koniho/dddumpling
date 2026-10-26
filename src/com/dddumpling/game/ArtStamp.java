package com.dddumpling.game;

/** Optional host cache for immutable local artwork. Unsupported painters use vectors. */
interface ArtStamp {
    int PALM=0, PEBBLE=1, COUNT=2;
    /** Alpha is applied to the completed foliage image, avoiding hundreds of translucent paths. */
    default boolean forest(int art,float x,float y,float r,float lean,int color,int alpha) {return false;}
    default boolean waterBand(float[] xy,float radius,int alpha) {return false;}
    default boolean waterGlow(float x,float y,float rx,float ry,int alpha) {return false;}
    default boolean reefVista(int art,float x,float y,float w,float h,int alpha) {return false;}
    // shade is palette clarity: 0 uses water color, 1 uses the cached rock palette.
    default boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
        return shade==1 && reefVista(art,x,y,w,h,alpha,mono);
    }
    default boolean reefRock(int art,float x,float y,float w,float h,int alpha) {return false;}
    default boolean crystal(int layer,float x,float y,float w,float h,int alpha) {return false;}
    default boolean basin(int art,float x,float y,float w,float h,int alpha) {return false;}
    default boolean basinMesh(int art,float x,float y,float w,float h,float[] offsets,int alpha) {return false;}
    /** Hosts opt in to filtering cached images; unsupported caches retain the vector palette. */
    default boolean waterBand(float[] xy,float radius,int alpha,float mono) {return mono==0 && waterBand(xy,radius,alpha);}
    default boolean waterGlow(float x,float y,float rx,float ry,int alpha,float mono) {return mono==0 && waterGlow(x,y,rx,ry,alpha);}
    default boolean reefVista(int art,float x,float y,float w,float h,int alpha,float mono) {return mono==0 && reefVista(art,x,y,w,h,alpha);}
    default boolean reefRock(int art,float x,float y,float w,float h,int alpha,float mono) {return mono==0 && reefRock(art,x,y,w,h,alpha);}
    default boolean crystal(int layer,float x,float y,float w,float h,int alpha,float mono) {return mono==0 && crystal(layer,x,y,w,h,alpha);}
    boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount);
    default boolean greenStamp(int art,float x,float y,float w,float h,float angle,int tint,float amount,float phase) {
        return false;
    }
}
