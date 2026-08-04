package org.cru.soularium.firebase

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
 * Firebase access is lazy and defensive: in a process where Firebase isn't initialized (unit
 * tests, previews), `Firebase.crashlytics` throws. Swallowing that keeps this writer inert —
 * a logging call never crashes the process. In the apps themselves Firebase is initialized at
 * startup — automatically on Android via the google-services plugin, and by
 * `FirebaseApp.configure()` in `FirebaseAppDelegate.swift` on iOS.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CrashlyticsLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        try {
            val crashlytics = Firebase.crashlytics
            crashlytics.log("$severity: ($tag) $message")
            if (throwable != null) crashlytics.recordException(throwable)
        } catch (_: Exception) {
            // Firebase isn't initialized in this process (unit tests, previews), so there's no
            // Crashlytics instance to report to. Stay inert rather than let a logging call bring
            // down the process.
        }
    }
}
