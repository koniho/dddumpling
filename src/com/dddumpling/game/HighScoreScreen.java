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
        if(selected>=0 && selected<c.highScores.displayCount()) {
            // The title screen is intentionally lively behind the glass. A summary needs a quieter
            // inner page so its small portraits and labels do not compete with those giant figures.
            p.fillRect(L.w*.07f,listTop(L),L.w*.93f,listBottom(L),Glyph.withAlpha(0xFF1D1935,205));
            summary(p,c,L,c.highScores.displayRun(selected));
        }
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
    private void score(Painter p,GameCore c,Layout L,HighScores.Run run,float y,float s) {
        if(run.id==c.highScores.latest) {
            for(int i=4;i>0;i--) p.fillEllipse(L.w*.5f,y-s*.42f,s*(3.2f+i*(.2f+.1f*pulse(c.clock))),s*(.65f+i*(.1f+.1f*pulse(c.clock))),
                    Glyph.withAlpha(GOLD,(int)((13-i*2)*(.4f+2f*pulse(c.clock)))));
        }
        p.text(String.valueOf(run.score),L.w*.5f,y,type(s*1.08f),INK,Painter.CENTER,true);
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
    private static void icon(Painter p,int kind,float x,float y,float r,float clock) {
        ReleaseChange.icon(p,kind==1?ReleaseChange.SHUFFLE:ReleaseChange.SWIPE,x,y,r,clock);
    }
    private static int used(int[] counts) { int n=0;for(int count:counts)if(count>0)n++;return n; }
    private static float effectUnits(int[] counts) {
        int n=used(counts);return n==0?0f:.85f+((n+1)/2)*1.55f;
    }
    private static void stat(Painter p,Layout L,float x,float y,float u,String label,String value) {
        p.text(value,x,y,type(u*.82f),INK,Painter.CENTER,true);
        p.text(label,x,y+u*.61f,type(u*.33f),INK_DIM,Painter.CENTER,true);
    }
    private static void portraits(Painter p,GameCore c,Layout L,int[] values,boolean boss,
            float top,float bottom,float maxR) {
        if(values.length==0 || bottom<=top)return;
        float width=L.w*.72f,height=bottom-top;
        int cols=Math.max(1,(int)Math.ceil(Math.sqrt(values.length*width/height)));
        cols=Math.min(cols,values.length);
        int rows=(values.length+cols-1)/cols;
        float cellW=width/cols,cellH=height/rows,r=Math.min(maxR,Math.min(cellW,cellH)*.40f);
        for(int i=0;i<values.length;i++) {
            int row=i/cols,col=i%cols,inRow=Math.min(cols,values.length-row*cols);
            float x=L.w*.5f+(col-(inRow-1)*.5f)*cellW;
            float y=top+(row+.5f)*cellH;
            if(boss)BossCollect.draw(p,values[i],x,y,r,c.clock,true,1f);
            else Trinket.draw(p,values[i],x,y,r,c.clock,true,1f);
        }
    }
    private static float effects(Painter p,GameCore c,Layout L,String title,int first,int[] counts,
            float top,float u) {
        int used=used(counts);if(used==0)return top;
        float height=effectUnits(counts)*u;
        p.text(title,L.w*.5f,top+u*.38f,type(u*.43f),
                first==0?GOLD:0xFFCDBDEA,Painter.CENTER,true);
        int slot=0;
        for(int i=0;i<counts.length;i++) {
            if(counts[i]==0)continue;
            int row=slot/2,col=slot%2,effect=first+i;
            float x=L.w*(col==0?.30f:.68f),y=top+u*(1.30f+row*1.55f);
            int hue=effect>=Power.COUNT?0xFF7761B8:Glyph.cycle(effect*.21f);
            Renderer.powerIcon(p,effect,x,y-u*.18f,u*.78f,hue,1f);
            p.text("×"+counts[i],x+u*.76f,y+u*.04f,type(u*.56f),INK,Painter.LEFT,true);
            slot++;
        }
        return top+height;
    }
    private void summary(Painter p,GameCore c,Layout L,HighScores.Run run) {
        float s=size(L),t=listTop(L),b=listBottom(L),cx=L.w*.5f;
        float units=6.25f+(run.bossOrder.length>0?3.05f:0f)
                +(run.prizes.length>0?(run.prizes.length>16?4.7f:run.prizes.length>8?4f:3.2f):0f)
                +effectUnits(run.powerUses)+effectUnits(run.debuffUses);
        float u=Math.min(s*1.48f,(b-t)/units);
        float y=t+u*.18f;

        float heroX=L.w*.24f,heroY=y+u*1.30f,heroR=u*1.20f;
        p.fillPoly(Glyph.hex(heroX,heroY,heroR*1.28f),Glyph.withAlpha(0xFF6E72C8,58));
        p.strokePoly(Glyph.hex(heroX,heroY,heroR*1.28f),Glyph.withAlpha(GOLD,130),u*.07f);
        Trinket.draw(p,Math.max(0,run.character),heroX,heroY,heroR,c.clock,run.character>=0,1f);
        score(p,c,L,run,y+u*1.22f,u);
        y+=u*2.75f;

        int pct=run.accuracy();
        stat(p,L,L.w*.23f,y+u*.45f,u,"STAGE",String.valueOf(run.stage));
        stat(p,L,L.w*.50f,y+u*.45f,u,"ACCURACY",pct+"%");
        stat(p,L,L.w*.77f,y+u*.45f,u,"BEST COMBO",String.valueOf(run.combo));
        p.text(run.stages+" STAGES CLEARED   •   "+run.squishes+" SQUISHES",cx,y+u*1.65f,
                type(u*.41f),INK_DIM,Painter.CENTER,true);
        y+=u*2.35f;

        if(run.bossOrder.length>0) {
            p.text("BOSSES DEFEATED",cx,y+u*.38f,type(u*.43f),GOLD,Painter.CENTER,true);
            portraits(p,c,L,run.bossOrder,true,y+u*.62f,y+u*2.72f,u*1.02f);
            y+=u*3.05f;
        }
        if(run.prizes.length>0) {
            float height=(run.prizes.length>16?4.45f:run.prizes.length>8?3.75f:2.95f)*u;
            p.text("DUMPLINGS COLLECTED",cx,y+u*.38f,type(u*.43f),GOLD,Painter.CENTER,true);
            portraits(p,c,L,run.prizes,false,y+u*.62f,y+height-u*.10f,u*.90f);
            y+=height+u*.25f;
        }
        y=effects(p,c,L,"POWERUPS",0,run.powerUses,y,u);
        if(used(run.powerUses)>0)y+=u*.25f;
        y=effects(p,c,L,"DEBUFFS",Power.COUNT,run.debuffUses,y,u);
        y+=u*.72f;
        p.text("↟ "+run.swipes+" RESCUES   •   START STAGE "+(run.land*Boss.EVERY+1)
                +(run.kids?"   •   KIDS MODE":""),cx,y,type(u*.39f),INK_DIM,Painter.CENTER,true);
    }
}
