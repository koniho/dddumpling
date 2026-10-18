package com.dddumpling.game;

/** One-finger chase, yarn defense and cage prompts. Coordinates share the drawn arena. */
final class RainbowCat {
    static final int CHASE=0, WINDUP=1, RECOVER=2, CAGED=3;
    static final int NONE=0, CAT=1, YARN=2;
    static final float WIND=.55f, REST=.85f, ESCAPE=6f;
    final Softbody pastry=new Softbody(18,151), head=new Softbody(14,152);
    int phase, pointer=-1, held, at, count, facing=1, captures;
    final int[] prompt=new int[5];
    float x,y,vx,vy,ballX,ballY,ballVX,ballVY,r,ballR,cageX,cageY,cageR;
    float approachT;
    float left,right,top,bottom,timer,clock,batCooldown,safe,flash,catchEase,pointX,pointY,lastX,lastY;
    boolean placed,kids;
    int cue;
    void reset() {
        phase=CHASE;pointer=-1;held=NONE;placed=false;at=count=captures=cue=0;
        vx=vy=ballVX=ballVY=clock=timer=flash=catchEase=0;batCooldown=1.5f;safe=1.5f;approachT=0;
    }
    void release() {pointer=-1;held=NONE;vx=vy=0;}
    void place(Layout L) {
        r=L.w*.073f;ballR=r*.55f;
        left=L.playLeft+r*1.8f;right=L.playRight-r*1.8f;
        top=L.playTop+L.unit*6.1f+r;bottom=L.dangerY-r*1.5f;
        cageR=r*1.8f;cageX=L.w*.5f;cageY=top+cageR*.7f;
        if(!placed) {
            x=L.w*.33f;y=top+(bottom-top)*.53f;resetBall();
            pastry.reset(x,y,r,1.15f);head.reset(x+r,y+r*.1f,r*.65f);
            pastry.shapeMemory=12;head.shapeMemory=12;placed=true;
        }
    }
    void resetBall() {ballX=(left+right)*.5f;ballY=top+(bottom-top)*.64f;ballVX=ballVY=0;safe=1.4f;}
    void animate(float dt,Layout L) {
        place(L);clock+=dt;flash=Math.max(0,flash-dt*2);catchEase=Math.max(0,catchEase-dt*2);
        pastry.moveTo(x,y);pastry.update(dt);
        head.moveTo(x+facing*r*.96f,y+r*.10f);head.update(dt);
    }
    int update(Boss b,float dt,Layout L,java.util.Random rnd) {
        place(L);approachT=pointer>=0?approachT+dt:0;safe=Math.max(0,safe-dt);batCooldown=Math.max(0,batCooldown-dt);
        if(phase==CAGED) {
            timer-=dt;
            if(timer<=0) {phase=RECOVER;timer=REST;x=cageX;y=cageY+cageR*1.65f;at=count=0;resetBall();cue=3;}
            return 0;
        }
        if(held!=YARN) {
            ballX+=ballVX*dt;ballY+=ballVY*dt;
            float drag=(float)Math.exp(-dt*.55f);ballVX*=drag;ballVY*=drag;
            if(ballX<left-ballR) {ballX=left-ballR;ballVX=Math.abs(ballVX)*.75f;}
            if(ballX>right+ballR) {ballX=right+ballR;ballVX=-Math.abs(ballVX)*.75f;}
            if(ballY<top+ballR) {ballY=top+ballR;ballVY=Math.abs(ballVY)*.65f;}
            if(ballY+ballR>=L.dangerY && safe<=0) {resetBall();phase=RECOVER;timer=REST;release();cue=4;return 1;}
        }
        if(held==CAT)return 0;
        if(phase==WINDUP) {
            timer-=dt;
            if(timer<=0) {
                // The paw must reach the yarn; dragging it away can make a committed bat miss.
                if(held!=YARN && distance(x,y,ballX,ballY)<r*3.2f) {
                    ballVX=((L.w*.5f)-ballX)*.9f;
                    ballVY=(L.dangerY-ballY)/(kids?2.5f:1.75f);safe=0;cue=2;
                }
                phase=RECOVER;timer=REST;batCooldown=1.8f;pastry.squash(.5f);
            }
            return 0;
        }
        if(phase==RECOVER) {timer-=dt;if(timer<=0)phase=CHASE;return 0;}
        float tx=ballX,ty=ballY-r*1.8f;
        // Batting has priority. A reacting cat dodges a nearby approach, then commits and rests.
        if(pointer>=0 && approachT>(kids?.28f:.16f) && held==NONE && distance(pointX,pointY,x,y)<r*3.2f && batCooldown>.25f) {
            tx=x+(x-pointX)*1.7f;ty=y+(y-pointY)*1.7f;
        }
        tx=clamp(tx,left,right);ty=clamp(ty,top+cageR*1.7f,bottom);
        float dx=tx-x,dy=ty-y,dist=Math.max(1,distance(x,y,tx,ty));
        float speed=L.w*(kids?.18f:.27f)*(1+(1-b.health())*.25f),blend=Math.min(1,dt*5f);
        vx+=(dx/dist*speed-vx)*blend;vy+=(dy/dist*speed-vy)*blend;
        if(dist<r*.3f){vx*=.75f;vy*=.75f;}
        x=clamp(x+vx*dt,left,right);y=clamp(y+vy*dt,top+cageR*1.5f,bottom);
        if(Math.abs(vx)>r*.15f)facing=vx<0?-1:1;
        if(held!=YARN && batCooldown<=0 && distance(x,y,ballX,ballY)<r*2.8f) {
            phase=WINDUP;timer=WIND;vx=vy=0;cue=1;head.squash(-.25f);
        }
        return 0;
    }
    boolean touch(GameCore c,Layout L,int action,int id,float px,float py) {
        Boss b=c.boss;
        if(b.kind!=Boss.RAINBOW_CAT || !b.fighting() || c.paused || c.onboarding.briefing) {release();return false;}
        place(L);kids=c.kidsRun;
        if(action==3){boolean owned=pointer>=0;release();return owned;}
        if(action==0 || action==5) {
            if(pointer>=0 || py>=L.deckTop || py<L.playTop || phase==CAGED)return false;
            pointer=id;approachT=0;pointX=lastX=px;pointY=lastY=py;held=NONE;
        } else if(id!=pointer)return false;
        if(pointer<0)return false;
        pointX=px;pointY=py;
        if(held==NONE && phase!=CAGED) {
            // The yarn wins overlapping catches; an established owner never changes objects.
            if(segment(lastX,lastY,px,py,ballX,ballY)<ballR+r*.40f)held=YARN;
            else if(segment(lastX,lastY,px,py,x,y)<r*(kids?1.65f:1.35f)) {
                held=CAT;catchEase=1;pastry.squash(.6f);cue=5;
            }
        }
        if(held==YARN) {
            ballX=clamp(px,left-ballR,right+ballR);ballY=clamp(py,top,L.dangerY-ballR*1.3f);
            ballVX=ballVY=0;safe=Math.max(safe,.25f);
        } else if(held==CAT) {
            x=clamp(px,left,right);y=clamp(py,top,bottom);vx=vy=0;
            if(distance(x,y,cageX,cageY)<cageR*.70f) {
                phase=CAGED;held=NONE;x=cageX;y=cageY;timer=kids?9f:ESCAPE;
                count=3+captures;at=0;
                for(int i=0;i<count;i++)prompt[i]=Roster.random(b.rosterFull,c.rnd);
                ballVX=ballVY=0;cue=6;pastry.squash(.7f);
                // Retain this pointer until lift so the capture gesture cannot tap a key.
            }
        }
        lastX=px;lastY=py;
        if(action==1 || action==6)release();
        return true;
    }
    String companionHint() {
        if(phase==CAGED)return pointer>=0?"LIFT, THEN TAP!":"MATCH THE KEYS!";
        if(held==CAT)return "DRAG CAT TO CAGE!";
        if(held==YARN)return "MOVE THE YARN UP!";
        if(ballVY>0)return "DRAG YARN AWAY!";
        if(phase==WINDUP || phase==RECOVER)return "CATCH HIM NOW!";
        return "CATCH THE CAT!";
    }
    int press(Boss b,int glyph) {
        if(phase!=CAGED)return Boss.REBUFF;
        if(prompt[at]!=glyph) {flash=1;return Boss.REBUFF;}
        b.hitX=x;b.hitY=y;head.squash(.4f);
        if(++at<count)return Boss.PART;
        captures++;phase=RECOVER;timer=1.2f;at=count=0;flash=1;resetBall();
        x=cageX;y=cageY+cageR*1.65f;return b.damage(1);
    }
    static float clamp(float v,float low,float high){return Math.max(low,Math.min(high,v));}
    static float distance(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}
    static float segment(float ax,float ay,float bx,float by,float px,float py) {
        float dx=bx-ax,dy=by-ay,d=dx*dx+dy*dy;
        float t=d<=0?0:clamp(((px-ax)*dx+(py-ay)*dy)/d,0,1);
        return distance(px,py,ax+dx*t,ay+dy*t);
    }
}
