package fun.neural.client.modules.impl.combat.components.rotations;

import fun.neural.Neural;
import fun.neural.api.utils.neuro.TinyNet;
import fun.neural.api.utils.rotate.Rotation;
import fun.neural.api.utils.rotate.RotationUtils;
import fun.neural.api.storages.implement.RotationStorage;
import fun.neural.api.utils.chat.ChatUtils;
import fun.neural.client.modules.impl.combat.components.RotationsSystem;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class NeuroRotation extends RotationsSystem {
    public static final NeuroRotation INSTANCE = new NeuroRotation();

    private static final int INPUTS = 3;
    private static final int HIDDEN = 12;
    private static final int OUTPUTS = 2;
    private static final int MAX_SAMPLES = 60000;

    @Getter
    private boolean recording;
    @Getter
    private boolean training;
    @Getter
    private boolean modelReady;
    @Getter
    private int sampleCount;

    private TinyNet net;
    private final List<float[]> dataset = new ArrayList<>();
    private float prevYaw;
    private float prevPitch;
    private boolean hasPrev;
    private int lastApplyTick = -1;

    private File directory() {
        return new File(Neural.INSTANCE.globalsDir, "neuro_aim");
    }

    private NeuroRotation() {
        try {
            File file = new File(directory(), "model.bin");
            if (file.isFile()) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                    net = TinyNet.load(in);
                    modelReady = true;
                }
            }
        } catch (Exception ignored) {
            net = null;
            modelReady = false;
        }
        if (net == null) {
            net = new TinyNet(INPUTS, HIDDEN, OUTPUTS);
        }
        loadDataset();
    }

    public void startRecording() {
        dataset.clear();
        sampleCount = 0;
        hasPrev = false;
        recording = true;
    }

    public void stopRecording() {
        recording = false;
        saveDataset();
    }

    public void recordTick(LivingEntity target, float yaw, float pitch) {
        if (!recording || mc.player == null || mc.world == null) {
            return;
        }
        if (!hasPrev) {
            prevYaw = yaw;
            prevPitch = pitch;
            hasPrev = true;
            return;
        }

        float labelYaw = wrapDegrees(yaw - prevYaw);
        float labelPitch = pitch - prevPitch;
        prevYaw = yaw;
        prevPitch = pitch;

        if (target == null) {
            return;
        }

        Vec2f rot = aimRotations(target);
        float yawOffset = wrapDegrees(rot.x - yaw);
        float pitchOffset = rot.y - pitch;

        // если игрок стоит и не ведёт прицел — пропускаем (мусор),
        // но «удержание цели» с нулевым лейблом записываем: сеть должна учиться и стоять на цели
        if (labelYaw == 0.0F && labelPitch == 0.0F
                && (Math.abs(yawOffset) > 20.0F || Math.abs(pitchOffset) > 15.0F)) {
            return;
        }

        float dist = mc.player.distanceTo(target);

        float[] sample = new float[INPUTS + 2];
        sample[0] = MathHelper.clamp(yawOffset / 90.0F, -1.5F, 1.5F);
        sample[1] = MathHelper.clamp(pitchOffset / 60.0F, -1.5F, 1.5F);
        sample[2] = MathHelper.clamp(dist / 6.0F, 0.0F, 1.5F);
        sample[3] = MathHelper.clamp(labelYaw / 45.0F, -1.5F, 1.5F);
        sample[4] = MathHelper.clamp(labelPitch / 30.0F, -1.5F, 1.5F);

        if (dataset.size() >= MAX_SAMPLES) {
            dataset.remove(0);
        }
        dataset.add(sample);
        sampleCount = dataset.size();
    }

    public void trainAsync() {
        if (training) {
            return;
        }
        if (dataset.size() < 200) {
            chat("Мало данных: " + dataset.size() + " сэмплов (нужно 200+). Запиши .neuro record и поиграй.");
            return;
        }
        training = true;
        List<float[]> data = new ArrayList<>(dataset);
        Thread thread = new Thread(() -> {
            try {
                TinyNet trainingNet = new TinyNet(INPUTS, HIDDEN, OUTPUTS);
                int epochs = 40;
                float lr = 0.05F;
                for (int epoch = 0; epoch < epochs; epoch++) {
                    float loss = trainingNet.trainEpoch(data, lr);
                    lr *= 0.93F;
                    if (epoch % 10 == 0) {
                        chat("Обучение: эпоха " + epoch + "/" + epochs + ", ошибка " + String.format("%.4f", loss));
                    }
                }
                synchronized (this) {
                    net = trainingNet;
                    modelReady = true;
                }
                saveModel();
                chat("Готово! Модель обучена на " + data.size() + " сэмплах. Включи режим Neuro в ауре.");
            } catch (Exception e) {
                chat("Ошибка обучения: " + e.getMessage());
            } finally {
                training = false;
            }
        }, "NeuroTrain");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void updateRotations(LivingEntity target) {
        // updateRotations дёргается на ~240 Гц, а модель обучена на изменениях за тик (~20 Гц).
        // применяем один раз за тик, иначе камеру трясёт в 12 раз быстрее, чем учили.
        if (mc.player == null) {
            return;
        }
        int tick = mc.player.age;
        if (tick == lastApplyTick) {
            return;
        }
        lastApplyTick = tick;

        Vec2f rot = aimRotations(target);
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float deltaYaw = wrapDegrees(rot.x - currentYaw);
        float deltaPitch = rot.y - currentPitch;

        float outYaw;
        float outPitch;
        TinyNet activeNet;
        synchronized (this) {
            activeNet = modelReady ? net : null;
        }

        if (activeNet != null && Math.abs(deltaYaw) < 70.0F && !training) {
            float[] x = new float[INPUTS];
            x[0] = MathHelper.clamp(deltaYaw / 90.0F, -1.5F, 1.5F);
            x[1] = MathHelper.clamp(deltaPitch / 60.0F, -1.5F, 1.5F);
            x[2] = MathHelper.clamp(mc.player.distanceTo(target) / 6.0F, 0.0F, 1.5F);
            float[] out = activeNet.predict(x);

            float stepYaw = MathHelper.clamp(out[0] * 45.0F, -35.0F, 35.0F) + deltaYaw * 0.25F;
            float stepPitch = MathHelper.clamp(out[1] * 30.0F, -25.0F, 25.0F) + deltaPitch * 0.25F;

            outYaw = currentYaw + stepYaw;
            outPitch = MathHelper.clamp(currentPitch + stepPitch, -90.0F, 90.0F);
        } else {
            float speedYaw = MathHelper.clamp(Math.abs(deltaYaw) * 1.4F, 4.0F, 45.0F);
            float speedPitch = MathHelper.clamp(Math.abs(deltaPitch) * 1.3F, 3.0F, 35.0F);
            float blend = Math.min(1.0F, speedYaw / Math.max(1.0F, Math.abs(deltaYaw)));
            outYaw = currentYaw + deltaYaw * blend;
            outPitch = MathHelper.clamp(currentPitch + deltaPitch * blend, -90.0F, 90.0F);
        }

        Vec2f corrected = correctRotation(outYaw, outPitch);
        RotationStorage.update(new Rotation(corrected.x, corrected.y), 24.0F, 18.0F, 16.0F, 12.0F, 1, 1, false);
    }

    private Vec2f aimRotations(LivingEntity target) {
        Vec3d point = target.getPos().add(0, target.getHeight() * 0.55F, 0).subtract(mc.player.getEyePos());
        double distXZ = Math.hypot(point.x, point.z);
        float yaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(point.z, point.x)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(point.y, distXZ)));
        return new Vec2f(yaw, pitch);
    }

    private void saveModel() {
        try {
            File dir = directory();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "model.bin");
            synchronized (this) {
                try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                    net.save(out);
                }
            }
        } catch (Exception e) {
            chat("Не удалось сохранить модель: " + e.getMessage());
        }
    }

    private void loadDataset() {
        try {
            File file = new File(directory(), "dataset.bin");
            if (!file.isFile()) {
                return;
            }
            try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    float[] sample = new float[INPUTS + 2];
                    for (int k = 0; k < sample.length; k++) {
                        sample[k] = in.readFloat();
                    }
                    dataset.add(sample);
                }
            }
            sampleCount = dataset.size();
        } catch (Exception ignored) {
            dataset.clear();
        }
    }

    private void saveDataset() {
        try {
            File dir = directory();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "dataset.bin");
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                out.writeInt(dataset.size());
                for (float[] sample : dataset) {
                    for (float v : sample) {
                        out.writeFloat(v);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void clearDataset() {
        dataset.clear();
        sampleCount = 0;
        saveDataset();
    }

    public String getStatus() {
        synchronized (this) {
            return "сэмплов: " + dataset.size()
                    + ", модель: " + (modelReady ? "обучена" : "нет")
                    + (training ? " (идёт обучение)" : "")
                    + (recording ? " (запись...)" : "");
        }
    }

    private void chat(String message) {
        Runnable task = () -> ChatUtils.sendMessage("Neuro: " + message);
        if (mc != null && mc.isOnThread()) {
            task.run();
        } else if (mc != null) {
            mc.execute(task);
        }
    }
}
