# Changelog

## [1.0.2] - Unreleased (Audit, Xilonen Jump Flip & Cross-Loader Optimization)

### Fixed
- **Mavuika Genshin-Authentic Flamestrider Combo (`CCD CD F CD CD( F CDCD(`) (`FlamestriderBikeEntity`, `OmniWheelOverlay`, `VeloriseStavesClient`)**:
  - Added a 4-tick (`200ms`) Donut Continuation Grace Window (`spinReleaseGraceTicks`) plus early Finisher windup re-hold cancel (`naTicks >= 9`) so releasing and quickly re-holding Left-Click (`(` / Continuous CA tech) seamlessly continues Donut Spin and immediately triggers a new Charged Attack Cyclic (`C`) hit without forcing the Finisher (`F`) jump.
  - Separated Dash Ram hit cooldowns (`dashHitCooldowns`) from Donut Spin hit cooldowns (`spinHitCooldowns`) so Dash Cancels (`D`) never lock out the follow-up Charged Attack (`C`) hit in `CCD` / `CD` strings.
  - Added `bikeComboChainTicks` (`requiredHoldTicks = 1` during active Donut/Dash/Finisher chains), tight 3-tick (`0.55D`) combat step-dash during Charged Attack cancels so the bike stays in melee range, post-Finisher recovery cancel (`FDC` / `F -> CD`), and automatic 2-dash cooldown refresh upon landing Finisher (`F`).
- **Xilonen Skate Jump 360° Front Flip (`ClientAuraTracker`, `FlamestriderBikeRenderer`, `XilonenVisualRenderer`)**:
  - Replaced unanchored `((rider.tickCount + partialTicks) * 30.0F) % 360.0F` rotation with normalized jump-progress tracking (`XILONEN_JUMP_FLIP_DURATION_MS = 740L`, matching `vy = 0.58D` ~15-tick airtime on flat ground) and smooth easing (`easedFlip * 360.0F`) so Xilonen completes **exactly 1 single 360° flip** right as she lands.
  - Added a `350ms` duplicate server-echo guard in `ClientAuraTracker.triggerStaffAnimation` so server broadcast packets do not reset `xilonenJumpFlipStartTime` 1–3 ticks after client takeoff.
  - Fixed mid-air limb untucking at the jump apex (`-0.45D <= dy <= 0.15D`) in `XilonenVisualRenderer.applySkateStrideAndWallClimb` by blending the gymnastic tuck (`Mth.sin(flipProg * PI)`) continuously across the entire flip progress.
- **Cross-Loader Parity & Combat Logic (`RaidenManager`, `ElementalGaugeManager`, `MavuikaManager`, `FlamestriderBikeEntity`, `VentiVisualRenderer`)**:
  - Restored 100% SHA-256 parity across all 20 shared classes in `build-all.ps1` (`RaidenManager.java` line 863 `erds.getReaction() != null`).
  - Fixed `ElementalGaugeManager` line 955 in `neoforge-1.21.1` and `fabric-1.21.1` to check `isTouchingEnvironmentalFireBlock(target)` instead of residual `isOnFire() / getRemainingFireTicks()`, preventing infinite Pyro gauge re-injection after Overloaded/Vaporize/Melt extinguish.
  - Synced dual-innate `enforceInnateLock`, Mavuika Fighting Spirit / Crucible of Death and Life / 2-dash chain cooldown / spin-lean interpolation, and Venti 3D Burst Vortex wind stream bands in `forge-1.20.1`.
- **Memory Leak Prevention & Render Optimization (`VeloriseStavesClient`, `ClientAuraTracker`, `ClientAuraRenderer`, `VentiVisualRenderer`, `EnvironmentalBlockManager`, `ArcaneStaffEvents`)**:
  - Added missing 100-tick `ClientAuraTracker.pruneStale` and `ElementalReactionBadgeRenderer.pruneStaleFrozenStates` calls in `fabric-1.21.1/VeloriseStavesClient.java`, and included `MAVUIKA_STATES` and `RAIDEN_STATES` in `pruneStale`.
  - Cleared static renderer caches (`WindGliderRenderer`, `VentiVisualRenderer`, `TsaritsaVisualRenderer`, `MavuikaVisualRenderer`) on client disconnect/world unload and cleared per-player manager maps + transient attribute modifiers (`XilonenManager`, `NahidaManager`) on server disconnect/dimension change.
  - Skipped per-frame `entitiesForRendering()` iteration in `VentiVisualRenderer` when no Skill E wind orb is active and reduced redundant `level.getEntity()` lookups in `ClientAuraRenderer`.


All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-11-14

### Added (All 3 Loaders: NeoForge 1.21.1, Fabric 1.21.1, Forge 1.20.1)

- **New Staves & Materials**:
  - **Nights of Formal Focus** *(Cryo)* & **Frost Rod**: New Cryo shield staff with Night Stars and homing Shooting Stars, crafted with the new Frost Rod (`cryo_element` + Blaze/Breeze Rod).
  - **Splendor of Tranquil Waters** *(Hydro)*: Summons Salon Solitaire Hydro pets (Ousia) or a healing water summon (Pneuma) with water-walking while active.
  - **Vortex Vanquisher** *(Geo)*: Tap to raise a stackable Stone Stele construct pulsing Geo waves; Hold to conjure a Jade Shield with Geo RES shred.
- **Hidden Advancements**:
  - Added new hidden advancements for staves and elemental reactions.
- **Animations, HUD & Codex UX**:
  - Added 1st-person and 3rd-person (`F5` / Multiplayer) animations for *Splendor of Tranquil Waters*, *Vortex Vanquisher*, and *Ghastfire Staff*.
  - Added a compact Charge Progress Bar HUD beneath the crosshair for charged staves.
  - Added 16x16 default item icons with an optional 32x32 HD client toggle, F3 HUD auto-hide, and clickable reaction hyperlinks in the Elemental Codex.

### Changed & Fixed

- **Guardian Lens Rework**: Pierces all enemies along the beam, stops self-HP drain at `<= 50% Max HP`, and locks Sourcewater Droplets to their creator.
- **Zhongli & Construct Fixes**: Improved underwater, mid-air, and stacked Stone Stele placement, fixed standing on submerged pillars on Fabric 1.21.1, fixed Jade Shield survival absorption cap, and made the Jade Shield barrier more compact.
- **Recipes, Tags & Balance**: Updated crafting recipes (`splendor_of_tranquil_waters`, `evoker_fang_staff`, `ghastfire_staff`, `frostbolt_wand`, `webcaster`), standardized Netherite staves to 1.5x Basic stats without cooldown reduction, synced all staves in tags and JEI, and tuned Elemental Resonance & co-op scaling.
- **Core Bugfixes**: Fixed `StackOverflowError` in `checkInnateMutation`, permanent Pyro aura on entities, `EvokerFangsModel` & i-frame reset for Geo Fang, Arc Caster tooltip display, Overload chain, and screen shake issues.

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
