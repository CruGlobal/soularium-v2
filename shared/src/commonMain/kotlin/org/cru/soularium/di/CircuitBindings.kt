package org.cru.soularium.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.jvm.JvmSuppressWildcards

@BindingContainer
@ContributesTo(AppScope::class)
interface CircuitBindings {
    @Multibinds(allowEmpty = true)
    fun presenterFactories(): Set<Presenter.Factory>

    @Multibinds(allowEmpty = true)
    fun uiFactories(): Set<Ui.Factory>

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providesCircuit(
            presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>,
            uiFactories: @JvmSuppressWildcards Set<Ui.Factory>,
        ): Circuit = Circuit.Builder()
            .addPresenterFactories(presenterFactories)
            .addUiFactories(uiFactories)
            .build()
    }
}
