package fun.neural.mixin;

import fun.neural.client.modules.impl.render.ItemSkins;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemSkinsMixin {

    @ModifyVariable(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack neural$modify1(ItemStack stack) {
        return ItemSkins.getRenderStack(stack);
    }

    @ModifyVariable(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack neural$modify2(ItemStack stack) {
        return ItemSkins.getRenderStack(stack);
    }

    @ModifyVariable(
            method = "updateForLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack neural$modify3(ItemStack stack) {
        return ItemSkins.getRenderStack(stack);
    }

    @ModifyVariable(
            method = "updateForNonLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack neural$modify4(ItemStack stack) {
        return ItemSkins.getRenderStack(stack);
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("RETURN")
    )
    private void neural$apply1(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean leftHand, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        ItemSkins.applySkin(state, stack);
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("RETURN")
    )
    private void neural$apply2(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        ItemSkins.applySkin(state, stack);
    }

    @Inject(
            method = "updateForLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/entity/LivingEntity;)V",
            at = @At("RETURN")
    )
    private void neural$apply3(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean leftHand, LivingEntity entity, CallbackInfo ci) {
        ItemSkins.applySkin(state, stack);
    }

    @Inject(
            method = "updateForNonLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/entity/Entity;)V",
            at = @At("RETURN")
    )
    private void neural$apply4(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, Entity entity, CallbackInfo ci) {
        ItemSkins.applySkin(state, stack);
    }
}