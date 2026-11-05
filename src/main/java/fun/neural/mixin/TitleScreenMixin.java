package fun.neural.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.neural.client.ui.ItemSkinsPackLoader;
import fun.neural.client.ui.alt.AltManagerScreen;
import fun.neural.client.ui.MenuBackground;
import fun.neural.client.ui.MenuLogo;
import fun.neural.client.ui.MenuShaderRenderer;
import fun.neural.client.ui.MenuTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Map;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique
    private static final String neural$TITLE = "NEURALCLIENT";
    @Unique
    private static final Identifier neural$FONT_ID = Identifier.of("neural", "title");
    @Unique
    private static TextRenderer neural$titleRenderer;

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void neural$renderCustomBackground(DrawContext context, float delta, CallbackInfo ci) {
        MenuBackground.loadIfNeeded();
        if (MenuBackground.isAvailable()) {
            MenuBackground.draw(context);
            String status = MenuBackground.getStatusText();
            if (!status.isEmpty()) {
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int sw = textRenderer.getWidth(status);
                context.drawText(textRenderer, status,
                        context.getScaledWindowWidth() - sw - 6, 6, 0x90FFFFFF, true);
            }
            ci.cancel();
            return;
        }
        if (!MenuShaderRenderer.isFailed()) {
            MenuShaderRenderer.render(context);
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void neural$clearTheme(CallbackInfo ci) {
        MenuTheme.clear();
        ItemSkinsPackLoader.tryEnable();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void neural$buildMenu(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        for (Element element : ((ScreenAccessor) this).neural$getChildren()) {
            if (element instanceof ClickableWidget widget) {
                widget.visible = false;
            }
        }

        int buttonWidth = Math.min(300, Math.round(screen.width * 0.86F));
        int buttonHeight = 22;
        int gap = 8;
        int x = screen.width / 2 - buttonWidth / 2;
        int y = screen.height / 4 + 48;

        neural$pill(Text.literal("Single Player"), b -> client.setScreen(new SelectWorldScreen(screen)), x, y, buttonWidth, buttonHeight);
        y += buttonHeight + gap;
        neural$pill(Text.literal("Multi Player"), b -> client.setScreen(new MultiplayerScreen(screen)), x, y, buttonWidth, buttonHeight);
        y += buttonHeight + gap;
        neural$pill(Text.literal("Alt Screen"), b -> client.setScreen(new AltManagerScreen(screen)), x, y, buttonWidth, buttonHeight);
        y += buttonHeight + gap + 6;
        int arrowWidth = Math.min(40, Math.round(buttonWidth * 0.16F));
        int midWidth = buttonWidth - arrowWidth * 2 - gap * 2;
        neural$pill(Text.literal("\u25c0"), b -> MenuBackground.cycle(-1), x, y, arrowWidth, buttonHeight);
        neural$pill(Text.literal("Папка фона"), b -> {
            File folder = new File(client.runDirectory, "menu-background");
            if (!folder.exists() && !folder.mkdirs()) {
                folder = client.runDirectory;
            }
            Util.getOperatingSystem().open(folder);
        }, x + arrowWidth + gap, y, midWidth, buttonHeight);
        neural$pill(Text.literal("\u25b6"), b -> MenuBackground.cycle(1),
                x + arrowWidth + gap + midWidth + gap, y, arrowWidth, buttonHeight);

        ButtonWidget exit = ButtonWidget.builder(Text.empty(), b -> client.scheduleStop())
                .dimensions(11, screen.height - 31, 20, 20).build();
        MenuTheme.registerIcon(exit, true);
        ((ScreenAccessor) this).neural$addDrawableChild(exit);

        ButtonWidget settings = ButtonWidget.builder(Text.empty(), b -> client.setScreen(new OptionsScreen(screen, client.options)))
                .dimensions(screen.width - 31, screen.height - 31, 20, 20).build();
        MenuTheme.registerIcon(settings, false);
        ((ScreenAccessor) this).neural$addDrawableChild(settings);
    }

    @Unique
    private void neural$pill(Text label, java.util.function.Consumer<ButtonWidget> action, int x, int y, int width, int height) {
        ButtonWidget button = ButtonWidget.builder(label, action::accept)
                .dimensions(x, y, width, height).build();
        MenuTheme.register(button);
        ((ScreenAccessor) this).neural$addDrawableChild(button);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void neural$drawNeuralTitle(LogoDrawer instance, DrawContext context, int width, float alpha, Operation<Void> original) {
        MenuLogo.loadIfNeeded();
        if (MenuLogo.isAvailable()) {
            MenuLogo.draw(context, width);
            return;
        }

        if (neural$titleRenderer == null) {
            try {
                FontManager fontManager = ((IMinecraftClientAccessor) MinecraftClient.getInstance()).neural$getFontManager();
                Map<Identifier, FontStorage> storages = ((FontManagerAccessor) fontManager).neural$getStorages();
                FontStorage missing = ((FontManagerAccessor) fontManager).neural$getMissing();
                neural$titleRenderer = new TextRenderer(id -> storages.getOrDefault(id, missing), false);
            } catch (Throwable t) {
                neural$titleRenderer = MinecraftClient.getInstance().textRenderer;
            }
        }

        Text title = Text.literal(neural$TITLE).styled(style -> style.withFont(neural$FONT_ID));
        TextRenderer textRenderer = neural$titleRenderer;
        int textWidth = textRenderer.getWidth(title);
        int textHeight = textRenderer.fontHeight;

        float scale = Math.min(Math.max(width / 220.0F, 1.6F), 3.0F);
        int x = -textWidth / 2;
        int y = 24;

        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F, 0.0F, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);

        for (int[] off : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            context.drawText(textRenderer, title, x + off[0], y + off[1], 0x60FFFFFF, false);
        }
        for (int[] off : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            context.drawText(textRenderer, title, x + off[0], y + off[1], 0x28FFFFFF, false);
        }
        context.drawText(textRenderer, title, x, y, 0xFFFFFFFF, true);
        context.getMatrices().pop();

        int underlineY = Math.round(y * scale + textHeight * scale) + 6;
        int underlineWidth = Math.round(textWidth * scale);
        context.fillGradient(width / 2 - underlineWidth / 2, underlineY, width / 2 + underlineWidth / 2, underlineY + 2,
                0xB0FFFFFF, 0x10FFFFFF);
    }
}
