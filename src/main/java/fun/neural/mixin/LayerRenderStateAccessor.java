package fun.neural.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {

    @Accessor("model")
    BakedModel neural$getModel();

    @Accessor("model")
    void neural$setModel(BakedModel model);
}