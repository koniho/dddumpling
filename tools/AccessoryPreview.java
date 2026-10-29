package com.dddumpling.game;
import java.io.File;

/** Portrait review sheets use exactly the live wardrobe and character renderers. */
final class AccessoryPreview {
    static void frames(File dir,Layout L,int w,int h,int ss) throws Exception {
        GameCore c=TestAccessories.ready();TestAccessories.grant(c);c.clock=1.2f;
        c.settingsOpen=true;c.settingsPage=1;c.settingsTab=SettingsUi.PROGRESS;
        RasterPainter settings=new RasterPainter(w,h,ss);settings.clear(Draw.BG);
        PlayerSettings.draw(settings,c,L);
        Png.write(new File(dir,"214-developer-accessories.png"),settings.resolve(),w,h);
        c.settingsOpen=false;c.caseOpen=true;c.caseFade=1;c.caseIndex=0;c.openStory();c.storyT=2;c.wardrobe.screen.trayT=1;
        for(int i=0;i<3;i++) {
            c.wardrobe.screen.show(c,0);c.wardrobe.screen.trayT=1;c.wardrobe.screen.selected=i==0?0:3;
            if(i==1) {c.wardrobe.equip(c,3,1);c.wardrobe.equip(c,3,0);c.wardrobe.screen.slot=Accessories.GLASSES;}
            if(i==2) {c.wardrobe.screen.reward=true;}
            RasterPainter canvas=new RasterPainter(w,h,ss);Renderer.draw(canvas,c,L);
            Png.write(new File(dir,"214-wardrobe-"+i+".png"),canvas.resolve(),w,h);
            if(i==0) {
                float clock=c.clock;c.clock+=8;
                RasterPainter drift=new RasterPainter(w,h,ss);Renderer.draw(drift,c,L);
                Png.write(new File(dir,"214-night-drift.png"),drift.resolve(),w,h);c.clock=clock;
            }
        }
        c.wardrobe.screen.show(c,0);
        for(int category=0;category<Accessories.SLOTS;category++) {
            c.wardrobe.screen.choose(c,10+category);
            for(int sample=0;sample<2;sample++) {
                c.wardrobe.screen.trayT=sample==0?.9f:3.1f;c.wardrobe.screen.pulse=2;
                RasterPainter window=new RasterPainter(w,h,ss);Renderer.draw(window,c,L);
                Png.write(new File(dir,"214-box-window-"+category+"-"+sample+".png"),window.resolve(),w,h);
            }
        }
        c.wardrobe.screen.show(c,0);c.wardrobe.screen.trayT=1;c.wardrobe.screen.choose(c,10+Accessories.HAT);c.wardrobe.screen.trayT=1;
        c.wardrobe.screen.back(c);
        for(int sample=0;sample<4;sample++) {
            c.wardrobe.screen.exitT=new float[]{.10f,.30f,.49f,.70f}[sample];
            RasterPainter leaving=new RasterPainter(w,h,ss);Renderer.draw(leaving,c,L);
            Png.write(new File(dir,"214-wardrobe-exit-"+sample+".png"),leaving.resolve(),w,h);
        }
        c.closeStory();c.openStory();c.storyT=2;
        for(int skin=0;skin<Skins.COUNT;skin++) {
            RasterPainter canvas=new RasterPainter(w,h,ss);canvas.clear(0xFF211D36);
            final Painter p=Skins.painter(canvas,skin);
            p.text("A LITTLE MORE YOU",w*.5f,h*.055f,w*.052f,0xFFFFDF9A,Painter.CENTER,true);
            for(int item=0;item<Accessories.COUNT;item++) {
                final int selected=item;
                Painter outfit=new OpacityPainter(p,1) {
                    public int accessory(int who,int slot) {return slot==Accessories.slot(selected)?selected:-1;}
                };
                float x=w*(.18f+(item%3)*.32f),y=h*(.12f+(item/3)*(.80f/((Accessories.COUNT+2)/3))),r=w*.08f;
                Trinket.drawReacting(outfit,0,x,y,r,1.2f,1,0,0);
                p.text(Accessories.NAME[item],x,y+r*1.75f,w*.025f,Accessories.COLOR[item],Painter.CENTER,true);
            }
            Png.write(new File(dir,"214-accessory-sheet-"+skin+".png"),canvas.resolve(),w,h);
        }
        for(int slot:new int[]{Accessories.MASK,Accessories.BOOTS,Accessories.FLAIR}) {
            RasterPainter review=new RasterPainter(w,h,ss);review.clear(0xFF211D36);
            review.text(slot==Accessories.MASK?"PICK YOUR SECRET IDENTITY":slot==Accessories.FLAIR?"A LITTLE EXTRA MAGIC":"HAPPY FEET",w*.5f,h*.045f,w*.04f,0xFFFFDF9A,Painter.CENTER,true);
            int[] friends={0,Collect.bossPrize(Boss.MUSHROOM),Collect.DUCK_FIRST};
            for(int v=0;v<Accessories.count(slot);v++) {
                final int item=Accessories.item(slot,v);
                Painter dressed=new OpacityPainter(review,1) {
                    public int accessory(int who,int category) {return category==slot?item:-1;}
                };
                float row=.86f/Accessories.count(slot),yy=h*(.12f+v*row);
                for(int col=0;col<3;col++)Trinket.drawReacting(dressed,friends[col],w*(.18f+col*.32f),yy,Math.min(w*.11f,h*row*.32f),1.5f,1,0,0);
                review.text(Accessories.NAME[item],w*.5f,yy+h*row*.39f,w*.028f,Accessories.COLOR[item],Painter.CENTER,true);
            }
            Png.write(new File(dir,"214-"+(slot==Accessories.MASK?"masks":slot==Accessories.FLAIR?"flair":"footwear")+".png"),review.resolve(),w,h);
        }
        c.wardrobe.screen.show(c,0);c.wardrobe.screen.trayT=1;
        c.wardrobe.equip(c,Accessories.item(Accessories.GLASSES,3),0);
        c.wardrobe.screen.slot=Accessories.GLASSES;c.wardrobe.screen.selected=Accessories.item(Accessories.GLASSES,3);c.wardrobe.screen.scroll=c.wardrobe.screen.scrollTarget=1;
        RasterPainter googly=new RasterPainter(w,h,ss);Renderer.draw(googly,c,L);
        Png.write(new File(dir,"214-googly-editor.png"),googly.resolve(),w,h);
        c.wardrobe.equip(c,AccessoryGlitter.ITEM,0);
        c.wardrobe.screen.slot=Accessories.FLAIR;c.wardrobe.screen.selected=AccessoryGlitter.ITEM;
        RasterPainter glitter=new RasterPainter(w,h,ss);Renderer.draw(glitter,c,L);
        Png.write(new File(dir,"214-glitter-editor.png"),glitter.resolve(),w,h);
        c.wardrobe.screen.show(c,0);c.wardrobe.screen.trayT=1;c.wardrobe.screen.slot=Accessories.HAT;
        c.wardrobe.equip(c,Accessories.item(Accessories.HAT,2),0);
        RasterPainter crown=new RasterPainter(w,h,ss);Renderer.draw(crown,c,L);
        Png.write(new File(dir,"214-crown-editor.png"),crown.resolve(),w,h);
        c.wardrobe.screen.show(c,0);c.wardrobe.screen.trayT=1;c.wardrobe.screen.slot=Accessories.MASK;c.wardrobe.screen.selected=Accessories.item(Accessories.MASK,3);c.wardrobe.equip(c,c.wardrobe.screen.selected,0);
        RasterPainter masks=new RasterPainter(w,h,ss);Renderer.draw(masks,c,L);
        Png.write(new File(dir,"214-mask-editor.png"),masks.resolve(),w,h);
        c.wardrobe.screen.slot=Accessories.FLAIR;c.wardrobe.screen.selected=Accessories.item(Accessories.FLAIR,0);
        RasterPainter flairEditor=new RasterPainter(w,h,ss);Renderer.draw(flairEditor,c,L);
        Png.write(new File(dir,"214-flair-editor.png"),flairEditor.resolve(),w,h);
        c.wardrobe.screen.choose(c,20);c.wardrobe.screen.danceT=.10f;
        RasterPainter flash=new RasterPainter(w,h,ss);Renderer.draw(flash,c,L);
        Png.write(new File(dir,"214-inline-flash.png"),flash.resolve(),w,h);
        c.wardrobe.screen.danceT=.8f;
        RasterPainter dance=new RasterPainter(w,h,ss);Renderer.draw(dance,c,L);
        Png.write(new File(dir,"214-inline-dance.png"),dance.resolve(),w,h);
        c.wardrobe.screen.touch(c,L,0,7,WardrobeToybox.itemX(c.wardrobe.screen,L,4,1),WardrobeToybox.itemY(c.wardrobe.screen,L,4,1));
        c.wardrobe.screen.touch(c,L,2,7,L.w*.65f,L.h*.4f);
        if(!c.wardrobe.screen.dragging)throw new AssertionError("preview drag did not start");
        RasterPainter drag=new RasterPainter(w,h,ss);Renderer.draw(drag,c,L);
        Png.write(new File(dir,"214-inline-drag.png"),drag.resolve(),w,h);
        c.wardrobe.screen.cancelTouch();
        WardrobeScreen toy=c.wardrobe.screen;toy.choose(c,10+Accessories.GLASSES);toy.trayT=.30f;
        RasterPainter entry=new RasterPainter(w,h,ss);Renderer.draw(entry,c,L);
        Png.write(new File(dir,"214-toybox-arrival.png"),entry.resolve(),w,h);
        toy.trayT=1;toy.scroll=toy.scrollTarget=0;
        float tx=WardrobeToybox.itemX(toy,L,Accessories.count(toy.slot),1),ty=WardrobeToybox.itemY(toy,L,Accessories.count(toy.slot),1);
        toy.touch(c,L,0,9,tx,ty);toy.touch(c,L,2,9,tx-L.w*.16f,ty);
        RasterPainter swipe=new RasterPainter(w,h,ss);Renderer.draw(swipe,c,L);
        Png.write(new File(dir,"214-toybox-swoosh.png"),swipe.resolve(),w,h);
        toy.touch(c,L,1,9,tx-L.w*.16f,ty);toy.update(c,1);
        toy.shake(c);toy.update(c,.48f);toy.danceT=.65f;
        RasterPainter shuffle=new RasterPainter(w,h,ss);Renderer.draw(shuffle,c,L);
        Png.write(new File(dir,"214-toybox-shuffle.png"),shuffle.resolve(),w,h);
        toy.cancelTouch();
        toy.danceT=3;toy.scrambleT=2;toy.picking.capture(c,L);
        float[] hat=toy.picking.targets[Accessories.HAT];
        float hatX=(hat[0]+hat[4])*.5f,hatY=(hat[1]+hat[5])*.5f;
        toy.touch(c,L,0,7,hatX,hatY);toy.touch(c,L,2,7,L.w*.87f,Storybook.heroY(c,L)+L.w*.20f);
        if(!toy.dragging)throw new AssertionError("worn accessory drag did not start");
        RasterPainter lifted=new RasterPainter(w,h,ss);Renderer.draw(lifted,c,L);
        Png.write(new File(dir,"214-worn-drag.png"),lifted.resolve(),w,h);
        toy.cancelTouch();
        for(int who:new int[]{Collect.DUCK_FIRST,Collect.bossPrize(Boss.MUSHROOM),Collect.ALLIGATOR_FRIEND,Collect.RAINBOW_CAT_FRIEND}) {
            c.closeStory();c.collected=Collect.add(c.collected,who);c.caseIndex=who;c.openStory();c.storyT=2;c.wardrobe.screen.trayT=1;c.storyPulse=2;
            for(int category=0;category<Accessories.SLOTS;category++)c.wardrobe.equip(c,Accessories.item(category,0),who);
            RasterPainter family=new RasterPainter(w,h,ss);Renderer.draw(family,c,L);
            Png.write(new File(dir,"214-picker-family-"+who+".png"),family.resolve(),w,h);
        }
        int[] cast={0,4,7,12,18,24,30,35,45,46,47,48,49,57,60,61};
        RasterPainter canvas=new RasterPainter(w,h,ss);canvas.clear(0xFF211D36);
        Painter p=new OpacityPainter(canvas,1) {
            public int accessory(int who,int slot) {return Accessories.item(slot,who%Accessories.count(slot));}
        };
        p.text("MIX, MATCH, MAKE IT YOURS",w*.5f,h*.04f,w*.04f,0xFFFFDF9A,Painter.CENTER,true);
        for(int k=0;k<cast.length;k++) {
            float x=w*(.16f+(k%4)*.225f),y=h*(.19f+(k/4)*.225f),r=w*.077f;
            Trinket.drawReacting(p,cast[k],x,y,r,1.3f,1,0,0);
            p.text(Collect.NAME[cast[k]],x,y+r*1.9f,w*.020f,0xFFE9E2FF,Painter.CENTER,true);
        }
        Png.write(new File(dir,"214-outfit-families.png"),canvas.resolve(),w,h);
    }
}
