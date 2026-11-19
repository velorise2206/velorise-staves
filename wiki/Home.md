# Velorise's Staves & Elements — Official Wiki

Welcome to the complete technical and gameplay reference manual for **Velorise's Staves & Elements** (supporting **NeoForge 1.21.1**, **Fabric 1.21.1**, and **Forge 1.20.1**).

The mod brings a faithful, zero-mana adaptation of Genshin Impact's **Elemental Gauge Theory (EGT)** and character combat kits into Minecraft:
- **22 Elemental Combat Staves** (both **Basic** and **Netherite** tiers):
  - **7 Archon Signature Weapons:** Mavuika (*A Thousand Blazing Suns*), Furina (*Splendor of Tranquil Waters*), Zhongli (*Vortex Vanquisher*), Venti (*The Daybreak Chronicles*), Raiden Shogun (*Engulfing Lightning*), Nahida (*A Thousand Floating Dreams*), and the Tsaritsa (*Silver Dawn* Bow ⇄ Spear).
  - **3 Genshin Character Signature Weapons (Omni-Eligible):** Neuvillette (*Guardian Lens*), Xilonen (*Peak Patrol Song*), and Layla (*Nights of Formal Focus*).
  - **11 Standard Elemental Staves** + **1 Master Omni Staff** (with `[Tab]` Radial Element Wheel and Auto-Best Reaction AI).
- **19 Elemental Reactions** (including **Forward/Reverse Vaporize & Melt**, **Frozen & Shatter**, **Quicken/Aggravate/Spread**, **Bloom/Hyperbloom/Burgeon**, **Overvape**, and Tsaritsa's 3 **Stellar Cryo Reactions**).
- **Dynamic World & Environmental Physics:** Electrified water pools, Tesla Coils (Lightning Rod on Redstone Block), Sculk Sensor & Observer zaps, Amethyst Cluster Overloaded mining, Slime/Honey/Mushroom Dendro contact, Dripstone Geo impacts, and AI hazard avoidance.
- **Innate Creature Elements & Mutations:** Natural mob elemental affinities, a configurable `7%` innate mutation system (`6%` Elemental + `1%` **Omni-Innate `Omni ∞`**), and **Omni Pet Taming**.
- **Relics, Gliders, Enchantments & 39 Custom Advancements:** *Void Translocator*, *Magic Mirror*, *Wings of First Flight (Wind Glider)*, *Elemental Codex* (9 languages), *Elemental Mastery I–III*, *Elemental Protection I–IV*, and *Anvil Sword Imbuing*.

---

## 📚 Wiki Navigation Directory

1. **[Staves List & Character Kits](Staves-List)**
   - Complete controls table, all **10 Genshin Character Signature Kits** (Normal/Charged/Skill/Burst/Passives/Combos), **8 3D AFK Idle Animations**, **12 Standard & Omni Staves**, and **Elemental Resonance** buffs.
2. **[Elements, Reactions & Enchantments](Elements-and-Reactions)**
   - Mathematical breakdown of **Elemental Gauge Theory (1U/2U/4U)**, Aura Decay, Internal Cooldown (ICD), all **19 Elemental Reactions** (including *Stellar Melt*, *Stellar Frozen*, and *Stellar Conduct*), and **Elemental Mastery I–III / Elemental Protection I–IV** scaling.
3. **[Environmental Interactions & Mob Mutations](Environmental-Interactions)**
   - How water, rain, fire, lava, powder snow, **Tesla Coils**, **Sculk Sensors**, **Observers**, **Redstone**, **Amethyst Clusters**, **Slime/Honey/Mushrooms**, and **Dripstone** interact with elements, plus the full **Innate Mob Table**, **Omni-Innate Mutations**, **Omni Pet Taming**, and **Mob Hazard Avoidance AI**.
4. **[Relics, Gliders, Rods & Advancements](Relics-and-Utilities)**
   - *Void Translocator*, *Magic Mirror*, *Wings of First Flight (Wind Glider & Plunge Attack)*, *Elemental Codex*, *8 Elemental Essences*, *4 Elemental Rods*, and the complete list of **39 Custom Advancements** (including hidden challenges).
5. **[Anvil Imbuing & Omni Staff Rules](Anvil-Imbuing)**
   - How to imbue any sword with an element, craft/upgrade the **Omni Staff**, swap absorbed non-Archon staves/Trident/Void Translocator, and understand the **7 Archons Restriction (`!isArchon()`)**.
6. **[Configuration Guide (TOML & In-Game GUI)](Configuration-Guide)**
   - Full reference for all server/client TOML settings (`mechanics`, `elemental_gauge`, `scaling`, `gameplay`, `audio`, `innate_mutation`, `cooldowns`, `damage`, `durability`), Co-Op dynamic mob scaling, and live server-to-client Codex sync.
7. **[FAQ, Keybinds, Resource Packs & Compatibility](FAQ-and-Compatibility)**
   - Frequently asked questions, full keybind list, **Resource Pack Guide (Custom Wind Glider Textures & Character Voice Packs)**, and compatibility details for **JEI/REI/EMI**, **Jade/Waila**, **Iris/Oculus Shaders**, **Sodium/Embeddium**, and **First-Person Model** mods.


---

## Reworked & Upgraded Non-Archon Staves (Progression & Combat Update)

- **Electro Rod (`velorise_staves:electro_rod`) & Cryo Rod (`velorise_staves:cryo_rod`)**: Custom Elemental Rods (alongside `hydro_rod`, `geo_rod`, vanilla `blaze_rod`, and `breeze_rod`) with bidirectional rod conversion recipes (`blaze`, `breeze`, `cryo`, `geo`, `hydro` ⇄ `electro_rod`), recolored directly on the classic vanilla Blaze Rod shape.
- **Updated Archon Progression Recipes**:
  - **Nahida (`thousand_floating_dreams`)**: Slot 5 requires `#velorise_staves:dendro_staves` (non-Archon Dendro staff).
  - **Raiden Shogun (`engulfing_lightning`)**: Slots 2, 3, 4 = `electro_element` / `omni_element`; Slots 5, 7 = `#velorise_staves:electro_staves` (non-Archon Electro staves).
  - **Zhongli (`vortex_vanquisher`)**: Slot 7 requires `#velorise_staves:geo_staves` (non-Archon Geo staff).
  - **Venti (`the_daybreak_chronicles`)**: Slots 2, 4 require `#velorise_staves:anemo_staves` (non-Archon Anemo staves).
  - **Tsaritsa (`tsaritsa_bow`)**: Slots 2, 4 require `#velorise_staves:cryo_staves` (non-Archon Cryo staves).
  - **Omni Staff (`omni_staff`)**: Strictly restricted to 7 non-Archon staves (`!isArchon()`).
- **Dragonfire Scepter (`DRAGONFIRE` — Non-Archon, Omni-Eligible)**:
  - **Left-Click (Normal Attack, `4.0s` CD)**: Fires the Ender Dragon plasma orb (`3.5❤` Basic / `5.25❤` Netherite Electro AoE + `6s` Dragonfire Cloud).
  - **Right-Click (`E` Skill, No CD / `0.25s` anti-spam)**: Toggles a `1.0-block` **Mini Ender Dragon Pet** (`30s` duration) that smoothly patrols around the player, teleports if `> 18m` away, **auto-switches targets** to whoever the player attacks, and **hovers still while firing** every `2.0s` (`1.0❤` Basic / `1.5❤` Netherite, `1.0U` Electro).
  - **`[R]` Elemental Burst (`20.0s` CD, `-10%` with Anemo Resonance)**:
    - *Variant 1 (Pet Active — Skyfall Dive)*: Grants `2.0s` I-frames; Mini Dragon shoots straight into the sky and vanishes; `2.0s` later, a **Giant Ender Dragon** crashes down `1 block` ahead of the player in a **`5×5 blocks`** AoE (`8.0❤` Basic / `12.0❤` Netherite, `4.0U` Electro + Dragonfire Cloud).
    - *Variant 2 (No Pet — Sweeping Strafe)*: Grants `1.8s` I-frames; a **Giant Ender Dragon** swoops in a curved arc skimming a **`3m wide × 10m long`** corridor along the player's look direction (`6.0❤` Basic / `9.0❤` Netherite, `3.0U` Electro).
  - **3D Idle Animation**: After standing still for `3.0s`, the Mini Ender Dragon flies freely around the player and periodically hovers directly in front of the player's face looking at its master.
  - **Hidden Passive (*Dragon-Wing Glider*)**: Carrying `Dragonfire Scepter` (or `Omni Staff` with `Dragonfire Scepter` absorbed) transforms the Wind Glider into a **3D Ender Dragon (`5.0-block` horizontal wingspan)** gripping the player's shoulders and boosts gliding horizontal speed by **+50%**.
- **Resonant Echo (`ECHO`)**:
  - **Resonance Buff**: Retains the `8.0s` default cooldown, but each valid non-dummy enemy hit that loses HP reduces the cooldown by **`1.0s`** (up to **`5.0s`** max across 5 targets).
- **Inferno Rod (`INFERNO`) & Frostbolt Wand (`FROST`)**:
  - **3rd-Shot Finale**: The 3rd shot in each 3-shot burst flies **+35% faster**, deals **+50% damage**, applies **`2.0U`** (`3.0U` Netherite), and triggers a **`2.5m` AoE Pyro explosion** (*Inferno Rod*) or **`2.5m` Cryo Permafrost Nova + Slowness IV** (*Frostbolt Wand*).
- **Wither Scepter (`WITHER`)**:
  - **Wither Carapace (`<= 50% Max HP`)**: Holding `Wither Scepter` in main hand or offhand while at or below `50%` HP grants **100% immunity to all incoming projectiles**.
  - **Withering Contagion**: Enemies marked by `Wither Scepter` that die within `8.0s` detonate in a **`3.0m` Wither + Dendro cloud** (`1.5❤`➔`2.25❤`, `1.5U` Dendro + Wither II `80t`).
- **Shulker Seeker (`SHULKER`)**:
  - **Left-Click Attack & 3rd-Shot Gravity Slam**: Fires a 3-shot homing Shulker bullet salvo; the 3rd shot launches the target upward (`+0.78Y`) and `0.5s` later **slams them violently into the ground (`-1.45Y`)** for **`2.5m` AoE Electro damage** (`2.5❤`➔`3.75❤`, `2.0U` Electro).
  - **Right-Click Portable Shulker Chest & Shell Guard**: Opens an integrated **27-slot** portable storage chest (`54` scrollable slots when upgraded to **Netherite**, keeping a compact 3-row UI with scrollbar). While the storage chest is open, incoming damage is reduced by **-30%** (**-40%** on Netherite).
