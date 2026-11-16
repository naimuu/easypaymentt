# Add project specific ProGuard rules here.
-keep class com.easyq.easypayment.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
