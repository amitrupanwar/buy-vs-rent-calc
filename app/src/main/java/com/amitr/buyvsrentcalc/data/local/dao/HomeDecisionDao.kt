package com.amitr.buyvsrentcalc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.amitr.buyvsrentcalc.data.local.entity.HomeDecisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDecisionDao {

    @Query("SELECT * FROM home_decisions ORDER BY createdTimestamp DESC")
    fun getAllDecisions(): Flow<List<HomeDecisionEntity>>

    @Query("SELECT * FROM home_decisions WHERE id = :id")
    suspend fun getDecisionById(id: Long): HomeDecisionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(entity: HomeDecisionEntity): Long

    @Update
    suspend fun updateDecision(entity: HomeDecisionEntity)

    @Delete
    suspend fun deleteDecision(entity: HomeDecisionEntity)
}
