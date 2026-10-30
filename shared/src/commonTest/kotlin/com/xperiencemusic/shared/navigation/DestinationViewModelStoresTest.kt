package com.xperiencemusic.shared.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DestinationViewModelStoresTest {
    private class ScreenViewModel : ViewModel() {
        var isCleared = false
        override fun onCleared() {
            isCleared = true
        }
    }

    private fun ViewModelStore.screenViewModel(): ScreenViewModel =
        ViewModelProvider.create(this, viewModelFactory { initializer { ScreenViewModel() } }, CreationExtras.Empty)[ScreenViewModel::class]

    @Test
    fun `une destination retrouve ses ViewModels, par exemple après une rotation`() {
        val stores = DestinationViewModelStores()
        val signIn = stores.storeFor("sign-in").screenViewModel()
        stores.keepOnly("sign-in")
        assertSame(signIn, stores.storeFor("sign-in").screenViewModel())
        assertFalse(signIn.isCleared)
    }

    @Test
    fun `changer de destination efface les ViewModels des autres`() {
        val stores = DestinationViewModelStores()
        val signIn = stores.storeFor("sign-in").screenViewModel()
        val onboarding = stores.storeFor("onboarding").screenViewModel()
        stores.keepOnly("onboarding")
        assertTrue(signIn.isCleared, "the sign-in of the previous session is dropped")
        assertFalse(onboarding.isCleared)

        stores.storeFor("ready")
        stores.keepOnly("ready")
        assertTrue(onboarding.isCleared)
        // Back on the sign-in after signing out: a blank screen, not the previous code.
        val nextSignIn = stores.storeFor("sign-in").screenViewModel()
        assertNotSame(signIn, nextSignIn)
        assertFalse(nextSignIn.isCleared)
    }

    @Test
    fun `fermer l'app efface toutes les destinations`() {
        val activityStore = ViewModelStore()
        val stores = ViewModelProvider.create(
            activityStore,
            viewModelFactory { initializer { DestinationViewModelStores() } },
            CreationExtras.Empty,
        )[DestinationViewModelStores::class]
        val ready = stores.storeFor("ready").screenViewModel()
        activityStore.clear()
        assertTrue(ready.isCleared)
    }
}
