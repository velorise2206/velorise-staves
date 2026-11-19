# Environmental Interactions, Innate Mobs & Mutations

In **Velorise's Staves**, the Minecraft world is a living elemental sandbox. Terrain blocks, fluids, weather, redstone circuits, sculk sensors, amethyst geodes, and creatures interact with the **Elemental Gauge Theory (EGT)** automatically every tick.

---

## 🌍 Environmental Block & Weather Interactions

### 1. Water Bodies, Rain & Hydro Interactions
- **Water Pools, Rivers & Oceans:** Standing or swimming in water continuously applies a non-decaying **`1.0U` Hydro Aura** and extinguishes Burning/Pyro auras.
  - **Cryo on Water (Ice Bridging):** Any Cryo projectile, Tsaritsa Spear footstep, or Cryo-Imbued mob touching water freezes surface water blocks into temporary **Frosted Ice**.
  - **Electro on Water (Electrified Water Pools):** Striking water with any Electro attack (or when a Powered Creeper / Mutated Electro mob enters water) electrifies the water zone (`ElectrifiedWaterManager`). Crackling purple arcs jump across the water surface and between submerged mobs, dealing **`0.5❤` (`1.0F`) + `0.8U` Electro** every `1.0s` (`20t`) to trigger continuous **Electro-Charged** shocks!
- **Rainfall:** Entities standing outdoors under rain receive continuous **Hydro Aura**, enabling effortless *Vaporize, Electro-Charged, Bloom,* and *Freeze* combos.
- **Hydro Attacks on Terrain:** Hydro beams and splashes extinguish Fire blocks, unlight lit Campfires and Candles, solidify Lava into Obsidian/Cobblestone, and hydrate Farmland.

### 2. Fire, Lava, Campfires & Pyro Terrain Physics
- **Fire, Soul Fire & Lava:** Touching Fire/Lava maintains a continuous **Pyro Aura** (`1.0U` on Fire, `2.0U` on Soul Campfire & Lava).
- **Pyro Attacks on Terrain (`applyPyroEnvironmentEffects`):**
  - **Melts Ice & Snow:** Instantly melts Frosted Ice, Ice, Packed Ice (`radius >= 2.0m`), Snow layers, Snow Blocks, Powder Snow, and converts Powder Snow Cauldrons into Water Cauldrons.
  - **Ignites & Primes:** Lights unlit Campfires and Candles, primes **TNT blocks** directly into live `PrimedTnt`, and burns away **Cobwebs** and dry replaceable vegetation!
- **Amethyst Cluster Overloaded Mining (`checkPyroAmethystMining`):**
  - Just like Genshin Impact's Electro Crystals, Amethyst Clusters and Buds carry natural Electro energy. Hitting an **Amethyst Cluster** with any **Pyro attack** or **Overloaded explosion** triggers an Overloaded detonation that shatters the cluster and drops **2 to 4 Amethyst Shards**!

### 3. Tesla Coils, Lightning Rods, Redstone & Sculk Sensors (Electro)
- **Tesla Coil Structure (`Lightning Rod` placed on top of a `Redstone Block`):**
  - **Passive Mode:** Automatically zaps the nearest hostile mob within **`6.0m`** every `2.5s` (`50t`) for **`0.5❤` + `1.0U` Electro** with a visual lightning arc.
  - **Overcharged Mode (`15.0s` / `300t`):** Striking a Tesla Coil with **any Electro staff/sword** or a natural **Lightning Bolt** overcharges it for `15.0s` (unlocking the **High-Voltage Architect** advancement!). While overcharged, it zaps up to **4 targets** within **`8.0m`** every `1.0s` (`20t`) for **`1.5❤` + `2.0U` Electro**, electrifies nearby water, and triggers **Hyperbloom** on nearby Dendro Cores!
- **Standalone Lightning Rods:** When struck by lightning or an Electro attack, discharges a **`4.5m` Electro shockwave** (`1.0U` Electro) and electrifies adjacent water.
- **Sculk Sensors & Calibrated Sculk Sensors:**
  - When a **Sculk Sensor** enters its `ACTIVE` phase from sound vibrations, it immediately zaps the nearest entity within **`8.0m`** for **`1.0U` Electro** (and every `0.5s` while active).
  - An active **Calibrated Sculk Sensor** zaps within **`16.0m`** for a heavy **`2.0U` Electro**!
- **Observers & Energized Redstone:**
  - **Observer Blocks:** When triggered (`POWERED = true`), emits a **`1.0U` Electro** shock from its back output port (`0.3s` ICD).
  - **Redstone Contact:** Touching a **Redstone Block**, powered **Redstone Wire (`power > 0`)**, **Lit Redstone Ore**, **Redstone Torch**, or active **Powered Rail** applies **`1.0U` Electro** with crackling arcs!
- **Amethyst Geodes:** Touching solid **Amethyst Blocks / Budding Amethyst** or standing within `1.0m` of an **Amethyst Cluster** applies **`1.0U` Electro** every `1.0s`.

### 4. Dendro, Cryo & Geo Environmental Hazards
- **Dendro Contact:** Standing on **Slime Blocks**, **Honey Blocks**, or touching **Giant Red/Brown Mushroom Blocks** applies **`1.0U` Dendro Aura**; walking into **Cactus** or **Sweet Berry Bushes** also applies Dendro.
- **Cryo Contact:** Standing in **Powder Snow** or touching **Blue Ice / Packed Ice** applies **`1.0U` Cryo Aura**.
- **Geo Contact:** Being struck by falling **Pointed Dripstone (Stalactites)** or landing on **Stalagmites** applies **`1.0U` Geo Aura** (and shatters Frozen targets!).
- **Thorns Armor Reflection & Elemental Armor Wear:**
  - Wearing armor enchanted with **Thorns** reflects **`0.5U` per Thorns level** of the attacker's or victim's active element back onto melee attackers!
  - Elemental reactions accelerate armor durability wear on victims (configurable via `elemental_armor_durability_wear`: `"auto"`, `"true"`, `"false"`).

---

## 👾 Innate Creature Elemental Affinities

Many vanilla Minecraft mobs possess a permanent **Innate Elemental Aura** (and are **100% Immune** to damage of their own innate element while naturally reacting with opposing elements):

| Element | Innate Vanilla Creatures | Combat Tips & Natural Reactions |
| :--- | :--- | :--- |
| ![Pyro][pyro] **Pyro** | **Blaze, Magma Cube, Strider, Ghast, Zombified Piglin, Zoglin, Wither Skeleton** | Immune to Pyro & Fire. Hit with **Hydro** for `2.0×` *Forward Vaporize*, **Cryo** for `1.5×` *Reverse Melt*, or **Electro** for *Overloaded*! |
| ![Hydro][hydro] **Hydro** | **Guardian, Elder Guardian, Drowned, Axolotl** | Immune to Hydro. Hit with **Cryo** for instant *Frozen*, **Dendro** for *Bloom* cores, or **Electro** for *Electro-Charged*! |
| ![Anemo][anemo] **Anemo** | **Breeze, Phantom, Vex** | Immune to Anemo & Fall Damage (except Stalagmites). Hit with Pyro/Hydro/Electro/Cryo to trigger instant `5.0m` **Swirl**! |
| ![Electro][electro] **Electro** | **Enderman, Ender Dragon, Endermite, Shulker, Powered Creeper** | Immune to Electro. **Powered Creepers** passively radiate `1.0U` Electro within `1.0m` every `1.0s` and electrify water! Hit with **Pyro** for *Overloaded* or **Dendro** for *Quicken*. |
| ![Dendro][dendro] **Dendro** | **Bogged, Mooshroom, Bee, Cave Spider, Slime** | Immune to Dendro. Hit with **Pyro** for *Burning*, **Hydro** for *Bloom*, or **Electro** for *Quicken / Aggravate*! |
| ![Cryo][cryo] **Cryo** | **Stray, Snow Golem, Polar Bear** | Immune to Cryo. Freeze water underfoot! Hit with **Pyro** for `2.0×` *Forward Melt* or **Electro** for *Superconduct* (`-40%` Phys RES). |
| ![Geo][geo] **Geo** | **Armadillo, Iron Golem, Husk, Silverfish** | Immune to Geo. Hit with Pyro/Hydro/Electro/Cryo to spawn **Crystallize Shards** for free elemental shields! |

---

## 🧬 Innate Mob Mutations (`7%` Default) & Omni Pet Taming

When normal mobs spawn in the world, they roll against the configurable **Innate Mutation System** (`StavesConfig` `innate_mutation`):
1. **Standard Elemental Mutation (`~6%` default):**
   - The mob gains one of the **7 Elements** as an innate aura (displayed as an overhead elemental badge and in Jade/Waila tooltips), becomes immune to that element, inflicts `1.0U` of that element on melee attacks, and drops its matching **Elemental Essence** at a **15% rate** on player kill (vs `5%` for natural innate mobs).
2. **Omni-Innate Mutation (`~1%` default — `Omni ∞`):**
   - Approximately `14.3%` (`omni_mutation_share = 0.143`) of mutated mobs ascend into **Omni-Innate Creatures**, marked by a spinning iridescent **Omni Prism Badge (`Omni ∞`)**.
   - **100% Immune to All Elemental & Magic Damage:** Omni-mutated mobs take **zero damage** from staves, reactions, potions, and elemental hazards — you must defeat them using **Physical Melee or Bow attacks**!
   - **Adaptive Elemental Strikes:** When an Omni-mutated mob attacks a target that already has an elemental aura, it **automatically applies the strongest reaction-triggering element**!
   - **25% Essence Drop Rate:** Dropping an **Omni Element** or Elemental Essence on player kill.
3. **Taming Omni-Innate Mobs (`Transcendent Companion` Advancement):**
   - Right-clicking a tamable animal (such as a **Wolf** or **Fox** — whitelisted by default in `mutable_mob_whitelist`) that spawned with an **Omni-Innate Mutation** using an **Omni Element** (`omni_element`) **instantly tames it as an immortal elemental-immune companion** and unlocks the **Transcendent Companion** challenge advancement!

---

## 🧠 Mob Elemental Hazard Avoidance AI (`MobHazardAvoidanceManager`)

Non-boss pathfinding mobs feature a custom AI goal (`AvoidElementalHazardsGoal`) and danger-memory reflex:
- **Pathfinding Penalty (`WalkNodeEvaluatorMixin`):** Mobs actively steer around **Electrified Water**, **Ghastfire Fields**, **Dragonfire Clouds**, **Active Tesla Coils**, **Lit Campfires**, and **Temporary Toxic Webs** (unless they are innate-immune to that hazard's element!).
- **Unseen Sniper Evasion:** If a mob is struck by a long-range elemental reaction with no visible player nearby, it records the hazard coordinates and flees away from the blast zone for `2.0s` (`40t`).

[pyro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png
[hydro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png
[anemo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png
[electro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png
[dendro]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png
[cryo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png
[geo]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png
[omni]: https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/omni.png
