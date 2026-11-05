package client.module.player;

import client.module.Category;
import client.module.Module;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class SwapWheel extends Module {
   public SwapWheel() {
      super("SwapWheel", Category.PLAYER);
   }

   public static void onIntFloatIntIntFloatIntFloatFloatBufferBuilderFloatFloatMatrix4f(
      int count, float value, int count2, int count3, float value2, int count4, float value3, float value4, BufferBuilder bufferBuilder, float value5, float value6, Matrix4f matrix4f
   ) {
   }
}