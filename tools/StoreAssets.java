package com.dddumpling.game;

import java.io.File;

/** Store artwork made with the same characters and lettering as the game. */
final class StoreAssets extends Draw {
    public static void main(String[] args) throws Exception {
        File dir = new File(args.length == 0 ? "out/play-store" : args[0]);
        dir.mkdirs();
        RasterPainter icon = new RasterPainter(512, 512, 3);
        icon.clear(0xFF17142B);
        float s = 512f / 108f;
        icon.fillPoly(new float[] {54*s,20*s,83*s,37*s,83*s,71*s,
                54*s,88*s,25*s,71*s,25*s,37*s},0xFFFF8FA8);
        icon.fillPoly(new float[] {54*s,34*s,71*s,44*s,71*s,64*s,
                54*s,74*s,37*s,64*s,37*s,44*s},0xFF17142B);
        icon.fillCircle(54*s,44*s,9*s,0xFFFFE1A0);
        Png.write(new File(dir,"icon-512.png"),icon.resolve(),512,512);

        RasterPainter feature = new RasterPainter(1024,500,3);
        feature.clear(0xFF17142B);
        for (int i=0;i<20;i++) {
            float x=hash(i+80)*1024, y=hash(i+190)*500;
            feature.fillCircle(x,y,25+hash(i+5)*50,0x103F355E);
        }
        String name="DDDUMPLING";
        for (int i=0;i<name.length();i++)
            TitleBubbleFont.draw(feature,name.charAt(i),116+i*88,185,118,
                    Glyph.COLOR[i%Glyph.COUNT],1f,i*.37f,1f);
        for (int i=0;i<Glyph.COUNT;i++) {
            float x=132+i*152;
            feature.fillEllipse(x,383,51,10,0x663E355F);
            Kawaii.draw(feature,i,x,323,59,Glyph.COLOR[i],1f,1f);
        }
        feature.text("TAP. SQUISH. COLLECT.",512,455,28,0xFFFFE1A0,Painter.CENTER,true);
        Png.write(new File(dir,"feature-1024x500.png"),feature.resolve(),1024,500);
    }
}
