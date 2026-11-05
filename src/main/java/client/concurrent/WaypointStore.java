package client.concurrent;

import client.data.Waypoint;
import java.util.List;

public class WaypointStore {
   private static final WaypointStore instance = new WaypointStore();

   private WaypointStore() {
   }

   public static WaypointStore getInstance() {
      return instance;
   }

   public static boolean check() {
      return false;
   }

   public static String getString2() {
      return "none";
   }

   public List<Waypoint> getList() {
      return List.of();
   }

   public List<Waypoint> getListByString(String text) {
      return List.of();
   }

   public void update() {
   }

   public void isString(String text) {
   }

   public void onWaypoint(Waypoint waypoint) {
   }

   public void onStringString(String text, String text2) {
   }

   public void onStringIntIntInt(String text, int count, int count2, int count3) {
   }

   public void onStringBoolean(String text, boolean flag) {
   }

   public Waypoint getWaypointByStringIntIntIntString2(String text, int count, int count2, int count3, String text2) {
      return null;
   }

   public void removeString(String text) {
   }
}