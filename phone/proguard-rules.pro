# Skeleton: nothing to keep yet. The ported classes arrive with their phases (S0003 onward) and bring
# their own rules with them.

# S0008: the Data Layer envelopes and payloads are read and written by Gson through reflection. Their
# fields carry pinned @SerializedName names (the wire contract with the watch module), and R8 must keep
# the annotated fields as they are or a release build would drop or rename what the watch reads.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.sza.tourist.domain.model.WearEventEnvelope { *; }
-keep class com.sza.tourist.domain.model.WearSettingsPayload { *; }
-keep class com.sza.tourist.domain.model.WearTouristHealthPayload { *; }
-keep class com.sza.tourist.domain.model.WearTouristWeatherPayload { *; }
-keep class com.sza.tourist.domain.model.WearTouristWeatherRequestPayload { *; }
