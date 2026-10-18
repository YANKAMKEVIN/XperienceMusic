package com.xperiencemusic.shared.data.learning

import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.domain.repository.AudioStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ObjectiveRecordingsStoreTest {
    @Test fun replacingAnAttemptCleansOldFileAndKeepsMembersSeparate()=runTest {
        val files=mutableMapOf<String,ByteArray>()
        val storage=object:AudioStorage {
            override suspend fun upload(path:String,mimeType:String,bytes:ByteArray){files[path]=bytes}
            override suspend fun delete(path:String){files.remove(path)}
        }
        val repo=ObjectiveRecordingsStore(storage,null,RemoteLoadMonitor())
        repo.send("me","piano-c2",byteArrayOf(1),4)
        val first=repo.observe("me").first().single().audioPath
        repo.send("me","piano-c2",byteArrayOf(2),5)
        assertEquals(1,files.size);assertFalse(first in files)
        assertEquals(5,repo.observe("me").first().single().seconds)
        assertTrue(repo.observe("other").first().isEmpty())
        repo.resetSession()
        assertTrue(repo.observe("me").first().isEmpty())
    }
}
