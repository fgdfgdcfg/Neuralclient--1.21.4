package fun.neural.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import fun.neural.client.modules.impl.render.SeeInvisiblesRenderState;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SeeInvisiblesRenderState {

    @Unique
    private boolean neural$seeInvisiblesTarget;

    @Override
    public boolean neural$isSeeInvisiblesTarget() {
        return neural$seeInvisiblesTarget;
    }

    @Override
    public void neural$setSeeInvisiblesTarget(boolean value) {
        neural$seeInvisiblesTarget = value;
    }
}
