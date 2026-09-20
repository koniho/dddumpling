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
                ReleaseChange.icon(p,ReleaseContent.ICONS[item],110+frame*150,y+85,48,.4f+frame*.65f);
            p.text(ReleaseContent.TITLES[item],510,y+70,25,INK,Painter.LEFT,true);
            p.text("AUTO RESET: "+(ReleaseContent.AUTO_RESET[item] ? "YES" : "NO"),510,y+110,18,INK_DIM,Painter.LEFT,false);
        }
        Png.write(new File(args[0]),p.resolve(),width,height);
    }
}
