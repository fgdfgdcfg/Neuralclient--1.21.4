package fun.neural.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import fun.neural.Neural;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.api.storages.implement.FreeLookStorage;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.impl.combat.components.gcd.GCDUtil;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ListSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class AimAssist extends Module {

    public static AimAssist INSTANCE = new AimAssist();

    // ============================================================ настройки
    private final ModeSetting mode = new ModeSetting("Режим аима", "Дефолт", "Дефолт", "Доводка", "Помощник", "Тест");

    private final FloatSetting shake = new FloatSetting("Сила шейкинга", 3.0f, 0f, 10f, 0.1f);
    private final FloatSetting speedYaw = new FloatSetting("Скорость YAW", 60f, 0f, 250f, 1f);
    private final FloatSetting speedPitch = new FloatSetting("Скорость PITCH", 30f, 0f, 250f, 1f);

    private final BooleanSetting magnet = new BooleanSetting("Магнит", true).visible(() -> mode.is("Помощник"));
    private final FloatSetting magnetPower = new FloatSetting("Сила магнита", 1.2f, 0.1f, 2.5f, 0.05f)
            .visible(() -> mode.is("Помощник") && magnet.isState());

    private final FloatSetting testFarSpeed = new FloatSetting("Тест: скорость далеко", 150f, 1f, 250f, 1f).visible(() -> mode.is("Тест"));
    private final FloatSetting testNearSpeed = new FloatSetting("Тест: скорость близко", 45f, 1f, 250f, 1f).visible(() -> mode.is("Тест"));
    private final FloatSetting testZone = new FloatSetting("Тест: зона доводки", 8f, 1f, 45f, 0.5f).visible(() -> mode.is("Тест"));

    private final FloatSetting height = new FloatSetting("Высота", 0.6f, 0.1f, 1.0f, 0.01f);
    private final FloatSetting attackRange = new FloatSetting("Дистанция атаки", 3f, 3f, 6f, 0.1f);
    private final FloatSetting rotateRange = new FloatSetting("Дистанция ротации", 3f, 3f, 12f, 1f);
    private final FloatSetting elytraRange = new FloatSetting("Дистанция на элитре", 14f, 8f, 36f, 1f);
    private final BooleanSetting bowAim = new BooleanSetting("Аим на лук/трезубец", true);
    private final FloatSetting bowRange = new FloatSetting("Дистанция аима", 16f, 8f, 50f, 1f).visible(bowAim::isState);

    private final ListSetting targets = new ListSetting("Кого атаковать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Модераторы", false),
            new BooleanSetting("Игроки без брони", false),
            new BooleanSetting("Мобы", true),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Невидимых и голых", true),
            new BooleanSetting("Невидимых", true)
    );

    private final ListSetting extra = new ListSetting("Дополнительно",
            new BooleanSetting("Не бить в GUI", true),
            new BooleanSetting("Бить в инвентаре", false),
            new BooleanSetting("Наводить через стены", false),
            new BooleanSetting("Фокус на одном", true),
            new BooleanSetting("Сортировка целей", true)
    );

    private final ListSetting sortBy = new ListSetting("Сортировать по",
            new BooleanSetting("Здоровью", true),
            new BooleanSetting("Броне", true),
            new BooleanSetting("Дистанции", true),
            new BooleanSetting("Полю зрения", true),
            new BooleanSetting("Зельям", false)
    ).visible(() -> extra.is("Сортировка целей"));

    private final ModeSetting attackMode = new ModeSetting("Режим атаки", "1.9", "1.9", "1.8");

    // ============================================================ состояние
    private LivingEntity target;
    private int lastTargetId = -1;

    private long lastTime;

    // трекинг «смещения» наводимого взгляда (для доводки/магнита)
    private float mouseDeltaYaw;
    private float mouseDeltaPitch;
    private float alignFactor;
    private float appliedYaw;
    private float appliedPitch;
    private boolean applied;

    // шейкинг (лёгкий дрожание точки прицела)
    private int shankTargetId = -1;
    private float shankOffsetX;
    private float shankOffsetZ;
    private long shankNoiseTime;

    // рандомное смещение точки прицела
    private int noiseTargetId = -1;
    private float noiseX;
    private float noiseY;
    private long noiseTime;

    // сглаживание точки (ty9405)
    private Vec3d smoothedPoint;

    // состояние «Доводка»
    private int holdTicks;
    private float dovodka;
    private float dovodkaStrength = 0.36f;

    // состояние «Магнит»
    private float magnetEase;
    private float magnetStrength;

    // состояние «Тест»
    private float testYawBlend;
    private float testPitchBlend;

    // остатки для GCD-квантования
    private float gcdRemYaw;
    private float gcdRemPitch;

    public AimAssist() {
        super("AimAssist", "Автоматически наводит прицел на игроков в выбранной дистанции", ModuleCategory.COMBAT);
        addSettings(
                mode,
                shake, speedYaw, speedPitch,
                magnet, magnetPower,
                testFarSpeed, testNearSpeed, testZone,
                height, attackRange, rotateRange, elytraRange,
                bowAim, bowRange,
                targets, extra, sortBy, attackMode
        );
    }

    private boolean isMode(String name) {
        return mode.is(name);
    }

    private boolean isExtra(String name) {
        return extra.is(name);
    }

    private boolean isTarget(String name) {
        return targets.is(name);
    }

    private float getGCD() {
        float sens = mc.options.getMouseSensitivity().getValue().floatValue();
        float gcd = sens * 0.6f + 0.2f;
        return gcd * gcd * gcd * 1.2f;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    private void resetState() {
        target = null;
        lastTargetId = -1;
        lastTime = 0L;
        mouseDeltaYaw = 0f;
        mouseDeltaPitch = 0f;
        alignFactor = 0f;
        appliedYaw = 0f;
        appliedPitch = 0f;
        applied = false;
        shankTargetId = -1;
        shankOffsetX = 0f;
        shankOffsetZ = 0f;
        shankNoiseTime = 0L;
        noiseTargetId = -1;
        noiseX = 0f;
        noiseY = 0f;
        noiseTime = 0L;
        smoothedPoint = null;
        holdTicks = 0;
        dovodka = 0f;
        dovodkaStrength = 0.36f;
        magnetEase = 0f;
        magnetStrength = 0f;
        testYawBlend = 0f;
        testPitchBlend = 0f;
        gcdRemYaw = 0f;
        gcdRemPitch = 0f;
    }

    // ============================================================ выбор цели
    private boolean isValid(LivingEntity e) {
        if (e == null || e == mc.player || !e.isAlive()) return false;
        if (e instanceof ArmorStandEntity) return false;
        if (e.isSpectator() || !e.canHit()) return false;

        if (isFriend(e)) return false;
        if (e instanceof PlayerEntity player) {
            if (!isTarget("Игроки")) return false;
            boolean staff = isStaff(player);
            if (staff && !isTarget("Модераторы")) return false;
            boolean naked = isNaked(player);
            if (!isTarget("Игроки без брони") && naked) return false;
            boolean invisible = player.isInvisible();
            if (invisible) {
                boolean allow = isTarget("Невидимых") || (isTarget("Невидимых и голых") && naked);
                if (!allow) return false;
            }
        } else if (e instanceof Monster || e instanceof HostileEntity) {
            if (!isTarget("Мобы")) return false;
        } else if (e instanceof AnimalEntity) {
            if (!isTarget("Животные")) return false;
        } else {
            return false;
        }
        return true;
    }

    private boolean isFriend(Entity entity) {
        return Neural.INSTANCE != null
                && Neural.INSTANCE.friendStorage != null
                && entity instanceof PlayerEntity player
                && Neural.INSTANCE.friendStorage.isFriend(player.getName().getString());
    }

    private boolean isStaff(PlayerEntity player) {
        return Neural.INSTANCE != null
                && Neural.INSTANCE.staffStorage != null
                && Neural.INSTANCE.staffStorage.isStaff(player.getName().getString());
    }

    private boolean isNaked(PlayerEntity player) {
        for (ItemStack stack : player.getArmorItems()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    private LivingEntity pickBest(List<LivingEntity> list) {
        if (list.isEmpty()) return null;
        if (!isExtra("Сортировка целей")) {
            LivingEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (LivingEntity e : list) {
                double d = mc.player.squaredDistanceTo(e);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            return best;
        }
        double reach = getSelectionRange();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double score = score(e, reach);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private double score(LivingEntity e, double reach) {
        double score = 0.0;
        double angle = Math.abs(wrapDegrees(getYawTo(e) - rotationYaw()));
        if (sortBy.is("Здоровью")) score += e.getHealth();
        if (sortBy.is("Броне")) score += e.getArmor() * 0.6;
        if (sortBy.is("Дистанции")) score += mc.player.distanceTo(e);
        if (sortBy.is("Полю зрения")) score += angle;
        if (sortBy.is("Зельям")) score += (e.getActiveStatusEffects().isEmpty() ? 6.0 : 0.0);
        if (reach > 0) score = score / reach * reach;
        return score + angle * 0.001;
    }

    private void refreshTarget(double range) {
        List<LivingEntity> list = mc.world.getEntitiesByClass(LivingEntity.class,
                mc.player.getBoundingBox().expand(range), e -> isValid(e));
        target = pickBest(list);
    }

    // ============================================================ хелперы ротаций
    private float rotationYaw() {
        return mc.player.getYaw();
    }

    private float rotationPitch() {
        return mc.player.getPitch();
    }

    private float getYawTo(Entity e) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d pos = e.getBoundingBox().getCenter();
        return (float) (Math.toDegrees(Math.atan2(pos.z - eyes.z, pos.x - eyes.x)) - 90.0);
    }

    private EntityHitResult raycastEntity(float yaw, float pitch, double distance, Predicate<Entity> filter) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eyes.add(dir.multiply(distance));
        return ProjectileUtil.raycast(mc.player, eyes, end,
                mc.player.getBoundingBox().expand(distance).stretch(dir.multiply(distance)), filter, distance * distance);
    }

    private boolean aimingAt(LivingEntity e, float yaw, float pitch, double distance) {
        if (mc.player == null || e == null) return false;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eyes.add(dir.multiply(distance));
        return e.getBoundingBox().expand(0.08f, 0.22f, 0.08f).raycast(eyes, end).isPresent();
    }

    private double getSelectionRange() {
        boolean bow = bowAim.isState() && isRanged();
        if (bow) return bowRange.get();
        if (mc.player != null && mc.player.isGliding()) return elytraRange.get();
        return rotateRange.get();
    }

    private boolean isRanged() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }

    // ============================================================ точка прицела (Hh99 + shank + оптимизация)
    private Vec3d aimPoint(LivingEntity e, boolean changed) {
        Box box = e.getBoundingBox();
        Vec3d center = box.getCenter();
        Vec3d eyes = mc.player.getEyePos();
        Vec3d flat = new Vec3d(center.x - eyes.x, 0.0, center.z - eyes.z);
        Vec3d perp = flat.lengthSquared() > 1.0E-5 ? new Vec3d(-flat.z, 0.0, flat.x).normalize() : Vec3d.ZERO;

        float power = MathHelper.clamp(shake.get() / 10.0f, 0.0f, 1.0f);
        float micro = (float) Math.sin(System.nanoTime() * 1.0E-9 * 1.35 + e.getId() * 0.37f) * 0.006f * power;
        double y = box.minY + e.getHeight() * MathHelper.clamp(height.get(), 0.18f, 0.92f) + noiseY;

        Vec3d point = new Vec3d(
                center.x + perp.x * (noiseX + micro),
                MathHelper.clamp(y, box.minY + 0.18, box.maxY - 0.08),
                center.z + perp.z * (noiseX + micro)
        );

        if (e.isGliding()) {
            point = point.add(glidePrediction(e, e.getVelocity()));
        }
        return point;
    }

    private Vec3d glidePrediction(LivingEntity e, Vec3d v) {
        Vec3d look = e.getRotationVector();
        float pitch = e.getPitch() * ((float) Math.PI / 180);
        double lookHoriz = Math.sqrt(look.x * look.x + look.z * look.z);
        double dist = v.horizontalLength();
        double g = 0.08;
        double cos = MathHelper.square(Math.cos(pitch));
        v = v.add(0.0, g * (-1.0 + cos * 0.75), 0.0);
        if (v.y < 0.0 && lookHoriz > 0.0) {
            double d = v.y * -0.1 * cos;
            v = v.add(look.x * d / lookHoriz, d, look.z * d / lookHoriz);
        }
        if (pitch < 0.0f && lookHoriz > 0.0) {
            double d = dist * (-Math.sin(pitch)) * 0.04;
            v = v.add(-look.x * d / lookHoriz, d * 3.2, -look.z * d / lookHoriz);
        }
        if (lookHoriz > 0.0) {
            v = v.add((look.x / lookHoriz * dist - v.x) * 0.1, 0.0, (look.z / lookHoriz * dist - v.z) * 0.1);
        }
        return v.multiply(0.99, 0.98, 0.99);
    }

    // ============================================================ шейкинг (xn47)
    private void updateShank(float delta) {
        if (target == null) {
            shankTargetId = -1;
            shankOffsetX = 0f;
            shankOffsetZ = 0f;
            return;
        }
        if (shankTargetId != target.getId()) {
            shankTargetId = target.getId();
            shankOffsetX = 0f;
            shankOffsetZ = 0f;
        }
        float power = MathHelper.clamp(shake.get(), 0.0f, 10.0f);
        if (power <= 0.001f) {
            shankTargetId = target.getId();
            shankOffsetX = 0f;
            shankOffsetZ = 0f;
            return;
        }
        double time = System.nanoTime() * 1.0E-9;
        float ph = (target.getId() & 0x3FF) * 0.173f;
        float amp = power * 0.013f;
        float dx = (float) (Math.sin(time * 1.47 + ph) + Math.sin(time * 2.13 + ph * 0.41f) * 0.32f) * amp;
        float dz = (float) (Math.cos(time * 1.31 + ph * 0.7f) + Math.sin(time * 1.91 + ph + 1.4f) * 0.28f) * amp;
        dx = MathHelper.clamp(dx, -amp, amp);
        dz = MathHelper.clamp(dz, -amp, amp);
        float ea = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 5.2f), 0.025f, 0.16f);
        shankOffsetX = MathHelper.lerp(ea, shankOffsetX, dx);
        shankOffsetZ = MathHelper.lerp(ea, shankOffsetZ, dz);
    }

    // ============================================================ рандом шума точки (net9036)
    private void updateAimNoise(boolean changed) {
        if (target == null) {
            noiseTargetId = -1;
            return;
        }
        long now = System.nanoTime();
        if (changed || noiseTargetId != target.getId() || now >= noiseTime) {
            noiseTargetId = target.getId();
            float power = MathHelper.clamp(shake.get() / 10.0f, 0.0f, 1.0f);
            float ax = 0.012f + power * 0.03f;
            float ay = 0.008f + power * 0.024f;
            noiseX = ThreadLocalRandom.current().nextFloat(-ax, ax);
            noiseY = ThreadLocalRandom.current().nextFloat(-ay, ay);
            noiseTime = now + ThreadLocalRandom.current().nextLong(340000000L, 760000000L);
        }
    }

    // ============================================================ mouse tracking (pd834)
    private float updateAlign(float yaw, float pitch, float delta) {
        if (!applied) {
            mouseDeltaYaw = 0f;
            mouseDeltaPitch = 0f;
            alignFactor = 0f;
            return 0f;
        }
        mouseDeltaYaw = wrapDegrees(yaw - appliedYaw);
        mouseDeltaPitch = MathHelper.clamp(pitch - appliedPitch, -45.0f, 45.0f);
        float gcd = getGCD();
        float minActive = Math.max(0.004f, gcd * 0.08f);
        float total = Math.abs(mouseDeltaYaw) + Math.abs(mouseDeltaPitch) * 1.35f;
        float t = MathHelper.clamp((total - minActive) / 0.62f, 0.0f, 1.0f);
        float ea = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 18.0f), 0.16f, 0.48f);
        alignFactor = MathHelper.lerp(ea, alignFactor, t);
        return alignFactor;
    }

    // ============================================================ Доводка strength (mzm)
    private float dovodkaStrength(float yawErr, float pitchErr, float align) {
        float total = Math.abs(mouseDeltaYaw) + Math.abs(mouseDeltaPitch) * 1.35f;
        if (total <= 0.002f) return 0.36f;
        float dot = mouseDeltaYaw * yawErr + mouseDeltaPitch * pitchErr * 0.72f;
        float abs = Math.max(0.001f, Math.abs(yawErr) + Math.abs(pitchErr) * 0.72f);
        float c = MathHelper.clamp(dot / Math.max(0.001f, total * abs), -1.0f, 1.0f);
        float gain = c > 0.0f ? MathHelper.lerp(c, 0.62f, 1.0f) : 0.3f;
        return MathHelper.clamp((0.42f + align * 0.86f) * gain, 0.24f, 1.22f);
    }

    private float dovodkaVt() {
        float power = MathHelper.clamp(shake.get(), 0.0f, 10.0f);
        return MathHelper.clamp(power / 10.0f * 0.22f, 0.0f, 0.22f);
    }

    // ============================================================ Помощник / Магнит (nu)
    private float[] magnet(float yaw, float pitch, float yawErr, float pitchErr, float delta, boolean changed, boolean aimed) {
        if (changed) {
            magnetEase = 0f;
            magnetStrength = 0f;
        }
        float mY = mouseDeltaYaw;
        float mP = mouseDeltaPitch;
        float mouseMove = Math.abs(mY) + Math.abs(mP);
        float gcd = getGCD();
        float dead = Math.max(0.02f, gcd * 0.5f);
        if (mouseMove < dead) {
            magnetStrength *= 0.6f;
            return new float[]{yaw, pitch};
        }
        float dist = Math.abs(yawErr) + Math.abs(pitchErr) * 0.62f;
        float maxDist = MathHelper.clamp(0f, 24.0f, 180.0f);
        if (!aimed && dist > maxDist) {
            magnetStrength *= 0.6f;
            return new float[]{yaw, pitch};
        }
        float sum = Math.max(0.001f, Math.abs(yawErr) + Math.abs(pitchErr));
        float dot = mY * yawErr + mP * pitchErr;
        float align = MathHelper.clamp(dot / (mouseMove * sum), 0.0f, 1.0f);
        float onTarget = aimed ? MathHelper.clamp(1.0f - dist / 8.5f, 0.35f, 1.0f) : 0.0f;
        float ea = MathHelper.clamp(1.0f - (float) Math.exp(-delta * (aimed ? 7.5f : 4.2f)), 0.02f, aimed ? 0.2f : 0.12f);
        magnetEase = MathHelper.lerp(ea, magnetEase, onTarget);
        float power = MathHelper.clamp(magnetPower.get(), 0.1f, 2.5f);
        float yawScale = MathHelper.clamp(speedYaw.get() / 60.0f, 0.5f, 3.0f);
        float target = MathHelper.clamp(0.42f * yawScale * power, 0.0f, 0.95f) * align * MathHelper.lerp(magnetEase, 1.0f, 0.45f);
        float easeUp = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 42.0f), 0.2f, 0.85f);
        float easeDown = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 12.0f), 0.08f, 0.4f);
        float ease = target > magnetStrength ? easeUp : easeDown;
        magnetStrength = MathHelper.lerp(ease, magnetStrength, target);
        float st = magnetStrength;
        float stepYaw = yawErr * st;
        float stepPitch = pitchErr * st * 0.92f;
        float cap = MathHelper.clamp(0.85f * power, 0.3f, 1.7f);
        float capYaw = Math.abs(mY) * cap;
        float capPitch = Math.abs(mP) * cap + Math.abs(mY) * 0.12f;
        stepYaw = MathHelper.clamp(stepYaw, -capYaw, capYaw);
        stepPitch = MathHelper.clamp(stepPitch, -capPitch, capPitch);
        stepYaw = clampFrac(stepYaw, yawErr, 1.0f);
        stepPitch = clampFrac(stepPitch, pitchErr, 1.0f);
        return new float[]{yaw + stepYaw, pitch + stepPitch};
    }

    private static float clampFrac(float value, float diff, float scale) {
        float cap = Math.abs(diff) * scale;
        if (cap <= 0.001f) return 0.0f;
        return MathHelper.clamp(value, -cap, cap);
    }

    // ============================================================ GCD-квантование (qhi311)
    private float quantizeGcd(float current, float next, float gcd, boolean yaw) {
        if (gcd <= 0.0f) return next;
        float rem = yaw ? gcdRemYaw : gcdRemPitch;
        float delta = next - current + rem;
        float q = Math.round(delta / gcd) * gcd;
        float newRem = MathHelper.clamp(delta - q, -gcd, gcd);
        if (yaw) gcdRemYaw = newRem;
        else gcdRemPitch = newRem;
        return current + q;
    }

    // ============================================================ главный тик
    @EventLink
    public void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (FreeLookStorage.isActive()) {
            target = null;
            return;
        }

        if (mc.player.isDead() || !mc.player.isAlive()) {
            target = null;
            return;
        }

        boolean inGui = mc.currentScreen != null;
        if (isExtra("Не бить в GUI") && inGui) {
            boolean inventoryOk = isExtra("Бить в инвентаре") && mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>;
            if (!inventoryOk) {
                target = null;
                return;
            }
        }

        float delta = deltaTime();

        boolean focused = isExtra("Фокус на одном") && target != null && isValid(target);
        if (!focused) {
            refreshTarget(getSelectionRange());
        }

        if (target == null) {
            lastTargetId = -1;
            smoothedPoint = null;
            applied = false;
            holdTicks = 0;
            return;
        }

        boolean changed = lastTargetId != target.getId();
        lastTargetId = target.getId();

        if (changed) {
            smoothedPoint = null;
            holdTicks = 0;
            dovodka = 0f;
            dovodkaStrength = 0.36f;
            magnetEase = 0f;
            magnetStrength = 0f;
            testYawBlend = 0f;
            testPitchBlend = 0f;
        }

        updateShank(delta);
        updateAimNoise(changed);

        float currentYaw = rotationYaw();
        float currentPitch = rotationPitch();
        float align = updateAlign(currentYaw, currentPitch, delta);

        boolean dov = isMode("Доводка");
        boolean pom = isMode("Помощник");
        boolean test = isMode("Тест");

        Vec3d point = aimPoint(target, changed);
        point = point.add(shankOffsetX, 0.0, shankOffsetZ);

        if (dov || pom || test) {
            Vec3d eyes = mc.player.getEyePos();
            Vec3d diff = point.subtract(eyes);
            float pa = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 13.0f), 0.06f, 0.28f);
            if (changed || smoothedPoint == null) {
                smoothedPoint = point;
            } else {
                smoothedPoint = new Vec3d(
                        smoothedPoint.x + (point.x - smoothedPoint.x) * pa,
                        smoothedPoint.y + (point.y - smoothedPoint.y) * pa,
                        smoothedPoint.z + (point.z - smoothedPoint.z) * pa
                );
            }
        } else {
            smoothedPoint = null;
        }

        Vec3d usePoint = smoothedPoint != null ? smoothedPoint : point;
        Vec3d diff = usePoint.subtract(mc.player.getEyePos());

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));

        Box box = target.getBoundingBox();
        double lowY = box.getMin(net.minecraft.util.math.Direction.Axis.Y) - mc.player.getEyePos().y - 0.2;
        double highY = box.getMax(net.minecraft.util.math.Direction.Axis.Y) - mc.player.getEyePos().y - 0.2;
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float pBottom = (float) (-Math.toDegrees(Math.atan2(lowY, horizontal)));
        float pTop = (float) (-Math.toDegrees(Math.atan2(highY, horizontal)));
        targetPitch = MathHelper.clamp(targetPitch, pTop, pBottom);

        float yawErr = wrapDegrees(targetYaw - currentYaw);
        float pitchErr = targetPitch - currentPitch;

        float maxYaw = Math.max(1.0f, speedYaw.get()) * 2.8f;
        float maxPitch = Math.max(1.0f, speedPitch.get()) * 2.2f;
        float yawEase = 1.0f - (float) Math.exp(-delta * (2.8f + MathHelper.clamp(speedYaw.get() / 60.0f, 0.0f, 4.2f) * 4.4f));
        float pitchEase = 1.0f - (float) Math.exp(-delta * (2.4f + MathHelper.clamp(speedPitch.get() / 30.0f, 0.0f, 4.4f) * 3.8f));
        float yawStep = MathHelper.clamp(yawErr * yawEase, -maxYaw * delta, maxYaw * delta);
        float pitchStep = MathHelper.clamp(pitchErr * pitchEase, -maxPitch * delta, maxPitch * delta);

        float outYaw;
        float outPitch;

        if (pom) {
            if (!magnet.isState()) {
                outYaw = currentYaw + yawStep;
                outPitch = currentPitch + pitchStep;
            } else {
                double aimDist = Math.min(12.0, Math.max(3.0, mc.player.distanceTo(target) + 0.75));
                boolean aimed = aimingAt(target, currentYaw, currentPitch, aimDist);
                float[] res = magnet(currentYaw, currentPitch, yawErr, pitchErr, delta, changed, aimed);
                outYaw = res[0];
                outPitch = res[1];
            }
        } else if (test) {
            float zoneTotal = Math.abs(yawErr) + Math.abs(pitchErr) * 0.7f;
            float zone = Math.max(0.5f, testZone.get());
            float t = MathHelper.clamp(zoneTotal / zone, 0.0f, 1.0f);
            float strength = MathHelper.lerp(t, Math.max(1.0f, testFarSpeed.get()), Math.max(1.0f, testNearSpeed.get()));
            float tMaxYaw = strength * 2.8f;
            float tMaxPitch = strength * 2.2f;
            float tYawEase = 1.0f - (float) Math.exp(-delta * (2.8f + MathHelper.clamp(strength / 60.0f, 0.0f, 4.2f) * 4.4f));
            float tPitchEase = 1.0f - (float) Math.exp(-delta * (2.4f + MathHelper.clamp(strength / 30.0f, 0.0f, 4.4f) * 3.8f));
            float sy = MathHelper.clamp(yawErr * tYawEase, -tMaxYaw * delta, tMaxYaw * delta);
            float sp = MathHelper.clamp(pitchErr * tPitchEase, -tMaxPitch * delta, tMaxPitch * delta);
            sy = sy * 0.7f + testYawBlend * 0.3f;
            sp = sp * 0.7f + testPitchBlend * 0.3f;
            testYawBlend = sy;
            testPitchBlend = sp;
            outYaw = currentYaw + sy;
            outPitch = currentPitch + sp;
        } else if (dov) {
            float strength = dovodkaStrength(yawErr, pitchErr, align);
            if (changed) {
                dovodkaStrength = strength;
            } else {
                float ea = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 16.0f), 0.12f, 0.5f);
                dovodkaStrength = MathHelper.lerp(ea, dovodkaStrength, strength);
            }
            float st = dovodkaStrength;

            double aimDist = Math.min(12.0, Math.max(3.0, mc.player.distanceTo(target) + 0.5));
            boolean aimed = aimingAt(target, currentYaw, currentPitch, aimDist);
            if (aimed) {
                holdTicks = 5;
            } else if (holdTicks > 0) {
                holdTicks--;
            }
            float vt;
            if (aimed || holdTicks > 0) {
                vt = dovodkaVt();
            } else {
                vt = 1.0f;
            }
            if (changed) {
                dovodka = vt;
            } else {
                float ea = MathHelper.clamp(1.0f - (float) Math.exp(-delta * 8.0f), 0.04f, 0.22f);
                dovodka = MathHelper.lerp(ea, dovodka, vt);
            }
            outYaw = currentYaw + yawStep * dovodka * st;
            outPitch = currentPitch + pitchStep * dovodka * st;
        } else {
            outYaw = currentYaw + yawStep;
            outPitch = currentPitch + pitchStep;
        }

        outPitch = MathHelper.clamp(outPitch, -90.0f, 90.0f);

        float gcd = getGCD();
        outYaw = quantizeGcd(currentYaw, outYaw, gcd, true);
        outPitch = quantizeGcd(currentPitch, outPitch, gcd, false);
        outPitch = MathHelper.clamp(outPitch, -90.0f, 90.0f);

        if (!isExtra("Наводить через стены") && smoothedPoint != null) {
            // смены цели достаточно
        }

        if (Math.abs(outYaw - currentYaw) >= 0.003f || Math.abs(outPitch - currentPitch) >= 0.003f) {
            mc.player.setYaw(outYaw);
            mc.player.setPitch(outPitch);
        }

        appliedYaw = mc.player.getYaw();
        appliedPitch = mc.player.getPitch();
        applied = true;
    }

    private float deltaTime() {
        long now = System.currentTimeMillis();
        if (lastTime == 0L) {
            lastTime = now;
            return 0.05f;
        }
        float delta = (now - lastTime) / 1000.0f;
        lastTime = now;
        return MathHelper.clamp(delta, 0.004166667f, 0.05f);
    }

    public LivingEntity getTarget() {
        return target;
    }
}