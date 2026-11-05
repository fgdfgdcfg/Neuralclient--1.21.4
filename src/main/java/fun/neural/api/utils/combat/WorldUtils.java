package fun.neural.api.utils.combat;

import fun.neural.api.QClient;
import fun.neural.api.utils.rotate.RotationUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@UtilityClass
public class WorldUtils implements QClient {

    public Direction getBestVisibleFaceToObsidian(BlockPos pos, PlayerEntity entity) {
        Direction bestFace = null;
        double bestDist = 0;

        for (Direction face : Direction.values()) {
            BlockPos sidePos = pos.offset(face);

            if (!mc.world.getBlockState(sidePos).isAir()) {
                continue;
            }

            Vec3d point = getFacePoint(pos, face);
            Vec2f rot = RotationUtils.getRotations(point);

            if (RaycastUtils.raycastBlock(rot.x, rot.y, pos, 4.5F) == null) {
                continue;
            }

            double damage = CrystalDamageCalculator.getDamageOfGhostBlock(sidePos.up().toCenterPos(), entity, sidePos);
            if (damage > bestDist) {
                bestDist = damage;
                bestFace = face;
            }
        }

        return bestFace;
    }

    public Direction getBestVisibleFace(BlockPos pos, PlayerEntity entity) {
        Vec3d eye = entity.getEyePos();
        Direction bestFace = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction face : Direction.values()) {
            Vec3d point = getFacePoint(pos, face);
            Vec2f rot = RotationUtils.getRotations(point);

            if (RaycastUtils.raycastBlock(rot.x, rot.y, pos, 4.5F) == null) {
                continue;
            }

            double dist = eye.squaredDistanceTo(point);
            if (dist < bestDist) {
                bestDist = dist;
                bestFace = face;
            }
        }

        return bestFace;
    }

    public Vec3d getFacePoint(BlockPos pos, Direction face) {
        return new Vec3d(
                pos.getX() + 0.5 + face.getOffsetX() * 0.5001,
                pos.getY() + 0.5 + face.getOffsetY() * 0.5001,
                pos.getZ() + 0.5 + face.getOffsetZ() * 0.5001
        );
    }
}
