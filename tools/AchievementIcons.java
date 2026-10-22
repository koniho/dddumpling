package com.dddumpling.game;

import java.io.File;

/** Store badges rendered from the game's own character art. */
final class AchievementIcons {
    public static void main(String[] args) throws Exception {
        File out=new File(args[0]); out.mkdirs();
        for(int i=0;i<Achievements.size();i++) {
            RasterPainter p=new RasterPainter(512,512,2);
            p.clear(Draw.BG);
            p.fillCircle(256,256,232,0xFF52496C);
            p.fillCircle(256,256,214,0xFF293E50);
            if(i>=Achievements.bossStart() && i<Achievements.extraStart())
                BossCollect.draw(p,i-Achievements.bossStart(),256,245,132,1,true,1);
            else {
                int glyph=i<3?Kawaii.DUMPLING:i<7?Kawaii.CAT:i==7?Kawaii.DUMPLING:Kawaii.SQUISHY;
                Kawaii.draw(p,glyph,256,235,120,Glyph.COLOR[i%Glyph.COLOR.length],1,.4f);
                if(i<7 || i>=Achievements.extraStart()) {
                    int number=Achievements.target(i);
                    if(Achievements.id(i).startsWith("survival_"))number/=60;
                    p.text(Integer.toString(number),256,405,70,Draw.GOLD,Painter.CENTER,true);
                }
                else p.text(i==7?"STEAM":"STAR",256,405,45,Draw.GOLD,Painter.CENTER,true);
            }
            Png.write(new File(out,Achievements.id(i)+".png"),p.resolve(),512,512);
        }
    }
}
