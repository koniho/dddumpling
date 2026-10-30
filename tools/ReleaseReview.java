package com.dddumpling.game;

import java.io.File;

/** Review sheet rendered by the same icon path as the in-game release book. */
final class ReleaseReview extends Draw {
    public static void main(String[] args) throws Exception {
        int count=ReleaseContent.ITEMS[0].length, width=1000, height=120+count*230;
        RasterPainter p=new RasterPainter(width,height,2);
        p.clear(BG);
        p.text("RELEASE "+ReleaseContent.VERSIONS[0],50,65,30,INK,Painter.LEFT,true);
        for(int row=0;row<count;row++) {
            int item=ReleaseContent.ITEMS[0][row];
            float y=120+row*230;
            for(int frame=0;frame<3;frame++)
                ReleaseChange.icon(p,ReleaseContent.ICONS[item],110+frame*150,y+85,48,ReleaseChange.homes(ReleaseContent.ICONS[item])?frame*ReleaseHomes.PERIOD+1.8f:ReleaseContent.ICONS[item]==ReleaseChange.WARDROBE?.4f+frame*4f:.4f+frame*.65f);
            p.text(ReleaseContent.TITLES[item],510,y+70,25,INK,Painter.LEFT,true);
            p.text("AUTO RESET: "+(ReleaseContent.AUTO_RESET[item] ? "YES" : "NO"),510,y+110,18,INK_DIM,Painter.LEFT,false);
        }
        Png.write(new File(args[0]),p.resolve(),width,height);
        for(int row=0;row<count;row++) {
            int item=ReleaseContent.ITEMS[0][row];
            Layout layout=new Layout();layout.compute(640,1400,0,0,0,0);
            GameCore game=new GameCore(null,3500);ReleaseNotes notes=game.releaseNotes;
            notes.show(game,layout);notes.update(ReleaseTransition.DURATION,layout);notes.select(0,row,layout);
            notes.update(ReleaseNotes.PAGE_TIME,layout);
            RasterPainter popup=new RasterPainter(640,1400,2);popup.clear(BG);notes.draw(popup,game,layout);
            Png.write(new File(args[0].replace(".png","-popup-"+row+".png")),popup.resolve(),640,1400);
            notes.touch(game,layout,layout.w*.5f,(notes.demoTop(layout)+notes.demoBottom(layout))*.5f);
            notes.update(ReleaseNotes.RESTART_DELAY+.2f,layout);
            if(!ReleaseContent.AUTO_RESET[item] && (ReleaseContent.ICONS[item]==ReleaseChange.WARDROBE
                    || ReleaseMascots.active(ReleaseContent.ICONS[item])) && notes.celebration<ReleaseNotes.RESTART_DELAY)
                throw new AssertionError("Unexpected automatic reset: "+ReleaseContent.TITLES[item]);
            if(!ReleaseChange.homes(ReleaseContent.ICONS[item]))continue;
            for(int stop=0;stop<ReleaseHomes.HOSTS.length;stop++) {
                Layout L=new Layout();L.compute(640,1400,0,0,0,0);
                GameCore c=new GameCore(null,3500);ReleaseNotes n=c.releaseNotes;
                n.show(c,L);n.update(ReleaseTransition.DURATION,L);n.select(0,row,L);
                n.update(ReleaseNotes.PAGE_TIME,L);n.update(stop*ReleaseHomes.PERIOD+1.8f,L);
                RasterPainter card=new RasterPainter(640,1400,2);card.clear(BG);n.draw(card,c,L);
                String path=args[0].replace(".png","-home-"+row+"-"+stop+".png");
                Png.write(new File(path),card.resolve(),640,1400);
            }
        }
    }
}
