# Keep JS interface for YouTube IFrame bridge
-keepclassmembers class com.kidstv.app.player.YouTubeJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
