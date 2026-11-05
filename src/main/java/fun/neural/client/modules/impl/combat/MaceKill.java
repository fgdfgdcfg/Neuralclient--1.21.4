package fun.neural.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.lang.reflect.Field;

import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventPacket;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;

public class MaceKill extends Module {

    public static MaceKill INSTANCE = new MaceKill();
    public static boolean cancelCrit;

    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "Grim");
    private final FloatSetting height = new FloatSetting("Высота", 6.0f, 1.0f, 64.0f, 0.5f).visible(() -> mode.is("Vanilla"));
    private final BooleanSetting onlyMace = new BooleanSetting("Только булава", true);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", false);

    private Entity grimTarget;
    private int grimTimer;
    private boolean grimActive;
    private boolean internalSend;

    public MaceKill() {
        super("MaceKill", "Бонусный урон булавы от fake-падения", ModuleCategory.COMBAT);
        addSettings(mode, height, onlyMace, onlyPlayers);
    }

    @EventLink
    public void onPacketSend(EventPacket event) {
        if (event.getType() != EventPacket.Type.SEND) return;
        if (internalSend) return;
        if (!(event.getPacket() instanceof PlayerInteractEntityC2SPacket packet)) return;
        if (!isAttack(packet)) return;

        Entity ent = getEntity(packet);
        if (ent == null || ent instanceof EndCrystalEntity || cancelCrit) return;
        if (onlyPlayers.isState() && !(ent instanceof PlayerEntity)) return;
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Grim")) {
            if (tryGrim(ent)) event.cancel();
            return;
        }

        doVanillaCrit();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!grimActive || mc.player == null || mc.world == null) {
            if (grimTimer > 0) {
                grimActive = false;
                grimTarget = null;
                grimTimer = 0;
            }
            return;
        }

        grimTimer++;
        if (grimTimer > 20) {
            grimActive = false;
            grimTarget = null;
            grimTimer = 0;
            return;
        }

        if (mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 1.5f && grimTimer >= 2) {
            Entity ent = grimTarget != null ? mc.world.getEntityById(grimTarget.getId()) : null;
            grimActive = false;
            grimTarget = null;
            grimTimer = 0;
            if (ent != null) {
                internalSend = true;
                try {
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(ent, mc.player.isSneaking()));
                } finally {
                    internalSend = false;
                }
            }
        }
    }

    private boolean tryGrim(Entity target) {
        if (grimActive) return false;
        if (mc.player.isOnGround() && !mc.player.isInLava() && !mc.player.isSubmergedInWater() && !mc.player.isRiding()) {
            if (onlyMace.isState() && !isHoldingMace()) return false;
            grimTarget = target;
            grimActive = true;
            grimTimer = 0;
            mc.player.jump();
            return true;
        }
        return false;
    }

    private void doVanillaCrit() {
        if (!isEnable()) return;
        if (onlyMace.isState() && !isHoldingMace()) return;
        if ((mc.player.isOnGround() || mc.player.getAbilities().flying) && !mc.player.isInLava() && !mc.player.isSubmergedInWater() && !mc.player.isRiding()) {
            simulationFallPacket(height.get());
            simulationFallPacket(0.0);
        }
    }

    private void simulationFallPacket(double yDelta) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), false, false));
    }

    private boolean isHoldingMace() {
        return mc.player.getMainHandStack().getItem() instanceof MaceItem;
    }

    private static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        int id = getEntityId(packet);
        if (id < 0 || mc.world == null) return null;
        return mc.world.getEntityById(id);
    }

    private static int getEntityId(PlayerInteractEntityC2SPacket packet) {
        try {
            Field field;
            try {
                field = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            } catch (NoSuchFieldException ignored) {
                field = PlayerInteractEntityC2SPacket.class.getDeclaredField("field_12870");
            }
            field.setAccessible(true);
            return field.getInt(packet);
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static boolean isAttack(PlayerInteractEntityC2SPacket packet) {
        try {
            Object value = getTypeValue(packet);
            if (value == null) return true;
            Object attack = getAttackHandler();
            if (attack != null) return value == attack;
            return value.getClass().getName().endsWith("$1");
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static Object getTypeValue(PlayerInteractEntityC2SPacket packet) throws ReflectiveOperationException {
        Field field;
        try {
            field = PlayerInteractEntityC2SPacket.class.getDeclaredField("type");
        } catch (NoSuchFieldException ignored) {
            field = PlayerInteractEntityC2SPacket.class.getDeclaredField("field_12871");
        }
        field.setAccessible(true);
        return field.get(packet);
    }

    private static Object getAttackHandler() {
        try {
            Field field;
            try {
                field = PlayerInteractEntityC2SPacket.class.getDeclaredField("ATTACK");
            } catch (NoSuchFieldException ignored) {
                field = PlayerInteractEntityC2SPacket.class.getDeclaredField("field_29170");
            }
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}