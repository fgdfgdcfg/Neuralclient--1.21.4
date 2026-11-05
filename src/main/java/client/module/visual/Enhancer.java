package client.module.visual;

import client.module.Category;
import client.module.Module;

public class Enhancer extends Module {
   public static final String text8 = "Обычное";
   public static final String text9 = "Плавное";
   public static final String text10 = "Чёткое";
   public static final String text11 = "Скрытое";

   public Enhancer() {
      super("Enhancer", Category.VISUAL);
   }

   public boolean check7() {
      return false;
   }

   public boolean check9() {
      return false;
   }

   public float getFloat() {
      return 1.0F;
   }

   public String getString() {
      return "none";
   }
}