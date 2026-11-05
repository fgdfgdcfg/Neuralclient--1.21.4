package client.util;

public final class ViewModelController {
   private static boolean value;

   private ViewModelController() {
   }

   public static void setFlag(boolean flag) {
      value = flag;
   }

   public static boolean isFlag() {
      return value;
   }
}