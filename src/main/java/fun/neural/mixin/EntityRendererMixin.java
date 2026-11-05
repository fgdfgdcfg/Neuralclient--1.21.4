package fun.neural.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.neural.Neural;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.client.modules.impl.render.EntityESP;
import fun.neural.client.modules.impl.render.FakePlayer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void neural$renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        EntityESP esp = ModuleClass.entityESP;
        if (esp == null) return;

        if (esp.shouldHideVanillaTags()) {
            ci.cancel();
        }
    }

    // файк-плеер рендерится всегда, даже вне фрустума камеры, чтобы не "пропадал под углом"
    @Inject(method = "canBeCulled", at = @At("HEAD"), cancellable = true)
    private void neural$fakePlayerCanBeCulled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FakePlayer.INSTANCE != null && FakePlayer.INSTANCE.isEnable()
                && FakePlayer.INSTANCE.getFakeEntity() == entity) {
            cir.setReturnValue(false);
        }
    }
}
