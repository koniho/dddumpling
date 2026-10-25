package com.dddumpling.game;

final class TestCrystalField extends Check {
    static void all(Layout L) {
        CrystalField field=new CrystalField();field.prepare(L.w);
        CrystalField.Cell[] cached=field.cells;
        field.prepare(L.w);
        check("crystal geometry stays cached between frames",field.cells==cached);
        boolean covers=true,finite=true;
        // Sample across the wrapping seam too: every point must have exactly one owner.
        for(int row=0;row<61;row++)for(int col=0;col<37;col++) {
            float x=L.w*(col+.317f)/37,y=L.w*CrystalField.PERIOD*(row+.219f)/61;
            int owners=0;
            for(CrystalField.Cell cell:field.cells)for(int tile=-1;tile<=1;tile++)
                if(inside(cell.rim,x,y-tile*CrystalField.PERIOD*L.w))owners++;
            covers&=owners==1;
        }
        int vertices=0;
        for(CrystalField.Cell cell:field.cells) {
            vertices+=cell.rim.length/2;
            for(float v:cell.rim)finite&=Float.isFinite(v);
        }
        check("crystal floor covers the whole tile with no overlapping cells or seam gaps",covers);
        check("crystal geometry stays finite and bounded",finite && vertices<150);
        CrystalField duplicate=new CrystalField();duplicate.prepare(L.w);
        boolean same=true;
        for(int i=0;i<field.cells.length;i++)same&=java.util.Arrays.equals(field.cells[i].rim,duplicate.cells[i].rim);
        check("crystal geometry is deterministic",same);
        field.prepare(L.w*.75f);
        check("crystal cache adapts to a resized viewport",field.cells!=cached
                && Math.abs(field.cells[0].rim[0]-cached[0].rim[0]*.75f)<.01f);
        check("crystal scrolling wraps after a full tile",
                Math.abs(CrystalField.scroll(5,L.w)-CrystalField.scroll(5+CrystalField.PERIOD/CrystalField.SPEED,L.w))<.01f);
        GameCore c=new GameCore(new Mem(),248L);c.startGame();c.stage=6;c.landBlend=1;
        check("crystal stage uses the faceted floor",CrystalField.presence(c)==1);
        c.landFrom=1;c.stage=11;c.landBlend=.5f;
        check("crystal floor fades on land departure",Math.abs(CrystalField.presence(c)-.5f)<.001f);
        c.state=GameCore.TITLE;
        check("crystal floor does not replace the title",CrystalField.presence(c)==0);
        c.state=GameCore.PLAY;c.survival.active=true;
        check("crystal floor does not replace Survival",CrystalField.presence(c)==0);
    }
    private static boolean inside(float[] p,float x,float y) {
        boolean hit=false;
        for(int i=0,j=p.length-2;i<p.length;j=i,i+=2)
            if((p[i+1]>y)!=(p[j+1]>y) && x<(p[j]-p[i])*(y-p[i+1])/(p[j+1]-p[i+1])+p[i])hit=!hit;
        return hit;
    }
}
