package client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class ShapeShader {
   private static int value2 = 0;

   public static void update() {
      if (value2 != 0) {
         if (--value2 == 0) {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
         }
      }
   }

   public static void update2() {
      if (value2++ == 0) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
      }
   }

   private static void stateBegin() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
   }

   private static void stateEnd() {
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
   }

   private static int getA(int count) {
      return count >>> 24 & 0xFF;
   }

   private static int getR(int count) {
      return count >> 16 & 0xFF;
   }

   private static int getG(int count) {
      return count >> 8 & 0xFF;
   }

   private static int getB(int count) {
      return count & 0xFF;
   }

   private static int pack(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int mulAlpha(int count, float value) {
      float f = value <= 0.0F ? 1.0F : value;
      f = f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
      return pack(getR(count), getG(count), getB(count), Math.round((float)getA(count) * f));
   }

   public static void onFloatIntFloatFloatFloatIntMatrix4fFloatIntIntFloat(
      float value, int count, float value3, float value4, float value5, int count2, Matrix4f matrix4f, float value6, int count3, int count4, float value7
   ) {
      onRounded(matrix4f, value, value3, value5, value7, value4, value4, value4, value4, count2, count, 1.0F, Math.max(0.0F, value6));
   }

   public static void onIntMatrix4fFloatIntFloatFloatFloat(int count, Matrix4f matrix4f, float value, int count2, float value2, float value3, float value4) {
      float f = Math.max(0.5F, value);
      int i = mulAlpha(count2, value4);
      if (getA(i) == 0) {
         return;
      }
      stateBegin();
      BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      float f1 = (float)getA(i) / 255.0F;
      bufferbuilder.vertex(matrix4f, value2, value3, 0.0F).color(getR(i), getG(i), getB(i), f1);
      for (int j = 0; j <= 36; j++) {
         float f2 = (float)j * 0.17453294F;
         bufferbuilder
            .vertex(matrix4f, value2 + (float)(Math.cos((double)f2) * f), value3 + (float)(Math.sin((double)f2) * f), 0.0F)
            .color(getR(i), getG(i), getB(i), f1);
      }
      flush(bufferbuilder);
      stateEnd();
   }

   public static void onIntFloatFloatIntFloatFloatMatrix4fFloatFloat(
      int count, float value, float value2, int count2, float value3, float value4, Matrix4f matrix4f, float value5, float value6
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value6, value6, value6, value6, count2, count, value5, 0.0F);
   }

   public static void onFloatIntIntFloatFloatMatrix4fIntIntFloat(
      float value, int count, int count2, float value2, float value3, Matrix4f matrix4f, int count3, int count4, float value4
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value4, value4, value4, value4, count2, count, 1.0F, 0.0F);
   }

   public static void onIntFloatMatrix4fFloatFloatFloatFloatFloat(
      int count, float value, Matrix4f matrix4f, float value3, float value4, float value5, float value6, float value7
   ) {
      if (value <= 0.0F) {
         return;
      }
      float f = Math.max(0.5F, value7);
      float f1 = Math.min(f, value * 0.5F);
      int i = mulAlpha(count, value6);
      if (getA(i) == 0) {
         return;
      }
      float f2 = Math.clamp(value3, 0.0F, 1.0F);
      int j = Math.max(2, Math.round(f2 * 48.0F));
      if (j == 0) {
         return;
      }
      stateBegin();
      float f3 = (float)getA(i) / 255.0F;
      float f4 = value4 + value / 2.0F;
      float f5 = value5 + value / 2.0F;
      BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float f6 = -1.5707964F;
      for (int k = 0; k < j; k++) {
         float f7 = f6 + (float)k / (float)j * 6.2831855F;
         float f8 = f6 + (float)(k + 1) / (float)j * 6.2831855F;
         float f9 = (float)Math.cos((double)f7);
         float f10 = (float)Math.sin((double)f7);
         float f11 = (float)Math.cos((double)f8);
         float f12 = (float)Math.sin((double)f8);
         bufferbuilder.vertex(matrix4f, f4 + (value - f1) * f9, f5 + (value - f1) * f10, 0.0F).color(getR(i), getG(i), getB(i), f3);
         bufferbuilder.vertex(matrix4f, f4 + value * f9, f5 + value * f10, 0.0F).color(getR(i), getG(i), getB(i), f3);
         bufferbuilder.vertex(matrix4f, f4 + value * f11, f5 + value * f12, 0.0F).color(getR(i), getG(i), getB(i), f3);
         bufferbuilder.vertex(matrix4f, f4 + (value - f1) * f11, f5 + (value - f1) * f12, 0.0F).color(getR(i), getG(i), getB(i), f3);
      }
      flush(bufferbuilder);
      stateEnd();
   }

   public static void onFloatFloatMatrix4fFloatFloatFloatFloat(float value, float value3, Matrix4f matrix4f, float value4, float value5, float value6, float value7) {
      if (value <= 0.0F || value6 <= 0.0F) {
         return;
      }
      stateBegin();
      BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      hueStrip(matrix4f, value5, value3, value6, value, value7, bufferbuilder);
      flush(bufferbuilder);
      stateEnd();
   }

   private static void hueStrip(Matrix4f matrix4f, float x, float y, float w, float h, float alpha, BufferBuilder bufferbuilder) {
      int i = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F);
      for (int j = 0; j < 24; j++) {
         float f1 = (float)j / 24.0F;
         float f2 = (float)(j + 1) / 24.0F;
         int k = MathHelper.hsvToArgb(f1, 1.0F, 1.0F, i);
         int l = MathHelper.hsvToArgb(f2, 1.0F, 1.0F, i);
         bufferbuilder.vertex(matrix4f, x + f1 * w, y, 0.0F).color(getR(k), getG(k), getB(k), getA(k));
         bufferbuilder.vertex(matrix4f, x + f1 * w, y + h, 0.0F).color(getR(k), getG(k), getB(k), getA(k));
         bufferbuilder.vertex(matrix4f, x + f2 * w, y + h, 0.0F).color(getR(l), getG(l), getB(l), getA(l));
         bufferbuilder.vertex(matrix4f, x + f2 * w, y, 0.0F).color(getR(l), getG(l), getB(l), getA(l));
      }
   }

   public static void onFloatMatrix4fFloatFloatFloatFloatFloatInt(
      float value, Matrix4f matrix4f, float value3, float value4, float value5, float value6, float value7, int count
   ) {
      if (value3 <= 0.0F || value6 <= 0.0F) {
         return;
      }
      stateBegin();
      BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      hueStrip(matrix4f, value5, value7, value6, value3, value, bufferbuilder);
      flush(bufferbuilder);
      stateEnd();
   }

   public static void onFloatFloatFloatIntFloatArrayFloatFloatArrayFloatMatrix4fFloatFloatFloatFloatFloatFloatIntFloatFloatIntFloat(
      float value,
      float value2,
      float value3,
      int count,
      float[] valueArray,
      float value4,
      float[] valueArray2,
      float value5,
      Matrix4f matrix4f,
      float value6,
      float value7,
      float value8,
      float value9,
      float value10,
      float value11,
      int count2,
      float value12,
      float value13,
      int count3,
      float value14
   ) {
      float f = valueArray != null && valueArray.length >= 1 ? valueArray[0] : value10;
      onRounded(matrix4f, value, value2, value8, value9, f, f, f, f, count3, count, Math.max(1.0F, value14), Math.max(1.0F, value13));
   }

   public static void onFloatArrayFloatFloatFloatIntFloatArrayFloatFloatFloatFloatFloatIntFloatMatrix4fFloatIntFloatFloatArrayFloatArray(
      float[] valueArray, float value, float value2, float value3, int count, float[] valueArray2, float value4, float value5, float value6, float value7, float value8, int count2, float value9, Matrix4f matrix4f, float value10, int count3, float value11, float[] valueArray3, float[] valueArray4
   ) {
      if (value8 <= 0.0F || value <= 0.0F || value3 <= 0.0F) {
         return;
      }
      float f = valueArray != null && valueArray.length >= 1 ? valueArray[0] : value7;
      onRounded(matrix4f, value, value2, value3, value8, f, f, f, f, count3, count2, Math.max(1.0F, value10), Math.max(0.0F, value9));
   }

   public static void onFloatFloatIntMatrix4fFloatFloatFloatFloat(
      float value, float value2, int count, Matrix4f matrix4f, float value3, float value4, float value5, float value6
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value6, value6, value6, value6, count, 0, value5, 0.0F);
   }

   public static void onFloatIntIntFloatFloatFloatFloatFloatFloatMatrix4fFloatIntFloatFloatFloatFloatFloat(
      float value,
      int count,
      int count2,
      float value2,
      float value3,
      float value4,
      float value5,
      float value6,
      float value7,
      Matrix4f matrix4f,
      float value8,
      int count3,
      float value9,
      float value10,
      float value11,
      float value12,
      float value13
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value5, value5, value5, value5, count, count2, value8, Math.max(0.0F, value6));
   }

   public static void onFloatFloatIntIntMatrix4fFloatFloat(float value, float value2, int count, int count2, Matrix4f matrix4f, float value3, float value4) {
      onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(1.0F, value3, value4, matrix4f, value2, count, value3, value, count2, value4);
   }

   public static void onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(
      float value, float value2, float value3, Matrix4f matrix4f, float value4, int count, float value5, float value6, int count2, float value7
   ) {
      float f = Math.max(0.0F, value);
      int i = count;
      int j = count2;
      if (value3 > 0.001F) {
         i = 0;
         j = mulAlpha(count2, value3);
      }
      onRounded(matrix4f, value6, value4, value2, value7, value5, value5, value5, value5, count, j, Math.max(1.0F, value), 0.0F);
   }

   public static void onFloatIntFloatFloatFloatFloatFloatIntMatrix4f(
      float value, int count, float value2, float value3, float value4, float value5, float value6, int count2, Matrix4f matrix4f
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value5, value5, value5, value5, count, 0, value6, 0.0F);
   }

   public static void onFloatFloatMatrix4fFloatFloatIntFloatFloat(
      float value, float value2, Matrix4f matrix4f, float value3, float value4, int count, float value5, float value6
   ) {
      onRounded(matrix4f, value, value2, value3, value4, value6, value6, value6, value6, count, 0, value5, 0.0F);
   }

   public static void onFloatFloatFloatFloatFloatMatrix4fInt(float value, float value2, float value3, float value4, float value5, Matrix4f matrix4f, int count) {
      if (value5 <= 0.0F || value2 <= 0.0F) {
         return;
      }
      onRounded(matrix4f, value3, value4, value2, value5, value2, value2, value2, value2, count, 0, value, 0.0F);
   }

   private static void onRounded(
      Matrix4f matrix4f,
      float x,
      float y,
      float w,
      float h,
      float rTL,
      float rTR,
      float rBR,
      float rBL,
      int fillColor,
      int borderColor,
      float alphaMul,
      float borderW
   ) {
      float f = alphaMul <= 0.0F ? 1.0F : alphaMul;
      int i = mulAlpha(fillColor, f);
      if (w <= 0.0F || h <= 0.0F || getA(i) == 0 && borderColor == 0) {
         return;
      }
      float f1 = Math.min(Math.max(0.0F, rTL), w * 0.5F);
      float f2 = Math.min(Math.max(0.0F, rTR), h * 0.5F);
      float f3 = Math.min(Math.max(0.0F, rBR), w * 0.5F);
      float f4 = Math.min(Math.max(0.0F, rBL), h * 0.5F);
      stateBegin();
      BufferBuilder bufferbuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      if (borderColor != 0 && borderW > 0.0F) {
         roundedQuadBatch(matrix4f, x - borderW, y - borderW, w + borderW * 2.0F, h + borderW * 2.0F, f1 + borderW, f2 + borderW, f3 + borderW, f4 + borderW, borderColor, bufferbuilder);
      }
      roundedQuadBatch(matrix4f, x, y, w, h, f1, f2, f3, f4, i, bufferbuilder);
      flush(bufferbuilder);
      stateEnd();
   }

   private static void roundedQuadBatch(
      Matrix4f matrix4f, float x, float y, float w, float h, float rTL, float rTR, float rBR, float rBL, int color, BufferBuilder bufferbuilder
   ) {
      if (getA(color) == 0) {
         return;
      }
      int i = getR(color);
      int j = getG(color);
      int k = getB(color);
      float f = (float)getA(color) / 255.0F;
      quad(matrix4f, x + rTL, y, x + w - rTR, y + h, i, j, k, f, bufferbuilder);
      quad(matrix4f, x, y + rTL, x + rTL, y + h - rBL, i, j, k, f, bufferbuilder);
      quad(matrix4f, x + w - rTR, y + rTR, x + w, y + h - rBR, i, j, k, f, bufferbuilder);
      quad(matrix4f, x, y + h - rBL, x + w, y + h, i, j, k, f, bufferbuilder);
      corner(matrix4f, x + rTL, y + rTL, rTL, i, j, k, f, bufferbuilder);
      corner(matrix4f, x + w - rTR, y + rTR, rTR, i, j, k, f, bufferbuilder);
      corner(matrix4f, x + w - rBR, y + h - rBR, rBR, i, j, k, f, bufferbuilder);
      corner(matrix4f, x + rBL, y + h - rBL, rBL, i, j, k, f, bufferbuilder);
   }

   private static void corner(Matrix4f matrix4f, float cx, float cy, float r, int rcol, int gcol, int bcol, float a, BufferBuilder bufferbuilder) {
      if (r <= 0.0F) {
         return;
      }
      for (int i = 0; i < 8; i++) {
         float f1 = (float)i * 0.3926991F;
         float f2 = (float)(i + 1) * 0.3926991F;
         bufferbuilder.vertex(matrix4f, cx, cy, 0.0F).color(rcol, gcol, bcol, a);
         bufferbuilder.vertex(matrix4f, cx + (float)(Math.cos((double)f1) * r), cy + (float)(Math.sin((double)f1) * r), 0.0F).color(rcol, gcol, bcol, a);
         bufferbuilder.vertex(matrix4f, cx + (float)(Math.cos((double)f2) * r), cy + (float)(Math.sin((double)f2) * r), 0.0F).color(rcol, gcol, bcol, a);
         bufferbuilder.vertex(matrix4f, cx, cy, 0.0F).color(rcol, gcol, bcol, a);
      }
   }

   private static void quad(Matrix4f matrix4f, float x1, float y1, float x2, float y2, int rcol, int gcol, int bcol, float a, BufferBuilder bufferbuilder) {
      if (x2 <= x1 || y2 <= y1) {
         return;
      }
      bufferbuilder.vertex(matrix4f, x1, y1, 0.0F).color(rcol, gcol, bcol, a);
      bufferbuilder.vertex(matrix4f, x1, y2, 0.0F).color(rcol, gcol, bcol, a);
      bufferbuilder.vertex(matrix4f, x2, y2, 0.0F).color(rcol, gcol, bcol, a);
      bufferbuilder.vertex(matrix4f, x2, y1, 0.0F).color(rcol, gcol, bcol, a);
   }

   private static void flush(BufferBuilder bufferBuilder) {
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }
}