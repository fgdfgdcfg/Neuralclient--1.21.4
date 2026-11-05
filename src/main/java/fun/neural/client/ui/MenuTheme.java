package fun.neural.client.ui;

import fun.neural.client.ui.alt.AltManagerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.text.Text;

import java.util.IdentityHashMap;
import java.util.Map;

public final class MenuTheme {
    private static final Map<ClickableWidget, State> THEMED = new IdentityHashMap<>();

    public static boolean appliesTo(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof AltManagerScreen;
    }

    private static final class State {
        float hover;
        boolean icon;
        boolean danger;
    }

    private MenuTheme() {
    }

    public static void clear() {
        THEMED.clear();
    }

    public static void register(ClickableWidget widget) {
        State s = new State();
        THEMED.put(widget, s);
    }

    public static void registerIcon(ClickableWidget widget, boolean danger) {
        State s = new State();
        s.icon = true;
        s.danger = danger;
        THEMED.put(widget, s);
    }

    public static boolean isThemed(ClickableWidget widget) {
        return THEMED.containsKey(widget);
    }

    public static void draw(DrawContext context, ClickableWidget w, int mouseX, int mouseY) {
        State state = THEMED.get(w);
        if (state == null || !w.visible) {
            return;
        }
        boolean hovered = w.active && w.isMouseOver(mouseX, mouseY);
        state.hover += (hovered ? 1.0F : 0.0F - state.hover) * 0.0F;
        float target = hovered ? 1.0F : 0.0F;
        state.hover += (target - state.hover) * 0.3F;
        if (Math.abs(state.hover - target) < 0.01F) {
            state.hover = target;
        }
        float p = state.hover;

        if (state.icon) {
            drawIcon(context, w, state, p);
            return;
        }

        int x = w.getX();
        int y = w.getY() - Math.round(p);
        int width = w.getWidth();
        int height = w.getHeight();

        int bg = lerp(0x8C1E2632, 0xA62D3746, p);
        int border = lerp(0x1AFFFFFF, 0x47FFFFFF, p);

        drawPill(context, x - 1, y - 1, width + 2, height + 2, border);
        drawPill(context, x, y, width, height, bg);

        String message = w.getMessage().getString();
        if (!message.isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int textColor = lerp(0xFFE9EDF3, 0xFFFFFFFF, p);
            context.drawText(tr, message, x + width / 2 - tr.getWidth(message) / 2,
                    y + (height - tr.fontHeight) / 2 + 1, textColor, false);
        }
    }

    private static void drawIcon(DrawContext context, ClickableWidget w, State state, float p) {
        int x = w.getX();
        int y = w.getY() - Math.round(p);
        int d = w.getWidth();

        int bg = state.danger ? lerp(0x78321B1B, 0x9640221F, p) : lerp(0x8C1E2632, 0xA62D3746, p);
        int border = lerp(0x1AFFFFFF, 0x47FFFFFF, p);

        drawPill(context, x - 1, y - 1, d + 2, d + 2, border);
        drawPill(context, x, y, d, d, bg);

        int cx = x + d / 2;
        int cy = y + d / 2;

        if (state.danger) {
            int glyph = lerp(0xFFC23B3B, 0xFFE05548, p);
            context.fill(cx - 6, cy - 1, cx + 4, cy + 1, glyph);
            context.getMatrices().push();
            context.getMatrices().translate(cx + 4, cy, 0.0F);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F));
            context.fill(-1, -5, 1, 0, glyph);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
            context.fill(-1, -5, 1, 0, glyph);
            context.getMatrices().pop();
        } else {
            int glyph = lerp(0xFFAEB6C2, 0xFFFFFFFF, p);
            for (int s = -1; s <= 1; s++) {
                int ly = cy + s * 5;
                context.fill(cx - 7, ly, cx + 7, ly + 1, glyph);
            }
            context.fill(cx - 4, cy - 6, cx - 1, cy - 3, glyph);
            context.fill(cx + 2, cy - 1, cx + 5, cy + 2, glyph);
            context.fill(cx - 3, cy + 4, cx, cy + 7, glyph);
        }
    }

    public static void drawPill(DrawContext context, int x, int y, int width, int height, int argb) {
        int r = height / 2;
        if (r <= 0) {
            context.fill(x, y, x + width, y + height, argb);
            return;
        }
        context.fill(x + r, y, x + width - r, y + height, argb);
        for (int i = 0; i < r; i++) {
            double dist = i + 0.5;
            int half = (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dist * dist)));
            if (half <= 0) {
                continue;
            }
            context.fill(x + r - 1 - i, y + height / 2 - half, x + r - i, y + height / 2 + half, argb);
            context.fill(x + width - r + i, y + height / 2 - half, x + width - r + i + 1, y + height / 2 + half, argb);
        }
    }

    private static int lerp(int from, int to, float t) {
        int fa = (from >>> 24) & 0xFF;
        int fr = (from >>> 16) & 0xFF;
        int fg = (from >>> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF;
        int tr = (to >>> 16) & 0xFF;
        int tg = (to >>> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
