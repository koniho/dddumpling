package com.dddumpling.game;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Bounded browser textures for the same expensive art cached by Android and iOS. */
final class WebStamps implements ArtStamp {
    private final WebPainter p;
    WebStamps(WebPainter p) {this.p=p;}
    static JSObject image(String group,int art) {
        String key=group+art;
        JSObject image=find(key);if(image!=null)return image;
        int w=256,h=256;
        switch(group) {
            case "basin":w=SlimeBasinArt.width(art);h=SlimeBasinArt.height(art);break;
            case "crystal":w=CrystalField.STAMP_WIDTH;h=CrystalField.STAMP_HEIGHT;break;
            case "vista":w=UnderwaterVista.width(art);h=UnderwaterVista.height(art);break;
            case "rock":w=Underwater.ROCK_WIDTH;h=Underwater.ROCK_HEIGHT;break;
            case "gator":w=h=384;break;
            case "water":w=art==0?2:Underwater.WATER_TEXTURE;h=Underwater.WATER_TEXTURE;break;
        }
        image=create(w,h);WebPainter q=new WebPainter(context(image),false);
        switch(group) {
            case "water":int[] pixels=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)pixels[y*w+x]=Underwater.waterPixel(x,y,art==0);pixels(context(image),pixels,w,h);break;
            case "basin":SlimeBasinArt.paint(q,art,w,h);break;
            case "crystal":CrystalField.paintLayer(q,art);break;
            case "vista":UnderwaterVista.paint(q,art);break;
            case "rock":Underwater.paintRock(q,art);break;
            case "gator":q.translate(w*.5f,h*.5f);AlligatorArt.stampArt(q,art,w/2.5f);break;
            case "forest":q.translate(w*.5f,h*.5f);MushroomForest.stampArt(q,art,w/5f);break;
        }
        remember(key,image);return image;
    }
    public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount) {return greenStamp(art,x,y,w,h,angle,tint,amount,0);}
    public boolean greenStamp(int art,float x,float y,float w,float h,float angle,int tint,float amount,float phase) {
        float[] m=new float[20];GatorPalette.matrix(m,tint,amount,phase);
        drawMatrix(p.context,image("gator",art),x,y,-w*1.25f,-h*1.25f,w*2.5f,h*2.5f,angle,255,m);return true;
    }
    public boolean basin(int art,float x,float y,float w,float h,int alpha) {draw(p.context,image("basin",art),x,y,w,h,alpha,0,1,0);return true;}
    public boolean basinMesh(int art,float x,float y,float w,float h,float[] offsets,int alpha) {mesh(p.context,image("basin",art),x,y,w,h,offsets,SlimeBasinArt.side(art)==0,alpha);return true;}
    public boolean crystal(int art,float x,float y,float w,float h,int alpha) {return crystal(art,x,y,w,h,alpha,0);}
    public boolean crystal(int art,float x,float y,float w,float h,int alpha,float mono) {draw(p.context,image("crystal",art),x,y,w,h,alpha,mono,1,0);return true;}
    public boolean reefVista(int art,float x,float y,float w,float h,int alpha) {return reefVista(art,x,y,w,h,alpha,0);}
    public boolean reefVista(int art,float x,float y,float w,float h,int alpha,float mono) {return reefVistaShaded(art,x,y,w,h,alpha,1,mono);}
    public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {draw(p.context,image("vista",art),x,y,w,h,alpha,mono,shade,UnderwaterVista.waterColor(mono));return true;}
    public boolean reefRock(int art,float x,float y,float w,float h,int alpha) {return reefRock(art,x,y,w,h,alpha,0);}
    public boolean reefRock(int art,float x,float y,float w,float h,int alpha,float mono) {draw(p.context,image("rock",art),x-w*1.3f,y-h*1.35f,w*3.1f,h*2.95f,alpha,mono,1,0);return true;}
    public boolean forest(int art,float x,float y,float r,float lean,int color,int alpha) {
        float angle=art<3?(Math.abs(lean)-1)*29*(float)Math.PI/180:lean-MushroomForest.litterAngle(art);
        float[] m=new float[20];m[0]=color==0?1:((color>>>16)&255)/113f;m[6]=color==0?1:((color>>>8)&255)/145f;m[12]=color==0?1:(color&255)/122f;m[18]=1;
        forest(p.context,image("forest",art),x,y,r,angle,art<3 && lean<0,alpha,m);return true;
    }
    public boolean waterGlow(float x,float y,float rx,float ry,int alpha) {return waterGlow(x,y,rx,ry,alpha,0);}
    public boolean waterGlow(float x,float y,float rx,float ry,int alpha,float mono) {draw(p.context,image("water",1),x-rx,y-ry,rx*2,ry*2,alpha,mono,1,0);return true;}
    public boolean waterBand(float[] xy,float radius,int alpha) {return waterBand(xy,radius,alpha,0);}
    public boolean waterBand(float[] xy,float radius,int alpha,float mono) {band(p.context,image("water",0),xy,radius,alpha,mono);return true;}
    @JSBody(params={"c","pixels","w","h"},script="ddd.cache.pixels(c,pixels,w,h);") private static native void pixels(JSObject c,int[] pixels,int w,int h);
    @JSBody(params={"c","img","xy","r","a","mono"},script="ddd.cache.band(c,img,xy,r,a,mono);") private static native void band(JSObject c,JSObject img,float[] xy,float r,int a,float mono);
    @JSBody(params="key",script="return ddd.cache.get(key);") private static native JSObject find(String key);
    @JSBody(params={"w","h"},script="return ddd.cache.create(w,h);") private static native JSObject create(int w,int h);
    @JSBody(params="c",script="return c.getContext('2d');") private static native JSObject context(JSObject c);
    @JSBody(params={"key","image"},script="ddd.cache.put(key,image);") private static native void remember(String key,JSObject image);
    @JSBody(params={"c","img","x","y","w","h","a","mono","shade","water"},script="ddd.cache.draw(c,img,x,y,w,h,a,mono,shade,water);") private static native void draw(JSObject c,JSObject img,float x,float y,float w,float h,int a,float mono,float shade,int water);
    @JSBody(params={"c","img","x","y","l","t","w","h","angle","a","m"},script="ddd.cache.matrix(c,img,x,y,l,t,w,h,angle,a,m);") private static native void drawMatrix(JSObject c,JSObject img,float x,float y,float l,float t,float w,float h,float angle,int a,float[] m);
    @JSBody(params={"c","img","x","y","r","angle","flip","a","m"},script="c.save();c.translate(x,y);c.scale(flip?-1:1,1);ddd.cache.matrix(c,img,0,0,-r*2.5,-r*2.5,r*5,r*5,angle,a,m);c.restore();") private static native void forest(JSObject c,JSObject img,float x,float y,float r,float angle,boolean flip,int a,float[] m);
    @JSBody(params={"c","img","x","y","w","h","offsets","left","a"},script="ddd.cache.mesh(c,img,x,y,w,h,offsets,left,a);") private static native void mesh(JSObject c,JSObject img,float x,float y,float w,float h,float[] offsets,boolean left,int a);
}
