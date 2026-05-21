package org.cru.soularium

import androidx.compose.ui.window.ComposeUIViewController
import org.cru.soularium.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}
