package client.concurrent;

import client.module.Category;
import client.module.CategoryType;
import client.module.Module;
import client.module.client.HudModule;
import client.module.client.SliskGui;
import client.module.client.StreamBypass;
import client.module.client.ThemeModule;
import client.module.client.Waypoints;
import client.module.movement.FreeCam;
import client.module.player.AutoSwap;
import client.module.player.ItemTracker;
import client.module.player.Protect;
import client.module.player.SwapWheel;
import client.module.visual.Enhancer;
import client.module.visual.FullBright;
import client.setting.BooleanSetting;
import client.setting.ChoiceSetting;
import client.setting.InputSetting;
import client.setting.Setting;
import client.setting.SliderSetting;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModuleRegistry {
   private final SystemClient systemClient;
   private final Map<String, Module> map = new HashMap<>();
   private List<Module> list = new ArrayList<>();
   private boolean built;

   public ModuleRegistry(SystemClient systemClient2) {
      this.systemClient = systemClient2;
   }

   private synchronized void build() {
      if (this.built) {
         return;
      }
      this.built = true;
      ArrayList<Module> arraylist = new ArrayList<>();
      try {
         if (ModuleClass.INSTANCE != null && ModuleClass.INSTANCE.getObject() != null) {
            for (fun.neural.client.modules.Module neuralModule : ModuleClass.INSTANCE.getObject()) {
               Module module = this.getModuleByNeural(neuralModule);
               if (module != null) {
                  arraylist.add(module);
               }
            }
         }
      } catch (Exception exception) {
      }
      arraylist.add(new ThemeModule());
      arraylist.add(new HudModule());
      arraylist.add(new SliskGui());
      arraylist.add(new StreamBypass());
      arraylist.add(new Waypoints());
      arraylist.add(new FreeCam());
      arraylist.add(new AutoSwap());
      arraylist.add(new ItemTracker());
      arraylist.add(new Protect());
      arraylist.add(new SwapWheel());
      arraylist.add(new Enhancer());
      arraylist.add(new FullBright());
      this.list = arraylist;
      for (Module module2 : arraylist) {
         this.map.put(module2.getName(), module2);
      }
   }

   private Module getModuleByNeural(fun.neural.client.modules.Module neuralModule) {
      try {
         String name = neuralModule.getName();
         if (name == null) {
            return null;
         }
         Module module = new Module(name, getCategoryByNeural(neuralModule.getCategory()));
         module.setWrapped(neuralModule);
         if (neuralModule.getKey() > 0) {
            module.getKeybindSetting().setInt(neuralModule.getKey());
         }
         for (fun.neural.client.modules.settings.Setting neuralSetting : neuralModule.getSettings()) {
            Setting setting = convertSetting(neuralSetting);
            if (setting != null) {
               module.addSetting(setting);
            }
         }
         return module;
      } catch (Exception exception) {
         return null;
      }
   }

   private static Category getCategoryByNeural(fun.neural.client.modules.Module.ModuleCategory moduleCategory) {
      if (moduleCategory == null) {
         return Category.RENDER;
      } else {
         switch (moduleCategory) {
            case COMBAT:
               return Category.COMBAT;
            case MOVEMENT:
               return Category.MOVEMENT;
            case PLAYER:
               return Category.PLAYER;
            case RENDER:
               return Category.RENDER;
            case MISC:
            default:
               return Category.VISUAL;
         }
      }
   }

   private Setting convertSetting(fun.neural.client.modules.settings.Setting neuralSetting) {
      try {
         if (neuralSetting instanceof fun.neural.client.modules.settings.implement.BooleanSetting booleansetting) {
            BooleanSetting settingsetting = new BooleanSetting("", "", booleansetting.isState());
            settingsetting.setName(neuralSetting.name());
            settingsetting.setOnChange(() -> {
               booleansetting.setState(settingsetting.isFlag3());
            });
            wireVisible(settingsetting, neuralSetting);
            return settingsetting;
         } else if (neuralSetting instanceof fun.neural.client.modules.settings.implement.FloatSetting floatsetting) {
            SliderSetting slidersetting = new SliderSetting("", "", (double)floatsetting.get(), (double)floatsetting.getMin(), (double)floatsetting.getMax(), (double)floatsetting.getIncrement());
            slidersetting.setName(neuralSetting.name());
            slidersetting.setOnChange(() -> {
               floatsetting.setValue((float)slidersetting.getValue2());
            });
            wireVisible(slidersetting, neuralSetting);
            return slidersetting;
         } else if (neuralSetting instanceof fun.neural.client.modules.settings.implement.ModeSetting modesetting) {
            List<String> listqu = modesetting.getMods();
            if (listqu == null || listqu.size() < 2) {
               return null;
            }
            String text = listqu.get(0);
            String text2 = listqu.get(1);
            ChoiceSetting choicesetting = new ChoiceSetting("", "", new client.data.ChoiceOption(text), new client.data.ChoiceOption(text2), modesetting.is(text2));
            choicesetting.setName(neuralSetting.name());
            choicesetting.setOnChange(() -> {
               modesetting.set(choicesetting.isFlag3() ? text2 : text);
            });
            wireVisible(choicesetting, neuralSetting);
            return choicesetting;
         } else if (neuralSetting instanceof fun.neural.client.modules.settings.implement.TextSetting textsetting) {
            InputSetting inputsetting = new InputSetting("", "", textsetting.get());
            inputsetting.setName(neuralSetting.name());
            inputsetting.setOnChange(() -> {
               textsetting.setText(inputsetting.getString());
            });
            wireVisible(inputsetting, neuralSetting);
            return inputsetting;
         } else {
            return null;
         }
      } catch (Exception exception) {
         return null;
      }
   }

   private static void wireVisible(Setting setting, fun.neural.client.modules.settings.Setting neuralSetting) {
      try {
         if (neuralSetting.visible() != null) {
            setting.setVisibleWhen(() -> neuralSetting.visible());
         }
      } catch (Exception exception) {
      }
   }

   public void update() {
   }

   public void update2() {
   }

   public List<Module> getList32() {
      this.build();
      return this.list;
   }

   public List<Module> getList22() {
      return this.getList32();
   }

   public List<Module> getListByCategory2(Category category) {
      this.build();
      ArrayList<Module> arraylist = new ArrayList<>();
      for (Module module : this.list) {
         if (module.getCategory() == category) {
            arraylist.add(module);
         }
      }
      return arraylist;
   }

   public Module getModuleByString(String text) {
      this.build();
      if (text == null) {
         return null;
      }
      Module module = this.map.get(text);
      if (module != null) {
         return module;
      }
      for (Module module2 : this.list) {
         if (text.equals(module2.getDisplayName())) {
            return module2;
         }
      }
      return null;
   }

   public Module getModuleByClass(Class<? extends Module> moduleClass) {
      this.build();
      for (Module module : this.list) {
         if (moduleClass.isInstance(module)) {
            return module;
         }
      }
      return null;
   }

   public void onConsumer(Consumer consumer) {
   }
}