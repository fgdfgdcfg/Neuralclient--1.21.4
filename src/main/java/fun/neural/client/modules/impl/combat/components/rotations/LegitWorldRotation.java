package fun.neural.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import fun.neural.api.QClient;
import fun.neural.api.storages.implement.RotationStorage;
import fun.neural.api.utils.math.MathUtils;
import fun.neural.api.utils.rotate.Rotation;
import fun.neural.api.utils.rotate.RotationUtils;
import fun.neural.client.modules.impl.combat.Aura;
import fun.neural.client.modules.impl.combat.components.RotationsSystem;

import java.util.concurrent.ThreadLocalRandom;

public class LegitWorldRotation extends RotationsSystem implements QClient {

    private Vec3d lastTargetPos = null;
    private Vec3d predictedVelocity = Vec3d.ZERO;

    @Override
    public void updateRotations(LivingEntity target) {
        Vec3d currentPos = target.getPos();
        if (lastTargetPos != null) {
            Vec3d delta = currentPos.subtract(lastTargetPos);
            predictedVelocity = predictedVelocity.lerp(delta, 0.5);
        }
        lastTargetPos = currentPos;

        boolean behindWall = Aura.INSTANCE.isTargetBehindWall(target);
        boolean wallBypass = Aura.INSTANCE.isRwWallBypass();
        boolean breakAttack = Aura.INSTANCE.isEnemyAttackBreak();

        // при включённом обходе стен целимся сквозь стену в центр цели на обычной скорости
        boolean effectiveBehindWall = behindWall && !wallBypass;

        Vec3d aimPoint = getAimPoint(target, effectiveBehindWall);

        Vec2f rot = RotationUtils.getRotations(aimPoint);

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = MathHelper.wrapDegrees(rot.x - currentYaw);
        float diffPitch = MathHelper.wrapDegrees(rot.y - currentPitch);

        float errorYaw = ThreadLocalRandom.current().nextFloat(-0.35f, 0.35f);
        float errorPitch = ThreadLocalRandom.current().nextFloat(-0.25f, 0.25f);

        float factorYaw;
        float factorPitch;
        float maxYaw;
        float maxPitch;
        if (breakAttack) {
            factorYaw = MathUtils.random(0.8f, 0.95f);
            factorPitch = MathUtils.random(0.8f, 0.95f);
            maxYaw = MathUtils.random(110f, 150f);
            maxPitch = MathUtils.random(90f, 130f);
        } else if (effectiveBehindWall) {
            // за стеной целимся в ближайшую точку хитбокса и двигаем камеру медленно,
            // чтобы античит не видел резких досягаемых поворотов
            factorYaw = MathUtils.random(0.26f, 0.42f);
            factorPitch = MathUtils.random(0.24f, 0.38f);
            maxYaw = MathUtils.random(38f, 62f);
            maxPitch = MathUtils.random(28f, 46f);
        } else {
            factorYaw = MathUtils.random(0.14f, 0.26f);
            factorPitch = MathUtils.random(0.12f, 0.22f);
            maxYaw = MathUtils.random(20f, 32f);
            maxPitch = MathUtils.random(15f, 24f);
        }

        float stepYaw = MathHelper.clamp(diffYaw * factorYaw, -maxYaw, maxYaw);
        float stepPitch = MathHelper.clamp(diffPitch * factorPitch, -maxPitch, maxPitch);

        if (effectiveBehindWall && !breakAttack
                && Math.abs(diffYaw) < 35f
                && ThreadLocalRandom.current().nextFloat() < 0.10f) {
            stepYaw *= 0.2f;
            stepPitch *= 0.2f;
        }

        float newYaw = currentYaw + stepYaw + errorYaw;
        float newPitch = MathHelper.clamp(currentPitch + stepPitch + errorPitch, -90f, 90f);

        Vec2f corrected = correctRotation(newYaw, newPitch);
        Rotation rotation = new Rotation(corrected.x, corrected.y);

        RotationStorage.update(
                rotation,
                maxYaw, maxPitch,
                maxYaw, maxPitch,
                1, 6,
                Aura.clientLook.isState()
        );
    }

    private Vec3d getAimPoint(LivingEntity target, boolean behindWall) {
        Box box = target.getBoundingBox();

        if (behindWall) {
            Vec3d closest = fun.neural.api.utils.rotate.MultipointUtils.getClosestPoint(target);
            if (closest != null) {
                return closest.add(predictedVelocity.multiply(1.2));
            }
        }

        Vec3d center = box.getCenter();
        Vec3d point = new Vec3d(center.x, box.minY + box.getLengthY() * 0.72, center.z);
        point = point.add(predictedVelocity.multiply(3));
        return point;
    }

    public void reset() {
        lastTargetPos = null;
        predictedVelocity = Vec3d.ZERO;
    }
}
