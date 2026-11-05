package client.util;

import client.concurrent.ModuleRegistry;
import client.concurrent.SystemClient;
import client.module.Module;

public final class UnsafeAccess<T extends Module> {
   public static final sun.misc.Unsafe unsafe;

   private final Class<T> moduleClass;

   static {
      sun.misc.Unsafe unsafe2 = null;
      try {
         java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         unsafe2 = (sun.misc.Unsafe)field.get(null);
      } catch (Throwable throwable) {
      }
      unsafe = unsafe2;
   }

   public UnsafeAccess(Class<T> moduleClass2) {
      this.moduleClass = moduleClass2;
   }

   public T getModule2() {
      return this.getModule();
   }

   public T getModule() {
      SystemClient systemclient = SystemClient.getInstance();
      if (systemclient == null) {
         return null;
      }
      ModuleRegistry moduleregistry = systemclient.getModuleRegistry();
      if (moduleregistry == null) {
         return null;
      }
      Module module = moduleregistry.getModuleByClass(this.moduleClass);
      return (T)module;
   }
}