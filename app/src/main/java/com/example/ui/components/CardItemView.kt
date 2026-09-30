package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.PokemonCard
import com.example.data.util.NetworkUtils
import com.example.data.util.TcgdexHelper
import com.example.ui.theme.PocketBluePrimary
import com.example.ui.theme.PocketBorder
import com.example.ui.theme.PocketGold
import com.example.ui.theme.PocketRed
import com.example.ui.theme.PocketSurface
import com.example.ui.theme.PocketTextMuted
import com.example.ui.theme.PocketTextPrimary
import com.example.ui.theme.PocketTextSecondary

@Composable
fun CardItemView(
  card: PokemonCard,
  ownedCount: Int,
  isWishlist: Boolean,
  onToggleWishlist: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isOwned = ownedCount > 0

  // Consumir el StateFlow reactivo de red
  val isOnline by NetworkUtils.isOnline.collectAsStateWithLifecycle()

  // Gestión de URLs Candidatas y Estado Dinámico asociado al ID de la carta
  val candidateUrls = remember(card.id) { TcgdexHelper.getCardImageUrlsList(card.id) }
  var currentUrlIndex by remember(card.id) { mutableIntStateOf(0) }

  val currentUrl = candidateUrls.getOrNull(currentUrlIndex) ?: ""
  Log.d("CoilDebug", "Procesando carta ${card.id} (${card.name}) [índice ${currentUrlIndex + 1}/${candidateUrls.size}]: URL = '$currentUrl' | En línea: $isOnline")

  // Optimización de ImageRequest con memoria en caché estable y política de disco activa
  val imageRequest = remember(card.id, currentUrl) {
    ImageRequest.Builder(context)
      .data(currentUrl)
      .memoryCacheKey(card.id)
      .diskCacheKey(card.id)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .diskCachePolicy(CachePolicy.ENABLED)
      .crossfade(enable = true)
      .build()
  }

  val grayscaleMatrix = remember {
    ColorMatrix().apply { setToSaturation(0f) }
  }

  val typeColor = remember(card.type) {
    when (card.type.lowercase()) {
      "planta" -> Color(0xFF10B981)
      "fuego" -> Color(0xFFEF4444)
      "agua" -> Color(0xFF0284C7)
      "rayo" -> Color(0xFFF59E0B)
      "psíquico" -> Color(0xFF8B5CF6)
      "lucha" -> Color(0xFFD97706)
      "oscuridad" -> Color(0xFF475569)
      "metal" -> Color(0xFF64748B)
      "dragón" -> Color(0xFFF97316)
      else -> Color(0xFF94A3B8)
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(0.714f)
      .shadow(if (isOwned) 3.dp else 1.dp, shape = RoundedCornerShape(12.dp), clip = false)
      .testTag("card_item_${card.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isOwned) PocketSurface else Color(0xFFE2E8F0),
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = SolidColor(
        if (isOwned) PocketBorder else Color(0xFFCBD5E1),
      ),
    ),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (isOnline && currentUrl.isNotBlank()) {
        SubcomposeAsyncImage(
          model = imageRequest,
          contentDescription = card.name,
          contentScale = ContentScale.Crop,
          colorFilter = if (!isOwned) ColorFilter.colorMatrix(grayscaleMatrix) else null,
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.714f)
            .clip(RoundedCornerShape(12.dp))
            .alpha(if (isOwned) 1.0f else 0.5f),
          loading = {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(if (isOwned) Color(0xFFF1F5F9) else Color(0xFFCBD5E1)),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PocketBluePrimary,
                strokeWidth = 2.dp,
              )
            }
          },
          error = {
            if (currentUrlIndex < (candidateUrls.size - 1)) {
              LaunchedEffect(currentUrlIndex) {
                Log.w("CoilDebug", "Error HTTP con '$currentUrl'. Cambiando al siguiente candidato de forma asíncrona...")
                currentUrlIndex++
              }
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(if (isOwned) Color(0xFFF1F5F9) else Color(0xFFCBD5E1)),
              )
            } else {
              CardFallbackDesign(card = card, isOwned = isOwned, typeColor = typeColor)
            }
          },
        )
      } else {
        CardFallbackDesign(card = card, isOwned = isOwned, typeColor = typeColor)
      }

      // Floating Badge Top-Right: Quantity (x1, x2, x3...) or (x0)
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(if (isOwned) Color(0xFFFEF3C7) else Color(0xFF0F172A).copy(alpha = 0.8f))
          .border(
            1.dp,
            if (isOwned) PocketGold else Color.White.copy(alpha = 0.3f),
            RoundedCornerShape(6.dp),
          )
          .padding(horizontal = 5.dp, vertical = 1.dp),
      ) {
        Text(
          text = "x$ownedCount",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = if (isOwned) Color(0xFFB45309) else Color.White,
        )
      }

      // Wishlist Heart Icon Top-Left
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(4.dp)
          .size(22.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.85f))
          .clickable { onToggleWishlist() }
          .testTag("wishlist_btn_${card.id}"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
          contentDescription = if (isWishlist) "Quitar de deseadas" else "Añadir a deseadas",
          tint = if (isWishlist) PocketRed else PocketTextMuted,
          modifier = Modifier.size(13.dp),
        )
      }

      // Center Overlay Lock for Unowned Cards (Quantity = 0)
      if (!isOwned) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.35f)),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                .border(1.dp, Color.White, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Bloqueada",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
              Text(
                text = "x0 • Bloqueada",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CardFallbackDesign(
  card: PokemonCard,
  isOwned: Boolean,
  typeColor: Color,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .alpha(if (isOwned) 1.0f else 0.5f)
      .background(if (isOwned) Color(0xFFF8FAFC) else Color(0xFFE2E8F0))
      .padding(6.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = card.id,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = PocketTextSecondary,
      )
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(if (isOwned) typeColor else Color(0xFF94A3B8)),
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = card.name,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = if (isOwned) PocketTextPrimary else Color(0xFF64748B),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (card.hp > 0) {
        Text(
          text = "${card.hp} PS",
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          color = PocketTextSecondary,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = card.rarity.symbol,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = PocketGold,
      )
      Text(
        text = card.pack.displayName.replace("Sobre ", ""),
        fontSize = 7.sp,
        color = PocketTextMuted,
      )
    }
  }
}
