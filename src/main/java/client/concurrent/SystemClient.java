package client.concurrent;

import client.util.HashUtil;

public class SystemClient {
   private static final SystemClient instance = new SystemClient();
   private final ModuleRegistry moduleRegistry;
   private final ConfigManager configManager;
   private final HashUtil hashUtil;

   private SystemClient() {
      this.moduleRegistry = new ModuleRegistry(this);
      this.configManager = new ConfigManager(this.moduleRegistry);
      this.hashUtil = null;
   }

   public static SystemClient getInstance() {
      return instance;
   }

   public ModuleRegistry getModuleRegistry() {
      return this.moduleRegistry;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public HashUtil getHashUtil() {
      return this.hashUtil;
   }
}