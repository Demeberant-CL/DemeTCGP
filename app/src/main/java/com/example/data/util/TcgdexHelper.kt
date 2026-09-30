package com.example.data.util

object TcgdexHelper {
  private const val BASE_ASSETS_URL = "https://assets.tcgdex.net"

  /**
   * Obtiene la URL principal (con mayor probabilidad de éxito) para una carta.
   */
  fun getCardImageUrl(cardFullId: String, lang: String = "es"): String {
    return getCardImageUrlsList(cardFullId, lang).firstOrNull() ?: ""
  }

  /**
   * Genera una lista ordenada de URLs candidatas de mayor a menor probabilidad de éxito.
   * Normaliza el número de la carta con 3 dígitos (ej: "001", "004", "036").
   */
  fun getCardImageUrlsList(cardFullId: String, lang: String = "es"): List<String> {
    if (cardFullId.isBlank()) return emptyList()
    val parts = cardFullId.split("-")
    val setId = if (parts.size > 1) parts[0].trim() else "A1"
    val cardId = if (parts.size > 1) parts[1].trim() else cardFullId.trim()
    return getCardImageUrlsList(setId, cardId, lang)
  }

  /**
   * Genera la lista ordenada de URLs candidatas usando setId y cardId por separado.
   */
  fun getCardImageUrlsList(setId: String, cardId: String, lang: String = "es"): List<String> {
    val cleanSet = setId.lowercase().trim().ifBlank { "a1" }
    val cleanCardId = cardId.trim()

    // Normalizar a 3 dígitos (ej: "001", "036") y formato sin ceros para respaldo
    val digits = cleanCardId.replace(Regex("^[^0-9]*"), "")
    val numInt = digits.toIntOrNull() ?: 1
    val paddedNum = numInt.toString().padStart(3, '0')
    val unpaddedNum = numInt.toString()

    val languages = listOf(lang, "en")
    val extensions = listOf("webp", "png")
    val numberFormats = listOf(paddedNum, unpaddedNum)
    val setFormats = listOf(cleanSet, cleanSet.uppercase())

    val candidates = mutableListOf<String>()

    for (l in languages) {
      for (ext in extensions) {
        for (num in numberFormats) {
          for (set in setFormats) {
            val url = "$BASE_ASSETS_URL/$l/tcgp/$set/$num/high.$ext"
            if (!candidates.contains(url)) {
              candidates.add(url)
            }
          }
        }
      }
    }

    return candidates
  }
}
