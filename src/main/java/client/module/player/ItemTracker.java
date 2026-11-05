package client.module.player;

import client.enums.TrackedItem;
import client.module.Category;
import client.module.Module;
import net.minecraft.item.ItemStack;

public class ItemTracker extends Module {
   public ItemTracker() {
      super("ItemTracker", Category.PLAYER);
   }

   public static TrackedItem getTrackedItemByItemStack(ItemStack itemStack) {
      return null;
   }
}