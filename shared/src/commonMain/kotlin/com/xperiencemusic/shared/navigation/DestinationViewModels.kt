package com.xperiencemusic.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * One [ViewModelStore] per root destination (see `AppUiState.destination`). Held by the activity's
 * store, so a rotation keeps them; moving to another destination clears the others.
 */
class DestinationViewModelStores : ViewModel() {
    private val stores = mutableMapOf<String, ViewModelStore>()

    fun storeFor(destination: String): ViewModelStore = stores.getOrPut(destination) { ViewModelStore() }

    /** Clears the ViewModels of every destination but [destination]. */
    fun keepOnly(destination: String) {
        stores.keys.filter { it != destination }.forEach { stores.remove(it)?.clear() }
    }

    override fun onCleared() {
        stores.values.forEach(ViewModelStore::clear)
        stores.clear()
    }
}

/**
 * Gives [content] the ViewModels of [destination] only. Without it, every root screen shared the
 * activity's: signing out then in again found the sign-in still on the code step with the old code,
 * and the onboarding still "submitting".
 */
@Composable
fun DestinationViewModelScope(destination: String, content: @Composable () -> Unit) {
    val stores = viewModel { DestinationViewModelStores() }
    val owner = remember(stores, destination) {
        val store = stores.storeFor(destination)
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }
    // Once the new destination is on screen, the previous one's ViewModels go.
    DisposableEffect(stores, destination) {
        stores.keepOnly(destination)
        onDispose { }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
