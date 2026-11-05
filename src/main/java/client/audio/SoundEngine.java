package client.audio;

import client.enums.SoundEvent;
import client.module.Feature;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

public class SoundEngine {
   private static final SoundEngine instance = new SoundEngine();
   private boolean flag3;

   private SoundEngine() {
   }

   public static SoundEngine getInstance() {
      return instance;
   }

   public void onSoundEvent(SoundEvent soundEvent) {
      if (soundEvent != null && !this.flag3 && Feature.mc != null && Feature.mc.world != null && Feature.mc.player != null) {
         try {
            float f = Feature.mc.options.getSoundVolume(net.minecraft.sound.SoundCategory.MASTER);
            if (f <= 0.0F) {
               return;
            }
            if (soundEvent == SoundEvent.CATEGORY_SWITCH) {
               Feature.mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1.0F, 1.6F);
            } else if (soundEvent == SoundEvent.GUI_OPEN || soundEvent == SoundEvent.GUI_CLOSE) {
               Feature.mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.2F);
            } else if (soundEvent == SoundEvent.MODULE_ENABLE || soundEvent == SoundEvent.TOGGLE_ON || soundEvent == SoundEvent.FRIEND_ADD || soundEvent == SoundEvent.FAVOURITE_ADD) {
               Feature.mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 2.0F);
            } else if (soundEvent == SoundEvent.MODULE_DISABLE || soundEvent == SoundEvent.TOGGLE_OFF || soundEvent == SoundEvent.FRIEND_REMOVE) {
               Feature.mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 0.8F);
            } else {
               Feature.mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9F, 1.0F);
            }
         } catch (Exception exception) {
         }
      }
   }

   public void setFlag3(boolean flag) {
      this.flag3 = flag;
   }

   public void setBoolean(boolean flag) {
      this.flag3 = flag;
   }

   public void update() {
   }
}