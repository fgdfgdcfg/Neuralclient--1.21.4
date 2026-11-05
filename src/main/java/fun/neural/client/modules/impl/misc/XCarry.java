package fun.neural.client.modules.impl.misc;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventPacket;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BooleanSetting;

public class XCarry extends Module {

    public static XCarry INSTANCE = new XCarry();
    public BooleanSetting autoDisable = new BooleanSetting("Авто выкл", true);

    private boolean wasInInventory = false;

    public XCarry() {
        super("XCarry", "Дополнительные слоты", ModuleCategory.MISC);
        addSettings(autoDisable);
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof CloseHandledScreenC2SPacket && mc.currentScreen instanceof InventoryScreen) {
            event.cancel();
            wasInInventory = true;
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (wasInInventory && mc.currentScreen == null) {
            if (autoDisable.isState()) {
                toggle();
            }
            wasInInventory = false;
        }
    }
}
