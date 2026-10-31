package com.dddumpling.game;

import java.util.ArrayList;

/** A run-local packing puzzle. Piece area survives splitting; only settled, contained pieces count. */
final class SquishyFit {
    static final float TARGET=.60f, INITIAL_RADIUS=.285f, MIN_RADIUS=.035f, REPORT=2.4f;
    private static final float[] SQUEEZES={1,1.16f,.86f,1.35f,.74f,1.5f,.67f};
    static final int MAX_PIECES=64, GRID=48;
    static final String[] SHAPES={"OVAL", "CLOVER", "BOW", "STAR"};
    static final int[] COLORS={0xFF8EDDC4,0xFFF0B5CB,0xFFB7AEF2,0xFFFFD88D,0xFF94CAE9};
    static final class Piece {
        float x,y,r,bad,pop,safeX,safeY,reject,returnX,returnY;
        boolean placed,safePlaced,tooBig;
        int level;
        float squeeze=1,drawSqueeze=1,safeSqueeze=1;
        final int color;
        final Softbody body=new Softbody();
        Piece(float x,float y,float r,int color) {
            this.x=x;this.y=y;safeX=x;safeY=y;this.r=r;this.color=color;
            body.reset(x*1000,y*1000,r*1000);body.jiggle=.65f;
        }
        void move(float x,float y) {this.x=x;this.y=y;body.moveTo(x*1000,y*1000);}
        float returnAmount() {float u=Math.max(0,Math.min(1,reject/.4f));return u*u*(3-2*u);}
    }
    final ArrayList<Piece> pieces=new ArrayList<>();
    boolean next,active,ready,finished,won,paid;
    int completed,feedback;
    float timer,duration,report,clock,coverage,burst,burstX,burstY;
    private int first=-1,second=-1;
    private Piece a,b;
    private float downX,downY,travel;
    private float ax,ay,bx,by,offsetAX,offsetAY,offsetBX,offsetBY,startDistance;

    float target() {return TARGET;}
    int shape() {return Math.min(completed,SHAPES.length-1);}
    static float attemptTime(float earned) {
        if(earned>=GameCore.MASH_PERFECT)return 10;
        if(earned>=GameCore.MASH_UNHURT)return 8;
        if(earned>=GameCore.MASH_HURT)return 6;
        return 5;
    }
    static float centerY(Layout L) {return L.playTop+(L.dangerY-L.playTop)*.61f;}
    static float px(Layout L,float x) {return L.w*(.5f+x);}
    static float py(Layout L,float y) {return centerY(L)+L.w*y;}
    static float nx(Layout L,float x) {return x/L.w-.5f;}
    static float ny(Layout L,float y) {return (y-centerY(L))/L.w;}
    float boundary(float angle) {
        if(shape()==1)return .85f+.15f*(float)Math.cos(3*angle-Math.PI/2);
        if(shape()==2)return .73f+.27f*(float)Math.cos(2*angle);
        if(shape()==3)return .78f+.22f*(float)Math.cos(5*angle+Math.PI/2);
        return 1;
    }
    boolean contains(float x,float y) {
        float xx=x/.38f,yy=y/.27f;
        return xx*xx+yy*yy<=sq(boundary((float)Math.atan2(yy,xx)));
    }
    boolean fits(Piece q) {
        if(q.r>=INITIAL_RADIUS-.00001f || !contains(q.x,q.y))return false;
        for(int i=0;i<32;i++) {
            double t=i*Math.PI/16;
            if(!contains(q.x+q.r*q.squeeze*(float)Math.cos(t),q.y+q.r/q.squeeze*(float)Math.sin(t)))return false;
        }
        return true;
    }
    boolean room(Piece q) {
        if(!fits(q))return false;
        for(Piece other:pieces)if(other!=q && other.placed && overlaps(q,other))return false;
        return true;
    }
    private boolean overlaps(Piece q,Piece other) {
        // Separating axes of the two rounded silhouettes; preserve area as they compress.
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/24;float x=(float)Math.cos(a),y=(float)Math.sin(a);
            float extent=q.r*(float)Math.sqrt(sq(x*q.squeeze)+sq(y/q.squeeze))
                    +other.r*(float)Math.sqrt(sq(x*other.squeeze)+sq(y/other.squeeze));
            if(Math.abs((q.x-other.x)*x+(q.y-other.y)*y)>=extent*.98f)return false;
        }
        return true;
    }
    boolean place(Piece q) {q.placed=room(q);return q.placed;}
    private boolean mold(Piece q) {
        float previous=q.squeeze;
        if(room(q))return true;
        for(float scale:SQUEEZES) {
            q.squeeze=scale;if(room(q))return true;
        }
        q.squeeze=previous;return false;
    }
    boolean settle(Piece q) {
        if(mold(q)){q.placed=true;return true;}
        float x=q.x,y=q.y,scale=q.squeeze;
        // Nearby drops find a roomy home without asking for pixel-perfect placement.
        if(contains(x,y))for(int ring=1;ring<=5;ring++)for(int i=0;i<24;i++) {
            double a=i*Math.PI/12;q.x=x+ring*.026f*(float)Math.cos(a);q.y=y+ring*.026f*(float)Math.sin(a);
            if(mold(q)){q.move(q.x,q.y);q.placed=true;return true;}
        }
        q.x=x;q.y=y;q.squeeze=scale;q.placed=false;return false;
    }
    private boolean canFitSize(float radius) {
        Piece probe=new Piece(0,0,radius,0);
        for(float y=-.24f;y<=.24f;y+=.03f)for(float x=-.36f;x<=.36f;x+=.03f) {
            probe.x=x;probe.y=y;for(float scale:SQUEEZES){probe.squeeze=scale;if(fits(probe))return true;}
        }
        return false;
    }
    private void staging(Piece q,Layout L,int side) {
        q.safeX=side*Math.min(.48f-q.r,Math.min(.24f,q.r*1.1f));
        q.safeY=ny(L,L.playTop+L.unit*4+q.r*L.w);
        q.safePlaced=false;
    }
    private void drop(Piece q,Layout L) {
        q.body.letGo();float fromX=q.x,fromY=q.y;
        q.squeeze=q.safeSqueeze;q.move(q.safeX,q.safeY);
        boolean restore=q.safePlaced && place(q);
        if(!restore) {
            q.placed=false;
            if(contains(q.x,q.y)) {staging(q,L,fromX<0?-1:1);q.move(q.safeX,q.safeY);}
        }
        q.reject=.4f;q.returnX=fromX-q.x;q.returnY=fromY-q.y;q.bad=1;q.body.squash(.5f);
    }
    void measure() {
        int total=0,filled=0;
        for(int iy=0;iy<GRID;iy++)for(int ix=0;ix<GRID;ix++) {
            float x=((ix+.5f)/GRID*2-1)*.38f,y=((iy+.5f)/GRID*2-1)*.27f;
            if(!contains(x,y))continue;
            total++;
            for(Piece q:pieces)if(q.placed && sq((x-q.x)/q.squeeze)+sq((y-q.y)*q.squeeze)<=q.r*q.r) {filled++;break;}
        }
        coverage=total==0?0:filled/(float)total;
    }
    void resetRun() {
        stop();next=false;completed=0;pieces.clear();coverage=0;clock=0;
    }
    void begin(GameCore c,Layout L) {
        cancelTouch();active=true;ready=true;finished=won=paid=false;report=0;burst=0;feedback=0;
        duration=timer=attemptTime(c.earnedMash);
        if(pieces.isEmpty()) {
            float y=ny(L,L.playTop+(L.dangerY-L.playTop)*.25f);
            pieces.add(new Piece(0,y,INITIAL_RADIUS,COLORS[completed%COLORS.length]));
        }
        for(Piece q:pieces)q.tooBig=!canFitSize(q.r);
        measure();
    }
    void stop() {cancelTouch();active=false;ready=false;finished=won=paid=false;feedback=0;}
    void cancelTouch() {
        if(a!=null)a.body.letGo();if(b!=null)b.body.letGo();
        a=b=null;first=second=-1;
    }
    int takeFeedback() {int result=feedback;feedback=0;return result;}
    void update(float dt,float elapsed) {
        clock+=dt;burst=Math.max(0,burst-dt*2);
        for(Piece q:pieces) {q.drawSqueeze+=(q.squeeze-q.drawSqueeze)*Math.min(1,dt*14);q.body.update(dt);q.bad=Math.max(0,q.bad-dt);q.reject=Math.max(0,q.reject-dt);q.pop=Math.max(0,q.pop-dt*3);}
        if(finished){report=Math.max(0,report-elapsed);return;}
        if(ready)return;
        timer=Math.max(0,timer-elapsed);
        if(coverage>=target())finish(true);
        else if(timer==0)finish(false);
    }
    private void finish(boolean success) {
        cancelTouch();finished=true;won=success;report=REPORT;feedback=success?2:1;
        if(success)burst=1;
    }
    private Piece hit(float x,float y) {
        Piece best=null;float distance=Float.MAX_VALUE;
        for(Piece q:pieces) {
            if(q.reject>0)continue;
            float d=sq(q.x-x)+sq(q.y-y);
            if(sq((q.x-x)/q.squeeze)+sq((q.y-y)*q.squeeze)<=sq(q.r+.035f) && d<distance) {distance=d;best=q;}
        }
        return best;
    }
    boolean held(Piece q) {return q==a || q==b;}
    private void constrain(Piece q,Layout L,float x,float y) {
        float low=ny(L,L.playTop+L.unit*2)+q.r,high=ny(L,L.dangerY-L.unit*.6f)-q.r;
        q.move(Math.max(-.48f+q.r,Math.min(.48f-q.r,x)),Math.max(low,Math.min(high,y)));
    }
    /** Shared action values match Android MotionEvent and IOSTouch. */
    void touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(!active || finished || c.paused || c.settingsOpen)return;
        x=nx(L,x);y=ny(L,y);
        if(action==3){cancelTouch();measure();return;}
        if(action==0 || action==5) {
            Piece q=hit(x,y);if(q==null)return;
            if(first<0) {first=id;a=q;ax=x;ay=y;downX=x;downY=y;travel=0;offsetAX=q.x-x;offsetAY=q.y-y;}
            else if(second<0 && id!=first) {
                travel=1;second=id;b=q;bx=x;by=y;offsetBX=q.x-x;offsetBY=q.y-y;
                startDistance=(float)Math.sqrt(sq(ax-bx)+sq(ay-by));
            } else return;
            if(q!=a || q!=b) {q.safeX=q.x;q.safeY=q.y;q.safePlaced=q.placed;q.safeSqueeze=q.squeeze;}
            ready=false;q.placed=false;measure();q.pop=1;feedback=1;
            return;
        }
        if(action==2) {
            if(id==first){ax=x;ay=y;travel=Math.max(travel,(float)Math.sqrt(sq(x-downX)+sq(y-downY)));}
            else if(id==second){bx=x;by=y;}
            else return;
            if(a!=null && a==b) {
                float distance=(float)Math.sqrt(sq(ax-bx)+sq(ay-by));
                a.body.encompass(ax*1000,ay*1000,bx*1000,by*1000);
                if(distance>=startDistance+Math.max(.055f,a.r*.55f))split(c,L);
            } else if(id==first && a!=null){constrain(a,L,x+offsetAX,y+offsetAY);mold(a);}
            else if(id==second && b!=null){constrain(b,L,x+offsetBX,y+offsetBY);mold(b);}
            return;
        }
        if(action==1 || action==6) {
            Piece q=id==first?a:id==second?b:null;if(q==null)return;
            q.body.letGo();
            if(id==first)travel=Math.max(travel,(float)Math.sqrt(sq(x-downX)+sq(y-downY)));
            if(id==first && second<0 && travel<.025f && !q.safePlaced) {
                splitTap(c,L,q);return;
            }
            if(id==first){a=null;first=-1;}else{b=null;second=-1;}
            if(a==q || b==q) {if(a==q){offsetAX=q.x-ax;offsetAY=q.y-ay;}else{offsetBX=q.x-bx;offsetBY=q.y-by;}}
            else {
                boolean accepted=settle(q);q.bad=accepted?0:1;q.pop=1;
                if(accepted){q.safeX=q.x;q.safeY=q.y;q.safePlaced=true;q.safeSqueeze=q.squeeze;q.body.squash(.35f);}
                else drop(q,L);
                feedback=accepted?2:1;
                if(c.sound!=null) {if(accepted)c.sound.collect(0);else c.sound.uiBloop();}
                measure();if(coverage>=target())finish(true);
            }
            // Keep the remaining finger as the primary pointer for the next pinch.
            if(first<0 && second>=0){first=second;a=b;ax=bx;ay=by;offsetAX=offsetBX;offsetAY=offsetBY;second=-1;b=null;}
        }
    }
    private void split(GameCore c,Layout L) {
        Piece parent=a;
        if(parent.r*.70710678f<MIN_RADIUS || pieces.size()>=MAX_PIECES) {
            parent.bad=1;return;
        }
        float r=parent.r*.70710678f;
        pieces.remove(parent);
        int color=COLORS[(parent.level+1)%COLORS.length];
        a=new Piece(parent.x,parent.y,r,color);b=new Piece(parent.x,parent.y,r,color);
        a.level=b.level=parent.level+1;
        pieces.add(a);pieces.add(b);
        a.tooBig=b.tooBig=!canFitSize(r);staging(a,L,-1);staging(b,L,1);
        constrain(a,L,ax,ay);constrain(b,L,bx,by);
        a.body.squash(.65f);b.body.squash(.65f);a.pop=b.pop=1;
        offsetAX=offsetAY=offsetBX=offsetBY=0;
        burst=1;burstX=parent.x;burstY=parent.y;feedback=2;
        if(c.sound!=null)c.sound.divideSplit();
        c.companion.react(RunCompanion.WORD,.7f);
    }
    private void splitTap(GameCore c,Layout L,Piece parent) {
        if(parent.r*.70710678f<MIN_RADIUS || pieces.size()>=MAX_PIECES){cancelTouch();return;}
        ax=parent.x-parent.r*.78f;bx=parent.x+parent.r*.78f;ay=by=parent.y;
        split(c,L);
        Piece left=a,right=b;cancelTouch();
        spread(left,L,-1);spread(right,L,1);
    }
    private void spread(Piece q,Layout L,int side) {
        float best=Float.MAX_VALUE,bestX=q.x,bestY=q.y;
        float top=ny(L,L.playTop+L.unit*4)+q.r;
        for(int row=0;row<5;row++)for(int col=0;col<9;col++) {
            float x=(-.48f+q.r)+( .96f-2*q.r)*col/8;
            float y=top+row*.07f;
            if(y+q.r>-.27f)continue;
            float cost=Math.abs(x-side*.23f)*.02f+row*.004f;
            for(Piece other:pieces)if(other!=q && !other.placed) {
                float overlap=Math.max(0,q.r+other.r+.015f-(float)Math.sqrt(sq(x-other.x)+sq(y-other.y)));
                cost+=overlap*overlap*20;
            }
            if(cost<best){best=cost;bestX=x;bestY=y;}
        }
        q.move(bestX,bestY);q.safeX=bestX;q.safeY=bestY;
    }
    static float sq(float x) {return x*x;}
}
