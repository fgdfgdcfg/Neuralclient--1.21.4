package fun.neural.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class MenuShaderRenderer {
    private static final String VERTEX_SRC = """
            #version 150 core
            in vec2 aPos;
            out vec2 texcoord;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
                texcoord = aPos * 0.5 + 0.5;
            }
            """;

    private static final String FRAGMENT_SRC = """
            #version 150 core
            uniform float time;
            uniform vec2 resolution;
            in vec2 texcoord;
            out vec4 fragColor;

            float hash12(vec2 p) {
                p = fract(p * vec2(443.8975, 397.2973));
                p += dot(p, p + 19.19);
                return fract(p.x * p.y);
            }

            float drawBubble(vec2 uv, vec2 center, float size, float t) {
                vec2 diff = uv - center;
                float dist = length(diff);
                float circle = 1.0 - smoothstep(size * 0.7, size * 1.3, dist);
                float glow = exp(-dist * 6.0 / size);
                vec2 lightDir = normalize(vec2(-0.5, 0.7));
                float spec = max(0.0, dot(normalize(diff + 0.001), lightDir));
                float highlight = pow(spec, 8.0) * 0.8;
                float pulse = 0.7 + 0.3 * sin(t * 2.0 + center.x * 5.0 + center.y * 5.0);
                return (circle * 0.7 + glow * 0.3 + highlight) * pulse;
            }

            void main() {
                vec2 uv = texcoord;
                float aspect = resolution.x / resolution.y;
                vec2 pos = (uv - 0.5) * vec2(aspect, 1.0);

                vec3 bgColor = mix(vec3(0.02, 0.02, 0.06), vec3(0.08, 0.03, 0.12), uv.y);

                for (int i = 0; i < 50; i++) {
                    float fi = float(i);
                    vec2 starPos = vec2(fract(fi * 0.618033988749895 + 0.3), fract(fi * 0.314159265358979 + 0.7));
                    starPos = (starPos - 0.5) * vec2(aspect, 1.0);
                    float starDist = length(pos - starPos);
                    float starSize = 0.002 + 0.003 * hash12(vec2(fi, 0.0));
                    float starBright = 0.3 + 0.7 * sin(time * 0.5 + fi * 3.7);
                    if (starDist < starSize) {
                        bgColor += vec3(0.5, 0.6, 1.0) * starBright * (1.0 - starDist / starSize);
                    }
                }

                vec3 bubbleColor = vec3(0.0);
                const int BUBBLE_COUNT = 30;

                for (int i = 0; i < BUBBLE_COUNT; i++) {
                    float fi = float(i);
                    vec2 basePos = vec2(fract(fi * 0.618033988749895 + 0.1), fract(fi * 0.314159265358979 + 0.9));
                    basePos = (basePos - 0.5) * vec2(aspect, 1.0);
                    float speed = 0.2 + fi * 0.03;
                    float phase = fi * 1.7;
                    vec2 offset = vec2(
                        sin(time * speed + phase) * (0.1 + 0.1 * hash12(vec2(fi, 1.0))),
                        cos(time * speed * 0.7 + phase * 1.3) * (0.1 + 0.1 * hash12(vec2(fi, 2.0)))
                    );
                    vec2 center = basePos + offset;
                    float baseSize = 0.03 + 0.07 * hash12(vec2(fi, 3.0));
                    float sizeVar = 0.5 + 0.5 * sin(time * (0.5 + fi * 0.1) + fi * 2.3);
                    float size = baseSize * (0.6 + 0.8 * sizeVar);
                    float hue = time * 0.05 + fi * 0.2;
                    vec3 color = 0.5 + 0.5 * vec3(sin(hue), sin(hue + 2.094), sin(hue + 4.188));
                    float brightness = 0.5 + 0.5 * sin(time * 0.7 + fi * 1.3);
                    color *= brightness;
                    float bubble = drawBubble(pos, center, size, time);
                    float edgeFade = 1.0 - smoothstep(0.3, 0.8, abs(center.x) / aspect);
                    edgeFade *= 1.0 - smoothstep(0.3, 0.8, abs(center.y));
                    bubbleColor += color * bubble * edgeFade * 0.6;
                }

                bubbleColor = min(bubbleColor, vec3(1.0));

                vec3 finalColor = bgColor + bubbleColor * 0.6;

                float vignette = 1.0 - length(pos) * 0.4;
                finalColor *= vignette;

                float grain = hash12(uv * resolution + time * 0.1) * 0.02;
                finalColor += grain;

                fragColor = vec4(finalColor, 1.0);
            }
            """;

    private static boolean failed;
    private static int program = -1;
    private static int vao = -1;
    private static int timeLoc;
    private static int resolutionLoc;

    private MenuShaderRenderer() {
    }

    public static boolean isFailed() {
        return failed;
    }

    public static void render(DrawContext context) {
        if (failed || !RenderSystem.isOnRenderThread()) {
            return;
        }
        try {
            if (program == -1) {
                init();
            }
        } catch (Throwable t) {
            failed = true;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(program);
        GL20.glUniform1f(timeLoc, System.currentTimeMillis() % 1000000L / 1000.0F);
        GL20.glUniform2f(resolutionLoc, (float) width, (float) height);
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GL30.glBindVertexArray(prevVertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
        GL20.glUseProgram(prevProgram);
        if (prevDepth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        if (prevBlend) {
            GL11.glEnable(GL11.GL_BLEND);
        }
    }

    private static void init() {
        program = GL20.glCreateProgram();
        int vs = compile(GL20.GL_VERTEX_SHADER, VERTEX_SRC);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SRC);
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(GL20.glGetProgramInfoLog(program));
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        timeLoc = GL20.glGetUniformLocation(program, "time");
        resolutionLoc = GL20.glGetUniformLocation(program, "resolution");

        vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        float[] quad = {-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F};
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0L);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int compile(int type, String src) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
