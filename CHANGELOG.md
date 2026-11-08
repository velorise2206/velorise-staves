# Changelog

All notable changes to **Velorise's Staves** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.1] - 2026-11-08

### Fixed
- **1.21.1 Enchantment Compatibility**: Added `minecraft:enchantable/durability` and `minecraft:enchantable/vanishing` item tags in `shared-resources` so `Unbreaking`, `Mending`, and `Curse of Vanishing` work properly in Survival on NeoForge 1.21.1 and Fabric 1.21.1.
- **Enchantability & Anvil Material Repair**: Overrode `getEnchantmentValue()` (15 for Basic, 20 for Netherite) and `isValidRepairItem()` across all staves and artifacts.
- **Webcaster Owner Immunity & Server Shutdown Cleanup**: Fixed `TemporaryWebManager` poisoning the caster and added `clearAll()` hook on server stopping across Forge, NeoForge, and Fabric so temporary cobwebs never persist permanently after server restarts.
- **Burst-Fire Stalls & Durability Drain**: Fixed burst staves (`Inferno Rod`, `Wither Scepter`, `Frostbolt Wand`, `Webcaster`) consuming 3 durability per burst and stalling with a full cooldown on expired partial bursts.
- **Shulker Seeker Homing**: Fixed `prepareProjectile()` overwriting `ShulkerBullet` homing movement when a target is locked.
- **Mid-Air Projectile Max Range**: Enforced `maxRange` despawning in mid-air via tick checks across Forge, NeoForge, and Fabric (`ProjectileMixin`).
- **Guardian Lens Damage Tick Count**: Fixed `onUseTick` skipping the initial tick (`elapsed == 0`), restoring full 10-tick channel damage (25.0 Basic / 35.0 Netherite).
- **Offhand Slot & Foil Parity**: Fixed hardcoded `EquipmentSlot.MAINHAND` in channeled items and restored `isFoil()` enchantment glint on `MagicMirrorItem` and `VoidTranslocatorItem` in NeoForge and Fabric 1.21.1.
- **Resource & Script Cleanup**: Stripped UTF-8 BOM from 41 JSON files and fixed leftover project names in `.bat` and `gradlew` scripts.

---

## [1.0.0] - 2026-11-08

### Added
- **Initial Release** of Velorise's Staves (Vanilla-friendly Magic Staves & Utility Artifacts).
- **11 Combat Staves**:
  - `Dragonfire Scepter`: Spawns lingering dragon breath flames.
  - `Resonant Echo`: Channels Warden sonic booms piercing all targets in range.
  - `Inferno Rod`: Fires rapid blaze fireballs in burst sequence.
  - `Ghastfire Staff`: Launches explosive ghast fireballs.
  - `Wither Scepter`: Multi-projectile wither skull spread.
  - `Shulker Seeker`: Homing shulker bullets causing levitation.
  - `Guardian Lens`: Continuous channeled guardian laser beam.
  - `Evoker Fang Staff`: Summons rows of erupting evoker fangs.
  - `Stormcaller`: Calls down lightning bolts at the crosshair.
  - `Frostbolt Wand`: Freezing ice projectile with severe slowness & freeze ticks.
  - `Webcaster`: Fires clusters of trapping venomous cobwebs.
- **2 Mobility & Utility Artifacts**:
  - `Void Translocator`: Safe forward blink teleportation.
  - `Magic Mirror`: Channeled recall home to bed or world spawn.
- **Multi-Loader Support**:
  - Added **Fabric 1.21.1** port (`fabric-1.21.1`, Java 21) with full parity across all 26 items, custom Mixin hit interception, and ModMenu/JEI support.
  - Added **Forge 1.20.1** port (`forge-1.20.1`, Java 17).
  - Added **NeoForge 1.21.1** loader (`neoforge-1.21.1`, Java 21).
  - Automated build & export script (`build-all.ps1`) for Forge, NeoForge, and Fabric.
  - Built-in JEI dev integration for rapid recipe inspection.
- Balanced around **Vanilla Cooldowns & Durability** (fully compatible with `Unbreaking` and `Mending`).
  - Combat staves cooldowns rebalanced to 4.0s - 8.0s for authentic tactical magic pacing.
  - Mobility artifacts rebalanced: Void Translocator (5.0s cooldown), Magic Mirror (2.5s channel, 15.0s cooldown).
- **Smithing Table Netherite Upgrade System**:
  - All 13 staves and artifacts can now be upgraded at a Smithing Table using `Netherite Upgrade Template` + `Netherite Ingot`.
  - Upgraded weapons receive the `Netherite + [Name]` title, custom Netherite textures/models, and fire-resistant trait (`fireResistant()`).
  - Netherite tier features massively increased durability (500), amplified damage, enlarged AOE radiuses, and longer reach (Void Translocator blinks 36 blocks).
  - Basic Magic Mirror is locked to intra-dimension recalls; Netherite Magic Mirror enables inter-dimensional recalls (e.g. Nether/End to Overworld).
  - Basic staves durability rebalanced to 120-150 (Void: 80, Mirror: 40) to encourage upgrading.
- Complete localization for English (`en_us`) and Vietnamese (`vi_vn`).
