package client.concurrent;

import client.data.ConfigData;
import client.data.ClientAccess;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;

public class ConfigManager {
   private final Gson gson = new GsonBuilder().create();
   private final ModuleRegistry moduleRegistry;
   private final Map<String, float[]> map = new ConcurrentHashMap<>();
   private final Map<String, Map<String, Object>> map2 = new ConcurrentHashMap<>();
   private final List<String> list = new ArrayList<>();
   private final List<String> list2 = new ArrayList<>();
   private final List<Integer> list3 = new ArrayList<>();
   private ConfigData configData = null;
   private Runnable runnable;
   private Supplier<ConfigData> supplier;
   private String text;

   public ConfigManager(ModuleRegistry moduleRegistry2) {
      this.moduleRegistry = moduleRegistry2;
      load();
   }

   private File getFile() {
      try {
         MinecraftClient minecraftclient = MinecraftClient.getInstance();
         return new File(minecraftclient.runDirectory, "clientconfigs/client.json");
      } catch (Exception exception) {
         return null;
      }
   }

   private void load() {
      try {
         File file = this.getFile();
         if (file != null && file.exists()) {
            String s = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            SavedState savedstate = (SavedState)this.gson.fromJson(s, SavedState.class);
            if (savedstate != null) {
               this.configData = savedstate.configData;
               this.text = savedstate.text;
               if (savedstate.map != null) {
                  this.map.clear();
                  this.map.putAll(savedstate.map);
               }
               if (savedstate.map2 != null) {
                  this.map2.clear();
                  this.map2.putAll(savedstate.map2);
               }
               if (savedstate.list != null) {
                  this.list.clear();
                  this.list.addAll(savedstate.list);
               }
               if (savedstate.list2 != null) {
                  this.list2.clear();
                  this.list2.addAll(savedstate.list2);
               }
               if (savedstate.list3 != null) {
                  this.list3.clear();
                  this.list3.addAll(savedstate.list3);
               }
            }
         }
      } catch (Exception exception) {
      }
   }

   private void save() {
      try {
         File file = this.getFile();
         if (file != null) {
            File parent = file.getParentFile();
            if (parent != null) {
               parent.mkdirs();
            }
            SavedState savedstate = new SavedState();
            savedstate.configData = this.configData;
            savedstate.text = this.text;
            savedstate.map = new HashMap<>(this.map);
            savedstate.map2 = new HashMap<>(this.map2);
            savedstate.list = new ArrayList<>(this.list);
            savedstate.list2 = new ArrayList<>(this.list2);
            savedstate.list3 = new ArrayList<>(this.list3);
            Files.write(file.toPath(), this.gson.toJson(savedstate).getBytes(java.nio.charset.StandardCharsets.UTF_8));
         }
      } catch (IOException ioexception) {
      }
   }

   private static class SavedState {
      public ConfigData configData;
      public String text;
      public Map<String, float[]> map;
      public Map<String, Map<String, Object>> map2;
      public List<String> list;
      public List<String> list2;
      public List<Integer> list3;
   }

   public List<String> getList() {
      return new ArrayList<>(this.list);
   }

   public void onList(List list2) {
      this.list.clear();
      if (list2 != null) {
         this.list.addAll(list2);
      }
      this.save();
   }

   public void onList2(List list2) {
      this.list3.clear();
      if (list2 != null) {
         for (Object object : list2) {
            if (object instanceof Integer integer) {
               this.list3.add(integer);
            }
         }
      }
      this.save();
   }

   public void onList3(List list2) {
      this.list2.clear();
      if (list2 != null) {
         this.list2.addAll(list2);
      }
      this.save();
   }

   public List<String> getList2() {
      return new ArrayList<>(this.list2);
   }

   public List<Integer> getList3() {
      return new ArrayList<>(this.list3);
   }

   public void onFloatFloatString(float value, float value2, String text) {
      this.map.put(text, new float[]{value2, value});
      this.save();
   }

   public float[] getFloatArrayByString(String text) {
      return this.map.get(text);
   }

   public Map getMapByString(String text) {
      Map mapx = this.map2.get(text);
      return mapx == null ? null : new HashMap(mapx);
   }

   public ConfigData getConfigData() {
      return this.configData;
   }

   public void setConfigData(ConfigData configData2) {
      this.configData = configData2;
      this.save();
   }

   public void setRunnable(Runnable runnable2) {
      this.runnable = runnable2;
   }

   public void setSupplier(Supplier<ConfigData> supplier2) {
      this.supplier = supplier2;
   }

   public void onString(String text) {
   }

   public void update() {
   }

   public void onStringString(String text, String text2) {
   }

   public void onStringBoolean(String text, boolean flag) {
   }

   public void onStringString2(String text, String text2) {
   }

   public void onStringString3(String text, String text2) {
   }

   public String getText() {
      return this.text;
   }

   public void setString(String text) {
      this.text = text;
      this.save();
   }

   public void update2() {
   }

   public void check2() {
   }

   public boolean isString2(String text) {
      return false;
   }

   public void isString3(String text) {
   }

   public client.data.ConfigEntry getConfigEntryByString(String text) {
      return null;
   }

   public client.data.ConfigEntry getConfigEntryByStringString(String text, String text2) {
      return null;
   }

   public String getStringByString(String text) {
      return null;
   }

   public String getStringByString2(String text) {
      return null;
   }

   public List getListByString(String text) {
      return null;
   }

   public List getList4() {
      return java.util.List.of();
   }
}