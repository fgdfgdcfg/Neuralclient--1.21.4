package fun.neural.client.modules.impl.combat;

import fun.neural.api.events.EventLink;
import fun.neural.api.events.implement.EventBinding;
import fun.neural.api.events.implement.EventPacket;
import fun.neural.api.events.implement.EventUpdate;
import fun.neural.api.storages.implement.RotationStorage;
import fun.neural.api.utils.input.KeyBoardUtils;
import fun.neural.api.utils.rotate.Rotation;
import fun.neural.api.utils.rotate.RotationUtils;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.BindSetting;
import fun.neural.client.modules.settings.implement.BooleanSetting;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class AutoCrystal extends Module {

    private static final String TICK_MODE = "Tиковая";
    private static final String MS_MODE = "Oбычная";

    private enum State {
        IDLE,
        SWAP_TO_OBSIDIAN,
        PLACE_OBSIDIAN,
        VERIFY_OBSIDIAN,
        SWAP_TO_CRYSTAL,
        PLACE_CRYSTAL,
        WAIT_CRYSTAL,
        ATTACK_CRYSTAL,
        SWAP_BACK,
        DONE
    }

    public static final AutoCrystal INSTANCE = new AutoCrystal();

    private final ModeSetting delayType = new ModeSetting("Тип задержки", TICK_MODE, TICK_MODE, MS_MODE);
    private final FloatSetting obsidianDelayTicks = new FloatSetting("Делей ставки", 0f, 0f, 10f, 1f)
            .visible(() -> delayType.is(TICK_MODE));
    private final FloatSetting obsidianDelayMs = new FloatSetting("Делей ставки (мс)", 0f, 0f, 50f, 1f)
            .visible(() -> !delayType.is(TICK_MODE));
    private final FloatSetting maxCrystalWait = new FloatSetting("Макс. ожидание кристалла", 5f, 3f, 30f, 1f);
    private final BooleanSetting rotation = new BooleanSetting("Ротация", true);
    private final FloatSetting aimSpeed = new FloatSetting("Скорость наводки", 10f, 1f, 10f, 0.5f)
            .visible(rotation::isState);
    private final BooleanSetting autoOnPlace = new BooleanSetting("Авто при ставке", false);
    private final BooleanSetting swapBack = new BooleanSetting("Свапать обратно", true);
    private final BindSetting obsidianKey = new BindSetting("Клавиша обсидиана", -1);

    private final PlaceCooldown placeCooldown = new PlaceCooldown();

    private State state = State.IDLE;
    private int savedSlot = -1;
    private int obsidianSlot = -1;
    private int crystalSlot = -1;
    private BlockPos blockPos;
    private int crystalWaitTicks;
    private int verifyRetries;
    private boolean delayActive;
    private Vec3d rotationTarget;
    private Vec3d hitVec;
    private BlockPos hitBlockPos;
    private Direction hitSide;

    public AutoCrystal() {
        super("AutoCrystal", "По клавише ставит обсидиан и кристалл и взрывает его", ModuleCategory.COMBAT);
        addSettings(autoOnPlace, delayType, obsidianDelayTicks, obsidianDelayMs, maxCrystalWait,
                aimSpeed, swapBack, rotation, obsidianKey);
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (event.getKey() == obsidianKey.getKey()) {
            startChainFromCrosshair();
        }
    }

    private void startChainFromCrosshair() {
        if (state != State.IDLE || mc.player == null || mc.world == null) {
            return;
        }
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHitResult)
                || blockHitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos clicked = blockHitResult.getBlockPos();
        if (mc.world.getBlockState(clicked).isAir() || mc.world.getBlockState(clicked).isLiquid()) {
            return;
        }
        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(clicked)) > 4.5) {
            return;
        }
        crystalSlot = findSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) {
            return;
        }

        Block clickedBlock = mc.world.getBlockState(clicked).getBlock();
        if (clickedBlock == Blocks.OBSIDIAN
                || clickedBlock == Blocks.BEDROCK
                || clickedBlock == Blocks.CRYING_OBSIDIAN) {
            savedSlot = mc.player.getInventory().selectedSlot;
            obsidianSlot = -1;
            blockPos = clicked;
            crystalWaitTicks = 0;
            verifyRetries = 0;
            delayActive = false;
            rotationTarget = Vec3d.ofCenter(clicked).add(0.0, 0.5, 0.0);
            state = State.SWAP_TO_CRYSTAL;
            return;
        }

        Direction side = blockHitResult.getSide();
        BlockPos placePos = clicked.offset(side);
        if (!isPlaceableBlock(placePos)) {
            return;
        }
        savedSlot = mc.player.getInventory().selectedSlot;
        blockPos = placePos;
        hitVec = blockHitResult.getPos();
        hitBlockPos = clicked;
        hitSide = side;
        crystalWaitTicks = 0;
        verifyRetries = 0;
        delayActive = false;
        rotationTarget = hitVec;
        if (mc.player.getMainHandStack().getItem() == Items.OBSIDIAN) {
            state = State.PLACE_OBSIDIAN;
        } else {
            obsidianSlot = findSlot(Items.OBSIDIAN);
            if (obsidianSlot == -1) {
                reset();
                return;
            }
            state = State.SWAP_TO_OBSIDIAN;
        }
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.getType() != EventPacket.Type.SEND) {
            return;
        }
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)) {
            return;
        }
        if (!autoOnPlace.isState() || state != State.IDLE) {
            return;
        }
        if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) {
            return;
        }
        BlockPos placePos = packet.getBlockHitResult().getBlockPos().offset(packet.getBlockHitResult().getSide());
        if (!isPlaceableBlock(placePos)) {
            return;
        }
        crystalSlot = findSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) {
            return;
        }
        savedSlot = mc.player.getInventory().selectedSlot;
        obsidianSlot = -1;
        blockPos = placePos;
        hitVec = null;
        hitBlockPos = null;
        hitSide = null;
        crystalWaitTicks = 0;
        verifyRetries = 0;
        delayActive = false;
        rotationTarget = Vec3d.ofCenter(placePos);
        armObsidianDelay();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            if (state != State.IDLE) {
                reset();
            }
            return;
        }

        if (state == State.IDLE && KeyBoardUtils.isBindHeld(obsidianKey.getKey())) {
            startChainFromCrosshair();
        }

        if (state != State.IDLE && rotationTarget != null && rotation.isState()) {
            Vec2f rotations = RotationUtils.getRotations(rotationTarget);
            float speed = aimSpeed.getValue().floatValue();
            RotationStorage.update(new Rotation(rotations.x, rotations.y), speed, speed, speed, 2, 1);
        }

        while (state != State.IDLE) {
            if (delayActive && !placeCooldown.check()) {
                placeCooldown.update2();
                return;
            }

            State before = state;
            switch (state) {
                case SWAP_TO_OBSIDIAN -> swapToObsidian();
                case PLACE_OBSIDIAN -> placeObsidian();
                case VERIFY_OBSIDIAN -> verifyObsidian();
                case SWAP_TO_CRYSTAL -> swapToCrystal();
                case PLACE_CRYSTAL -> placeCrystal();
                case WAIT_CRYSTAL -> waitCrystal();
                case ATTACK_CRYSTAL -> attackCrystal();
                case SWAP_BACK -> swapBackItem();
                case DONE -> reset();
                case IDLE -> {}
            }

            if (state == State.IDLE || (delayActive && !placeCooldown.check()) || state == before) {
                break;
            }
        }
    }

    private void swapToObsidian() {
        if (obsidianSlot < 9) {
            mc.player.getInventory().selectedSlot = obsidianSlot;
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, obsidianSlot, savedSlot,
                    SlotActionType.SWAP, mc.player);
        }
        setState(State.PLACE_OBSIDIAN, 1);
    }

    private void placeObsidian() {
        if (hitVec == null || hitBlockPos == null || hitSide == null || blockPos == null) {
            reset();
            return;
        }
        if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) {
            reset();
            return;
        }
        BlockState clickedState = mc.world.getBlockState(hitBlockPos);
        if (clickedState.isAir() || clickedState.isLiquid()) {
            reset();
            return;
        }
        if (!isPlaceableBlock(blockPos)) {
            reset();
            return;
        }
        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(hitBlockPos)) > 4.5) {
            reset();
            return;
        }
        rotationTarget = hitVec;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(hitVec, hitSide, hitBlockPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        hitVec = null;
        hitBlockPos = null;
        hitSide = null;
        verifyRetries = 0;
        armObsidianDelay();
    }

    private void armObsidianDelay() {
        state = State.VERIFY_OBSIDIAN;
        if (delayType.is(TICK_MODE)) {
            int ticks = (int) obsidianDelayTicks.getValue().floatValue();
            if (ticks <= 0) {
                delayActive = false;
                placeCooldown.update();
            } else {
                delayActive = true;
                placeCooldown.setFlag(false);
                placeCooldown.setInt(ticks);
            }
        } else {
            int millis = (int) obsidianDelayMs.getValue().floatValue();
            if (millis <= 0) {
                delayActive = false;
                placeCooldown.update();
            } else {
                delayActive = true;
                placeCooldown.setFlag(true);
                placeCooldown.setLong(millis);
            }
        }
    }

    private void verifyObsidian() {
        if (blockPos == null) {
            reset();
            return;
        }
        if (mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) {
            setState(State.SWAP_TO_CRYSTAL, 0);
        } else {
            verifyRetries++;
            if (verifyRetries >= 5) {
                reset();
            } else {
                setState(State.VERIFY_OBSIDIAN, 1);
            }
        }
    }

    private void swapToCrystal() {
        crystalSlot = findSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) {
            reset();
            return;
        }
        if (crystalSlot < 9) {
            mc.player.getInventory().selectedSlot = crystalSlot;
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, crystalSlot,
                    mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
        }
        setState(State.PLACE_CRYSTAL, 0);
    }

    private void placeCrystal() {
        if (blockPos == null) {
            reset();
            return;
        }
        BlockPos obsidianPos = findObsidian(blockPos);
        if (obsidianPos == null) {
            reset();
            return;
        }
        if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            reset();
            return;
        }
        BlockPos crystalPos = obsidianPos.up();
        if (!mc.world.getBlockState(crystalPos).isReplaceable()
                || !mc.world.getBlockState(crystalPos.up()).isReplaceable()) {
            reset();
            return;
        }
        Vec3d target = Vec3d.ofCenter(obsidianPos).add(0.0, 0.5, 0.0);
        rotationTarget = target;
        if (rotation.isState() && !isAligned(target, 15.0)) {
            setState(State.PLACE_CRYSTAL, 1);
            return;
        }
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(target, Direction.UP, obsidianPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        crystalWaitTicks = 0;
        setState(State.WAIT_CRYSTAL, 0);
    }

    private void waitCrystal() {
        if (blockPos == null) {
            reset();
            return;
        }
        EndCrystalEntity crystal = findCrystal(blockPos);
        if (crystal != null) {
            rotationTarget = crystal.getPos();
            setState(State.ATTACK_CRYSTAL, 0);
        } else {
            crystalWaitTicks++;
            if (crystalWaitTicks >= maxCrystalWait.getValue().intValue()) {
                reset();
            } else {
                setState(State.WAIT_CRYSTAL, 1);
            }
        }
    }

    private void attackCrystal() {
        if (blockPos == null) {
            reset();
            return;
        }
        EndCrystalEntity crystal = findCrystal(blockPos);
        if (crystal == null) {
            reset();
            return;
        }
        rotationTarget = crystal.getPos();
        if (rotation.isState() && !isAligned(crystal.getPos(), 15.0)) {
            setState(State.ATTACK_CRYSTAL, 1);
            return;
        }
        if (mc.interactionManager.isBreakingBlock()) {
            setState(State.ATTACK_CRYSTAL, 1);
            return;
        }
        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (swapBack.isState()) {
            setState(State.SWAP_BACK, 1);
        } else {
            setState(State.DONE, 1);
        }
    }

    private void swapBackItem() {
        if (obsidianSlot != -1 && obsidianSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, obsidianSlot,
                    mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
        }
        mc.player.getInventory().selectedSlot = savedSlot;
        rotationTarget = null;
        setState(State.DONE, 0);
    }

    private void setState(State newState, int count) {
        state = newState;
        if (count <= 0) {
            delayActive = false;
            placeCooldown.update();
        } else {
            delayActive = true;
            if (delayType.is(TICK_MODE)) {
                placeCooldown.setFlag(false);
                placeCooldown.setInt(count);
            } else {
                placeCooldown.setFlag(true);
                placeCooldown.setLong(count * 50L);
            }
        }
    }

    private void reset() {
        state = State.IDLE;
        placeCooldown.update();
        savedSlot = -1;
        obsidianSlot = -1;
        crystalSlot = -1;
        blockPos = null;
        crystalWaitTicks = 0;
        verifyRetries = 0;
        delayActive = false;
        rotationTarget = null;
        hitVec = null;
        hitBlockPos = null;
        hitSide = null;
    }

    private int findSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlaceableBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isReplaceable() && !state.isLiquid();
    }

    private BlockPos findObsidian(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN) {
            return pos;
        }
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN) {
            return pos.down();
        }
        return null;
    }

    private EndCrystalEntity findCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && !crystal.isRemoved() && crystal.isAlive()) {
                BlockPos entityPos = crystal.getBlockPos();
                if (entityPos.equals(pos.up()) || entityPos.down().equals(pos)) {
                    return crystal;
                }
            }
        }
        return null;
    }

    private boolean isAligned(Vec3d point, double maxDegrees) {
        Vec3d dir = point.subtract(mc.player.getEyePos()).normalize();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double dot = MathHelper.clamp(dir.dotProduct(look), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot)) <= maxDegrees;
    }

    private final class PlaceCooldown {
        private boolean msMode;
        private int ticks;
        private long delayMs;
        private long start;

        void setFlag(boolean flag) {
            this.msMode = flag;
        }

        void setInt(int count) {
            this.ticks = count;
        }

        void setLong(long ms) {
            this.delayMs = ms;
            this.start = System.currentTimeMillis();
        }

        boolean check() {
            if (msMode) {
                return System.currentTimeMillis() - start >= delayMs;
            }
            return ticks <= 0;
        }

        void update2() {
            if (!msMode && ticks > 0) {
                ticks--;
            }
        }

        void update() {
            this.ticks = 0;
            this.start = System.currentTimeMillis();
        }
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }
}