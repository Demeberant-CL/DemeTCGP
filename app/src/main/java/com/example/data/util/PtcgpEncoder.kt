package com.example.data.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.example.data.repository.DeckCardEntry
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object PtcgpEncoder {

  /**
   * Serializa la lista de cartas del mazo a un string compatible con el formato PTCGP (representación Base64 compacta).
   */
  fun encodeDeckToString(entries: List<DeckCardEntry>): String {
    val rawString = entries.joinToString(";") { "${it.card.id}:${it.count}" }
    return try {
      val encodedBytes = Base64.encode(rawString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
      "PTCGP:${String(encodedBytes, Charsets.UTF_8)}"
    } catch (_: Exception) {
      "PTCGP:$rawString"
    }
  }

  /**
   * Transforma un texto / payload en un Bitmap QR nítido con margen (Quiet Zone).
   */
  fun generateQrBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
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
