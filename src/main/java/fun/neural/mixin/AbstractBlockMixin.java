package fun.neural.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.neural.api.events.Event;
import fun.neural.api.events.implement.EventBlockCollide;
import fun.neural.client.modules.impl.player.AutoTool;

import static fun.neural.api.QClient.mc;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "getOutlineShape", at = @At(value = "HEAD"), cancellable = true)
    public void getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Event event = new EventBlockCollide(pos);
        event.call();
        if (event.isCancelled()) cir.setReturnValue(VoxelShapes.empty());
    }

    @Inject(method = "getCollisionShape", at = @At(value = "HEAD"), cancellable = true)
    public void getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Event event = new EventBlockCollide(pos);
        event.call();
        if (event.isCancelled()) cir.setReturnValue(VoxelShapes.empty());
    }

    @Inject(method = "getRaycastShape", at = @At(value = "HEAD"), cancellable = true)
    public void getRaycastShape(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        Event event = new EventBlockCollide(pos);
        event.call();
        if (event.isCancelled()) cir.setReturnValue(VoxelShapes.empty());
    }

    @Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
    public void calcBlockBreakingDeltaHook(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> ci) {
        if (AutoTool.INSTANCE.isEnable() && AutoTool.INSTANCE.silent.isState()) {
            float f = state.getHardness(world, pos);
            if (f < 0.0F) {
                ci.setReturnValue(0.0f);
            } else {
                float dig_speed = getDigSpeed(state, player.getInventory().getStack(AutoTool.itemIndex)) / f;
                ci.setReturnValue(player.getInventory().getStack(AutoTool.itemIndex).isSuitableFor(state) ? dig_speed / 30.0F : dig_speed / 100.0F);
            }
        }
    }

    public float getDigSpeed(BlockState state, ItemStack stack) {
        float str = stack.getMiningSpeedMultiplier(state);

        if (str > 1.0f) {
            var efficiencyEntry = mc.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.EFFICIENCY);

            int level = EnchantmentHelper.getLevel(efficiencyEntry, stack);

            if (level > 0) {
                str += (float) (level * level + 1);
            }
        }

        return Math.max(str, 0.0f);
    }
}
