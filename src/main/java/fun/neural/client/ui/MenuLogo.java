package fun.neural.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public final class MenuLogo {
    private static Identifier textureId;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded;
    private static boolean checked;

    private MenuLogo() {
    }

    public static void loadIfNeeded() {
        if (checked) {
            return;
        }
        checked = true;
        MinecraftClient client = MinecraftClient.getInstance();
        for (String name : new String[]{"menu-logo.png", "menu-logo.jpg", "menu-logo.jpeg"}) {
            File file = new File(client.runDirectory, name);
            if (!file.isFile()) {
                continue;
            }
            if (tryLoad(() -> new FileInputStream(file))) {
                return;
            }
        }
        // встроенный лого, чтобы он был и на чужих лаунчерах без run-папки
        tryLoad(() -> MenuLogo.class.getResourceAsStream("/assets/neural/menu-logo.png"));
    }

    private static boolean tryLoad(InputStreamFactory factory) {
        MinecraftClient client = MinecraftClient.getInstance();
        try (InputStream in = factory.open()) {
            if (in == null) {
                return false;
            }
            NativeImage image = NativeImage.read(in);
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            textureId = Identifier.of("neural", "menu_logo");
            client.getTextureManager().registerTexture(textureId, texture);
            loaded = true;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @FunctionalInterface
    private interface InputStreamFactory {
        InputStream open() throws Exception;
    }

    public static boolean isAvailable() {
        return loaded && textureId != null;
    }

    public static void draw(DrawContext context, int windowWidth) {
        if (!isAvailable()) {
            return;
        }
        float maxWidth = windowWidth * 0.8F;
        float maxHeight = 160.0F;
        float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
        int drawWidth = Math.max(1, Math.round(imageWidth * scale));
        int drawHeight = Math.max(1, Math.round(imageHeight * scale));
        int x = windowWidth / 2 - drawWidth / 2;
        int y = 16;
        context.drawTexture(RenderLayer::getGuiTextured, textureId,
                x, y,
                0.0F, 0.0F,
                drawWidth, drawHeight,
                imageWidth, imageHeight,
                imageWidth, imageHeight);
    }
}
