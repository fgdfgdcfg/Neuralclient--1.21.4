package client.render;

import client.module.Feature;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;

public class TextShader {
   private static int value10 = 0;

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

   private static int getScale(Matrix4f matrix4f) {
      if (matrix4f == null) {
         return 1;
      }
      double d0 = Math.sqrt((double)(matrix4f.m00() * matrix4f.m00() + matrix4f.m01() * matrix4f.m01()));
      return d0 > 1.0E-5 ? (int)Math.round(d0) : 1;
   }

   public static boolean isStringValid(String text) {
      return text != null && !text.isEmpty();
   }

   private static DrawContext getDrawContext() {
      return ItemIconCache.getDrawContext();
   }

   public static void update() {
      if (value10 > 0 && --value10 == 0) {
         update2();
      }
   }

   public static void update2() {
   }

   public static void update3() {
      value10++;
   }

   public static float getFloatByFloat3(float value) {
      return value * 1.2F;
   }

   private static float measure(String text, float size) {
      if (!isStringValid(text) || size <= 0.0F) {
         return 0.0F;
      }
      try {
         if (Feature.mc == null || Feature.mc.textRenderer == null) {
            return 0.0F;
         }
         float f = size / 9.0F;
         return (float)Feature.mc.textRenderer.getWidth(text) * f;
      } catch (Exception exception) {
         return 0.0F;
      }
   }

   public static float getFloatByFloatString(float value, String text) {
      return measure(text, value);
   }

   public static float getFloatByStringFloat(String text, float value) {
      return measure(text, value);
   }

   private static List<net.minecraft.text.StringVisitable> wrap(String text, float size, float maxWidth) {
      int i = (int)Math.max(1.0F, maxWidth / Math.max(0.01F, size / 9.0F));
      try {
         return Feature.mc.textRenderer.getTextHandler().wrapLines(text, i, net.minecraft.text.Style.EMPTY);
      } catch (Exception exception) {
         return java.util.Collections.singletonList(net.minecraft.text.StringVisitable.plain(text));
      }
   }

   private static int wrapCount(String text, float size, float maxWidth) {
      if (!isStringValid(text) || maxWidth <= 0.0F) {
         return 1;
      }
      try {
         return Math.max(1, wrap(text, size, maxWidth).size());
      } catch (Exception exception) {
         return 1;
      }
   }

   public static int getIntByFloatFloatStringArray(float value, float value2, String[] textArray) {
      if (textArray == null || textArray.length == 0) {
         return 0;
      }
      int i = 0;
      for (String s : textArray) {
         i += wrapCount(s, value2, value);
      }
      return i;
   }

   public static float getFloatByFloatStringArray(float value, String[] textArray) {
      if (textArray == null || textArray.length == 0) {
         return 0.0F;
      }
      float f = 0.0F;
      for (String s : textArray) {
         f = Math.max(f, measure(s, value));
      }
      return f;
   }

   public static float getFloatByFloatFloatStringArray(float value, float value2, String[] textArray) {
      return (float)getIntByFloatFloatStringArray(value, value2, textArray) * getFloatByFloat3(value2);
   }

   public static float getFloatByIntStringArrayFloatInt(byte align, String[] textArray, float size, int count) {
      if (textArray == null || textArray.length == 0 || size <= 0.0F) {
         return 0.0F;
      }
      int i = Math.min(textArray.length, Math.max(count, 0));
      float f = 0.0F;
      for (int j = 0; j < i; j++) {
         f = Math.max(f, measure(textArray[j], size));
      }
      return f;
   }

   public static float getFloatByFloatFloatString(float value, float value2, String text) {
      return (float)wrapCount(text, value2, value) * getFloatByFloat3(value2);
   }

   public static float getFloatByFloatString2(float value, String text) {
      return measure(text, value) + getFloatByFloat3(value);
   }

   private static void draw(Matrix4f matrix4f, float x, float y, String text, float size, int color, float alpha) {
      if (text == null || text.isEmpty() || size <= 0.0F || getA(color) == 0) {
         return;
      }
      DrawContext drawcontext = getDrawContext();
      if (drawcontext == null) {
         return;
      }
      int i = mulAlpha(color, alpha <= 0.0F ? 1.0F : alpha);
      if (getA(i) == 0) {
         return;
      }
      MatrixStack matrixstack = drawcontext.getMatrices();
      matrixstack.push();
      try {
         if (matrix4f != null) {
            float f = matrix4f.m00() * x + matrix4f.m10() * y + matrix4f.m30();
            float f1 = matrix4f.m01() * x + matrix4f.m11() * y + matrix4f.m31();
            double d0 = Math.sqrt((double)(matrix4f.m00() * matrix4f.m00() + matrix4f.m01() * matrix4f.m01()));
            float f2 = d0 > 1.0E-5 ? (float)d0 : 1.0F;
            float f3 = f2 * (size / 9.0F);
            matrixstack.translate(f, f1, 0.0F);
            matrixstack.scale(f3, f3, 1.0F);
         } else {
            float f4 = size / 9.0F;
            matrixstack.translate(x, y, 0.0F);
            matrixstack.scale(f4, f4, 1.0F);
         }
         drawcontext.drawText(Feature.mc.textRenderer, text, 0, 0, i, false);
      } finally {
         matrixstack.pop();
      }
   }

   private static void drawLines(Matrix4f matrix4f, float x, float y, String[] textArray, float size, int from, int to, int color, float alpha) {
      if (textArray == null || textArray.length == 0 || from < 0 || to > textArray.length || from >= to) {
         return;
      }
      float f = getFloatByFloat3(size);
      for (int i = from; i < to; i++) {
         draw(matrix4f, x, y + (float)(i - from) * f, textArray[i], size, color, alpha);
      }
   }

   public static void onFloatFloatIntFloatFloatStringMatrix4f(float value, float value2, int count, float value3, float value4, String text, Matrix4f matrix4f) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onFloatFloatIntFloatMatrix4fStringFloatFloat(
      float value, float value2, int count, float value3, Matrix4f matrix4f, String text, float value4, float value5
   ) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onMatrix4fStringFloatFloatFloatIntFloat(Matrix4f matrix4f, String text, float value, float value2, float value3, int count, float value4) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onMatrix4fStringFloatFloatFloatIntFloat2(Matrix4f matrix4f, String text, float value, float value2, float value3, int count, float value4) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onMatrix4fStringFloatFloatFloatIntFloat3(Matrix4f matrix4f, String text, float value, float value2, float value3, int count, float value4) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onFloatStringFloatFloatIntFloatMatrix4f(float value, String text, float value2, float value3, int count, float value4, Matrix4f matrix4f) {
      draw(matrix4f, value3, value4, text, value2, count, value);
   }

   public static void onFloatStringFloatFloatMatrix4fFloatFloatInt(
      float value, String text, float value2, float value3, Matrix4f matrix4f, float value4, float value5, int count
   ) {
      draw(matrix4f, value3, value4, text, value2, count, value);
   }

   public static void onIntStringFloatMatrix4fFloatFloatFloatFloat(
      int count, String text, float value, Matrix4f matrix4f, float value2, float value3, float value4, float value5
   ) {
      draw(matrix4f, value2, value3, text, value, count, value4);
   }

   public static void onIntFloatFloatMatrix4fFloatFloatString(
      int count, float value, float value2, Matrix4f matrix4f, float value3, float value4, String text
   ) {
      draw(matrix4f, value2, value, text, value3, count, value4);
   }

   public static void onMatrix4fFloatFloatIntFloatStringFloatFloat(
      Matrix4f matrix4f, float value, float value2, int count, float value3, String text, float value4, float value5
   ) {
      draw(matrix4f, value, value2, text, value3, count, value4);
   }

   public static void onFloatFloatFloatStringArrayFloatIntMatrix4fFloat(
      float value, float value2, float value3, String[] textArray, float value4, int count, Matrix4f matrix4f, float value5
   ) {
      drawLines(matrix4f, value2, value3, textArray, value4, 0, textArray == null ? 0 : textArray.length, count, value);
   }

   public static void onIntFloatStringArrayMatrix4fFloatFloatFloatFloat(
      int count, float value, String[] textArray, Matrix4f matrix4f, float value2, float value3, float value4, float value5
   ) {
      if (textArray == null || textArray.length == 0) {
         return;
      }
      float f = getFloatByFloat3(value);
      for (int i = 0; i < textArray.length; i++) {
         draw(matrix4f, value2, value3 + (float)i * f, textArray[i], value, count, value4);
      }
   }

   public static void onIntFloatStringArrayFloatMatrix4fIntIntFloatFloat(
      int count, float value, String[] textArray, float value2, Matrix4f matrix4f, int count2, int count3, float value3, float value4
   ) {
      if (textArray != null && textArray.length != 0) {
         int i = Math.max(0, count3);
         int j = Math.min(textArray.length, count2);
         drawLines(matrix4f, value3, value4, textArray, value, i, j, count, 1.0F);
      }
   }
}