package fun.neural.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.neural.client.modules.impl.render.SkinModel;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void neural$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures overridden = SkinModel.overrideSkin(((PlayerListEntry) (Object) this).getProfile().getId(), cir.getReturnValue());
        if (overridden != null) {
            cir.setReturnValue(overridden);
        }
    }
}
