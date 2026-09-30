package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.CardDao
import com.example.data.local.InventoryCardEntity
import com.example.data.local.InventoryDao
import com.example.data.local.SavedDeckDao
import com.example.data.local.SavedDeckEntity
import com.example.data.local.UserCardDao
import com.example.data.local.UserCardEntity
import com.example.data.model.PokemonCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CardWithInventory(
  val card: PokemonCard,
  val ownedCount: Int,
  val isWishlist: Boolean,
)

data class ParsedCsvCard(
  val setCode: String,
  val cardNumber: String,
  val name: String,
  val rarity: String,
  val quantity: Int,
  val isRegistered: Boolean,
)

class InventoryRepository(
  private val inventoryDao: InventoryDao,
  private val savedDeckDao: SavedDeckDao,
  private val userCardDao: UserCardDao,
  private val cardDao: CardDao,
) {

  val inventoryFlow: Flow<List<CardWithInventory>> = combine(
    inventoryDao.getAllCardsFlow(),
    cardDao.getAllCards(),
  ) { invEntities, cardEntities ->
    val invMap = invEntities.associateBy { it.cardId.uppercase() }
    val cardMap = cardEntities.associateBy { "${it.setId}-${it.cardId}".uppercase() }

    val allKnownCards = (CardCatalog.ALL_CARDS + invEntities.map { entity ->
      CardCatalog.getCardById(entity.cardId) ?: CardCatalog.registerCard(
        id = entity.cardId,
        name = entity.cardName,
        raritySymbol = entity.rarity,
      )
    } + cardEntities.map { entity ->
      val fullId = "${entity.setId}-${entity.cardId}"
      CardCatalog.getCardById(fullId) ?: CardCatalog.registerCard(
        id = fullId,
        name = entity.cardName,
      )
    }).distinctBy { it.id.uppercase() }

    allKnownCards.map { card ->
      val inv = invMap[card.id.uppercase()]
      val cardEnt = cardMap[card.id.uppercase()]
      val count = maxOf(inv?.quantity ?: 0, cardEnt?.ownedCount ?: 0)
      val wish = (inv?.isWishlist == true) || (cardEnt?.isWishlisted == true)

      CardWithInventory(
        card = card,
        ownedCount = count,
        isWishlist = wish,
      )
    }
  }

  val savedDecksFlow: Flow<List<SavedDeckEntity>> = savedDeckDao.getAllSavedDecksFlow()

  suspend fun saveDeck(deck: SavedDeckEntity): Long {
    return savedDeckDao.insertDeck(deck)
  }

  suspend fun deleteDeck(deckId: Long) {
    savedDeckDao.deleteDeckById(deckId)
  }

  suspend fun toggleWishlist(cardId: String) {
    val existing = inventoryDao.getCardById(cardId)
    val card = CardCatalog.getCardById(cardId) ?: return
    if (existing == null) {
      inventoryDao.insertCard(
        InventoryCardEntity(
          cardId = card.id,
          cardName = card.name,
          packName = card.pack.displayName,
          rarity = card.rarity.displayName,
          quantity = 0,
          isWishlist = true,
        ),
      )
    } else {
      inventoryDao.updateCard(existing.copy(isWishlist = !existing.isWishlist))
    }
  }

  suspend fun processParsedCsvCards(csvCards: List<ParsedCsvCard>) {
    val inventoryEntities = mutableListOf<InventoryCardEntity>()
    val userCardEntities = mutableListOf<UserCardEntity>()

    csvCards.forEach { parsed ->
      val formattedId = "${parsed.setCode}-${parsed.cardNumber}"

      val registeredCard = CardCatalog.registerCard(
        id = formattedId,
        name = parsed.name,
        raritySymbol = parsed.rarity,
      )

      val existing = inventoryDao.getCardById(registeredCard.id)

      inventoryEntities.add(
        InventoryCardEntity(
          cardId = registeredCard.id,
          cardName = registeredCard.name,
          packName = registeredCard.pack.displayName,
          rarity = registeredCard.rarity.displayName,
          quantity = parsed.quantity,
          isWishlist = existing?.isWishlist ?: false,
        ),
      )

      userCardEntities.add(
        UserCardEntity(
          cardId = registeredCard.id,
          setCode = parsed.setCode,
          cardNumber = parsed.cardNumber,
          name = registeredCard.name,
          rarity = parsed.rarity,
          quantity = parsed.quantity,
          isRegistered = parsed.isRegistered,
          isFavorite = existing?.isWishlist ?: false,
        ),
      )
    }

    if (inventoryEntities.isNotEmpty()) {
      inventoryDao.insertCards(inventoryEntities)
    }
    if (userCardEntities.isNotEmpty()) {
      userCardDao.insertUserCards(userCardEntities)
    }
  }

  companion object {
    fun fromDatabase(db: AppDatabase): InventoryRepository {
      return InventoryRepository(
        inventoryDao = db.inventoryDao(),
        savedDeckDao = db.savedDeckDao(),
        userCardDao = db.userCardDao(),
        cardDao = db.cardDao(),
      )
    }
  }
}
