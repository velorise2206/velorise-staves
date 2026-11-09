# Changelog

All notable changes to **Velorise's Staves** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-11-08

### Added
- **Elemental Gauge Theory (EGT) Combat System**: Implemented a comprehensive Genshin Impact-style combat mechanics engine across Forge 1.20.1, NeoForge 1.21.1, and Fabric 1.21.1 (`com.velorise.staves.element`).
  - **Aura & Trigger Separation**: 5 persistent aura elements (🔥 Pyro, 💧 Hydro, ❄️ Cryo, ⚡ Electro, 🌿 Dendro) and 2 trigger-only elements (🌪️ Anemo, 🪨 Geo) that never linger as auras on entities.
  - **Gauge Units (U) & Aura Tax**: Đòn đánh gắn ấn chịu 20% Thuế Ấn (Aura Tax $0.8\times$), suy giảm theo thời gian ($Duration = 2.5U + 7.0s$).
  - **Reaction Matrix & Modifiers**:
    - *Forward Vaporize* (💧 on 🔥, $2.0\times$ U consumption, $\times 2.0$ DMG).
    - *Reverse Vaporize* (🔥 on 💧, $0.5\times$ U consumption, $\times 1.5$ DMG).
    - *Forward Melt* (🔥 on ❄️, $2.0\times$ U consumption, $\times 2.0$ DMG).
    - *Reverse Melt* (❄️ on 🔥, $0.5\times$ U consumption, $\times 1.5$ DMG).
    - *Overloaded* (🔥 + ⚡, $1.0\times$ U, $3.5\text{m}$ AoE explosion + knockback).
    - *Superconduct* (❄️ + ⚡, $1.0\times$ U, $3.5\text{m}$ AoE Cryo blast deal $1.0$ DMG ($0.5$ heart) + reduces enemy Physical RES by $40\%$ for $12\text{s}$, increasing physical damage from swords/arrows by $+40\%$).
    - *Frozen & Shatter* (💧 + ❄️ immobilizes for $3.5\text{s}$; 🪨 Geo triggers Shatter for $+10.0$ physical burst damage).
    - *Electro-Charged* (💧 + ⚡ coexisting auras, continuous lightning chain damage at $0.4\text{U/s}$).
    - *Burning* (🔥 + 🌿 continuous fire damage consuming $0.4\text{U/s}$ Dendro).
    - *Quicken, Aggravate & Spread* (⚡ + 🌿 creates Quicken; ⚡ triggers Aggravate $+1.0$ DMG ($0.5$ heart) and 🌿 triggers Spread $+1.25$ DMG without consuming Quicken).
    - *Bloom* (💧 + 🌿 creates Dendro core $3.0\text{m}$ AoE spore explosion).
    - *Overvape* (🔥 on Hydro + Electro triggers simultaneous Vaporize and Overload).
    - *Swirl* (🌪️ consumes $0.5\times$ aura, triggers $5\text{m}$ elemental vortex spreading the element to all nearby mobs).
    - *Crystallize* (🪨 consumes $0.5\times$ aura, grants absorption hearts shield).
  - **Actionbar HUD & Visuals**: Real-time actionbar notifications displaying reaction name, damage multiplier, and remaining U, alongside ambient hovering element particles and distinct reaction sound effects.
  - **TOML Config Toggles**: Added `[elemental_gauge]` section with `enable_elemental_gauge`, `show_actionbar_feedback`, and `show_elemental_particles`.

### Changed
- **50% Base Damage Rebalance**: Reduced base damage across all basic staves by 50% to incentivize tactical elemental reaction combos and prevent un-comboed one-shots.
- **Tooltips Update**: Updated damage hover tooltips on all 11 staves to clearly display the calibrated damage and exact Gauge Units (e.g. `3.0 x 3 (Hỏa 1U/v)`).

---

## [1.0.1] - 2026-11-08

### Added
- **TOML Configuration System (`config/velorise_staves.toml`)**: Added full TOML configuration support across Forge 1.20.1, NeoForge 1.21.1, and Fabric 1.21.1. Allows customizing global damage/cooldown multipliers, individual staff cooldowns (in seconds), maximum durability (both Basic and Netherite tiers), Creeper lightning charging toggle, cross-dimension Magic Mirror toggle, web trap duration, and Void Translocator teleport distances.
- **Item Tooltips (Damage & Range)**: Added clear display of attack damage, effective range, and AOE radius to hover tooltips across all 11 combat staves (both Basic and Netherite tiers) and `Void Translocator` in both English (`en_us`) and Vietnamese (`vi_vn`).
- **Build Distribution Directory**: Configured `build-all.ps1` to automatically deploy all generated `.jar` artifacts into `myMod/mods/velorise_staves/` matching workspace organizational standards.

### Fixed
- **Stormcaller Creeper Transformation**: Fixed `Stormcaller` failing to charge Creepers (and trigger mob lightning transformations like Villagers to Witches and Pigs to Zombified Piglins) by invoking `target.thunderHit(level, bolt)` on all entities caught in the lightning strike AOE.
- **1.21.1 Enchantment Compatibility**: Added `minecraft:enchantable/durability` and `minecraft:enchantable/vanishing` item tags in `shared-resources` so `Unbreaking`, `Mending`, and `Curse of Vanishing` work properly in Survival on NeoForge 1.21.1 and Fabric 1.21.1.
- **Enchantability & Anvil Material Repair**: Overrode `getEnchantmentValue()` (15 for Basic, 20 for Netherite) and `isValidRepairItem()` across all staves and artifacts.
- **Webcaster Owner Immunity & Server Shutdown Cleanup**: Fixed `TemporaryWebManager` poisoning the caster and added `clearAll()` hook on server stopping across Forge, NeoForge, and Fabric so temporary cobwebs never persist permanently after server restarts.
- **Burst-Fire Stalls & Durability Drain**: Fixed burst staves (`Inferno Rod`, `Wither Scepter`, `Frostbolt Wand`, `Webcaster`) consuming 3 durability per burst and stalling with a full cooldown on expired partial bursts.
- **Shulker Seeker Homing**: Fixed `prepareProjectile()` overwriting `ShulkerBullet` homing movement when a target is locked.
- **Mid-Air Projectile Max Range**: Enforced `maxRange` despawning in mid-air via tick checks across Forge, NeoForge, and Fabric (`ProjectileMixin`).
- **Guardian Lens Damage Tick Count**: Fixed `onUseTick` skipping the initial tick (`elapsed == 0`), restoring full 10-tick channel damage (25.0 Basic / 35.0 Netherite).
- **Offhand Slot & Foil Parity**: Fixed hardcoded `EquipmentSlot.MAINHAND` in channeled items and restored `isFoil()` enchantment glint on `MagicMirrorItem` and `VoidTranslocatorItem` in NeoForge and Fabric 1.21.1.
- **Dev Environment Tools**: Added Jade HUD (`11.13.3+forge`, `15.10.6+neoforge`, `15.10.6+fabric`) runtime dependencies across all three loaders for in-game testing.
- **Resource & Script Cleanup**: Stripped UTF-8 BOM from 41 JSON files and fixed leftover project names in `.bat` and `gradlew` scripts.

---

## [1.1.0] - 2026-11-08

### Added
- **Elemental Gauge Theory (EGT) Combat System**: Implemented a comprehensive Genshin Impact-style combat mechanics engine across Forge 1.20.1, NeoForge 1.21.1, and Fabric 1.21.1 (`com.velorise.staves.element`).
  - **Aura & Trigger Separation**: 5 persistent aura elements (🔥 Pyro, 💧 Hydro, ❄️ Cryo, ⚡ Electro, 🌿 Dendro) and 2 trigger-only elements (🌪️ Anemo, 🪨 Geo) that never linger as auras on entities.
  - **Gauge Units (U) & Aura Tax**: Đòn đánh gắn ấn chịu 20% Thuế Ấn (Aura Tax $0.8\times$), suy giảm theo thời gian ($Duration = 2.5U + 7.0s$).
  - **Reaction Matrix & Modifiers**:
    - *Forward Vaporize* (💧 on 🔥, $2.0\times$ U consumption, $\times 2.0$ DMG).
    - *Reverse Vaporize* (🔥 on 💧, $0.5\times$ U consumption, $\times 1.5$ DMG).
    - *Forward Melt* (🔥 on ❄️, $2.0\times$ U consumption, $\times 2.0$ DMG).
    - *Reverse Melt* (❄️ on 🔥, $0.5\times$ U consumption, $\times 1.5$ DMG).
    - *Overloaded* (🔥 + ⚡, $1.0\times$ U, $3.5\text{m}$ AoE explosion + knockback).
    - *Superconduct* (❄️ + ⚡, $1.0\times$ U, $3.5\text{m}$ AoE Cryo blast deal $1.0$ DMG ($0.5$ heart) + reduces enemy Physical RES by $40\%$ for $12\text{s}$, increasing physical damage from swords/arrows by $+40\%$).
    - *Frozen & Shatter* (💧 + ❄️ immobilizes for $3.5\text{s}$; 🪨 Geo triggers Shatter for $+10.0$ physical burst damage).
    - *Electro-Charged* (💧 + ⚡ coexisting auras, continuous lightning chain damage at $0.4\text{U/s}$).
    - *Burning* (🔥 + 🌿 continuous fire damage consuming $0.4\text{U/s}$ Dendro).
    - *Quicken, Aggravate & Spread* (⚡ + 🌿 creates Quicken; ⚡ triggers Aggravate $+1.0$ DMG ($0.5$ heart) and 🌿 triggers Spread $+1.25$ DMG without consuming Quicken).
    - *Bloom* (💧 + 🌿 creates Dendro core $3.0\text{m}$ AoE spore explosion).
    - *Overvape* (🔥 on Hydro + Electro triggers simultaneous Vaporize and Overload).
    - *Swirl* (🌪️ consumes $0.5\times$ aura, triggers $5\text{m}$ elemental vortex spreading the element to all nearby mobs).
    - *Crystallize* (🪨 consumes $0.5\times$ aura, grants absorption hearts shield).
  - **Actionbar HUD & Visuals**: Real-time actionbar notifications displaying reaction name, damage multiplier, and remaining U, alongside ambient hovering element particles and distinct reaction sound effects.
  - **TOML Config Toggles**: Added `[elemental_gauge]` section with `enable_elemental_gauge`, `show_actionbar_feedback`, and `show_elemental_particles`.

### Changed
- **50% Base Damage Rebalance**: Reduced base damage across all basic staves by 50% to incentivize tactical elemental reaction combos and prevent un-comboed one-shots.
- **Tooltips Update**: Updated damage hover tooltips on all 11 staves to clearly display the calibrated damage and exact Gauge Units (e.g. `3.0 x 3 (Hỏa 1U/v)`).

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
