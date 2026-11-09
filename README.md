# Velorise's Staves

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%20%7C%201.20.1-brightgreen.svg)](https://minecraft.net/)
[![Loaders](https://img.shields.io/badge/Modloader-NeoForge%20%7C%20Forge%20%7C%20Fabric-orange.svg)](https://github.com/hungbbdzz/velorise-staves)

A lightweight Minecraft magic & combat mod introducing **12 elemental combat staves**, **2 utility artifacts**, and a complete **Genshin Impact-inspired Elemental Reaction System (EGT)**.

No complex mana systems, no bloated tiers, and no EMC — just pure, satisfying magic powered by **Vanilla Durability & Cooldowns**, fully enchantable and upgradeable to Netherite.

---

## Features

### Combat Staves (12 Types + Netherite Upgrades)

- **Inferno Rod** (Pyro): Rapid-fires blazing fireballs in tight bursts.
- **Ghastfire Staff** (Pyro): Launches explosive fireballs that detonate on contact.
- **Guardian Lens** (Hydro): Channels a continuous prismatic laser burning through targets.
- **Frostbolt Wand** (Cryo): Propels frost orbs inflicting deep freeze and severe slowness.
- **Stormcaller** (Electro): Summons instant lightning strikes onto your crosshair.
- **Wind Staff** (Anemo): Fires gusting wind charges that trigger rapid Swirl reactions.
- **Resonant Echo** (Anemo): Releases a devastating Warden sonic boom that pierces through blocks.
- **Webcaster** (Dendro): Traps enemies in temporary toxic cobwebs that dissolve automatically.
- **Evoker Fang Staff** (Geo): Erupts a straight line of vicious evoker fangs along the ground.
- **Shulker Seeker** (Electro): Fires homing shulker bullets causing levitation.
- **Dragonfire Scepter** (Electro): Hurls dragon breath creating a lingering mystic damage cloud.
- **Wither Scepter** (Nether): Discharges a 3-skull spread inflicting deadly Wither decay.

*All staves can be upgraded to Netherite via the Smithing Table for boosted damage, range, and reduced cooldowns.*

### Utility & Mobility Artifacts

- **Void Translocator**: Blink forward up to 24 blocks through solid obstacles safely.
- **Magic Mirror**: Channel to recall back to your bed or world spawn point.

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

[pyro]: shared-resources/assets/velorise_staves/textures/element/pyro.png
[hydro]: shared-resources/assets/velorise_staves/textures/element/hydro.png
[cryo]: shared-resources/assets/velorise_staves/textures/element/cryo.png
[electro]: shared-resources/assets/velorise_staves/textures/element/electro.png
[anemo]: shared-resources/assets/velorise_staves/textures/element/anemo.png
[geo]: shared-resources/assets/velorise_staves/textures/element/geo.png
[dendro]: shared-resources/assets/velorise_staves/textures/element/dendro.png

### World & Creature Interactions

- **Innate Creature Elements**: Nether mobs (Blaze, Magma Cube, Piglin) possess Innate Pyro; Strays have Innate Cryo; Guardians have Innate Hydro; Endermen have Innate Electro; Iron Golems have Innate Geo, etc. When attacking, they inflict their innate element; same-element attacks trigger **IMMUNE**.
- **Environmental Afflictions**: Water & Rain apply Hydro; Lava & Fire apply Pyro; Powder Snow applies Cryo; Thorns apply Dendro; Stalactites apply Geo.

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

## License

This project is licensed under the [MIT License](LICENSE).
