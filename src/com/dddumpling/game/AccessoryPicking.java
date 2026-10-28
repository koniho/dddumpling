package com.dddumpling.game;

/** Trace only worn attachment regions on tap, using the same body and dance transforms as drawing. */
final class AccessoryPicking implements Painter {
    private static final int[] FRONT={Accessories.HAT,Accessories.BOOTS,Accessories.GLASSES,Accessories.MASK,Accessories.FLAIR};
    final float[][] targets=new float[Accessories.SLOTS][];
    private GameCore core;
    private float width=640;
    int hit(GameCore c,Layout L,float x,float y) {
        capture(c,L);
        for(int slot:FRONT)if(contains(slot,x,y,L.unit*.45f))return slot;
        return -1;
    }
    void capture(GameCore c,Layout L) {
        java.util.Arrays.fill(targets,null);core=c;width=L.w;
        if(c.storyOpen())Storybook.hero(this,c,L);
    }
    boolean contains(int slot,float x,float y,float pad) {
        float[] q=targets[slot];if(q==null)return false;
        float ux=q[2]-q[0],uy=q[3]-q[1],vx=q[6]-q[0],vy=q[7]-q[1];
        float det=ux*vy-uy*vx;if(Math.abs(det)<.001f)return false;
        float dx=x-q[0],dy=y-q[1],u=(dx*vy-dy*vx)/det,v=(ux*dy-uy*dx)/det;
        if(slot==Accessories.FLAIR) {
            float nx=2*u-1,ny=2*v-1,d=nx*nx+ny*ny;
            return d>=.38f && d<=1.12f;
        }
        float pu=pad/(float)Math.hypot(ux,uy),pv=pad/(float)Math.hypot(vx,vy);
        return u>=-pu && u<=1+pu && v>=-pv && v<=1+pv;
    }
    public int accessory(int who,int slot) {return core.wardrobe.equipped(who,slot);}
    public float accessorySize(float radius) {return radius/width;}
    public int skin() {return core.preferences.skin;}
    public DuckBodies ducks() {return core.ducks;}
    public GatorCompanion gator() {return core.gator;}
    public boolean accessoryTargets() {return true;}
    public void accessoryTarget(int slot,float[] corners) {targets[slot]=corners;}
    public void fillPoly(float[] p,int c) {}
    public void fillContours(float[][] p,int c) {}
    public void strokePoly(float[] p,int c,float w) {}
    public void polyline(float[] p,int c,float w) {}
    public void fillCircle(float x,float y,float r,int c) {}
    public void strokeCircle(float x,float y,float r,int c,float w) {}
    public void fillEllipse(float x,float y,float rx,float ry,int c) {}
    public void arc(float x,float y,float rx,float ry,float a,float sweep,int c,float w) {}
    public void fillRect(float l,float t,float r,float b,int c) {}
    public void line(float x,float y,float xx,float yy,int c,float w) {}
    public void text(String t,float x,float y,float size,int c,int align,boolean bold) {}
    public void clipRect(float l,float t,float r,float b) {}
    public void clipOutCircle(float x,float y,float r) {}
    public void save() {}
    public void restore() {}
    public void translate(float x,float y) {}
}
