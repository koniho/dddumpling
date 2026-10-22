package com.dddumpling.game;

/** Device-local introduction and status; native hosts supply service availability. */
final class CloudSaveUi extends Draw {
    static final int OFF=0, SIGN_IN=1, DRIVE=2, PENDING=3, SAVED=4, FAILED=5, ACCOUNT=6;
    boolean enabled, seen, open, introduction, notice, failureShown, reduceMotion, playGames;
    int status, pending=-1, character=Collect.STAR_FIRST, action, pointer=-1, pressed;
    float age;
    boolean noticeHeld;
    float downX,downY;
    void status(int value) {
        status=value;enabled=value!=OFF;
        if((value==FAILED || value==ACCOUNT) && !failureShown)notice=true;
        if(value==SAVED || value==PENDING)notice=false;
    }
    void collected(int who) { if(enabled && !seen && pending<0) pending=who; }
    boolean update(GameCore c,float elapsed) {
        if(!enabled) {open=false;return false;}
        if(open && introduction && (status==PENDING || status==SAVED)) {remember(c);open=false;}
        if(!open && pending>=0 && !seen && safe(c)) {
            if(status==PENDING || status==SAVED) { remember(c);pending=-1; }
            else {character=pending;pending=-1;show(true);}
        }
        if(open) { age+=Math.min(.1f,elapsed);return true; }
        return false;
    }
    private boolean safe(GameCore c) {
        boolean between=c.state==GameCore.PLAY && c.stageGap>0 && c.enemies.isEmpty()
                && !c.boss.active() && !c.pendingBonus && !c.starting();
        return (c.state==GameCore.TITLE || between) && !c.starter.open && !c.caseOpen && !c.townOpen
                && !c.settingsOpen && !c.paused && !c.releaseNotes.open && !c.highScoreScreen.open
                && !c.onboarding.speaking(c) && !c.onboarding.collectionPending(c)
                && c.onboarding.practice==null && c.returnFade<=0 && c.homeT<=0;
    }
    void show(boolean intro) {open=true;introduction=intro;age=0;pointer=-1;pressed=0;}
    void remember(GameCore c) {seen=true;if(c.store!=null)c.store.saveCloudIntro(true);}
    void close(GameCore c) {
        if(introduction)remember(c);
        if(status==FAILED || status==ACCOUNT)failureShown=true;
        open=false;pointer=-1;pressed=0;notice=false;
    }
    int takeAction() {int value=action;action=0;return value;}
    boolean noticeVisible(GameCore c) {return enabled && notice && !open && c.state==GameCore.TITLE && safe(c);}
    boolean noticeTouch(GameCore c,Layout L,int kind,int id,float x,float y) {
        boolean inside=noticeVisible(c) && x>=L.w*.08f && x<=L.w*.92f
                && Math.abs(y-(L.topSafe+unit(L)*2))<=unit(L)*1.2f;
        if(kind==0 && inside) {noticeHeld=true;pointer=id;downX=x;downY=y;}
        if(!noticeHeld)return false;
        if(kind==3) {noticeHeld=false;pointer=-1;return true;}
        if((kind==1 || kind==6) && id==pointer) {
            noticeHeld=false;pointer=-1;
            if(inside && Math.abs(x-downX)+Math.abs(y-downY)<unit(L)) {failureShown=true;notice=false;show(false);}
        }
        return true;
    }
    static float unit(Layout L) {return Math.min(L.w/22f,(L.h-L.topSafe-L.padB)/32f);}
    static float top(Layout L) {return L.topSafe+(L.h-L.topSafe-L.padB-unit(L)*29f)*.5f;}
    int hit(Layout L,float x,float y) {
        float s=unit(L),t=top(L);
        if(x<L.w*.1f || x>L.w*.9f)return 0;
        if(Math.abs(y-(t+s*23.5f))<s*1.4f)return 1;
        if(Math.abs(y-(t+s*27f))<s*1.2f)return 2;
        return 0;
    }
    boolean touch(GameCore c,Layout L,int kind,int id,float x,float y) {
        if(!open)return false;
        if(kind==3) {pointer=-1;pressed=0;return true;}
        if(kind==0) {pointer=id;pressed=age>.35f?hit(L,x,y):0;downX=x;downY=y;}
        if(kind==2 && id==pointer && Math.abs(x-downX)+Math.abs(y-downY)>unit(L))pressed=0;
        if((kind==1 || kind==6) && pointer==id) {
            int h=pressed;pointer=-1;pressed=0;
            if(h!=0 && h==hit(L,x,y))choose(c,h);
        }
        return true;
    }
    void choose(GameCore c,int choice) {
        if(!open)return;
        if(choice==2 || !introduction && status!=SIGN_IN)close(c);
        else {remember(c);introduction=false;action=1;}
    }
    String shortLabel() {
        switch(status) {
            case SIGN_IN:return "SIGN IN";
            case PENDING:return "SYNC PENDING";
            case SAVED:return "SAVE QUEUED";
            case DRIVE:return "UNAVAILABLE";
            case FAILED:return "NEEDS ATTENTION";
            case ACCOUNT:return "SYNC PAUSED";
            default:return "ON THIS DEVICE";
        }
    }
    void icon(Painter p,float x,float y,float r,float clock) {
        int cloud=enabled?0xFFD6F4F1:0xFF92949F;
        p.fillRect(x-r*.75f,y-r*.05f,x+r*.75f,y+r*.48f,cloud);
        p.fillCircle(x-r*.7f,y+r*.05f,r*.43f,cloud);
        p.fillCircle(x-r*.18f,y-r*.25f,r*.65f,cloud);
        p.fillCircle(x+r*.52f,y-r*.02f,r*.5f,cloud);
        float bx=x+r*.76f,by=y+r*.44f,b=r*.5f;
        int color=status==FAILED || status==DRIVE?0xFFFFC56B:enabled?0xFF86E5CE:0xFFB7B7C6;
        p.fillCircle(bx,by,b*1.2f,0xFF211D36);
        p.strokeCircle(bx,by,b,color,r*.08f);
        if(status==PENDING) {
            float angle=reduceMotion?0:clock*150f;
            p.arc(bx,by,b*.65f,b*.65f,angle,260,color,r*.13f);
            double rad=Math.toRadians(angle+260);
            p.fillCircle(bx+(float)Math.cos(rad)*b*.65f,by+(float)Math.sin(rad)*b*.65f,r*.11f,color);
        } else if(status==SAVED) {
            // The native callback confirms queuing, not arrival on Google's servers.
            p.line(bx,by-b*.6f,bx,by,color,r*.09f);
            p.line(bx,by,bx+b*.4f,by+b*.22f,color,r*.09f);
        } else if(status==SIGN_IN) {
            p.fillCircle(bx,by-b*.3f,b*.24f,color);
            p.fillEllipse(bx,by+b*.3f,b*.43f,b*.26f,color);
        } else if(status==ACCOUNT) {
            for(int side=-1;side<=1;side+=2)
                p.line(bx+side*b*.25f,by-b*.45f,bx+side*b*.25f,by+b*.45f,color,r*.13f);
        } else if(status==FAILED || status==DRIVE) {
            p.line(bx,by-b*.5f,bx,by+b*.08f,color,r*.12f);
            p.fillCircle(bx,by+b*.47f,r*.07f,color);
        } else {
            p.line(bx-b*.5f,by+b*.5f,bx+b*.5f,by-b*.5f,color,r*.12f);
        }
    }
    String primary() {return introduction?"Set up cloud saves":status==SIGN_IN?"Sign in":"Done";}
    String summary() {
        return introduction?"Keep your collection safe. Sync your squishies and progress across "+(playGames?"Android":"Apple")+" devices. Your local save stays here."
                :label()+". "+String.join(" ",details());
    }
    String label() {
        switch(status) {
            case SIGN_IN:return playGames?"SIGN IN TO PLAY GAMES":"SIGN IN TO GAME CENTER";
            case DRIVE:return "ICLOUD DRIVE UNAVAILABLE";
            case PENDING:return "CLOUD SAVE PENDING";
            case SAVED:return playGames?"CLOUD SAVE QUEUED":"CLOUD SAVE PENDING";
            case ACCOUNT:return "SYNC PAUSED";
            case FAILED:return playGames?"CLOUD SYNC NEEDS ATTENTION":"COULDN'T SAVE TO ICLOUD";
            default:return "SAVED ON THIS DEVICE";
        }
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(noticeVisible(c)) {
            float s=unit(L),y=L.topSafe+s*2;
            p.fillPoly(pill(L.w*.5f,y,L.w*.42f,s*1.2f,12),0xEE4A3654);
            p.text("CLOUD SAVE NEEDS ATTENTION",L.w*.5f,y+s*.2f,type(s*.45f),INK,Painter.CENTER,true);
        }
        if(!open)return;
        float s=unit(L),t=top(L),x=L.w*.5f;
        p.fillRect(0,0,L.w,L.h,0xAA1B1730);
        p.save();p.translate(0,reduceMotion?0:(1f-panelTravel(age/.35f))*s*3f);
        glassPanel(p,L.w*.06f,t,L.w*.94f,t+s*29f,s);
        float hop=reduceMotion?0:(float)Math.sin(age*2.5f),r=s*3.3f;
        p.save();p.translate(x,t+s*7f-hop*s*.3f);
        Trinket.drawReacting(p,character,0,0,r*(1f+hop*.035f),reduceMotion?0:c.clock,1f,1,0f);p.restore();
        for(int i=0;i<3;i++) {
            float phase=(reduceMotion?0:age*1.8f)+i*2.1f;
            float sx=x+(float)Math.cos(phase)*s*5f,sy=t+s*7f+(float)Math.sin(phase)*s*2.5f;
            p.fillCircle(sx,sy,s*.1f,GOLD);
        }
        p.text(introduction?"KEEP YOUR COLLECTION":"CLOUD SAVES",x,t+s*12.2f,type(s*.73f),INK,Painter.CENTER,true);
        p.text(introduction?"SAFE":label(),x,t+s*14f,type(s*(introduction?.85f:.5f)),introduction?GOLD:INK_DIM,Painter.CENTER,true);
        String[] lines=introduction?new String[]{"Your squishies can travel with you.","Keep your progress across "+(playGames?"Android":"Apple")+" devices.","Your local save stays right here."}:details();
        for(int i=0;i<lines.length;i++)p.text(lines[i],x,t+s*(16.2f+i*1.35f),type(s*.43f),INK,Painter.CENTER,false);
        p.fillPoly(pill(x,t+s*23.5f,s*8.4f,s*1.4f,12),0xFF387B65);
        p.text(introduction?"SET UP CLOUD SAVES":status==SIGN_IN?"SIGN IN":"DONE",x,t+s*23.8f,type(s*.58f),INK,Painter.CENTER,true);
        p.text(introduction?"NOT NOW":"BACK",x,t+s*27.25f,type(s*.5f),INK_DIM,Painter.CENTER,true);
        p.restore();
    }
    private String[] details() {
        if(playGames) switch(status) {
            case SIGN_IN:return new String[]{"Sign in to Play Games to get started.","Use the same profile on your devices.","Your progress is saved on this device."};
            case ACCOUNT:return new String[]{"This save belongs to another profile.","Return to your original Play Games profile.","Your progress is saved on this device."};
            case FAILED:return new String[]{"Check your connection and Play Games.","Reopen the game to try connecting again.","Your local progress stays safe."};
            case PENDING:case SAVED:return new String[]{"Your progress is saved on this device.","Play Games transfers it in the background.","Other devices may take a moment."};
            default:return new String[]{"Your progress is saved on this device.","Cloud sync is not available right now."};
        }
        switch(status) {
            case SIGN_IN:return new String[]{"Sign in to Game Center to get started.","Cloud saves also need iCloud Drive.","Your progress is saved on this device."};
            case DRIVE:return new String[]{"Open Settings on your iPhone.","Apple Account > iCloud > Drive", "Turn on Sync this iPhone."};
            case ACCOUNT:return new String[]{"This save belongs to another account.","Return to your original accounts to sync.","Your progress is saved on this device."};
            case FAILED:return new String[]{"Check your connection and iCloud storage.","We'll retry while the game is open.","Your progress is saved on this device."};
            default:return new String[]{"Your progress is saved on this device.","iCloud transfers saves in the background.","Other devices may take a moment."};
        }
    }
}
