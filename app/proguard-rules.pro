# R8 & ProGuard rules for MoveUrAss
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep Room database & entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep domain models & enums
-keep enum com.kegeltrainer.app.domain.model.** { *; }
-keep class com.kegeltrainer.app.domain.model.** { *; }
-keep class com.kegeltrainer.app.data.db.** { *; }

# Keep kotlinx.serialization data classes and companion serializers
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.kegeltrainer.app.data.backup.** { *; }
