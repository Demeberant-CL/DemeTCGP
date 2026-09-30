package com.example.data.repository

import com.example.data.model.PokemonCard

data class AiDeckBuildResult(
  val deckName: String,
  val archetype: String,
  val entries: List<DeckCardEntry>,
  val replacementCount: Int,
)

object MetaDeckAiEngine {

  fun generateMetaDeckWithAlternatives(
    preset: MetaDeckPreset,
    userInventory: List<CardWithInventory>,
  ): AiDeckBuildResult {
    val ownedMap = userInventory.associateBy { it.card.id.uppercase() }
    val evaluatedEntries = mutableMapOf<String, DeckCardEntry>()
    var replacementCount = 0

    val requiredCounts = preset.requiredCardIds.groupingBy { it.uppercase() }.eachCount()

    requiredCounts.forEach { (cardId, requiredCount) ->
      val ownedItem = ownedMap[cardId]
      val availableOwned = ownedItem?.ownedCount ?: 0

      val cardToUse = CardCatalog.getCardById(cardId)
      if (cardToUse != null) {
        val countToTake = minOf(requiredCount, if (availableOwned > 0) availableOwned else 0)
        if (countToTake > 0) {
          evaluatedEntries[cardToUse.id.uppercase()] = DeckCardEntry(cardToUse, countToTake)
        }

        val missingCount = requiredCount - countToTake
        if (missingCount > 0) {
          replacementCount += missingCount
          val substitute = findSmartSubstitute(
            targetCard = cardToUse,
            userInventory = userInventory,
            excludedIds = evaluatedEntries.keys,
          )

          if (substitute != null) {
            val existing = evaluatedEntries[substitute.card.id.uppercase()]
            if (existing != null) {
              evaluatedEntries[substitute.card.id.uppercase()] = existing.copy(count = existing.count + missingCount)
            } else {
              evaluatedEntries[substitute.card.id.uppercase()] = DeckCardEntry(substitute.card, missingCount)
            }
          } else {
            val fallback = userInventory.firstOrNull { (it.ownedCount > 0) && (it.card.id.uppercase() !in evaluatedEntries) }
            if (fallback != null) {
              evaluatedEntries[fallback.card.id.uppercase()] = DeckCardEntry(fallback.card, missingCount)
            } else {
              evaluatedEntries[cardToUse.id.uppercase()] = DeckCardEntry(cardToUse, missingCount)
            }
          }
        }
      }
    }

    val finalEntries = evaluateAndNormalizeTo20(evaluatedEntries.values.toList(), userInventory)

    return AiDeckBuildResult(
      deckName = preset.name,
      archetype = preset.archetype,
      entries = finalEntries,
      replacementCount = replacementCount,
    )
  }

  private fun findSmartSubstitute(
    targetCard: PokemonCard,
    userInventory: List<CardWithInventory>,
    excludedIds: Set<String>,
  ): CardWithInventory? {
    val ownedAvailable = userInventory.filter { (it.ownedCount > 0) && (it.card.id.uppercase() !in excludedIds) }

    // 1. Priorizar mismo tipo elemental
    val sameType = ownedAvailable.filter { it.card.type.equals(targetCard.type, ignoreCase = true) }
    if (sameType.isNotEmpty()) {
      return sameType.maxByOrNull { it.card.hp }
    }

    // 2. Entrenadores
    if (targetCard.type.equals("Entrenador", ignoreCase = true)) {
      val trainers = ownedAvailable.filter { it.card.type.equals("Entrenador", ignoreCase = true) }
      if (trainers.isNotEmpty()) return trainers.first()
    }

    // 3. Mayor cantidad poseída
    return ownedAvailable.maxByOrNull { it.ownedCount }
  }

  private fun evaluateAndNormalizeTo20(
    entries: List<DeckCardEntry>,
    userInventory: List<CardWithInventory>,
  ): List<DeckCardEntry> {
    val mutableList = entries.toMutableList()
    var total = mutableList.sumOf { it.count }

    if (total == 20) return mutableList

    if (total < 20) {
      val fallbackPool = userInventory.filter { it.ownedCount > 0 }
      while ((total < 20) && (fallbackPool.isNotEmpty())) {
        for (item in fallbackPool) {
          if (total >= 20) break
          val existing = mutableList.find { it.card.id.equals(item.card.id, ignoreCase = true) }
          if (existing != null) {
            val idx = mutableList.indexOf(existing)
            if (existing.count < 2) {
              mutableList[idx] = existing.copy(count = existing.count + 1)
              total++
            }
          } else {
            mutableList.add(DeckCardEntry(item.card, 1))
            total++
          }
        }
        break
      }
    } else if (total > 20) {
      while ((total > 20) && (mutableList.isNotEmpty())) {
        val last = mutableList.last()
        if (last.count > 1) {
          val idx = mutableList.indexOf(last)
          mutableList[idx] = last.copy(count = last.count - 1)
        } else {
          mutableList.removeAt(mutableList.size - 1)
        }
        total = mutableList.sumOf { it.count }
      }
    }

    return mutableList
  }
}
