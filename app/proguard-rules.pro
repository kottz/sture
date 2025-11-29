# Keep Room entities
-keep class dev.minlauncher.data.model.** { *; }

# Keep data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
