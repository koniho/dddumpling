package com.dddumpling.game;

/** Springy first-play invitation; each letter stays tethered to its choreographed position. */
final class TitleStart extends Draw {
    static final float SQUEEZE=.32f, BURST=.60f, TRANSITION=SQUEEZE+BURST;
    static final int COUNT=11;
    static final float MAX_OFFSET=.8f, MAX_SPEED=4.5f;
    private static final String[] WORDS={"LET'S","SQUISH"};
    final float[] ox=new float[COUNT],oy=new float[COUNT],vx=new float[COUNT],vy=new float[COUNT];
    final float[] squash=new float[COUNT],squashV=new float[COUNT];
    private final float[] ax=new float[COUNT],ay=new float[COUNT],pressure=new float[COUNT];
    private final Pose[] poses=new Pose[COUNT];
    TitleStart() {for(int i=0;i<COUNT;i++)poses[i]=new Pose();}
    private static final class Pose {
        float x,y,size,shape,fade,phase,dx,dy;
        char letter;int color;
    }
    void reset() {
        for(int i=0;i<COUNT;i++)ox[i]=oy[i]=vx[i]=vy[i]=squash[i]=squashV[i]=0;
    }
    static boolean transitioning(GameCore c) {
        return c.state==GameCore.TITLE && c.starter.open && !c.starter.exiting && c.starter.age<TRANSITION;
    }
    private static float ease(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static float clamp(float n,float limit) {return Math.max(-limit,Math.min(limit,n));}
    static boolean visible(GameCore c) {
        return c.modes.entryAvailable(c) && Starter.eligible(c) && !c.starter.open;
    }
    static float y(Layout L) { return L.h*.49f; }
    static boolean hit(GameCore c,Layout L,float x,float y) {
        return visible(c) && Math.abs(x-L.w*.5f)<=L.w*.43f && Math.abs(y-y(L))<=L.w*.16f;
    }
    void update(GameCore c,float elapsed,Layout L) {
        if(!visible(c) && !transitioning(c)) {reset();return;}
        if(!(elapsed>0) || !Float.isFinite(elapsed))return;
        // Small substeps and bounded tethers keep contact forces stable after a hitch.
        float dt=Math.min(.1f,elapsed),unit=L.w*.115f;
        int steps=Math.max(1,(int)Math.ceil(dt*120));float step=dt/steps;
        for(int n=0;n<steps;n++) {
            float back=dt-(n+1)*step,clock=c.clock-back,age=Math.max(0,c.starter.age-back);
            for(int i=0;i<COUNT;i++) {
                pose(poses[i],c,L,i,clock,age);
                ax[i]=-65f*ox[i]-13f*vx[i]+7f*(float)Math.sin(clock*2.1f+i*1.7f);
                ay[i]=-65f*oy[i]-13f*vy[i]+4f*(float)Math.cos(clock*2.4f+i*1.3f);
                pressure[i]=0;
            }
            for(int i=0;i<COUNT;i++)for(int j=i+1;j<COUNT;j++) {
                float dx=(poses[j].x-poses[i].x)/unit+ox[j]-ox[i];
                float dy=(poses[j].y-poses[i].y)/unit+oy[j]-oy[i];
                float reach=(radius(poses[i])+radius(poses[j]))/unit;
                float distance=(float)Math.sqrt(dx*dx+dy*dy);
                if(distance>=reach)continue;
                float overlap=reach-distance;
                if(distance<.001f) {dx=(i+j)%2==0?1f:-1f;dy=0;distance=1;}
                float nx=dx/distance,ny=dy/distance;
                // Equal/opposite soft contact, with no extra bounce energy injected.
                float force=Math.min(90f,overlap*110f);
                ax[i]-=nx*force;ay[i]-=ny*force;ax[j]+=nx*force;ay[j]+=ny*force;
                float shape=(nx*nx-ny*ny)*overlap*.6f;
                pressure[i]+=shape;pressure[j]+=shape;
            }
            for(int i=0;i<COUNT;i++) {
                vx[i]=clamp(vx[i]+ax[i]*step,MAX_SPEED);vy[i]=clamp(vy[i]+ay[i]*step,MAX_SPEED);
                ox[i]+=vx[i]*step;oy[i]+=vy[i]*step;
                if(Math.abs(ox[i])>MAX_OFFSET) {ox[i]=clamp(ox[i],MAX_OFFSET);vx[i]=0;}
                if(Math.abs(oy[i])>MAX_OFFSET) {oy[i]=clamp(oy[i],MAX_OFFSET);vy[i]=0;}
                squashV[i]=clamp(squashV[i]+((clamp(pressure[i],.22f)-squash[i])*90f-14f*squashV[i])*step,3f);
                squash[i]=clamp(squash[i]+squashV[i]*step,.24f);
            }
        }
    }
    private static float radius(Pose pose) {return pose.size*(pose.letter=='\''?.12f:.47f);}
    static void draw(Painter p,GameCore c,Layout L) {
        boolean leaving=transitioning(c);
        if(!visible(c) && !leaving)return;
        float h=L.w*.115f,cy=y(L);
        float burst=leaving?Math.max(0,(c.starter.age-SQUEEZE)/BURST):0;
        if(burst>0) {
            float width=L.w*(.009f-.003f*burst),r=width*7f+L.w*.72f*burst;
            float fade=Math.min(1f,burst/.075f)*(1-burst)*(1-burst);
            for(int side=-1;side<=1;side+=2)
                Renderer.rainbowRing(p,L.w*(.5f+side*.095f),cy,r,width,fade);
        }
        Pose pose=new Pose();TitleStart bodies=c.titleStart;
        for(int i=0;i<COUNT;i++) {
            pose(pose,c,L,i,c.clock,c.starter.age);
            float x=pose.x+bodies.ox[i]*h,y=pose.y+bodies.oy[i]*h;
            if(burst>0)p.line(x-pose.dx*.045f*(1-burst),y-pose.dy*.045f*(1-burst),x,y,
                    fadeBy(pose.color,pose.fade*.35f),pose.size*.08f);
            if(pose.letter=='\'')p.fillEllipse(x,y,pose.size*.055f,pose.size*.11f,fadeBy(pose.color,pose.fade));
            else TitleBubbleFont.draw(p,pose.letter,x,y+pose.size*.4f,pose.size,pose.color,pose.fade,
                    pose.phase,Math.max(.65f,pose.shape+bodies.squash[i]));
        }
        for(int i=0;i<6;i++) {
            float phase=c.clock*1.5f+i*1.7f;
            float x=L.w*.5f+(i%2==0?-1:1)*L.w*(.36f+.025f*(float)Math.sin(phase));
            float y=cy+(i/2-1)*h*.9f+h*.12f*(float)Math.cos(phase);
            float r=L.unit*(.10f+.08f*(.5f+.5f*(float)Math.sin(phase)));
            int color=fadeBy(Glyph.COLOR[i%Glyph.COUNT],.7f*(leaving?1-ease(c.starter.age/SQUEEZE):1));
            p.line(x-r,y,x+r,y,color,L.unit*.05f);
            p.line(x,y-r,x,y+r,color,L.unit*.05f);
        }
    }
    private static void pose(Pose p,GameCore c,Layout L,int index,float clock,float age) {
        int row=index<5?0:1,i=row==0?index:index-5;
        String text=WORDS[row];float h=L.w*.115f,cx=L.w*.5f,cy=y(L);
        boolean leaving=transitioning(c);
        float squeeze=leaving?ease(age/SQUEEZE):0,burst=leaving?Math.max(0,(age-SQUEEZE)/BURST):0;
        if(leaving)clock-=age;
        float width=0,left=0;
        for(int j=0;j<text.length();j++) {
            float advance=h*(text.charAt(j)=='\''?.30f:1.03f);
            width+=advance;if(j<i)left+=advance;
        }
        p.letter=text.charAt(i);p.color=Glyph.COLOR[(i+(row==0?0:3))%Glyph.COUNT];
        p.phase=clock*2.8f-i*.6f;
        float advance=h*(p.letter=='\''?.30f:1.03f);
        p.x=cx-width*.5f+left+advance*.5f;
        p.y=cy+h*(row==0?-.65f:.65f)-h*.10f*Math.max(0,(float)Math.sin(p.phase));
        p.size=h;p.shape=1f+.06f*(float)Math.sin(p.phase);p.fade=1;p.dx=p.dy=0;
        if(leaving) {
            p.x=cx+(p.x-cx)*(1-.86f*squeeze);
            p.y=cy+(p.y-cy)*(1-.80f*squeeze);
            p.size=h*(1-.20f*squeeze);p.shape+=.45f*squeeze;
            if(burst>0) {
                float angle=index*6.283185f/COUNT+.23f,travel=1-(1-burst)*(1-burst);
                float distance=L.h*(.85f+.06f*(index%3));
                p.dx=(float)Math.cos(angle)*distance;p.dy=(float)Math.sin(angle)*distance;
                p.x+=p.dx*travel;p.y+=p.dy*travel+L.h*.10f*burst*burst;
                p.fade=1-ease((burst-.45f)/.55f);
                p.size*=1+.16f*(float)Math.sin(burst*8+index);
                p.shape=1+.25f*(float)Math.sin(burst*10+index);
            }
        }
        if(p.letter=='\'')p.y-=p.size*.32f;
    }
}
