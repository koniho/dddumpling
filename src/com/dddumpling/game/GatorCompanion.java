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
        int edge=0xFF347D67,mint=0xFF71D5A0,light=0xFFB7F0BF;
        AccessoryGlitter glitter=known?AccessoryGlitter.begin(p,Collect.ALLIGATOR_FRIEND,x,y,r,clock,1):null;
        Painter surface=p;if(glitter!=null)p=glitter;
        float bx=x+r*(a==null?bob*.025f:a.body.centreX()*.45f),by=y+r*.39f;
        float sx=a==null?1:a.body.radiusX(),sy=a==null?1:a.body.radiusY();
        float tailBounce=a==null?wag*.10f:a.tail.centreY()+(.95f-a.tail.radiusY())*.2f;
        float tailY=y+r*(.44f+tailBounce);
        float[] tailShape={-.7f,.1f,-.2f,.60f,.5f,.51f,.96f,.12f,1.05f,-.60f,.76f,-.31f,.31f,-.05f,-.22f,-.28f};
        p.fillPoly(AlligatorArt.form(x+r*.50f,tailY,r*.43f,r*.30f,tailShape),edge);
        p.fillPoly(AlligatorArt.form(x+r*.50f,tailY-r*.015f,r*.40f,r*.26f,tailShape),mint);
        p.arc(x+r*.61f,tailY-r*.025f,r*.25f,r*.14f,12,130,0xA3D8FFCD,r*.04f);
        for(int i=0;i<3;i++) {
            float tx=x+r*(.45f+i*.15f),ty=tailY-r*(.12f+i*.08f);
            p.fillPoly(AlligatorArt.form(tx,ty,r*(.075f-i*.01f),r*.085f,
                    -1,.4f,-.8f,-.2f,0,-1,.8f,-.2f,1,.4f),0xFFB5E9A0);
        }
        // Bean feet and a single jelly belly replace the little boss's armored plates.
        for(int side=-1;side<=1;side+=2) {
            float fx=bx+side*r*.28f*sx,fy=y+r*(.79f+.025f*bob*side);
            p.fillEllipse(fx,fy,r*.19f,r*.13f,edge);
            p.fillEllipse(fx-r*.014f,fy-r*.024f,r*.17f,r*.11f,mint);
            p.arc(fx,fy-r*.015f,r*.12f,r*.065f,205,115,0xC4E7FFD8,r*.028f);
        }
        if(a==null) {
            p.fillEllipse(bx,by,r*.47f,r*.45f,edge);
            p.fillEllipse(bx-r*.012f,by-r*.018f,r*.44f,r*.42f,mint);
            p.fillEllipse(bx-r*.07f,by-r*.09f,r*.36f,r*.33f,0x809DEBAA);
        } else {
            p.fillPoly(skin(a.body,x,y+r*.39f,r*.47f,r*.45f),edge);
            p.fillPoly(skin(a.body,x-r*.012f,y+r*.37f,r*.44f,r*.42f),mint);
            p.fillPoly(skin(a.body,x-r*.07f,y+r*.30f,r*.36f,r*.33f),0x809DEBAA);
        }
        p.fillEllipse(bx,by+r*.065f,r*.31f*sx,r*.31f*sy,0xFFB8DB94);
        p.fillEllipse(bx-r*.015f,by+r*.035f,r*.28f*sx,r*.29f*sy,0xFFF0F5BD);
        for(int i=0;i<2;i++)p.arc(bx,by+r*(.02f+i*.14f),r*.24f*sx,r*.075f*sy,
                30,120,0x639FBF7C,r*.024f);
        p.arc(bx-r*.20f,by-r*.02f,r*.17f*sx,r*.29f*sy,142,82,0xB5ECFFE1,r*.04f);
        float hx=x+r*(a==null?.04f*bob:a.head.centreX()*.6f);
        float hy=y-r*.19f+r*(a==null?.035f*bob:a.head.centreY()*.6f);
        float hw=r*.80f*(a==null?1:Math.max(.84f,Math.min(1.17f,a.head.radiusX())));
        float hh=r*.63f*(a==null?1:Math.max(.84f,Math.min(1.17f,a.head.radiusY())));
        // One rounded head contour incorporates the eye bumps and short, broad snout.
        float[] outline={-.96f,.03f,-.86f,-.50f,-.63f,-.83f,-.37f,-.87f,-.12f,-.67f,
                .15f,-.68f,.39f,-.86f,.66f,-.80f,.87f,-.48f,.98f,.05f,
                .88f,.49f,.53f,.70f,0,.77f,-.55f,.68f,-.88f,.45f};
        for(int i=0;i<3;i++) {
            float tx=hx+hw*(i-1)*.18f,ty=hy-hh*(.70f+(i==1?.06f:0));
            p.fillPoly(AlligatorArt.form(tx,ty,hw*.10f,hh*.18f,
                    -1,.50f,-.85f,-.12f,0,-1,.85f,-.12f,1,.50f),edge);
            p.fillPoly(AlligatorArt.form(tx,ty-hh*.02f,hw*.065f,hh*.13f,
                    -1,.50f,-.85f,-.12f,0,-1,.85f,-.12f,1,.50f),0xFFD8EFA5);
        }
        p.fillPoly(AlligatorArt.form(hx,hy,hw,hh,outline),edge);
        p.fillPoly(AlligatorArt.form(hx,hy-hh*.018f,hw*.965f,hh*.95f,outline),mint);
        p.fillPoly(AlligatorArt.form(hx-hw*.055f,hy-hh*.10f,hw*.88f,hh*.80f,outline),0x8AC0F5BD);
        p.fillEllipse(hx,hy+hh*.27f,hw*.77f,hh*.42f,0xA34CB68A);
        p.fillEllipse(hx-hw*.018f,hy+hh*.22f,hw*.74f,hh*.37f,0xFFAFF0B4);
        p.arc(hx-hw*.05f,hy+hh*.20f,hw*.59f,hh*.27f,190,104,0xADDEFFCF,r*.025f);
        p.arc(hx-hw*.39f,hy-hh*.37f,hw*.27f,hh*.30f,205,104,0xD9EFFFF0,r*.045f);
        p.fillEllipse(hx-hw*.62f,hy-hh*.31f,hw*.065f,hh*.09f,0xE6F4FFF0);
        for(int side=-1;side<=1;side+=2) {
            p.fillEllipse(hx+side*hw*.20f,hy+hh*.13f,hw*.033f,hh*.04f,0xFF4D9677);
            p.fillEllipse(hx+side*hw*.66f,hy+hh*.27f,hw*.13f,hh*.10f,0xA8FFA3BB);
        }
        if(glitter!=null)p=surface;
        float faceR=hw*1.03f,faceY=hy-hh*.08f;
        Trinket.reactionFace(p,hx,faceY,faceR,clock,1,mood<0?0:mood,look*.25f);
        if(mood<=1 || mood==4 || mood==8 || mood==9) {
            float tx=hx+hw*.095f,ty=faceY+faceR*.35f;
            p.fillPoly(AlligatorArt.form(tx,ty,r*.045f,r*.060f,
                    -1,-.42f,1,-.42f,.80f,.45f,0,1,-.80f,.45f),0xFFFFFDE8);
        }
        if(known)AccessoryArt.outfit(p,Collect.ALLIGATOR_FRIEND,hx,faceY,faceR,hy-hh*.82f,
                bx,y+r*.79f,r,clock,1,ninja,.85f*sx);
        if(ninja)Trinket.ninjaMask(new MaskFit(p,faceY,.64f),hx,faceY,hw*.92f,1);
        // Tiny mitten paws wave in front of the cheeks; title punches retain their reach.
        for(int side=-1;side<=1;side+=2) {
            float wave=(float)Math.sin(clock*4.2f+side*.8f);
            float lift=(mood==4 || mood==8)?-.16f:0;
            float reach=side<0 && known?Math.max(0,Math.min(1,punch)):0;
            float tx=bx+side*r*(.53f+.04f*wave),ty=y+r*(.36f+lift+.08f*wave);
            tx+=(bx-r*1.8f-tx)*reach;ty+=(y+r*.55f-ty)*reach;
            p.line(bx+side*r*.34f,by-r*.06f,tx,ty,edge,r*.21f);
            p.line(bx+side*r*.34f,by-r*.08f,tx,ty-r*.015f,mint,r*.17f);
            p.fillEllipse(tx,ty,r*.16f,r*.19f,edge);
            p.fillEllipse(tx-r*.01f,ty-r*.02f,r*.14f,r*.16f,mint);
            p.fillEllipse(tx-r*.04f,ty-r*.07f,r*.06f,r*.07f,0xB9D4FFD3);
            for(int toe=-1;toe<=1;toe++)p.fillCircle(tx+toe*r*.065f,ty+r*.06f,r*.018f,0xFFECF8CB);
        }
        if(glitter!=null)glitter.finish();
        if(known)AccessoryArt.flair(p,Collect.ALLIGATOR_FRIEND,bx,hy-hh*.82f,y+r*.79f,r,clock,1);
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
