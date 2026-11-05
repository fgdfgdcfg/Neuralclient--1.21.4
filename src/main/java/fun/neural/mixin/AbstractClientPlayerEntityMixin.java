package fun.neural.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.neural.client.modules.impl.render.SkinModel;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void neural$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures overridden = SkinModel.overrideSkin((AbstractClientPlayerEntity) (Object) this, cir.getReturnValue());
        if (overridden != null) {
            cir.setReturnValue(overridden);
        }
    }
}
