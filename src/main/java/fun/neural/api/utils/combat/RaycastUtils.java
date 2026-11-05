package fun.neural.api.utils.combat;

import fun.neural.api.QClient;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

@UtilityClass
public class RaycastUtils implements QClient {

    public BlockHitResult raycastBlock(BlockPos targetPos, double maxDistance) {
        if (mc.player == null || mc.world == null) return null;
        return raycastBlock(mc.player, targetPos, maxDistance);
    }

    public BlockHitResult raycastBlock(float yaw, float pitch, BlockPos targetPos, double maxDistance) {
        if (mc.player == null) return null;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector(pitch, yaw);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        Box blockBox = new Box(targetPos);
        Optional<Vec3d> hitOpt = blockBox.raycast(eyePos, endPos);
        if (hitOpt.isEmpty()) return null;
        Vec3d hitVec = hitOpt.get();
        Direction side = getHitSide(hitVec, targetPos);
        return new BlockHitResult(hitVec, side, targetPos, false);
    }

    public BlockHitResult raycastBlock(PlayerEntity player, BlockPos targetPos, double maxDistance) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        Box blockBox = new Box(targetPos);
        Optional<Vec3d> hitOpt = blockBox.raycast(eyePos, endPos);
        if (hitOpt.isEmpty()) return null;
        Vec3d hitVec = hitOpt.get();
        Direction side = getHitSide(hitVec, targetPos);
        return new BlockHitResult(hitVec, side, targetPos, false);
    }

    public boolean canReachBox(Box box, double maxDistance) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d closestPoint = getClosestPointOnBox(eyePos, box);
        double distanceSq = eyePos.squaredDistanceTo(closestPoint);
        return distanceSq <= maxDistance * maxDistance;
    }

    public boolean isLookingAtEntity(Entity entity, double maxDistance) {
        if (mc.player == null || entity == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        Box entityBox = entity.getBoundingBox();
        return entityBox.raycast(eyePos, endPos).isPresent() || entityBox.contains(eyePos);
    }

    public boolean isLookingAtEntity(float yaw, float pitch, Entity entity, double maxDistance) {
        if (mc.player == null || entity == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector(pitch, yaw);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        Box entityBox = entity.getBoundingBox();
        return entityBox.raycast(eyePos, endPos).isPresent() || entityBox.contains(eyePos);
    }

    public boolean rayTrace(Vec3d clientVec, double range, Box box) {
        if (mc.player == null) return false;
        Vec3d cameraVec = mc.player.getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }

    public boolean rayTrace(float yaw, float pitch, double range, Box box) {
        if (mc.player == null) return false;
        Vec3d cameraVec = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVector(pitch, yaw);
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(dir.multiply(range))).isPresent();
    }

    private Vec3d getClosestPointOnBox(Vec3d point, Box box) {
        double x = clamp(point.x, box.minX, box.maxX);
        double y = clamp(point.y, box.minY, box.maxY);
        double z = clamp(point.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Direction getHitSide(Vec3d hitVec, BlockPos blockPos) {
        double dx = hitVec.x - (blockPos.getX() + 0.5);
        double dy = hitVec.y - (blockPos.getY() + 0.5);
        double dz = hitVec.z - (blockPos.getZ() + 0.5);

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        if (absDx > absDy && absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
