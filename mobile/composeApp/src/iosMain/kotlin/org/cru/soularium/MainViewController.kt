package org.cru.soularium

import androidx.compose.ui.window.ComposeUIViewController
import org.cru.soularium.di.initKoin

// PascalCase is required: this is the Kotlin/Native entry point consumed by
// the Swift side as MainViewControllerKt.MainViewController().
@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController {
        initKoin()
        App()
    }
