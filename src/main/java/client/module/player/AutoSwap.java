package client.module.player;

import client.enums.TrackedItem;
import client.module.Category;
import client.module.Module;
import client.setting.HotkeySetting;
import net.minecraft.item.ItemStack;

public class AutoSwap extends Module {
   private final HotkeySetting[] hotkeySettingArray = new HotkeySetting[0];
   private final HotkeySetting keyZaryadaVetra;

   public AutoSwap() {
      super("AutoSwap", Category.PLAYER);
      this.keyZaryadaVetra = new HotkeySetting("", "", 0, () -> {
      });
      this.keyZaryadaVetra.setName("Заряд ветра");
      this.addSettings(this.keyZaryadaVetra);
   }

   public HotkeySetting[] getHotkeySettingArray() {
      return this.hotkeySettingArray;
   }

   public HotkeySetting getKeyZaryadaVetra() {
      return this.keyZaryadaVetra;
   }

   public static TrackedItem[] getTrackedItemArray() {
      return new TrackedItem[0];
   }

   public static boolean isTrackedItemItemStack4(TrackedItem trackedItem, ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty() || trackedItem == null) {
         return false;
      }
      return trackedItem.item != null && itemStack.getItem() == trackedItem.item;
   }
}