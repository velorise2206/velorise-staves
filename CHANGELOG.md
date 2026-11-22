# Changelog

All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.1] - 2026-11-22

### Added (All 3 Loaders: NeoForge 1.21.1, Fabric 1.21.1, Forge 1.20.1)

- **Authentic Genshin Wall Climbing**: Added wall clinging, climbing up/down, lateral shimmying, stamina consumption, smooth ledge mantling (including climbing & mantling Geo Constructs / Zhongli's Stone Stele), 3D climber animations, and 80-tick anti-kick protection.
- **Plunging Attack Rework**: Scaled plunge damage by weapon base (`0.64x` Mid-Air, `1.28x` Low, `1.60x` High) with tiered knockback/VFX and Genshin height-based plunge fall damage.
- **Cryo Regisvine (Internal Testing)**: Added Regisvine boss entity with procedural 3D laser beams, 14 Blockbench animations, and shield mechanics for API testing (summonable via `/summon velorise_staves:cryo_regisvine`; hidden from survival recipes & creative tab).
- **Public API (`com.velorise.staves.api`)**: Unified mod API and cross-loader `ElementalShieldManager`.

### Fixed & Changed

- **Mavuika Flamestrider**: Refactored edge-triggered FSM & gcsim-aligned combo timing, added smooth `partialTicks` front/rear wheel spin and steering animation, and fixed the Ultimate sideways wheelie facing direction (now faces right from the player's view).
- **Raiden Shogun Overhaul**: Added authentic 3-stage 3D dimensional space-tear slashes, snap-open cosmic eye rift with 3-petal Mitsudomoe crest and starburst rays in 1st-person Burst, clean asymmetric X-rift Charged Attack, balanced E coordinated strikes, and restored exact 1.1.0 levitation isolated before RenderLivingEvent.Post so Magatsu hands remain grounded.
- **Nahida Combat & Animations**: Added `50` Stamina cost to Nahida's Charged Attack, migrated swing/viewfinder/platform offsets to `LivingEntityRenderer` internal `PoseStack.pushPose()` / `popPose()` block with exact 1.1.0 seat alignment (-3.2px drop, +3.45px sway along swing facing), preventing RenderLivingEvent.Post prop stacking and external animation mod overrides (`player-animation-lib`, `EMF`, `Mo' Bends`).
- **Venti Holy Lyre Idle**: Transcribed 3 authentic HoYoLAB Windsong Lyre sheets into Note Block range with dedicated warm NOTE_BLOCK_BASS chords, flute highlights, note particles, and non-repeating alternating playback; locked Genshin Main Theme (28.2s) to exact 3/4 Waltz (24t/bar) with pickup note separated so all melody/bass downbeats land precisely on beat 1.
- **HUD Improvements**: Fixed Furina CA hold charge progress arc not updating on Fabric 1.21.1 and early-release reset on NeoForge/Forge; aligned Neuvillette's charge icon fill to exact opaque bounds (`Y=3..11`) for a smooth 0.0s → 3.0s visual indicator.
- **Mini Ender Dragon Targeting**: Refactored target evaluation so the dragon pet immediately follows up on any target attacked by the owner (`player.getLastHurtMob()`), including passive animals, villagers, target dummies, and NoAI mobs, while preserving strict exploit prevention for ambient scanning and Resonant Echo.
- **Geo Construct Shatter Rules**: Enforced authentic Genshin mechanics where Geo Constructs (`GeoFangPillarEntity` / Zhongli's Stone Stele) immediately shatter if spawned directly overlapping a Boss (including Cryo Regisvine) or oversized entity (`width >= 1.5` or `height >= 2.8`), preventing bosses from being lifted or stuck on pillars.
- **Keybinds & Cleanup**: Renamed Burst keybind to `key.velorise_staves.elemental_burst` across all 3 loaders and 9 locales, updated `mod_authors` to `velorise2206`, and deduplicated shared resources.

---

## [1.1.0] - 2026-11-19

### Added (All 3 Loaders: NeoForge 1.21.1, Fabric 1.21.1, Forge 1.20.1)

- **New Staves, Archon Kits & Relics**:
  - **7 Archon Weapons & 3 Genshin Weapons**: *The Daybreak Chronicles*, *Vortex Vanquisher*, *Engulfing Lightning*, *A Thousand Floating Dreams*, *Splendor of Tranquil Waters*, *A Thousand Blazing Suns*, *Silver Dawn* (Tsaritsa Bow/Spear), *Guardian Lens*, *Nights of Formal Focus*, and *Peak Patrol Song*, complete with 3D Blockbench models (Raiden Robot, Tsaritsa Throne, Nahida Shrine, Mavuika Flamestrider & Helmet, Xilonen Skates), 8 AFK 3D idle animations, and character voice-over packs (`[V]`).
  - **Wind Glider & Materials**: *Wings of First Flight* (`[Space]` mid-air glider + Plunge Attack, upgraded to 5m Ender Dragon Glider when carrying Dragonfire Scepter) and **Frost Rod** (`cryo_element` + Blaze/Breeze Rod).
  - **39 Custom Advancements**: Expanded advancement progression including hidden elemental & staff challenges.
- **Animations, HUD & Codex UX**:
  - Added 1st-person and 3rd-person (`F5` / Multiplayer) animations, Charge Progress Bar HUD, 16x16 default item icons with 32x32 HD toggle, F3 HUD auto-hide, Creative Tab sorting (`Basic Staves -> Genshin Weapons -> Archon Weapons` in Basic + Netherite pairs), and clickable Codex hyperlinks with highlighted keywords across all 9 locales.

### Changed & Fixed

- **Dragonfire Scepter & Reworks**: Right-click toggles the free-flying Mini Ender Dragon (no item cooldown, `0.25s` anti-spam gate, 30s duration, immediate target switching on owner hit, snout-aligned lightning arcs, idle blocked while active); `[R]` Burst (`20.0s` CD) supports *Skyfall Dive* (`10x10m`/`12x12m`) & *Sweeping Strafe* (`5x20m`/`6x24m`); Left-click fires Plasma Orb (`4.0s` CD).
- **Combat Combos & Visual Polish**: Added Genshin-authentic Mavuika Flamestrider combo (`CCD CD F CD CD( F CDCD(`), single normalized 360° Xilonen skate jump flip, Layla Night Stars inside the Curtain of Slumber sphere, Guardian Lens piercing & `<= 50%` HP drain guard, and Zhongli Stone Stele / Jade Shield fixes.
- **Cross-Loader Parity, Memory & Balance**: Restored 100% shared-class parity across NeoForge 1.21.1, Fabric 1.21.1, and Forge 1.20.1 (including Fabric `OmniWheelOverlay` fast-attack checks and `ElementalGaugeManager` environmental fire checks), standardized Netherite staves to `1.5x` Basic stats, cleared static client/server caches on disconnect/dimension change, and removed unused textures.

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
