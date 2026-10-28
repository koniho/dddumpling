package com.dddumpling.game;
import java.io.File;
/** Only the wardrobe cues, at the same gain used by both native hosts. */
final class AccessoryAudioPreview {
    public static void main(String[] args) throws Exception {
        File dir=new File(args.length>0?args[0]:"out/accessory-sfx");dir.mkdirs();
        for(int slot=0;slot<Accessories.SLOTS;slot++)for(boolean off:new boolean[]{false,true}) {
            short[] pcm=Sfx.build(Sfx.accessoryId(slot,off)).clone();
            for(int i=0;i<pcm.length;i++)pcm[i]=(short)(pcm[i]*Sfx.ACCESSORY_GAIN);
            Wav.write(new File(dir,Accessories.SLOT[slot].toLowerCase(java.util.Locale.US)+(off?"-off":"-on")+".wav"),pcm,Sfx.RATE);
        }
    }
}
