package org.cru.soularium.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.ui.nav.SoulariumPresenterFactory
import org.cru.soularium.ui.nav.SoulariumUiFactory
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
        @IntoSet
        fun providesSoulariumPresenterFactory(factory: SoulariumPresenterFactory): Presenter.Factory = factory

        @Provides
        @IntoSet
        fun providesSoulariumUiFactory(): Ui.Factory = SoulariumUiFactory

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
