package client.module.client;

import client.module.Category;
import client.module.Module;
import client.setting.BooleanSetting;

public class Waypoints extends Module {
   private final BooleanSetting renderVHude;

   public Waypoints() {
      super("Waypoints", Category.CLIENT);
      this.renderVHude = new BooleanSetting("", "", true);
      this.renderVHude.setName("Рендер в худе");
      this.addSettings(this.renderVHude);
   }

   public BooleanSetting getRenderVHude() {
      return this.renderVHude;
   }

   public boolean check3() {
      return this.isEnabled();
   }

   public boolean check4() {
      return this.isEnabled() && this.renderVHude.isFlag3();
   }

   public double getDouble() {
      return 0.0;
   }
}