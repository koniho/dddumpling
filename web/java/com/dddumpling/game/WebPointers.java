package com.dddumpling.game;

/** Browser pointer IDs may be sparse. Up packets retain the departing pointer. */
final class WebPointers {
    private final IOSGame game;
    private final int[] ids=new int[16];
    private final float[] xs=new float[16],ys=new float[16];
    private int count;
    private static final int MOUSE=2147483646,MIRROR=2147483645;
    private boolean mirrored,chooseDirection;
    private int mirrorNode=-1;
    private float pivotX,pivotY,offsetX,offsetY,startX,startY,initialReach;
    void mouse(int action,float x,float y) {
        if(action==0 && count==0) {
            GameCore c=game.core();Boss b=c.boss;
            if(c.state==GameCore.PLAY && !c.paused && !c.settingsOpen && b.fighting() && b.kind==Boss.SPLITTER
                    && y<game.geometry().deckTop && !c.onboarding.wantsTouch(c,game.geometry(),0,x,y)) {
                float nearest=1.44f;
                for(int n=0;n<Boss.DIVIDE_NODES;n++)if(b.nodeActive(n) && b.pieceHits[n]>=Boss.DIVIDE_HITS && b.divideBody[n]!=null) {
                    Softbody body=b.divideBody[n];
                    float dx=(x-body.centreX())/Math.max(1,body.radiusX()),dy=(y-body.centreY())/Math.max(1,body.radiusY());
                    float distance=dx*dx+dy*dy;if(distance<nearest){nearest=distance;mirrorNode=n;}
                }
                if(mirrorNode>=0) {
                    Softbody body=b.divideBody[mirrorNode];pivotX=body.centreX();pivotY=body.centreY();
                    float dx=x-pivotX,dy=y-pivotY,length=(float)Math.hypot(dx,dy);
                    float radius=Math.min(body.radiusX(),body.radiusY());
                    float reach=Math.max(length,radius*.65f);
                    chooseDirection=length<radius*.2f;startX=x;startY=y;initialReach=reach;
                    float ux=length<1?1:dx/length,uy=length<1?0:dy/length;
                    offsetX=pivotX+ux*reach-x;offsetY=pivotY+uy*reach-y;mirrored=true;
                    event(0,MOUSE,x+offsetX,y+offsetY);
                    event(0,MIRROR,2*pivotX-x-offsetX,2*pivotY-y-offsetY);return;
                }
            }
        }
        if(!mirrored){event(action,MOUSE,x,y);return;}
        if(action==1||action==3) {
            event(action,MIRROR,2*pivotX-x-offsetX,2*pivotY-y-offsetY);
            if(action==1)event(1,MOUSE,x+offsetX,y+offsetY);
            mirrored=false;mirrorNode=-1;return;
        }
        if(action==2) {
            // A centre click has no outward direction until the player starts dragging.
            if(chooseDirection) {
                float dx=x-startX,dy=y-startY,distance=(float)Math.hypot(dx,dy);
                if(distance>2){offsetX=pivotX-startX+dx/distance*initialReach;offsetY=pivotY-startY+dy/distance*initialReach;chooseDirection=false;}
            }
            for(int i=0;i<count;i++)if(ids[i]==MIRROR){xs[i]=2*pivotX-x-offsetX;ys[i]=2*pivotY-y-offsetY;}
            event(2,MOUSE,x+offsetX,y+offsetY);
        }
    }
    void drawMirror(Painter p,Layout l) {
        if(!mirrored||game.core().boss.pinchNode!=mirrorNode)return;
        for(int i=0;i<count;i++)if(ids[i]==MIRROR||ids[i]==MOUSE) {
            float r=l.w*.018f;
            p.fillCircle(xs[i],ys[i],r,0x704DDAFF);
            p.strokeCircle(xs[i],ys[i],r*1.5f,0xDDF5F0FF,r*.22f);
        }
    }
    WebPointers(IOSGame game) {this.game=game;}
    void event(int action,int id,float x,float y) {
        int at=-1;for(int i=0;i<count;i++)if(ids[i]==id)at=i;
        if(action==0) {
            if(at>=0 || count==ids.length || id<0)return;
            at=count++;ids[at]=id;
        } else if(at<0)return;
        xs[at]=x;ys[at]=y;
        int kind=action==0?(count==1?0:5):action==1?(count==1?1:6):action;
        game.touch(new IOSTouch(kind,at,java.util.Arrays.copyOf(ids,count),
                java.util.Arrays.copyOf(xs,count),java.util.Arrays.copyOf(ys,count)));
        if(action==1) {
            count--;
            for(int i=at;i<count;i++){ids[i]=ids[i+1];xs[i]=xs[i+1];ys[i]=ys[i+1];}
        } else if(action==3)count=0;
    }
    void cancel() {mirrored=false;mirrorNode=-1;if(count>0)event(3,ids[0],xs[0],ys[0]);game.cancelKeyboard();}
}
