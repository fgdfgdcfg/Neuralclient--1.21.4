package client.module;

import client.concurrent.Translations;

public enum Category {
   COMBAT("Combat", CategoryType.COMBAT),
   MOVEMENT("Movement", CategoryType.MOVEMENT),
   PLAYER("Player", CategoryType.PLAYER),
   RENDER("Render", CategoryType.RENDER),
   VISUAL("Visual", CategoryType.VISUAL),
   CLIENT("Client", CategoryType.CLIENT);

   private final String text;
   private final CategoryType categoryType;
   private final Category category;

   Category(String text2, CategoryType categoryType2, Category category2) {
      this.text = text2;
      this.categoryType = categoryType2;
      this.category = category2;
   }

   Category(String text, CategoryType categoryType) {
      this(text, categoryType, null);
   }

   public String getText() {
      return this.text;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean check() {
      return this.category != null;
   }

   public CategoryType getCategoryType() {
      return this.categoryType;
   }

   public static Category getCategoryByString(String text) {
      return Enum.valueOf(Category.class, text);
   }

   public String getString() {
      return Translations.getInstance().getStringByString2(this.text);
   }
}