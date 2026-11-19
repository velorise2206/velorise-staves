# Elements, Reactions & Enchantments (Elemental Gauge Theory)

**Velorise's Staves** implements a mathematical adaptation of Genshin Impact's **Elemental Gauge Theory (EGT)**, including Gauge Unit decay, Aura Tax, Simultaneous Reaction Priority, Internal Cooldowns (ICD), and **19 distinct Elemental Reactions**.

---

## 📐 Elemental Gauge Theory (EGT) Fundamentals

### 1. Gauge Units (`U`) & Linear Decay Formula
Every elemental application applies a specific amount of **Gauge Units (`U`)**:
- **`1.0U` (Standard):** Basic staff projectiles, full-charge Imbued Sword swings, and ambient environmental contact (`9.5s` duration).
- **`1.5U`–`2.0U` (Medium):** Netherite staff attacks, Archon Elemental Skills, and Calibrated Sculk Sensor zaps (`10.75s`–`12.0s` duration).
- **`3.0U`–`4.0U` (Heavy):** Archon Elemental Bursts (`[R]`) and Netherite Omni Orbs (`14.5s`–`17.0s` duration).

When an element establishes an aura on a target, its duration follows the linear decay law:
$$\text{Duration (seconds)} = 2.5 \times U + 7.0$$

- **Decay Rate Inheritance:** If a weaker application refreshes an existing aura of the same element, the gauge units update to $\max(U_{\text{current}}, 0.8 \times U_{\text{applied}})$ while preserving the original decay rate.
- **Innate Lock:** Mobs with a natural or mutated **Innate Element** have a permanent self-aura that never decays naturally.

### 2. Internal Cooldown (ICD)
- **Standard ICD:** Most multi-hit attacks follow the **2.5-second (`50t`) or 3-hit rule** (every 1st, 4th, 7th hit within `2.5s` applies elemental gauge).
- **Bypass / Custom ICD:** Dedicated bursts, environmental shocks (Observers, Sculk Sensors, Lightning Rods), Swirl chain spreads, and Nahida's *Tri-Karma Purification* bypass standard ICD or use independent reaction timers.

---

## ![Pyro][pyro] Amplifying Reactions (Multiplicative Damage)

Amplifying reactions multiply the **triggering attack's direct damage** instead of dealing a separate damage instance:

| Reaction | Trigger ➔ Existing Aura | Base Multiplier | Aura Consumption | Notes & Synergy |
| :--- | :--- | :--- | :--- | :--- |
| **Forward Vaporize** | ![Hydro][hydro] **Hydro** on ![Pyro][pyro] **Pyro** | **2.0× DMG** | `2.0 × U` | Strong side. Consumes Pyro aura rapidly; ideal for Neuvillette burst waves or Guardian Lens hits. |
| **Reverse Vaporize** | ![Pyro][pyro] **Pyro** on ![Hydro][hydro] **Hydro** | **1.5× DMG** | `0.5 × U` | Weak side. Preserves Hydro aura for multiple consecutive Pyro hits (Mavuika bike / Inferno Rod). |
| **Forward Melt** | ![Pyro][pyro] **Pyro** on ![Cryo][cryo] **Cryo** | **2.0× DMG** | `2.0 × U` | Strong side. Massive single-hit nuke for Mavuika's *Sunfell Slice* `[R]` or *Ghastfire Staff*. |
| **Reverse Melt** | ![Cryo][cryo] **Cryo** on ![Pyro][pyro] **Pyro** | **1.5× DMG** | `0.5 × U` | Weak side. Allows all 3 icicles of *Frostbolt Wand* or *Tsaritsa* hits to Melt a single Pyro aura. |

---

## ![Electro][electro] Transformative & Crowd-Control Reactions

| Reaction | Combination | Combat Effect, Damage & Crowd Control |
| :--- | :--- | :--- |
| **Overloaded** | ![Pyro][pyro] **Pyro** + ![Electro][electro] **Electro** | Detonates a fiery shockwave (`3.0m` AoE) dealing **AoE Pyro DMG**, knocking enemies backward, shattering Geo shields, and **mining Amethyst Clusters into 2–4 Amethyst Shards**! |
| **Superconduct** | ![Cryo][cryo] **Cryo** + ![Electro][electro] **Electro** | Releases an icy shockwave (`3.5m` AoE) dealing **AoE Cryo DMG** and shredding all hit targets' **Physical Resistance by -40% for 12.0s** (`240t`). |
| **Electro-Charged** | ![Hydro][hydro] **Hydro** + ![Electro][electro] **Electro** | **Dual-Aura Coexistence!** Both Hydro and Electro remain active simultaneously, shocking the target every `1.0s` (`20t`), interrupting actions, and arcing lightning to nearby wet targets. |
| **Overvape** | ![Pyro][pyro] **Pyro** on (![Hydro][hydro] **Hydro** + ![Electro][electro] **Electro**) | Striking an **Electro-Charged** target with Pyro triggers **Overloaded AND Reverse Vaporize simultaneously** in a single hit! |
| **Frozen** | ![Hydro][hydro] **Hydro** + ![Cryo][cryo] **Cryo** | Encases the target in a 3D translucent **Ice Statue**! Immobilizes mobs (`NoAI` pose lock) and locks player camera rotation and attack inputs (`struggle with [Space] to thaw faster`). Underlying Hydro/Cryo gauges can still react! |
| **Shatter** | ![Geo][geo] **Geo** / Heavy Hit on **Frozen** | Striking a Frozen target with any **Geo attack**, **Claymore** (*A Thousand Blazing Suns*), or **Plunging Attack** shatters the ice statue for **heavy bonus Physical/Geo damage**! |
| **Swirl** | ![Anemo][anemo] **Anemo** + (![Pyro][pyro] / ![Hydro][hydro] / ![Electro][electro] / ![Cryo][cryo]) | Absorbs the target's element and blasts a **`5.0m` AoE Swirl wave**, dealing Anemo damage to the primary target and spreading `1.0U` + elemental damage of the swirled element to all surrounding enemies! |
| **Crystallize** | ![Geo][geo] **Geo** + (![Pyro][pyro] / ![Hydro][hydro] / ![Electro][electro] / ![Cryo][cryo]) | Spawns a glowing 3D **Elemental Crystal Shard** on the ground (`15s` lifetime). Walking near it picks it up, granting an **Elemental Shield** (`15s` duration, **250% absorption** against the matching element, displayed on HUD & inventory). |
| **Burning** | ![Pyro][pyro] **Pyro** + ![Dendro][dendro] **Dendro** | Ignites a self-sustaining **`2.0U` Burning Aura** that deals rapid Pyro damage ticks every `0.25s` (`5t`) and continually refreshes Pyro for Reverse Melt / Forward Vaporize setups! |

---

## ![Dendro][dendro] Catalyze & Dendro Core Reactions

| Reaction | Combination | Mechanics & Scaling |
| :--- | :--- | :--- |
| **Quicken** | ![Dendro][dendro] **Dendro** + ![Electro][electro] **Electro** | Consumes both gauges to establish an underlying **Quicken Aura** (`8.0s–12.0s` crackling green-gold vines) without consuming future Dendro or Electro applications. |
| **Aggravate** | ![Electro][electro] **Electro** on **Quicken** | Adds a **Flat Electro Damage Bonus** (`+1.75❤` base, scaled by Elemental Mastery) directly into the triggering Electro hit while keeping the Quicken aura intact! |
| **Spread** | ![Dendro][dendro] **Dendro** on **Quicken** | Adds a **Flat Dendro Damage Bonus** (`+2.0❤` base, scaled by Elemental Mastery) directly into the triggering Dendro hit while keeping the Quicken aura intact! |
| **Bloom** | ![Hydro][hydro] **Hydro** + ![Dendro][dendro] **Dendro** | Spawns a physical 3D **Dendro Core (Bountiful Seed)** on the ground (max `5` active cores; spawning a 6th instantly detonates the oldest). If untouched for `6.0s` (`120t`), bursts for `3.5m` AoE Dendro DMG. |
| **Hyperbloom** | ![Electro][electro] **Electro** on **Dendro Core** | Instantly transforms the Dendro Core into a high-velocity **Homing Sprawling Shot** that tracks down the nearest hostile entity within `12.0m` for high single-target Dendro damage (`1.5×` Bloom DMG)! |
| **Burgeon** | ![Pyro][pyro] **Pyro** on **Dendro Core** | Instantly detonates the Dendro Core into a violent **`5.0m` AoE Dendro Explosion** (`1.5×` Bloom DMG) with zero delay! |

---

## 👑 Exclusive Stellar Cryo Reactions (Tsaritsa *Azure Crown*)

When the player has the **Azure Crown** buff active (granted by **The Tsaritsa's Support Burst `[R]`** in Bow Stance), all Cryo-related reactions executed by the player automatically upgrade into **Stellar Reactions** (each trigger also shortens the Tsaritsa's Spear Stance seal by `2.0s`):

| Stellar Reaction | Formula | Upgraded Mechanics & Special Effects |
| :--- | :--- | :--- |
| **Stellar Melt** | ![Pyro][pyro] **Pyro** + ![Cryo][cryo] **Stellar Cryo** | Amplifies triggering hit damage by **2.5×** (Forward) / **1.85×** (Reverse) AND spawns **3 Homing Frostlotus Blades** that seek out nearby enemies for bonus Cryo damage! |
| **Stellar Frozen** | ![Hydro][hydro] **Hydro** + ![Cryo][cryo] **Stellar Cryo** | Encases the target in **Permafrost Ice** (**+50% Freeze duration**) and shreds the victim's **Cryo & Hydro Resistance by -25%** while frozen! |
| **Stellar Conduct** | ![Electro][electro] **Electro** + ![Cryo][cryo] **Stellar Cryo** | Unleashes a **`4.5m` Arctic Lightning Nova** dealing `1.6×` Superconduct AoE Cryo damage, chaining frost arcs across up to 4 targets, and deploying a **`6.0m` Polestar Field** (`10.0s`, up to 4 stacks granting **+10% Crit Rate & +14% Base DMG per stack** plus **-40% Physical RES** / `1.40×` Physical DMG)! |

---

## 📖 Custom Enchantments (`Elemental Mastery` & `Elemental Protection`)

Both custom enchantments are available in the **Enchanting Table**, **Villager Trading**, **Loot Chests**, and **Anvil**:

### 1. Elemental Mastery I–III (`velorise_staves:elemental_mastery`)
- **Applicable Items:** All **22 Combat Staves** (Basic & Netherite, including all 7 Archon weapons and the Omni Staff).
- **Scaling per Level (`EM Level` = Enchantment Level + Dendro Resonance + Nahida Shrine of Maya):**
  - **Amplifying & Catalyze Reactions** (*Vaporize, Melt, Stellar Melt, Aggravate, Spread*): **+15% reaction bonus per EM level** (`+15% / +30% / +45%` at Levels I–III, stacking further with Dendro Resonance & Nahida Burst).
  - **Transformative & Dendro Core Reactions** (*Overloaded, Superconduct, Electro-Charged, Swirl, Burning, Shatter, Bloom, Hyperbloom, Burgeon, Stellar Conduct*): **+25% reaction damage per EM level** (`+25% / +50% / +75%` at Levels I–III).
  - **Crystallize Shields:** **+20% Shield HP per EM level** (`+20% / +40% / +60%` at Levels I–III).

### 2. Elemental Protection I–IV (`velorise_staves:elemental_protection`)
- **Applicable Items:** All **Armor pieces** (`Helmet, Chestplate, Leggings, Boots`).
- **Effect:** Reduces all incoming **Elemental Skill, Reaction, and Environmental Elemental Damage** by **-6% per level per piece** (up to **-80% cap** across a full set of Level IV armor).

[pyro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png
[hydro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png
[anemo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png
[electro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png
[dendro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png
[cryo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png
[geo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png
[omni]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/omni.png
