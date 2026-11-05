package fun.neural.client.modules.impl.combat;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import fun.neural.Neural;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventAttackEntity;
import fun.neural.api.events.implement.EventGameUpdate;
import fun.neural.api.events.implement.EventKeyboardInput;
import fun.neural.api.events.implement.EventMoveInput;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.api.events.implement.EventUpdatePost;
import fun.neural.api.storages.implement.FreeLookStorage;
import fun.neural.api.storages.implement.NeuroAuraStorage;
import fun.neural.api.storages.implement.RotationStorage;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.api.utils.combat.IdealHitUtils;
import fun.neural.api.utils.combat.PredictUtils;
import fun.neural.api.utils.input.MovingUtil;
import fun.neural.api.utils.math.MathUtils;
import fun.neural.api.utils.math.TimerUtils;
import fun.neural.api.utils.player.HotbarUtil;
import fun.neural.api.utils.player.SlotSearchResult;
import fun.neural.api.utils.rotate.MultipointUtils;
import fun.neural.api.utils.rotate.Rotation;
import fun.neural.api.utils.rotate.RotationUtils;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.impl.combat.components.RotationsSystem;
import fun.neural.client.modules.impl.combat.components.interpolation.BestPoint;
import fun.neural.client.modules.impl.combat.components.rotations.LegitRotation;
import fun.neural.client.modules.impl.combat.components.rotations.NeuroRotation;
import fun.neural.client.modules.impl.combat.components.rotations.ShardRotation;
import fun.neural.client.modules.impl.combat.components.rotations.SlothRotation;
import fun.neural.client.modules.impl.combat.components.rotations.TestRotation;
import fun.neural.client.modules.impl.combat.components.rotations.WellMineRotation;
import fun.neural.client.modules.impl.combat.components.rotations.WhiteRiseRotation;
import fun.neural.client.modules.impl.combat.components.rotations.LegitWorldRotation;
import fun.neural.client.modules.impl.render.FakePlayer;
import fun.neural.client.modules.impl.movement.Sprint;
import fun.neural.client.modules.impl.player.AutoEat;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ListSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;
import fun.neural.mixin.ILivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class Aura extends Module {
    public static Aura INSTANCE = new Aura();

    public final ModeSetting rotationType = new ModeSetting("Ротация", "NoRotate",
            "Smooth", "Snap", "Data", "Sloth", "NoRotate", "Neuro", "Shard", "LegitWorld");

    private final ListSetting targets = new ListSetting("Таргеты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Невидимки", true),
            new BooleanSetting("Мирные", false),
            new BooleanSetting("Мобы", true)
    );

    private final FloatSetting range = new FloatSetting("Дистанция атаки", 3f, 0f, 6f, 0.05f);
    private final FloatSetting aimRange = new FloatSetting("Дистанция наводки", 3f, 0f, 6f, 0.05f);
    private final FloatSetting elytraAimRange = new FloatSetting("Дистанция на элитрах", 50f, 10f, 100f, 0.05f);
    public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", false);
    private final ModeSetting sprintReset = new ModeSetting("Сброс спринта", "Легитный", "Нет", "Пакетный", "Легитный");
    private final BooleanSetting throughWalls = new BooleanSetting("Бить через стены", true);
    private final BooleanSetting raycast = new BooleanSetting("Проверка на наведение", false);
    private final BooleanSetting unpressShield = new BooleanSetting("Отжимать щит", false);
    private final BooleanSetting breakShield = new BooleanSetting("Ломать щит", true);
    private final BooleanSetting attackOnEating = new BooleanSetting("Не бить когда ешь", true);
    public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
    private final ModeSetting moveFix = new ModeSetting("Коррекция", "Нет", "Нет", "Свободная", "Сфокусированная", "Полная");
    private final ModeSetting priority = new ModeSetting("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");

    @Getter
    private LivingEntity target;
    @Getter
    private Vec2f currentRotations = new Vec2f(0f, 0f);
    @Getter
    private Vec2f targetRotations = new Vec2f(0f, 0f);

    public void setSilentRotation(Vec2f currentRot, Vec2f targetRot) {
        this.currentRotations = currentRot;
        this.targetRotations = targetRot;
    }
    @Getter
    private final NeuroAuraStorage dataSystem = new NeuroAuraStorage();
    @Getter
    private final TimerUtils attackTimer = new TimerUtils();
    private final BooleanSetting rwWallBypass = new BooleanSetting("Обход рв стен", false);
    private final BooleanSetting rwWallLookDown = new BooleanSetting("Смотреть вниз", false).visible(rwWallBypass::isState);
    private final BooleanSetting wallCritBypass = new BooleanSetting("Криты в стене", true).visible(() -> rotationType.is("LegitWorld"));
    private final WellMineRotation wellMineRotation = new WellMineRotation();
    private final TestRotation testRotation = new TestRotation();
    private final SlothRotation slothRotation = new SlothRotation();
    private final WhiteRiseRotation whiteRiseRotation = new WhiteRiseRotation(this);
    private final LegitWorldRotation legitWorldRotation = new LegitWorldRotation();

    private final TimerUtils backTimer = new TimerUtils();

    private int lastTargetAttackTicks = Integer.MAX_VALUE;
    private boolean enemyAttackBreak = false;

    private BlockPos lastWallDigPos = null;
    private Direction lastWallDigDir = null;
    private long lastWallDigTime = 0L;
    private long lastWallCritPackets = 0L;

    private LivingEntity wallCheckEntity = null;
    private boolean wallCheckResult = false;
    private int wallCheckTick = Integer.MIN_VALUE;
    private int insideBlocksTick = Integer.MIN_VALUE;
    private boolean insideBlocksResult = false;

    private TpsSync tpsSync;

    private long cps = 0;
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;
    private int ticksToAttack = 0;
    private int snapAttackAge = -1;
    private boolean snapAttackQueued = false;
    private LivingEntity snapAttackTarget = null;
    private LivingEntity lastDataTarget = null;

    private float lastYaw = 0f;
    private float lastPitch = 0f;

    public static float adjYaw;
    public static float adjPitch;
    public static float otvodkaYaw;
    public static float otvodkaPitch;

    public boolean isRotated;

    public Aura() {
        super("AttackAura", "Автоматически наводиться и бьёт таргета", ModuleCategory.COMBAT);
        addSettings(rotationType, targets, range, aimRange, elytraAimRange, smartCrit, sprintReset,
                attackOnEating, throughWalls, rwWallBypass, rwWallLookDown, wallCritBypass, raycast, unpressShield, breakShield, clientLook, moveFix, priority);
    }


    @EventLink
    public void onPlayerTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (tpsSync == null && Neural.INSTANCE != null && Neural.INSTANCE.moduleStorage != null) {
            tpsSync = ModuleClass.tpsSync;
        }

        lastYaw++;
        updateTarget();

if (dataSystem.isRecording()) {
            LivingEntity recordTarget = findTargetForRecording();
            dataSystem.recordTick(recordTarget, mc.player.getYaw(), mc.player.getPitch());
            }
            if (NeuroRotation.INSTANCE.isRecording()) {
                LivingEntity neuroTarget = findTargetForRecording();
                NeuroRotation.INSTANCE.recordTick(neuroTarget, mc.player.getYaw(), mc.player.getPitch());
            }
    }

    @EventLink
    public void onAttackEntity(EventAttackEntity event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getPlayer() != mc.player) return;
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        if (!isValidTarget(living)) return;

        target = living;
    }

    @EventLink
    public void onMoveInput(EventMoveInput event) {
        if (needSprintReset) {
            event.setForward(0);
            event.setStrafe(0);
            needSprintReset = false;
            sprintResetDone = true;
            sprintResetTicks = 0;
            return;
        }

        applyMoveFix(event);
    }

    private void applyMoveFix(EventMoveInput event) {
        if (mc.player == null || target == null || rotationType.is("NoRotate") || moveFix.getIndex() == 0) {
            return;
        }

        if (moveFix.getIndex() == 1) {
            MovingUtil.fixMovementFree(event);
        }
    }

    @EventLink
    public void onKeyboardInput(EventKeyboardInput event) {
        if (mc.player == null || mc.world == null || target == null || rotationType.is("NoRotate")) {
            return;
        }

        float correctionYaw = getCorrectionYaw();
        if (moveFix.getIndex() == 2) {
            event.setYaw(correctionYaw, mc.player.getYaw());
        } else if (moveFix.getIndex() == 3) {
            event.setYaw(correctionYaw, getTargetDirectionYaw());
        }
    }

    private float getCorrectionYaw() {
        if (RotationStorage.instance != null && RotationStorage.instance.targetRotation() != null) {
            return RotationStorage.instance.targetRotation().getYaw();
        }
        return mc.player.getYaw();
    }

    private float getTargetDirectionYaw() {
        return RotationUtils.getRotations(target.getBoundingBox().getCenter()).x;
    }

    private void applyMovementCorrection(EventMoveInput event, float yaw, float directionYaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        double angle = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.direction(directionYaw, forward, strafe)));
        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedForward == 0.0F && predictedStrafe == 0.0F) {
                    continue;
                }

                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.direction(yaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    @EventLink
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null || mc.world == null || target == null) return;
        rotate();
    }


    @EventLink
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (ticksToAttack > 0) {
            ticksToAttack--;
        }

        if (sprintResetDone) {
            sprintResetTicks++;
        }

        boolean packetCrits = ModuleClass.packetCriticals.isEnable()
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);

        if (!packetCrits) {
            processAttack();
        }

        if (dataSystem.isShowStats() && mc.player.age % 40 == 0 && (dataSystem.isRecording() || dataSystem.isUsingNeuro())) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(dataSystem.getStatusString()), true);
        }
    }

    @EventLink
    public void onPost(EventUpdatePost e) {
        if (mc.player == null || mc.world == null) return;

        boolean packetCrits = ModuleClass.INSTANCE.packetCriticals.isEnable()
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);

        if (packetCrits && mc.player.fallDistance > 0 && mc.player.fallDistance < 1) {
            processAttack();
        }
    }

    private LivingEntity findTargetForRecording() {
        if (FakePlayer.INSTANCE != null && FakePlayer.INSTANCE.isEnable()) {
            LivingEntity fake = FakePlayer.INSTANCE.getFakeEntity();
            if (fake != null && fake.isAlive() && fake.getHealth() > 0) {
                return fake;
            }
        }

        LivingEntity bestTarget = null;
        double bestDistance = 100.0;
        Vec3d eyePos = mc.player.getEyePos();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.getHealth() <= 0) continue;
            if (living instanceof ArmorStandEntity) continue;

            double distance = eyePos.squaredDistanceTo(living.getBoundingBox().getCenter());
            if (distance > bestDistance) continue;

            bestDistance = distance;
            bestTarget = living;
        }

        return bestTarget;
    }


    private void processAttack() {
        updateTarget();

        if (target != null) {
            if ((shouldAttack() || shouldUseQueuedSnapAttack()) && cps <= System.currentTimeMillis()) {
                if (attackOnEating.isState() && (shouldBlockAttackWhileUsingItem() || AutoEat.shouldSuppressCombat())) return;

                if (!sprintReset.is("Нет")
                        && mc.player.isSprinting()
                        && !sprintResetDone
                        && !shouldSkipSprintResetInWater()
                        && !IdealHitUtils.isNoJumpDelayJumpCritWindow()) {
                    if (sprintReset.is("Пакетный")) {
                        sendSprintResetPacket(ClientCommandC2SPacket.Mode.STOP_SPRINTING);
                        sprintResetDone = true;
                        sprintResetTicks = 0;
                    } else {
                        needSprintReset = true;
                    }
                    return;
                }

                if (!sprintReset.is("Нет") && sprintResetDone && sprintResetTicks < 1) {
                    return;
                }

                if (isSnapRotationActive() && !prepareSnapAttack()) {
                    return;
                }
                attack();
                if (sprintReset.is("Пакетный") && sprintResetDone) {
                    sendSprintResetPacket(ClientCommandC2SPacket.Mode.START_SPRINTING);
                }
                resetSnapAttack();
                sprintResetDone = false;
                sprintResetTicks = 0;
            }
        } else {
            cps = System.currentTimeMillis();
            backTimer.reset();
            adjPitch = 0;
            adjYaw = 0;
                wellMineRotation.reset();
                testRotation.reset();
                slothRotation.reset();
                whiteRiseRotation.reset();
                legitWorldRotation.reset();
                dataSystem.resetState();
                lastDataTarget = null;
                enemyAttackBreak = false;
            resetWallDigState();
            sprintResetDone = false;
            sprintResetTicks = 0;
            ticksToAttack = 0;
            resetSnapAttack();
        }
    }

    public void Rotate() {
        rotate();
    }


    private void rotate() {
        if (ModuleClass.elytraresolver != null && ModuleClass.elytraresolver.isEscaping()) {
            Vec3d targetPos = ModuleClass.elytraresolver.getEscapePos();
            double diffX = targetPos.x - mc.player.getX();
            double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double diffZ = targetPos.z - mc.player.getZ();
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

            float gcd = (mc.options.getMouseSensitivity().getValue().floatValue() * 0.6F + 0.2F);
            gcd = gcd * gcd * gcd * 1.2F;
            yaw -= yaw % gcd;
            pitch -= pitch % gcd;

            RotationStorage.update(new Rotation(yaw, pitch), 180, 180, 25, 20, 1, 1, clientLook.isState());
            return;
        }

        if (target == null) return;

        // во время записи не крутим камеру (ни серверно, ни спуфом),
        // иначе лейблы для обучения будут ротацией ауры, а не движением игрока
        if (NeuroRotation.INSTANCE.isRecording() || dataSystem.isRecording()) {
            return;
        }

        detectEnemyAttack();

        if (rotationType.is("Data") && target != lastDataTarget) {
            dataSystem.resetState();
            lastDataTarget = target;
        }

        if (isSnapRotationActive()) {
            updateSnapRotation(target);
            return;
        }

        RotationsSystem system;

        if (rotationType.is("Smooth")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    if (!mc.player.isGliding()) {
                        Vec3d relativePos = target.getPos().add(0, target.getHeight() * 0.6f, 0).subtract(mc.player.getEyePos());
                        final float yaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(relativePos.z, relativePos.x)) - 90);
                        float pitch = (float) (-Math.toDegrees(Math.atan2(relativePos.y, Math.hypot(relativePos.x, relativePos.z))));
                        RotationStorage.update(new Rotation(yaw, pitch), 360, 360, 360, 360, 1, 1, clientLook.isState());
                    } else {
                        Vec3d interpolatedRotation = Vec3d.fromPolar(target.getLerpTargetPitch(), target.getLerpTargetYaw());
                        Vec3d rotationVector = target.getRotationVector();
                        Vec3d relativePos = target.getPos().add(0, target.getHeight() * 0.6f, 0).subtract(mc.player.getEyePos());
                        Vec3d blendedDirection = interpolatedRotation.normalize().lerp(rotationVector, interpolatedRotation.length());
                        if (mc.player.isGliding() && target.isGliding() && ModuleClass.elytraTarget.isEnable()) {
                            relativePos = relativePos.add(blendedDirection.normalize().multiply(ModuleClass.elytraTarget.forward.getValue().floatValue()));
                        }
                        final float yaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(relativePos.z, relativePos.x)) - 90);
                        float pitch = (float) (-Math.toDegrees(Math.atan2(relativePos.y, Math.hypot(relativePos.x, relativePos.z))));
                        RotationStorage.update(new Rotation(yaw, pitch), 360, 360, 360, 360, 1, 1, clientLook.isState());
                    }
                }
            };
        } else if (rotationType.is("WellMine")) {
            system = wellMineRotation;
        } else if (rotationType.is("Test")) {
            system = testRotation;
        } else if (rotationType.is("Sloth")) {
            system = whiteRiseRotation;
            } else if (rotationType.is("Neuro")) {
            system = NeuroRotation.INSTANCE;
            } else if (rotationType.is("Shard")) {
            system = ShardRotation.INSTANCE;
        } else if (rotationType.is("LegitWorld")) {
            system = legitWorldRotation;
        } else if (rotationType.is("NoRotate")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    RotationStorage.update(new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()), MathUtils.random(100, 170), MathUtils.random(100, 170), MathUtils.random(100, 170), MathUtils.random(100, 170), 1, 6, false);
                }
            };
        } else if (rotationType.is("Data")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    boolean focusRotation = shouldFocusDataRotation();
                    Vec3d fallbackPoint = getDataRotationPoint(target);
                    Vec2f fallbackRot = RotationUtils.getRotations(fallbackPoint);
                    float currentYaw = mc.player.getYaw();
                    float currentPitch = mc.player.getPitch();
                    float yawDelta = Math.abs(MathHelper.wrapDegrees(fallbackRot.x - currentYaw));
                    float pitchDelta = Math.abs(fallbackRot.y - currentPitch);
                    boolean hardAcquire = yawDelta > 70.0f || (yawDelta > 42.0f && mc.player.squaredDistanceTo(target) < 9.0);
                    Rotation rotation = null;

                    if (shouldUseElytraPredict(target)) {
                        Vec2f rot = RotationUtils.getRotations(getPredictedPoint(target, fallbackPoint));
                        rotation = new Rotation(rot.x, rot.y);
                    } else if (!hardAcquire) {
                        rotation = dataSystem.getNeuroRotation(
                                target,
                                currentYaw,
                                currentPitch,
                                focusRotation
                        );
                    }

                    if (rotation == null || hardAcquire) {
                        if (hardAcquire) {
                            dataSystem.resetState();
                        }
                        rotation = new Rotation(fallbackRot.x, fallbackRot.y);
                    }

                    targetRotations = new Vec2f(rotation.getYaw(), rotation.getPitch());
                    currentRotations = new Vec2f(currentYaw, currentPitch);

                    float yawSpeed = hardAcquire ? Math.max(95.0f, Math.min(180.0f, yawDelta * 1.45f)) : (focusRotation ? 24.0f : 11.5f);
                    float pitchSpeed = hardAcquire ? Math.max(55.0f, Math.min(110.0f, Math.max(18.0f, pitchDelta * 1.35f))) : (focusRotation ? 18.0f : 9.0f);
                    float yawReturnSpeed = hardAcquire ? yawSpeed : (focusRotation ? 18.0f : 9.0f);
                    float pitchReturnSpeed = hardAcquire ? pitchSpeed : (focusRotation ? 14.0f : 7.0f);

                    RotationStorage.update(
                            rotation,
                            yawSpeed,
                            pitchSpeed,
                            yawReturnSpeed,
                            pitchReturnSpeed,
                            1,
                            1, clientLook.isState()
                    );
                }
            };
        } else {

            Vec2f targetRot;
            targetRot = RotationUtils.getRotations(getPredictedRotationPoint(target, target.getLeashPos(1)));


            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(new Rotation(targetRot.x, targetRot.y), 360, 360, 360, 360, 1, 1, clientLook.isState());
                }
            };
        }

        system.updateRotations(target);
    }

    private void updateSnapRotation(LivingEntity target) {
        Vec3d point = MultipointUtils.getClosestPoint(target);
        if (point == null) {
            point = target.getBoundingBox().getCenter();
        }

        Vec3d predicted = getPredictedRotationPoint(target, point);
        Vec2f targetRot = RotationUtils.getRotations(predicted);

        targetRotations = targetRot;
        currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());

        boolean isAttackSnapTick = mc.player.age <= snapAttackAge;

        float finalYaw = targetRot.x;
        float finalPitch = targetRot.y;
        if (!isAttackSnapTick) {
            if (shouldKeepRwWallPitchDown()) {
                finalPitch = 90.0f;
            } else {
                finalYaw = FreeLookStorage.getFreeYaw();
                finalPitch = FreeLookStorage.getFreePitch();
            }
        }

        RotationStorage.update(new Rotation(finalYaw, finalPitch), 360f, 360f, 360f, 360f, 0, 6, clientLook.isState());
    }

    private boolean isSnapRotationActive() {
        return rotationType.is("Snap") || isUsingRwWallSnap();
    }

    private boolean prepareSnapAttack() {
        if (!snapAttackQueued) {
            snapAttackQueued = true;
            snapAttackAge = mc.player.age + 1;
            snapAttackTarget = target;
            return false;
        }

        if (mc.player.age > snapAttackAge) {
            resetSnapAttack();
            return false;
        }

        return isSnapAimReadyForAttack();
    }

    private boolean shouldUseQueuedSnapAttack() {
        if (!snapAttackQueued || mc.player == null || target == null || target != snapAttackTarget) {
            return false;
        }

        if (mc.player.age > snapAttackAge + 1) {
            resetSnapAttack();
            return false;
        }

        return mc.player.age >= snapAttackAge;
    }

    private boolean isSnapAimReadyForAttack() {
        if (target == null || mc.player == null) {
            return false;
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotations.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(targetRotations.y - mc.player.getPitch());
        boolean onTarget = isUsingRwWallSnap()
                || (mc.player.isGliding() && target.isGliding());

        if (!onTarget) {
            EntityHitResult result = getAttackRaycastResult();
            onTarget = result != null && result.getEntity() == target;
        }

        return yawDiff <= 3.0f && pitchDiff <= 2.5f && onTarget;
    }

    private boolean isUsingRwWallSnap() {
        return rwWallBypass.isState() && target != null && isTargetBehindWall(target);
    }

    public boolean isEnemyAttackBreak() {
        return enemyAttackBreak;
    }

    public boolean isRwWallBypass() {
        return rwWallBypass.isState();
    }

    private void detectEnemyAttack() {
        if (target == null) {
            enemyAttackBreak = false;
            lastTargetAttackTicks = Integer.MAX_VALUE;
            return;
        }
        int tat = ((ILivingEntity) target).getLastAttackedTicks();
        if (tat < lastTargetAttackTicks) {
            enemyAttackBreak = true;
        } else if (tat > 14) {
            enemyAttackBreak = false;
        }
        lastTargetAttackTicks = tat;
    }

    private boolean isAimCloseToTarget() {
        if (target == null) return false;
        Vec2f rot = RotationUtils.getRotations(target.getBoundingBox().getCenter());
        float yawDiff = Math.abs(MathHelper.wrapDegrees(rot.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(rot.y - mc.player.getPitch());
        return yawDiff <= 8f && pitchDiff <= 6f;
    }

    private boolean shouldKeepRwWallPitchDown() {
        return isUsingRwWallSnap() && rwWallLookDown.isState();
    }

    private void sendSprintResetPacket(ClientCommandC2SPacket.Mode mode) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, mode));
    }

    private boolean shouldSkipSprintResetInWater() {
        return mc.player != null
                && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater())
                && Sprint.INSTANCE != null
                && Sprint.INSTANCE.shouldKeepSprintInWater();
    }

    private EntityHitResult getAttackRaycastResult() {
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        float reach = range.getValue().floatValue() * 2.0f;
        Vec3d reachVec = eyePos.add(lookVec.multiply(reach));

        return ProjectileUtil.raycast(
                mc.player,
                eyePos,
                reachVec,
                mc.player.getBoundingBox().expand(reach),
                ex -> ex != mc.player && ex.isAlive(),
                reach * reach
        );
    }

    public boolean isTargetBehindWall(LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null) {
            return false;
        }

        // рейкасты дорогие — вызываются несколько раз за тик, кэшируем на 2 тика
        int now = mc.player.age;
        if (entity == wallCheckEntity && now - wallCheckTick <= 1) {
            return wallCheckResult;
        }

        wallCheckResult = !mc.player.canSee(entity) || hasNarrowRwWallGap(entity);
        wallCheckEntity = entity;
        wallCheckTick = now;
        return wallCheckResult;
    }

    private boolean hasNarrowRwWallGap(LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null || !rwWallBypass.isState()) {
            return false;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double centerX = box.getCenter().x;
        double centerZ = box.getCenter().z;
        Vec3d[] points = new Vec3d[]{
                box.getCenter(),
                getStableBodyPoint(entity),
                new Vec3d(centerX, box.maxY - 0.08, centerZ),
                new Vec3d(centerX, box.minY + 0.12, centerZ),
                new Vec3d(box.minX + 0.04, box.minY + box.getLengthY() * 0.55, centerZ),
                new Vec3d(box.maxX - 0.04, box.minY + box.getLengthY() * 0.55, centerZ),
                new Vec3d(centerX, box.minY + box.getLengthY() * 0.55, box.minZ + 0.04),
                new Vec3d(centerX, box.minY + box.getLengthY() * 0.55, box.maxZ - 0.04)
        };

        int blocked = 0;
        int clear = 0;
        for (Vec3d point : points) {
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    eyePos,
                    point,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                blocked++;
            } else {
                clear++;
            }
        }

        return clear > 0 && blocked >= clear;
    }

    private Vec3d getPredictedRotationPoint(LivingEntity target, Vec3d point) {
        if (mc.player != null
                && target != null
                && mc.player.isGliding()
                && target.isGliding()
                && ModuleClass.elytraTarget != null
                && ModuleClass.elytraTarget.isEnable()) {
            return PredictUtils.bypasselytrahacking(target);
        }
        return point;
    }

    private Vec3d getDataRotationPoint(LivingEntity target) {
        if (isInsideOrNearHitbox(target)) {
            return getPredictedRotationPoint(target, getStableBodyPoint(target));
        }
        Vec3d point = BestPoint.getNearestPoint(target);
        if (point == null) {
            point = MultipointUtils.getClosestPoint(target);
        }
        if (point == null) {
            point = getStableBodyPoint(target);
        }
        return getPredictedRotationPoint(target, point);
    }

    private boolean isInsideOrNearHitbox(LivingEntity target) {
        if (mc.player == null || target == null) {
            return false;
        }
        Vec3d eyePos = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        if (box.expand(0.12).contains(eyePos)) {
            return true;
        }
        Vec3d stablePoint = getStableBodyPoint(target);
        return eyePos.squaredDistanceTo(stablePoint) <= 2.25;
    }

    private Vec3d getStableBodyPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        return new Vec3d(
                box.getCenter().x,
                box.minY + box.getLengthY() * 0.72,
                box.getCenter().z
        );
    }

    private LivingEntity findTarget() {
        List<LivingEntity> entities = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;
            entities.add(living);
        }

        if (entities.isEmpty() || !isEnable()) return null;

        switch (priority.getCurrent()) {
            case "Дистанция" ->
                    entities.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().squaredDistanceTo(mc.player.getEyePos())));
            case "Здоровье" -> entities.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case "Угол" -> entities.sort(Comparator.comparingDouble(entity -> {
                Vec2f vec = RotationUtils.getRotations(entity.getBoundingBox().getCenter());
                double dy = Math.abs(wrapDegrees(vec.x - mc.player.getYaw()));
                double dp = Math.abs(wrapDegrees(vec.y - mc.player.getPitch()));
                return dy + dp;
            }));
            case "Никакой" -> {
            }
        }

        return entities.isEmpty() ? null : entities.get(0);
    }

    private void updateTarget() {
        if (!isEnable()) {
            target = null;
            return;
        }

        if (target != null && isValidTarget(target)) {
            return;
        }

        target = findTarget();
    }

    private boolean shouldFocusDataRotation() {
        float cooldown = mc.player.getAttackCooldownProgress(1.5f);
        float focusThreshold = Math.max(0.82f, IdealHitUtils.getAICooldown() - 0.08f);
        boolean readyByCooldown = cooldown >= focusThreshold;
        boolean fallingForCrit = !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0
                && mc.player.fallDistance > 0.0f;

        return readyByCooldown || fallingForCrit;
    }


    private void attack() {
        if (!prepareThroughWallAttack()) {
            cps = System.currentTimeMillis() + 250L;
            return;
        }

        if (shouldUseWallCritBypass()) {
            sendWallCritPackets();
        }

        Hand blockedShieldHand = null;
        if (unpressShield.isState()) {
            blockedShieldHand = getBlockingShieldHand();
            if (blockedShieldHand != null) {
                mc.interactionManager.stopUsingItem(mc.player);
            }
        }

        boolean attacked = false;
        if (target instanceof PlayerEntity player && player.isBlocking() && breakShield.isState()) {
            attacked = shieldBreak(player);
        }

        if (!attacked) {
            mc.interactionManager.attackEntity(mc.player, target);
            if (ModuleClass.elytraresolver != null) {
                ModuleClass.elytraresolver.onAuraAttack();
            }
        }
        mc.player.swingHand(Hand.MAIN_HAND);

        if (blockedShieldHand != null) {
            restoreShieldBlocking(blockedShieldHand);
        }

        if (rotationType.is("WhiteRise")) slothRotation.onAttack();
        if (rotationType.is("Sloth")) whiteRiseRotation.onAttack();

        long cooldown = 467L;

        if (tpsSync != null && tpsSync.isEnable()) {
            cooldown = (long) (tpsSync.getAdjustedCooldown(cooldown) * 1.1f);
        }

        cps = System.currentTimeMillis() + cooldown;
        ticksToAttack = 10;
        attackTimer.reset();
        enemyAttackBreak = false;
    }

    private boolean shouldUseWallCritBypass() {
        return wallCritBypass.isState()
                && rotationType.is("LegitWorld")
                && mc.player != null
                && mc.world != null
                && !mc.player.isTouchingWater()
                && !mc.player.isSubmergedInWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.hasVehicle()
                && !mc.player.getAbilities().flying
                && isPlayerInsideBlocks();
    }

    private boolean isPlayerInsideBlocks() {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        int now = mc.player.age;
        if (now - insideBlocksTick <= 10) {
            return insideBlocksResult;
        }

        boolean result = false;
        Box box = mc.player.getBoundingBox().contract(0.005);
        for (BlockPos pos : BlockPos.iterate(
                MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ),
                MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ))) {
            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (shape.isEmpty()) continue;
            if (box.intersects(shape.getBoundingBox().offset(pos))) {
                result = true;
                break;
            }
        }

        insideBlocksResult = result;
        insideBlocksTick = now;
        return result;
    }

    private void sendWallCritPackets() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        // троттлинг: лишние move-пакеты в один тик = кик за спам движений
        long now = System.currentTimeMillis();
        if (now - lastWallCritPackets < 420L) {
            return;
        }
        lastWallCritPackets = now;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        double jump = 0.104 + MathUtils.random(0f, 0.03f);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + jump, z, false, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
    }

    private boolean prepareThroughWallAttack() {
        if (target == null || mc.player == null || mc.world == null || mc.player.networkHandler == null) return true;
        if (mc.player.canSee(target)) return true;

        Vec3d start = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        Vec3d center = box.getCenter();
        Vec3d[] aimPoints = new Vec3d[]{
                center,
                getStableBodyPoint(target),
                new Vec3d(center.x, box.minY + box.getLengthY() * 0.45, center.z),
                new Vec3d(center.x, box.maxY - 0.1, center.z)
        };

        BlockPos blockPos = null;
        Direction direction = Direction.UP;
        for (Vec3d point : aimPoints) {
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    start,
                    point,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                blockPos = hit.getBlockPos();
                direction = hit.getSide() == null ? Direction.UP : hit.getSide();
                break;
            }
        }

        if (blockPos == null) return true;
        if (mc.world.getBlockState(blockPos).isAir()) return true;
        if (mc.world.getBlockState(blockPos).getHardness(mc.world, blockPos) < 0.0f) return false;

        // не спамим диг-пакеты: на многих АЦ это мгновенный кик
        Vec2f blockRot = RotationUtils.getRotations(Vec3d.ofCenter(blockPos));
        float yawDiff = Math.abs(MathHelper.wrapDegrees(blockRot.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(blockRot.y - mc.player.getPitch());
        if (yawDiff > 45f || pitchDiff > 35f) {
            return true;
        }

        long now = System.currentTimeMillis();
        boolean sameSpot = blockPos.equals(lastWallDigPos) && direction == lastWallDigDir;
        if (sameSpot && now - lastWallDigTime < 650L) {
            return true;
        }

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
        lastWallDigPos = blockPos;
        lastWallDigDir = direction;
        lastWallDigTime = now;
        return true;
    }

    private boolean shieldBreak(PlayerEntity entity) {
        SlotSearchResult axeSlot = HotbarUtil.getAxe();
        if (!axeSlot.found()) {
            return false;
        }

        int previousSlot = mc.player.getInventory().selectedSlot;
        if (axeSlot.slot() == previousSlot) {
            mc.interactionManager.attackEntity(mc.player, entity);
            if (ModuleClass.elytraresolver != null) {
                ModuleClass.elytraresolver.onAuraAttack();
            }
            return true;
        }

        if (axeSlot.isInHotBar()) {
            return attackWithSilentHotbarSlot(entity, axeSlot.slot(), previousSlot);
        }

        if (mc.player.currentScreenHandler.syncId != 0) {
            return false;
        }

        int swapHotbarSlot = findSilentSwapHotbarSlot(previousSlot);
        if (swapHotbarSlot == -1) {
            return false;
        }

        swapInventoryIntoHotbar(axeSlot.slot(), swapHotbarSlot);
        boolean attacked = false;
        try {
            attacked = attackWithSilentHotbarSlot(entity, swapHotbarSlot, previousSlot);
        } finally {
            swapInventoryIntoHotbar(axeSlot.slot(), swapHotbarSlot);
        }
        return attacked;
    }

    private boolean attackWithSilentHotbarSlot(PlayerEntity entity, int attackSlot, int previousSlot) {
        if (mc.player == null || mc.player.networkHandler == null || mc.interactionManager == null) {
            return false;
        }

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(attackSlot));
        try {
            mc.interactionManager.attackEntity(mc.player, entity);
            if (ModuleClass.elytraresolver != null) {
                ModuleClass.elytraresolver.onAuraAttack();
            }
            return true;
        } finally {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        }
    }

    private void swapInventoryIntoHotbar(int inventorySlot, int hotbarSlot) {
        if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, inventorySlot, hotbarSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    private int findSilentSwapHotbarSlot(int previousSlot) {
        for (int slot = 8; slot >= 0; slot--) {
            if (slot != previousSlot) {
                return slot;
            }
        }
        return previousSlot >= 0 && previousSlot < 9 ? previousSlot : -1;
    }

    private Hand getBlockingShieldHand() {
        if (mc.player == null || !mc.player.isBlocking()) {
            return null;
        }

        Hand activeHand = mc.player.getActiveHand();
        if (activeHand == null) {
            return null;
        }

        return isShieldStack(mc.player.getStackInHand(activeHand)) ? activeHand : null;
    }

    private boolean shouldBlockAttackWhileUsingItem() {
        if (mc.player == null || !mc.player.isUsingItem()) {
            return false;
        }

        return !(unpressShield.isState() && getBlockingShieldHand() != null);
    }

    private void restoreShieldBlocking(Hand hand) {
        if (mc.player == null || mc.interactionManager == null || hand == null) {
            return;
        }

        if (!isShieldStack(mc.player.getStackInHand(hand)) || mc.player.isUsingItem()) {
            return;
        }

        mc.interactionManager.interactItem(mc.player, hand);
    }

    private boolean isShieldStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
    }

    private boolean isWeapon() {
        Item item = mc.player.getMainHandStack().getItem();
        return item != Items.AIR && (item instanceof SwordItem
                || item instanceof PickaxeItem
                || item instanceof AxeItem
                || item instanceof HoeItem
                || item instanceof ShovelItem
                || item instanceof MaceItem
                || item == Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;

        // дешёвая отсечка ДО дорогих проверок (AntiBot, BestPoint и т.д.)
        double maxRange = getMaxAimRange() + 2.0;
        if (entity.squaredDistanceTo(mc.player) > maxRange * maxRange) return false;

        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof IronGolemEntity || entity instanceof BatEntity) return false;

        if (AntiBot.checkBot(entity)) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targets.is("Игроки")) return false;
            if (isNaked(player) && !targets.is("Голые")) return false;
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !targets.is("Невидимки")) return false;
            if (Neural.INSTANCE.friendStorage.isFriend(entity.getName().getString())) return false;
        } else if (entity instanceof PassiveEntity || entity instanceof CodEntity) {
            if (!targets.is("Мирные")) return false;
        } else if (entity instanceof HostileEntity) {
            if (!targets.is("Мобы")) return false;
        } else {
            return false;
        }

        Vec3d nearestPoint = BestPoint.getNearestPoint(entity);
        if (nearestPoint == null) nearestPoint = MultipointUtils.getClosestPoint(entity);
        if (mc.player.getEyePos().distanceTo(nearestPoint) > getMaxAimRange()) return false;
        if (!throughWalls.isState() && !rwWallBypass.isState() && !mc.player.canSee(entity)) return false;

        return true;
    }

    private boolean isNaked(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            if (!armorStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldAttack() {
        if (mc.player.getAttackCooldownProgress(1.5f) < IdealHitUtils.getAICooldown()) {
            return false;
        }

        EntityHitResult result = getAttackRaycastResult();

        if (raycast.isState() && !isUsingRwWallSnap()
                && (result != null ? result.getEntity() == null || result.getEntity() != target : true)
                && !(mc.player.isGliding() && target.isGliding())) {
            return false;
        }

        if (rotationType.is("Data") && !isUsingRwWallSnap() && !isDataAimReady(result)) {
            return false;
        }

        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);
            int predict = 0;
            if (ModuleClass.elytraTarget != null && ModuleClass.elytraTarget.isEnable()) {
                predict = ModuleClass.elytraTarget.forward.getValue().intValue();
            }
            Vec3d predictedPos = PredictUtils.predict(target, targetPos, predict);
            if (mc.player.getEyePos().distanceTo(predictedPos) > 5) return false;
        } else {
            double distanceCheck = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            Vec3d checkPoint = distanceCheck > 3 ? BestPoint.getNearestPoint(target) : target.getBoundingBox().getCenter();
            if (checkPoint == null) checkPoint = MultipointUtils.getClosestPoint(target);
            if (mc.player.getEyePos().distanceTo(checkPoint) > range.getValue().floatValue()) return false;
        }

        if (shouldUseWallCritBypass()) {
            return true;
        }

        return IdealHitUtils.canCritical(target);
    }

    public int getWhiteRiseTicksToAttack() {
        return ticksToAttack;
    }

    private boolean isDataAimReady(EntityHitResult result) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotations.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(targetRotations.y - mc.player.getPitch());
        boolean closeToAim = yawDiff <= 1.15f && pitchDiff <= 0.9f;
        boolean onTarget = result != null && result.getEntity() == target;

        return closeToAim && onTarget;
    }

    public boolean isAboveWater() {
        BlockPos pos = BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0));
        return !mc.player.isSubmergedInWater() && mc.world.getBlockState(pos).isOf(Blocks.WATER);
    }

    public float getAttackCooldown() {
        return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
    }

    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20);
    }

    private float getMaxAimRange() {
        return mc.player.isGliding()
                ? elytraAimRange.getValue().floatValue()
                : range.getValue().floatValue() + aimRange.getValue().floatValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (target != null) backTimer.reset();
        target = null;
        resetWallDigState();
        wellMineRotation.reset();
        legitWorldRotation.reset();
        testRotation.reset();
        slothRotation.reset();
        whiteRiseRotation.reset();
        dataSystem.resetState();
        lastDataTarget = null;
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        ticksToAttack = 0;
        resetSnapAttack();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetWallDigState();
        wellMineRotation.reset();
        testRotation.reset();
        legitWorldRotation.reset();
        slothRotation.reset();
        whiteRiseRotation.reset();
        dataSystem.resetState();
        lastDataTarget = null;
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        ticksToAttack = 0;
        enemyAttackBreak = false;
        lastTargetAttackTicks = Integer.MAX_VALUE;
        resetSnapAttack();
        if (mc.player != null) {
            currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    private void resetSnapAttack() {
        snapAttackAge = -1;
        snapAttackQueued = false;
        snapAttackTarget = null;
    }

    private void resetWallDigState() {
        lastWallDigPos = null;
        lastWallDigDir = null;
        lastWallDigTime = 0L;
        lastWallCritPackets = 0L;
    }
}

