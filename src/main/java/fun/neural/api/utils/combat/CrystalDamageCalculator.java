package fun.neural.api.utils.combat;

import fun.neural.api.QClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Objects;

public final class CrystalDamageCalculator implements QClient {

    public static boolean terrainIgnore = false;

    public static float getExplosionDamage(Vec3d explosionPos, PlayerEntity target, boolean optimized) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0f;

        if (!new Box(MathHelper.floor(explosionPos.x - 11), MathHelper.floor(explosionPos.y - 11), MathHelper.floor(explosionPos.z - 11),
                MathHelper.floor(explosionPos.x + 13), MathHelper.floor(explosionPos.y + 13), MathHelper.floor(explosionPos.z + 13))
                .intersects(target.getBoundingBox()))
            return 0f;

        if (!target.isInvulnerable()) {
            double distExposure = target.squaredDistanceTo(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
                terrainIgnore = false;
                double exposure = getExposure(explosionPos, target.getBoundingBox(), optimized);
                terrainIgnore = false;
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);

                if (mc.world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                DamageSource source = mc.world.getDamageSources().explosion(null, null);
                toDamage = DamageUtil.getDamageLeft(target, toDamage, source, target.getArmor(),
                        (float) target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));

                toDamage = applyResistance(target, toDamage);
                toDamage = applyProtection(target, toDamage);

                return toDamage;
            }
        }
        return 0f;
    }

    public static float getDamageOfGhostBlock(Vec3d explosionPos, PlayerEntity target, BlockPos bp) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0f;

        double maxDist = 12;
        if (!new Box(MathHelper.floor(explosionPos.x - maxDist - 1.0), MathHelper.floor(explosionPos.y - maxDist - 1.0),
                MathHelper.floor(explosionPos.z - maxDist - 1.0), MathHelper.floor(explosionPos.x + maxDist + 1.0),
                MathHelper.floor(explosionPos.y + maxDist + 1.0), MathHelper.floor(explosionPos.z + maxDist + 1.0))
                .intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (!target.isInvulnerable()) {
            double distExposure = target.squaredDistanceTo(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
                terrainIgnore = true;
                double exposure = getExposureGhost(explosionPos, target, bp);
                terrainIgnore = false;
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

                if (mc.world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                DamageSource source = mc.world.getDamageSources().explosion(null, null);
                toDamage = DamageUtil.getDamageLeft(target, toDamage, source, target.getArmor(),
                        (float) target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));

                toDamage = applyResistance(target, toDamage);
                toDamage = applyProtection(target, toDamage);

                return toDamage;
            }
        }
        return 0f;
    }

    public static float getDamageIgnoringBlock(Vec3d explosionPos, PlayerEntity target, BlockPos airPos) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null || airPos == null) return 0f;

        double maxDist = 12.0;
        if (!new Box(MathHelper.floor(explosionPos.x - maxDist - 1.0), MathHelper.floor(explosionPos.y - maxDist - 1.0),
                MathHelper.floor(explosionPos.z - maxDist - 1.0), MathHelper.floor(explosionPos.x + maxDist + 1.0),
                MathHelper.floor(explosionPos.y + maxDist + 1.0), MathHelper.floor(explosionPos.z + maxDist + 1.0))
                .intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (!target.isInvulnerable()) {
            double distExposure = target.squaredDistanceTo(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
                terrainIgnore = true;
                double exposure = getExposureIgnoreBlock(explosionPos, target, airPos);
                terrainIgnore = false;

                double finalExposure = (1.0 - distExposure) * exposure;
                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

                if (mc.world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                DamageSource source = mc.world.getDamageSources().explosion(null, null);
                toDamage = DamageUtil.getDamageLeft(target, toDamage, source, target.getArmor(),
                        (float) target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));

                toDamage = applyResistance(target, toDamage);
                toDamage = applyProtection(target, toDamage);

                return toDamage;
            }
        }
        return 0f;
    }

    private static float applyResistance(LivingEntity entity, float toDamage) {
        if (entity.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int resistance = 25 - (Objects.requireNonNull(entity.getStatusEffect(StatusEffects.RESISTANCE)).getAmplifier() + 1) * 5;
            float resistance1 = toDamage * resistance;
            toDamage = Math.max(resistance1 / 25f, 0f);
        }
        if (toDamage <= 0f) return 0f;
        return toDamage;
    }

    private static float applyProtection(LivingEntity entity, float toDamage) {
        float protAmount = getProtectionAmount(List.of(
                entity.getEquippedStack(EquipmentSlot.HEAD),
                entity.getEquippedStack(EquipmentSlot.CHEST),
                entity.getEquippedStack(EquipmentSlot.LEGS),
                entity.getEquippedStack(EquipmentSlot.FEET)
        ));
        if (protAmount > 0) toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
        return toDamage;
    }

    private static float getExposureIgnoreBlock(Vec3d source, Entity entity, BlockPos airPos) {
        return getExposureIgnoreBlock(source, entity.getBoundingBox(), entity, airPos);
    }

    private static float getExposureIgnoreBlock(Vec3d source, Box box, Entity entity, BlockPos airPos) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;

        if (d < 0.0 || e < 0.0 || f < 0.0) {
            return 0.0f;
        }

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = MathHelper.lerp(k, box.minX, box.maxX);
                    double o = MathHelper.lerp(l, box.minY, box.maxY);
                    double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                    Vec3d vec3d = new Vec3d(n + g, o, p + h);

                    if (raycastIgnoreBlock(new RaycastContext(vec3d, source, RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE, entity), airPos).getType() == HitResult.Type.MISS) {
                        ++i;
                    }
                    ++j;
                }
            }
        }
        return (float) i / (float) j;
    }

    private static BlockHitResult raycastIgnoreBlock(RaycastContext context, BlockPos airPos) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (inner, pos) -> {
            BlockState state = pos.equals(airPos) ? Blocks.AIR.getDefaultState() : mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            return shape.raycast(inner.getStart(), inner.getEnd(), pos);
        }, inner -> BlockHitResult.createMissed(inner.getEnd(),
                Direction.getFacing(inner.getStart().x - inner.getEnd().x, inner.getStart().y - inner.getEnd().y, inner.getStart().z - inner.getEnd().z),
                BlockPos.ofFloored(inner.getEnd())));
    }

    private static float getExposureGhost(Vec3d source, Entity entity, BlockPos pos) {
        return getExposureGhost(source, entity.getBoundingBox(), entity, pos);
    }

    private static float getExposureGhost(Vec3d source, Box box, Entity entity, BlockPos pos) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;

        if (d < 0.0 || e < 0.0 || f < 0.0) {
            return 0.0f;
        }

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = MathHelper.lerp(k, box.minX, box.maxX);
                    double o = MathHelper.lerp(l, box.minY, box.maxY);
                    double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                    Vec3d vec3d = new Vec3d(n + g, o, p + h);
                    if (raycastGhost(new RaycastContext(vec3d, source, RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE, entity), pos).getType() == HitResult.Type.MISS)
                        ++i;
                    ++j;
                }
            }
        }
        return (float) i / (float) j;
    }

    private static BlockHitResult raycastGhost(RaycastContext context, BlockPos ghostPos) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (inner, pos) -> {
            BlockState state;
            if (pos.equals(ghostPos)) {
                state = Blocks.OBSIDIAN.getDefaultState();
            } else {
                state = mc.world.getBlockState(pos);
            }
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            return shape.raycast(context.getStart(), context.getEnd(), pos);
        }, inner -> BlockHitResult.createMissed(inner.getEnd(), Direction.UP, BlockPos.ofFloored(inner.getEnd())));
    }

    public static float getExposure(Vec3d source, Box box, boolean optimized) {
        if (!optimized) return getExposure(source, box);

        int miss = 0;
        int hit = 0;

        for (int k = 0; k <= 1; k += 1) {
            for (int l = 0; l <= 1; l += 1) {
                for (int m = 0; m <= 1; m += 1) {
                    double n = MathHelper.lerp(k, box.minX, box.maxX);
                    double o = MathHelper.lerp(l, box.minY, box.maxY);
                    double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                    Vec3d vec3d = new Vec3d(n, o, p);
                    if (raycast(vec3d, source) == HitResult.Type.MISS)
                        ++miss;
                    ++hit;
                }
            }
        }
        return (float) miss / (float) hit;
    }

    public static float getExposure(Vec3d source, Box box) {
        double d = 0.4545454446934474;
        double e = 0.21739130885479366;
        double f = 0.4545454446934474;

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d)
            for (double l = 0.0; l <= 1.0; l += e)
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = MathHelper.lerp(k, box.minX, box.maxX);
                    double o = MathHelper.lerp(l, box.minY, box.maxY);
                    double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                    Vec3d vec3d = new Vec3d(n + 0.045454555306552624, o, p + 0.045454555306552624);
                    if (raycast(vec3d, source) == HitResult.Type.MISS)
                        ++i;
                    ++j;
                }

        return (float) i / (float) j;
    }

    public static HitResult.Type raycast(Vec3d start, Vec3d end) {
        return BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }

    public static boolean intersectsEntityBox(BlockPos pos, Entity entity) {
        Box blockBox = new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        return entity.getBoundingBox().intersects(blockBox);
    }

    public static int getProtectionAmount(Iterable<ItemStack> equipment) {
        MutableInt mutableInt = new MutableInt();
        equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
        return mutableInt.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> blastEntry = registry.getOrThrow(Enchantments.BLAST_PROTECTION);
        RegistryEntry<Enchantment> protEntry = registry.getOrThrow(Enchantments.PROTECTION);
        int modifierBlast = EnchantmentHelper.getLevel(blastEntry, stack);
        int modifier = EnchantmentHelper.getLevel(protEntry, stack);
        return modifierBlast * 2 + modifier;
    }
}
