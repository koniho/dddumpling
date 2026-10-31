package com.dddumpling.game;

final class TestSquishyFit extends Check {
    static GameCore game(Layout L) {
        GameCore c=new GameCore(new Mem(),218);c.startGame();c.stageBanner=0;
        c.earnedMash=GameCore.MASH_PERFECT;c.squishyFit.next=true;Interlude.enterBonus(c,L);return c;
    }
    static void touch(GameCore c,Layout L,int action,int id,float x,float y) {
        c.squishyFit.touch(c,L,action,id,SquishyFit.px(L,x),SquishyFit.py(L,y));
    }
    static float area(SquishyFit m) {float sum=0;for(SquishyFit.Piece q:m.pieces)sum+=q.r*q.r;return sum;}
    static void all(Layout L) {
        group("Squishy Fit");
        check("perfect grants ten seconds",SquishyFit.attemptTime(GameCore.MASH_PERFECT)==10);
        check("unhurt grants eight seconds",SquishyFit.attemptTime(GameCore.MASH_UNHURT)==8);
        check("damage grants six seconds",SquishyFit.attemptTime(GameCore.MASH_HURT)==6);
        check("rescue grants five seconds",SquishyFit.attemptTime(GameCore.MASH_PANIC)==5);
        GameCore c=game(L);SquishyFit m=c.squishyFit;
        check("separate interlude excludes steamer and star inputs",m.active && !c.starBonus && !c.bonusMashing() && !c.bonusRolling() && !c.bonusStatus());
        SquishyFit.Piece parent=m.pieces.get(0);
        float startX=parent.x,startY=parent.y;
        touch(c,L,0,7,startX,startY);touch(c,L,2,7,0,0);touch(c,L,1,7,0,0);
        check("oversized drop rebounds outside without credit",parent.tooBig && !parent.placed && parent.reject>0 && parent.x==startX && parent.y==startY && m.coverage==0);
        m.update(.05f,.05f);parent.reject=0;m.ready=true;m.timer=10;parent.move(0,0);
        check("initial piece cannot fit",!m.fits(parent));
        m.update(.05f,3);check("instructions do not consume attempt time",m.ready && m.timer==10);
        float original=area(m),y=parent.y;
        touch(c,L,0,4,-.1f,y);touch(c,L,5,9,.1f,y);
        touch(c,L,2,4,-.23f,y);touch(c,L,2,9,.23f,y);
        check("two fingers split into two bodies",m.pieces.size()==2);
        check("split conserves material",Math.abs(area(m)-original)<.000001f);
        touch(c,L,6,9,.23f,y);touch(c,L,1,4,-.23f,y);
        m.finished=m.won=false;m.timer=10;
        m.pieces.clear();SquishyFit.Piece q=new SquishyFit.Piece(0,0,.1f,0xFF99EECC);m.pieces.add(q);
        check("contained piece settles",m.place(q));m.measure();float fill=m.coverage;
        check("placed area counts",fill>0 && fill<SquishyFit.TARGET);
        touch(c,L,0,14,0,0);touch(c,L,2,14,.16f,0);touch(c,L,1,14,.16f,0);
        check("fitting piece can be repositioned",q.placed && Math.abs(q.x-.16f)<.0001f);
        touch(c,L,0,14,.16f,0);touch(c,L,2,14,.4f,.1f);touch(c,L,1,14,.4f,.1f);
        check("invalid reposition returns to previous tray spot",q.placed && Math.abs(q.x-.16f)<.0001f && q.reject>0);
        q.reject=0;q.move(0,0);m.measure();fill=m.coverage;
        SquishyFit.Piece overlap=new SquishyFit.Piece(0,0,.1f,0xFF99EECC);m.pieces.add(overlap);
        check("overlap cannot double-count",!m.place(overlap));m.measure();check("coverage unchanged for invalid overlap",m.coverage==fill);
        overlap.move(.37f,0);check("outside piece rejected",!m.place(overlap));
        int hits=c.hits,misses=c.misses;for(int i=0;i<6;i++)c.tapBonus(i);
        check("packing leaves combat accuracy alone",c.hits==hits && c.misses==misses);
        touch(c,L,0,6,0,0);Pause.open(c);float time=m.timer;c.update(.05f,L);
        check("pause cancels held pieces and freezes timer",!m.held(q) && m.timer==time);Pause.resume(c);
        m.ready=false;m.timer=.1f;m.update(.05f,.2f);
        check("real elapsed time ends attempt despite physics cap",m.finished && !m.won && m.timer==0);
        int count=m.pieces.size();float px=q.x,py=q.y;SquishyFitInterlude.update(c,L,.05f,3);
        check("failed attempt stays selected and keeps pieces",m.next && !m.active && m.pieces.size()==count && q.x==px && q.y==py);
        c.state=GameCore.PLAY;Interlude.enterBonus(c,L);
        check("next stage resumes tray",m.active && m.pieces.size()==count);
        c.toTitle();c.startGame();check("new run clears tray and shape difficulty",m.pieces.isEmpty() && m.completed==0 && !m.next);
        c=game(L);m=c.squishyFit;parent=m.pieces.get(0);
        touch(c,L,0,1,parent.x,parent.y);touch(c,L,1,1,parent.x,parent.y);
        check("single tap splits without multitouch",m.pieces.size()==2 && !m.held(m.pieces.get(0)));
        SquishyFit.Piece child=m.pieces.get(0),sibling=m.pieces.get(1);
        check("split level gives children a distinct shared color",child.level==1 && child.color==sibling.color && child.color!=parent.color);
        check("tap children spring apart",Math.abs(child.x-sibling.x)>child.r+sibling.r);
        child.move(0,.08f);check("round child does not fit near edge",!m.fits(child));
        check("piece compresses to fit and preserves area",m.settle(child) && child.squeeze!=1 && Math.abs((child.r*child.squeeze)*(child.r/child.squeeze)-child.r*child.r)<.000001f);
        m.measure();check("first tray completes with one split and one placement",m.coverage>=m.target());
        m.update(.01f,.01f);check("first easy placement wins",m.won);
        m.pieces.clear();q=new SquishyFit.Piece(.32f,0,.1f,0);m.pieces.add(q);
        check("nearby valid spot attracts an imprecise drop",!m.room(q) && m.settle(q) && q.x<.32f && m.fits(q));
        SquishyFit.Piece collision=new SquishyFit.Piece(q.x,q.y,.07f,0);m.pieces.add(collision);
        check("deformed pieces still reject overlapping centers",!m.place(collision));
        c=game(L);m=c.squishyFit;
        // Deterministic packing search uses only the original material divided into 64 equal parts.
        for(int shape=0;shape<SquishyFit.SHAPES.length;shape++) {
            m.completed=shape;pack(m);
            check("shape solvable with conserved material "+shape,m.coverage>=SquishyFit.TARGET && area(m)<=original+.000001f);
        }
        int total=c.collectTotal,score=c.score;m.ready=false;
        SquishyFitInterlude.update(c,L,.01f,.01f);
        check("completion rewards exactly one collectible",m.won && m.paid && c.collectTotal==total+1 && c.score==score+GameCore.FREE_BONUS);
        SquishyFitInterlude.update(c,L,.01f,.01f);check("reward cannot repeat",c.collectTotal==total+1);
        m.report=0;c.paradeTimer=0;int shape=m.completed;
        check("completed tray exits after celebration",SquishyFitInterlude.update(c,L,.01f,.01f));
        check("success advances shape and returns to steamer",!m.next && !c.starNext && m.completed==shape+1 && m.pieces.isEmpty());
        c=game(L);c.squishyFit.stop();c.squishyFit.next=false;c.starBonus=true;
        c.stars.won=true;c.stars.timer=0;c.paradeTimer=0;c.update(.01f,L);
        check("star completion queues packing",c.squishyFit.next && !c.starNext);
        SettingsUi u=new SettingsUi();u.compute(L,SettingsUi.MINIGAMES);
        check("developer chip has distinct hit",u.hit(L.w*.5f,u.fitY+u.testH*.5f)==SettingsUi.HIT_TEST+SettingsUi.TEST_FIT);
        c=new GameCore(new Mem(),218);c.startGame();c.jumpToStage(5,L);SquishyFitInterlude.playtest(c,L);
        check("developer launch cleans boss and enters real puzzle",c.squishyFit.active && !c.boss.active() && c.enemies.isEmpty() && c.state==GameCore.BONUS);
    }
    static void pack(SquishyFit m) {
        m.pieces.clear();float r=SquishyFit.INITIAL_RADIUS/8;
        for(float y=-.27f;y<=.27f;y+=r*.18f)for(float x=-.38f;x<=.38f;x+=r*.18f) {
            if(m.pieces.size()>=SquishyFit.MAX_PIECES)break;
            SquishyFit.Piece q=new SquishyFit.Piece(x,y,r,SquishyFit.COLORS[m.pieces.size()%5]);
            if(m.place(q))m.pieces.add(q);
        }
        m.measure();
    }
}
