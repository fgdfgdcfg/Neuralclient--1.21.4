package client.module;

import client.concurrent.Translations;
import client.setting.KeybindSetting;
import client.setting.Setting;
import java.util.ArrayList;
import java.util.List;

public class Module extends Feature {
   private final String name;
   private final Category category;
   private boolean enabled;
   private final List<Setting> settings = new ArrayList<>();
   protected KeybindSetting keybind;
   private fun.neural.client.modules.Module wrapped;

   public Module(String text, Category category2) {
      this.name = text;
      this.category = category2;
      if (this.keybind == null) {
         KeybindSetting keybindSetting = new KeybindSetting("", "", -1, this::toggle);
         keybindSetting.setName("Клавиша включения");
         this.keybind = keybindSetting;
         this.addSetting(this.keybind);
      }
   }

   public void setWrapped(fun.neural.client.modules.Module wrapped) {
      this.wrapped = wrapped;
   }

   public fun.neural.client.modules.Module getWrapped() {
      return this.wrapped;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void toggle() {
      if (this.wrapped != null) {
         this.wrapped.toggle();
      }
      this.enabled = !this.enabled;
   }

   public void setEnabled(boolean flag) {
      if (this.wrapped != null) {
         this.wrapped.setEnabled(flag);
      }
      this.enabled = flag;
   }

   public void setEnabledSilent(boolean flag) {
      if (this.wrapped != null && this.wrapped.isEnable() != flag) {
         this.wrapped.setEnable(flag);
      }
      this.enabled = flag;
   }

   public boolean isEnabled() {
      return this.wrapped != null ? this.wrapped.isEnable() : this.enabled;
   }

   public String getName() {
      return this.name;
   }

   public String getDisplayName() {
      return Translations.getInstance().getStringByString2(this.name);
   }

   public Category getCategory() {
      return this.category;
   }

   public KeybindSetting getKeybindSetting() {
      return this.keybind;
   }

   public Setting getSettingByName(String text) {
      for (Setting setting : this.settings) {
         if (setting.getName().equals(text)) {
            return setting;
         }
      }
      return null;
   }

   public Setting getSetting(String text) {
      return this.getSettingByName(text);
   }

   public Setting getSettingByHash(String text) {
      for (Setting setting : this.settings) {
         if (setting.getNameHash().equals(text)) {
            return setting;
         }
      }
      return null;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public List<Setting> getSettingsCopy() {
      return new ArrayList<>(this.settings);
   }

   public List<Setting> getVisibleSettings() {
      List<Setting> list = new ArrayList<>();
      for (Setting setting : this.settings) {
         if (setting.isVisible()) {
            list.add(setting);
         }
      }
      return list;
   }

   public int getSettingCount() {
      return this.settings.size();
   }

   public void addSetting(Setting setting) {
      if (setting != null && this.settings.stream().noneMatch(v -> v.getName().equals(setting.getName()))) {
         this.settings.add(setting);
      }
   }

   public void addSettings(Setting... settings2) {
      if (settings2 != null) {
         for (Setting setting : settings2) {
            this.addSetting(setting);
         }
      }
   }

   public boolean isToggling() {
      return false;
   }
}