# Keep the bundled yt-dlp / python bridge intact.
-keep class com.yausername.** { *; }
-keep class org.apache.commons.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.**

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Media3 session service is instantiated by the system.
-keep class dev.auralis.player.PlaybackService { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**
