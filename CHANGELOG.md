# Changelog

All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
