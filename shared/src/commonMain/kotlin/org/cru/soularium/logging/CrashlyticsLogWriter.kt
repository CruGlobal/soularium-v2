package org.cru.soularium.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Kermit [LogWriter] that forwards log statements to Firebase Crashlytics via the GitLive
 * firebase-kotlin-sdk: each message becomes a Crashlytics breadcrumb `log`, and any attached
 * [Throwable] is recorded as a non-fatal. Contributed into the `Set<LogWriter>` that
 * [org.cru.soularium.di.configureLogging] installs on the global [co.touchlab.kermit.Logger].
 *
 * Firebase access is lazy and defensive: until the config files (`google-services.json` /
 * `GoogleService-Info.plist`) are present, `Firebase.crashlytics` throws because Firebase isn't
 * initialized. Swallowing that keeps this writer inert — never crashing the app — until they land.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CrashlyticsLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        try {
            val crashlytics = Firebase.crashlytics
            crashlytics.log("$severity: ($tag) $message")
            if (throwable != null) crashlytics.recordException(throwable)
        } catch (expected: Exception) {
            // Firebase isn't configured yet (no google-services.json / GoogleService-Info.plist),
            // so there's no Crashlytics instance to report to. Stay inert rather than let a logging
            // call bring down the app; real reporting begins once the config files land.
        }
    }
}
