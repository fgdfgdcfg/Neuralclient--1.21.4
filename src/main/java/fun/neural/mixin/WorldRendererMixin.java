package fun.neural.mixin;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.neural.api.QClient;
import fun.neural.api.events.EventInvoker;
import fun.neural.api.events.implement.Event3DRender;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.client.modules.impl.render.Removals;
import fun.neural.client.modules.impl.render.ShaderEsp;
import fun.neural.client.modules.impl.render.SkyShader;
import fun.neural.client.modules.impl.render.Sonar;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements QClient {

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
    private void neural$renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Частицы")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void neural$renderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void neural$addWeatherParticlesAndSound(Camera camera, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void neural$renderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Облака")) {
            ci.cancel();
        }
        SkyShader skyShader = ModuleClass.INSTANCE.skyShader;
        if (skyShader != null && skyShader.isEnable()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"), cancellable = true)
    private void neural$renderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate mainConsumers, VertexConsumerProvider.Immediate translucentConsumers, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Блок-сущности")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Sonar sonar = ModuleClass.INSTANCE != null ? ModuleClass.INSTANCE.sonar : null;
        boolean has3DListeners = EventInvoker.hasListeners(Event3DRender.class);
        boolean renderSonar = sonar != null && sonar.isEnable();
        if (!has3DListeners && !renderSonar) {
            return;
        }

        Profilers.get().swap("neural_renderWorld");
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        if (has3DListeners) {
            new Event3DRender(matrices, positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)).call();
        }
        if (renderSonar) {
            sonar.renderFromMixin(positionMatrix, projectionMatrix, camera.getPos());
        }
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At("HEAD"), cancellable = true)
    private void neural$drawEntityOutlinesFramebuffer(CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;
        ShaderEsp shaderEsp = ModuleClass.INSTANCE.shaderEsp;
        if (shaderEsp != null && shaderEsp.isEnable()) {
            ci.cancel();
            return;
        }

    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    public void onDrawBlockOutline(CallbackInfo ci) {
        if (ModuleClass.INSTANCE.blockOverlay.isEnable()) ci.cancel();
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void neural$renderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;
        SkyShader skyShader = ModuleClass.INSTANCE.skyShader;
        if (skyShader != null && skyShader.isEnable()) {
            ci.cancel();
        }
    }
}
