package client.module.client;

import client.module.Category;
import client.module.Module;
import client.setting.BooleanSetting;

public class HudModule extends Module {
   private final BooleanSetting vatermarka;
   private final BooleanSetting koordinaty;
   private final BooleanSetting kuldauny;
   private final BooleanSetting kuldaunyTargets;
   private final BooleanSetting effectsZeliy;
   private final BooleanSetting inventar;
   private final BooleanSetting keybindy;
   private final BooleanSetting armor;
   private final BooleanSetting targethud;
   private final BooleanSetting spisokStafa;
   private final BooleanSetting svapBindy;
   private final BooleanSetting uvedomleniya;

   public HudModule() {
      super("HudModule", Category.CLIENT);
      this.vatermarka = s("Водяной знак", true);
      this.koordinaty = s("Координаты", true);
      this.kuldauny = s("Кулдауны", true);
      this.kuldaunyTargets = s("Кулдауны (цели)", false);
      this.effectsZeliy = s("Эффекты зелий", true);
      this.inventar = s("Инвентарь", true);
      this.keybindy = s("Кинбинды", true);
      this.armor = s("Броня", true);
      this.targethud = s("Таргет хул", true);
      this.spisokStafa = s("Список стаффа", true);
      this.svapBindy = s("Свап бинды", true);
      this.uvedomleniya = s("Уведомления", true);
      this.addSettings(this.vatermarka, this.koordinaty, this.kuldauny, this.kuldaunyTargets, this.effectsZeliy, this.inventar, this.keybindy, this.armor, this.targethud, this.spisokStafa, this.svapBindy, this.uvedomleniya);
   }

   private static BooleanSetting s(String name, boolean flag) {
      BooleanSetting booleansetting = new BooleanSetting("", "", flag);
      booleansetting.setName(name);
      return booleansetting;
   }

   public BooleanSetting getVatermarka() {
      return this.vatermarka;
   }

   public BooleanSetting getKoordinaty() {
      return this.koordinaty;
   }

   public BooleanSetting getKuldauny() {
      return this.kuldauny;
   }

   public BooleanSetting getKuldaunyTargets() {
      return this.kuldaunyTargets;
   }

   public BooleanSetting getEffectsZeliy() {
      return this.effectsZeliy;
   }

   public BooleanSetting getInventar() {
      return this.inventar;
   }

   public BooleanSetting getKeybindy() {
      return this.keybindy;
   }

   public BooleanSetting getArmor() {
      return this.armor;
   }

   public BooleanSetting getTargethud() {
      return this.targethud;
   }

   public BooleanSetting getSpisokStafa() {
      return this.spisokStafa;
   }

   public BooleanSetting getSvapBindy() {
      return this.svapBindy;
   }

   public BooleanSetting getUvedomleniya() {
      return this.uvedomleniya;
   }
}