package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryAvatarStorage
import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangeAvatarUseCaseTest {
    private val sessions = FakeSessionRepository(member())
    private val storage = InMemoryAvatarStorage()
    private val changeAvatar = ChangeAvatarUseCase(sessions, storage)
    private val jpeg = ByteArray(10) { it.toByte() }

    @Test
    fun `un avatar dessiné remplace les initiales`() = runTest {
        changeAvatar.choosePreset(AvatarPreset.DRUMS_GIRL_2)
        assertEquals(Avatar.Preset(AvatarPreset.DRUMS_GIRL_2), sessions.currentMember()?.avatar)
    }

    @Test
    fun `une photo est rangée dans le dossier du membre puis mise sur son profil`() = runTest {
        val result = changeAvatar.usePhoto(jpeg)
        val avatar = assertIs<Avatar.Photo>(assertIs<DomainResult.Success<*>>(result).let { sessions.currentMember()?.avatar })
        assertTrue(avatar.path.startsWith("me/") && avatar.path.endsWith(".jpg"))
        assertEquals(setOf(avatar.path), storage.paths)
    }

    @Test
    fun `l'ancienne photo est effacée quand on en change`() = runTest {
        changeAvatar.usePhoto(jpeg)
        changeAvatar.usePhoto(jpeg)
        assertEquals(1, storage.paths.size)
        changeAvatar.choosePreset(AvatarPreset.PIANO_BOY_1)
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `si le profil refuse, la photo envoyée est retirée et l'ancien avatar reste`() = runTest {
        changeAvatar.choosePreset(AvatarPreset.SINGER_BOY_1)
        sessions.failure = IllegalStateException("offline")
        val result = changeAvatar.usePhoto(jpeg)
        assertIs<DomainResult.Failure>(result)
        assertTrue(storage.paths.isEmpty())
        assertEquals(Avatar.Preset(AvatarPreset.SINGER_BOY_1), sessions.currentMember()?.avatar)
    }

    @Test
    fun `une photo vide ou trop lourde est refusée sans rien envoyer`() = runTest {
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), changeAvatar.usePhoto(ByteArray(0)))
        assertEquals(DomainResult.Failure(DomainError.FILE_TOO_LARGE), changeAvatar.usePhoto(ByteArray(ChangeAvatarUseCase.MAX_PHOTO_BYTES + 1)))
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `retirer l'avatar revient aux initiales et efface la photo`() = runTest {
        changeAvatar.usePhoto(jpeg)
        changeAvatar.remove()
        assertNull(sessions.currentMember()?.avatar)
        assertTrue(storage.paths.isEmpty())
    }
}
