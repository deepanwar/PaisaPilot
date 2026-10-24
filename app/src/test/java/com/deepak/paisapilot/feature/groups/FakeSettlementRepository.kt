package com.deepak.paisapilot.feature.groups

import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SettlementWriteResult
import com.deepak.paisapilot.domain.group.Settlement
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Stores whatever it is given. Balance validation is covered by the domain and Room tests. */
class FakeSettlementRepository : SettlementRepository {
    val settlements = MutableStateFlow<List<Settlement>>(emptyList())
    var creates = 0
    var updates = 0
    var throwOnWrite = false

    /** When set, create and update return this instead of touching the store. */
    var forcedResult: SettlementWriteResult? = null
    var lastWritten: Settlement? = null

    override suspend fun create(settlement: Settlement): SettlementWriteResult {
        creates += 1
        if (throwOnWrite) throw IOException("write failed")
        forcedResult?.let { return it }
        if (settlements.value.any { it.id == settlement.id }) return SettlementWriteResult.AlreadySaved
        lastWritten = settlement
        settlements.value = settlements.value + settlement
        return SettlementWriteResult.Saved
    }

    override suspend fun update(settlement: Settlement): SettlementWriteResult {
        updates += 1
        if (throwOnWrite) throw IOException("write failed")
        forcedResult?.let { return it }
        if (settlements.value.none { it.id == settlement.id }) return SettlementWriteResult.NotFound
        lastWritten = settlement
        settlements.value = settlements.value.map { if (it.id == settlement.id) settlement else it }
        return SettlementWriteResult.Saved
    }

    override suspend fun delete(id: String): GroupActionResult {
        if (throwOnWrite) throw IOException("write failed")
        if (settlements.value.none { it.id == id }) return GroupActionResult.NotFound
        settlements.value = settlements.value.filterNot { it.id == id }
        return GroupActionResult.Done
    }

    override fun observe(id: String): Flow<Settlement?> =
        settlements.map { list -> list.firstOrNull { it.id == id } }

    override fun observeGroup(groupId: String): Flow<List<Settlement>> =
        settlements.map { list -> list.filter { it.groupId == groupId } }
}
