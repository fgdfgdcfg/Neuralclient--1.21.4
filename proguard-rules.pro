-dontwarn
-dontoptimize
-repackageclasses ''
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,Signature,InnerClasses,EnclosingMethod,AnnotationDefault

# Entry point
-keep @net.fabricmc.api.Entrypoint class * { *; }
-keep class fun.neural.Neural { *; }
-keep class fun.neural.Neural$$Lambda* { *; }

# Fabric mod metadata
-adaptresourcefilecontents **.json
-adaptresourcefilecontents **.accesswidener
-adaptresourcefilenames **.json

# Mixin classes (referenced by name in neural.mixins.json)
-keep class fun.neural.mixin.** {
    <fields>;
    <methods>;
}
-keep class fun.neural.mixin.**$$Redirector* { *; }
-keep class fun.neural.mixin.**$$Invoker* { *; }
-keep class fun.neural.mixin.**$$Accessor* { *; }

# Keep mixin annotations intact
-keepclassmembers class fun.neural.mixin.** {
    @org.spongepowered.asm.mixin.Mixin <fields>;
    @org.spongepowered.asm.mixin.Mixin <methods>;
    @org.spongepowered.asm.mixin.injection.Inject <methods>;
    @org.spongepowered.asm.mixin.injection.Redirect <methods>;
    @org.spongepowered.asm.mixin.Shadow <methods>;
    @org.spongepowered.asm.mixin.Unique <methods>;
    @net.fabricmc.mixin.api.env.MixinEnvironment$Side <methods>;
}

# Module singleton pattern
-keepclassmembers class fun.neural.client.modules.impl.** {
    public static final * INSTANCE;
}

# Rotation implementations (instantiated from Aura; keep from being shrunk away)
-keep class fun.neural.client.modules.impl.combat.components.rotations.** { *; }

# Module class names (referenced in configs/commands)
-keepnames class fun.neural.client.modules.impl.** { *; }

# ModuleCategory enum
-keepclassmembers enum fun.neural.client.modules.Module$ModuleCategory {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Settings (serialized by name)
-keepnames class fun.neural.client.modules.settings.** { *; }
-keepclassmembers class fun.neural.client.modules.settings.** { *; }

# Lombok generated
-keepnames class * {
    @lombok.* <fields>;
    @lombok.* <methods>;
}

# Keep enum values and their fields
-keepclassmembers enum * {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Minecraft and Fabric (don't touch external libs)
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep class org.spongepowered.** { *; }
-keep class com.google.gson.** { *; }
-keep class org.jetbrains.** { *; }
-keep class org.slf4j.** { *; }

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep annotations
-keep @interface * { *; }

# Keep all annotations on classes/methods/fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.SerializedName <methods>;
}
