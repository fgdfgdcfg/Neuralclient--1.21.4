package fun.neural.client.modules.impl.combat;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import fun.neural.Neural;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ListSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class TriggerBot extends Module {

    public static TriggerBot INSTANCE = new TriggerBot();

    // ============================================================ настройки
    private final FloatSetting attackRange = new FloatSetting("Дистанция атаки", 3f, 3f, 6f, 0.1f);
    private final FloatSetting rotateRange = new FloatSetting("Дистанция ротации", 3f, 3f, 12f, 1f);
    private final FloatSetting elytraRange = new FloatSetting("Дистанция на элитре", 14f, 8f, 36f, 1f);
    private final ModeSetting attackMode = new ModeSetting("Режим атаки", "1.9", "1.9", "1.8");
    private final FloatSetting cps = new FloatSetting("Cps (ЛКМ)", 12f, 1f, 20f, 1f).visible(() -> attackMode.is("1.8"));
    private final BooleanSetting miss = new BooleanSetting("Мисать", false);
    private final FloatSetting missChance = new FloatSetting("Шанс мисса", 10f, 1f, 100f, 1f).visible(miss::isState);

    private final ListSetting targets = new ListSetting("Кого атаковать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Модераторы", false),
            new BooleanSetting("Игроки без брони", false),
            new BooleanSetting("Мобы", true),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Невидимых и голых", true),
            new BooleanSetting("Невидимых", true)
    );

    private final ListSetting attack = new ListSetting("Атака",
            new BooleanSetting("Бить через блоки", false),
            new BooleanSetting("Бить через игроков", false),
            new BooleanSetting("Бить когда ешь", false),
            new BooleanSetting("Бить только оружием", false),
            new BooleanSetting("Только криты", true),
            new BooleanSetting("Умные криты", true),
            new BooleanSetting("Ранний удар булавой", false),
            new BooleanSetting("Рандомизация", true),
            new BooleanSetting("Сброс спринта", true),
            new BooleanSetting("Обход бить через блоки", true)
    );

    private final ListSetting extra = new ListSetting("Дополнительно",
            new BooleanSetting("Не бить в GUI", true),
            new BooleanSetting("Бить в инвентаре", false),
            new BooleanSetting("Фокус на одном", true),
            new BooleanSetting("Бить только определенную цель", false),
            new BooleanSetting("Сортировка целей", true)
    );

    private final ListSetting sortBy = new ListSetting("Сортировать по",
            new BooleanSetting("Здоровью", true),
            new BooleanSetting("Броне", true),
            new BooleanSetting("Дистанции", true),
            new BooleanSetting("Зельям", false)
    ).visible(() -> extra.is("Сортировка целей"));

    // ============================================================ состояние
    private LivingEntity target;
    private long lastAttackTime;
    private long fakeDelayUntil;

    // рандомизация кулдауна
    private int randomTargetId = -1;
    private float randomThreshold = -1.0f;
    private long randomInterval = -1L;
    private long randomReachTime = -1L;
    private long randomDelayNs = -1L;

    // сброс спринта
    private boolean sprintResetPending;

    // мод "Работник" (элитры) — не используется, резерв
    private boolean elytraMode;

    public TriggerBot() {
        super("TriggerBot", "Автоматически атакует игроков в выбранной вами дистанции", ModuleCategory.COMBAT);
        addSettings(
                attackRange, rotateRange, elytraRange,
                attackMode, cps, miss, missChance,
                targets, attack, extra, sortBy
        );
    }

    private boolean isTarget(String name) {
        return targets.is(name);
    }

    private boolean isAttack(String name) {
        return attack.is(name);
    }

    private boolean isExtra(String name) {
        return extra.is(name);
    }

    private boolean isFriend(String name) {
        return Neural.INSTANCE != null && Neural.INSTANCE.friendStorage != null && Neural.INSTANCE.friendStorage.isFriend(name);
    }

    private boolean isStaff(String name) {
        return Neural.INSTANCE != null && Neural.INSTANCE.staffStorage != null && Neural.INSTANCE.staffStorage.isStaff(name);
    }

    @Override
    public void onEnable() {
        resetRandom();
        target = null;
        fakeDelayUntil = 0L;
        sprintResetPending = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetRandom();
        target = null;
        fakeDelayUntil = 0L;
        sprintResetPending = false;
        if (mc.player != null && mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
        super.onDisable();
    }

    private void resetRandom() {
        randomTargetId = -1;
        randomThreshold = -1.0f;
        randomInterval = -1L;
        randomReachTime = -1L;
        randomDelayNs = -1L;
    }

    // ============================================================ эффективная ротация (Aura / игрок)
    private boolean auraActive() {
        return Aura.INSTANCE != null && Aura.INSTANCE.isEnable() && Aura.INSTANCE.getCurrentRotations() != null;
    }

    private float effYaw() {
        if (auraActive()) return Aura.INSTANCE.getCurrentRotations().x;
        return mc.player.getYaw();
    }

    private float effPitch() {
        if (auraActive()) return Aura.INSTANCE.getCurrentRotations().y;
        return mc.player.getPitch();
    }

    private boolean auraTargets(LivingEntity entity) {
        return Aura.INSTANCE != null
                && Aura.INSTANCE.isEnable()
                && Aura.INSTANCE.getTarget() != null
                && Aura.INSTANCE.getTarget() == entity;
    }

    // ============================================================ рейкасты
    private EntityHitResult raycastEntity(float yaw, float pitch, double distance, Predicate<Entity> filter) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eyes.add(dir.multiply(distance));
        return ProjectileUtil.raycast(mc.player, eyes, end,
                mc.player.getBoundingBox().expand(distance).stretch(dir.multiply(distance)), filter, distance * distance);
    }

    private boolean aimingAt(LivingEntity entity, float yaw, float pitch, double distance) {
        if (mc.player == null || entity == null) return false;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eyes.add(dir.multiply(distance));
        return entity.getBoundingBox().expand(0.08f, 0.22f, 0.08f).raycast(eyes, end).isPresent();
    }

    private boolean isVisible(Entity entity) {
        if (mc.world == null || mc.player == null || entity == null) return false;
        return mc.world.raycast(new RaycastContext(mc.player.getEyePos(), entity.getEyePos(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    private boolean isBlockedAhead(LivingEntity entity, float yaw, float pitch, double distance) {
        if (mc.world == null || mc.player == null || entity == null) return false;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eyes.add(dir.multiply(distance));
        BlockHitResult blockHit = mc.world.raycast(new RaycastContext(eyes, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) return false;
        return blockHit.getPos().squaredDistanceTo(eyes) <= entity.getBoundingBox().getCenter().squaredDistanceTo(eyes) + 1.0E-4;
    }

    // ============================================================ валидность цели (Hw0)
    private boolean isValid(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.age < 3) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity.isInvulnerable()) return false;
        if (entity instanceof TameableEntity tameable
                && tameable.isTamed()
                && tameable.getOwnerUuid() != null
                && tameable.getOwnerUuid().equals(mc.player.getUuid())) {
            return false;
        }
        double range = rotateRange.get() + 0.1;
        if (mc.player.distanceTo(entity) > range) return false;

        if (entity instanceof PlayerEntity player) {
            String name = player.getName().getString().trim();
            if (!isTarget("Друзья") && isFriend(name)) return false;
            if (!isTarget("Игроки")) return false;
            if (isStaff(name) && !isTarget("Модераторы")) return false;
            boolean naked = isNaked(player);
            boolean invisible = player.isInvisible();
            if (!isTarget("Игроки без брони") && naked && !(invisible && isTarget("Невидимых и голых"))) return false;
            if (invisible && !isTarget("Невидимых") && !(naked && isTarget("Невидимых и голых"))) return false;
        } else if (entity instanceof Monster || entity instanceof HostileEntity) {
            if (!isTarget("Мобы")) return false;
        } else if (entity instanceof AnimalEntity) {
            if (!isTarget("Животные")) return false;
        } else {
            return false;
        }
        return true;
    }

    private boolean isNaked(PlayerEntity player) {
        for (ItemStack stack : player.getArmorItems()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    private boolean inRange(LivingEntity entity) {
        return mc.player.distanceTo(entity) <= attackRange.get() + 0.25;
    }

    // ============================================================ выбор цели (bw + Sjh0569)
    private LivingEntity findCandidate() {
        if (mc.player == null || mc.world == null) return null;

        float vy = effYaw();
        float vp = effPitch();
        double range = attackRange.get() + 0.25;

        if (isExtra("Бить только определенную цель")) {
            LivingEntity focused = auraTargets(Aura.INSTANCE.getTarget()) ? Aura.INSTANCE.getTarget() : target;
            if (focused == null || !isValid(focused) || !inRange(focused)) return null;
            boolean aimed = aimingAt(focused, vy, vp, range) || aimingAt(focused, mc.player.getYaw(), mc.player.getPitch(), range);
            return aimed ? focused : null;
        }

        if (mc.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity living && isValid(living) && inRange(living)) {
            return living;
        }

        EntityHitResult result = raycastEntity(vy, vp, range,
                e -> e instanceof LivingEntity l && isValid(l));
        if (result != null && result.getEntity() instanceof LivingEntity living) {
            return living;
        }

        if (auraActive() && Aura.INSTANCE.getTarget() != null) {
            LivingEntity auraTarget = Aura.INSTANCE.getTarget();
            if (isValid(auraTarget) && aimingAt(auraTarget, vy, vp, range + 0.25)) {
                return auraTarget;
            }
        }

        return null;
    }

    private LivingEntity findBest() {
        if (mc.player == null || mc.world == null) return null;
        double range = rotateRange.get() + 0.1;
        List<LivingEntity> list = new ArrayList<>();
        for (Entity entity : mc.world.getEntitiesByClass(LivingEntity.class,
                mc.player.getBoundingBox().expand(range), e -> true)) {
            if (!isValid((LivingEntity) entity)) continue;
            if (!isAttack("Бить через блоки") && !isVisible(entity)) continue;
            list.add((LivingEntity) entity);
        }
        if (list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        if (isExtra("Сортировка целей")) {
            list.sort(Comparator.comparingDouble(this::sortScore));
        }
        return list.get(0);
    }

    private double sortScore(LivingEntity entity) {
        double score = 0.0;
        if (sortBy.is("Здоровью")) score += entity.getHealth();
        if (sortBy.is("Броне")) score += entity.getArmor() * 0.6;
        if (sortBy.is("Дистанции")) score += mc.player.distanceTo(entity);
        if (sortBy.is("Зельям")) score += (entity.getActiveStatusEffects().isEmpty() ? 0.0 : -8.0);
        return score + mc.player.distanceTo(entity) * 0.001;
    }

    // ============================================================ главный тик
    @EventLink
    public void onTick(EventUpdate event) {
        if (mc == null || mc.player == null || mc.world == null || mc.interactionManager == null) {
            clearState();
            return;
        }

        if (!isEnable()) {
            clearState();
            return;
        }

        if (mc.player.isDead() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0F) {
            setEnabled(false);
            return;
        }

        boolean inGui = mc.currentScreen != null;
        if (isExtra("Не бить в GUI") && inGui) {
            boolean inventoryOk = isExtra("Бить в инвентаре") && mc.currentScreen instanceof HandledScreen<?>;
            if (!inventoryOk) {
                clearState();
                return;
            }
        }

        boolean focused = isExtra("Фокус на одном") || isExtra("Бить только определенную цель");
        if (focused) {
            if (target == null || !isValid(target) || !inRange(target)) {
                target = findBest();
            }
        } else {
            target = null;
        }

        if (attackMode.is("1.9") && critReady()) {
            tryAttack();
        } else if (attackMode.is("1.8")) {
            tryAttack();
        }
    }

    private void clearState() {
        target = null;
        fakeDelayUntil = 0L;
        resetRandom();
    }

    // ============================================================ атака (Sxj4457 + Nwi0)
    private void tryAttack() {
        long now = System.currentTimeMillis();
        if (now < fakeDelayUntil) return;

        LivingEntity candidate = findCandidate();
        if (candidate == null) return;
        target = candidate;

        if (!canBeHit(target)) return;

        if (!isAttack("Бить когда ешь") && mc.player.isUsingItem()) return;
        if (isAttack("Бить только оружием") && !isMeleeWeapon()) return;
        if (!isAttack("Бить через блоки") && !isVisible(target)) return;
        if (mc.player.distanceTo(target) > attackRange.get()) return;

        if (attackMode.is("1.9") && usingMace() && !maceReady()) return;
        if (attackMode.is("1.9") && !isSwordAxeTrident() && now - lastAttackTime < 520L) return;
        if (attackMode.is("1.9") && isAttack("Умные криты") && jumpIntent() && !canCriticalFall()) {
            resetRandom();
            return;
        }
        if (attackMode.is("1.9") && isAttack("Только криты") && !critReady()) {
            resetRandom();
            return;
        }
        if (attackMode.is("1.9") && !cooldownReady(target)) return;

        if (isAttack("Сброс спринта") && mc.player.isSprinting()
                && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            doSprintReset();
            return;
        }

        if (attackMode.is("1.8")) {
            if (cpsReady(now)) {
                doAttack(now);
            }
        } else {
            doAttack(now);
        }
    }

    private void doAttack(long now) {
        if (!canBeHit(target)) return;
        if (attackMode.is("1.9") && usingMace() && !maceReady()) return;

        mc.player.setSprinting(false);
        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }

        if (shouldMiss()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            fakeDelayUntil = now + ThreadLocalRandom.current().nextLong(360L, 600L);
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = now;
        resetRandom();
    }

    private void doSprintReset() {
        mc.player.setSprinting(false);
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        sprintResetPending = true;
    }

    // ============================================================ проверки
    private boolean canBeHit(LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null) return false;
        float vy = effYaw();
        float vp = effPitch();
        double d = attackRange.get() + 0.03;

        EntityHitResult first = raycastEntity(mc.player.getYaw(), mc.player.getPitch(), d,
                e -> e == entity && !e.isSpectator() && e.canBeHitByProjectile());
        if (first == null || first.getEntity() != entity) return false;

        EntityHitResult second = raycastEntity(vy, vp, d,
                e -> e == entity && !e.isSpectator() && e.canBeHitByProjectile());
        if (second == null || second.getEntity() != entity) return false;

        if (!isAttack("Бить через блоки") && isBlockedAhead(entity, vy, vp, d)) {
            return false;
        }
        return true;
    }

    private boolean cooldownReady(LivingEntity entity) {
        float f = mc.player.getAttackCooldownProgress(0.5f);
        if (usingMace()) return f >= maceThreshold();
        if (!isAttack("Рандомизация")) return f >= 0.9f;

        int id = entity.getId();
        if (randomTargetId != id || randomThreshold < 0.0f) {
            randomTargetId = id;
            randomThreshold = randomThreshold();
            randomReachTime = -1L;
            randomDelayNs = -1L;
        }
        if (f < randomThreshold) {
            randomReachTime = -1L;
            return false;
        }
        if (randomReachTime < 0L) {
            randomReachTime = System.nanoTime();
            randomDelayNs = randomDelay();
        }
        return System.nanoTime() - randomReachTime >= randomDelayNs;
    }

    private float randomThreshold() {
        if (!isAttack("Рандомизация")) return 0.9f;
        if (isAttack("Только криты") || critFallReady()) {
            return clamp(0.905f + ThreadLocalRandom.current().nextFloat(0.0f, 0.05f), 0.9f, 0.965f);
        }
        float a = ThreadLocalRandom.current().nextFloat(0.855f, 0.955f);
        float b = ThreadLocalRandom.current().nextFloat(0.86f, 0.95f);
        float c = ThreadLocalRandom.current().nextFloat(0.858f, 0.948f);
        float m = (a + b + c) / 3.0f;
        if (ThreadLocalRandom.current().nextFloat() < 0.07f) {
            m += ThreadLocalRandom.current().nextFloat(0.018f, 0.048f);
        }
        return clamp(m, 0.855f, 0.965f);
    }

    private long randomDelay() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double d = 30.0 + rng.nextGaussian() * 12.0;
        if (rng.nextFloat() < 0.08f) {
            d += rng.nextFloat(30.0f, 80.0f);
        }
        d = Math.max(8.0, Math.min(150.0, d));
        return (long) (d * 1000000.0);
    }

    private boolean cpsReady(long now) {
        long interval = (long) (1000.0 / Math.max(1.0f, cps.get()));
        if (lastAttackTime <= 0L) {
            randomInterval = randomInterval(interval);
            return true;
        }
        if (randomInterval < 0L) {
            randomInterval = randomInterval(interval);
        }
        return now - lastAttackTime >= randomInterval;
    }

    private long randomInterval(long base) {
        if (!isAttack("Рандомизация")) return base;
        float a = ThreadLocalRandom.current().nextFloat(0.84f, 1.18f);
        float b = ThreadLocalRandom.current().nextFloat(0.88f, 1.14f);
        float c = ThreadLocalRandom.current().nextFloat(0.9f, 1.12f);
        float m = (a + b + c) / 3.0f;
        if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
            m += ThreadLocalRandom.current().nextFloat(0.04f, 0.12f);
        }
        return Math.max(1L, (long) ((float) base * m));
    }

    private boolean shouldMiss() {
        if (!miss.isState()) return false;
        int chance = Math.round(missChance.get());
        if (chance <= 0) return false;
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    // ============================================================ криты + mace
    private boolean inUncrittableState() {
        return mc.player == null
                || mc.player.isSubmergedInWater()
                || mc.player.isInLava()
                || mc.player.isClimbing()
                || mc.player.hasVehicle()
                || mc.player.getAbilities().flying;
    }

    private boolean maceAerial() {
        if (mc.player == null) return false;
        boolean blocked = mc.player.isSubmergedInWater() || mc.player.isInLava()
                || mc.player.isClimbing() || mc.player.hasVehicle() || mc.player.getAbilities().flying;
        return !blocked && !mc.player.isOnGround() && mc.player.fallDistance > 1.5f;
    }

    private boolean canCriticalFall() {
        if (inUncrittableState()) return false;
        if (usingMace()) return maceAerial();
        return !mc.player.isOnGround() && mc.player.fallDistance > 0.05f;
    }

    private boolean critReady() {
        if (isAttack("Только криты")) {
            if (usingMace()) return maceAerial();
            return canCriticalFall() || groundCrit();
        }
        return true;
    }

    private boolean groundCrit() {
        if (!isAttack("Умные криты") || mc.player == null || usingMace()) return false;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.9f) return false;
        if (inUncrittableState()) return true;
        return mc.player.isOnGround() && !mc.options.jumpKey.isPressed();
    }

    private boolean critFallReady() {
        return canCriticalFall() || groundCrit();
    }

    private boolean jumpIntent() {
        return mc.player != null
                && !inUncrittableState()
                && (mc.options.jumpKey.isPressed() || !mc.player.isOnGround());
    }

    private boolean usingMace() {
        return mc.player != null && mc.player.getMainHandStack().getItem() == Items.MACE;
    }

    private float maceThreshold() {
        return isAttack("Ранний удар булавой") ? 0.55f : 0.9f;
    }

    private boolean maceReady() {
        return usingMace() && maceAerial() && mc.player.getAttackCooldownProgress(0.5f) >= maceThreshold();
    }

    private boolean isSwordAxeTrident() {
        Item item = mc.player.getMainHandStack().getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
                || item instanceof HoeItem || item == Items.MACE;
    }

    private boolean isMeleeWeapon() {
        return isSwordAxeTrident();
    }

    // ============================================================ утилиты
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}