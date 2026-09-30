package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

  @Query("SELECT * FROM cards_collection")
  fun getAllCards(): Flow<List<CardEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateCard(card: CardEntity)

  @Query("UPDATE cards_collection SET ownedCount = :count, lastUpdated = :timestamp WHERE setId = :setId AND cardId = :cardId")
  suspend fun updateOwnedCount(
    setId: String,
    cardId: String,
    count: Int,
    timestamp: Long = System.currentTimeMillis(),
  )

  @Query("UPDATE cards_collection SET ownedCount = ownedCount + 1, lastUpdated = :timestamp WHERE setId = :setId AND cardId = :cardId")
  suspend fun incrementOwnedCount(
    setId: String,
    cardId: String,
    timestamp: Long = System.currentTimeMillis(),
  )
}
