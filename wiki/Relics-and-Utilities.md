# Relics, Gliders, Elemental Rods & Advancements

Beyond the 22 combat staves, **Velorise's Staves** provides movement relics, aerial gliders, crafting essences/rods, an interactive 9-tab guidebook, and **39 custom advancements**.

---

## 🧭 Utility Relics & Aerial Equipment

### 1. Void Translocator (`void_translocator` / `netherite_void_translocator`)
- **Function:** Instant spatial blink forward (`24.0m` Basic / `36.0m` Netherite) along your look vector, safely phasing through solid walls and obstacles while resetting fall distance, applying a brief Electro self-discharge, and granting fall immunity (`5.0s` default CD, `128` Basic / `256` Netherite durability).
- **Omni Staff Integration:** Can be used in the **Electro slot** when crafting an **Omni Staff** or absorbed into an existing Omni Staff via an **Anvil**!
- **Crafting:** Shaped Crafting Table recipe (`Ender Pearl` + `Electro/Omni Element` + `Blaze Rod`); upgradeable to Netherite in a Smithing Table.

### 2. Magic Mirror (`magic_mirror` / `netherite_magic_mirror`)
- **Function:** Hold Right-Click for `2.5s` (`magic_mirror_channel_seconds`) to channel a spatial recall back to your **Respawn Anchor / Bed** or **World Spawn** (`15.0s` default CD, `64` Basic / `128` Netherite durability). Taking damage interrupts the channel.
- **Cross-Dimensional Recall:** **Netherite Magic Mirror** can teleport you home across dimensions (e.g., from the Nether or The End back to your Overworld bed!); Basic Magic Mirror is same-dimension by default (`basic_mirror_cross_dimension = false`).
- **Crafting:** Shaped Crafting Table recipe (`2 Diamonds` + `2 Glass` + `1 Ender Pearl`); upgradeable to Netherite in a Smithing Table.

### 3. Wings of First Flight — Wind Glider (`wind_glider`)
- **Function:** Carry the **Wind Glider** anywhere in your inventory (or equip it) and press **Right-Click in midair** (`enable_wind_glider = true`, default `Unbound` fallback to Use/Right-Click to avoid conflicting with Creative mode flight; rebindable via `key.velorise_staves.wind_glider`) to unfold 3D procedural **Wings of First Flight**!
  - Smoothly glides through the air with zero durability loss and complete fall-damage protection.
  - **Updraft Synergy:** Gliding into **Venti's *Skyward Sonnet* wind current** or over rising heat sources propels you high into the sky!
  - **Mid-Air Plunging Attack (`Left-Click` while gliding or falling):** Folds the glider and slams straight down (`enable_plunge_attack = true`), dealing AoE impact damage on landing and canceling all fall damage!
- **Crafting:** Shaped Crafting Table recipe (`2 Phantom Membranes` + `1 Diamond` + `4 Feathers` + `1 Anemo/Omni Element`).

### 4. Genshin-Style Stamina, Quick Dash & Wall-Climbing
- **Progressive Stamina Bar (`enable_custom_stamina = true`):**
  - Starts at **`100` Max Stamina** in the Overworld and permanently upgrades to **`170`** upon reaching the Nether milestone and **`240`** upon reaching The End milestone!
  - Consumed by **Quick Dash** (`18` Stamina), sprinting, gliding, and vertical wall-climbing; automatically regenerates after a short pause (Creative mode is exempt).
- **Quick Dash (`enable_custom_dash = true`):** Pressing `[Sprint / Ctrl]` triggers a Genshin-style forward dash impulse with brief **I-frames** to dodge incoming attacks.
- **Universal Wall-Climbing (`enable_wall_climb = true`):** Pressing forward against solid vertical walls lets you climb directly up cliffs and structures while consuming Stamina.

### 5. Elemental Codex (`elemental_codex`)
- **Function:** Automatically gifted to every player upon first joining a world (and craftable anytime via `Book + Amethyst Shard`).
- **Interactive 9-Tab Guidebook:** Covers *Overview, Elements, Reactions, Staves, Artifacts, Sword Imbuing, World & Mobs, Resonances,* and *Live Config*. Features clickable cross-reference links, visual recipe grids, and **real-time server config synchronization** across **9 languages** (`en_us`, `vi_vn`, `zh_cn`, `ja_jp`, `ko_kr`, `ru_ru`, `fr_fr`, `es_es`, `pt_br`).

---

## 💎 Elemental Essences & Elemental Rods

### 1. The 8 Elemental Essences
- **7 Standard Essences:** `pyro_element`, `hydro_element`, `anemo_element`, `electro_element`, `dendro_element`, `cryo_element`, `geo_element`.
  - **Fixed Drop Rates (Unaffected by Looting):** Dropped when a player kills a hostile mob with an innate element:
    - **5%** from Natural Innate Mobs (e.g., Blaze, Stray, Guardian, Enderman).
    - **15%** from Mutated Elemental Mobs.
    - **25%** from **Omni-Innate (`Omni ∞`) Mobs**.
- **Omni Element (`omni_element`):**
  - Crafted by combining all **7 Standard Elemental Essences** in a Crafting Table, or dropped by **Omni-Innate Mobs**.
  - Can substitute for **any** of the 7 elements in staff crafting recipes, repair any staff in an Anvil, imbue any sword, or **tame an Omni-Innate animal**!

### 2. Wandering Trader Elemental Trades & Hidden Advancement
Even in **Peaceful mode** or without farming mutated mobs, players can acquire every Elemental Essence (including **Omni Element**) by trading Emeralds with **Wandering Traders**:
- **50% Spawn Offer Chance:** Every newly spawned Wandering Trader has a **50% chance** to generate **`2–3` extra Elemental Essence trade slots** (`1` purchase max per slot).
- **7 Standard Elemental Essences (`7/8` = `87.5%` per slot):** Priced at **`24–32 Emeralds`** (rewards **`12 XP`**).
- **Omni Element (`1/8` = `12.5%` per slot, max 1 per trader):** Priced at **`48–64 Emeralds`** (rewards **`25 XP`**).
- **Hidden Advancement (`Where is he from ??`):** Purchasing any Elemental Essence or Omni Element from a Wandering Trader immediately unlocks the hidden Goal advancement **Where is he from ??** (`where_is_he_from`).

### 3. Elemental Rods & 1.20.1 Backport Recipes
- **`cryo_rod` (Cryo), `electro_rod` (Electro), `hydro_rod` (Hydro), `geo_rod` (Geo), `breeze_rod` (Anemo), `blaze_rod` (Pyro):**
  - Any rod can be transmuted into another elemental rod (shapeless recipe: **Any Rod + Target Elemental Essence ➔ Target Rod**).
  - **Wind Charge Crafting:** `1 Anemo/Omni Element ➔ 4 Wind Charges` (allowing full Wind Staff & Daybreak Chronicles crafting even on Forge 1.20.1 or peaceful worlds!).

---

## 🏆 Complete List of 39 Custom Advancements

| Advancement Title | ID | Category / Frame | Exact Unlock Condition |
| :--- | :--- | :--- | :--- |
| **Velorise's Staves** | `root` | Task | Obtain the **Elemental Codex** (automatically granted on join or crafted). |
| **Apprentice Arcanist** | `first_staff` | Task | Obtain any of the **22 Combat Staves**. |
| **Elemental Alchemist** | `first_reaction` | Task | Trigger your first **Elemental Reaction** on any entity. |
| **Crystallized Defense** | `crystallize` | Task | Pick up an **Elemental Crystallize Shard** to gain a shield. |
| **Stone Architect** | `geo_construct` | Goal | Summon a **Geo Fang Pillar** or **Stone Stele**. |
| **Geo Architect** | `geo_architect` | Challenge *(Hidden)* | Have **3 active Geo Constructs** resonating simultaneously. |
| **Bountiful Harvest** | `dendro_core` | Goal | Trigger **Hyperbloom** or **Burgeon** on a **Dendro Core**. |
| **Scholar of Elements** | `elemental_mastery` | Goal | Enchant or hold a staff with **Elemental Mastery**. |
| **Netherite Ascendant** | `netherite_upgrade` | Goal | Upgrade any staff or relic to **Netherite Tier**. |
| **Master of All Elements** | `omni_staff` | Challenge | Obtain the **Omni Staff** or **Netherite Omni Staff**. |
| **Spatial Traveler** | `spatial_traveler` | Task | Teleport using the **Void Translocator** or **Magic Mirror**. |
| **High-Voltage Architect** | `tesla_coil` | Goal | Overcharge a **Tesla Coil** (Lightning Rod on Redstone Block). |
| **Transcendent Companion** | `tame_omni_pet` | Challenge | Tame an **Omni-Innate (`Omni ∞`)** creature using an **Omni Element**. |
| **Where is he from ??** | `where_is_he_from` | Goal *(Hidden)* | Successfully trade for any **Elemental Essence** or **Omni Element** from a mysterious **Wandering Trader**. |
| **Web Developer** | `web_developer` | Goal *(Hidden)* | Trap and poison a **Spider** inside your own **Webcaster** cobweb. |
| **Take My Ball!** | `take_my_ball` | Goal *(Hidden)* | Deflect or hit a Ghast/projectiles using Anemo wind blasts. |
| **Rat From the Sky** | `rat_from_the_sky` | Challenge | Land a **Mavuika Flamestrider Plunging Attack** or **Sunfell Slice `[R]`** from high altitude. |
| **Let the People Rejoice!** | `let_the_people_rejoice` | Challenge | Reach maximum **Fanfare** during **Furina's Elemental Burst `[R]`**. |
| **Beheaded** | `beheaded` | Challenge *(Hidden)* | Shatter *Splendor of Tranquil Waters* by depleting its durability to 0. |
| **This Is Order!** | `this_is_order` | Challenge | Petrify multiple enemies with **Zhongli's *Planet Befall* `[R]`**. |
| **Eroded Stone** | `eroded_stone` | Challenge *(Hidden)* | Have a **Stone Stele** destroyed by an enemy boss/mob attack. |
| **Broken Contract** | `broken_contract` | Challenge *(Hidden)* | Shatter *Vortex Vanquisher* by depleting its durability to 0. |
| **Must Be the Storm** | `must_be_the_storm` | Challenge | Trigger **Elemental Absorption** inside **Venti's *Stormeye Vortex* `[R]`**. |
| **The Wind Has Stilled** | `the_wind_has_stilled` | Challenge *(Hidden)* | Shatter *The Daybreak Chronicles* by depleting its durability to 0. |
| **Inazuma Shines Eternal!** | `inazuma_shines_eternal` | Challenge | Unleash **Raiden Shogun's *Musou no Hitotachi* `[R]`**. |
| **Nothing Lasts Forever** | `nothing_lasts_forever` | Challenge *(Hidden)* | Shatter *Engulfing Lightning* by depleting its durability to 0. |
| **Illusory Heart** | `illusory_heart` | Challenge | Expand **Nahida's *Shrine of Maya* `[R]`** dome. |
| **A Dream Awoken** | `a_dream_awoken` | Challenge *(Hidden)* | Shatter *A Thousand Floating Dreams* by depleting its durability to 0. |
| **No Love Left for Her People** | `no_love_left` | Challenge | Unleash **The Tsaritsa's Elemental Burst `[R]`**. |
| **Shattered Regalia** | `shattered_regalia` | Challenge *(Hidden)* | Shatter *Silver Dawn* by depleting its durability to 0. |
| **Starry Night** | `starry_night` | Challenge *(Hidden)* | Fire a full 4-star salvo from **Layla's *Nights of Formal Focus***. |
| **The Wind is strong today!** | `the_wind_is_strong_today` | Challenge | Launch a mob `>= 75` blocks skyward with **The Daybreak Chronicles**. |
| **Thermal Explosion** | `hidden_pyro` | Challenge *(Hidden)* | Trigger an **Overloaded** or **Vaporize** reaction. |
| **Absolute Zero** | `hidden_cryo` | Challenge *(Hidden)* | **Freeze** or **Shatter** targets with Cryo energy. |
| **Gale Dispersion** | `hidden_anemo` | Challenge *(Hidden)* | Swirl elemental energies with **Anemo**. |
| **High Voltage** | `hidden_electro` | Challenge *(Hidden)* | Trigger an **Electro-Charged** or **Superconduct** reaction. |
| **Wild Overgrowth** | `hidden_dendro` | Challenge *(Hidden)* | Trigger a **Bloom**, **Burning**, or **Quicken** reaction. |
| **Solidification** | `hidden_geo` | Challenge *(Hidden)* | Trigger a **Crystallize** reaction to generate an elemental shield shard. |
| **Prismatic Harmony** | `hidden_omni` | Challenge *(Hidden)* | Master and unleash all **seven elements**. |
