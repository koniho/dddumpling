package com.dddumpling.game;

import java.io.*;
import java.lang.reflect.Proxy;

/** Development footage from a real Time Attack run with bounded scripted inputs. */
final class GatorClips {
    static final int W=540,H=960,FPS=30;
    public static void main(String[] args) throws Exception {
        Layout l=new Layout();l.compute(W,H,0,0,0,0);
        GameCore c=TestTimeAttack.start(Boss.ALLIGATOR);
        Bot bot=new Bot(6f,.20f,0,false,168);
        boolean dry=args.length>0 && args[0].equals("--probe");
        File dir=new File("build/gator-clips");dir.mkdirs();
        Trailer.Mixer mix=new Trailer.Mixer();
        float[] sound=new float[Sfx.RATE*150];
        c.sound=(GameCore.Sound)Proxy.newProxyInstance(GatorClips.class.getClassLoader(),
                new Class<?>[]{GameCore.Sound.class},mix);
        Process encoder=dry?null:new ProcessBuilder("ffmpeg","-y","-hide_banner","-loglevel","error",
                "-f","rawvideo","-pixel_format","rgb24","-video_size",W+"x"+H,
                "-framerate",""+FPS,"-i","pipe:0","-an","-c:v","libx264","-preset","veryfast",
                "-crf","18","-pix_fmt","yuv420p",new File(dir,"full-silent.mp4").toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        RasterPainter p=new RasterPainter(W,H,1);byte[] rgb=new byte[W*H*3];
        int frames=0,dazes=0;float finished=0;
        try(OutputStream out=dry?OutputStream.nullOutputStream():new BufferedOutputStream(encoder.getOutputStream(),rgb.length)) {
            while(frames<FPS*140) {
                for(int step=0;step<2;step++) {
                    int at=Math.round((frames/(float)FPS+step/60f)*Sfx.RATE);
                    mix.at=0;
                    c.update(1f/60f,l);bot.step(c,l,1f/60f);
                    if(!dry)for(int i=0;i<Sfx.RATE*3 && at+i<sound.length;i++) {
                        sound[at+i]+=mix.audio[i];mix.audio[i]=0;
                    }
                    if(c.boss.alligator.dazes>dazes) {
                        dazes=c.boss.alligator.dazes;
                        System.out.println("long opening at "+frames/(float)FPS+"s, hp="+c.boss.hp);
                    }
                    if(c.state==GameCore.OVER)finished+=1f/60f;
                }
                if(!dry) {
                    p.clear(Draw.BG);Renderer.draw(p,c,l);int[] pixels=p.resolve();
                    for(int i=0,j=0;i<pixels.length;i++) {rgb[j++]=(byte)(pixels[i]>>16);rgb[j++]=(byte)(pixels[i]>>8);rgb[j++]=(byte)pixels[i];}
                    out.write(rgb);
                    if(frames%FPS==0)Png.write(new File(dir,"frame-"+(frames/FPS)+".png"),pixels,W,H);
                }
                frames++;if(finished>=2f)break;
            }
        }
        if(encoder!=null && encoder.waitFor()!=0)throw new IOException("Video encoding failed");
        float duration=frames/(float)FPS;
        System.out.println("duration="+duration+", won="+c.timeAttack.won+", lives="+c.lives+", hp="+c.boss.hp+", dazes="+dazes);
        if(!c.timeAttack.won)throw new IllegalStateException("No real Gator victory");
        if(dry)return;
        short[] music=Music.bossLoop(Music.SWING_STYLE),audio=new short[Math.round(duration*Sfx.RATE)];
        for(int i=0;i<audio.length;i++)audio[i]=(short)Math.max(-30000,Math.min(30000,sound[i]+music[i%music.length]*.22f));
        Wav.write(new File(dir,"full.wav"),audio,Sfx.RATE);
        System.out.println("Raw footage and game audio ready in "+dir);
    }
}
