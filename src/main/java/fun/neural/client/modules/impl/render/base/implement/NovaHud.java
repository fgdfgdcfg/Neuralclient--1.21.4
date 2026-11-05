package fun.neural.client.modules.impl.render.base.implement;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import fun.neural.Neural;
import fun.neural.api.events.implement.EventRender;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.api.utils.color.ColorUtils;
import fun.neural.api.utils.draggable.Draggable;
import fun.neural.api.utils.input.KeyBoardUtils;
import fun.neural.api.utils.math.MathUtils;
import fun.neural.api.utils.render.RenderUtils;
import fun.neural.api.utils.render.fonts.msdf.Font;
import fun.neural.api.utils.render.fonts.msdf.Fonts;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.impl.render.base.InterfaceProcessing;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NovaHud extends InterfaceProcessing {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Comparator<Module> WIDTH_COMPARATOR =
            Comparator.comparingDouble((Module m) -> Fonts.getFont("suisse", 14).getWidth(m.getDisplayName())).reversed();

    public NovaHud(Draggable draggable) {
        super(draggable);
    }

    private int themeColor() {
        if (!Neural.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return Neural.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        if (mc.player == null || mc.getWindow() == null) {
            draggable.setWidth(0);
            draggable.setHeight(0);
            return;
        }
        MatrixStack matrices = eventRender.getContext().getMatrices();
        int theme = themeColor();

        float[] watermarkSize = drawWatermark(matrices, theme);
        if (ModuleClass.interfaceModule.hudModules.is("Информация")) {
            drawInfoBar(matrices, theme, watermarkSize);
        }
        if (ModuleClass.interfaceModule.hudModules.is("Аррай лист")) {
            drawArrayList(matrices, theme);
        }
        if (ModuleClass.interfaceModule.hudModules.is("Горячие клавиши")) {
            drawKeyBinds(matrices, theme);
        }
        if (ModuleClass.interfaceModule.hudModules.is("Зелья")) {
            drawPotions(matrices, theme);
        }
    }

    private float[] drawWatermark(MatrixStack matrices, int theme) {
        float x = draggable.getX(), y = draggable.getY();
        Font font = Fonts.getFont("suisse", 14);
        Font smallFont = Fonts.getFont("suisse", 12);

        int fps = mc.getCurrentFps();
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        String time = LocalTime.now().format(TIME_FORMAT);
        String title = "neural";
        String stats = fps + " fps · " + ping + " ms · " + time;

        float titleW = font.getWidth(title);
        float statsW = smallFont.getWidth(stats);
        float width = 6f + 3f + 5f + titleW + 7f + statsW + 8f;
        float height = 16.5f;

        RenderUtils.drawRoundedRect(matrices, x, y, width, height, 4f, ModuleClass.interfaceModule.hudBg(ColorUtils.rgba(12, 12, 15, 205)));
        RenderUtils.drawGradientRect(matrices, x + 5f, y + 3.75f, 3f, height - 7.5f, 1.5f,
                ColorUtils.applyAlpha(theme, 255),
                ColorUtils.applyAlpha(ColorUtils.darken(theme, 0.45f), 255), true);

        font.draw(matrices, title, x + 6f + 3f + 5f, y + 5.6f, -1);
        smallFont.draw(matrices, stats, x + 6f + 3f + 5f + titleW + 7f, y + 6.1f, ColorUtils.rgba(225, 225, 232, 210));

        return new float[]{width, height};
    }

    private void drawInfoBar(MatrixStack matrices, int theme, float[] watermarkSize) {
        Font font = Fonts.getFont("suisse", 12);
        int px = (int) Math.floor(mc.player.getX());
        int py = (int) Math.floor(mc.player.getY());
        int pz = (int) Math.floor(mc.player.getZ());
        String coords = px + ", " + py + ", " + pz;
        String speed = String.format(java.util.Locale.US, "%.1f", MathUtils.calculateBPS()) + " b/s";
        String dir = directionLetter();

        float coordsW = font.getWidth(coords);
        float speedW = font.getWidth(speed);
        float dirW = font.getWidth(dir);
        float width = 7f + coordsW + 7f + speedW + 7f + dirW + 7f;
        float x = draggable.getX();
        float y = draggable.getY() + watermarkSize[1] + 3.5f;

        RenderUtils.drawRoundedRect(matrices, x, y, width, 13.5f, 4f, ModuleClass.interfaceModule.hudBg(ColorUtils.rgba(12, 12, 15, 175)));

        float cx = x + 7f;
        font.draw(matrices, coords, cx, y + 4.2f, ColorUtils.rgba(228, 228, 235, 215));
        cx += coordsW + 7f;
        font.draw(matrices, speed, cx, y + 4.2f, theme);
        cx += speedW + 7f;
        font.draw(matrices, dir, cx, y + 4.2f, ColorUtils.rgba(228, 228, 235, 215));
    }

    private String directionLetter() {
        float yaw = mc.player.getYaw() % 360f;
        if (yaw < 0) yaw += 360f;
        if (yaw >= 315f || yaw < 45f) return "юг";
        if (yaw < 135f) return "запад";
        if (yaw < 225f) return "север";
        return "восток";
    }

    private void drawArrayList(MatrixStack matrices, int theme) {
        Font font = Fonts.getFont("suisse", 14);
        List<Module> enabled = new ArrayList<>();
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            module.getArrayAnimka().update(module.isEnable() ? 1.0f : 0.0f);
            if (module.isEnable()) enabled.add(module);
        }
        enabled.sort(WIDTH_COMPARATOR);

        float scaledWidth = mc.getWindow().getScaledWidth();
        float y = 8;
        long flowSeed = System.currentTimeMillis() / 900L;

        for (int i = 0; i < enabled.size(); i++) {
            Module module = enabled.get(i);
            float anim = module.getArrayAnimka().getValue();
            String name = module.getDisplayName().toLowerCase();
            float textW = font.getWidth(name);

            float x = scaledWidth - 9f - textW;
            int shiftColor = ColorUtils.getThemeColor((int) ((flowSeed + i * 46L) % 360L));
            int textColor = i % 2 == 0
                    ? ColorUtils.rgba(240, 240, 245, (int) (235 * anim))
                    : ColorUtils.setAlphaColor(shiftColor, (int) (225 * anim));

            RenderUtils.drawRoundedRect(matrices, scaledWidth - 6.5f, y + 1.5f, 2.2f, 10.5f, 1.1f,
                    ColorUtils.setAlphaColor(shiftColor, (int) (255 * anim)));
            font.drawStringWithShadow(matrices, name, x, y, textColor);

            y += 13.5f * anim + 0.5f;
        }
    }

    private void drawKeyBinds(MatrixStack matrices, int theme) {
        Font font = Fonts.getFont("suisse", 12);
        List<Module> bound = new ArrayList<>();
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() != -1 && module.isEnable()) bound.add(module);
        }
        if (bound.isEmpty()) return;

        float scaledWidth = mc.getWindow().getScaledWidth();
        float widest = 0;
        List<String[]> lines = new ArrayList<>();
        for (Module module : bound) {
            String key = KeyBoardUtils.getKeyName(module.getKey()).toLowerCase();
            String name = module.getDisplayName().toLowerCase();
            lines.add(new String[]{name, key});
            widest = Math.max(widest, font.getWidth(name) + 4f + font.getWidth(key));
        }

        float y = mc.getWindow().getScaledHeight() - 10f - lines.size() * 12f;

        for (String[] line : lines) {
            String name = line[0];
            String key = "[" + line[1] + "]";
            float nameW = font.getWidth(name);

            font.drawStringWithShadow(matrices, key, scaledWidth - 9f - nameW - 4f - font.getWidth(key), y, theme);
            font.drawStringWithShadow(matrices, name, scaledWidth - 9f - nameW, y, ColorUtils.rgba(215, 215, 222, 200));
            y += 12f;
        }
    }

    private void drawPotions(MatrixStack matrices, int theme) {
        Font font = Fonts.getFont("suisse", 12);
        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        if (effects.isEmpty()) return;

        float scaledHeight = mc.getWindow().getScaledHeight();
        float y = scaledHeight - 8f - effects.size() * 14.5f;

        for (StatusEffectInstance effect : effects) {
            StatusEffect type = effect.getEffectType().value();
            String name = I18n.translate(type.getTranslationKey()) + levelSuffix(effect.getAmplifier());
            String timeText = effect.getDuration() < 0 ? "∞" : formatDuration(effect.getDuration());

            float nameW = font.getWidth(name);
            float timeW = font.getWidth(timeText);
            float width = 6f + nameW + 6f + timeW + 6.5f;
            float x = 9f;

            RenderUtils.drawRoundedRect(matrices, x, y, width, 12f, 3.5f, ModuleClass.interfaceModule.hudBg(ColorUtils.rgba(12, 12, 15, 180)));
            font.draw(matrices, name, x + 6f, y + 3.9f, ColorUtils.rgba(232, 232, 238, 220));
            font.draw(matrices, timeText, x + 6f + nameW + 6f, y + 3.9f, theme);

            y += 14.5f;
        }
    }

    private String levelSuffix(int amplifier) {
        switch (amplifier) {
            case 0: return "";
            case 1: return " II";
            case 2: return " III";
            case 3: return " IV";
            default: return " " + (amplifier + 1);
        }
    }

    private String formatDuration(int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
