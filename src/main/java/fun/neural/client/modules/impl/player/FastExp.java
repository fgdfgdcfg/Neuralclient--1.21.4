package fun.neural.client.modules.impl.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.mixin.IMinecraftClientAccessor;

public class FastExp extends Module {

    public static FastExp INSTANCE = new FastExp();

    public FastExp() {
        super("FastExp", "Позволяет бросать пузырьки опыта без задержки", ModuleCategory.PLAYER);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            ((IMinecraftClientAccessor) mc).setItemUseCooldown(0);
        }
    }
}
