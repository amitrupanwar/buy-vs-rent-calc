package com.amitr.buyvsrentcalc.data.repository

import com.amitr.buyvsrentcalc.data.local.dao.HomeDecisionDao
import com.amitr.buyvsrentcalc.data.local.entity.HomeDecisionEntity
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HomeDecisionRepository(private val dao: HomeDecisionDao) {

    val allDecisions: Flow<List<HomeDecisionConfig>> = dao.getAllDecisions().map { list ->
        list.map { it.toDomainConfig() }
    }

    suspend fun getDecisionById(id: Long): HomeDecisionConfig? {
        return dao.getDecisionById(id)?.toDomainConfig()
    }

    suspend fun saveDecision(config: HomeDecisionConfig): Long {
        val entity = HomeDecisionEntity.fromDomainConfig(config)
        return dao.insertDecision(entity)
    }

    suspend fun updateDecision(config: HomeDecisionConfig) {
        val entity = HomeDecisionEntity.fromDomainConfig(config)
        dao.updateDecision(entity)
    }

    suspend fun deleteDecision(config: HomeDecisionConfig) {
        val entity = HomeDecisionEntity.fromDomainConfig(config)
        dao.deleteDecision(entity)
    }
}
