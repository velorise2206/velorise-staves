# Changelog

## [v15] - Stale NoGravity NBT Cleanup & Glider FSM Guard Fix
- **Cleared Persisted `NoGravity: 1b` on Join/Respawn/Tick**: Added automatic `if (player.isNoGravity()) player.setNoGravity(false);` cleanup in `PlungeAndGliderManager.syncPlayerOnJoinOrRespawn` across all 3 loaders (`1.21.1/neoforge`, `1.21.1/fabric`, `1.20.1/forge`) so worlds previously saved during pre-v11 wall-climb tests can never leave players stuck floating in mid-air.
- **Fixed Wind Glider Toggle FSM Guard**: Corrected `PlungeAndGliderManager.handleToggleGlider` across all 3 loaders to check `MovementActionState.GLIDING` (only when opening) instead of `PLUNGE_DIVE`.

### Wall Climb v14 — Final Per-Tick Data Wipe Fix (All 3 Loaders)
- **Fixed Root-Cause Per-Tick Flicker & Frozen Gait (`ElementalReactionBadgeRenderer.java`)**: Added `!data.steveWallClimbing`, `data.steveWallClimbFade <= 0.01F`, `data.steveWallJumpUntilMs <= now`, and `data.steveWallMantleUntilMs <= now` guards before `ClientAuraTracker.remove(entity.getId())` in `renderReactionBadges()`. Previously, `ElementalReactionBadgeRenderer` deleted the player's `AuraDisplayData` entry on the first render frame of every tick, resetting `steveWallClimbFade`, `steveWallClimbDist`, `steveWallMoveX/Y`, and `steveWallMantleUntilMs` back to zero 20 times per second (causing both the 20Hz standing-pose flicker and the frozen climb cycle).
- **Exact `partialTick` Continuity (`WallClimbVisualRenderer.java`)**: Cached `lastRenderPartialTick` from `LivingEntityRendererMixin` so `applyWallClimbHumanoidPose` never falls back to `0.0F` when `ageInTicks` is zero, and removed all temporary v13 diagnostic logs.

### Wall Climb v13 — Root-Cause Flicker Fix (All 3 Loaders)
- **Fixed 10Hz Pose Flicker (`ClientAuraTracker.java`)**: Added `!entry.steveWallClimbing`, `entry.steveWallClimbFade <= 0.01F`, `entry.steveWallJumpUntilMs <= now`, and `entry.steveWallMantleUntilMs <= now` guards to the empty-aura cleanup check (`DATA.remove(entityId)`). Previously, periodic server aura sync packets deleted the player's `AuraDisplayData` entry every ~2 ticks while climbing, wiping `steveWallClimbFade`, `steveWallClimbDist`, and `wallNormalFace` and causing a 1-frame snap back to the standing pose.
- **Unified `HumanoidModelMixin` Pose Pipeline**: Kept wall-climb humanoid pose execution on the proven `HumanoidModelMixin` -> `ClientAuraRenderer.applyStaffHumanoidAnimation` path paired with the waist-pivot `(0, 0.9, 0)` `PoseStack` transform in `LivingEntityRendererMixin`.

### Wall Climb v11 — Authentic ParCool! Architecture Overhaul (All 3 Loaders)
- **Zero-Jitter Enforced Movement (`LocalPlayerMixin.move`)**: Added `LocalPlayerMixin` across NeoForge 1.21.1, Fabric 1.21.1, and Forge 1.20.1 to intercept `MoverType.SELF` during wall climbing and ledge mantling, canceling vanilla gravity/drag displacement before `super.move()` just like ParCool!'s `LocalPlayerMixin` + `BehaviorEnforcer`.
- **2-Phase Cubic-Eased `ClimbUp` Ledge Mantle**: Replaced velocity-impulse vaulting with ParCool!'s exact 2-phase cubic ease-in-out target-point trajectory (Phase 1 lifts straight up above the lip; Phase 2 glides cleanly onto the top surface with `+0.0625` block clearance).
- **6-DOF Keyframe + `Quaternionf.slerp` Limb Animation (`WallClimbVisualRenderer`)**: Replaced sine-wave Euler interpolation with 4-keyframe cubic-eased rock-climber cycles (Climb Up/Down, Lateral Shimmy, 3-keyframe ClimbUp Vault, Wall Jump, and breathing Cling Idle) combining 3-axis quaternion slerp with pixel bone translation (`part.x/y/z`) for shoulder reach and bent-knee foothold articulation.
- **Whole-Body Waist-Pivot Transform & Dirty-Pose Cleanup**: Moved wall-facing lock, wall-hug (`-0.175`), pitch lean, climber twist, roll weight-shift, and vertical heave into a single waist-pivot `(0, 0.9, 0)` `PoseStack` hook right before `PoseStack.scale(-1, -1, 1)` (eliminating `yBodyRot` snapping and hip-seam breaks), paired with `HumanoidModelMixin` `resetPose()` cleanup at `setupAnim` HEAD.

## [Unreleased] - Shared FSM Core & Authentic Genshin Wall Climbing (`MovementMachine`)

- **ParCool!-Style Wall-Climb Overhaul & Single-Pass Pose Pipeline (v10 — All 3 Loaders)**:
  - Eliminated double-execution of `WallClimbVisualRenderer.applyWallClimbHumanoidPose()` (removed duplicate call in `ClientAuraRenderer`, keeping the single post-`setupAnim` execution in `LivingEntityRendererMixin` across NeoForge, Fabric, and Forge).
  - Upgraded `WallClimbVisualRenderer` to a full 3-axis ParCool!-style climber gait: shaped reach-grip-pull wave, inward hand-angle `yRot`, frog-knee outward `zRot` splay, dynamic torso twist/lean, vertical heave bob, and directional head tilt looking toward the next hold.
  - Replaced multi-tick `LEDGE_MANTLE` velocity lock with a ParCool! `ClimbUp` single-vault impulse (`vaultVy = 0.32..0.46`, `inward = 0.22`) that immediately hands control back to vanilla physics to land smoothly on top of any ledge without getting stuck.
  - Kept `prepareLocalWallClimbPreTick()` strictly read-only on FSM state (`localWallLostGraceTicks == 0`) to eliminate seam jitter while preserving instant Shift exit and 2-tick wall-loss auto-exit.

- **Wall-Climb 2-Phase Ledge Mantle, Cross-Loader 3rd-Person Guarantee & Mid-Air Fix (v9 — All 3 Loaders)**:
  - Rewrote ledge mantling into a ParCool-style 2-phase maneuver (`WallClimbHelper.findLedgeTopY`): triggers only when feet are within `1.05` blocks of the top lip with 2 clear blocks above, rises straight up past the lip first (Phase 1, zero inward block slam), then steps smoothly onto the top surface (Phase 2).
  - Fixed mid-air freeze on Forge 1.20.1 by exiting wall climb immediately in `prepareLocalWallClimbPreTick()` whenever no wall is adjacent, and allowing Shift drop-off to cancel any wall state immediately.
  - Added a post-`EntityModel.setupAnim` injection in `LivingEntityRendererMixin` across all 3 loaders so NeoForge 1.21.1 and Forge 1.20.1 reliably render the exact same 3rd-person wall-climbing animation as Fabric 1.21.1.
  - Replaced the intrusive 1st-person climbing arm overlay with a clean, subtle ParCool-style main-hand rhythm bob (removing the floating extra offhand arm).

- **Wall-Climb Wall-Hug Transform, 1st-Person Hands & Mantle Cap (v8 — All 3 Loaders)**:
  - Added `WallClimbVisualRenderer.pushWallClimbBodyTransform()` hooked into `LivingEntityRendererMixin` across all 3 loaders to translate the player mesh `0.155` blocks inward toward the wall face while climbing, eliminating the floating air gap between Steve and the block surface.
  - Added first-person climbing animations (`applyFirstPersonWallClimbTransform` & `setupFirstPersonEmptyOffhandClimbPose`) in `ClientAuraRenderer` so both hands visibly reach and pull in 1st-person view.
  - Guaranteed immediate climbing pose engagement across all wall states (`WALL_CLING`, `WALL_CLIMB`, `WALL_JUMP`, `LEDGE_MANTLE`) and added a wall-contact safety guard in `prepareLocalWallClimbPreTick()` plus toned-down 4-tick `LEDGE_MANTLE` (`0.24 -> 0.08`) to prevent launching skyward on Forge 1.20.1.

- **Wall-Climb ParCool / NotEnoughAnimations Architecture Pass (v7 — All 3 Loaders)**:
  - Refactored `WallClimbVisualRenderer` into a 100% read-only, tick-interpolated renderer (`partialTick = ageInTicks - entity.tickCount`) with zero `System.nanoTime()` and zero state mutation inside `setupAnim()`, eliminating multi-pass pose flicker.
  - Upgraded climbing poses to authentic contra-lateral hand-over-hand reach/pull (`-2.95` to `-0.75 rad`), opposite knee drive (`-1.15` to `+0.45 rad`), torso twist, vertical climb bob, hip-pivot leg alignment (`setPos`), and distinct lateral shimmy / climb down / mantle / jump poses.
  - Removed `player.setNoGravity(true/false)` toggling on both client and server (`ClientAuraTracker`, `PlungeAndGliderManager`) so server `DATA_NO_GRAVITY` `SynchedEntityData` packets never fight client climbing motion; retained 80-tick anti-kick (`aboveGroundTickCount = 0`) and `fallDistance = 0.0F`.
  - Added `preferredNormal` face hysteresis (`+0.55D`) in `WallClimbHelper.findClimbWall` and zero-velocity Cling Idle (`0, 0, 0`) to prevent corner face-flipping and wall bounce.

- **Wall-Climb Anti-Flicker & Animation Pass (v6 — All 3 Loaders)**:
  - Suppressed vanilla WASD/Jump movement impulses while wall climbing (`MovementInputUpdateEvent` on NeoForge 1.21.1 & Forge 1.20.1, `KeyboardInputMixin` on Fabric 1.21.1) so `LivingEntity.travel()` no longer fights tangent-plane climbing velocity at 20Hz.
  - Added `ClientAuraTracker.prepareLocalWallClimbPreTick()` hooked into `ClientTickEvent.Pre` / `START_CLIENT_TICK` / `TickEvent.Phase.START` to apply climbing velocity, `noGravity`, and locked `yBodyRot` *before* vanilla movement integrates each tick.
  - Replaced constant inward wall slam with gentle conditional wall-hug bias (`0.018D` only when moving or not flush) and added a 10-tick server keepalive with `1.15D` attach tolerance in `PlungeAndGliderManager`.
  - Upgraded `WallClimbVisualRenderer` to clear `ArmPose` while climbing and widen Climb Up / Down / Strafe arm-and-leg reach/pull arcs (`BLEND_STIFFNESS = 22.0F`, `8.4 rad/s`) so active climbing is clearly distinct from Cling Idle.
- **Shared FSM Core (`com.velorise.staves.fsm`)**: Added reusable `StateNode`, `InputBuffer` (4–5 tick TTL), `PoseBlender` (exponential damping `1 - exp(-k * dt)`), and `MovementActionState` (`GROUNDED`, `WALL_CLING`, `WALL_CLIMB`, `WALL_JUMP`, `LEDGE_MANTLE`, `GLIDING`, `PLUNGE_DIVE`, `EXHAUSTED_FALL`) identically in `1.21.1/common` and `1.20.1/forge`.
- **Genshin Wall Climbing & Anti-Kick**: Added `WallClimbHelper` (`isFaceSturdy` filter, overhang blocking, `canLedgeMantle`) and `ServerGamePacketListenerAccessor` (`aboveGroundTickCount = 0` anti-80-tick floating kick) across `1.21.1/neoforge`, `1.21.1/fabric`, and `1.20.1/forge`.
- **Client Prediction, Pose Blending & Xilonen/Mavuika Isolation**: Implemented tangent-plane climbing, 0-stamina Cling (pausing regen), 25-stamina Climb Jump with `InputBuffer`, Ledge Mantle, `WallClimbVisualRenderer` + `LivingEntityRendererMixin` `yBodyRot` wall lock, priority gating (`MAVUIKA_BIKE > XILONEN_SKATE > VANILLA_CLIMBABLE > UNIVERSAL_AIR_MOVE`), and Genshin `EXHAUSTED_FALL` plunge lock.


## [Unreleased] - Authentic Genshin Plunging Attack Rework
- **Base-Scaled Plunging Attack (`PlungeAndGliderManager`)**: Replaced fixed plunge scaling with unified weapon-base multipliers following Genshin Impact's 1 : 2 : 2.5 ratio (`0.64x` Mid-Air Collision every 6 ticks / 0.3s in 1.0m radius, `1.28x` Low Plunge for `<= 2.4` blocks in 3.0m radius, `1.60x` High Plunge for `> 2.4` blocks in 5.0m radius) across `1.21.1/neoforge`, `1.21.1/fabric`, and `1.20.1/forge`.
- **Radial Impulse & VFX/SFX Tiering**: Added Low/High Plunge radial knockback (`0.25` vs `0.55` + `0.22Y` lift), scaled shockwave particle rings, and distinct audio pitch.
- **Authentic Genshin Plunge Fall Damage**: Removed unconditional fall-damage immunity on plunge landing; now computes fall damage strictly from plunge-start height using Genshin tiers (`0–40m`: 0% taken, `40–60m`: 10% taken, `60–100m`: 20% taken, `>100m`: 40% taken), including water-walking landings.


All notable changes to **Velorise's Staves** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
