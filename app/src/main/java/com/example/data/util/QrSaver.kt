package com.example.data.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.IOException
import java.io.OutputStream

object QrSaver {

  fun saveQrToGallery(context: Context, bitmap: Bitmap, deckName: String): Boolean {
    val filename = "PTCGP_${deckName.replace(Regex("[^A-Za-z0-9]"), "_")}_${System.currentTimeMillis()}.png"
    var fos: OutputStream? = null
    var imageUri: Uri? = null

    try {
      val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TCGPocketDecks")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      }

      val contentResolver = context.contentResolver
      val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      }

      imageUri = contentResolver.insert(collection, contentValues)
      if (imageUri == null) {
        throw IOException("Fallo al crear entrada en MediaStore para $filename")
      }

      fos = contentResolver.openOutputStream(imageUri)
      if (fos == null) {
        throw IOException("Fallo al abrir OutputStream para $imageUri")
      }

      // Guardar obligatoriamente en PNG para preservar nitidez de píxeles B/W
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      fos.flush()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(imageUri, contentValues, null, null)
      }

      return true
    } catch (e: Exception) {
      Log.e("QrSaver", "Error al guardar código QR en galería", e)
      imageUri?.let { uri ->
        try {
          context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {}
      }
      return false
    } finally {
      try {
        fos?.close()
      } catch (_: Exception) {}
    }
  }
}
