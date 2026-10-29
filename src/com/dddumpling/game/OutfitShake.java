package com.dddumpling.game;

/** Android acceleration in g: two distinct peaks, not a tilt or a single bump. */
final class OutfitShake {
    private long first=-1;
    private boolean high;
    void reset(){first=-1;high=false;}
    boolean sample(float x,float y,float z,long milliseconds) {
        float force=x*x+y*y+z*z;
        if(Float.isNaN(force) || Float.isInfinite(force)){reset();return false;}
        if(first>=0 && (milliseconds<first || milliseconds-first>750))first=-1;
        if(force<1.25f*1.25f){high=false;return false;}
        if(high || force<1.75f*1.75f)return false;
        high=true;
        if(first>=0 && milliseconds-first>=90){reset();high=true;return true;}
        if(first<0)first=milliseconds;
        return false;
    }
}
