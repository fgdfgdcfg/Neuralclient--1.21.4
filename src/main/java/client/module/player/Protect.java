package client.module.player;

import client.module.Category;
import client.module.Module;

public class Protect extends Module {
   public Protect() {
      super("Protect", Category.PLAYER);
   }

   public String getStringByString2(String text) {
      return text;
   }
}