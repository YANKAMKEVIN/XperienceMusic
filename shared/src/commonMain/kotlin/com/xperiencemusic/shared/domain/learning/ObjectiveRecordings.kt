package com.xperiencemusic.shared.domain.learning

import kotlinx.coroutines.flow.Flow

data class ObjectiveRecording(val memberId:String,val objectiveId:String,val audioPath:String,val seconds:Int)
interface ObjectiveRecordings {
    fun observe(memberId:String):Flow<List<ObjectiveRecording>>
    suspend fun send(memberId:String,objectiveId:String,wav:ByteArray,seconds:Int)
}
