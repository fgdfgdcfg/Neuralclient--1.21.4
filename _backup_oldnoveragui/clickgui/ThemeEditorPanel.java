package fun.neural.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import fun.neural.Neural;
import fun.neural.api.storages.implement.ThemeStorage;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.api.utils.animation.AnimationUtils;
import fun.neural.api.utils.animation.Easings;
import fun.neural.api.utils.color.ColorUtils;
import fun.neural.api.utils.math.HoveringUtils;
import fun.neural.api.utils.render.RenderUtils;
import fun.neural.api.utils.render.fonts.msdf.Font;
import fun.neural.api.utils.render.fonts.msdf.Fonts;
import fun.neural.client.modules.impl.render.Interface;

import java.util.List;

public class ThemeEditorPanel {
    public static final float PANEL_W = 200f;
    public static final float PANEL_H = 268f;
    private static final float PAD = 12f;
    private static final float SLIDER_H = 8f;
    private static final float BOX = 16f;
    private static final float BOX_GAP = 5f;
    private static final int PRESET_COLS = 8;
    private static final float SCREEN_MARGIN = 6f;

    private final AnimationUtils anim = new AnimationUtils(0f, 14f, Easings.CUBIC_OUT);
    private boolean open;
    private boolean colorPickerOpen;

    private float lastPanelX;
    private float lastPanelY;

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        open = !open;
        if (!open) {
            colorPickerOpen = false;
        }
    }

    public void close() {
        open = false;
        colorPickerOpen = false;
    }

    private float panelX(float progress, float windowW) {
        return SCREEN_MARGIN - PANEL_W + PANEL_W * progress;
    }

    private float panelY(float windowH) {
        return (windowH - PANEL_H) / 2f;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float alphaMul) {
        anim.update(open ? 1f : 0f);
        float progress = anim.getValue();
        if (progress < 0.01f) return;

        float windowW = window.getScaledWidth();
        float windowH = window.getScaledHeight();
        float panelX = panelX(progress, windowW);
        float panelY = panelY(windowH);
        lastPanelX = panelX;
        lastPanelY = panelY;

        int panelBg = ColorUtils.applyAlpha(ColorUtils.rgba(16, 16, 22, 255), alphaMul);
        int borderR = ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 22), alphaMul);
        int textWhite = ColorUtils.applyAlpha(-1, alphaMul);
        int textDim = ColorUtils.applyAlpha(ColorUtils.rgba(160, 160, 172, 255), alphaMul);

        RenderUtils.drawShadow(context.getMatrices(), panelX, panelY + 4f, PANEL_W, PANEL_H, 10f, 24f,
                ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 150), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, PANEL_W, PANEL_H, 9f, panelBg);
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), panelX - 0.5f, panelY - 0.5f,
                PANEL_W + 1f, PANEL_H + 1f, 9.5f, 9.5f, 9.5f, 9.5f, 0.5f, borderR);

        int themeCol = getThemeColor();

        Font titleFont = Fonts.getFont("suisse", 15);
        titleFont.draw(context.getMatrices(), "Редактор тем", panelX + PAD, panelY + 13f, textWhite);
        RenderUtils.drawGradientRect(context.getMatrices(), panelX + PAD, panelY + 30f, 26f, 2f, 1f,
                ColorUtils.applyAlpha(themeCol, alphaMul), ColorUtils.applyAlpha(ColorUtils.darken(themeCol, 0.55f), alphaMul), true);

        if (!colorPickerOpen) {
            renderPresets(context, panelX, panelY, alphaMul, mouseX, mouseY, textDim);
        } else {
            renderColorPicker(context, panelX, panelY, alphaMul, mouseX, mouseY, textDim, themeCol);
        }
    }

    private void renderPresets(DrawContext context, float panelX, float panelY, float alphaMul, int mouseX, int mouseY, int textDim) {
        List<ThemeStorage.Themes> themes = Neural.INSTANCE.themeStorage.getThemeList();
        ThemeStorage.Themes selected = Neural.INSTANCE.themeStorage.getThemes();
        if (themes != null && !themes.isEmpty()) {
            float gridW = PRESET_COLS * BOX + (PRESET_COLS - 1) * BOX_GAP;
            float startX = panelX + (PANEL_W - gridW) / 2f;
            float startY = panelY + 40f;
            for (int i = 0; i < themes.size(); i++) {
                int col = i % PRESET_COLS;
                int row = i / PRESET_COLS;
                float bx = startX + col * (BOX + BOX_GAP);
                float by = startY + row * (BOX + BOX_GAP);
                ThemeStorage.Themes t = themes.get(i);
                int disp = getDisplayColor(t);
                boolean sel = t == selected;
                boolean hov = HoveringUtils.isHovered(mouseX, mouseY, bx - 2f, by - 2f, BOX + 4f, BOX + 4f);
                if (sel) {
                    RenderUtils.drawRoundedRect(context.getMatrices(), bx - 2f, by - 2f, BOX + 4f, BOX + 4f, 5f,
                            ColorUtils.applyAlpha(-1, Math.max(0.6f, alphaMul)));
                } else if (hov) {
                    RenderUtils.drawRoundedRect(context.getMatrices(), bx - 1.5f, by - 1.5f, BOX + 3f, BOX + 3f, 4.5f,
                            ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 40), alphaMul));
                }
                RenderUtils.drawRoundedRect(context.getMatrices(), bx, by, BOX, BOX, 3.5f,
                        ColorUtils.applyAlpha(disp, Math.max(0.7f, alphaMul)));
            }
            int rows = (int) Math.ceil((double) themes.size() / PRESET_COLS);
            float btnY = startY + rows * (BOX + BOX_GAP) + 10f;

            int customR = (int) Interface.INSTANCE.customR.get();
            int customG = (int) Interface.INSTANCE.customG.get();
            int customB = (int) Interface.INSTANCE.customB.get();
            int customColor = ColorUtils.rgba(customR, customG, customB, 255);
            boolean customSel = selected == ThemeStorage.Themes.Custom;
            boolean btnHov = HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, btnY, PANEL_W - PAD * 2f, 22f);
            int btnBg = btnHov ? ColorUtils.rgba(32, 32, 42, 255) : ColorUtils.rgba(24, 24, 32, 255);
            RenderUtils.drawRoundedRect(context.getMatrices(), panelX + PAD, btnY, PANEL_W - PAD * 2f, 22f, 5f,
                    ColorUtils.applyAlpha(btnBg, alphaMul));
            if (customSel) {
                RenderUtils.drawRoundedRectOutline(context.getMatrices(), panelX + PAD - 0.5f, btnY - 0.5f, PANEL_W - PAD * 2f + 1f, 23f, 5.5f, 5.5f, 5.5f, 5.5f, 1f,
                        ColorUtils.applyAlpha(-1, Math.max(0.6f, alphaMul)));
            }
            RenderUtils.drawRoundedRect(context.getMatrices(), panelX + PAD + 5f, btnY + 5f, 12f, 12f, 3f,
                    ColorUtils.applyAlpha(customColor, alphaMul));
            Fonts.getFont("suisse", 11).draw(context.getMatrices(), "Свой цвет", panelX + PAD + 23f, btnY + 7f,
                    customSel ? ColorUtils.applyAlpha(-1, alphaMul) : ColorUtils.applyAlpha(ColorUtils.rgba(205, 205, 215, 255), alphaMul));

            renderOpacity(context, panelX, panelY, alphaMul, mouseX, mouseY, textDim);
        }
    }

    private void renderColorPicker(DrawContext context, float panelX, float panelY, float alphaMul, int mouseX, int mouseY, int textDim, int themeCol) {
        boolean backHov = HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + 38f, 70f, 16f);
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX + PAD, panelY + 38f, 70f, 16f, 4f,
                ColorUtils.applyAlpha(backHov ? ColorUtils.rgba(32, 32, 42, 255) : ColorUtils.rgba(24, 24, 32, 255), alphaMul));
        Fonts.getFont("suisse", 10).draw(context.getMatrices(), "← Назад", panelX + PAD + 8f, panelY + 42f, textDim);

        int customR = (int) Interface.INSTANCE.customR.get();
        int customG = (int) Interface.INSTANCE.customG.get();
        int customB = (int) Interface.INSTANCE.customB.get();
        int customColor = ColorUtils.rgba(customR, customG, customB, 255);

        float prevX = panelX + PAD;
        float prevY = panelY + 62f;
        float prevW = PANEL_W - PAD * 2f;
        float prevH = 46f;
        RenderUtils.drawRoundedRect(context.getMatrices(), prevX, prevY, prevW, prevH, 5f,
                ColorUtils.applyAlpha(customColor, alphaMul));
        String hex = String.format("#%02X%02X%02X", customR, customG, customB);
        Fonts.getFont("suisse", 12).drawCenteredString(context.getMatrices(), hex, prevX + prevW / 2f, prevY + prevH / 2f - 4f,
                ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 230), alphaMul));

        float sliderW = PANEL_W - PAD * 2f;
        float baseY = panelY + 122f;
        String[] labels = {"R", "G", "B"};
        int[] vals = {customR, customG, customB};
        for (int s = 0; s < 3; s++) {
            float sy = baseY + s * 30f;
            Fonts.getFont("suisse", 10).draw(context.getMatrices(), labels[s], panelX + PAD, sy - 2f, textDim);
            int bg = ColorUtils.applyAlpha(ColorUtils.rgba(30, 30, 38, 255), alphaMul);
            RenderUtils.drawRoundedRect(context.getMatrices(), panelX + PAD, sy + 8f, sliderW, SLIDER_H, 4f, bg);
            float fill = sliderW * MathHelper.clamp(vals[s] / 255f, 0f, 1f);
            if (fill > 1f) {
                RenderUtils.drawRoundedRect(context.getMatrices(), panelX + PAD, sy + 8f, fill, SLIDER_H, 4f,
                        ColorUtils.applyAlpha(themeCol, (int) (220 * alphaMul)));
            }
            RenderUtils.drawRoundCircle(context.getMatrices(), panelX + PAD + fill, sy + 8f + SLIDER_H / 2f, 5.5f,
                    ColorUtils.applyAlpha(-1, Math.max(0.8f, alphaMul)));
        }

        renderOpacity(context, panelX, panelY, alphaMul, mouseX, mouseY, textDim);
    }

    private void renderOpacity(DrawContext context, float panelX, float panelY, float alphaMul, int mouseX, int mouseY, int textDim) {
        float opW = PANEL_W - PAD * 2f;
        float opX = panelX + PAD;
        float y = panelY + PANEL_H - 34f;
        float guiOp = MathHelper.clamp(ModuleClass.interfaceModule != null ? ModuleClass.interfaceModule.guiOpacity.get() : 1f, 0.1f, 1f);
        Fonts.getFont("suisse", 10).draw(context.getMatrices(), "Прозрачность", opX, y, textDim);
        float valX = panelX + PANEL_W - PAD - Fonts.getFont("suisse", 10).getWidth(String.format("%.0f%%", guiOp * 100f));
        Fonts.getFont("suisse", 10).draw(context.getMatrices(), String.format("%.0f%%", guiOp * 100f), valX, y, textDim);
        y += 11f;
        int opBg = ColorUtils.applyAlpha(ColorUtils.rgba(30, 30, 38, 255), alphaMul);
        RenderUtils.drawRoundedRect(context.getMatrices(), opX, y, opW, SLIDER_H, 4f, opBg);
        float opFill = opW * MathHelper.clamp((guiOp - 0.1f) / 0.9f, 0f, 1f);
        if (opFill > 1f) {
            RenderUtils.drawRoundedRect(context.getMatrices(), opX, y, opFill, SLIDER_H, 4f,
                    ColorUtils.applyAlpha(getThemeColor(), (int) (220 * alphaMul)));
        }
        RenderUtils.drawRoundCircle(context.getMatrices(), opX + opFill, y + SLIDER_H / 2f, 5.5f,
                ColorUtils.applyAlpha(-1, Math.max(0.8f, alphaMul)));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        if (anim.getValue() < 0.05f || button != 0) return false;
        float windowW = window.getScaledWidth();
        float windowH = window.getScaledHeight();
        float panelX = panelX(anim.getValue(), windowW);
        float panelY = panelY(windowH);
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, panelY, PANEL_W, PANEL_H)) {
            return false;
        }

        if (!colorPickerOpen) {
            List<ThemeStorage.Themes> themes = Neural.INSTANCE.themeStorage.getThemeList();
            if (themes != null && !themes.isEmpty()) {
                float gridW = PRESET_COLS * BOX + (PRESET_COLS - 1) * BOX_GAP;
                float startX = panelX + (PANEL_W - gridW) / 2f;
                float startY = panelY + 40f;
                for (int i = 0; i < themes.size(); i++) {
                    int col = i % PRESET_COLS;
                    int row = i / PRESET_COLS;
                    float bx = startX + col * (BOX + BOX_GAP);
                    float by = startY + row * (BOX + BOX_GAP);
                    if (HoveringUtils.isHovered(mouseX, mouseY, bx - 2f, by - 2f, BOX + 4f, BOX + 4f)) {
                        ThemeStorage.Themes t = themes.get(i);
                        Neural.INSTANCE.themeStorage.setThemes(t);
                        Interface.INSTANCE.themePreset.set(t.name());
                        return true;
                    }
                }
                int rows = (int) Math.ceil((double) themes.size() / PRESET_COLS);
                float btnY = startY + rows * (BOX + BOX_GAP) + 10f;
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, btnY, PANEL_W - PAD * 2f, 22f)) {
                    Interface.INSTANCE.themePreset.set("Custom");
                    Neural.INSTANCE.themeStorage.setThemes(ThemeStorage.Themes.Custom);
                    colorPickerOpen = true;
                    return true;
                }
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + PANEL_H - 34f - 2f, PANEL_W - PAD * 2f, 22f)) {
                    updateOpacity(mouseX, window);
                    return true;
                }
            }
        } else {
            if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + 38f, 70f, 16f)) {
                colorPickerOpen = false;
                return true;
            }
            float baseY = panelY + 122f;
            for (int s = 0; s < 3; s++) {
                float sy = baseY + s * 30f;
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD - 2f, sy + 6f, PANEL_W - PAD * 2f + 4f, SLIDER_H + 4f)) {
                    updateRgb(s, mouseX, panelX);
                    return true;
                }
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + PANEL_H - 34f - 2f, PANEL_W - PAD * 2f, 22f)) {
                updateOpacity(mouseX, window);
                return true;
            }
        }
        return true;
    }

    public boolean mouseReleased(int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, Window window) {
        if (button != 0 || anim.getValue() < 0.05f) return false;
        float windowW = window.getScaledWidth();
        float windowH = window.getScaledHeight();
        float panelX = panelX(anim.getValue(), windowW);
        float panelY = panelY(windowH);
        if (!colorPickerOpen) {
            if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + PANEL_H - 34f - 2f, PANEL_W - PAD * 2f, 22f)) {
                updateOpacity(mouseX, window);
            }
        } else {
            float baseY = panelY + 122f;
            for (int s = 0; s < 3; s++) {
                float sy = baseY + s * 30f;
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD - 2f, sy + 6f, PANEL_W - PAD * 2f + 4f, SLIDER_H + 4f)) {
                    updateRgb(s, mouseX, panelX);
                }
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, panelX + PAD, panelY + PANEL_H - 34f - 2f, PANEL_W - PAD * 2f, 22f)) {
                updateOpacity(mouseX, window);
            }
        }
        return false;
    }

    private void updateRgb(int channel, double mouseX, float panelX) {
        float sliderW = PANEL_W - PAD * 2f;
        float ratio = MathHelper.clamp((float) (mouseX - (panelX + PAD)) / sliderW, 0f, 1f);
        int value = Math.round(ratio * 255f);
        if (channel == 0) Interface.INSTANCE.customR.setValue(value);
        else if (channel == 1) Interface.INSTANCE.customG.setValue(value);
        else Interface.INSTANCE.customB.setValue(value);
    }

    private void updateOpacity(double mouseX, Window window) {
        float windowW = window.getScaledWidth();
        float windowH = window.getScaledHeight();
        float panelX = panelX(anim.getValue(), windowW);
        float panelY = panelY(windowH);
        float opW = PANEL_W - PAD * 2f;
        float ratio = MathHelper.clamp((float) (mouseX - (panelX + PAD)) / opW, 0f, 1f);
        float opacity = 0.1f + ratio * 0.9f;
        if (ModuleClass.interfaceModule != null) {
            ModuleClass.interfaceModule.guiOpacity.setValue(opacity);
        }
    }

    private int getThemeColor() {
        if (!Neural.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return Neural.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private int getDisplayColor(ThemeStorage.Themes theme) {
        int color = theme.getTheme().getColor(0);
        if (ColorUtils.alpha(color) == 0) {
            return ColorUtils.rgba(220, 220, 220, 180);
        }
        return color;
    }

    public boolean isInsidePanel(double mouseX, double mouseY, Window window) {
        if (anim.getValue() < 0.05f) return false;
        float windowW = window.getScaledWidth();
        float windowH = window.getScaledHeight();
        float panelX = panelX(anim.getValue(), windowW);
        float panelY = panelY(windowH);
        return HoveringUtils.isHovered(mouseX, mouseY, panelX, panelY, PANEL_W, PANEL_H);
    }

    public static void renderFooter(DrawContext context, float x, float y, float w, float h, float alphaMul, boolean hovered, int theme) {
        int bg = hovered ? ColorUtils.rgba(30, 30, 40, 255) : ColorUtils.rgba(22, 22, 30, 255);
        RenderUtils.drawRoundedRect(context.getMatrices(), x, y, w, h, 4f, ColorUtils.applyAlpha(bg, alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), x + 8f, y + h / 2f - 3f, 6f, 6f, 2f,
                ColorUtils.applyAlpha(theme, Math.max(0.7f, alphaMul)));
        Fonts.getFont("suisse", 11).draw(context.getMatrices(), "Редактор тем", x + 20f, y + h / 2f - 5.5f,
                ColorUtils.applyAlpha(hovered ? -1 : ColorUtils.rgba(190, 190, 200, 255), alphaMul));
        String chev = hovered ? ">" : ">";
        Fonts.getFont("suisse", 11).draw(context.getMatrices(), chev, x + w - 16f, y + h / 2f - 5.5f,
                ColorUtils.applyAlpha(ColorUtils.rgba(150, 150, 162, 255), alphaMul));
    }

    public static boolean isFooterHovered(float x, float y, float w, float h, double mouseX, double mouseY) {
        return HoveringUtils.isHovered(mouseX, mouseY, x, y, w, h);
    }
}
