package com.dddumpling.game;

/** Encounter animation follows the same stomp clock as sound and native feedback. */
final class CaveEnemy extends Draw {
    static final float STEP=.26f/Cave.PACE,FLIGHT=.10f,RETREAT=.65f;
    final float[] dustAge=new float[8],dustX=new float[8],dustY=new float[8];
    final float[] boltAge=new float[5],boltX=new float[5],boltY=new float[5];
    final int[] boltColor=new int[5];
    int steps,dustCursor,boltCursor,impacts;
    float recoil,retreat=-1,fromX,fromY;
    CaveEnemy(){reset();}
    void reset(){steps=dustCursor=boltCursor=impacts=0;recoil=0;retreat=-1;for(int i=0;i<8;i++)dustAge[i]=2;for(int i=0;i<5;i++)boltAge[i]=2;}
    boolean visible(Cave v){return v.phase==Cave.SHADOW||v.phase==Cave.FIGHT||retreat>=0 && retreat<RETREAT;}
    void fire(Cave v,Layout L,int color){
        int i=boltCursor++%5;boltAge[i]=0;boltColor[i]=color;
        boltX[i]=v.pathX(v.cameraZ)+(v.playerX()-.5f)*L.w/(Cave.scale(L)*v.zoom());
        boltY[i]=v.pathY(v.z);
    }
    void defeated(Cave v){retreat=0;fromX=v.enemyX;fromY=v.enemyY;}
    void update(GameCore c,float dt){
        Cave v=c.cave;recoil=Math.max(0,recoil-dt);
        for(int i=0;i<8;i++)dustAge[i]+=dt;
        for(int i=0;i<5;i++){
            float before=boltAge[i];
            float flightDt=Math.min(dt,Math.max(0,FLIGHT-before)/c.traversalRate());
            boltAge[i]+=flightDt*c.traversalRate()+(dt-flightDt);
            if(before<FLIGHT && boltAge[i]>=FLIGHT){recoil=.36f;impacts++;v.effects.cue(c,Sfx.BOLT_POP,.38f);}
        }
        if(retreat>=0){
            retreat+=dt;float t=Math.max(0,Math.min(1,(retreat-FLIGHT)/(RETREAT-FLIGHT))),ease=t*t*(3-2*t);
            v.enemyX=fromX+(v.enemyStartX-fromX)*ease;v.enemyY=fromY+(v.enemyStartY-fromY)*ease;
        }
    }
    void stomp(GameCore c){
        Cave v=c.cave;int count=v.timer<Cave.REVEAL?0:1+(int)((v.timer-Cave.REVEAL)/STEP);
        if(count<=steps)return;
        steps=count;int i=dustCursor++%8;dustAge[i]=0;
        dustX[i]=v.enemyX+(steps%2==0?.026f:-.026f);dustY[i]=v.enemyY-.035f;
        v.effects.cue(c,Sfx.LINKED_THUD,.62f);
    }
    float radius(Cave v,Layout L){return L.w*(.043f+.023f*Math.min(1,v.timer/Cave.APPROACH));}
    float lift(Cave v){
        if(retreat>=0)return .28f*Math.abs((float)Math.sin(retreat*30));
        if(v.timer<Cave.REVEAL)return .5f*(float)Math.sin(v.timer/Cave.REVEAL*Math.PI);
        return .30f*Math.abs((float)Math.sin((v.timer-Cave.REVEAL)/STEP*Math.PI));
    }
    void draw(Painter p,GameCore c,Layout L){
        Cave v=c.cave;
        for(int i=0;i<8;i++)if(dustAge[i]<.40f){
            float t=dustAge[i],x=v.screenX(dustX[i],L),y=v.worldScreenY(dustY[i],L);
            for(int j=0;j<5;j++)p.fillEllipse(x+(j-2)*L.w*(.006f+t*.075f),y-L.w*t*.025f,
                    L.w*(.005f+t*.028f),L.w*(.003f+t*.015f),Glyph.withAlpha(CaveArt.LIGHT,(int)(110*(1-t/.4f))));
        }
        if(!visible(v)) {
            if(retreat>=0)CaveArt.stone(p,v.screenX(v.enemyStartX,L),v.worldScreenY(v.enemyStartY,L)+L.w*.03f,L.w*.085f,81,255);
            return;
        }
        float x=v.screenX(v.enemyX,L),ground=v.worldScreenY(v.enemyY,L),r=radius(v,L),y=ground-r*lift(v);
        float bx=v.screenX(v.enemyStartX,L),by=v.worldScreenY(v.enemyStartY,L);
        boolean covered=v.timer<Cave.REVEAL || retreat>RETREAT-.16f;
        if(!covered)CaveArt.stone(p,bx,by+L.w*.03f,L.w*.085f,81,255);
        int color=Glyph.mix(Glyph.COLOR[v.response[0]],CaveArt.MID,.2f);
        float flail=recoil>0||retreat>=0?1:0;
        p.fillEllipse(x,ground+r*.7f,r*1.15f,r*.24f,Glyph.withAlpha(0xFF000000,140));
        for(int side=-1;side<=1;side+=2){
            float foot=(float)Math.sin((retreat>=0?retreat*1.7f:v.timer)/STEP*Math.PI+side*Math.PI*.5f);
            p.fillEllipse(x+side*r*.52f,ground+r*(.70f-.15f*Math.max(0,foot)),r*.30f,r*.18f,color);
            float handY=y+r*(flail>0?-.30f+.65f*(float)Math.sin(v.effects.clock*40+side):.2f+foot*.28f);
            p.line(x+side*r*.6f,y+r*.1f,x+side*r*1.35f,handY,color,r*.19f);
            p.fillCircle(x+side*r*1.35f,handY,r*.16f,color);
        }
        Kawaii.determined(p,Kawaii.BLOB,x,y,r,color,1f+.10f*(1-lift(v)));
        p.fillEllipse(x,y+r*.44f,r*.42f,r*.27f,Glyph.mix(color,CaveArt.LAMP,.35f));
        if(flail>0){
            p.fillEllipse(x,y-r*.04f,r*.72f,r*.44f,color);
            for(int side=-1;side<=1;side+=2){
                p.fillEllipse(x+side*r*.31f,y-r*.13f,r*.21f,r*.26f,0xFFFFFBEB);
                p.fillEllipse(x+side*r*.31f,y-r*.09f,r*.075f,r*.12f,0xFF3A2E4F);
                p.line(x+side*r*.15f,y-r*.48f,x+side*r*.43f,y-r*.52f,0xFF3A2E4F,r*.045f);
            }
            p.fillEllipse(x,y+r*.20f,r*.15f,r*.18f,0xFF3A2E4F);
        }
        if(covered)CaveArt.stone(p,bx,by+L.w*.03f,L.w*.085f,81,255);
    }
    void bolts(Painter p,Cave v,Layout L){
        for(int i=0;i<5;i++){
            float age=boltAge[i];if(age>=FLIGHT+.16f)continue;
            float r=radius(v,L),tx=v.screenX(v.enemyX,L),ty=v.worldScreenY(v.enemyY,L)+r*(.44f-lift(v));
            if(age<FLIGHT){
                float t=age/FLIGHT,sx=v.screenX(boltX[i],L),sy=v.worldScreenY(boltY[i],L);
                float x=sx+(tx-sx)*t,y=sy+(ty-sy)*t,tail=Math.max(0,t-.30f);
                p.line(sx+(tx-sx)*tail,sy+(ty-sy)*tail,x,y,boltColor[i],L.w*.010f);
                p.fillCircle(x,y,L.w*.011f,CaveArt.LAMP);
            }else{
                float t=(age-FLIGHT)/.16f;
                p.strokeCircle(tx,ty,L.w*(.012f+t*.032f),Glyph.withAlpha(CaveArt.LAMP,(int)(255*(1-t))),L.w*.004f);
            }
        }
    }
}
