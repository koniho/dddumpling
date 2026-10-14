package com.dddumpling.game;

/** Pocket gator springs belong to the game; every portrait reads the same animated pose. */
final class GatorCompanion extends Draw {
    final Softbody body=new Softbody(16,1681),head=new Softbody(16,1682),tail=new Softbody(12,1683);
    float clock,beat,previousAge,previousClock;
    int previousReaction=-1;
    GatorCompanion() {reset();}
    void reset() {
        body.reset(0,0,1);head.reset(0,0,1);tail.reset(0,0,1);
        body.jiggle=1.4f;head.jiggle=1.6f;tail.jiggle=1.8f;
        clock=beat=previousAge=previousClock=0;previousReaction=-1;
    }
    void react(float amount) {
        body.squash(amount*.45f);head.squash(amount*.50f);tail.squash(amount*.65f);
        head.impulse(.65f,-.55f,amount*.20f);tail.impulse(-.4f,.4f,amount*.25f);
    }
    void update(GameCore c,float dt) {
        if(c.paused || c.settingsOpen)return;
        int who=Collect.ALLIGATOR_FRIEND;
        boolean companion=c.companion.who==who;
        if(!companion && c.runWho!=who && c.story!=who && c.prize!=who
                && !(c.state==GameCore.TITLE && Collect.has(c.collected,who)))return;
        if(companion) {
            if(c.companion.clock<previousClock)reset();
            if(c.companion.reaction!=previousReaction || c.companion.age<previousAge)
                react(.20f+c.companion.strength*.55f);
            previousReaction=c.companion.reaction;previousAge=c.companion.age;previousClock=c.companion.clock;
        }
        clock+=dt;beat+=dt;
        if(beat>=2.4f) {beat%=2.4f;react(.18f);}
        body.moveTo((float)Math.sin(clock*2.6f)*.025f,(float)Math.sin(clock*2.6f)*.035f);
        head.moveTo((float)Math.sin(clock*2.6f-.55f)*.06f,(float)Math.sin(clock*2.6f-.65f)*.055f);
        tail.moveTo((float)Math.sin(clock*4f)*.08f,(float)Math.sin(clock*4f-.8f)*.10f);
        body.update(dt);head.update(dt);tail.update(dt);
    }
    private static float[] skin(Softbody body,float x,float y,float w,float h) {
        float[] pts=body.outline();
        for(int i=0;i<pts.length;i+=2){pts[i]=x+pts[i]*w;pts[i+1]=y+pts[i+1]*h;}
        return pts;
    }
    static void draw(Painter painter,float x,float y,float r,float fallbackClock,boolean known,float fade,
            int mood,float look,boolean ninja) {
        draw(painter,x,y,r,fallbackClock,known,fade,mood,look,ninja,0);
    }
    static void draw(Painter painter,float x,float y,float r,float fallbackClock,boolean known,float fade,
            int mood,float look,boolean ninja,float punch) {
        GatorCompanion a=painter.gator();
        float clock=a==null?fallbackClock:a.clock;
        Painter p=new OpacityPainter(painter,fade);
        if(!known)p=new Silhouette(p);
        float bob=(float)Math.sin(clock*2.6f),wag=(float)Math.sin(clock*4.1f);
        float bx=x+r*(a==null?bob*.025f:a.body.centreX()*.45f),by=y+r*.33f;
        float sx=a==null?1:a.body.radiusX(),sy=a==null?1:a.body.radiusY();
        float tailBounce=a==null?wag*.10f:a.tail.centreY()+(.95f-a.tail.radiusY())*.2f;
        AlligatorArt.limb(p,bx+r*.27f,by+r*.12f,x+r*.89f,y+r*(.44f+tailBounce),r*.115f,true,
                0xFF65BD87,clock);
        // Small feet, soft round torso, and translucent jelly layers with a bright wet rim.
        for(int side=-1;side<=1;side+=2) {
            float fx=bx+side*r*.29f*sx,fy=y+r*(.79f+.035f*bob*side);
            p.fillEllipse(fx,fy,r*.21f,r*.125f,AlligatorArt.DARK);
            p.fillEllipse(fx-r*.018f,fy-r*.025f,r*.19f,r*.108f,0xFF70C895);
            p.arc(fx,fy-r*.015f,r*.15f,r*.07f,203,120,0xB4D3F6CA,r*.024f);
        }
        if(a==null) {
            p.fillEllipse(bx,by,r*.49f,r*.51f,AlligatorArt.DARK);
            p.fillEllipse(bx,by-r*.018f,r*.47f,r*.49f,0xDB68BF86);
        } else {
            p.fillPoly(skin(a.body,x,y+r*.33f,r*.49f,r*.51f),AlligatorArt.DARK);
            p.fillPoly(skin(a.body,x,y+r*.31f,r*.475f,r*.49f),0xEE65B985);
            p.fillPoly(skin(a.body,x-r*.055f,y+r*.25f,r*.41f,r*.42f),0x8097E6A3);
        }
        for(int i=0;i<3;i++) {
            float yy=by+r*(.01f+i*.15f)*sy;
            p.fillEllipse(bx,yy,r*(.36f-i*.035f)*sx,r*.098f*sy,0xB083AD68);
            p.fillEllipse(bx-r*.015f,yy-r*.024f,r*(.34f-i*.035f)*sx,r*.080f*sy,0xDDDCECAF);
            p.arc(bx-r*.04f,yy-r*.025f,r*(.26f-i*.028f)*sx,r*.042f*sy,190,127,0xC0F3FFDC,r*.023f);
        }
        for(int side=-1;side<=1;side+=2) {
            float wave=(float)Math.sin(clock*4.2f+side*.8f);
            float lift=(mood==4 || mood==8)?-.15f:0;
            float reach=side<0 && known?Math.max(0,Math.min(1,punch)):0;
            float tx=bx+side*r*(.65f+.04f*wave),ty=y+r*(.33f+lift+.12f*wave);
            tx+=(bx-r*1.8f-tx)*reach;ty+=(y+r*.55f-ty)*reach;
            AlligatorArt.limb(p,bx+side*r*.38f*sx,by-r*.10f,
                    tx,ty,r*.13f,false,0xFF6FCB98,clock);
        }
        float hx=x+r*(a==null?.04f*bob:a.head.centreX()*.6f),hy=y-r*.30f+r*(a==null?.035f*bob:a.head.centreY()*.6f);
        float hw=r*.79f*(a==null?1:Math.max(.84f,Math.min(1.17f,a.head.radiusX())));
        float hh=r*.46f*(a==null?1:Math.max(.84f,Math.min(1.17f,a.head.radiusY())));
        // Round cheeks and a shorter snout keep the friend baby-like at companion size.
        Painter jelly=new ColorFadePainter(p,0xFF95E7AB,.23f,1);
        AlligatorArt.head(jelly,null,hx,hy,hw,hh,clock,false,0,mood<0?0:mood,look);
        p.fillEllipse(hx-hw*.67f,hy+hh*.36f,hw*.115f,hh*.12f,0x85FFABBF);
        p.arc(hx-hw*.38f,hy-hh*.26f,hw*.32f,hh*.65f,213,80,0xA5F0FFE7,r*.026f);
        if(ninja) {
            float maskY=hy-hh*.43f;
            Trinket.ninjaMask(new MaskFit(p,maskY,hh/hw*1.15f),hx-hw*.10f,maskY,hw*.76f,1);
        }
        if(!known)painter.text("?",x,y+r*.25f,type(r*.55f),fadeBy(INK_DIM,fade),Painter.CENTER,true);
    }
    /** Flatten the real contour; translucent highlights must not reveal interior features. */
    private static final class Silhouette extends OpacityPainter {
        Silhouette(Painter p) {super(p,1);}
        int color(int c) {return (c>>>24)==0?0:0xFF393054;}
        public ArtStamp stamps() {return null;}
    }
    private static final class MaskFit extends OpacityPainter {
        public ArtStamp stamps() {return null;}
        final float cy,sy;
        MaskFit(Painter p,float y,float sy) {super(p,1);cy=y;this.sy=sy;}
        float y(float y) {return cy+(y-cy)*sy;}
        float[] points(float[] a) {float[] b=a.clone();for(int i=1;i<b.length;i+=2)b[i]=y(b[i]);return b;}
        public void fillPoly(float[] a,int c) {p.fillPoly(points(a),c);}
        public void fillContours(float[][] a,int c) {float[][] b=new float[a.length][];for(int i=0;i<a.length;i++)b[i]=points(a[i]);p.fillContours(b,c);}
        public void strokePoly(float[] a,int c,float w) {p.strokePoly(points(a),c,w);}
        public void fillCircle(float x,float y,float r,int c) {p.fillEllipse(x,y(y),r,r*sy,c);}
    }

}
