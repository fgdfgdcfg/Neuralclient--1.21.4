package client.render;

import client.module.CategoryType;
import org.joml.Matrix4f;

public class SvgShader {
   private SvgShader() {
   }

   public static void update() {
   }

   public static void update2() {
   }

   public static void update3() {
   }

   public static void update4() {
   }

   private static float clampAlpha(float value) {
      return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
   }

   public static void onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(
      float alpha, int color, Matrix4f matrix4f, float value2, CategoryType categoryType, float value3, float value4, float value5
   ) {
      if (matrix4f == null || value3 <= 0.0F || value5 <= 0.0F) {
         return;
      }
      float f = Math.max(2.0F, Math.min(value3, value5));
      ShapeShader.onFloatFloatMatrix4fFloatFloatIntFloatFloat(
         value4 - f / 2.0F, value2 - f / 2.0F, matrix4f, f, f, color, clampAlpha(alpha), Math.max(2.0F, f * 0.2F)
      );
   }

   public static void onFloatCategoryTypeIntMatrix4fFloatFloatFloat(
      float value, CategoryType categoryType, int count, Matrix4f matrix4f, float value2, float value3, float value4
   ) {
      onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(value2, count, matrix4f, value, categoryType, value3, value4, value3);
   }
}