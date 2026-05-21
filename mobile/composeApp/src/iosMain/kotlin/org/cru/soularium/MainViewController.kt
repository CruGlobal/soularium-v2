package org.cru.soularium

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.window.ComposeUIViewController
import org.cru.soularium.di.initKoin

// PascalCase is required: this is the Kotlin/Native entry point consumed by
// the Swift side as MainViewControllerKt.MainViewController().
@OptIn(ExperimentalComposeApi::class)
@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController(
        configure = {
            // The Xcode project uses a generated Info.plist and can't carry the
            // CADisableMinimumFrameDurationOnPhone key; the strict check is
            // disabled here. (ProMotion 120Hz opt-in is a later nicety.)
            enforceStrictPlistSanityCheck = false
            // Always expose the Compose semantics tree to iOS accessibility so
            // VoiceOver and UI-test automation can read the UI at any time.
            accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
        },
    ) {
        initKoin()
        App()
    }
