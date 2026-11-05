package fun.neural.client.ui.alt;

import fun.neural.client.ui.MenuTheme;
import fun.neural.mixin.IMinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class AltManagerScreen extends Screen {
    private static final int MAX_ROWS = 6;

    private final Screen parent;
    private TextFieldWidget nameField;
    private boolean deleteMode;
    private String status = "";

    public AltManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltsStorage.load();

        int panelWidth = Math.min(300, Math.round(width * 0.86F));
        int x = width / 2 - panelWidth / 2;
        int y = height / 4 + 12;

        nameField = new TextFieldWidget(textRenderer, x, y, panelWidth - 92, 20, Text.literal("Ник аккаунта"));
        nameField.setMaxLength(16);
        addDrawableChild(nameField);

        neural$pill(Text.literal("Добавить"), b -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                AltsStorage.add(name);
                nameField.setText("");
                refresh();
            }
        }, x + panelWidth - 88, y, 88, 20);

        y += 28;

        int rows = Math.min(AltsStorage.getAlts().size(), MAX_ROWS);
        for (int i = 0; i < rows; i++) {
            String altName = AltsStorage.getAlts().get(i);
            String label = (deleteMode ? "✕ " : "") + altName;
            neural$pill(Text.literal(label), b -> {
                if (deleteMode) {
                    AltsStorage.remove(altName);
                } else {
                    switchTo(altName);
                }
                refresh();
            }, x, y, panelWidth, 20);
            y += 24;
        }

        if (AltsStorage.getAlts().size() > MAX_ROWS) {
            y += 4;
        }

        y = Math.max(y + 8, height - 56);
        neural$pill(Text.literal(deleteMode ? "Режим: удаление" : "Режим: вход"), b -> {
            deleteMode = !deleteMode;
            refresh();
        }, x, y, panelWidth / 2 - 4, 20);
        neural$pill(Text.literal("Назад"), b -> close(), x + panelWidth / 2 + 4, y, panelWidth / 2 - 4, 20);
    }

    private void neural$pill(Text label, java.util.function.Consumer<ButtonWidget> action, int x, int y, int w, int h) {
        ButtonWidget button = ButtonWidget.builder(label, action::accept).dimensions(x, y, w, h).build();
        MenuTheme.register(button);
        addDrawableChild(button);
    }

    private void switchTo(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        Session session = new Session(name, uuid, "", Optional.empty(), Optional.empty(), Session.AccountType.LEGACY);
        ((IMinecraftClientAccessor) MinecraftClient.getInstance()).setSession(session);
        status = "Сменён на: " + name;
    }

    private void refresh() {
        client.setScreen(this);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        String title = "ALT MANAGER";
        context.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, height / 4 - 14,
                0xFFE9EDF3, false);

        String current = "Сейчас: " + (client.getSession() != null ? client.getSession().getUsername() : "?");
        context.drawText(textRenderer, current, width / 2 - textRenderer.getWidth(current) / 2, height / 4 + 2,
                0x90FFFFFF, false);

        if (!status.isEmpty()) {
            context.drawText(textRenderer, status, width / 2 - textRenderer.getWidth(status) / 2,
                    height - 24, 0xFF9FDCA0, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
