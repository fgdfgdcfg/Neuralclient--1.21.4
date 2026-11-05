package fun.neural.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import fun.neural.Neural;
import fun.neural.api.utils.animation.AnimationUtils;
import fun.neural.api.utils.animation.Easings;
import fun.neural.api.utils.color.ColorUtils;
import fun.neural.api.utils.input.KeyBoardUtils;
import fun.neural.api.utils.math.HoveringUtils;
import fun.neural.api.utils.render.RenderUtils;
import fun.neural.api.utils.render.fonts.msdf.Font;
import fun.neural.api.utils.render.fonts.msdf.Fonts;
import fun.neural.api.utils.scissor.ScissorUtils;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.Setting;
import fun.neural.client.modules.settings.implement.BindSetting;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ListSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;
import fun.neural.client.modules.settings.implement.TextSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NovaGui {
    private static final float WINDOW_W = 384f;
    private static final float WINDOW_H = 272f;
    private static final float SIDEBAR_W = 96f;
    private static final float PAD = 10f;
    private static final float COL_W = ClickGuiLayout.WIDTH;
    private static final float COL_GAP = 6f;
    private static final int GRID_COLS = 2;
    private static final float SEARCH_W = 118f;
    private static final float SEARCH_H = 16f;

    private record SideItem(Module.ModuleCategory category, float x, float y, float w, float h) {
    }

    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer = new ClickGuiSettingRenderer();
    private final ThemeEditorPanel themeEditorPanel;
    private final Map<Module, AnimationUtils> hoverAnimations = new HashMap<>();
    private final Map<Module, AnimationUtils> toggleAnimations = new HashMap<>();
    private final Map<Module.ModuleCategory, AnimationUtils> sideAnimations = new HashMap<>();
    private final AnimationUtils tooltipFade = new AnimationUtils(0f, 10f, Easings.CUBIC_OUT);
    private Module.ModuleCategory activeCategory = Module.ModuleCategory.values()[0];
    private Module hoveredModule;

    public NovaGui(ClickGuiState state) {
        this.state = state;
        this.themeEditorPanel = state.getThemeEditor();
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
        if (window == null) return;
        float alphaMul = MathHelper.clamp(animationProgress, 0.0f, 1.0f);
        if (alphaMul <= 0.01f) return;
        float offsetY = state.getRenderOffsetY();
        float wx = windowX(window, offsetY);
        float wy = windowY(window, offsetY);
        int theme = getThemeColor();
        hoveredModule = null;

        RenderUtils.drawShadow(context.getMatrices(), wx, wy + 3f, WINDOW_W, WINDOW_H, 8f, 20f,
                ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 180), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), wx - 0.5f, wy - 0.5f, WINDOW_W + 1f, WINDOW_H + 1f, 8.5f,
                ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 16), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), wx, wy, WINDOW_W, WINDOW_H, 8f,
                ColorUtils.applyAlpha(ColorUtils.rgba(14, 14, 18, 255), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), wx, wy, SIDEBAR_W, WINDOW_H, 8f,
                ColorUtils.applyAlpha(ColorUtils.rgba(10, 10, 14, 255), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), wx + SIDEBAR_W - 8f, wy, 8f, WINDOW_H, 0f,
                ColorUtils.applyAlpha(ColorUtils.rgba(10, 10, 14, 255), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), wx + SIDEBAR_W - 0.5f, wy + 6f, 0.5f, WINDOW_H - 12f, 0f,
                ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 14), alphaMul));

        Font logoFont = Fonts.getFont("suisse", 14);
        logoFont.draw(context.getMatrices(), "neural", wx + PAD, wy + 11f,
                ColorUtils.applyAlpha(-1, alphaMul));
        float logoW = logoFont.getWidth("neural");
        RenderUtils.drawRoundCircle(context.getMatrices(), wx + PAD + logoW + 8f, wy + 16.5f, 4.5f,
                ColorUtils.applyAlpha(theme, Math.max(0.75f, alphaMul)));

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(wx, wy, SIDEBAR_W, WINDOW_H);
        for (SideItem item : getSideItems(window, offsetY)) {
            boolean active = item.category() == activeCategory;
            boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, item.x(), item.y(), item.w(), item.h());
            AnimationUtils anim = sideAnimations.computeIfAbsent(item.category(),
                    key -> new AnimationUtils(key == activeCategory ? 1f : 0f, 9f, Easings.CUBIC_OUT));
            anim.update(active ? 1f : 0f);
            float activeP = anim.getValue();

            if (hovered && !active) {
                RenderUtils.drawRoundedRect(context.getMatrices(), item.x() + 4f, item.y() + 2f,
                        item.w() - 8f, item.h() - 4f, 4f,
                        ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 10), alphaMul));
            }
            if (activeP > 0.02f) {
                RenderUtils.drawRoundedRect(context.getMatrices(), item.x() + 4f, item.y() + 2f,
                        item.w() - 8f, item.h() - 4f, 4f,
                        ColorUtils.applyAlpha(ColorUtils.setAlphaColor(theme, (int) (30 * activeP)), alphaMul));
                RenderUtils.drawGradientRect(context.getMatrices(), item.x() + 6f, item.y() + 5f, 2f, item.h() - 10f, 1f,
                        ColorUtils.applyAlpha(theme, alphaMul * activeP),
                        ColorUtils.applyAlpha(ColorUtils.darken(theme, 0.45f), alphaMul * activeP), false);
            }

            int idleColor = active ? -1 : (hovered ? ColorUtils.rgba(210, 210, 220, 255) : ColorUtils.rgba(130, 130, 142, 255));
            int iconColor = ColorUtils.interpolate(idleColor, theme, activeP);
            int nameColor = ColorUtils.interpolate(idleColor, -1, activeP);

            Font iconFont = Fonts.getFont("icon", 11);
            Font nameFont = Fonts.getFont("suisse", 11);
            float iconW = iconFont.getWidth(item.category().getIcons());
            iconFont.draw(context.getMatrices(), item.category().getIcons(), item.x() + 12f, item.y() + 5.5f,
                    ColorUtils.applyAlpha(iconColor, alphaMul));
            nameFont.draw(context.getMatrices(), item.category().getName(), item.x() + 12f + iconW + 3f, item.y() + 5.8f,
                    ColorUtils.applyAlpha(nameColor, alphaMul));
        }
        ScissorUtils.pop();

        float[] content = getContentRect(window, offsetY);

        String categoryName = activeCategory.getName();
        Fonts.getFont("suisse", 16).draw(context.getMatrices(), categoryName, content[0], wy + 12f,
                ColorUtils.applyAlpha(-1, alphaMul));
        float catNameW = Fonts.getFont("suisse", 16).getWidth(categoryName);
        Fonts.getFont("suisse", 10).draw(context.getMatrices(), String.valueOf(state.getModules(activeCategory).size()),
                content[0] + catNameW + 5f, wy + 17f,
                ColorUtils.applyAlpha(ColorUtils.rgba(140, 140, 152, 255), alphaMul));
        RenderUtils.drawGradientRect(context.getMatrices(), content[0], wy + 30f, 24f, 2f, 1f,
                ColorUtils.applyAlpha(theme, alphaMul),
                ColorUtils.applyAlpha(ColorUtils.darken(theme, 0.55f), alphaMul), true);

        renderSearchField(context, window, offsetY, alphaMul, theme);

        float listTop = wy + 42f;
        float listHeight = wy + WINDOW_H - PAD - listTop;
        state.clampScrollTotal(activeCategory, listHeight, columnMaxHeight(listHeight));

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(content[0] - 4f, listTop - 2f, content[2] + 8f, listHeight + 4f);
        List<List<Module>> columns = buildGrid();
        float gridWidth = columns.size() * COL_W + Math.max(0, columns.size() - 1) * COL_GAP;
        float colX0 = content[0] + (content[2] - gridWidth) / 2f;
        for (int ci = 0; ci < columns.size(); ci++) {
            float colX = colX0 + ci * (COL_W + COL_GAP);
            float rowY = listTop + state.getScroll(activeCategory);
            for (Module module : columns.get(ci)) {
                float openProgress = state.getOpenProgress(module);
                renderModuleRow(context, mouseX, mouseY, colX, rowY, module, openProgress, theme, alphaMul);
                rowY += ClickGuiLayout.MODULE_GAP + ClickGuiLayout.getModuleHeight(module, openProgress);
            }
        }
        ScissorUtils.pop();

        renderScrollbar(context, window, offsetY, content, listTop, listHeight, alphaMul, theme);

        themeEditorPanel.render(context, mouseX, mouseY, window, alphaMul);

        tooltipFade.update(hoveredModule != null && !themeEditorPanel.isOpen() ? 1f : 0f);
        float tooltipAlpha = tooltipFade.getValue();
        if (hoveredModule != null && tooltipAlpha > 0.01f) {
            String description = hoveredModule.getDisplayDescription();
            if (description != null && !description.isBlank() && !"NULLABLE".equalsIgnoreCase(description) && !"desc".equalsIgnoreCase(description)) {
                Font font = Fonts.getFont("suisse", 11);
                float maxWidth = window.getScaledWidth() - 60f;
                while (font.getWidth(description) > maxWidth && description.length() > 1) {
                    description = description.substring(0, description.length() - 1);
                }
                float descWidth = font.getWidth(description);
                float panelWidth = descWidth + 18f;
                float panelHeight = 15f;
                float panelX = window.getScaledWidth() / 2f - panelWidth / 2f;
                float panelY = wy + WINDOW_H + 5f;
                float tipAlpha = alphaMul * tooltipAlpha;
                int tipBg = ColorUtils.rgba(255, 255, 255, (int) (186 * tipAlpha));
                int tipBorder = ColorUtils.rgba(255, 255, 255, (int) (50 * tipAlpha));
                RenderUtils.drawShadow(context.getMatrices(), panelX, panelY, panelWidth, panelHeight, 4.5f, 10f,
                        ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, (int) (90 * tipAlpha)), tipAlpha));
                RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight, 4.5f, tipBg);
                RenderUtils.drawRoundedRectOutline(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                        4.5f, 4.5f, 4.5f, 4.5f, 0.5f, tipBorder);
                RenderUtils.drawRoundCircle(context.getMatrices(), panelX + 7f, panelY + panelHeight / 2f, 3.4f,
                        ColorUtils.applyAlpha(theme, tipAlpha));
                font.draw(context.getMatrices(), description, panelX + 12f, panelY + 4f,
                        ColorUtils.applyAlpha(ColorUtils.rgba(30, 30, 35, 255), tipAlpha));
            }
        }
    }

    private void renderModuleRow(DrawContext context, double mouseX, double mouseY, float colX, float rowY,
                                 Module module, float openProgress, int theme, float alphaMul) {
        float height = ClickGuiLayout.getModuleHeight(module, openProgress);
        boolean hoveredRow = HoveringUtils.isHovered(mouseX, mouseY, colX, rowY, COL_W, height);
        float hoverP = animValue(hoverAnimations, module, hoveredRow ? 1f : 0f);
        float toggleP = animValue(toggleAnimations, module, module.isEnable() ? 1f : 0f);

        RenderUtils.drawRoundedRect(context.getMatrices(), colX, rowY, COL_W, height, 4f,
                ColorUtils.applyAlpha(ColorUtils.rgba(18, 18, 24, 240), alphaMul));
        if (module.isEnable()) {
            RenderUtils.drawRoundedRect(context.getMatrices(), colX, rowY, COL_W, height, 4f,
                    ColorUtils.applyAlpha(ColorUtils.setAlphaColor(theme, (int) (20 * toggleP)), alphaMul));
            RenderUtils.drawRoundedRect(context.getMatrices(), colX + 2.5f, rowY + 4.5f, 2f,
                    ClickGuiLayout.MODULE_HEADER_HEIGHT - 9f, 1f,
                    ColorUtils.applyAlpha(ColorUtils.setAlphaColor(theme, (int) (255 * (0.45f + 0.55f * toggleP))), alphaMul));
        }
        if (hoverP > 0.01f) {
            RenderUtils.drawRoundedRectOutline(context.getMatrices(), colX + 0.25f, rowY + 0.25f, COL_W - 0.5f, height - 0.5f,
                    3.75f, 3.75f, 3.75f, 3.75f, 0.5f,
                    ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, (int) (30 * hoverP)), alphaMul));
        }

        String bindText = "";
        if (state.getBindingModule() == module) {
            bindText = "[...]";
        } else if (module.getKey() != -1) {
            bindText = "[" + state.toEnglish(KeyBoardUtils.getBindName(module.getKey())) + "]";
        }

        int nameColor = module.isEnable() ? -1 : ColorUtils.rgba(255, 255, 255, 170);
        nameColor = ColorUtils.interpolate(nameColor, theme, hoveredRow && !module.isEnable() ? 0.35f * hoverP : 0f);
        Fonts.getFont("suisse", 12).draw(context.getMatrices(), module.getDisplayName(),
                colX + ClickGuiLayout.SETTING_LEFT + 1f, rowY + 6.5f,
                ColorUtils.applyAlpha(nameColor, alphaMul));
        if (!bindText.isEmpty()) {
            float bindWidth = Fonts.getFont("suisse", 9).getWidth(bindText);
            Fonts.getFont("suisse", 9).draw(context.getMatrices(), bindText, colX + COL_W - 21f - bindWidth, rowY + 8f,
                    ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 135), alphaMul));
        }

        float toggleX = colX + COL_W - 18f;
        float toggleY = rowY + 6f;
        int toggleBg = ColorUtils.interpolate(ColorUtils.rgba(40, 40, 50, 255), theme, toggleP);
        RenderUtils.drawRoundedRect(context.getMatrices(), toggleX, toggleY, 14f, 8f, 4f, ColorUtils.applyAlpha(toggleBg, alphaMul));
        float knobX = MathHelper.lerp(toggleP, toggleX + 4f, toggleX + 10f);
        int knobColor = ColorUtils.interpolate(ColorUtils.rgba(215, 215, 222, 255), -1, toggleP);
        RenderUtils.drawRoundCircle(context.getMatrices(), knobX, toggleY + 4f, 6.2f, ColorUtils.applyAlpha(knobColor, alphaMul));

        if (module.isOpen() && openProgress > 0.1f && module.getSettings() != null && !module.getSettings().isEmpty()) {
            settingRenderer.render(context, module, colX, rowY, openProgress, theme, mouseX, mouseY, state);
        }

        if (module.isOpen() && openProgress > 0.1f && ClickGuiLayout.isInterfaceModule(module)) {
            float footerH = ClickGuiLayout.THEME_EDITOR_FOOTER_H;
            float footerX = colX;
            float footerY = rowY + height - footerH;
            boolean footerHov = HoveringUtils.isHovered(mouseX, mouseY, footerX, footerY, COL_W, footerH);
            ThemeEditorPanel.renderFooter(context, footerX, footerY, COL_W, footerH, alphaMul, footerHov, theme);
        }

        if (hoveredRow) {
            hoveredModule = module;
        }
    }

    private void renderSearchField(DrawContext context, Window window, float offsetY, float alphaMul, int theme) {
        float[] rect = getSearchRect(window, offsetY);
        boolean active = state.isSearchActive();
        String query = state.getSearchText();

        iconsNew(11).draw(context.getMatrices(), "l", rect[0], rect[1] + 4.8f,
                ColorUtils.applyAlpha(active ? theme : ColorUtils.rgba(150, 150, 162, 255), alphaMul));

        String text = query.isEmpty() ? "search..." : query;
        int textColor = query.isEmpty()
                ? ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 105), alphaMul)
                : ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 230), alphaMul);

        float textX = rect[0] + 13f;
        if (state.hasSearchSelection()) {
            float selX = textX + Fonts.getFont("suisse", 11).getWidth(query.substring(0, state.getSearchSelectionStart()));
            float selW = Fonts.getFont("suisse", 11).getWidth(query.substring(state.getSearchSelectionStart(), state.getSearchSelectionEnd()));
            RenderUtils.drawRoundedRect(context.getMatrices(), selX, rect[1] + 2f, selW, rect[3] - 6f, 1.5f,
                    ColorUtils.applyAlpha(ColorUtils.rgba(42, 115, 255, 155), alphaMul));
        }
        Fonts.getFont("suisse", 11).draw(context.getMatrices(), text, textX, rect[1] + 4.7f, textColor);

        float lineWidth = active ? Math.max(Fonts.getFont("suisse", 11).getWidth(text) + 2f, 26f) : 0f;
        if (lineWidth > 0f) {
            RenderUtils.drawRoundedRect(context.getMatrices(), rect[0], rect[1] + rect[3] + 1.5f, lineWidth + 13f, 1f, 0.5f,
                    ColorUtils.applyAlpha(ColorUtils.setAlphaColor(theme, (int) (200 * alphaMul)), alphaMul));
        }
        if (active && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cursorX = textX + Fonts.getFont("suisse", 11).getWidth(query.substring(0, Math.min(state.getSearchCursor(), query.length())));
            RenderUtils.drawRoundedRect(context.getMatrices(), cursorX + 1f, rect[1] + 3.7f, 0.8f, rect[3] - 7f, 0f,
                    ColorUtils.applyAlpha(theme, alphaMul));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        if (window == null) return false;
        float offsetY = state.getRenderOffsetY();
        float wx = windowX(window, offsetY);
        float wy = windowY(window, offsetY);

        if (themeEditorPanel.isOpen() && themeEditorPanel.isInsidePanel(mouseX, mouseY, window)) {
            themeEditorPanel.mouseClicked(mouseX, mouseY, button, window);
            return true;
        }

        if (state.getBindingModule() != null && button >= 2) {
            state.getBindingModule().setKey(KeyBoardUtils.createMouseBind(button));
            state.setBindingModule(null);
            return true;
        }
        if (state.getBindingSetting() != null && button >= 2) {
            state.getBindingSetting().setKey(KeyBoardUtils.createMouseBind(button));
            state.setBindingSetting(null);
            return true;
        }

        state.setEditingTextSetting(null);

        float[] search = getSearchRect(window, offsetY);
        boolean searchHovered = HoveringUtils.isHovered(mouseX, mouseY, search[0], search[1], search[2], search[3]);
        if (button == 0 && searchHovered) {
            state.setEditingTextSetting(null);
            state.setSearchActive(true);
            state.startSearchSelection(getSearchIndexAt(mouseX, search[0] + 13f));
            return true;
        }
        if (button == 0 && !searchHovered && state.isSearchActive()) {
            state.setSearchActive(false);
            state.stopSearchSelection();
        }

        if (button == 0) {
            for (SideItem item : getSideItems(window, offsetY)) {
                if (HoveringUtils.isHovered(mouseX, mouseY, item.x(), item.y(), item.w(), item.h())) {
                    activeCategory = item.category();
                    return true;
                }
            }
        }

        float[] content = getContentRect(window, offsetY);
        float listTop = wy + 42f;
        float listHeight = wy + WINDOW_H - PAD - listTop;
        if (!HoveringUtils.isHovered(mouseX, mouseY, content[0], listTop, content[2], listHeight)) {
            return false;
        }

        state.clampScrollTotal(activeCategory, listHeight, columnMaxHeight(listHeight));
        List<List<Module>> columns = buildGrid();
        float gridWidth = columns.size() * COL_W + Math.max(0, columns.size() - 1) * COL_GAP;
        float colX0 = content[0] + (content[2] - gridWidth) / 2f;
        for (int ci = 0; ci < columns.size(); ci++) {
            float colX = colX0 + ci * (COL_W + COL_GAP);
            float rowY = listTop + state.getScroll(activeCategory);
            for (Module module : columns.get(ci)) {
                float openProgress = state.getOpenProgress(module);
                float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);
                if (HoveringUtils.isHovered(mouseX, mouseY, colX, rowY, COL_W, ClickGuiLayout.MODULE_HEADER_HEIGHT)) {
                    if (button == 0) {
                        module.toggle();
                        return true;
                    }
                    if (button == 1) {
                        module.setOpen(!module.isOpen());
                        state.clampScrollTotal(activeCategory, listHeight, columnMaxHeight(listHeight));
                        return true;
                    }
                    if (button == 2) {
                        state.setBindingModule(module);
                        return true;
                    }
                    return true;
                }
                if (module.isOpen() && openProgress > 0.1f) {
                    List<Setting> settings = module.getSettings();
                    if (settings != null && handleSettingClick(mouseX, mouseY, button, colX, rowY, settings)) {
                        return true;
                    }
                }
                if (module.isOpen() && openProgress > 0.1f && ClickGuiLayout.isInterfaceModule(module)) {
                    float footerH = ClickGuiLayout.THEME_EDITOR_FOOTER_H;
                    float footerX = colX;
                    float footerY = rowY + moduleHeight - footerH;
                    if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, footerX, footerY, COL_W, footerH)) {
                        themeEditorPanel.toggle();
                        return true;
                    }
                }
                rowY += ClickGuiLayout.MODULE_GAP + moduleHeight;
            }
        }
        if (themeEditorPanel.isOpen()) {
            themeEditorPanel.close();
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        state.stopSearchSelection();
        if (button == 0) {
            themeEditorPanel.mouseReleased(button);
            for (Module module : state.getAllModules()) {
                List<Setting> settings = module.getSettings();
                if (settings == null) continue;
                for (Setting setting : settings) {
                    if (setting instanceof FloatSetting floatSetting) {
                        floatSetting.setActive(false);
                        state.endSliderDrag(floatSetting);
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, Window window) {
        if (window == null) return false;
        float offsetY = state.getRenderOffsetY();
        float wx = windowX(window, offsetY);
        float wy = windowY(window, offsetY);
        if (themeEditorPanel.isOpen() && themeEditorPanel.isInsidePanel(mouseX, mouseY, window)) {
            themeEditorPanel.mouseDragged(mouseX, mouseY, button, window);
            return true;
        }
        if (button != 0 || !state.isSearchActive() || !state.isSearchDragging()) {
            return false;
        }
        float[] search = getSearchRect(window, state.getRenderOffsetY());
        state.updateSearchSelection(getSearchIndexAt(mouseX, search[0] + 13f));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount, Window window) {
        if (window == null) return false;
        float[] content = getContentRect(window, state.getRenderOffsetY());
        float listTop = windowY(window, state.getRenderOffsetY()) + 42f;
        float listHeight = windowY(window, state.getRenderOffsetY()) + WINDOW_H - PAD - listTop;
        if (!HoveringUtils.isHovered(mouseX, mouseY, content[0], listTop, content[2], listHeight)) {
            return false;
        }
        state.addScrollTotal(activeCategory, verticalAmount, listHeight, columnMaxHeight(listHeight));
        return true;
    }

    private boolean handleSettingClick(double mouseX, double mouseY, int button, float panelX, float moduleY, List<Setting> settings) {
        float settingYoffset = ClickGuiLayout.SETTING_START_Y;
        for (Setting setting : settings) {
            if (setting == null || !setting.visible()) continue;
            float settingY = moduleY + settingYoffset + ClickGuiLayout.SETTING_PADDING;
            if (setting instanceof BooleanSetting booleanSetting) {
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + 75, settingY - 2, 16, 10)) {
                    booleanSetting.setState(!booleanSetting.isState());
                    return true;
                }
                settingYoffset += 12f;
            } else if (setting instanceof TextSetting textSetting) {
                float boxWidth = ClickGuiLayout.TEXT_SETTING_WIDTH;
                float boxX = panelX + 49f;
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, boxX, settingY - 2.5f, boxWidth, 9)) {
                    state.setSearchActive(false);
                    state.stopSearchSelection();
                    state.setEditingTextSetting(textSetting);
                    return true;
                }
                settingYoffset += 12f;
            } else if (setting instanceof FloatSetting floatSetting) {
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + ClickGuiLayout.SETTING_LEFT, settingY + 9, ClickGuiLayout.SLIDER_WIDTH, 6)) {
                    floatSetting.setActive(true);
                    floatSetting.setValue(state.getSliderValue(floatSetting, panelX + ClickGuiLayout.SETTING_LEFT, mouseX));
                    state.beginSliderDrag(floatSetting, mouseX);
                    return true;
                }
                settingYoffset += 22f;
            } else if (setting instanceof ModeSetting modeSetting) {
                float modeY = settingY + 10;
                for (String mode : modeSetting.getMods()) {
                    if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + ClickGuiLayout.SETTING_LEFT, modeY - 2, ClickGuiLayout.CLICKABLE_WIDTH, 10)) {
                        modeSetting.set(mode);
                        return true;
                    }
                    modeY += 10f;
                }
                settingYoffset += ClickGuiLayout.calculateModeSettingHeight(modeSetting);
            } else if (setting instanceof ListSetting listSetting) {
                float listY = settingY + 10;
                for (BooleanSetting entry : listSetting.getSettings()) {
                    if (!entry.visible()) continue;
                    if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + ClickGuiLayout.SETTING_LEFT, listY - 2, ClickGuiLayout.CLICKABLE_WIDTH, 10)) {
                        entry.setState(!entry.isState());
                        return true;
                    }
                    listY += 10f;
                }
                settingYoffset += ClickGuiLayout.calculateListSettingHeight(listSetting);
            } else if (setting instanceof BindSetting bindSetting) {
                String bindString = state.getBindingSetting() == bindSetting ? "..." : state.toEnglish(KeyBoardUtils.getBindName(bindSetting.getKey()));
                float bindWidth = Fonts.getFont("suisse", 11).getWidth(bindString) + 6f;
                float bindX = panelX + ClickGuiLayout.SETTING_RIGHT - bindWidth;
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, bindX, settingY - 2.5f, bindWidth, 9)) {
                    state.setBindingSetting(bindSetting);
                    return true;
                }
                settingYoffset += 12f;
            }
        }
        return false;
    }

    private float animValue(Map<Module, AnimationUtils> animations, Module module, float target) {
        AnimationUtils animation = animations.computeIfAbsent(module, key -> new AnimationUtils(target, 12f, Easings.CUBIC_OUT));
        animation.update(target);
        return animation.getValue();
    }

    private List<Module> sortedModules() {
        List<Module> modules = new ArrayList<>(state.getModules(activeCategory));
        modules.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        return modules;
    }

    private List<List<Module>> buildGrid() {
        List<List<Module>> columns = new ArrayList<>();
        for (int i = 0; i < GRID_COLS; i++) {
            columns.add(new ArrayList<>());
        }
        List<Module> modules = sortedModules();
        for (int i = 0; i < modules.size(); i++) {
            columns.get(i % GRID_COLS).add(modules.get(i));
        }
        return columns;
    }

    private float columnMaxHeight(float contentHeight) {
        float max = 0f;
        for (List<Module> column : buildGrid()) {
            float height = 0f;
            for (Module module : column) {
                height += ClickGuiLayout.MODULE_GAP + ClickGuiLayout.getModuleHeight(module, state.getOpenProgress(module));
            }
            max = Math.max(max, height);
        }
        return max;
    }

    private List<SideItem> getSideItems(Window window, float offsetY) {
        List<SideItem> items = new ArrayList<>();
        float wy = windowY(window, offsetY);
        float y = wy + 30f;
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            items.add(new SideItem(category, windowX(window, offsetY), y, SIDEBAR_W, 20f));
            y += 20f;
        }
        return items;
    }

    private float[] getSearchRect(Window window, float offsetY) {
        float wx = windowX(window, offsetY);
        float wy = windowY(window, offsetY);
        return new float[]{wx + WINDOW_W - PAD - SEARCH_W, wy + 10f, SEARCH_W, SEARCH_H};
    }

    private int getSearchIndexAt(double mouseX, float searchX) {
        String text = state.getSearchText();
        float localX = (float) mouseX - searchX;
        if (localX <= 0f || text.isEmpty()) return 0;
        Font font = Fonts.getFont("suisse", 11);
        for (int i = 1; i <= text.length(); i++) {
            float previousWidth = font.getWidth(text.substring(0, i - 1));
            float currentWidth = font.getWidth(text.substring(0, i));
            float midpoint = previousWidth + (currentWidth - previousWidth) * 0.5f;
            if (localX < midpoint) return i - 1;
        }
        return text.length();
    }

    private float windowX(Window window, float offsetY) {
        float extra = themeEditorPanel.isOpen() ? ThemeEditorPanel.PANEL_W * 0.5f : 0f;
        return (window.getScaledWidth() - WINDOW_W) / 2f + extra;
    }

    private float windowY(Window window, float offsetY) {
        return (window.getScaledHeight() - WINDOW_H) / 2f + offsetY;
    }

    private float[] getContentRect(Window window, float offsetY) {
        float wx = windowX(window, offsetY);
        float wy = windowY(window, offsetY);
        return new float[]{wx + SIDEBAR_W + 14f, wy + 38f, WINDOW_W - SIDEBAR_W - 28f, WINDOW_H - 48f};
    }

    private void renderScrollbar(DrawContext context, Window window, float offsetY, float[] content,
                                 float listTop, float listHeight, float alphaMul, int theme) {
        float totalHeight = columnMaxHeight(listHeight);
        if (totalHeight <= listHeight + 1f) return;
        float thumbHeight = Math.max(20f, listHeight * (listHeight / totalHeight));
        float scroll = state.getScroll(activeCategory);
        float maxScroll = Math.min(0f, listHeight - totalHeight);
        float progress = maxScroll >= 0f ? 0f : MathHelper.clamp(scroll / maxScroll, 0f, 1f);
        float trackX = windowX(window, offsetY) + WINDOW_W - 4.5f;
        float thumbY = listTop + (listHeight - thumbHeight) * progress;
        RenderUtils.drawRoundedRect(context.getMatrices(), trackX, listTop + 2f, 1.5f, listHeight - 4f, 0.75f,
                ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 12), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), trackX, thumbY, 1.5f, thumbHeight, 0.75f,
                ColorUtils.applyAlpha(ColorUtils.setAlphaColor(theme, (int) (255 * Math.max(0.4f, alphaMul))), alphaMul));
    }

    private int getThemeColor() {
        if (!Neural.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return Neural.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icons(int size) {
        return Fonts.getFont("icon", size);
    }

    private Font iconsNew(int size) {
        return Fonts.getFont("icon1", size);
    }
}
