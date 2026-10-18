package com.dddumpling.game;

/** A cosmetic face-off before the normal boss arrival; it never advances combat. */
final class TimeAttackVersus extends Draw {
    static final float CLASH=.66f, READY=3.45f*VersusMusic.TEMPO_SCALE,
            EXIT=3.7f*VersusMusic.TEMPO_SCALE, DURATION=VersusMusic.DURATION;
    static final float SHIMMER=.78f, ANNOUNCE=.24f;
    float time=DURATION,leftShimmer=-1,rightShimmer=-1;
    int who,boss;
    boolean prepared;
    final Boss portrait=new Boss();

    void begin(GameCore c,int opponent) {
        time=0;who=c.runWho;boss=opponent;prepared=false;leftShimmer=rightShimmer=-1;
        portrait.begin(opponent,c.stage,new java.util.Random(196+opponent),c.playRosterFull());
    }
    void clear() {time=DURATION;prepared=false;leftShimmer=rightShimmer=-1;}
    boolean active(GameCore c) {
        return c.state==GameCore.PLAY && c.timeAttack.active && !c.timeAttack.finished
                && !c.boss.beaten && c.boss.kind==boss && c.boss.intro>0 && time<DURATION;
    }
    boolean update(GameCore c,float elapsed,Layout L) {
        if(!active(c)) {
            if(time<DURATION && c.state==GameCore.PLAY && c.timeAttack.active && !c.timeAttack.finished
                    && c.boss.kind==boss && c.boss.intro<=0) {
                time=DURATION;
                if(c.sound!=null) {c.sound.hush();c.sound.bossMusic(true);}
            }
            return false;
        }
        if(c.paused || c.settingsOpen)return true;
        if(!Float.isFinite(elapsed) || elapsed<=0)return true;
        if(!prepared) {
            // Establish the arrival geometry behind the departing panels without aging the fight.
            c.boss.update(0,L,c.rnd);
            portrait.update(0,L,new java.util.Random(196+boss));prepared=true;
        }
        animatePortrait(Math.min(.1f,elapsed),L);
        float before=time;
        time=Math.min(DURATION,time+Math.min(.1f,elapsed));
        if(c.sound!=null) {
            if(before<.06f && time>=.06f)c.sound.ninjaSwish(0);
            if(before<ANNOUNCE && time>=ANNOUNCE) {
                c.sound.ninjaSwish(1);
                c.sound.announceVersus(who,boss,c.preferences.skin);
            }
            if(before<CLASH && time>=CLASH)c.sound.linkedThud();
            if(before<READY && time>=READY)c.sound.courseStart();
            // Cancel a slow or still-warming speech engine before normal boss/tutorial audio.
            if(time>=DURATION) {c.sound.hush();c.sound.bossMusic(true);}
        }
        if(leftShimmer<0 && time>=ANNOUNCE)leftShimmer=time;
        if(rightShimmer<0 && leftShimmer>=0 && time>=leftShimmer+SHIMMER) {
            int offset=c.sound==null?-1:c.sound.versusSpeechOffset();
            if(offset>=Narration.versusOpponentOffset(who)
                    || (offset<0 && time>=Narration.versusOpponentFallback(who)))rightShimmer=time;
        }
        return time<DURATION;
    }

    /** Only the isolated portrait's springs run here; no attacks, timers or gameplay RNG. */
    private void animatePortrait(float dt,Layout L) {
        float before=portrait.age;
        portrait.age+=dt;
        float t=portrait.age,period=boss==Boss.SLIME?.78f:1.08f;
        boolean bounce=before<CLASH && t>=CLASH
                || (int)(Math.max(0,before-CLASH)/period)<(int)(Math.max(0,t-CLASH)/period);
        if(portrait.body!=null && boss!=Boss.SPLITTER && boss!=Boss.ALLIGATOR) {
            if(bounce)portrait.body.squash(boss==Boss.SLIME?.65f:.32f);
            portrait.body.update(dt);
        }
        if(boss==Boss.SPLITTER)for(int i=0;i<portrait.pieceCount();i++) {
            Softbody piece=portrait.pieceBody(i);
            if(bounce)piece.squash(.65f);
            piece.update(dt);
        }
        if(boss==Boss.MUSHROOM) {
            float r=Boss.bodyR(L);
            portrait.mushroomCapDX=r*.38f*(float)Math.sin(t*4.8f);
            portrait.mushroomCapDY=r*.12f*(float)Math.sin(t*9.6f);
            if(portrait.mushroomStem!=null) {
                portrait.mushroomStem.pull(portrait.body.centreX()+portrait.mushroomCapDX,
                        portrait.body.centreY()+portrait.mushroomCapDY,.22f);
                portrait.mushroomStem.update(dt);
            }
        }
        if(boss==Boss.OCTOPUS)portrait.poseOctopusPortrait(dt,L);
    }

    static float arrival(float time,float delay) {
        float t=Math.max(0,Math.min(1,(time-delay)/.52f));
        float back=t-1;
        return 1+2.2f*back*back*back+1.2f*back*back;
    }
    float exit() {return panelTravel((time-EXIT)/(DURATION-EXIT));}

    void draw(Painter p,GameCore c,Layout L) {
        if(!active(c))return;
        float w=L.w,h=L.h,out=exit();
        float impact=Math.max(0,time-CLASH),shake=time>=CLASH?(float)Math.exp(-impact*12)*w*.009f:0;
        int friend=Collect.BODY[who],opponent=Collect.BODY[Collect.bossPrize(boss)];
        p.save();p.clipRect(0,0,w,h);
        if(time<CLASH)p.fillRect(0,0,w,h,0xFF19162B);
        p.translate(shake*(float)Math.sin(impact*65),shake*.5f*(float)Math.cos(impact*53));
        panel(p,c,L,false,friend,-out*w*.72f);
        panel(p,c,L,true,opponent,out*w*.72f);
        float fade=1-Math.min(1,out*2);
        if(fade>0) {
            if(c.timeAttack.challenge==TimeAttack.ALL) {
                String round="ROUND "+(boss+1)+"/"+Boss.COUNT;
                float font=type(w*.056f);
                p.text(round,w*.503f,h*.215f+w*.004f,font,fadeBy(0x99201632,fade),Painter.CENTER,true);
                p.text(round,w*.5f,h*.215f,font,fadeBy(0xFFFFFFFF,fade),Painter.CENTER,true);
            }
            if(time>=CLASH) {
                float pop=(float)Math.exp(-impact*7),size=w*.095f*(1+.55f*pop);
                float cy=h*.435f;
                for(int ring=0;ring<3;ring++) {
                    float radius=w*(.06f+impact*.22f+ring*.025f);
                    p.strokeCircle(w*.5f,cy,radius,fadeBy(ring==1?friend:opponent,pop*.65f*fade),w*.012f*(3-ring));
                }
                for(int i=0;i<10;i++) {
                    float a=i*Softbody.TAU/10,dist=w*(.08f+impact*.26f);
                    p.fillPoly(star(w*.5f+(float)Math.cos(a)*dist,cy+(float)Math.sin(a)*dist,
                            w*.018f*pop,w*.007f*pop,4,a+time),fadeBy(i%2==0?GOLD:INK,fade*pop));
                }
                for(int letter=0;letter<2;letter++) {
                    float phase=impact*19-letter*.9f;
                    float jiggle=(.19f*(float)Math.exp(-impact*3.5f)+.045f)*(float)Math.sin(phase);
                    float x=w*.5f+(letter==0?-.48f:.48f)*size+size*jiggle*.24f;
                    TitleBubbleFont.draw(p,letter==0?'V':'S',x,cy-size*jiggle*.45f,size,GOLD,fade,
                            time+letter*.3f,1+jiggle);
                }
            }
            if(time>=READY) {
                float jump=(float)Math.sin(Math.min(1,(time-READY)/.27f)*Math.PI);
                p.text("LET'S SQUISH!",w*.5f,h*.76f-jump*w*.025f,type(w*.047f),fadeBy(GOLD,fade),Painter.CENTER,true);
            }
        }
        p.restore();
    }

    private void panel(Painter p,GameCore c,Layout L,boolean right,int color,float travel) {
        float w=L.w,h=L.h,side=right?1:-1;
        float[] shape=right?new float[]{w*.47f,0,w,0,w,h,w*.53f,h}
                :new float[]{0,0,w*.47f,0,w*.53f,h,0,h};
        float slide=side*w*.72f*(1-panelTravel(time/ANNOUNCE));
        p.save();p.translate(travel+slide,0);
        p.fillPoly(shape,Glyph.mix(right?0xFF12395C:0xFF402270,color,.20f));
        p.line(w*.47f,0,w*.53f,h,Glyph.mix(color,INK,.4f),w*.012f);
        // Seeded-looking streaks and confetti stay independent of gameplay randomness.
        for(int i=0;i<15;i++) {
            float y=h*((i*.137f+.07f)%1),x=(right?w*.66f:w*.16f)+w*((i*.071f)% .22f);
            float sweep=(time*(.2f+i%3*.04f)+i*.19f)%1;
            float length=w*(.05f+.10f*sweep);
            p.line(x-side*length,y,x,y-h*.018f,fadeBy(color,.22f),w*(.003f+(i%3)*.002f));
        }
        shimmer(p,shape,w,h,right,color);
        // Both panels arrive together; portraits follow once their backgrounds are in place.
        float entry=arrival(time,ANNOUNCE);
        float x=w*(right?.75f:.25f)+side*w*.8f*(1-entry);
        float bounce=entry>1?(entry-1)*w*.16f:0;
        float beat=Math.max(0,time-CLASH),hop=time>=CLASH?w*.02f*(float)Math.sin(beat*11)* (float)Math.exp(-beat*2):0;
        float y=h*.435f-bounce-hop,r=w*(right?.17f:.145f);
        p.fillEllipse(x,y+r*1.1f,r*1.02f,r*.14f,0x5530264C);
        for(int ring=2;ring>=0;ring--)
            p.strokeCircle(x,y,r*(1.15f+ring*.12f),fadeBy(color,.2f+ring*.05f),w*.013f);
        int mood=time>=CLASH && time<CLASH+.3f?5:8;
        if(right) {
            // Launch can enter PLAY and draw before the first versus update lays out the body.
            if(prepared)BossScreen.versusPortrait(p,c,L,portrait,x,y,r,time*2);
        } else Trinket.drawReacting(p,who,x,y,r,time*2,1,mood,.7f);
        name(p,right?Skins.bossName(p.skin(),boss):Collect.NAME[who],x,h*.61f,w,color);
        p.restore();
    }

    private void shimmer(Painter p,float[] panel,float w,float h,boolean right,int color) {
        float start=right?rightShimmer:leftShimmer;
        if(start<0)return;
        float phase=(time-start)/SHIMMER;
        if(phase<=0 || phase>=1)return;
        float slope=.38f,half=w*.16f;
        float first=right?w*.47f:0,last=(right?w:w*.53f)+slope*h;
        float center=first-half+(last-first+half*2)*phase;
        float glow=(float)Math.sin(phase*Math.PI);
        // Nested translucent bands make a soft glint with a narrow bright leading edge.
        for(int layer=0;layer<4;layer++) {
            float width=half*(1-layer*.24f),offset=layer==3?half*.24f:0;
            float[] band=shimmerClip(panel,slope,center+offset-width,false);
            band=shimmerClip(band,slope,center+offset+width,true);
            if(band.length>=6)p.fillPoly(band,fadeBy(Glyph.mix(color,INK,.85f),glow*(layer==3?.22f:.10f)));
        }
    }

    /** Intersect the glint with the slanted panel without requiring a native path clip. */
    private static float[] shimmerClip(float[] points,float slope,float edge,boolean below) {
        if(points.length==0)return points;
        float[] out=new float[points.length+4];int count=0;
        float px=points[points.length-2],py=points[points.length-1],pd=px+slope*py-edge;
        for(int i=0;i<points.length;i+=2) {
            float x=points[i],y=points[i+1],d=x+slope*y-edge;
            boolean inside=below?d<=0:d>=0,previous=below?pd<=0:pd>=0;
            if(inside!=previous) {
                float mix=pd/(pd-d);
                out[count++]=px+(x-px)*mix;out[count++]=py+(y-py)*mix;
            }
            if(inside) {out[count++]=x;out[count++]=y;}
            px=x;py=y;pd=d;
        }
        return java.util.Arrays.copyOf(out,count);
    }

    private static void name(Painter p,String label,float x,float y,float w,int color) {
        String[] words=label.split(" ");
        float font=type(w*.034f),line=font*1.15f;
        for(int i=0;i<words.length;i++) {
            float yy=y+(i+(3-words.length)*.5f)*line;
            p.text(words[i],x+w*.003f,yy+w*.004f,font,0x99201632,Painter.CENTER,true);
            p.text(words[i],x,yy,font,INK,Painter.CENTER,true);
        }
        p.line(x-w*.13f,y+2.75f*line,x+w*.13f,y+2.75f*line,color,w*.009f);
    }
}
