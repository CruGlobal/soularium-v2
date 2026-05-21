package org.cru.soularium

import androidx.compose.ui.window.ComposeUIViewController
import org.cru.soularium.di.initKoin

// PascalCase is required: this is the Kotlin/Native entry point consumed by
// the Swift side as MainViewControllerKt.MainViewController().
@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController(
        // The Xcode project uses a generated Info.plist and can't carry the
        // CADisableMinimumFrameDurationOnPhone key; the strict check is
        // disabled here. (ProMotion 120Hz opt-in is a later nicety.)
        configure = { enforceStrictPlistSanityCheck = false },
    ) {
        initKoin()
        App()
    }
