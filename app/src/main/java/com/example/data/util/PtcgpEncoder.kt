package com.example.data.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.example.data.repository.DeckCardEntry
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

object PtcgpEncoder {

  private const val OFFICIAL_DECK_URL_BASE = "https://pocket.pokemon.com/deck/share"

  /**
   * Genera el payload JSON oficial estructurado para el escáner de mazos de Pokémon TCG Pocket (PTCGP).
   */
  fun encodeDeckToJson(entries: List<DeckCardEntry>): JSONObject {
    val json = JSONObject()
    json.put("v", 1)

    // Deducir tipos de energía requeridos de las cartas del mazo
    val energies = mutableSetOf<String>()
    entries.forEach { entry ->
      when (entry.card.type.lowercase()) {
        "fuego" -> energies.add("fire")
        "agua" -> energies.add("water")
        "planta" -> energies.add("grass")
        "rayo" -> energies.add("lightning")
        "psíquico" -> energies.add("psychic")
        "lucha" -> energies.add("fighting")
        "oscuridad" -> energies.add("darkness")
        "metal" -> energies.add("metal")
      }
    }
    if (energies.isEmpty()) {
      energies.add("colorless")
    }

    val energyArray = JSONArray()
    energies.forEach { energyArray.put(it) }
    json.put("energies", energyArray)

    val cardsArray = JSONArray()
    entries.forEach { entry ->
      val cardObj = JSONObject()
      cardObj.put("id", entry.card.id)
      cardObj.put("qty", entry.count)
      cardsArray.put(cardObj)
    }
    json.put("cards", cardsArray)

    return json
  }

  /**
   * Serializa el mazo de 20 cartas a un flujo binario de bytes de alta compresión:
   * - Encabezado mágico de 4 bytes ("PTCG": 0x50, 0x54, 0x43, 0x47)
   * - Versión de protocolo: 0x01
   * - Bitmask de Energías (1 byte): indicador de tipos elementales
   * - Total de entradas (1 byte)
   * - Tuplas por carta: [Número de Carta 2 Bytes, Cantidad 1 Byte]
   */
  fun encodeDeckToBinaryBytes(entries: List<DeckCardEntry>): ByteArray {
    val bos = ByteArrayOutputStream()
    bos.write(byteArrayOf(0x50.toByte(), 0x54.toByte(), 0x43.toByte(), 0x47.toByte()))
    bos.write(0x01)

    var energyMask = 0
    entries.forEach { entry ->
      when (entry.card.type.lowercase()) {
        "planta" -> energyMask = energyMask or (1 shl 0)
        "fuego" -> energyMask = energyMask or (1 shl 1)
        "agua" -> energyMask = energyMask or (1 shl 2)
        "rayo" -> energyMask = energyMask or (1 shl 3)
        "psíquico" -> energyMask = energyMask or (1 shl 4)
        "lucha" -> energyMask = energyMask or (1 shl 5)
        "oscuridad" -> energyMask = energyMask or (1 shl 6)
        "metal" -> energyMask = energyMask or (1 shl 7)
      }
    }
    bos.write(energyMask)
    bos.write(entries.size)

    entries.forEach { entry ->
      val numStr = entry.card.id.substringAfter("-").filter { it.isDigit() }
      val num = numStr.toIntOrNull() ?: 1
      bos.write((num ushr 8) and 0xFF)
      bos.write(num and 0xFF)
      bos.write(entry.count and 0xFF)
    }

    return bos.toByteArray()
  }

  /**
   * Retorna la representación en Base64 URL-Safe de la estructura binaria.
   */
  fun encodeDeckToBinaryBase64(entries: List<DeckCardEntry>): String {
    val bytes = encodeDeckToBinaryBytes(entries)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
  }

  /**
   * Genera la URL de Deep Link oficial reconocida por el escáner nativo y la cámara del sistema.
   */
  fun encodeDeckToDeepLinkUrl(entries: List<DeckCardEntry>): String {
    return try {
      val jsonString = encodeDeckToJson(entries).toString()
      val base64Data = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
      val encodedData = URLEncoder.encode(base64Data, "UTF-8")
      "$OFFICIAL_DECK_URL_BASE?v=1&data=$encodedData"
    } catch (_: Exception) {
      encodeDeckToJson(entries).toString()
    }
  }

  /**
   * Genera la versión JSON compacta sin codificar.
   */
  fun encodeDeckToJsonString(entries: List<DeckCardEntry>): String {
    return encodeDeckToJson(entries).toString()
  }

  /**
   * Formato predeterminado optimizado para el lector de barajas.
   */
  fun encodeDeckToString(entries: List<DeckCardEntry>): String {
    return encodeDeckToDeepLinkUrl(entries)
  }

  /**
   * Transforma el payload en un Bitmap QR nítido usando ZXing con Nivel de Corrección M, Quiet Zone y UTF-8.
   */
  fun generateQrBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap {
    val hints = HashMap<EncodeHintType, Any>()
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
    hints[EncodeHintType.MARGIN] = 2
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M

    val writer = MultiFormatWriter()
    val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
    val matrixWidth = bitMatrix.width
    val matrixHeight = bitMatrix.height
    val bmp = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)

    for (x in 0 until matrixWidth) {
      for (y in 0 until matrixHeight) {
        bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
      }
    }
    return bmp
  }
}
