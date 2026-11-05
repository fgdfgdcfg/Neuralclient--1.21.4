package fun.neural.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import fun.neural.api.events.EventLink;
import fun.neural.api.events.Priority;
import fun.neural.api.events.implement.Event3DRender;
import fun.neural.api.utils.color.ColorUtils;
import fun.neural.api.utils.render.ShaderUtils;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.FloatSetting;
import fun.neural.client.modules.settings.implement.ListSetting;
import fun.neural.client.modules.settings.implement.BooleanSetting;

public class SkyShader extends Module {

    public static SkyShader INSTANCE = new SkyShader();

    private final ListSetting mode = new ListSetting("Режим",
            new BooleanSetting("Water", true),
            new BooleanSetting("Caustic", false)
    );

    private final FloatSetting speed = new FloatSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final FloatSetting scale = new FloatSetting("Размер", 5.0f, 1.0f, 20.0f, 0.5f);
    private final FloatSetting intensity = new FloatSetting("Интенсивность", 0.01f, 0.001f, 0.05f, 0.001f);
    private final FloatSetting alpha = new FloatSetting("Прозрачность", 1.0f, 0.3f, 1.0f, 0.05f);

    private long startMillis = -1;

    public SkyShader() {
        super("SkyShader", "Замена неба на шейдер", ModuleCategory.RENDER);
        addSettings(mode, speed, scale, intensity, alpha);
    }

    @Override
    public void onDisable() {
        startMillis = -1;
        super.onDisable();
    }

    @EventLink(priority = Priority.LOW)
    public void onRender3D(Event3DRender event) {
        if (!isEnable()) return;
        if (mc.world == null || mc.player == null) return;
        renderSkyShader();
    }

    public void renderSkyShader() {
        if (!isEnable()) return;
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        int themeColor = ColorUtils.getThemeColor();
        float cr = ColorUtils.redf(themeColor);
        float cg = ColorUtils.greenf(themeColor);
        float cb = ColorUtils.bluef(themeColor);

        net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
        float yawRad = (float) Math.toRadians(-cam.getYaw());
        float pitchRad = (float) Math.toRadians(cam.getPitch());
        float fov = (float) mc.options.getFov().getValue().intValue();

        try {
            ShaderProgramKey key = mode.is("Caustic") ? ShaderUtils.skyCaustic : ShaderUtils.skyWater;
            ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(key);
            if (shader == null) return;

            shader.getUniform("uTime").set(time);
            shader.getUniform("uResolution").set(fw, fh);
            shader.getUniform("uColor").set(cr, cg, cb);
            shader.getUniform("uAlpha").set(alpha.get());
            shader.getUniform("uSpeed").set(speed.get());
            shader.getUniform("uScale").set(scale.get());
            shader.getUniform("uIntensity").set(intensity.get());
            shader.getUniform("uCameraDir").set(yawRad, pitchRad);
            shader.getUniform("uFov").set(fov);

            RenderSystem.setShader(key);

            Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
            RenderSystem.setProjectionMatrix(new Matrix4f(), com.mojang.blaze3d.systems.ProjectionType.ORTHOGRAPHIC);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_EQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Matrix4f identity = new Matrix4f();
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            buf.vertex(identity, -1f, -1f, 1f);
            buf.vertex(identity, 1f, -1f, 1f);
            buf.vertex(identity, 1f, 1f, 1f);
            buf.vertex(identity, -1f, 1f, 1f);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.setProjectionMatrix(savedProj, com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
        } catch (Exception e) {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
    }
}
