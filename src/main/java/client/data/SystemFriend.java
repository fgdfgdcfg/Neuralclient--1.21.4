package client.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SystemFriend {
   private static final SystemFriend INSTANCE = new SystemFriend();
   private final Set<String> set = ConcurrentHashMap.newKeySet();
   private final Map<String, String> map = new ConcurrentHashMap<>();
   private final Map<String, String> map2 = new ConcurrentHashMap<>();

   private SystemFriend() {
   }

   public static SystemFriend getInstance() {
      return INSTANCE;
   }

   private static String norm(String text) {
      return text == null ? null : text.trim().toLowerCase();
   }

   public void onStringString3(String text, String text2) {
      this.map.put(norm(text2), text);
   }

   public void onStringString5(String text, String text2) {
      this.map.put(norm(text2), text);
   }

   public void addString2(String text) {
      String s = norm(text);
      if (s != null && !s.isEmpty()) {
         this.set.add(s);
      }
   }

   public void removeString(String text) {
      String s = norm(text);
      this.set.remove(s);
      this.map.remove(s);
      this.map2.remove(s);
   }

   public String getStringByString(String text) {
      return this.map.get(norm(text));
   }

   public String getStringByString2(String text) {
      return this.map2.get(norm(text));
   }

   public String getStringByString4(String text) {
      String s = this.map.get(norm(text));
      return s == null ? this.map2.get(norm(text)) : s;
   }

   public Set<String> getSet() {
      return this.set;
   }
}