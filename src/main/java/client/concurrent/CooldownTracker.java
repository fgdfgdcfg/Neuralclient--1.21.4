package client.concurrent;

import client.enums.CooldownItem;

public class CooldownTracker {
   private static final CooldownItem[] cooldownItemArray = new CooldownItem[0];

   private CooldownTracker() {
   }

   public static String getText() {
      return "";
   }

   public static CooldownItem[] getCooldownItemArray() {
      return cooldownItemArray;
   }

   public static long getLongByCooldownItem(CooldownItem cooldownItem) {
      return 0L;
   }
}