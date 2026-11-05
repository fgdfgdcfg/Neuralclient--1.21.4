package fun.neural.client.modules.impl.movement;

import fun.neural.Neural;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.impl.combat.Aura;
import fun.neural.client.modules.settings.implement.BooleanSetting;

public class AutoJump extends Module {

    public static AutoJump INSTANCE = new AutoJump();

    public AutoJump() {
        super("AutoJump","Прыгает автоматически при ауре", ModuleCategory.MOVEMENT);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        Aura aura = ModuleClass.INSTANCE.aura;

        if (aura == null || !aura.isEnable()) return;

        if (aura.getTarget() != null) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }
}
