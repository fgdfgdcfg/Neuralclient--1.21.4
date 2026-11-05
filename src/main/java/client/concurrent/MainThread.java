package client.concurrent;

import net.minecraft.client.MinecraftClient;

public final class MainThread {
   private MainThread() {
   }

   public static void onRunnable(Runnable runnable) {
      if (runnable != null) {
         MinecraftClient minecraftclient = MinecraftClient.getInstance();
         if (minecraftclient != null) {
            minecraftclient.execute(runnable);
         }
      }
   }

   public static void onRunnable2(Runnable runnable) {
      onRunnable(runnable);
   }
}