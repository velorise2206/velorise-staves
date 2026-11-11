# Changelog

All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- **Omni Staff & Netherite Omni Staff** (![Omni][omni]): Shapelessly crafted from any 1 staff of each of the 7 elements. Fires iridescent 3D prism orbs (`OmniOrbEntity`) that automatically trigger the strongest elemental reaction in **Omni Mode**, or hold `[Tab]` to open the 8-slot radial **Element Wheel** to lock onto any element.
- **Geo Fang Pillar (`velorise_staves:geo_fang_pillar`)**: Netherite Evoker Fang Staff raises a `1x2x1` Geo construct (`HP = 2x creator's max HP`, up to 3 active per owner) blocking attacks/projectiles and pulsing 4m concentric Geo waves every 2s.
- **Elemental Mastery I–III Enchantment**: Scales amplifying reactions (`+15%` / level), transformative reactions (`+30%` / level), and Crystallize shields.
- **World & Gear Interactions**: Copper Lightning Rod atop a Redstone Block forms a **Tesla Coil** (3–5 Electro arcs every 2s, 2x stats in rain or 30s on lightning strike); **Thorns** armor reflects `0.5U` of the target's element per level; Hydro beams extinguish environmental fire, soul fire, campfires, and candles.
- **Localization & Codex**: Expanded to **9 languages** (`en_us`, `vi_vn`, `zh_cn`, `ja_jp`, `ko_kr`, `de_de`, `es_es`, `fr_fr`, `ru_ru`) with interactive reaction links, live server config sync, and full custom bitmap icon glyphs (`\uE001`–`\uE00A`).

### Changed & Fixed

- Replaced Dendro Core model/textures with a custom Minecraft-style 16-faceted `64x64` self-lit 3D bud renderer (`0.75x` scale) and `16x16` item icon.
- Fixed Evoker Fang Staff (`velorise_staves:geo_fangs`) losing base damage on NeoForge 1.21.1 and Forge 1.20.1 due to vanilla hit i-frames.
- Fixed Burgeon (`17.5 HP`) and Hyperbloom (`16.0 HP`) heart values in `ElementalCodexScreen`, Forge 1.20.1 `CrystallizeShieldEffect` amplifier removal and Anemo Codex icon, and `1.21.1` `omni_staff` enchantable tags (`durability` / `vanishing`).
- Improved Fabric 1.21.1 parity: dynamic config durability via `ItemStackMixin`, mouse-click selection in `OmniWheelOverlay` via `MouseHandlerMixin`, Creative Combat tab entries for Omni Staves, and multiplayer-safe local config saving.

---

## [1.0.0] - 2026-11-09

### Added

- **Combat Staves & Netherite Upgrades**:
  - 12 elemental combat staves: Inferno Rod (![Pyro][pyro]), Ghastfire Staff (![Pyro][pyro]), Guardian Lens (![Hydro][hydro]), Frostbolt Wand (![Cryo][cryo]), Stormcaller (![Electro][electro]), Wind Staff (![Anemo][anemo], 1.21+ only), Resonant Echo (![Anemo][anemo]), Webcaster (![Dendro][dendro]), Evoker Fang Staff (![Geo][geo]), Shulker Seeker (![Electro][electro]), Dragonfire Scepter (![Electro][electro]), and Wither Scepter (Nether).
  - Smithing Table upgrade system: Upgrade basic staves and artifacts with a Netherite Ingot and Netherite Upgrade Template for boosted durability, damage, and reduced cooldowns.
  - Balanced around vanilla cooldowns and durability; fully enchantable with `Unbreaking` and `Mending`.

- **Utility & Mobility Artifacts**:
  - **Void Translocator**: Safely blinks forward through obstacles (up to 24 blocks Basic / 36 blocks Netherite).
  - **Magic Mirror**: Channels a recall back to bed or world spawn point (intra-dimension Basic / cross-dimension Netherite).

- **Elemental Gauge Theory (EGT) System**:
  - 7 elements: Pyro ![Pyro][pyro], Hydro ![Hydro][hydro], Cryo ![Cryo][cryo], Electro ![Electro][electro], Anemo ![Anemo][anemo], Geo ![Geo][geo], and Dendro ![Dendro][dendro].
  - 5 persistent aura elements (Pyro, Hydro, Cryo, Electro, Dendro) with 0.8x aura tax and decay over time ($Duration = 2.5U + 7.0s$); Anemo and Geo act as trigger-only elements.
  - Complete elemental reaction matrix: Vaporize, Melt, Overloaded, Superconduct, Frozen & Shatter, Electro-Charged, Burning, Quicken (Aggravate & Spread), Bloom (Hyperbloom & Burgeon), Swirl, and Crystallize.
  - World & Creature Interactions: Innate elements for vanilla creatures (Blaze, Stray, Guardian, Enderman, Armadillo, etc.) with matching element immunities; environmental aura application from rain, water, lava, fire, powder snow, and redstone/amethyst blocks.

- **Combat Feedback & Visuals**:
  - 3D Floating Combat Text displaying reaction titles and elemental damage numbers.
  - HD overhead 32x32 pixel-art element icons indicating active auras and dual-reaction animations.
  - Jade / Waila HUD tooltip integration showing active element gauges and remaining units (U).

- **Multi-Loader Support & Configuration**:
  - Full feature parity across **NeoForge 1.21.1** (Java 21), **Forge 1.20.1** (Java 17), and **Fabric 1.21.1** (Java 21).
  - Centralized TOML configuration (`config/velorise_staves.toml`) for damage multipliers, individual staff cooldowns, durability, and visual toggles.
  - Automated simultaneous multi-loader compilation script (`build-all.ps1`).
  - Full localization support for English (`en_us`), Vietnamese (`vi_vn`), and Simplified Chinese (`zh_cn`).

[pyro]: shared-resources/assets/velorise_staves/textures/element/pyro.png
[hydro]: shared-resources/assets/velorise_staves/textures/element/hydro.png
[cryo]: shared-resources/assets/velorise_staves/textures/element/cryo.png
[electro]: shared-resources/assets/velorise_staves/textures/element/electro.png
[anemo]: shared-resources/assets/velorise_staves/textures/element/anemo.png
[geo]: shared-resources/assets/velorise_staves/textures/element/geo.png
[dendro]: shared-resources/assets/velorise_staves/textures/element/dendro.png
[omni]: shared-resources/assets/velorise_staves/textures/element/omni.png
