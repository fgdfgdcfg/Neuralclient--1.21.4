package fun.neural.mixin;

import fun.neural.client.ui.MenuTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void neural$themeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!MenuTheme.appliesTo(MinecraftClient.getInstance().currentScreen)) {
            return;
        }
        ClickableWidget widget = (ClickableWidget) (Object) this;
        if (!MenuTheme.isThemed(widget)) {
            return;
        }
        ci.cancel();
        MenuTheme.draw(context, widget, mouseX, mouseY);
    }
}
