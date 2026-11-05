package fun.neural.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import fun.neural.api.events.EventInvoker;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventRotation;
import fun.neural.api.utils.rotate.RotationUtils;
import fun.neural.client.modules.impl.combat.Aura;
import fun.neural.client.modules.impl.combat.components.RotationsSystem;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class ShardRotation extends RotationsSystem {
    public static final ShardRotation INSTANCE;

    private static final float PX = 0.15F;
    private static final float YAW_CAP = 9.0F;
    private static final float PITCH_CAP = 4.0F;
    private static final float WHIP_YAW_CAP = 17.0F;

    private enum Phase { REACT, SACCADE, HOLD }

    static {
        ShardRotation rotation = new ShardRotation();
        INSTANCE = rotation;
        EventInvoker.register(rotation);
    }

    private boolean active;
    private boolean wasActive;

    private float visualYaw;
    private float visualPitch;

    private Phase phase = Phase.REACT;
    private LivingEntity lastTarget;
    private boolean whipUsed;

    private int reactTicks;

    private int saccadeProgress;
    private int saccadeLength;
    private float[] saccadeWeights;
    private float saccadeWeightSum;
    private float saccadeTotalYaw;
    private float saccadeTotalPitch;
    private boolean whipTick;
    private Vec3d aimPoint;

    private ShardRotation() {
    }

    public void reset() {
        wasActive = false;
        active = false;
        aimPoint = null;
        saccadeWeights = null;
        phase = Phase.REACT;
    }

    // камера крутится визуально, серверу не шлётся ничего
    @EventLink
    public void onRotation(EventRotation event) {
        if (!active) return;
        if (Aura.INSTANCE == null || !Aura.INSTANCE.isEnable()) return;
        if (!Aura.INSTANCE.rotationType.is("Shard")) return;
        if (Aura.INSTANCE.getTarget() == null) return;
        event.setYaw(visualYaw);
        event.setPitch(visualPitch);
    }

    private Vec3d randomMultipoint(LivingEntity target) {
        Box box = getPredictedBox(target);
        float expandX = ThreadLocalRandom.current().nextFloat(0.1f, 0.45f);
        float expandY = ThreadLocalRandom.current().nextFloat(0.0f, 0.3f);
        float expandZ = ThreadLocalRandom.current().nextFloat(0.1f, 0.45f);

        float rx = ThreadLocalRandom.current().nextFloat(-expandX, expandX);
        float ry = ThreadLocalRandom.current().nextFloat(-expandY, expandY);
        float rz = ThreadLocalRandom.current().nextFloat(-expandZ, expandZ);

        float midY = (float) (box.minY + box.maxY) / 2.0f;
        return new Vec3d(
                box.minX + (box.maxX - box.minX) / 2.0 + rx,
                midY + ry,
                box.minZ + (box.maxZ - box.minZ) / 2.0 + rz
        );
    }

    private static float[] bellWeights(int n, int peak) {
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            if (i <= peak) {
                w[i] = 0.5f - 0.5f * (float) Math.cos(Math.PI * i / peak);
            } else {
                w[i] = 0.5f + 0.5f * (float) Math.cos(Math.PI * (i - peak) / (n - peak));
            }
        }
        return w;
    }

    private void planSaccade(float totalYaw, float totalPitch, boolean small) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        float mag = (float) Math.hypot(totalYaw * 0.75f, totalPitch);
        int n;
        if (small) {
            n = MathHelper.clamp(Math.round(1.6f + mag * 0.85f), 2, 5);
        } else {
            n = MathHelper.clamp(Math.round(1.5f + mag * 0.7f), 3, 7);
        }

        float guard = 0.0f;
        while (true) {
            int peak = MathHelper.clamp(Math.round(n * rng.nextFloat(0.35f, 0.62f)), Math.max(1, n / 2), Math.max(1, n - 1));
            saccadeWeights = bellWeights(n, peak);
            saccadeWeightSum = 0.0f;
            for (float v : saccadeWeights) {
                saccadeWeightSum += v;
            }
            float maxFrac = 0.0f;
            for (float v : saccadeWeights) {
                maxFrac = Math.max(maxFrac, v / saccadeWeightSum);
            }
            int need = Math.max(
                    (int) Math.ceil(Math.abs(totalYaw) * maxFrac / YAW_CAP),
                    (int) Math.ceil(Math.abs(totalPitch) * maxFrac / PITCH_CAP)
            );
            if (need <= n) {
                break;
            }
            n = Math.min(need, small ? 6 : 8);
            if (++guard > 6) {
                n = small ? 6 : 8;
                break;
            }
        }

        saccadeLength = n;
        saccadeTotalYaw = totalYaw;
        saccadeTotalPitch = totalPitch;
        saccadeProgress = 0;

        whipTick = false;
        if (!small && Math.abs(totalYaw) > 42.0f && !whipUsed && rng.nextFloat() < 0.35f) {
            whipTick = true;
            whipUsed = true;
        }
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (mc.player == null || target == null) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        if (!wasActive) {
            wasActive = true;
            active = true;
            visualYaw = mc.player.getYaw();
            visualPitch = mc.player.getPitch();
            phase = Phase.REACT;
            reactTicks = rng.nextInt(2, 5);
            aimPoint = null;
            whipUsed = false;
            lastTarget = target;
            swingAimPoint(target, rng);
        }

        if (lastTarget != target) {
            lastTarget = target;
            phase = Phase.REACT;
            reactTicks = rng.nextInt(2, 5);
            aimPoint = null;
            swingAimPoint(target, rng);
        }

        switch (phase) {
            case REACT:
                if (--reactTicks <= 0) {
                    startSaccade(target, rng, false);
                } else {
                    holdEmit(rng);
                }
                break;
            case SACCADE:
                stepSaccade(rng);
                break;
            case HOLD:
                stepHold(target, rng);
                break;
        }

        if (aimPoint != null && Aura.INSTANCE != null) {
            Aura.INSTANCE.setSilentRotation(
                    new Vec2f(visualYaw, visualPitch),
                    RotationUtils.getRotations(aimPoint)
            );
        }
    }

    private void swingAimPoint(LivingEntity target, ThreadLocalRandom rng) {
        if (aimPoint == null || rng.nextFloat() < 0.4f) {
            aimPoint = randomMultipoint(target);
        }
    }

    private void startSaccade(LivingEntity target, ThreadLocalRandom rng, boolean small) {
        swingAimPoint(target, rng);
        Vec2f rot = RotationUtils.getRotations(aimPoint);
        float totalYaw = wrapDegrees(rot.x - visualYaw);
        float totalPitch = MathHelper.clamp(rot.y - visualPitch, -10.0f, 10.0f);
        planSaccade(totalYaw, totalPitch, small);
        phase = Phase.SACCADE;
    }

    private void stepSaccade(ThreadLocalRandom rng) {
        if (saccadeWeights == null || saccadeProgress >= saccadeLength) {
            phase = Phase.HOLD;
            return;
        }

        float frac = saccadeWeights[saccadeProgress] / saccadeWeightSum;
        float cap = whipTick && saccadeProgress == 0 ? WHIP_YAW_CAP : YAW_CAP;

        float dy = MathHelper.clamp(saccadeTotalYaw * frac, -cap, cap);
        float dp = MathHelper.clamp(saccadeTotalPitch * frac, -PITCH_CAP, PITCH_CAP);

        dy = applyPixels(dy);
        dp = applyPixels(dp);
        saccadeProgress++;

        visualYaw = wrapDegrees(visualYaw + dy);
        visualPitch = MathHelper.clamp(visualPitch + dp, -89.9F, 89.9F);
    }

    private void stepHold(LivingEntity target, ThreadLocalRandom rng) {
        Vec3d point = aimPoint != null ? aimPoint : getPredictedBox(target).getCenter();
        Vec2f rot = RotationUtils.getRotations(point);
        float errYaw = wrapDegrees(rot.x - visualYaw);
        float errPitch = rot.y - visualPitch;

        if (Math.abs(errYaw) > 1.5f || Math.abs(errPitch) > 1.0f) {
            startSaccade(target, rng, true);
            return;
        }

        float dy = microTrack(errYaw, rng);
        float dp = microTrack(errPitch, rng);
        dy += tremor(false, rng);
        dp += tremor(true, rng);

        visualYaw = wrapDegrees(visualYaw + dy);
        visualPitch = MathHelper.clamp(visualPitch + dp, -89.9F, 89.9F);
    }

    private void holdEmit(ThreadLocalRandom rng) {
        float dy = tremor(false, rng);
        float dp = tremor(true, rng);
        visualYaw = wrapDegrees(visualYaw + dy);
        visualPitch = MathHelper.clamp(visualPitch + dp, -89.9F, 89.9F);
    }

    private static float tremor(boolean pitch, ThreadLocalRandom rng) {
        float p = pitch ? 0.14f : 0.20f;
        if (rng.nextFloat() >= p) return 0.0F;
        return (rng.nextBoolean() ? PX : -PX);
    }

    private static float microTrack(float err, ThreadLocalRandom rng) {
        float abs = Math.abs(err);
        if (abs < 0.2f) return 0.0F;
        float p = Math.min(0.96f, abs / 0.75f);
        if (rng.nextFloat() >= p) return 0.0F;
        int px = rng.nextFloat() < 0.25f ? 2 : 1;
        return Math.copySign(px * PX, err);
    }

    private static float applyPixels(float degrees) {
        if (degrees == 0.0F) return 0.0F;
        float abs = Math.abs(degrees);
        int px = Math.max(1, Math.round(abs / PX));
        return Math.copySign(px * PX, degrees);
    }
}