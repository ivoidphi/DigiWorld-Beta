import digiworld.core.*;
import digiworld.app.*;

public class TmpCheck {
  public static void main(String[] a){
    World w = new World("World 2 - Alpha Village",50,38,32,25,19,new Npc[]{});
    int bushes=0, iso=0;
    for(int y=0;y<w.getHeight();y++) for(int x=0;x<w.getWidth();x++){
      if(w.getTile(x,y)==TileType.GRASS_BUSH){
        bushes++;
        boolean ok=true;
        for(int oy=-3;oy<=3;oy++) for(int ox=-3;ox<=3;ox++){
          if(ox==0&&oy==0) continue;
          if(w.getTile(x+ox,y+oy)==TileType.GRASS_BUSH) ok=false;
        }
        if(ok) iso++;
      }
    }
    System.out.println("bushes="+bushes+" iso3="+iso);
  }
}
