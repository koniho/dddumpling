package com.dddumpling.game;

/** Decorative run companion: event reactions never consume input or gameplay randomness. */
final class RunCompanion extends Draw {
    static final int IDLE=0, WORD=1, POWER_END=2, POWER=3, BOSS=4, DANGER=5, VICTORY=6, DAMAGE=7;
    int who=-1, reaction;
    float clock, left, age, strength;
    final Softbody home=new Softbody(20,118);
    private boolean bossSeen, wonSeen, threatSeen, powered;

    void begin(int selection) { clear();who=selection; }
    void clear() {
        who=-1;reaction=IDLE;clock=left=age=strength=0f;
        bossSeen=wonSeen=threatSeen=powered=false;
        home.reset(0,0,1f,1.25f);
    }
    void react(int event,float amount) {
        if(who<0 || (left>0f && event<reaction)) return;
        // Repeated clears strengthen this beat instead of restarting it indefinitely.
        if(left>0f && event==reaction) { strength=Math.min(1f,strength+.12f);return; }
        reaction=event;age=0f;strength=Math.max(.3f,Math.min(1f,amount));
        left=event==DAMAGE? .85f:event==VICTORY?1.2f:event==WORD?.6f:.8f;
        home.squash((event==DAMAGE?.7f:.4f)*strength);
        home.impulse(event==DAMAGE?-.7f:.7f,-.6f,.18f*strength);
    }
    void update(GameCore c,float dt) {
        boolean play=c.state==GameCore.PLAY;
        if((!play && c.state!=GameCore.BONUS) || c.paused || c.settingsOpen || who<0) return;
        clock+=dt;age+=dt;left=Math.max(0f,left-dt);
        if(left==0f) reaction=IDLE;
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
        if(reaction==DAMAGE) return 3;
        if(reaction==DANGER || reaction==BOSS) return 2;
        if(reaction==VICTORY || reaction==POWER) return 4;
        if(reaction==WORD || reaction==POWER_END || powered) return 1;
        return 0;
    }
    static float x(Layout L) { return (L.keyX[2]+L.keyX[3])*.5f; }
    static float y(Layout L) { return L.keyY[2]-L.keyR*1.55f; }
    static float halfWidth(Layout L) { return L.keyR*.72f; }
    static float halfHeight(Layout L) { return L.keyR*.50f; }
    static float radius(Layout L) { return L.keyR*.55f*.84f; }
    static float radius(GameCore c,Layout L) { return LandPicker.travelerRadius(c,L)*.84f; }
    float beat() { return left>0f?(float)Math.sin(Math.min(1f,age/.48f)*Math.PI)*strength:0f; }
    float squash() { return 1f+beat()*(reaction==DAMAGE?.20f:-.16f); }
    float[] outline(Layout L) {
        float[] ring=home.outline(),out=new float[ring.length];
        // Soft limits keep every wobble inside the reserved gap, even after repeated impacts.
        for(int i=0;i<ring.length;i+=2) {
            out[i]=x(L)+Math.max(-1.12f,Math.min(1.12f,ring[i]/1.25f))*halfWidth(L);
            out[i+1]=y(L)+Math.max(-1.12f,Math.min(1.12f,ring[i+1]))*halfHeight(L);
        }
        return out;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        RunCompanion a=c.companion;
        boolean steamer=c.state==GameCore.BONUS && !c.starBonus && !c.bossReward
                && !CaveInterlude.active(c);
        if((c.state!=GameCore.PLAY && !steamer) || a.who<0 || !c.buddy.out()) return;
        float x=x(L),y=y(L),w=halfWidth(L),h=halfHeight(L),r=radius(c,L);
        float beat=a.beat();
        int tint=a.reaction==DAMAGE?ROSE:a.reaction==VICTORY?GOLD:Collect.BODY[a.who];
        float[] skin=a.outline(L);
        p.fillEllipse(x,y+h*.95f,w*.8f,h*.12f,0x44302050);
        p.fillPoly(skin,Glyph.mix(0xFF352D52,tint,.16f));
        p.strokePoly(skin,Glyph.withAlpha(Glyph.mix(tint,INK,.4f),180),Math.max(1f,L.unit*.065f));
        p.arc(x-w*.25f,y-h*.22f,w*.5f,h*.5f,205f,65f,0x55FFFFFF,L.unit*.07f);
        float hop=(a.reaction==WORD || a.reaction==POWER || a.reaction==VICTORY)?beat:0f;
        float cx=x+w*.10f*(float)Math.sin(a.clock*1.6f);
        float cy=y+r*.10f+r*.055f*(float)Math.sin(a.clock*2.3f)-r*.42f*hop;
        if(a.reaction==DAMAGE) cx+=r*.12f*(float)Math.sin(a.age*55f)*beat;
        p.fillEllipse(x,y+r*.85f,r*.7f,r*.13f,0x55302045);
        Trinket.drawReacting(new Squash(p,cx,cy,a.squash()),a.who,cx,cy,r,a.clock,1f,a.mood(),
                .10f*(float)Math.sin(a.clock*.9f));
        if(a.reaction==POWER || a.reaction==VICTORY) {
            for(int i=0;i<2;i++) {
                float sx=x+(i==0?-1:1)*w*.74f,sy=y-h*.52f;
                p.fillPoly(star(sx,sy,r*(.12f+.13f*beat),r*.06f,4,a.clock),GOLD);
            }
        } else if(a.reaction==DANGER || a.reaction==BOSS) {
            p.line(x+w*.65f,y-h*.65f,x+w*.65f,y-h*.25f,GOLD,r*.1f);
            p.fillCircle(x+w*.65f,y-h*.05f,r*.07f,GOLD);
        }
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
