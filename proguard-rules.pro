# PDFBox / Bouncy Castle use reflective provider and font discovery.
-keep class com.tom_roush.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn javax.naming.**
-dontwarn javax.xml.bind.**
# Optional JPEG2000 codec is not bundled (same as earlier Folha releases).
-dontwarn com.gemalto.jp2.**
# Native integration test APK exercises these app contracts; retain names and package visibility.
-keep class br.com.folha.notes.** { *; }
