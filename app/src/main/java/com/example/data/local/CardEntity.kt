package com.example.data.local

import androidx.room.Entity

@Entity(
  tableName = "cards_collection",
  primaryKeys = ["setId", "cardId"],
)
data class CardEntity(
  val setId: String,
  val cardId: String,
  val cardName: String,
  val ownedCount: Int = 0,
  val isWishlisted: Boolean = false,
  val lastUpdated: Long = System.currentTimeMillis(),
)
