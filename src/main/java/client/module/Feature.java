package client.module;

import client.api.UiMetrics;
import net.minecraft.client.MinecraftClient;

public class Feature implements UiMetrics {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public void sendMessage(String text) {
      if (mc != null && mc.player != null) {
         mc.player.sendMessage(net.minecraft.text.Text.of(text), false);
      }
   }
}