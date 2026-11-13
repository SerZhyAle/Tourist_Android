package com.sza.tourist.wear.data.wear

/**
 * Path constants for the Wear OS Data Layer - watch side mirror.
 *
 * String values are identical to the phone-side `com.sza.tourist.service.WearDataLayerPaths` to
 * guarantee path matching under this product's `/tourist/` prefix (S0006, S0008). Both modules are
 * checked against each other by `WearDataLayerPathsParityTest`.
 */
object WearDataLayerPaths {

    /**
     * Message, watch -> phone. Starts the phone's distress signal in the mode this watch chose.
     *
     * The payload is the `SosMode` member name and nothing else - the two modules share no source, so
     * those names are the whole wire contract. Mirrored verbatim from the phone module's copy of this
     * object; one differing character produces a message nobody receives.
     */
    const val SOS_START_FROM_WATCH = "/tourist/watch/sos/start"

    /** Message, watch -> phone. Ends the phone's distress signal, whichever device started it. */
    const val SOS_STOP_FROM_WATCH = "/tourist/watch/sos/stop"

    /**
     * Message, phone -> watch. Starts this watch's distress signal in the mode the phone chose.
     *
     * Under the `/tourist/phone` prefix `wear/src/main/AndroidManifest.xml` declares for the watch
     * listener.
     */
    const val SOS_START_FROM_PHONE = "/tourist/phone/sos/start"

    /** Message, phone -> watch. Ends this watch's distress signal, whichever device started it. */
    const val SOS_STOP_FROM_PHONE = "/tourist/phone/sos/stop"

    /**
     * Data Item, phone -> watch. Carries the phone's settings the watch mirrors: the unit system
     * (S2731), the only field this product pushes.
     *
     * Under the `/tourist/wear` prefix the watch listener manifest declares. Mirrored verbatim in the
     * watch module's copy of this object.
     */
    const val SETTINGS_PUSH = "/tourist/wear/settings"

    /**
     * Data Item, watch -> phone. Carries the Tourist heart rate and body temperature (S4150).
     *
     * State, not an event: the phone keeps only the latest report and judges its age by arrival time.
     * Sent only while the Tourist screen on the watch already runs the sensors, so the phone never wakes
     * them. Mirrored verbatim in the other module's copy of this object - the two must not drift.
     */
    const val TOURIST_HEALTH = "/tourist/watch/tourist_health"

    /**
     * Data Item, watch -> phone. Asks for the Tourist weather at a rounded watch position (S4152).
     *
     * Sent only while the Tourist screen on the watch is open; its timestamp field makes every request a
     * change, so the phone hears it even at an unchanged position.
     */
    const val TOURIST_WEATHER_REQUEST = "/tourist/watch/tourist_weather_request"

    /**
     * Data Item, phone -> watch. Answers [TOURIST_WEATHER_REQUEST] with the weather details (S4152).
     *
     * State, not an event: Tourist reads the latest answer on opening and judges its age by the
     * observation time inside it.
     */
    const val TOURIST_WEATHER = "/tourist/phone/tourist_weather"

    // --- WearEventEnvelope.eventType constants ---

    /** eventType value for SETTINGS_PUSH envelopes. */
    const val EVENT_SETTINGS = "SETTINGS_PUSH"

    /** eventType value for TOURIST_HEALTH envelopes (S4150). */
    const val EVENT_TOURIST_HEALTH = "TOURIST_HEALTH"

    /** eventType value for TOURIST_WEATHER_REQUEST envelopes (S4152). */
    const val EVENT_TOURIST_WEATHER_REQUEST = "TOURIST_WEATHER_REQUEST"

    /** eventType value for TOURIST_WEATHER envelopes (S4152). */
    const val EVENT_TOURIST_WEATHER = "TOURIST_WEATHER"
}
