package fun.neural.client.modules.impl.player;

import net.minecraft.block.EnderChestBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BooleanSetting;

import java.util.ArrayList;
import java.util.List;

public class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();

    public final BooleanSetting swapBack = new BooleanSetting("SwapBack", true);
    public final BooleanSetting saveItem = new BooleanSetting("SaveItem", true);
    public final BooleanSetting silent = new BooleanSetting("Silent", false);
    public final BooleanSetting echestSilk = new BooleanSetting("EchestSilk", true);

    public static int itemIndex;
    private boolean swap;
    private long swapDelay;
    private int swingTimer;
    private final List<Integer> lastItem = new ArrayList<>();

    private AutoTool() {
        super("AutoTool", "Лучший инструмент автоматически", ModuleCategory.PLAYER);
        addSettings(swapBack, saveItem, silent, echestSilk);
    }

    @EventLink
    public void onEvent(final EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult)) return;
        BlockHitResult result = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = result.getBlockPos();
        if (mc.world.getBlockState(pos).isAir()) return;

        int tool = getTool(pos);
        boolean pressing = mc.options.attackKey.isPressed();

        if (!swap && pressing && tool != -1 && tool != mc.player.getInventory().selectedSlot) {
            lastItem.clear();
            lastItem.add(mc.player.getInventory().selectedSlot);

            mc.player.getInventory().selectedSlot = tool;
            itemIndex = tool;
            swap = true;

            swapDelay = System.currentTimeMillis();
        } else if (swap && !lastItem.isEmpty() && swapBack.isState()
                && (!pressing || System.currentTimeMillis() >= swapDelay + 300)) {
            mc.player.getInventory().selectedSlot = lastItem.get(0);
            itemIndex = lastItem.get(0);
            lastItem.clear();
            swap = false;
            swingTimer = 0;
        }

        if (swap && pressing && silent.isState()) {
            if (swingTimer++ % 2 == 0) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.0f;
        var blockState = mc.world.getBlockState(pos);

        if (blockState.isAir()) return -1;

        var enchantmentRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        var efficiencyEntry = enchantmentRegistry.getOrThrow(Enchantments.EFFICIENCY);
        var silkTouchEntry = enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH);

        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (saveItem.isState() && stack.isDamageable() && (stack.getMaxDamage() - stack.getDamage() <= 10)) {
                continue;
            }

            float digSpeed = stack.getMiningSpeedMultiplier(blockState);

            if (digSpeed > 1.0f) {
                int level = EnchantmentHelper.getLevel(efficiencyEntry, stack);
                if (level > 0) {
                    digSpeed += (float) (level * level + 1);
                }
            }

            if (blockState.getBlock() instanceof EnderChestBlock && echestSilk.isState()) {
                if (EnchantmentHelper.getLevel(silkTouchEntry, stack) > 0) {
                    if (digSpeed > currentFastest) {
                        currentFastest = digSpeed;
                        index = i;
                    }
                }
            } else {
                if (digSpeed > currentFastest) {
                    currentFastest = digSpeed;
                    index = i;
                }
            }
        }
        return index;
    }
}
