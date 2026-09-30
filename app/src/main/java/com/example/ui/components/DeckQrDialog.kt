package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DeckCardEntry
import com.example.data.util.PtcgpEncoder
import com.example.data.util.QrSaver
import com.example.ui.theme.PocketBluePrimary
import com.example.ui.theme.PocketBorder
import com.example.ui.theme.PocketSurface
import com.example.ui.theme.PocketTextPrimary
import com.example.ui.theme.PocketTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DeckQrDialog(
  deckName: String,
  deckCards: List<DeckCardEntry>,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isGenerating by remember { mutableStateOf(value = true) }

  LaunchedEffect(deckCards) {
    isGenerating = true
    val bitmap = withContext(Dispatchers.Default) {
      try {
        val payload = PtcgpEncoder.encodeDeckToString(deckCards)
        PtcgpEncoder.generateQrBitmap(payload, 512, 512)
      } catch (_: Exception) {
        null
      }
    }
    qrBitmap = bitmap
    isGenerating = false
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = PocketBluePrimary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Código QR de Mazo",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = PocketTextPrimary,
        )
        Text(
          text = deckName,
          fontSize = 11.sp,
          color = PocketTextSecondary,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(4.dp))

        Box(
          modifier = Modifier
            .size(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, PocketBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
          contentAlignment = Alignment.Center,
        ) {
          if ((isGenerating) || (qrBitmap == null)) {
            CircularProgressIndicator(color = PocketBluePrimary)
          } else {
            qrBitmap?.let { bmp ->
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Código QR del Mazo",
                modifier = Modifier.size(236.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Escanea este código desde la app oficial de Pokémon TCG Pocket para importar la baraja.",
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          color = PocketTextSecondary,
          lineHeight = 15.sp,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = {
              qrBitmap?.let { bmp ->
                val success = QrSaver.saveQrToGallery(context, bmp, deckName)
                if (success) {
                  Toast.makeText(context, "¡QR Guardado en Galería!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Error al guardar el código QR.", Toast.LENGTH_SHORT).show()
                }
              }
            },
            modifier = Modifier
              .weight(1f)
              .height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            enabled = qrBitmap != null,
          ) {
            Text("Guardar QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              val payload = PtcgpEncoder.encodeDeckToString(deckCards)
              val clip = ClipData.newPlainText("Mazo PTCGP", payload)
              clipboard?.setPrimaryClip(clip)
              Toast.makeText(context, "¡Código de mazo copiado!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
              .weight(1f)
              .height(40.dp),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Copiar Texto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cerrar")
      }
    },
    containerColor = PocketSurface,
  )
}
