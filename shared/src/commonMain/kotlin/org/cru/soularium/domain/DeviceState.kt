package org.cru.soularium.domain

/**
 * Per-device preferences that live outside the session database.
 *
 * @param hasSeenIntro whether the two-page Intro onboarding has been shown.
 * @param agreedToTos  whether the user has accepted the Terms of Use.
 */
data class DeviceState(val hasSeenIntro: Boolean = false, val agreedToTos: Boolean = false)
