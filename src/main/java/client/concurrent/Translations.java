package client.concurrent;

import client.enums.Language;
import fun.neural.Neural;

public class Translations {
   private static final Translations instance = new Translations();

   private Translations() {
   }

   public static Translations getInstance() {
      return instance;
   }

   public String getStringByString(int hash) {
      return null;
   }

   public String getStringByString(String text) {
      return text;
   }

   public String getStringByString2(String text) {
      if (text == null) {
         return null;
      }
      try {
         if (Neural.INSTANCE != null
            && Neural.INSTANCE.localizationStorage != null
            && Neural.INSTANCE.localizationStorage.getLanguage() != null) {
            String s = Neural.INSTANCE.localizationStorage.translate(text);
            if (s != null && !s.equals(text)) {
               return s;
            }
         }
      } catch (Exception exception) {
      }
      return text;
   }

public Language getLanguage() {
       try {
          if (Neural.INSTANCE != null && Neural.INSTANCE.localizationStorage != null) {
             Object object = Neural.INSTANCE.localizationStorage.getLanguage();
             if (object == client.enums.Language.RU) {
                return client.enums.Language.RU;
             }
             if (object == client.enums.Language.EN) {
                return client.enums.Language.EN;
             }
          }
       } catch (Exception exception) {
       }
       return Language.RU;
    }

    public void addRunnable(Runnable runnable) {
       if (runnable != null) {
          runnable.run();
       }
    }

   public void update() {
   }

   public void update2() {
   }

   public void update3() {
   }
}