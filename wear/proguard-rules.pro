# The Data Layer envelopes and payloads are read and written by Gson through reflection, so R8 must keep
# their classes and field names: a renamed field changes the wire format the phone mirrors, and the phone
# reads it by the original names (WearPayloadWireParityTest on the phone checks the two sets are equal).
-keep class com.sza.tourist.wear.domain.model.WearEventEnvelope { *; }
-keep class com.sza.tourist.wear.domain.model.WearSettingsPayload { *; }
-keep class com.sza.tourist.wear.domain.model.WearTouristHealthPayload { *; }
-keep class com.sza.tourist.wear.domain.model.WearTouristWeatherPayload { *; }
-keep class com.sza.tourist.wear.domain.model.WearTouristWeatherRequestPayload { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
