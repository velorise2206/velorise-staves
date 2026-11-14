# Changelog

All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.2] - 2026-11-14

### Changed & Fixed (All 3 Loaders: NeoForge 1.21.1, Fabric 1.21.1, Forge 1.20.1)

- **Neuvillette (`Guardian Lens`) Rework**:
  - Drains **8% Max HP every 0.5s** (`10 ticks`, up to `48% Max HP` over the 3.0s channel) and **automatically stops draining at `<= 50% Max HP`** while the beam continues firing normally.
  - **Sourcewater Droplets** now heal **16% Max HP** each (3 droplets = `48% Max HP`) and can **only be picked up or absorbed by their creator (`ownerId`)**.
  - The Hydro beam now **pierces through all enemies** along the straight line up to max range (`24m` / `28m`), accompanied by high-density rushing water particles and surging water audio on charge and fire.
- **Zhongli (`Vortex Vanquisher` & `GeoFangPillarEntity`) Updates**:
  - Reduced Stone Stele Geo resonance wave radius from `6 blocks` to **`4 blocks`** (`4.5D`).
  - Fixed underwater & mid-air pillar placement (`ClipContext.Block.COLLIDER` + construct raycast + downward solid/construct landing resolution and gravity settling), fixed standing on pillars submerged in water on **Fabric 1.21.1** (`supportEntitiesStandingOnTop` + `applyFurinaWaterWalking` construct guard), and enabled **pillar-on-pillar stacking**.
  - Shrunk the **Jade Shield** 3D barrier model by **~50%** so it extends compactly from **neck to knee** (`0.23H` to `0.77H`).
  - Split `Vortex Vanquisher` tooltip by tier: Basic displays **`+20% DMG`**, Netherite displays **`+30% DMG`**.
- **Furina (`Splendor of Tranquil Waters`) Fixes & Balance**:
  - Fixed 3rd-person (`F5` / Multiplayer) pet summon animation not playing because `ClientAuraRenderer.render()` was prematurely removing `AuraDisplayData` when `data.elements` was empty.
  - Adjusted 1st-person and 3rd-person Arkhe switch/summon animations to lower the extended arm in front of the body and rotate the scepter tip **90° straight down into the ground** instead of angled ~45° forward.
  - Updated default cooldown to **`20.0s`** with **`30.0s`** pet duration (matching Genshin Impact), and updated crafting recipe to require **3x `hydro_element`** (slots 2, 5, 6).
- **Crafting Recipes, Omni Staff Tags, JEI & 1.5x Netherite Rule**:
  - Updated recipes for **`ghastfire_staff`** (`pyro_element` in slot 3, `ghast_tear` in slot 5), **`frostbolt_wand`** (`cryo_element` in slot 5), and **`webcaster`** (`dendro_element` in slot 5).
  - Added all Basic & Netherite staves to `*_staves` and `omni_upgradeable_*_staves` item tags and registered all 15 Basic + 15 Netherite staves in `VeloriseJeiPlugin`.
  - Updated **`Arc Caster`** tooltip to **`0.5♥ / hit`** (Basic) and **`0.75♥ / hit`** (Netherite), and standardized all Netherite staves to **1.5x Basic stats** without reducing cooldowns.
  - Added **5 Hidden Advancements**: **Beheaded** (`beheaded`), **I Don't Have Order** (`i_dont_have_order`), **Web Developer** (`web_developer`), **Take My Ball** (`take_my_ball`), and **Solidify!** (`geo_architect`).

---

## [1.0.1] - 2026-11-13

### Added

- **8 Elemental Essence Items & Fixed Hostile Drops**: Added `pyro_element`, `hydro_element`, `anemo_element`, `electro_element`, `dendro_element`, `cryo_element`, `geo_element`, and `omni_element`. Dropped by player-slain hostile enemies without Looting scaling at fixed rates (**5%** Natural Innate, **15%** Mutated, **25%** Omni Mutated; split Slimes/Magma Cubes excluded). Used to craft recipes (`electro_element` replaces Lightning Rod in `arc_caster`, 7 elements combine into `omni_element`, `anemo_element` crafts into Wind Charges / Breeze Rod), repair matching Staves (`omni_element` repairs all Staves), upgrade any non-Omni Staff to `Omni Staff` / `Netherite Omni Staff` preserving Enchantments (`OmniStaffUpgradeRecipe`), and imbue Swords.
- **Element-Imbued Swords**: Combine any `SwordItem` with an Element (or `omni_element`) in an Anvil preserving all Enchantments and Netherite tier. Full-charge swings (`>= 90%`) apply `1.0U` and scale/boost sword damage directly on EGT reactions (physical damage unaffected on Immune targets; excluded from Elemental Resonance and the 4-Staff Cap).
- **Forge 1.20.1 Wind Backport**: Ported `Wind Staff` (`Kind.WIND`), `Netherite Wind Staff`, custom `velorise_staves:breeze_rod`, and throwable `velorise_staves:wind_charge` (`WindChargeEntity`) to Forge 1.20.1 for complete 13-staff parity across all 3 loaders.
- **Elemental Resonance & 4-Staff Cap**: Added 8-type Genshin Elemental Resonance buffs, HUD/Inventory overlays, and duplicate-exempt 4 distinct Staff cap with Omni Auto-mode slot priority (`Mainhand -> Offhand -> Hotbar 0..8 -> Main Inv 9..35`).
- **Omni Staff & Netherite Omni Staff** (![Omni][omni]): Shapelessly crafted from any 1 staff of each of the 7 elements (or upgraded from any Staff + `omni_element`). Fires iridescent 3D prism orbs (`OmniOrbEntity`) that automatically trigger the strongest elemental reaction in **Omni Mode**, or hold `[Tab]` to open the 8-slot radial **Element Wheel** to lock onto any element.
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
