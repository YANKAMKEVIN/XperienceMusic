package com.xperiencemusic.shared.data.learning

import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.SessionResettable
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.coroutines.cancellation.CancellationException

@Serializable
private data class RecordingDto(@SerialName("member_id") val memberId:String,@SerialName("objective_id") val objectiveId:String,
    @SerialName("audio_path") val audioPath:String,@SerialName("duration_seconds") val seconds:Int) {
    fun domain()=ObjectiveRecording(memberId,objectiveId,audioPath,seconds)
}

class ObjectiveRecordingsStore(private val storage:AudioStorage,private val client:SupabaseClient?,private val loads:RemoteLoadMonitor):ObjectiveRecordings,SessionResettable {
    private val rows=MutableStateFlow<Map<String,List<ObjectiveRecording>>>(emptyMap())
    private var generation=0
    override fun resetSession() { generation++; rows.value=emptyMap() }
    override fun observe(memberId:String):Flow<List<ObjectiveRecording>> = rows.onStart {
        val started=generation
        if(client!=null) loads.load("objective-audio-$memberId") {
            val fetched=client.from("objective_recordings").select { filter { eq("member_id",memberId) } }.decodeList<RecordingDto>()
            if(started!=generation) throw CancellationException("Session ended")
            rows.value=rows.value+(memberId to fetched.map { it.domain() })
        }
    }.map { it[memberId].orEmpty() }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun send(memberId:String,objectiveId:String,wav:ByteArray,seconds:Int) {
        require(seconds in 3..300 && wav.isNotEmpty())
        val started=generation
        val path="submissions/$memberId/objectives/${Uuid.random()}.wav"
        val old=rows.value[memberId].orEmpty().firstOrNull { it.objectiveId==objectiveId }
        storage.upload(path,"audio/wav",wav)
        try {
            if(started!=generation) throw CancellationException("Session ended")
            client?.from("objective_recordings")?.upsert(RecordingDto(memberId,objectiveId,path,seconds)) { onConflict="member_id,objective_id" }
        } catch(e:Exception) {
            runCatching { storage.delete(path) }; throw e
        }
        if(started!=generation) throw CancellationException("Session ended")
        val value=ObjectiveRecording(memberId,objectiveId,path,seconds)
        rows.value=rows.value+(memberId to (rows.value[memberId].orEmpty().filterNot { it.objectiveId==objectiveId }+value))
        old?.takeIf { it.audioPath!=path }?.let { runCatching { storage.delete(it.audioPath) } }
    }
}
