package com.dddumpling.game;

/** One saved rainbow ending drives both the companion skit and its run-summary blurb. */
final class SurvivalEnd extends Draw {
    static final float DURATION=3.2f;
    static final int COUNT=20;
    private static final int[] COLORS={0xFFFF8EA9,0xFFFFBC82,0xFFFFE993,0xFFA6EBAD,
            0xFF8EDDEB,0xFFABA6F3,0xFFE3A4EF};
    private SurvivalEnd() {}
    static boolean ownsCompanion(GameCore c) {return c.survival.active && c.state==GameCore.OVER;}
    private static float unit(float n) {return Math.max(0,Math.min(1,n));}
    private static float ease(float n) {n=unit(n);return n*n*(3-2*n);}
    static float[] pose(GameCore c,Layout L) {
        float t=c.deathProgress(),enter=ease(t/.23f),exit=unit((t-.70f)/.30f);
        float beat=unit((t-.23f)/.47f),a=beat*Softbody.TAU;
        int scene=c.survival.ending;
        float sway=(float)Math.sin(a)*L.w*.055f;
        if(scene==4)sway=L.w*.13f*ease(beat*2);
        if(scene==8 || scene==13 || scene==15)sway=-(float)Math.sin(a*.5f)*L.w*.09f;
        if(scene==19)sway=(float)Math.sin(a*2)*L.w*.075f;
        float x=RunCompanion.x(L)+(L.w*.5f+sway-RunCompanion.x(L))*enter;
        float y=RunCompanion.y(L)+(L.h*.43f-RunCompanion.y(L))*enter
                -L.unit*.45f*(float)Math.sin(a*2)*enter+(L.h*1.1f)*exit*exit;
        float r=RunCompanion.radius(c,L)+(L.w*.105f-RunCompanion.radius(c,L))*enter;
        return new float[]{x,y,r,beat,enter,exit};
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!ownsCompanion(c) || !c.dying() || c.runWho<0)return;
        float[] pose=pose(c,L);
        float x=pose[0],y=pose[1],r=pose[2],beat=pose[3],enter=pose[4];
        float clock=c.deathProgress()*DURATION,a=beat*Softbody.TAU;
        int scene=c.survival.ending;
        if(scene<0 || scene>=COUNT)return;
        p.save();p.clipRect(0,0,L.w,L.h);
        if(enter<1)RunCompanion.drawHomeOnly(p,c,L,1-enter);
        Painter props=new OpacityPainter(p,enter);
        wave(props,x,y+r*.78f,L.w*.72f,r,clock);
        // Props arrive with the companion, then travel away with the same current.
        switch(scene) {
            case 0: // Floaties peel away from both arms.
                for(int side:new int[]{-1,1})ring(props,x+side*r*(1.1f+beat*2),y-r*beat,r*.48f,COLORS[side<0?2:4]);
                break;
            case 2: // A shower of candy sprinkles.
                for(int i=0;i<21;i++) {
                    float sy=y-r*2.5f+((beat*2+i*.137f)%1)*r*3.2f;
                    float sx=x+r*(i%7-3)*.65f;
                    props.line(sx,sy,sx+r*.12f,sy+r*.20f,COLORS[i%7],r*.07f);
                }
                break;
            case 3: props.fillEllipse(x,y+r*.92f,r*1.8f,r*.20f,COLORS[2]);break;
            case 4: // A rainbow curl scoops the little rider along.
                for(int i=6;i>=0;i--)props.arc(x+r*.7f,y-r*.1f,r*(1.3f+i*.09f),r*(1.1f+i*.09f),-80,235,COLORS[i],r*.10f);
                break;
            case 5: // Cloud daycare's cradle and star mobile.
                cloud(props,x,y+r*.9f,r*1.7f);
                props.arc(x,y,r*1.8f,r*1.8f,190,160,COLORS[4],r*.10f);
                for(int i=-1;i<=1;i++) {
                    float sx=x+i*r*.75f,sy=y-r*(1.7f-.18f*(float)Math.sin(a+i));
                    props.line(sx,y-r*2,sx,sy,INK,r*.035f);
                    props.fillPoly(star(sx,sy,r*.2f,r*.09f,5,a*.15f),COLORS[i+2]);
                }
                break;
            case 6: // A rinse shower; no spinning machine.
                for(int i=0;i<7;i++) {
                    float sx=x+(i-3)*r*.35f;
                    props.line(sx,y-r*2.2f,sx,y-r*(1.2f+.25f*(float)Math.sin(a+i)),COLORS[i],r*.075f);
                }
                props.arc(x,y-r*2,r*1.4f,r*.5f,180,180,COLORS[4],r*.15f);break;
            case 7: props.fillEllipse(x,y+r*.7f,r*1.8f,r*.55f,COLORS[5]);break;
            case 8: // The current tugs an unfolded map out of reach.
                map(props,x-r*(1.2f+beat*1.2f),y-r*.6f,r*.8f);break;
            case 9: // Jellybean sailboat.
                props.fillPoly(pill(x,y+r*.8f,r*1.65f,r*.5f,16),COLORS[1]);
                props.fillEllipse(x+r*.6f,y+r*.6f,r*.5f,r*.13f,0x77FFFFFF);
                props.line(x+r*1.1f,y+r*.7f,x+r*1.1f,y-r*1.9f,INK,r*.06f);
                props.fillPoly(new float[]{x+r*1.1f,y-r*1.9f,x+r*1.1f,y-r*.4f,x+r*2,y-r*.6f},COLORS[6]);break;
            case 12: carton(props,x,y,r,false);break;
            case 13: // A floating snack stop slips away.
                ReleaseMascot.steamer(props,x-r*(1.6f+beat*1.4f),y+r*.1f,r*.6f,clock);
                props.fillEllipse(x-r*(1.6f+beat*1.4f),y+r*.65f,r*.85f,r*.14f,COLORS[2]);break;
            case 14: cloud(props,x,y+r,r*1.5f);break;
            case 15: // The forgotten towel flaps behind the current.
                float tx=x+r*(1.5f+beat),ty=y-r*.4f;
                props.fillPoly(new float[]{tx-r*.45f,ty-r*.7f,tx+r*.55f,ty-r*.5f,
                        tx+r*.45f,ty+r*(.65f+.18f*(float)Math.sin(a*3)),tx-r*.55f,ty+r*.5f},COLORS[4]);
                props.line(tx-r*.43f,ty+r*.24f,tx+r*.43f,ty+r*.36f,INK,r*.09f);break;
            case 16: // The river offers a welcoming heart.
                heart(props,x,y-r*1.8f,r*.38f,COLORS[0]);break;
            case 17: cloud(props,x,y+r*.55f,r*1.8f);break;
            case 19: // Fan of technicolor wakes.
                for(int i=0;i<7;i++)props.polyline(new float[]{x,y+r*.7f,x+(i-3)*r*.45f,y+r*1.7f,
                        x+(i-3)*r*.9f,y+r*3},COLORS[i],r*.16f);break;
            default:break;
        }
        int mood=scene==5 || scene==17?2:scene==4 || scene==8 || scene==10?4:1;
        float look=scene==13 || scene==15?-.8f:(float)Math.sin(a)*.6f;
        float bounce=scene==1 || scene==2 || scene==14?Math.abs((float)Math.sin(a*2))*.14f:0;
        Trinket.drawReacting(p,c.runWho,x,y-r*bounce,r*(1+.035f*(float)Math.sin(a*2)),clock,1,mood,look);
        // Two little hands paddle, wave, or hug; no gameplay state is changed.
        for(int side:new int[]{-1,1}) {
            float lift=scene==11 && side==1?(.6f+.45f*(float)Math.sin(a*4)):
                    scene==7 || scene==18?.4f*(float)Math.sin(a*3+side):.15f*(float)Math.sin(a*2+side);
            float hx=x+side*r*1.12f,hy=y+r*(.25f-lift);
            props.line(x+side*r*.78f,y+r*.2f,hx,hy,Collect.BODY[c.runWho],r*.17f);
            props.fillCircle(hx,hy,r*.16f,Collect.BODY[c.runWho]);
        }
        if(scene==1) {
            heart(props,x-r*1.45f,y-r*.8f,r*.24f,COLORS[0]);
            heart(props,x+r*1.5f,y-r*1.1f,r*.20f,COLORS[6]);
        }
        if(scene==3 || scene==18) {
            float[] noodle=new float[26];
            for(int i=0;i<13;i++) {noodle[i*2]=x+r*(i/6f-1);noodle[i*2+1]=y+r*(.4f+.15f*(float)Math.sin(i*.8f+a*3));}
            props.polyline(noodle,scene==18?COLORS[2]:COLORS[1],r*.12f);
            if(scene==18)ring(props,x,y+r*.45f,r*.38f,COLORS[2]);
        }
        if(scene==10) {
            float hat=y-r*(1.05f+.12f*(float)Math.sin(a));
            props.fillPoly(new float[]{x-r*.48f,hat,x+r*.44f,hat,x+r*.2f,hat-r*.4f,
                    x-r*.1f,hat-r*.55f,x-r*.22f,hat-r*.2f},COLORS[2]);
            props.line(x-r*.65f,hat,x+r*.65f,hat+r*.1f,COLORS[2],r*.12f);
        }
        if(scene==12)carton(props,x,y,r,true);
        if(scene==16 || scene==17)cloud(props,x,y+r*.85f,r*1.25f);
        if(scene==6 || scene==10 || scene==14)for(int i=0;i<9;i++) {
            float phase=(beat+i*.113f)%1,sx=x+(float)Math.sin(i*2.4f)*r*1.5f;
            float sy=y+r*(1.3f-phase*3.2f);
            ring(props,sx,sy,r*(scene==10?.07f:.10f+i%3*.055f),COLORS[(i+4)%7]);
        }
        p.restore();
    }
    private static void wave(Painter p,float x,float y,float width,float r,float clock) {
        for(int band=6;band>=0;band--) {
            float[] path=new float[50];
            for(int i=0;i<25;i++) {
                path[i*2]=x-width+width*2*i/24;
                path[i*2+1]=y+band*r*.13f+r*.19f*(float)Math.sin(i*.40f-clock*4);
            }
            p.polyline(path,COLORS[band],r*.16f);
        }
    }
    private static void ring(Painter p,float x,float y,float r,int color) {
        p.arc(x,y,r,r*.78f,0,360,color,r*.22f);
        p.arc(x,y,r*.78f,r*.60f,205,75,0xAAFFFFFF,r*.1f);
    }
    private static void cloud(Painter p,float x,float y,float r) {
        p.fillEllipse(x,y,r,r*.35f,0xFFDEE9FF);
        for(int i=-2;i<=2;i++)p.fillCircle(x+i*r*.34f,y-r*.15f,r*(.28f+.1f*(2-Math.abs(i))),0xFFF1F5FF);
    }
    private static void heart(Painter p,float x,float y,float r,int color) {
        p.fillCircle(x-r*.38f,y-r*.22f,r*.58f,color);p.fillCircle(x+r*.38f,y-r*.22f,r*.58f,color);
        p.fillPoly(new float[]{x-r*.87f,y,x+r*.87f,y,x,y+r},color);
    }
    private static void map(Painter p,float x,float y,float r) {
        p.fillPoly(new float[]{x-r,y-r*.65f,x-r*.3f,y-r*.8f,x+r*.3f,y-r*.5f,x+r,y-r*.7f,
                x+r,y+r*.65f,x+r*.3f,y+r*.8f,x-r*.3f,y+r*.5f,x-r,y+r*.7f},0xFFFFEAC6);
        p.polyline(new float[]{x-r*.7f,y+r*.3f,x-r*.3f,y-r*.2f,x+r*.2f,y+r*.3f,x+r*.6f,y-r*.3f},COLORS[4],r*.12f);
        p.line(x+r*.45f,y-r*.5f,x+r*.75f,y-r*.2f,COLORS[0],r*.09f);
        p.line(x+r*.75f,y-r*.5f,x+r*.45f,y-r*.2f,COLORS[0],r*.09f);
    }
    private static void carton(Painter p,float x,float y,float r,boolean front) {
        if(!front) {
            p.arc(x,y,r*1.35f,r*1.4f,185,170,COLORS[4],r*.08f);return;
        }
        p.fillPoly(new float[]{x-r*1.1f,y+r*.55f,x+r*1.1f,y+r*.55f,x+r*.85f,y+r*1.5f,x-r*.85f,y+r*1.5f},0xFFFFEAC6);
        p.fillPoly(new float[]{x-r*1.1f,y+r*.55f,x-r*1.45f,y+r*.22f,x-r*.6f,y+r*.65f},0xFFFFD7AA);
        p.fillPoly(new float[]{x+r*1.1f,y+r*.55f,x+r*1.45f,y+r*.22f,x+r*.6f,y+r*.65f},0xFFFFD7AA);
        heart(p,x,y+r*1.04f,r*.2f,COLORS[0]);
    }
}
