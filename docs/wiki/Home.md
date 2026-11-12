# Velorise's Staves & Elements Documentation

<div align="center">
  <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/omni_staff.png" width="64" height="64" alt="Omni Staff" />
  <p><i>A dynamic, environment-interactive elemental combat and magic framework for Minecraft.</i></p>

  <p>
    <a href="https://modrinth.com/mod/velorises-staves-elements"><img src="https://img.shields.io/badge/Modrinth-Release-00AF5C?style=flat-square&logo=modrinth&logoColor=white" alt="Modrinth" /></a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/velorises-staves-elements"><img src="https://img.shields.io/badge/CurseForge-Release-F16436?style=flat-square&logo=curseforge&logoColor=white" alt="CurseForge" /></a>
    <img src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1-blue?style=flat-square" alt="Versions" />
  </p>
</div>

---

## Overview

**Velorise's Staves & Elements** transforms combat in Minecraft from basic sword spamming into an intricate, responsive elemental magic system inspired by modern Action-RPGs.

Instead of generic magic projectiles that simply apply flat damage or vanilla potion effects, Velorise's Staves implements a dedicated **Elemental Gauge Theory (EGT)** engine:

* **7 Arcane Elements + Omni Essence:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png" width="16" height="16" /> Pyro, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="16" height="16" /> Hydro, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png" width="16" height="16" /> Cryo, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="16" height="16" /> Electro, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png" width="16" height="16" /> Dendro, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="16" height="16" /> Anemo, <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png" width="16" height="16" /> Geo, and <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/omni.png" width="16" height="16" /> Omni.
* **15+ Chain Reactions:** Multiplicative damage (Vaporize, Melt), persistent stuns (Frozen, Shatter), cascading area-of-effect shocks (Electro-Charged), and organic seed explosions (Bloom, Hyperbloom, Burgeon).
* **13 Distinct Arcane Staves:** Each with dedicated projectile types, burst mechanics, charging phases, or continuous channeled streams.
* **Anvil Weapon Imbuing:** Infuse standard swords (Wood through Netherite) with elemental essences on an anvil for 3 XP levels to trigger aura applications upon full-strength melee strikes.
* **Physical World Interactions:** Electrify entire bodies of water, freeze water surfaces into temporary ice bridges, summon Geo pillars, and resonate with Sculk sensors.

---

## Quick Start Guide

<div align="center">
  <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/docs/showcase/vaporize.gif" width="480" alt="Vaporize Demo" />
  <br/><sub><i>Applying Hydro to wet an enemy, followed by a Pyro strike to trigger a 2.0x Forward Vaporize multiplier.</i></sub>
</div>

### Step 1: Collecting Elemental Essences
Defeat mobs that naturally channel elemental properties (5% drop chance on player kill) or hostile mobs with innate mutations (7% spawn chance, 15% drop chance):
* **Pyro Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/pyro_element.png" width="16" height="16" /> Dropped by Blazes, Magma Cubes, Ghasts & Wither Skeletons.
* **Hydro Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/hydro_element.png" width="16" height="16" /> Dropped by Drowned, Guardians & Elder Guardians.
* **Cryo Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/cryo_element.png" width="16" height="16" /> Dropped by Strays, Snow Golems & Polar Bears.
* **Electro Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/electro_element.png" width="16" height="16" /> Dropped by Shulkers, Endermen, Endermites, Powered Creepers & Ender Dragons.
* **Anemo Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/anemo_element.png" width="16" height="16" /> Dropped by Breezes, Phantoms & Vexes.
* **Dendro Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/dendro_element.png" width="16" height="16" /> Dropped by Bogged, Cave Spiders, Bees, Mooshrooms & standard Slimes.
* **Geo Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/geo_element.png" width="16" height="16" /> Dropped by Armadillos (and mutated hostile mobs).
* **Omni Element:** <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/omni_element.png" width="16" height="16" /> Dropped by rare Omni-mutated mobs (25% drop rate) or crafted shapelessly from all 7 elemental essences.

### Step 2: Crafting Your First Staff
Combine crafting ingredients in a Crafting Table:
* **Frostbolt Wand (Cryo):** 1 Blue Ice + 2 Blaze Rods placed diagonally.
* **Inferno Rod (Pyro):** 1 Fire Charge + 2 Blaze Rods placed diagonally.
* **Arc Caster (Electro):** 1 Electro Element + 2 Amethyst Shards placed diagonally.

### Step 3: Crafting the Elemental Codex
Place **1 Book + 1 Amethyst Shard** anywhere in a Crafting Table (shapeless recipe) to craft the <img src="https://raw.githubusercontent.com/hungbbdzz/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/elemental_codex.png" width="16" height="16" /> **Elemental Codex**. Right-clicking with this book opens an in-game GUI displaying all recipes, reaction formulas, and staff stats.

---

## Controls & Keybindings

| Action | Default Input | Description |
| :--- | :--- | :--- |
| **Cast Primary Spell** | `Right-Click` | Discharges the elemental projectile, burst volley, or channeled beam. |
| **Charge Spell** | `Hold Right-Click` | Required for charged staves (Resonant Echo: 1.5s; Ghastfire Staff: 2.0s). |
| **Open Omni Wheel** | `R` or `Shift + Right-Click` | Opens the radial elemental selector for the Omni Staff to switch active elements. |
| **Recall to Bed / Spawn** | `Hold Right-Click` (Magic Mirror) | Channels for 2.5s (or 1.0s for Netherite) to recall back to your bed or spawn point. |

---

## Wiki Directory

* **[Elements & Reactions](Elements-and-Reactions)**: Complete breakdown of all 15+ reactions, damage scaling equations, and aura duration mechanics.
* **[Staves & Weapons](Staves-List)**: In-depth analysis of the 13 base staves and their Netherite-tier upgrades.
* **[Anvil Weapon Imbuing](Anvil-Imbuing)**: Guide to infusing swords with elemental properties at an anvil.
* **[Environmental Interactions](Environmental-Interactions)**: Details on electrified water, temporary frost walking, Geo pillars, and Sculk resonance.
* **[Relics & Utilities](Relics-and-Utilities)**: Documentation for the Magic Mirror, Void Translocator, and Crystallize shields.
* **[Configuration Guide](Configuration-Guide)**: Complete documentation of server and client configuration options.
* **[FAQ & Compatibility](FAQ-and-Compatibility)**: Multiplayer support, mod compatibility (JEI, Jade, Better Combat), and shader performance.
