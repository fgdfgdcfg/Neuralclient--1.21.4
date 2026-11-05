package client.module.client;

import client.module.Category;
import client.module.Module;

public class StreamBypass extends Module {
   private static boolean flag = true;

   public StreamBypass() {
      super("StreamBypass", Category.CLIENT);
   }

   public static boolean isFlag() {
      return flag;
   }

   public static boolean check3() {
      return false;
   }

   public static void setFlag(boolean flag2) {
      flag = flag2;
   }
}