package fun.neural.api.utils.neuro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TinyNet {
    private final int inputs;
    private final int hidden;
    private final int outputs;
    private final float[] w1;
    private final float[] b1;
    private final float[] w2;
    private final float[] b2;
    private final Random random = new Random();

    public TinyNet(int inputs, int hidden, int outputs) {
        this.inputs = inputs;
        this.hidden = hidden;
        this.outputs = outputs;
        this.w1 = new float[hidden * inputs];
        this.b1 = new float[hidden];
        this.w2 = new float[outputs * hidden];
        this.b2 = new float[outputs];
        randomize();
    }

    private void randomize() {
        for (int i = 0; i < w1.length; i++) w1[i] = (random.nextFloat() - 0.5F) * 0.8F;
        for (int i = 0; i < w2.length; i++) w2[i] = (random.nextFloat() - 0.5F) * 0.8F;
    }

    public float[] predict(float[] x) {
        float[] h = new float[hidden];
        for (int j = 0; j < hidden; j++) {
            float sum = b1[j];
            for (int i = 0; i < inputs; i++) {
                sum += w1[j * inputs + i] * x[i];
            }
            h[j] = (float) Math.tanh(sum);
        }
        float[] o = new float[outputs];
        for (int k = 0; k < outputs; k++) {
            float sum = b2[k];
            for (int j = 0; j < hidden; j++) {
                sum += w2[k * hidden + j] * h[j];
            }
            o[k] = sum;
        }
        return o;
    }

    public float trainEpoch(List<float[]> data, float lr) {
        List<Integer> order = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) order.add(i);
        Collections.shuffle(order, random);

        float lossSum = 0.0F;
        for (int idx : order) {
            float[] sample = data.get(idx);
            float loss = trainSample(sample, lr);
            lossSum += loss;
        }
        return lossSum / Math.max(1, data.size());
    }

    private float trainSample(float[] sample, float lr) {
        float[] x = new float[inputs];
        System.arraycopy(sample, 0, x, 0, inputs);
        int labelOffset = inputs;
        float yYaw = sample[labelOffset];
        float yPitch = sample[labelOffset + 1];

        float[] h = new float[hidden];
        for (int j = 0; j < hidden; j++) {
            float sum = b1[j];
            for (int i = 0; i < inputs; i++) {
                sum += w1[j * inputs + i] * x[i];
            }
            h[j] = (float) Math.tanh(sum);
        }

        float[] o = new float[outputs];
        for (int k = 0; k < outputs; k++) {
            float sum = b2[k];
            for (int j = 0; j < hidden; j++) {
                sum += w2[k * hidden + j] * h[j];
            }
            o[k] = sum;
        }

        float eYaw = o[0] - yYaw;
        float ePitch = o[1] - yPitch;
        float loss = eYaw * eYaw + ePitch * ePitch;

        float[] eh = new float[hidden];
        for (int j = 0; j < hidden; j++) {
            float grad = w2[j] * eYaw + w2[hidden + j] * ePitch;
            eh[j] = grad * (1.0F - h[j] * h[j]);
        }

        for (int j = 0; j < hidden; j++) {
            float g = lr * eh[j];
            b1[j] -= g;
            for (int i = 0; i < inputs; i++) {
                w1[j * inputs + i] -= g * x[i];
            }
        }
        for (int k = 0; k < outputs; k++) {
            float e = k == 0 ? eYaw : ePitch;
            float g = lr * e;
            b2[k] -= g;
            for (int j = 0; j < hidden; j++) {
                w2[k * hidden + j] -= g * h[j];
            }
        }
        return loss;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(inputs);
        out.writeInt(hidden);
        out.writeInt(outputs);
        for (float v : w1) out.writeFloat(v);
        for (float v : b1) out.writeFloat(v);
        for (float v : w2) out.writeFloat(v);
        for (float v : b2) out.writeFloat(v);
    }

    public static TinyNet load(DataInputStream in) throws IOException {
        int inputs = in.readInt();
        int hidden = in.readInt();
        int outputs = in.readInt();
        TinyNet net = new TinyNet(inputs, hidden, outputs);
        for (int i = 0; i < net.w1.length; i++) net.w1[i] = in.readFloat();
        for (int i = 0; i < net.b1.length; i++) net.b1[i] = in.readFloat();
        for (int i = 0; i < net.w2.length; i++) net.w2[i] = in.readFloat();
        for (int i = 0; i < net.b2.length; i++) net.b2[i] = in.readFloat();
        return net;
    }
}
