package com.xperiencemusic.shared.presentation.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.usecase.ObserveTabAlertsUseCase
import com.xperiencemusic.shared.domain.usecase.TabAlerts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The dots of the tab bar: what waits for the member, said as soon as the app opens. */
class TabAlertsViewModel(observeTabAlerts: ObserveTabAlertsUseCase) : ViewModel() {
    val state: StateFlow<TabAlerts> =
        observeTabAlerts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TabAlerts())
}
