package client.render;

import client.module.Feature;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class RoundedTextureShader {
   private RoundedTextureShader() {
   }

   private static float clampAlpha(float value) {
      return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
   }

   public static void onFloatIdentifierFloatFloatFloatMatrix4fFloat(float size, Identifier identifier, float x, float radius, float y, Matrix4f matrix4f, float alpha) {
      if (identifier == null || size <= 0.0F || alpha <= 0.0F) {
         return;
      }
      DrawContext drawcontext = ItemIconCache.getDrawContext();
      if (drawcontext == null || Feature.mc == null) {
         return;
      }
      double d0 = Math.sqrt((double)(matrix4f.m00() * matrix4f.m00() + matrix4f.m01() * matrix4f.m01()));
      float f = d0 > 1.0E-5 ? (float)d0 : 1.0F;
      float f1 = matrix4f.m00() * x + matrix4f.m10() * y + matrix4f.m30();
      float f2 = matrix4f.m01() * x + matrix4f.m11() * y + matrix4f.m31();
      int i = Math.round(clampAlpha(alpha) * 255.0F) << 24 | 0xFFFFFF;
      MatrixStack matrixstack = drawcontext.getMatrices();
      matrixstack.push();
      try {
         matrixstack.translate(f1, f2, 0.0F);
         matrixstack.scale(f, f, 1.0F);
         RenderSystem.enableBlend();
         drawcontext.drawTexture(
            identifier2 -> net.minecraft.client.render.RenderLayer.getGuiTextured(identifier2), identifier, 0, 0, 8.0F, 8.0F, Math.max(1, Math.round(size)), Math.max(1, Math.round(size)), 8, 8, i
         );
         drawcontext.draw();
      } finally {
         matrixstack.pop();
      }
   }

   public static void onFloatFloatFloatFloatFloatFloatIntFloatFloatMatrix4fIdentifierFloatFloatFloatFloatFloat(
      float value, float value2, float value3, float value4, float value5, float value6, int count, float value7, float value8,
      Matrix4f matrix4f, Identifier identifier, float value9, float value10, float value11, float value12, float value13
   ) {
      if (identifier == null || value2 <= 0.0F || value4 <= 0.0F || value12 <= 0.0F || matrix4f == null) {
         return;
      }
      float f = clampAlpha(value12);
      int i = count & 16777215 | Math.round((count >>> 24 & 0xFF) * f) << 24;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      try {
         RenderSystem.setShaderTexture(0, identifier);
         RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);
         float f1 = value;
         float f2 = value + value2;
         float f3 = value7;
         float f4 = value7 + value4;
         BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         bufferbuilder.vertex(matrix4f, f1, f3, 0.0F).texture(value10, value11).color(i);
         bufferbuilder.vertex(matrix4f, f1, f4, 0.0F).texture(value10, value5).color(i);
         bufferbuilder.vertex(matrix4f, f2, f4, 0.0F).texture(value3, value5).color(i);
         bufferbuilder.vertex(matrix4f, f2, f3, 0.0F).texture(value3, value11).color(i);
         BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
      } finally {
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }
}