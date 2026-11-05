package fun.neural.client.modules.impl.render;

import com.mojang.authlib.GameProfile;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventAttackEntity;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class FakePlayer extends Module {

    public static FakePlayer INSTANCE = new FakePlayer();

    private final FloatSetting distance = new FloatSetting("Дистанция", 3.0f, 1.0f, 8.0f, 0.25f);
    private final BooleanSetting copySkin = new BooleanSetting("Копия скина", true);

    private OtherClientPlayerEntity fakePlayer;
    private Vec3d anchor;
    private float hitCooldown = 0f;

    public FakePlayer() {
        super("FakePlayer", "Клиентский игрок для тренировки нейро-киллауры", ModuleCategory.RENDER);
        addSettings(distance, copySkin);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        anchor = null;
        spawn();
    }

    @Override
    public void onDisable() {
        despawn();
        super.onDisable();
    }

    @EventLink
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) {
            if (fakePlayer != null) {
                despawn();
            }
            return;
        }
        if (fakePlayer == null || !fakePlayer.isAlive()
                || mc.world.getEntityById(fakePlayer.getId()) != fakePlayer) {
            despawn();
            anchor = null;
            spawn();
        }
        if (fakePlayer == null) {
            return;
        }

        // стоит на месте. точка ставится при включении и не следует за игроком
        if (anchor == null) {
            anchor = computeAnchor();
        }
        if (mc.player.getPos().distanceTo(anchor) > 16.0) {
            anchor = computeAnchor();
        }

        fakePlayer.setNoGravity(true);
        fakePlayer.setVelocity(Vec3d.ZERO);
        fakePlayer.setOnGround(true);
        if (fakePlayer.getPos().distanceTo(anchor) > 0.01) {
            snapTo(anchor);
        }

        if (hitCooldown > 0f) {
            hitCooldown--;
        }
    }

    public LivingEntity getFakeEntity() {
        return fakePlayer;
    }

    // реагируем на атаки (и рукой, и киллаурой): фейк-плеер — клиентская сущность,
    // урон через атаку не применяется, поэтому рисуем сами
    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (mc.player == null || fakePlayer == null || mc.world == null || fakePlayer.isRemoved()) {
            return;
        }
        if (event.getPlayer() != mc.player || event.getTarget() != fakePlayer) {
            return;
        }
        if (hitCooldown > 0f) {
            return;
        }
        hitCooldown = 1f;

        fakePlayer.setHeadYaw(mc.player.getYaw());
        fakePlayer.setBodyYaw(mc.player.getYaw());
        fakePlayer.animateDamage((float) Math.toDegrees(Math.atan2(
                mc.player.getZ() - fakePlayer.getZ(), mc.player.getX() - fakePlayer.getX())));
        mc.world.playSound(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.9f, false);
        double dx = fakePlayer.getX() - mc.player.getX();
        double dz = fakePlayer.getZ() - mc.player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 1.0E-4) {
            fakePlayer.takeKnockback(0.35, dx / len, dz / len);
        }

        float damage = Math.max(0.0f, fakePlayer.getHealth() - 6.0f);
        fakePlayer.setHealth(damage);
    }

    private void spawn() {
        if (mc.player == null || mc.world == null) return;
        despawn();

        GameProfile profile;
        if (copySkin.isState()) {
            GameProfile self = mc.player.getGameProfile();
            profile = new GameProfile(UUID.randomUUID(), self.getName());
            profile.getProperties().putAll(self.getProperties());
        } else {
            profile = new GameProfile(UUID.randomUUID(), "FakePlayer");
        }

        OtherClientPlayerEntity entity = new OtherClientPlayerEntity(mc.world, profile);
        entity.setId(nextFreeId());
        entity.setInvulnerable(true);
        entity.setNoGravity(true);
        entity.setOnGround(true);
        entity.setVelocity(Vec3d.ZERO);
        entity.setHealth(20.0f);

        if (anchor == null) {
            anchor = computeAnchor();
        }
        setEntityAt(entity, anchor);
        setEntityFacingPlayer(entity);

        fakePlayer = entity;
        mc.world.addEntity(entity);
    }

    private void despawn() {
        if (fakePlayer == null) return;
        if (mc != null && mc.world != null
                && mc.world.getEntityById(fakePlayer.getId()) == fakePlayer) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
    }

    private Vec3d computeAnchor() {
        float len = distance.get();
        float yaw = mc.player.getYaw();
        double rad = Math.toRadians(yaw);
        double fx = -MathHelper.sin((float) rad) * len;
        double fz = MathHelper.cos((float) rad) * len;
        Vec3d pos = mc.player.getPos();
        double x = pos.x + fx;
        double z = pos.z + fz;
        double y = findFloorY(x, z);
        return new Vec3d(x, y, z);
    }

    // ищем пол: верхнюю поверхность твёрдого блока с воздухом над ней
    private double findFloorY(double x, double z) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        int top = MathHelper.floor(mc.player.getY()) + 3;
        int bottom = top - 24;
        for (int y = top; y >= bottom; y--) {
            BlockPos at = new BlockPos(bx, y, bz);
            if (!mc.world.getBlockState(at).isAir()
                    && mc.world.getBlockState(at.up()).isAir()) {
                return y + 1.0;
            }
        }
        return mc.player.getY();
    }

    private void snapTo(Vec3d point) {
        if (fakePlayer == null) return;
        setEntityAt(fakePlayer, point);
    }

    private void setEntityAt(OtherClientPlayerEntity entity, Vec3d point) {
        entity.lastRenderX = entity.getX();
        entity.lastRenderY = entity.getY();
        entity.lastRenderZ = entity.getZ();
        entity.setPos(point.x, point.y, point.z);
        entity.updateTrackedPosition(point.x, point.y, point.z);
    }

    private void setEntityFacingPlayer(OtherClientPlayerEntity entity) {
        if (mc.player == null) return;
        double dx = mc.player.getX() - entity.getX();
        double dz = mc.player.getZ() - entity.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setBodyYaw(yaw);
        entity.setPitch(0.0f);
    }

    private int nextFreeId() {
        int candidate = 0x7000000;
        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getId() >= candidate) {
                    candidate = entity.getId() + 1;
                }
            }
        }
        return candidate;
    }
}