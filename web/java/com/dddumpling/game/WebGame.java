package com.dddumpling.game;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Browser host reuses the tested, platform-free iOS gesture/frame router. */
public final class WebGame {
    private static IOSGame game;
    private static WebPainter painter;
    private static WebPointers pointers;
    public static void main(String[] args) {
        game=new IOSGame(new WebStore(),new WebSound(),230L);
        game.geometry().nativePause=false;
        game.achievementsEnabled(false);
        game.cloudStatus(CloudSaveUi.OFF);
        game.reviewAvailable(false);
        game.setHost(new IOSGame.Host() {
            public void tick() {vibrate(9);}
            public void impact() {vibrate(24);}
            public void openPrivacy(String url) {open(url);}
            public void openAppAction(boolean share) {notice("Store sharing and reviews are not available in this browser preview.");game.externalFinished();}
        });
        painter=new WebPainter(context(),true);
        pointers=new WebPointers(game);
        connect();
    }
    public static void resize(float w,float h) {pointers.cancel();game.layout(w,h,0,0,0,0);}
    public static void frame(float dt) {game.update(dt);game.draw(painter);}
    public static void pointer(int action,int id,float x,float y) {pointers.event(action,id,x,y);}
    public static boolean key(int code,boolean down,boolean repeat) {return down?game.keyDown(code,repeat):game.keyUp(code);}
    public static void hidden(boolean value) {pointers.cancel();game.background(value);}
    public static void back() {game.back();}
    public static void shake() {game.shakeOutfit();}
    public static void scene(String name) {
        if(!BuildFlags.DEVELOPER)return;
        pointers.cancel();
        if(name.equals("survival")) {
            game.debugScene("play");game.core().modes.selected=ModeSelector.SURVIVAL;game.core().startGame();
            game.core().survival.seconds=180;game.core().onboarding.clear();game.core().onboarding.saved|=Onboarding.SKIPPED;
        } else {
            if(!name.equals("title") && !name.equals("case") && !name.startsWith("progress:"))game.core().modes.selected=ModeSelector.ADVENTURE;
            game.debugScene(name);
        }
        game.core().devInfiniteLives=true;
        if(name.startsWith("stage:") || name.equals("play") || name.equals("ninja")) {
            game.core().onboarding.clear();game.core().onboarding.saved|=Onboarding.SKIPPED;
        }
    }
    public static String status() {return game.debugStatus()+";best="+game.core().best;}
    public static boolean paused() {return game.paused();}
    public static void warm(int i) {painter.warm(i);}
    @JSBody(script="return ddd.context;") private static native JSObject context();
    @JSBody(params="ms",script="if(ddd.haptics && navigator.vibrate)navigator.vibrate(ms);") private static native void vibrate(int ms);
    @JSBody(params="url",script="window.open(url,'_blank','noopener,noreferrer');") private static native void open(String url);
    @JSBody(params="s",script="ddd.notice(s);") private static native void notice(String s);
    @JSBody(script="ddd.connect({resize:(w,h)=>javaMethods.get('com.dddumpling.game.WebGame.resize(FF)V').invoke(w,h),frame:dt=>javaMethods.get('com.dddumpling.game.WebGame.frame(F)V').invoke(dt),pointer:(a,id,x,y)=>javaMethods.get('com.dddumpling.game.WebGame.pointer(IIFF)V').invoke(a,id,x,y),key:(c,d,r)=>javaMethods.get('com.dddumpling.game.WebGame.key(IZZ)Z').invoke(c,d,r),hidden:v=>javaMethods.get('com.dddumpling.game.WebGame.hidden(Z)V').invoke(v),back:()=>javaMethods.get('com.dddumpling.game.WebGame.back()V').invoke(),shake:()=>javaMethods.get('com.dddumpling.game.WebGame.shake()V').invoke(),scene:s=>javaMethods.get('com.dddumpling.game.WebGame.scene(Ljava/lang/String;)V').invoke(s),status:()=>javaMethods.get('com.dddumpling.game.WebGame.status()Ljava/lang/String;').invoke(),paused:()=>javaMethods.get('com.dddumpling.game.WebGame.paused()Z').invoke(),warm:i=>javaMethods.get('com.dddumpling.game.WebGame.warm(I)V').invoke(i)});")
    private static native void connect();
}
