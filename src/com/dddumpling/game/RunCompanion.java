package com.dddumpling.game;

/** Decorative run companion: event reactions never consume input or gameplay randomness. */
final class RunCompanion extends Draw {
    static final int IDLE=0, WORD=1, POWER_END=2, POWER=3, BOSS=4, DANGER=5, BOSS_HIT=6,
            STAGE_CLEAR=7, VICTORY=8, DAMAGE=9, CRY=10;
    static final float RESCUE_HOLD=.5f,RESCUE_RETURN=.42f,
            RESCUE_TIME=GameCore.PUSH_TIME+RESCUE_HOLD+RESCUE_RETURN;
    private static final int HOME_NODES=20;
    private static final float[] HOME_SHAPE=roundedHex();
    int who=-1, reaction;
    float clock, left, age, strength, rescueT;
    final Softbody home=new Softbody(HOME_NODES,118);
    private boolean bossSeen, wonSeen, threatSeen, powered;

    void begin(int selection) { clear();who=selection; }
    void clear() {
        who=-1;reaction=IDLE;clock=left=age=strength=rescueT=0f;
        bossSeen=wonSeen=threatSeen=powered=false;
        home.reset(0,0,1f,1.25f);
    }
    void react(int event,float amount) {
        if(who<0 || (left>0f && event<reaction)) return;
        if(event==DAMAGE || event==CRY) rescueT=0f;
        // Repeated clears strengthen this beat instead of restarting it indefinitely.
        if(left>0f && event==reaction) { strength=Math.min(1f,strength+.12f);return; }
        reaction=event;age=0f;strength=Math.max(.3f,Math.min(1f,amount));
        left=event==DAMAGE? .85f:event==VICTORY?1.2f:event==STAGE_CLEAR?1.6f
                :event==BOSS_HIT?.95f:event==WORD?.6f:.8f;
        home.squash((event==DAMAGE?.7f:.4f)*strength);
        home.impulse(event==DAMAGE?-.7f:.7f,-.6f,.18f*strength);
    }
    void update(GameCore c,float dt) {
        boolean play=c.state==GameCore.PLAY,over=c.state==GameCore.OVER;
        if((!play && !over && c.state!=GameCore.BONUS) || c.paused || c.settingsOpen || who<0) return;
        clock+=dt;age+=dt;rescueT=Math.max(0f,rescueT-dt);
        if(!over) {
            left=Math.max(0f,left-dt);
            if(left==0f) reaction=IDLE;
        }
        boolean boss=play && c.boss.active(),won=boss && c.boss.beaten;
        boolean threat=play && c.boss.fighting() && (c.boss.boltCount()>0 || c.boss.hasGlob()
                || c.boss.mushroomCharge>0f || c.boss.octoLock);
        if(boss && !bossSeen) react(BOSS,.7f);
        if(threat && !threatSeen) react(DANGER,.8f);
        if(won && !wonSeen) react(VICTORY,1f);
        bossSeen=boss;wonSeen=won;threatSeen=threat;powered=play && c.powerActive();
        home.pull(.15f*(float)Math.sin(clock*1.7f),.8f,.14f);
        home.update(dt);
    }
    int mood() {
        if(reaction==CRY) return 5;
        if(reaction==DAMAGE) return 3;
        if(reaction==DANGER || reaction==BOSS) return 2;
        if(reaction==BOSS_HIT) return 8;
        if(reaction==STAGE_CLEAR) return 9;
        if(reaction==VICTORY || reaction==POWER) return 4;
        if(reaction==WORD || reaction==POWER_END || powered) return 1;
        return 0;
    }
    int displayMood(GameCore c) {
        if(reaction==DAMAGE || reaction==CRY) return mood();
        if(rescuePowered()) return 6;
        if(c.pushUsed && reaction==IDLE) return 7;
        return mood();
    }
    static float x(Layout L) { return (L.keyX[2]+L.keyX[3])*.5f; }
    static float y(Layout L) { return L.keyY[2]-L.keyR*1.55f; }
    static float halfWidth(Layout L) { return L.keyR*.736f; }
    static float halfHeight(Layout L) { return halfWidth(L)*.8660254f; }
    static float radius(Layout L) { return L.keyR*.55f*.84f; }
    static float radius(GameCore c,Layout L) { return LandPicker.travelerRadius(c,L)*.84f; }
    float beat() { return left>0f?(float)Math.sin(Math.min(1f,age/.48f)*Math.PI)*strength:0f; }
    float squash() { return 1f+beat()*(reaction==DAMAGE?.20f:-.16f); }
    float damagePush(Layout L) { return reaction==DAMAGE?halfHeight(L)*1.5f*beat():0f; }
    void rescue() { rescueT=RESCUE_TIME; }
    float rescueAge() { return RESCUE_TIME-rescueT; }
    float rescueLift(Layout L) {
        if(rescueT<=0f) return 0f;
        float age=rescueAge();
        float amount;
        if(age<GameCore.PUSH_TIME) amount=age/GameCore.PUSH_TIME;
        else if(age<GameCore.PUSH_TIME+RESCUE_HOLD) amount=1f;
        else if(age<GameCore.PUSH_TIME+RESCUE_HOLD+RESCUE_RETURN)
            amount=1f-(age-GameCore.PUSH_TIME-RESCUE_HOLD)/RESCUE_RETURN;
        else return 0f;
        amount=Math.max(0f,Math.min(1f,amount));
        amount=amount*amount*(3f-2f*amount);
        return -(L.dangerY-L.playTop)*GameCore.PUSH_LIFT*amount;
    }
    boolean rescuePowered() {
        return rescueT>RESCUE_RETURN;
    }
    float rescueBarFade() {
        if(!rescuePowered()) return 0f;
        float left=rescueT-RESCUE_RETURN;
        return Math.min(1f,left/.18f);
    }
    float[] outline(Layout L) {
        float[] ring=home.outline(),out=new float[ring.length];
        int points=ring.length/2;
        // A rounded six-sided rest path keeps the home distinct from its oval shadow. The live
        // ring only supplies displacement, so the home retains its soft-body wobble and impacts.
        for(int i=0;i<ring.length;i+=2) {
            int point=i/2;
            float angle=Softbody.TAU*point/points;
            float dx=ring[i]/1.25f-(float)Math.cos(angle);
            float dy=ring[i+1]-(float)Math.sin(angle);
            out[i]=x(L)+Math.max(-1.12f,Math.min(1.12f,HOME_SHAPE[i]+dx*.55f))*halfWidth(L);
            out[i+1]=y(L)+Math.max(-1.12f,Math.min(1.12f,HOME_SHAPE[i+1]+dy*.55f))*halfHeight(L);
        }
        return out;
    }
    private static float[] roundedHex() {
        float[] out=new float[HOME_NODES*Softbody.SMOOTH*2];
        for(int point=0;point<out.length/2;point++) {
            float path=point*6f/(out.length/2);
            int side=(int)path,next=(side+1)%6,after=(next+1)%6;
            float t=path-side;
            float ax=(float)Math.cos(side*Softbody.TAU/6f),ay=(float)Math.sin(side*Softbody.TAU/6f);
            float bx=(float)Math.cos(next*Softbody.TAU/6f),by=(float)Math.sin(next*Softbody.TAU/6f);
            float cx=(float)Math.cos(after*Softbody.TAU/6f),cy=(float)Math.sin(after*Softbody.TAU/6f);
            float round=.28f;
            float sx=ax+(bx-ax)*round,sy=ay+(by-ay)*round;
            float ex=bx+(ax-bx)*round,ey=by+(ay-by)*round;
            float ox=bx+(cx-bx)*round,oy=by+(cy-by)*round;
            if(t<.5f) {
                float u=t*2f;
                out[point*2]=sx+(ex-sx)*u;out[point*2+1]=sy+(ey-sy)*u;
            } else {
                float u=(t-.5f)*2f,v=1f-u;
                out[point*2]=v*v*ex+2*v*u*bx+u*u*ox;
                out[point*2+1]=v*v*ey+2*v*u*by+u*u*oy;
            }
        }
        return out;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        RunCompanion a=c.companion;
        boolean steamer=c.state==GameCore.BONUS && !c.starBonus && !c.bossReward
                && !CaveInterlude.active(c);
        boolean over=c.state==GameCore.OVER;
        if((c.state!=GameCore.PLAY && !steamer && !over) || a.who<0 || !c.buddy.out()) return;
        float x=x(L),y=y(L),w=halfWidth(L),h=halfHeight(L),r=radius(c,L);
        float rescueLift=a.rescueLift(L),damagePush=a.damagePush(L);
        p.save();p.translate(0,rescueLift+damagePush);
        float beat=a.beat();
        boolean rescuePowered=a.rescuePowered();
        int body=Collect.BODY[a.who];
        int tint=a.reaction==DAMAGE?Glyph.mix(body,ROSE,.25f+.75f*beat)
                :rescuePowered?GOLD:(a.reaction==VICTORY || a.reaction==BOSS_HIT)?GOLD:body;
        float[] skin=a.outline(L);
        float stageGlow=a.reaction==STAGE_CLEAR
                ?(float)Math.sin(Math.min(1f,a.age/1.25f)*Math.PI)*a.strength:0f;
        if(stageGlow>0f) {
            float[] halo=skin.clone();
            for(int i=0;i<halo.length;i+=2) {
                halo[i]=x+(halo[i]-x)*1.12f;
                halo[i+1]=y+(halo[i+1]-y)*1.12f;
            }
            p.fillPoly(halo,Glyph.withAlpha(GOLD,(int)(105*stageGlow)));
            p.strokePoly(skin,Glyph.withAlpha(GOLD,(int)(105*stageGlow)),Math.max(1f,L.unit*.42f));
            p.strokePoly(skin,Glyph.withAlpha(0xFFFFFFFF,(int)(210*stageGlow)),Math.max(1f,L.unit*.16f));
            for(int k=0;k<4;k++) {
                float a0=k*Softbody.TAU/4f+a.clock*.32f;
                float sx=x+(float)Math.cos(a0)*w*1.18f,sy=y+(float)Math.sin(a0)*h*1.28f;
                p.fillPoly(star(sx,sy,r*(.08f+.06f*stageGlow),r*.035f,4,a0),
                        Glyph.withAlpha(GOLD,(int)(230*stageGlow)));
            }
        }
        float barFade=a.rescueBarFade();
        if(barFade>0f) {
            float u=Math.min(1f,a.rescueAge()/GameCore.PUSH_TIME);
            float spread=w+(L.w*.62f-w)*(u*u*(3f-2f*u));
            float pulse=.78f+.22f*(float)Math.sin(a.clock*24f);
            p.fillPoly(pill(x,y,spread,h*.24f,10),Glyph.withAlpha(GOLD,(int)(42*pulse*barFade)));
            p.fillPoly(pill(x,y,spread,h*.10f,10),
                    Glyph.withAlpha(0xFFFFFFFF,(int)(155*pulse*barFade)));
            for(int k=0;k<12;k++) {
                float angle=k*Softbody.TAU/12f+a.clock*.8f;
                float inner=r*(1.15f+.08f*(float)Math.sin(a.clock*18f+k));
                float outer=inner+r*(.46f+.18f*(k%2));
                p.line(x+(float)Math.cos(angle)*inner,y+(float)Math.sin(angle)*inner,
                        x+(float)Math.cos(angle)*outer,y+(float)Math.sin(angle)*outer,
                        Glyph.withAlpha(GOLD,180),r*.07f);
            }
        }
        p.fillEllipse(x,y+h*.95f,w*.8f,h*.12f,0x44302050);
        p.fillPoly(skin,Glyph.mix(0xFF352D52,tint,.16f));
        p.strokePoly(skin,Glyph.withAlpha(Glyph.mix(tint,INK,.4f),180),Math.max(1f,L.unit*.065f));
        if(a.reaction==DAMAGE && beat>0f)
            p.strokePoly(skin,Glyph.withAlpha(ROSE,(int)(210*beat)),Math.max(1f,L.unit*(.07f+.08f*beat)));
        p.polyline(new float[]{x-w*.42f,y-h*.22f,x-w*.24f,y-h*.52f,x+w*.02f,y-h*.62f},
                0x55FFFFFF,L.unit*.07f);
        float hop=(a.reaction==WORD || a.reaction==POWER || a.reaction==VICTORY
                || a.reaction==BOSS_HIT)?beat:0f;
        float cx=x+w*.10f*(float)Math.sin(a.clock*1.6f);
        float cy=y+r*.10f+r*.055f*(float)Math.sin(a.clock*2.3f)
                -r*(a.reaction==BOSS_HIT?.78f:.42f)*hop;
        if(a.reaction==DAMAGE) cx+=r*.12f*(float)Math.sin(a.age*55f)*beat;
        p.fillEllipse(x,y+r*.85f,r*.7f,r*.13f,0x55302045);
        Trinket.drawReacting(new Squash(p,cx,cy,a.squash()),a.who,cx,cy,r,a.clock,1f,a.displayMood(c),
                .10f*(float)Math.sin(a.clock*.9f),c.flinging());
        if(a.reaction==BOSS_HIT) {
            float poleX=x+w*.60f,poleTop=y-h*(.94f+.18f*beat),poleBottom=y+h*.30f;
            p.line(poleX,poleBottom,poleX,poleTop,Glyph.mix(INK,GOLD,.45f),r*.075f);
            float wave=r*.13f*(float)Math.sin(a.clock*18f);
            p.fillPoly(new float[]{poleX,poleTop,poleX+r*.64f,poleTop+r*.18f+wave,
                    poleX,poleTop+r*.42f},ROSE);
            p.fillPoly(star(poleX+r*.25f,poleTop+r*.20f+wave*.45f,r*.10f,r*.045f,4,0),GOLD);
        }
        if(a.reaction==POWER || a.reaction==VICTORY) {
            for(int i=0;i<2;i++) {
                float sx=x+(i==0?-1:1)*w*.74f,sy=y-h*.52f;
                p.fillPoly(star(sx,sy,r*(.12f+.13f*beat),r*.06f,4,a.clock),GOLD);
            }
        } else if(a.reaction==DANGER || a.reaction==BOSS) {
            p.line(x+w*.65f,y-h*.65f,x+w*.65f,y-h*.25f,GOLD,r*.1f);
            p.fillCircle(x+w*.65f,y-h*.05f,r*.07f,GOLD);
        }
        p.restore();
    }

    /** Scale the existing collectible artwork without changing its body or accessories. */
    private static final class Squash implements Painter {
        final Painter p;final float cx,cy,sx,sy;
        Squash(Painter p,float x,float y,float squash) { this.p=p;cx=x;cy=y;sx=squash;sy=1f/squash; }
        float x(float x) { return cx+(x-cx)*sx; }
        float y(float y) { return cy+(y-cy)*sy; }
        float[] points(float[] a) { float[] b=a.clone();for(int i=0;i<b.length;i+=2){b[i]=x(b[i]);b[i+1]=y(b[i+1]);}return b; }
        public void fillPoly(float[] a,int c){p.fillPoly(points(a),c);}
        public void fillContours(float[][] a,int c){float[][] b=new float[a.length][];for(int i=0;i<a.length;i++)b[i]=points(a[i]);p.fillContours(b,c);}
        public void strokePoly(float[] a,int c,float w){p.strokePoly(points(a),c,w);}
        public void polyline(float[] a,int c,float w){p.polyline(points(a),c,w);}
        public void fillCircle(float x,float y,float r,int c){fillEllipse(x,y,r,r,c);}
        public void strokeCircle(float x,float y,float r,int c,float w){arc(x,y,r,r,0,360,c,w);}
        public void fillEllipse(float x,float y,float rx,float ry,int c){p.fillEllipse(x(x),y(y),rx*sx,ry*sy,c);}
        public void arc(float x,float y,float rx,float ry,float a,float sweep,int c,float w){p.arc(x(x),y(y),rx*sx,ry*sy,a,sweep,c,w);}
        public void fillRect(float l,float t,float r,float b,int c){p.fillRect(x(l),y(t),x(r),y(b),c);}
        public void line(float a,float b,float c,float d,int ink,float w){p.line(x(a),y(b),x(c),y(d),ink,w);}
        public void text(String s,float x,float y,float size,int c,int align,boolean bold){p.text(s,x(x),y(y),size,c,align,bold);}
        public void clipRect(float l,float t,float r,float b){p.clipRect(x(l),y(t),x(r),y(b));}
        public void clipOutCircle(float x,float y,float r){p.clipOutCircle(x(x),y(y),r*Math.max(sx,sy));}
        public void save(){p.save();} public void restore(){p.restore();}
        public void translate(float x,float y){p.translate(x*sx,y*sy);}
    }
}
