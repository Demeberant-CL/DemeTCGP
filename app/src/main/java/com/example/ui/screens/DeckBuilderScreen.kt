package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.data.repository.CardWithInventory
import com.example.data.repository.DeckCardEntry
import com.example.data.repository.MetaDeckAiEngine
import com.example.data.repository.MetaDecksPreset
import com.example.data.util.TcgdexHelper
import com.example.ui.components.CardItemView
import com.example.ui.components.DeckQrDialog
import com.example.ui.theme.PocketBackground
import com.example.ui.theme.PocketBluePrimary
import com.example.ui.theme.PocketBorder
import com.example.ui.theme.PocketGold
import com.example.ui.theme.PocketRed
import com.example.ui.theme.PocketSurface
import com.example.ui.theme.PocketTextPrimary
import com.example.ui.theme.PocketTextSecondary
import com.example.ui.viewmodel.TcgViewModel

@Composable
fun DeckBuilderScreen(
  viewModel: TcgViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val inventory by viewModel.inventoryList.collectAsStateWithLifecycle()
  val deckState by viewModel.deckBuilderState.collectAsStateWithLifecycle()

  var showRenameDialog by remember { mutableStateOf(value = false) }
  var showSaveDialog by remember { mutableStateOf(value = false) }
  var showQrDialog by remember { mutableStateOf(value = false) }
  var showAiDropdown by remember { mutableStateOf(value = false) }
  var tempDeckNameInput by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(PocketBackground)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1. CABECERA (DeckHeader)
    DeckHeader(
      deckName = deckState.deckName,
      totalCards = deckState.totalCardCount,
      maxSize = deckState.maxDeckSize,
      onRenameClick = {
        tempDeckNameInput = deckState.deckName
        showRenameDialog = true
      },
      onSaveClick = { showSaveDialog = true },
      onQrClick = {
        if (deckState.totalCardCount > 0) {
          showQrDialog = true
        } else {
          Toast.makeText(context, "Añade cartas al mazo antes de generar el QR.", Toast.LENGTH_SHORT).show()
        }
      },
    ) {
      viewModel.clearCurrentDeck()
    }

    // Botón IA Meta Deck Generator con DropdownMenu
    Box(modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = { showAiDropdown = true },
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PocketBluePrimary),
      ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sugerir Mazo Meta con IA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      DropdownMenu(
        expanded = showAiDropdown,
        onDismissRequest = { showAiDropdown = false },
      ) {
        MetaDecksPreset.PRESETS.forEach { preset ->
          DropdownMenuItem(
            text = { Text("${preset.name} (${preset.archetype})") },
            onClick = {
              showAiDropdown = false
              val replacements = viewModel.applyAiPreset(preset)
              val replacementText = if (replacements > 0) " ($replacements sustitutos asignados)" else ""
              Toast.makeText(context, "¡Mazo '${preset.name}' cargado por IA!$replacementText", Toast.LENGTH_LONG).show()
            },
          )
        }
      }
    }

    // COMPOSICIÓN ACTUAL DEL MAZO (Baraja interactiva superior)
    DeckCurrentComposition(
      deckCards = deckState.deckCards,
      onIncrement = { entry ->
        val cardWithInv = inventory.find { it.card.id.equals(entry.card.id, ignoreCase = true) }
        if (cardWithInv != null) {
          val (success, errorMsg) = viewModel.addCardToCurrentDeck(cardWithInv)
          if ((!success) && (errorMsg != null)) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
          }
        }
      },
      onDecrement = { entry ->
        viewModel.removeCardFromCurrentDeck(entry)
      },
    )

    // 2. CUADRÍCULA DE SELECCIÓN (DeckGrid)
    Text(
      text = "Selecciona cartas de tu colección:",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = PocketTextPrimary,
    )

    DeckGrid(
      inventory = inventory,
      currentDeckCards = deckState.deckCards,
      onCardClick = { cardWithInv ->
        val (success, errorMsg) = viewModel.addCardToCurrentDeck(cardWithInv)
        if ((!success) && (errorMsg != null)) {
          Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    )
  }

  // Diálogo para cambiar nombre del mazo
  if (showRenameDialog) {
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text("Renombrar Mazo", fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = tempDeckNameInput,
          onValueChange = { tempDeckNameInput = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (tempDeckNameInput.isNotBlank()) {
              viewModel.setCustomDeckName(tempDeckNameInput.trim())
              showRenameDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = PocketBluePrimary),
        ) {
          Text("Aceptar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text("Cancelar")
        }
      },
    )
  }

  // Diálogo para guardar mazo
  if (showSaveDialog) {
    AlertDialog(
      onDismissRequest = { showSaveDialog = false },
      title = { Text("Guardar Mazo", fontWeight = FontWeight.Bold) },
      text = {
        Text("¿Deseas guardar '${deckState.deckName}' (${deckState.totalCardCount}/${deckState.maxDeckSize} cartas) en tu base de datos local?", fontSize = 12.sp, color = PocketTextSecondary)
      },
      confirmButton = {
        Button(
          onClick = {
            if (deckState.totalCardCount > 0) {
              viewModel.saveCustomDeck(deckState.deckName, deckState.deckCards)
              Toast.makeText(context, "¡Mazo guardado con éxito!", Toast.LENGTH_SHORT).show()
              showSaveDialog = false
            } else {
              Toast.makeText(context, "El mazo está vacío.", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
        ) {
          Text("Guardar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveDialog = false }) {
          Text("Cancelar")
        }
      },
    )
  }

  // Diálogo del Código QR
  if (showQrDialog) {
    DeckQrDialog(
      deckName = deckState.deckName,
      deckCards = deckState.deckCards,
      onDismiss = { showQrDialog = false },
    )
  }
}

// -------------------------------------------------------------
// 1. CABECERA (DeckHeader)
// -------------------------------------------------------------
@Composable
fun DeckHeader(
  deckName: String,
  totalCards: Int,
  maxSize: Int,
  onRenameClick: () -> Unit,
  onSaveClick: () -> Unit,
  onQrClick: () -> Unit,
  onClearClick: () -> Unit,
) {
  val progress = if (maxSize > 0) totalCards.toFloat() / maxSize.toFloat() else 0f

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(3.dp, RoundedCornerShape(16.dp), clip = false)
      .testTag("deck_header_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PocketSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = deckName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = PocketTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(onClick = onRenameClick, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar nombre", tint = PocketBluePrimary, modifier = Modifier.size(16.dp))
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = onQrClick,
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PocketBluePrimary),
          ) {
            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onSaveClick,
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Guardar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onClearClick,
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
          ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = PocketRed, modifier = Modifier.size(14.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Progreso del Mazo",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = PocketTextSecondary,
        )
        Text(
          text = "$totalCards / $maxSize cartas",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = if (totalCards == maxSize) Color(0xFF10B981) else PocketBluePrimary,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = if (totalCards == maxSize) Color(0xFF10B981) else PocketBluePrimary,
        trackColor = Color(0xFFE2E8F0),
      )
    }
  }
}

// COMPOSICIÓN ACTUAL (Fila horizontal de cartas añadidas)
@Composable
fun DeckCurrentComposition(
  deckCards: List<DeckCardEntry>,
  onIncrement: (DeckCardEntry) -> Unit,
  onDecrement: (DeckCardEntry) -> Unit,
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(2.dp, RoundedCornerShape(14.dp), clip = false),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = PocketSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PocketBorder)),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = "Cartas en el Mazo (${deckCards.sumOf { it.count }})",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PocketTextPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))

      if (deckCards.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Tu mazo está vacío. Toca las cartas de abajo para añadirlas.",
            fontSize = 11.sp,
            color = PocketTextSecondary,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
          items(deckCards, key = { it.card.id }) { entry ->
            val candidateUrls = remember(entry.card.id) { TcgdexHelper.getCardImageUrlsList(entry.card.id) }
            var currentUrlIndex by remember(entry.card.id) { mutableIntStateOf(0) }
            val currentUrl = candidateUrls.getOrNull(currentUrlIndex) ?: ""

            Box(
              modifier = Modifier
                .width(64.dp)
                .aspectRatio(0.714f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, PocketBorder, RoundedCornerShape(8.dp))
                .background(PocketBackground),
            ) {
              if (currentUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                  model = ImageRequest.Builder(context)
                    .data(currentUrl)
                    .memoryCacheKey(entry.card.id)
                    .diskCacheKey(entry.card.id)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(enable = true)
                    .build(),
                  contentDescription = entry.card.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                  error = {
                    if (currentUrlIndex < (candidateUrls.size - 1)) {
                      LaunchedEffect(currentUrlIndex) {
                        currentUrlIndex++
                      }
                    } else {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(PocketSurface)
                          .padding(2.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = entry.card.name,
                          fontSize = 8.sp,
                          fontWeight = FontWeight.Bold,
                          textAlign = TextAlign.Center,
                          color = PocketTextPrimary,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  },
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(PocketSurface)
                    .padding(2.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = entry.card.name,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = PocketTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              // Badge con cantidad
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(2.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(PocketGold)
                  .padding(horizontal = 4.dp, vertical = 1.dp),
              ) {
                Text(
                  text = "x${entry.count}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF78350F),
                )
              }

              // Botones flotantes + / -
              Row(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.6f)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = { onDecrement(entry) },
                  modifier = Modifier.size(20.dp),
                ) {
                  Icon(Icons.Filled.Remove, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(12.dp))
                }
                IconButton(
                  onClick = { onIncrement(entry) },
                  modifier = Modifier.size(20.dp),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(12.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. CUADRÍCULA DE SELECCIÓN (DeckGrid)
// -------------------------------------------------------------
@Composable
fun DeckGrid(
  inventory: List<CardWithInventory>,
  currentDeckCards: List<DeckCardEntry>,
  onCardClick: (CardWithInventory) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (inventory.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No hay cartas disponibles en el inventario.",
        fontSize = 12.sp,
        color = PocketTextSecondary,
      )
    }
  } else {
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      contentPadding = PaddingValues(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = modifier,
    ) {
      items(inventory, key = { it.card.id }) { item ->
        val inDeckCount = currentDeckCards.find { it.card.id == item.card.id }?.count ?: 0

        Box {
          CardItemView(
            card = item.card,
            ownedCount = item.ownedCount,
            isWishlist = item.isWishlist,
            onToggleWishlist = { },
            modifier = Modifier.clickable { onCardClick(item) },
          )

          // Badge indicando cuántas copias están en el mazo actual
          if (inDeckCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PocketBluePrimary)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
              Text(
                text = "Mazo: x$inDeckCount",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
          }
        }
      }
    }
  }
}
