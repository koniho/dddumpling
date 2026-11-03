package com.dddumpling.game;
import java.nio.file.*;
import java.io.*;

/** Offline audio export: retain the same synthesized/recorded samples as native hosts. */
public final class WebAssets {
    private static void le(DataOutputStream out,int value,int bytes)throws IOException {for(int i=0;i<bytes;i++)out.writeByte(value>>>(i*8));}
    private static void wav(Path path,short[] pcm)throws IOException {
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeBytes("RIFF");le(out,36+pcm.length*2,4);out.writeBytes("WAVEfmt ");le(out,16,4);le(out,1,2);le(out,1,2);
            le(out,Sfx.RATE,4);le(out,Sfx.RATE*2,4);le(out,2,2);le(out,16,2);out.writeBytes("data");le(out,pcm.length*2,4);
            for(short s:pcm)le(out,s,2);
        }
    }
    public static void main(String[] args)throws Exception {
        Path dir=Path.of(args[0]);Files.createDirectories(dir);
        for(int i=0;i<Sfx.count();i++)wav(dir.resolve("sfx-"+i+".wav"),Sfx.build(i));
        wav(dir.resolve("rocket.wav"),Sfx.rocket());wav(dir.resolve("bubble.wav"),Sfx.bubble());
        wav(dir.resolve("versus.wav"),VersusMusic.clip());
        for(int i=0;i<Music.NAMES.length;i++) {
            wav(dir.resolve("music-"+i+".wav"),Music.loop(i));
            wav(dir.resolve("frenzy-"+i+".wav"),Music.loop(i,true));
            wav(dir.resolve("boss-"+i+".wav"),Music.bossLoop(i));
        }
        Files.writeString(dir.resolve("manifest.json"),"{\"effects\":"+Sfx.count()+",\"music\":"+Music.NAMES.length+",\"rate\":"+Sfx.RATE+"}");
        System.out.println("Exported "+Sfx.count()+" effects and native music arrangements.");
    }
}
