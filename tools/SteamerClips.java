package com.dddumpling.game;

import java.io.*;
import java.lang.reflect.Proxy;

/** Short portrait captures driven through the game's steamer inputs and reward flow. */
final class SteamerClips {
    static final int W=540,H=960,FPS=30;
    public static void main(String[] args) throws Exception {
        File dir=new File(args.length==0?"build/steamer-clips":args[0]);dir.mkdirs();
        for(int take=1;take<=3;take++)capture(dir,take);
    }
    static void capture(File dir,int take) throws Exception {
        Layout l=new Layout();l.compute(W,H,0,0,0,0);
        GameCore c=new GameCore(new Check.Mem(),7500L+take);c.startGame();c.stage=4;
        Trailer.Mixer mix=new Trailer.Mixer();
        c.sound=(GameCore.Sound)Proxy.newProxyInstance(SteamerClips.class.getClassLoader(),
                new Class<?>[]{GameCore.Sound.class},mix);
        c.playtestSteamer(l);
        String name="steamer-success-"+take;
        File silent=new File(dir,name+"-silent.mp4");
        Process encoder=new ProcessBuilder("ffmpeg","-y","-hide_banner","-loglevel","error",
                "-f","rawvideo","-pixel_format","rgb24","-video_size",W+"x"+H,
                "-framerate",""+FPS,"-i","pipe:0","-an","-c:v","libx264","-preset","veryfast",
                "-crf","18","-pix_fmt","yuv420p",silent.toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        RasterPainter painter=new RasterPainter(W,H,1);byte[] rgb=new byte[W*H*3];
        int frames=0;float tapDue=0,swipeAge=0,finished=0;boolean won=false;
        try(BufferedOutputStream out=new BufferedOutputStream(encoder.getOutputStream(),rgb.length)) {
            while(frames<FPS*20) {
                for(int step=0;step<2;step++) {
                    float dt=1f/60f;mix.at=Math.round((frames/(float)FPS+step*dt)*Sfx.RATE);
                    tapDue-=dt;
                    if(c.bonusSwipeReady()) {
                        swipeAge+=dt;c.dragBonusLid(Math.min(1f,swipeAge/.3f));
                        if(swipeAge>=.3f) {c.swipeBonus();won=c.bonusPrizeWon();}
                    } else if(c.bonusMashing() && tapDue<=0) {c.tapBonus(c.steamer.wanted());tapDue=.14f;}
                    c.update(dt,l);
                    if(won && c.state==GameCore.PLAY)finished+=dt;
                }
                painter.clear(Draw.BG);Renderer.draw(painter,c,l);int[] pixels=painter.resolve();
                for(int i=0,j=0;i<pixels.length;i++) {
                    rgb[j++]=(byte)(pixels[i]>>16);rgb[j++]=(byte)(pixels[i]>>8);rgb[j++]=(byte)pixels[i];
                }
                out.write(rgb);
                if(frames%FPS==0)Png.write(new File(dir,name+"-"+(frames/FPS)+"s.png"),pixels,W,H);
                frames++;
                if(finished>.25f)break;
            }
        }
        if(encoder.waitFor()!=0 || !won || c.collectTotal==0 || finished==0)
            throw new IllegalStateException("Capture did not complete a genuine steamer win: "+name);
        float duration=frames/(float)FPS;
        short[] music=Music.loop(Music.SWING_STYLE),audio=new short[Math.round(duration*Sfx.RATE)];
        for(int i=0;i<audio.length;i++) {
            float fade=Math.min(1f,Math.min(i/(Sfx.RATE*.2f),(audio.length-i)/(Sfx.RATE*.35f)));
            audio[i]=(short)Math.max(-30000,Math.min(30000,(mix.audio[i]+music[i%music.length]*.2f)*fade));
        }
        File wav=new File(dir,name+".wav");Wav.write(wav,audio,Sfx.RATE);
        float clipDuration=duration-.7f;
        Process mux=new ProcessBuilder("ffmpeg","-y","-hide_banner","-loglevel","error",
                "-i",silent.toString(),"-i",wav.toString(),"-t",""+clipDuration,
                "-vf","fade=t=out:st="+(clipDuration-.25f)+":d=0.25",
                "-c:v","libx264","-preset","fast","-crf","18","-c:a","aac","-b:a","160k",
                "-af","loudnorm=I=-16:TP=-1.5:LRA=11,afade=t=out:st="+(clipDuration-.25f)+":d=0.25",
                "-movflags","+faststart","-shortest",
                new File(dir,name+".mp4").toString()).inheritIO().start();
        if(mux.waitFor()!=0)throw new IOException("Audio mux failed");
        System.out.println(name+": "+duration+"s, prize="+c.prize+", collected="+c.collectTotal);
    }
}
