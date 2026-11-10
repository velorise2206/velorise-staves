# Velorise's Staves

[![License: All Rights Reserved](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%20%7C%201.20.1-brightgreen.svg)](https://minecraft.net/)
[![Loaders](https://img.shields.io/badge/Modloader-NeoForge%20%7C%20Forge%20%7C%20Fabric-orange.svg)](https://github.com/hungbbdzz/velorise-staves)

A lightweight Minecraft magic & combat mod introducing **12 elemental combat staves**, **2 utility artifacts**, and a complete **Genshin Impact-inspired Elemental Reaction System (EGT)**.

No complex mana systems, no bloated tiers, and no EMC — just pure, satisfying magic powered by **Vanilla Durability & Cooldowns**, fully enchantable and upgradeable to Netherite.

---

## Features

### Combat Staves (12 Types + Netherite Upgrades)

- **Inferno Rod** (![Pyro][pyro] Pyro): Rapid-fires blazing fireballs in tight bursts.
- **Ghastfire Staff** (![Pyro][pyro] Pyro): Launches explosive fireballs that detonate on contact.
- **Guardian Lens** (![Hydro][hydro] Hydro): Channels a continuous prismatic beam that damages targets and extinguishes environmental fire, soul fire, campfires, and candles.
- **Frostbolt Wand** (![Cryo][cryo] Cryo): Propels frost orbs inflicting deep freeze and severe slowness.
- **Stormcaller** (![Electro][electro] Electro): Summons instant lightning strikes onto your crosshair.
- **Wind Staff** (![Anemo][anemo] Anemo, 1.21+ only): Fires gusting wind charges that trigger rapid Swirl reactions.
- **Resonant Echo** (![Anemo][anemo] Anemo): Releases a devastating Warden sonic boom that pierces through blocks.
- **Webcaster** (![Dendro][dendro] Dendro): Traps enemies in temporary toxic cobwebs that dissolve automatically.
- **Evoker Fang Staff** (![Geo][geo] Geo): Erupts a line of evoker fangs; the Netherite tier also raises a **Geo Construct** (`HP = 2x creator's max HP`, up to 3 active) that blocks attacks/projectiles and pulses 4m concentric Geo waves every 2s.
- **Shulker Seeker** (![Electro][electro] Electro): Fires homing shulker bullets causing levitation.
- **Dragonfire Scepter** (![Electro][electro] Electro): Hurls dragon breath creating a lingering mystic damage cloud.
- **Wither Scepter** (Dendro/Nether): Discharges a 3-skull spread inflicting Wither decay and applying Dendro.

*All staves can be upgraded to Netherite via the Smithing Table for boosted damage, range, and reduced cooldowns, and scale with the custom **Elemental Mastery I–III** enchantment.*

> [!NOTE]
> **Version Note:** The **Wind Staff** is exclusive to Minecraft 1.21.1 (NeoForge & Fabric), as its crafting recipe and mechanics utilize Breeze Rods and Wind Charges introduced in the 1.21 Tricky Trials update (Trial Chambers). Minecraft 1.20.1 (Forge) purposefully features 11 combat staves and uses **Resonant Echo** as its primary Anemo weapon.

### Utility Artifacts & Codex

- **Void Translocator**: Blink forward up to 24 blocks (36 Netherite) through solid obstacles safely.
- **Magic Mirror**: Channel to recall back to your bed or world spawn point (cross-dimensional on Netherite).
- **Elemental Codex**: Interactive in-game guidebook with clickable reaction links, heart-rendered damage stats, live server config sync, and **9-language** localization (`en_us`, `vi_vn`, `zh_cn`, `ja_jp`, `ko_kr`, `de_de`, `es_es`, `fr_fr`, `ru_ru`).

---

## Elemental Reaction System (EGT)

Built upon **Elemental Gauge Theory**, attacks and the environment apply elemental auras with decay, internal cooldowns (ICD), and reactions:

- **7 Elements**: Pyro ![Pyro][pyro], Hydro ![Hydro][hydro], Cryo ![Cryo][cryo], Electro ![Electro][electro], Anemo ![Anemo][anemo], Geo ![Geo][geo], Dendro ![Dendro][dendro].
- **Core Reactions**:
  - **Vaporize** (![Hydro][hydro] + ![Pyro][pyro]) & **Melt** (![Pyro][pyro] + ![Cryo][cryo]) — Multiplicative damage scaling.
  - **Overloaded** (![Pyro][pyro] + ![Electro][electro]) — High-knockback AOE explosion.
  - **Frozen & Shatter** (![Hydro][hydro] + ![Cryo][cryo], then ![Geo][geo]/Heavy) — Locks enemies in ice, then shatters for heavy damage.
  - **Electro-Charged** (![Hydro][hydro] + ![Electro][electro]) — Continuous chain lightning shock.
  - **Burning** (![Pyro][pyro] + ![Dendro][dendro]) — Rapid continuous flame ticks.
  - **Quicken, Aggravate & Spread** (![Dendro][dendro] + ![Electro][electro]) — Flat elemental damage amplification.
  - **Bloom, Hyperbloom & Burgeon** (![Hydro][hydro] + ![Dendro][dendro], then ![Electro][electro]/![Pyro][pyro]) — Spawns 3D physical Dendro Cores that burst or home into enemies.
  - **Swirl** (![Anemo][anemo] + Aura) — Spreads absorbed elements in an expanding gale.
  - **Crystallize** (![Geo][geo] + Aura) — Drops 3D elemental crystal shards providing player shields and 250% matching resistance.

### World & Creature Interactions

- **Innate Creature Elements**: Nether mobs (Blaze, Magma Cube, Piglin) possess Innate Pyro; Strays have Innate Cryo; Guardians have Innate Hydro; Endermen have Innate Electro; Iron Golems have Innate Geo, etc. When attacking, they inflict their innate element; same-element attacks trigger **IMMUNE**.
- **Environmental Afflictions & Tesla Coil**: Water & Rain apply Hydro (and Hydro beams extinguish fire/campfires/candles); Lava & Fire apply Pyro; Powder Snow applies Cryo; Cactus & Sweet Berries apply Dendro; Stalactites apply Geo; Thorns armor reflects **0.5U per level** of the target's element back to attackers. Placing a Copper Lightning Rod atop a Redstone Block builds a **Tesla Coil** firing 3–5 Electro arcs every 2s (2x stats during rain or 30s on lightning strike).

---

## Visual & Combat Feedback

- **3D Floating Combat Text**: Reaction titles (`VAPORIZE`, `IMMUNE`, etc.) and elemental damage numbers pop cleanly at torso height.
- **HD Overhead Element Icons**: 32x32 pixel-art icons above mobs displaying active auras and dual-reaction animations.
- **Jade / Waila HUD**: Native tooltip integration showing active element gauges and remaining durations.

---

## Building from Source

Supports **NeoForge 1.21.1**, **Forge 1.20.1**, and **Fabric 1.21.1**:

```powershell
# Clone repository
git clone https://github.com/hungbbdzz/velorise-staves.git
cd velorise-staves

# Build all 3 loaders simultaneously
powershell -ExecutionPolicy Bypass -File .\build-all.ps1
```

Compiled jar artifacts will be placed in the `mods/velorise_staves/` directory.

---

## License & Disclaimer

Copyright (c) 2026 **hungbbdzz** — **All Rights Reserved (Code & Documentation Only)** ([LICENSE](LICENSE)).

- **Scope of License (Excluding Assets):** The **All Rights Reserved** license applies solely to this mod's original source code, compiled binaries, data/localization files, and documentation authored by **hungbbdzz**. You may NOT copy, redistribute, reupload, modify, or create derivative works from this mod's source code or binaries without prior written permission from the copyright holder. It does **not** cover or claim exclusive ownership over visual assets, element icons, textures, or 3D models.
- **Third-Party IP Disclaimer:** *Velorise's Staves* is an **unofficial, non-commercial fan-made mod** inspired by *Genshin Impact* and is **not affiliated with, endorsed by, or associated with HoYoverse / Cognosphere Pte. Ltd.** (or Mojang / Microsoft). All *Genshin Impact* trademarks, names, element icons/symbols, and related IP belong to **HoYoverse / Cognosphere Pte. Ltd.**; any third-party fan textures or 3D models belong to their respective creators.

[pyro]: shared-resources/assets/velorise_staves/textures/element/pyro.png
[hydro]: shared-resources/assets/velorise_staves/textures/element/hydro.png
[cryo]: shared-resources/assets/velorise_staves/textures/element/cryo.png
[electro]: shared-resources/assets/velorise_staves/textures/element/electro.png
[anemo]: shared-resources/assets/velorise_staves/textures/element/anemo.png
[geo]: shared-resources/assets/velorise_staves/textures/element/geo.png
[dendro]: shared-resources/assets/velorise_staves/textures/element/dendro.png

