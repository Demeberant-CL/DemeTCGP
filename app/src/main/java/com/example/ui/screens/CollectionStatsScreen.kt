package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.PocketBackground
import com.example.ui.theme.PocketBluePrimary
import com.example.ui.theme.PocketBorder
import com.example.ui.theme.PocketGold
import com.example.ui.theme.PocketSurface
import com.example.ui.theme.PocketTextPrimary
import com.example.ui.theme.PocketTextSecondary
import com.example.ui.viewmodel.TcgViewModel

@Composable
fun CollectionStatsScreen(
  viewModel: TcgViewModel,
  modifier: Modifier = Modifier,
) {
  val inventory by viewModel.inventoryList.collectAsStateWithLifecycle()

  // Cálculos eficientes cacheados con remember(inventory)
  val stats = remember(inventory) {
    val totalCatalog = inventory.size
    val uniqueOwned = inventory.count { it.ownedCount > 0 }
    val totalCopies = inventory.sumOf { it.ownedCount }
    val duplicates = maxOf(0, totalCopies - uniqueOwned)
    val completionRatio = if (totalCatalog > 0) uniqueOwned.toFloat() / totalCatalog.toFloat() else 0f

    // Desglose por tipo
    val typesMap = inventory.groupBy { it.card.type.lowercase() }
    val typeStats = typesMap.mapValues { (_, cards) ->
      val total = cards.size
      val owned = cards.count { it.ownedCount > 0 }
      Pair(owned, total)
    }

    // Desglose por rareza
    val rarityMap = inventory.groupBy { it.card.rarity.displayName }
    val rarityStats = rarityMap.mapValues { (_, cards) ->
      val total = cards.size
      val owned = cards.count { it.ownedCount > 0 }
      Pair(owned, total)
    }

    CollectionStatsData(
      totalCatalog = totalCatalog,
      uniqueOwned = uniqueOwned,
      totalCopies = totalCopies,
      duplicates = duplicates,
      completionRatio = completionRatio,
      typeStats = typeStats,
      rarityStats = rarityStats,
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(PocketBackground)
      .verticalScroll(rememberScrollState())
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. INDICADOR DE PROGRESO GENERAL (Circular Progress)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
        .testTag("stats_general_card"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = PocketSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.Analytics, contentDescription = null, tint = PocketBluePrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Estadísticas Generales del Álbum",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = PocketTextPrimary,
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Gráfico Circular Grande
        Box(
          modifier = Modifier.size(130.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE2E8F0),
            strokeWidth = 12.dp,
          )
          CircularProgressIndicator(
            progress = { stats.completionRatio },
            modifier = Modifier.fillMaxSize(),
            color = PocketBluePrimary,
            strokeWidth = 12.dp,
            trackColor = Color.Transparent,
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${(stats.completionRatio * 100).toInt()}%",
              fontSize = 24.sp,
              fontWeight = FontWeight.Black,
              color = PocketTextPrimary,
            )
            Text(
              text = "Completado",
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              color = PocketTextSecondary,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          StatItem(title = "Únicas", value = "${stats.uniqueOwned}/${stats.totalCatalog}")
          StatItem(title = "Copias Totales", value = stats.totalCopies.toString())
          StatItem(title = "Repetidas", value = stats.duplicates.toString())
        }
      }
    }

    // 2. DESGLOSE POR TIPOS
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(16.dp), clip = false),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = PocketSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.CatchingPokemon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Progreso por Tipos de Pokémon",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PocketTextPrimary,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        stats.typeStats.forEach { (type, pair) ->
          val (owned, total) = pair
          val ratio = if (total > 0) owned.toFloat() / total.toFloat() else 0f
          val typeCapitalized = type.replaceFirstChar { it.uppercase() }

          Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(text = typeCapitalized, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PocketTextPrimary)
              Text(text = "$owned / $total (${(ratio * 100).toInt()}%)", fontSize = 11.sp, color = PocketTextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { ratio },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = PocketBluePrimary,
              trackColor = Color(0xFFE2E8F0),
            )
          }
        }
      }
    }

    // 3. DESGLOSE POR RAREZAS
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(16.dp), clip = false),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = PocketSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Star, contentDescription = null, tint = PocketGold, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Progreso por Rareza",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PocketTextPrimary,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        stats.rarityStats.forEach { (rarity, pair) ->
          val (owned, total) = pair
          val ratio = if (total > 0) owned.toFloat() / total.toFloat() else 0f

          Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(text = rarity, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PocketTextPrimary)
              Text(text = "$owned / $total (${(ratio * 100).toInt()}%)", fontSize = 11.sp, color = PocketTextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { ratio },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = PocketGold,
              trackColor = Color(0xFFE2E8F0),
            )
          }
        }
      }
    }

    // 4. DATOS DE VALOR & REPETIDAS
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(16.dp), clip = false),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = PocketSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Layers, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Resumen de Duplicados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PocketTextPrimary,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Tienes un total de ${stats.duplicates} cartas repetidas acumuladas en tu inventario que pueden ser útiles para intercambios o bonificaciones.",
          fontSize = 12.sp,
          color = PocketTextSecondary,
          lineHeight = 18.sp,
        )
      }
    }
  }
}

@Composable
private fun StatItem(title: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PocketTextPrimary)
    Text(text = title, fontSize = 10.sp, color = PocketTextSecondary)
  }
}

data class CollectionStatsData(
  val totalCatalog: Int,
  val uniqueOwned: Int,
  val totalCopies: Int,
  val duplicates: Int,
  val completionRatio: Float,
  val typeStats: Map<String, Pair<Int, Int>>,
  val rarityStats: Map<String, Pair<Int, Int>>,
)
