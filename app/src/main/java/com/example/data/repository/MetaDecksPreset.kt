package com.example.data.repository

data class MetaDeckPreset(
  val name: String,
  val archetype: String,
  val requiredCardIds: List<String>,
)

object MetaDecksPreset {
  val PRESETS = listOf(
    MetaDeckPreset(
      name = "Pikachu ex Turbo",
      archetype = "Rayo Beatdown",
      requiredCardIds = listOf(
        "A1-096", "A1-096", // 2x Pikachu ex
        "A1-101", "A1-101", // 2x Zapdos ex
        "A1-095", "A1-095", // 2x Pikachu
        "A1-097", "A1-097", // 2x Raichu
        "A1-225", "A1-225", // 2x Sabrina
        "A1-186", "A1-186", // 2x Pidgey
        "A1-189", "A1-189", // 2x Rattata
        "A1-190", "A1-190", // 2x Raticate
        "A1-196", "A1-196", // 2x Meowth
        "A1-206", "A1-206", // 2x Eevee
      ),
    ),
    MetaDeckPreset(
      name = "Mewtwo ex & Gardevoir",
      archetype = "Psíquico Aceleración",
      requiredCardIds = listOf(
        "A1-129", "A1-129", // 2x Mewtwo ex
        "A1-130", "A1-130", // 2x Gardevoir
        "A1-123", "A1-123", // 2x Gengar ex
        "A1-121", "A1-121", // 2x Haunter
        "A1-225", "A1-225", // 2x Sabrina
        "A1-115", "A1-115", // 2x Abra
        "A1-116", "A1-116", // 2x Kadabra
        "A1-117", "A1-117", // 2x Alakazam
        "A1-118", "A1-118", // 2x Slowpoke
        "A1-119", "A1-119", // 2x Slowbro
      ),
    ),
  )
}
