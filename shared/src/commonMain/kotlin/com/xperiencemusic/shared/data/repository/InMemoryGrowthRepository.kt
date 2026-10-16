package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/** Demo growth: confirming a request moves the member up, as the server's trigger does. */
class InMemoryGrowthRepository(
    private val sessions: SessionRepository,
    private val members: MembersRepository,
    private val dates: DateProvider,
    requests: List<LevelRequest> = emptyList(),
    checks: List<ObjectiveCheck> = emptyList(),
) : GrowthRepository {
    private val requests = MutableStateFlow(requests)
    private val checks = MutableStateFlow(checks)
    private var nextId = 1

    override fun observeRequests(): Flow<List<LevelRequest>> = requests

    override fun observeChecks(): Flow<List<ObjectiveCheck>> = checks

    override suspend fun requestLevel(memberId: String, target: Level) {
        val member = sessions.currentMember()
        val previous = requests.value.firstOrNull { it.memberId == memberId && it.targetLevel == target }
        val request = LevelRequest(
            id = previous?.id ?: "level-${nextId++}",
            memberId = memberId,
            memberName = member?.profile?.displayName.orEmpty(),
            instrument = member?.profile?.instrument,
            voicePart = member?.profile?.voicePart,
            targetLevel = target,
            status = ExerciseStatus.SUBMITTED,
            feedback = null,
            submittedOn = dates.today(),
        )
        requests.update { list -> listOf(request) + list.filterNot { it.id == request.id } }
    }

    override suspend fun reviewRequest(requestId: String, status: ExerciseStatus, feedback: String?) {
        requests.update { list -> list.map { if (it.id == requestId) it.copy(status = status, feedback = feedback) else it } }
        val request = requests.value.firstOrNull { it.id == requestId } ?: return
        if (status == ExerciseStatus.VALIDATED) {
            members.observeMembers().first().firstOrNull { it.id == request.memberId }?.let { members.changeLevel(it, request.targetLevel) }
        }
    }

    override suspend fun tickObjective(memberId: String, objectiveId: String) {
        val member = sessions.currentMember()
        val check = ObjectiveCheck(memberId, member?.profile?.displayName.orEmpty(), member?.profile?.instrument, objectiveId, ExerciseStatus.SUBMITTED, null, dates.today())
        checks.update { list -> listOf(check) + list.filterNot { it.memberId == memberId && it.objectiveId == objectiveId } }
    }

    override suspend fun untickObjective(memberId: String, objectiveId: String) {
        checks.update { list -> list.filterNot { it.memberId == memberId && it.objectiveId == objectiveId } }
    }

    override suspend fun reviewObjective(memberId: String, objectiveId: String, status: ExerciseStatus, feedback: String?) {
        checks.update { list ->
            list.map { if (it.memberId == memberId && it.objectiveId == objectiveId) it.copy(status = status, feedback = feedback) else it }
        }
    }
}
