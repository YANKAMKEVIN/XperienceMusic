package com.xperiencemusic.shared.presentation.common

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class LateFallbackTest {
    private fun source(afterMs: Long) = flow {
        delay(afterMs)
        emit("complet")
        delay(5_000)
        emit("mis à jour")
    }

    @Test
    fun `des données arrivées à temps s'affichent sans passer par l'écran partiel`() = runTest {
        source(afterMs = 400).withLateFallback(1500.milliseconds) { "partiel" }.test {
            assertEquals("complet", awaitItem())
            assertEquals("mis à jour", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `des données en retard laissent d'abord la place à l'écran partiel`() = runTest {
        source(afterMs = 4_000).withLateFallback(1500.milliseconds) { "partiel" }.test {
            assertEquals("partiel", awaitItem())
            assertEquals("complet", awaitItem())
            assertEquals("mis à jour", awaitItem())
            awaitComplete()
        }
    }
}
