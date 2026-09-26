package com.dddumpling.game;

/** Saved runs in the release-notes glass, using the Settings gesture and back routes. */
final class HighScoreScreen extends Draw {
    static final int CLOSE=-1, BACK=-2, ROW=2000;
    static final float ENTRY_TIME=PANEL_SLIDE_TIME;
    boolean open,closing;
    float entrance;
    int selected=-1;
    static float size(Layout L) { return Math.min(ReleaseNotes.size(L),(L.dangerY-L.topSafe)/27f); }
    static float top(Layout L) { return L.topSafe+size(L); }
    static float bottom(Layout L) { return L.h-L.padB-size(L); }
    static float listTop(Layout L) { return top(L)+size(L)*3.5f; }
    static float listBottom(Layout L) { return bottom(L)-size(L); }
    static float rowHeight(GameCore c,Layout L) {
        return Math.min(size(L)*3.6f,(listBottom(L)-listTop(L))/Math.max(1,c.highScores.displayCount()));
    }
    static boolean entryHit(GameCore c,Layout L,float x,float y) {
        return ReleaseNotes.available(c) && !c.releaseNotes.open
                && Math.abs(x-L.w*.5f)<L.w*.30f && Math.abs(y-L.h*.292f)<L.unit*1.1f;
    }
    void show(GameCore c) {
        if(!ReleaseNotes.available(c) || c.releaseNotes.open) return;
        Pause.release(c);c.highScores.unread=false;open=true;selected=-1;entrance=0f;closing=false;feedback(c);
    }
    void update(float dt) {
        if(!open) return;
        entrance=Math.max(0f,Math.min(1f,entrance+(closing?-dt:dt)/ENTRY_TIME));
        if(closing && entrance==0f) { open=false;closing=false; }
    }
    boolean moving() { return closing || entrance<1f; }
    float offsetY(Layout L) { return -bottom(L)*(1f-panelTravel(entrance)); }
    static float pulse(float time) { return .5f-.5f*(float)Math.cos(time*3.5f); }
    static boolean titleAttention(GameCore c) {
        return c.highScores.unread && !c.settingsOpen && !c.releaseNotes.open && !c.highScoreScreen.open && !c.storyOpen();
    }
    static float titleTextScale(GameCore c) { return titleAttention(c)?1f+.1f*pulse(c.time):1f; }
    static int titleTextColor(GameCore c) { return titleAttention(c)?Glyph.mix(ROSE,GOLD,pulse(c.time)*.8f):ROSE; }
    static void titleGlow(Painter p,GameCore c,Layout L,float fade) {
        if(!titleAttention(c)) return;
        float s=L.unit,font=type(s*.74f)*titleTextScale(c),y=L.h*.292f-font*.36f;
        float pulse=pulse(c.time);
        float width=Math.min(L.w*.42f,("BEST "+c.best).length()*font*.36f+s*.3f);
        for(int i=6;i>0;i--) {
            float spread=s*i*(.09f+.04f*pulse);
            p.fillEllipse(L.w*.5f,y,width+spread,font*.55f+spread,
                    fadeBy(GOLD,fade*(7-i)*.012f*(.25f+.75f*pulse)));
        }
    }
    private void feedback(GameCore c) { if(c.sound!=null) c.sound.uiBloop(); }
    private void close(GameCore c) {
        if(!open || closing) return;
        closing=true;feedback(c);
    }
    void back(GameCore c) {
        if(closing) return;
        if(selected>=0) { selected=-1;feedback(c); } else close(c);
    }
    int hit(GameCore c,Layout L,float x,float y) {
        if(moving()) return 0;
        float s=size(L),t=top(L);
        if(x<L.w*.04f || x>L.w*.96f || y<t || y>bottom(L)) return CLOSE;
        if(x>L.w*.82f && y<t+2.5f*s) return CLOSE;
        if(selected>=0) return x<L.w*.21f && y<t+2.5f*s?BACK:0;
        if(y<listTop(L) || y>listBottom(L)) return 0;
        int row=(int)((y-listTop(L))/rowHeight(c,L));
        return row<c.highScores.displayCount()?ROW+row:0;
    }
    void action(GameCore c,int hit) {
        if(moving()) return;
        if(hit==CLOSE) close(c);
        else if(hit==BACK) back(c);
        else if(hit>=ROW && hit<ROW+c.highScores.displayCount()) { selected=hit-ROW;feedback(c); }
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!open) return;
        float s=size(L),t=top(L),b=bottom(L);
        p.save();p.clipRect(0,0,L.w,L.h);p.translate(0,offsetY(L));
        p=new OpacityPainter(p,PANEL_OPACITY);
        glassPanel(p,L.w*.04f,t,L.w*.96f,b,s);
        float x=L.w*.89f,y=t+s*1.25f,r=s*.3f;
        p.line(x-r,y-r,x+r,y+r,INK,s*.12f);
        p.line(x-r,y+r,x+r,y-r,INK,s*.12f);
        if(selected>=0) {
            x=L.w*.13f;
            p.polyline(new float[]{x+r,y-r,x-r,y,x+r,y+r},INK,s*.12f);
        }
        p.text(selected<0?"HIGH SCORES":"RUN SUMMARY",L.w*.5f,t+s*1.7f,type(s*.8f),GOLD,Painter.CENTER,true);
        p.save();p.clipRect(L.w*.07f,listTop(L),L.w*.93f,listBottom(L));
        if(selected>=0 && selected<c.highScores.displayCount()) summary(p,c,L,c.highScores.displayRun(selected));
        else if(c.highScores.displayCount()==0) {
            Kawaii.moodDumpling(p,L.w*.5f,t+8f*s,2f*s,Glyph.COLOR[0],.7f,1f);
            p.text("Your next run starts the list.",L.w*.5f,t+12f*s,type(s*.52f),INK_DIM,Painter.CENTER,false);
        } else {
            for(int i=0;i<c.highScores.displayCount();i++) {
                float ry=listTop(L)+i*rowHeight(c,L);
                if(ry+rowHeight(c,L)<listTop(L) || ry>listBottom(L)) continue;
                row(p,c,L,c.highScores.displayRun(i),ry);
            }
        }
        p.restore();
        p.restore();
    }
    private void bosses(Painter p,GameCore c,HighScores.Run run,float cx,float y,float r,float width) {
        int count=Integer.bitCount(run.bosses);
        float step=Math.min(r*1.35f,(width-2f*r)/Math.max(1,count-1));
        int slot=0;
        // Later bosses are painted last, in front of the earlier portraits.
        for(int boss=0;boss<Boss.COUNT;boss++) {
            if((run.bosses&(1<<boss))==0) continue;
            BossCollect.draw(p,boss,cx+(slot++-(count-1)*.5f)*step,y,r,c.clock,true,1f);
        }
    }
    private void row(Painter p,GameCore c,Layout L,HighScores.Run run,float y) {
        float height=rowHeight(c,L),s=Math.min(size(L),height/3.2f),cy=y+height*.5f;
        String value=String.valueOf(run.score);
        float font=Math.min(type(s*.52f),L.w*.13f/(value.length()*.73f));
        float baseline=cy+font*.36f;
        if(run.id==c.highScores.latest) {
            p.fillRect(L.w*.08f,y,L.w*.92f,y+height,Glyph.withAlpha(GOLD,(int)(10+20*pulse(c.clock))));
            float half=value.length()*font*.36f;
            for(int i=4;i>0;i--) p.fillEllipse(L.w*.21f+half,cy,half+i*s*(.1f+.07f*pulse(c.clock)),font*.6f+i*s*(.08f+.06f*pulse(c.clock)),
                    Glyph.withAlpha(GOLD,(int)((13-i*2)*(.4f+2f*pulse(c.clock)))));
        }
        Trinket.draw(p,Math.max(0,run.character),L.w*.135f,cy,Math.min(s*.9f,L.w*.037f),c.clock,run.character>=0,1f);
        p.text(value,L.w*.21f,baseline,font,INK,Painter.LEFT,true);
        java.util.ArrayList<String> lines=new java.util.ArrayList<>();
        int limit=Math.max(1,(int)(L.w*.27f/(font*.73f)));
        String line="";
        for(String word:run.blurb().split(" ")) {
            if(!line.isEmpty() && line.length()+word.length()+1>limit) { lines.add(line);line=""; }
            line+=line.isEmpty()?word:" "+word;
        }
        lines.add(line);
        float leading=font*1.25f;
        for(int i=0;i<lines.size();i++) p.text(lines.get(i),L.w*.36f,
                baseline+(i-(lines.size()-1)*.5f)*leading,font,INK_DIM,Painter.LEFT,false);
        bosses(p,c,run,L.w*.71f,cy,Math.min(s*.9f,L.w*.032f),L.w*.14f);
        haul(p,run.dumplings,L.w*.865f,cy,Math.min(s*1.05f,L.w*.047f),c.clock);
        p.line(L.w*.10f,y+height-s*.1f,L.w*.90f,y+height-s*.1f,0x25FFFFFF,s*.05f);
    }
    private static void haul(Painter p,int count,float x,float y,float r,float clock) {
        ReleaseMascot.steamer(p,x,y,r,clock);
        // A bamboo label covers the mascot face and keeps long counts inside the basket.
        p.fillEllipse(x,y+r*.18f,r*.79f,r*.35f,0xFFF3D4A2);
        String value=String.valueOf(count);
        float font=Math.min(type(r*.48f),r*1.45f/(value.length()*.73f));
        p.text(value,x,y+r*.18f+font*.36f,font,0xFF3A2E4F,Painter.CENTER,true);
    }
    private static String value(int n) { return String.valueOf(n); }
    private static void stat(Painter p,float titleX,float valueX,float y,float font,String title,String value) {
        p.text(title,titleX,y,font,Glyph.mix(INK_DIM,INK,.18f),Painter.LEFT,false);
        p.text(value,valueX,y,font,INK,Painter.LEFT,true);
    }
    private static void companion(Painter p,GameCore c,HighScores.Run run,float x,float y,float r) {
        if(run.character<0 || run.character>=Collect.COUNT) return;
        float breath=1f+.045f*(float)Math.sin(c.clock*2.3f);
        float bob=r*.12f*(float)Math.sin(c.clock*1.8f);
        float look=.14f*(float)Math.sin(c.clock*.9f);
        p.fillEllipse(x,y+r*.88f,r*.72f,r*.12f,0x55302045);
        Trinket.drawReacting(p,run.character,x,y+bob,r*breath,c.clock,1f,0,look);
    }
    private static void bossCollection(Painter p,GameCore c,int mask,float left,float y,float r,
            float font) {
        if(mask==0) {
            p.text("0",left,y,font,INK,Painter.LEFT,true);
            return;
        }
        int slot=0;
        for(int boss=0;boss<Boss.COUNT;boss++) if((mask&(1<<boss))!=0)
            BossCollect.draw(p,boss,left+r+slot++*r*1.5f,y,r,c.clock,true,1f);
    }
    /** Exact won characters, including repeats, overlap by 25% and wrap once. */
    private static void prizeCollection(Painter p,GameCore c,HighScores.Run run,float left,
            float right,float y,float r,float font) {
        if(run.prizes.length==0) {
            p.text(value(run.dumplings),left,y,font,INK,Painter.LEFT,true);
            return;
        }
        float step=r*1.5f;
        int perRow=Math.max(1,(int)((right-left-2f*r)/step)+1);
        int capacity=Math.min(perRow*2,12),shown=Math.min(run.prizes.length,capacity);
        boolean overflow=run.prizes.length>capacity;
        if(overflow) shown=Math.min(run.prizes.length,perRow);
        for(int i=0;i<shown;i++) {
            int row=i/perRow,slot=i%perRow;
            float x=left+r+slot*step;
            float yy=y+(row-.5f*(shown>perRow?1f:0f))*r*1.25f;
            Trinket.draw(p,run.prizes[i],x,yy,r,c.clock,true,1f);
        }
        if(overflow) {
            String more="+"+(run.prizes.length-shown);
            p.text(more,left,y+r*1.35f,font,INK,Painter.LEFT,true);
        }
    }
    private static void effect(Painter p,float iconX,float titleX,float valueX,float y,float s,
            float font,float clock,int effect,int count) {
        Renderer.summaryPowerIcon(p,effect,iconX,y-s*.18f,s*.54f,clock);
        stat(p,titleX,valueX,y,font,Power.NAMES[effect],value(count));
    }
    private static void stages(Painter p,GameCore c,Layout L,HighScores.Run run,float y,float s,
            float font) {
        float[] x={L.w*.29f,L.w*.71f};
        int[] land={run.land,Lands.forStage(run.stage)},stage={run.land*Boss.EVERY+1,run.stage};
        String[] label={"START STAGE ","STAGE REACHED "};
        for(int i=0;i<2;i++) {
            Lands.logo(p,land[i],x[i],y,s*.78f,255,c.clock);
            p.text(label[i]+stage[i],x[i],y+s*1.48f,font,INK,Painter.CENTER,true);
        }
    }
    private void summary(Painter p,GameCore c,Layout L,HighScores.Run run) {
        float s=Math.min(size(L)*1.15f,(listBottom(L)-listTop(L))/30f),t=listTop(L);
        float iconX=L.w*.14f,titleX=L.w*.26f,valueX=L.w*.72f;
        float font=type(s*.55f),y=t+s*1.45f;
        p.fillRect(L.w*.065f,t-s*.25f,L.w*.935f,listBottom(L),0xE02A2542);

        companion(p,c,run,iconX,y-s*.20f,s*1.02f);
        p.text(run.character>=0&&run.character<Collect.COUNT?Collect.NAME[run.character]:"COMPANION SQUISHY",
                titleX,y,font,INK,Painter.LEFT,true);
        y+=s*2.20f;
        p.line(titleX,y-s*.60f,L.w*.89f,y-s*.60f,0x35FFFFFF,s*.055f);

        stat(p,titleX,valueX,y,font,"SCORE",value(run.score)); y+=s*1.55f;
        stages(p,c,L,run,y+s*.35f,s,font); y+=s*3.25f;
        p.line(titleX,y-s*.52f,L.w*.89f,y-s*.52f,0x35FFFFFF,s*.055f);

        stat(p,titleX,valueX,y,font,"ACCURACY",run.accuracy()+"%"); y+=s*1.42f;
        stat(p,titleX,valueX,y,font,"SQUISHES",value(run.squishes)); y+=s*1.42f;
        stat(p,titleX,valueX,y,font,"BEST COMBO",value(run.combo)); y+=s*1.42f;
        stat(p,titleX,valueX,y,font,"STAGES COMPLETED",value(run.stages)); y+=s*1.80f;

        p.text("BOSSES BEATEN",titleX,y,font,Glyph.mix(INK_DIM,INK,.18f),Painter.LEFT,false);
        bossCollection(p,c,run.bosses,valueX,y-s*.16f,s*.47f,font);
        y+=s*2.00f;
        p.text("DUMPLINGS COLLECTED",titleX,y,font,Glyph.mix(INK_DIM,INK,.18f),Painter.LEFT,false);
        prizeCollection(p,c,run,valueX,L.w*.89f,y-s*.16f,s*.47f,font);
        y+=s*2.45f;
        p.line(titleX,y-s*.68f,L.w*.89f,y-s*.68f,0x35FFFFFF,s*.055f);

        for(int effect:Power.OFFERED) {
            effect(p,iconX,titleX,valueX,y,s,font,c.clock,effect,run.effects[effect]);
            y+=s*1.62f;
        }
        for(int effect=Power.INCOGNITO;effect<=Power.MONOCHROME;effect++) {
            effect(p,iconX,titleX,valueX,y,s,font,c.clock,effect,run.effects[effect]);
            y+=s*1.62f;
        }
        ReleaseChange.icon(p,ReleaseChange.SWIPE,iconX,y-s*.20f,s*.57f,c.clock);
        stat(p,titleX,valueX,y,font,"RESCUE SWIPES",value(run.swipes)); y+=s*1.62f;
        SettingsArt.kidsPear(p,iconX,y-s*.18f,s*.62f,run.kids);
        stat(p,titleX,valueX,y,font,"KIDS MODE",run.kids?"ON":"OFF");
    }
}
