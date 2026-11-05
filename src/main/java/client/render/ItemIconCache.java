package client.render;

import client.module.Feature;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

public final class ItemIconCache {
   private static DrawContext drawContext = null;

   private ItemIconCache() {
   }

   public static DrawContext getDrawContext() {
      return drawContext;
   }

   public static void setDrawContext(DrawContext drawContext2) {
      drawContext = drawContext2;
   }

   public static void update() {
   }

   private static void render(DrawContext drawcontext, Matrix4f matrix4f, float size, ItemStack itemStack, float y, float x, float alpha) {
      MatrixStack matrixstack = drawcontext.getMatrices();
      matrixstack.push();
      try {
         matrixstack.peek().getPositionMatrix().set(matrix4f);
         matrixstack.translate(x, y, 0.0F);
         if (size != 16.0F) {
            float f = size / 16.0F;
            matrixstack.scale(f, f, 1.0F);
         }
         boolean flagx = alpha < 1.0F;
         if (flagx) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         }
         try {
            if (Feature.mc != null && Feature.mc.getItemRenderer() != null) {
               drawcontext.drawItem(itemStack, 0, 0);
            }
            drawcontext.draw();
         } finally {
            if (flagx) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      } finally {
         matrixstack.pop();
      }
   }

   public static void onFloatFloatFloatItemStackMatrix4fFloat(float size, float alpha, float x, ItemStack itemStack, Matrix4f matrix4f, float y) {
      if (itemStack == null || itemStack.isEmpty() || alpha <= 0.001F || size <= 0.0F) {
         return;
      }
      DrawContext drawcontext = drawContext;
      if (drawcontext == null) {
         drawcontext = ItemIconCache.getDrawContext();
      }
      if (drawcontext == null) {
         return;
      }
      render(drawcontext, matrix4f, size, itemStack, y, x, alpha);
   }

   public static void onFloatItemStackFloatFloatFloatDrawContext(float size, ItemStack itemStack, float alpha, float y, float x, DrawContext drawcontext) {
      if (itemStack == null || itemStack.isEmpty() || alpha <= 0.001F || size <= 0.0F) {
         return;
      }
      MatrixStack matrixstack = drawcontext.getMatrices();
      matrixstack.push();
      matrixstack.translate(x, y, 0.0F);
      if (size != 16.0F) {
         float f = size / 16.0F;
         matrixstack.scale(f, f, 1.0F);
      }
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      try {
         if (Feature.mc != null && Feature.mc.getItemRenderer() != null) {
            drawcontext.drawItem(itemStack, 0, 0);
         }
         drawcontext.draw();
      } finally {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         matrixstack.pop();
      }
   }
}