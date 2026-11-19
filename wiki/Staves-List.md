# Staves List, Character Kits & Resonances (22 Combat Weapons)

Every combat staff in **Velorise's Staves** is available in **Basic** and **Netherite** tiers. Upgrading a staff in a **Smithing Table** (`Netherite Upgrade Smithing Template` + `Basic Staff` + `Netherite Ingot`) increases **Base Damage, HP Scaling, Elemental Gauge Units (U), and Effect Durations by +50% (1.5×)** and expands **Range / AoE Radius**, while keeping Cooldowns unchanged.

---

## 🎮 Controls & Keybind Mapping (Minecraft ⇄ Genshin Impact)

| Action | Default Minecraft Input | Genshin Equivalent | Technical Behavior & Notes |
| :--- | :--- | :--- | :--- |
| **Elemental Skill (Tap E)** | **Right-Click** (Tap) | **Tap E** | Casts the weapon's primary Elemental Skill. Works from main hand or offhand (when main hand is not another Genshin weapon). |
| **Elemental Skill (Hold E)** | **Hold Right-Click** | **Hold E** | Used by **Zhongli** (`1.5s` Jade Shield + Stele), **Nahida** (`Camera Viewfinder` mark/harvest), and **Mavuika** (mount Flamestrider). |
| **Normal Attack (NA)** | **Left-Click** (Tap) | **Normal Attack** | Custom multi-step combos for **Mavuika** (5-hit bike), **Raiden** (5-hit Musou Tachi), **Nahida** (4-step hopscotch), **Tsaritsa** (Bow shot / 3-step Spear cleave), **Venti** (quick wind shot), and **Xilonen** (Roller Skate kicks). |
| **Charged Attack (CA)** | **Hold Left-Click** | **Charged Attack** | **Neuvillette** (`Equitable Judgment` Hydro Beam), **Furina** (`Ousia ⇄ Pneuma` Arkhe switch), **Mavuika** (Donut Spin + Finisher), **Raiden** (Crescent upward slash), **Nahida** (`3×3×3m` Dendro cube), **Venti** & **Tsaritsa** (3-stage Bow pull `_pulling_0/1/2`). |
| **Elemental Burst (Q)** | **`[R]` Key** | **Elemental Burst [Q]** | Unleashes the signature Ultimate ability with **1.5s–2.5s Invulnerability Frames (I-frames)** and burst root/camera choreography. Supports main hand and offhand! |
| **Combat Dash / Dodge** | **`[Sprint / Ctrl]`** | **Dash / Dodge** | While riding **Mavuika's Flamestrider** or skating in **Xilonen's Nightsoul state**, performs a 2-dash chain (`5t` gap between Dash 1 & Dash 2, then `1.5s` / `30t` cooldown). |
| **Omni Element Wheel** | **`[Tab]` Key** (Hold/Tap) | **Element Switcher** | Opens the radial **Omni Wheel** to lock onto any of the 7 elements or `Auto Best` mode (`0`). |
| **Character Voice Menu** | **`[V]` Key** | **Voice-Over Profile** | Opens the **Character Voice Screen** (`18` Zhongli voice lines in EN/ZH + voice language selector). |
| **Wind Glider** | **`[Space]` in midair** *(while falling)* | **Wings of First Flight** | Deploys the Wind Glider while falling (`enable_wind_glider`). Press `[Space]` again or land to fold. Carrying **Dragonfire Scepter** upgrades it to a **5m Ender Dragon Glider** (**+50%** horizontal speed). |
| **Plunging Attack** | **`Left-Click` in midair** *(rebindable)* | **Plunging Attack** | Dives straight down (`enable_plunge_attack`), dealing AoE impact damage and **canceling 100% of fall damage**. |

---

## 🪄 Basic & Boss Combat Staves & Master Omni Staff (12 Weapons)

All 11 standard elemental staves below have `isArchon() == false` and **can be combined or absorbed into the Omni Staff**!

| Element | Staff Name & ID | Config Base DMG (Basic ➔ Netherite) | Cooldown | Durability (Basic ➔ Neth) | Range & Full Combat Mechanics |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Pyro** | **Inferno Rod**<br>`inferno_rod` | `3.0` (`1.5❤ × 3`) ➔ `4.5` (`2.25❤ × 3`) | `4.0s` | `150` ➔ `550` | `30m`. Fires a 3-shot burst of blazing fireballs (`11t` interval). **3rd-Shot Finale:** `+35%` projectile speed, `+50%` damage, `2.0U➔3.0U` Pyro, and a **`2.5m` AoE Pyro explosion**! Melts ice/snow, lights campfires/candles, and primes TNT. |
| **Pyro** | **Ghastfire Staff**<br>`ghastfire_staff` | `6.0` (`3.0❤`) ➔ `9.0` (`4.5❤`) | `5.0s` | `120` ➔ `450` | `36m`. Launches a high-velocity Ghast fireball that detonates on impact (`3.5m➔4.5m` AoE) and leaves a **Ghastfire Field** (`5.0s`) dealing continuous Pyro ticks and triggering **Amethyst Cluster Overloaded Mining**. |
| **Dendro** | **Wither Scepter**<br>`wither_scepter` | `4.0` (`2.0❤ × 3`) ➔ `6.0` (`3.0❤ × 3`) | `6.0s` | `140` ➔ `500` | `32m`. Fires 3 decaying Dendro skulls inflicting **Wither II** (`4.0s`) and `1.0U➔1.5U` Dendro. **Wither Carapace:** `<= 50%` HP grants **100% projectile immunity**. **Withering Contagion:** Marked foes that die within `8s` explode in a **`3.0m` Dendro + Wither II cloud** (`1.5❤➔2.25❤`, `1.5U`). |
| **Electro** | **Shulker Seeker**<br>`shulker_seeker` | `6.0` (`3.0❤ × 3`) ➔ `9.0` (`4.5❤ × 3`) | `6.0s` | `130` ➔ `500` | `30m`. **Left-Click:** 3-shot homing salvo with **3rd-Shot Gravity Slam** (`+0.78Y` launch, `0.5s` later `-1.45Y` ground slam for `2.5m` AoE Electro). **Right-Click:** Portable Shulker Chest (`27` slots Basic / `54` scrollable slots Netherite); **Shulker Shell Guard** grants **-30%/-40% DMG reduction** while open. |
| **Electro** | **Dragonfire Scepter**<br>`dragonfire_scepter` | `7.0` (`3.5❤`) ➔ `10.5` (`5.25❤`) | `4.0s` (NA) / `0s` (E, `30s`) / `20.0s` `[R]` | `130` ➔ `500` | `32m`. **Left-Click (`4.0s` CD):** Fires Ender Plasma Orb (`3.5❤➔5.25❤` Electro AoE + `6s` Dragonfire Cloud). **Right-Click (No CD, `0.25s` anti-spam):** Toggles a `1m` **Mini Ender Dragon** (`30s`) that patrols, **auto-switches targets** to whoever you hit, and **hovers still** while firing every `2.0s` (`1.0❤➔1.5❤`, `1.0U`). **`[R]` (`20.0s` CD):** With Pet active, **Skyfall Dive** (`2.2s` I-frames, Giant Dragon `10×10m` ➔ `12×12m` slam, `8.0❤➔12.0❤`, `3.5U➔4.0U`); without Pet, **Sweeping Strafe** (`1.6s` I-frames, `5×20m` ➔ `6×24m` corridor, `6.0❤➔9.0❤`, `3.0U➔3.5U`). **Passive:** Upgrades Wind Glider to **5m Ender Dragon** (`+50%` speed). |
| **Geo** | **Evoker Fang Staff**<br>`evoker_fang_staff` | `3.0` (`1.5❤ × 9`) ➔ `4.5` (`2.25❤ × 13`) | `5.0s` | `120` ➔ `450` | `13.5m➔19.5m`. Erupts a line of Geo Evoker Fangs (`1.0U➔1.5U` Geo) and raises a physical **Geo Fang Pillar** (`1×1×1` Basic / `1×2×1` Netherite, `1×/2×` owner Max HP, `30s` duration, max 3 constructs) that blocks projectiles, taunts hit mobs, and pulses square Geo waves every `2.0s`. |
| **Electro** | **Arc Caster**<br>`arc_caster` | `1.0` (`0.5❤`/hit) ➔ `1.5` (`0.75❤`/hit) | `6.0s` | `130` ➔ `500` | `30m➔36m`. Instant Electro lightning strike that chains across wet/conductive targets (`6.0m` Netherite), charges Creepers (`arc_caster_charge_creeper`), overcharges **Tesla Coils**, and electrifies water pools. |
| **Cryo** | **Frostbolt Wand**<br>`frostbolt_wand` | `4.5` (`2.25❤ × 3`) ➔ `6.75` (`3.38❤ × 3`) | `4.0s` | `150` ➔ `500` | `28m`. Fires a 3-shot burst of cryogenic icicles inflicting **Slowness II**, `1.0U➔1.5U` Cryo, and freezing water. **3rd-Shot Finale:** `+35%` speed, `+50%` damage, `2.0U➔3.0U` Cryo, and **`2.5m` Cryo Permafrost Nova + Slowness IV**! |
| **Dendro** | **Webcaster**<br>`webcaster` | `1.5` (`0.75❤ × 3`) ➔ `2.25` (`1.13❤ × 3`) | `5.0s` | `140` ➔ `500` | `28m`. Fires a 3-shot burst of venomous silk orbs that trap victims in **Temporary Toxic Cobwebs** (`5.0s` default via `temporary_web_duration_ticks`, auto-restores blocks), inflicting **Poison** and `1.0U➔1.5U` Dendro. |
| **Anemo** | **Wind Staff**<br>`wind_staff` | `1.5` (`0.75❤`) ➔ `2.25` (`1.13❤`) | `1.5s` | `140` ➔ `500` | `28m`. Fires a high-speed Wind Charge (`3.5m➔4.5m` Swirl AoE + strong knockback). Firing at your feet propels you into a high **Wind-Jump** with fall-damage protection! |
| **Anemo** | **Resonant Echo**<br>`resonant_echo` | `11.0` (`5.5❤`) ➔ `16.5` (`8.25❤`) | `8.0s` *(up to `-5.0s`)* | `130` ➔ `500` | `28m➔32m`. Channels a Warden **Sonic Boom** that pierces walls and shields (`2.0U➔3.0U` Swirl). **Resonance Passive:** Each valid non-dummy hostile target damaged reduces cooldown by **`1.0s`** (up to **`-5.0s`** max across 5 targets). |
| **Omni** | **Omni Staff**<br>`omni_staff` | `10.0` (`5.0❤` / `2U`) ➔ `15.0` (`7.5❤` / `3U`) | `6.0s` | Sum of 7 Staves (`250` ➔ `900` fallback) | `36m➔48m`. Holds 1 non-Archon weapon per element! Hold `[Tab]` to open the **Omni Wheel**:<br>• **Mode 0 (`OMNI` Auto-Best):** Fires an iridescent **Omni Orb** that inspects the target's active aura on impact and **automatically triggers the highest-priority reaction**!<br>• **Modes 1–7 (Locked Element):** Delegates directly to the absorbed staff for that element (or fires a pure elemental Omni Orb when sneaking), including **Riptide Trident throw** (Hydro) or **Spatial Blink** (Electro Void Translocator)! |

---

## ⭐ Genshin Weapons (3 Weapons — Omni-Eligible)

### 1. Neuvillette — *Guardian Lens (Tome of the Eternal Flow)* (`guardian_lens`)
- **Element:** 💧 Hydro | **Role:** Hydro Dragon Sovereign Catalyst (HP-Scaled Hydro Beam & Droplet Sustainer) | **Archon:** No (`isArchon = false` — **Omni Staff Eligible!**)
- **Config Base Damage:** `2.0` (Scales with **Max HP**) | **Skill CD:** `12.0s` | **Burst CD:** `18.0s` | **Durability:** `260` Basic / `520` Netherite
- **Elemental Skill — *O Tears, I Shall Repay* (`Right-Click`, `12.0s` CD):**
  - Summons a **Surging Waterfall** at the target (`20.0m` Basic / `26.0m` Netherite range, `3.5m` AoE) dealing Hydro damage (`1.5❤ + 5% Max HP`) and spawning **3 Sourcewater Droplets** on the ground (`15s` lifetime).
- **Charged Attack — *Equitable Judgment* (`Hold Left-Click`):**
  - Channels an Empowerment Ring (up to `3.0s` / `60t` charge). Automatically absorbs up to **3 Sourcewater Droplets** within `12.0m` — each absorbed droplet **reduces charge time by `1.0s` (`20t`)** (3 droplets = **instant `0.0s` beam fire!**) and **heals Neuvillette for `16% Max HP`**.
  - Fires a continuous `3.0s` (`60t`) **Torrential Hydro Cannon** (`24.0m` Basic / `28.0m` Netherite range, `1.2m➔1.35m` beam radius) hitting all enemies in line 8 times (`every 8t`).
  - While charging or firing, grants **Water Walking** (unless sneaking) and consumes `8% Max HP` every `0.5s` only while HP is above `50%` (triggering Furina's Fanfare and Neuvillette's HP-fluctuation buff).
  - **Passives — *Heir to the Ancient Sea's Authority* & *Discipline of the Supreme Arbitration*:**
    - Triggering Hydro reactions grants up to **3 Past Draconic Glories stacks** (`30s` each), multiplying *Equitable Judgment* beam damage by **1.10× / 1.25× / 1.60×**!
    - Gains up to **+30% Hydro DMG Bonus** based on current HP above `30% Max HP` ( reaching max `+30%` at `>= 80% HP`).
- **Elemental Burst — *O Tides, I Have Returned* (`[R]` Key, `18.0s` CD):**
  - Grants `1.8s` (`36t`) I-frames, unleashes a `7.0m➔9.0m` crashing Hydro wave (`2.0❤ + 7% Max HP`) followed by 2 ascending waterfall waves, and immediately spawns **6 Sourcewater Droplets** around the player (enough for **2 consecutive instant Charged Beams**!).

---

### 2. Xilonen — *Peak Patrol Song* (`peak_patrol_song`)
- **Element:** 🪨 Geo | **Role:** Natlan Roller-Skating Sword (Multi-Element RES Shredder, Healer & Nightsoul Battery) | **Archon:** No (`isArchon = false` — **Omni Staff Eligible!**)
- **Config Base Damage:** `1.0` | **Skill CD:** `7.0s` | **Burst CD:** `15.0s` | **Durability:** `380` Basic / `760` Netherite
- **Elemental Skill — *Yohual's Scratch* (`Right-Click`, `7.0s` CD):**
  - Enters **Nightsoul's Blessing** (`9.0s` max duration, `45` initial Nightsoul points): equips **Roller Skates** (`+35%` movement speed, step-up assist, and **smooth vertical Wall-Climbing**!).
  - **Skate Dash (`[Sprint]` while skating):** Performs a forward roller-lunge dealing Geo damage (`2-dash chain`: `5t` gap between Dash 1 & 2, then `1.5s` / `30t` cooldown). Consuming Nightsoul points directly feeds **Fighting Spirit** to Mavuika!
  - **Normal Attack Kicks & *Source Samples* (`Left-Click` while skating):**
    - Each Roller Skate kick hits enemies in front and grants **+35 Nightsoul points**.
    - Reaching **90 Nightsoul points** (2 kicks!) consumes all Nightsoul to activate **3 Source Samples** for `15.0s` (`300t`), accompanied by a glowing holographic Natlan **DJ Turntable ring**:
      - Inspects your inventory for **Pyro, Hydro, Cryo, and Electro** staves to convert up to 3 Geo samples into those elements.
      - **Support Mode (`>= 2` converted Pyro/Hydro/Cryo/Electro samples):** Shreds nearby enemies' corresponding Elemental & Geo Resistance by **-36%** (`-45%` Netherite) within `8.0m`!
      - **Geo DPS Mode (`< 2` converted samples):** Grants **+30% Geo DMG Bonus** while skating.
- **Elemental Burst — *Ocelotlicue Point!* (`[R]` Key, `15.0s` CD):**
  - Grants `1.5s` (`30t`) I-frames and drops a high-decibel Natlan beat (`6.0m` AoE Geo damage).
  - **Support Mode (`>= 2` converted samples):** Pulses **8 rhythmic healing waves** over `12.0s` (`1.5s` interval), restoring HP to the player and nearby allies within `8.0m`.
  - **Geo DPS Mode (`< 2` converted samples):** Unleashes **2 additional follow-up Geo shockwaves**!

---

### 3. Layla — *Nights of Formal Focus* (`nights_focus_staff`)
- **Element:** ❄️ Cryo | **Role:** Rtawahist Astrologer Scepter (HP-Scaled Cryo Shield & Homing Starlight Battery) | **Archon:** No (`isArchon = false` — **Omni Staff Eligible!**)
- **Config Base Damage:** `4.0` | **Skill CD:** `12.0s` | **Burst CD:** `12.0s` | **Durability:** `260` Basic / `520` Netherite
- **Elemental Skill — *Nights of Formal Focus* (`Right-Click`, `12.0s` CD):**
  - Deals AoE Cryo damage (`1.0❤` Basic / `1.5❤` Netherite) and wraps the player in the **Curtain of Slumber Shield** for `12.0s`:
    - Shield capacity equals **40% Max HP** (Basic) / **60% Max HP** (Netherite), with **250% absorption efficiency** against Cryo damage.
    - Passively generates **1 Night Star** every `1.5s`, plus **+2 Night Stars** whenever you cast any staff skill (up to 4 stars orbiting the shield).
    - Upon reaching **4 Night Stars**, automatically fires a salvo of **4 Homing Shooting Stars** that track down nearby enemies for Cryo damage (`0.75❤➔1.13❤` each) and apply Cryo aura!
- **Elemental Burst — *Dream of the Star-Stream Shaker* (`[R]` Key, `12.0s` CD):**
  - Grants `1.5s` (`30t`) I-frames and summons a hovering **Celestial Dreamsphere** above the player for `12.0s`.
  - Fires homing **Starlight Slugs** at enemies within `12.0m` every `1.5s` while simultaneously generating **+1 Night Star** for Layla's shield on each shot!

---

## 👑 The Seven Archons Signature Weapons (7 Weapons)

> [!IMPORTANT]
> **Omni Staff Eligibility Rule (`isArchon()`):**
> - The **7 Archon Signature Weapons** (`Mavuika`, `Furina`, `Zhongli`, `Venti`, `Raiden Shogun`, `Nahida`, `Tsaritsa`) have `isArchon() == true` and **CANNOT** be crafted or absorbed into the **Omni Staff**.
> - The **3 Non-Archon Genshin Character Weapons** — **Neuvillette** (`Guardian Lens`), **Xilonen** (`Peak Patrol Song`), and **Layla** (`Nights of Formal Focus`) — have `isArchon() == false` and **CAN** be crafted or absorbed into the **Omni Staff**!

### 1. Mavuika — *A Thousand Blazing Suns* (`thousand_blazing_suns`)
- **Element:** 🔥 Pyro | **Role:** Pyro Archon Claymore (On-Field Motorcycle Hypercarry & Off-Field Sub-DPS) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `4.0` (`2.0❤` Basic / `3.0❤` Netherite) | **Skill CD:** `15.0s` | **Burst CD:** `18.0s` | **Durability:** `420` Basic / `840` Netherite
- **Elemental Skill — *The Named Moment* (`Right-Click`):**
  - **Tap Right-Click (Off-Field Mode):** Summons the **Ring of Searing Radiance** above the player (`20s` duration, `80` Nightsoul points). Strikes nearby enemies with coordinated Pyro pillars every `2.0s` within `3.0m` (Basic) / `8.0m` (Netherite) and harvests Amethyst Clusters via Overloaded.
  - **Hold Right-Click or Re-Press Right-Click while Ring is active (All-Fire Armaments):** Summons and mounts the **Flamestrider Motorcycle**!
    - **Driving & Terrain Physics:** Smooth momentum steering (`WASD`), `[Space]` to jump/wheelie, **Water & Lava Surface Skimming**, and **Vertical Cliff Wall-Climbing** (hold `[Sprint]` into a wall; toggleable via `enable_mavuika_wall_climb`).
    - **5-Step Normal Attack (`Tap Left-Click` while riding):** Cleaves in a wide frontal arc with escalating multipliers (`1.0× ➔ 1.05× ➔ 1.15× ➔ 1.25× ➔ 1.55×`), consuming `1.0` Nightsoul Fuel per swing and generating **Fighting Spirit**.
    - **Cyclic Donut Charged Attack (`Hold Left-Click` while riding):** Spins the Flamestrider around its front wheel pivot (`3.2m` cyclic Pyro AoE every `10t`). Releasing Left-Click after spinning unleashes the **Charged Finisher** forward slam.
    - **Sprint Dash & `CD CD CC` Cancel Tech (`[Sprint]` while riding):** Executes a high-speed ram dash (`10t` I-frames, damages enemies in path, costs `3.0` Fuel). Supports a **2-dash chain** (`5t` gap between 1st & 2nd dash, then `1.5s` / `30t` cooldown). **Pro Tech (`CD CD CC`):** Spinning in Donut Charged Attack for `>= 8 ticks` (`0.4s`) **immediately resets the Dash cooldown and chain counter on both Client and Server**, enabling seamless Charged-Dash cancel loops!
    - **Mid-Air Plunge Slam (`Left-Click` in midair on bike):** Slams the motorcycle straight down, detonating a `4.0m` Pyro shockwave on landing.
  > [!TIP]
  > **Camera Recommendation:** Play in **Third-Person Perspective (`F5`)** while riding the Flamestrider Motorcycle (especially during Cyclic Donut Charged Attack spins and `CD CD CC` dash-cancel combos). In First-Person view, the camera spins continuously with the bike's rapid 360° rotations and can cause motion sickness or dizziness.
- **Elemental Burst — *Hour of Burning Skies* (`[R]` Key):**
  - Grants `1.7s` (`34t`) I-frames and leaps high on the Flamestrider to deliver **Sunfell Slice** (`5.0❤` Basic / `10.0❤` Netherite base AoE Pyro, amplified up to **2.0×** by **Fighting Spirit** stacks `0–10`).
  - Activates **Crucible of Death and Life** for `7.0s` (`140t`), cutting all Nightsoul Fuel consumption by **50%** and increasing attack speed and damage.

---

### 2. Furina — *Splendor of Tranquil Waters* (`splendor_of_tranquil_waters`)
- **Element:** 💧 Hydro | **Role:** Hydro Archon Rapier (Dual-Arkhe Summoner, Water Walker & Universal Buffer) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `1.0` (Max HP-scaled summons) | **Skill CD:** `20.0s` (`30s` summon duration) | **Burst CD:** `15.0s` | **Durability:** `420` Basic / `840` Netherite
- **Arkhe Alignment Switch (`Hold Left-Click`):**
  - Switches between **Arkhe: Ousia** (dark outfit/aura, offensive summon mode) and **Arkhe: Pneuma** (light outfit/aura, healing summon mode) with a `1.0s` (`20t`) switch cooldown. If summons are currently active in the world, switching Arkhe morphs them in real time!
- **Elemental Skill — *Salon Solitaire* (`Right-Click`):**
  - **Ousia Mode (*Salon Members*):** Summons 3 independent 3D Hydro companions for `30s` — *Gentilhomme Usher* (ball octopus), *Surintendante Chevalmarin* (bubbly seahorse), and *Mademoiselle Crabaletta* (armored crab). They hover around you, prioritize targets you attack, and consume `1.6%–2.4% Max HP` from nearby party members above `50% HP` to multiply their Hydro damage up to **1.4×**.
  - **Pneuma Mode (*Singer of Many Waters*):** Summons a stationary radiant Hydro totem that periodically heals the player and nearby allies within `8.0m` (`1.0❤ + 5% Max HP` every `2.0s`).
  - **Passive — *Endless Waltz & Water Walking*:** While any *Salon Solitaire* summon is active and you carry *Splendor of Tranquil Waters*, you can **walk and sprint across liquid Water surfaces** as if on solid ground (hold `[Shift]` to dive).
- **Elemental Burst — *Let the People Rejoice* (`[R]` Key):**
  - Grants `1.8s` (`36t`) I-frames, deals AoE Hydro damage around the stage, and activates **Universal Revelry** for `18.0s` (`360t`).
  - Every `1%` of Max HP gained or lost by the player generates **Fanfare Points** (up to `300` stacks), granting up to **+75% All-Damage Bonus** (`+112.5%` Netherite) and **+30% Incoming Healing Bonus**, accompanied by glowing bubble borders on the screen.

---

### 3. Zhongli — *Vortex Vanquisher* (`vortex_vanquisher`)
- **Element:** 🪨 Geo | **Role:** Geo Archon Polearm (Unbreakable Shield, All-RES Shred, Geo Construct Resonance & Petrification) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `4.0` (`+ Max HP scaling`) | **Tap E CD:** `4.0s` | **Hold E CD:** `12.0s` | **Burst CD:** `12.0s` | **Durability:** `420` Basic / `840` Netherite
- **Elemental Skill — *Dominus Lapidis* (`Right-Click`):**
  - **Tap Right-Click (`4.0s` CD):** Summons a `3.5m` physical **Stone Stele** (`GeoFangPillarEntity` in Stele mode, `30s` lifetime, max `3` constructs per player). The Stele blocks projectiles, taunts hit monsters, and emits expanding square **Geo Resonance Waves** every `2.0s` (`3.5m` Basic / `5.5m` Netherite) that trigger **Sympathetic Resonance Pulses** from other nearby Geo Constructs!
  - **Hold Right-Click for `1.5s` (`12.0s` CD):** Unleashes an inward Geo shockwave, summons a Stone Stele (if below cap), and forms the **Jade Shield** for `20.0s`:
    - **150% Damage Absorption** against all Elemental and Physical damage + complete **Knockback Immunity**.
    - **Universal `-20%` RES Shred Aura:** Enemies within `6.0m` of a Jade-Shielded player suffer **-20% All Elemental & Physical Resistance** (indicated by downward golden arrows).
    - **Geo Drain & Mining:** Instantly shatters nearby ore blocks and drains Geo shields/aura from surrounding mobs.
- **Elemental Burst — *Planet Befall* (`[R]` Key):**
  - Grants `2.5s` (`50t`) I-frames & dramatic arms-crossed levitation while summoning a colossal 3D **Meteorite** from the heavens (`6.0m` Basic / `8.5m` Netherite impact radius).
  - Deals heavy Geo damage (`(6.0 + 33% Max HP)` scaled by tier) and inflicts **Petrification** (`4.0s` Basic / `5.0s` Netherite), turning enemies into solid stone statues unable to move, rotate, or attack.
  - Includes **18 authentic Zhongli voice lines** (English & Chinese) accessible via `[V]`.

---

### 4. Venti — *The Daybreak Chronicles* (`the_daybreak_chronicles`)
- **Element:** 🌪️ Anemo | **Role:** Anemo Archon Bow (3-Stage Gale Archer, Updraft Generator & Vortex Crowd Control) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `1.5` (`0.75❤` Basic / `1.13❤` Netherite base shot) | **Skill CD:** `2.0s` | **Burst CD:** `15.0s` | **Durability:** `380` Basic / `760` Netherite
- **Normal & 3-Stage Charged Attack (`Left-Click` / `Hold Left-Click`):**
  - **Tap Left-Click:** Fires a fast Anemo wind bolt (`0.45×` multiplier).
  - **Hold Left-Click (`6t` / `16t`):** Draws the bow through 3 visual pulling stages (`_pulling_0`, `_pulling_1`, `_pulling_2`) without triggering block-breaking swings. At full charge (`>= 16t`), fires a piercing **Aimed Gale Arrow** (`1.45×` AoE Anemo DMG + Swirl + launches the shooter into a Wind-Jump if aiming downward). **Netherite Tier (C1 *Splitting Gales*):** Fires **2 additional homing side arrows** at `±15°`!
- **Elemental Skill — *Skyward Sonnet* (`Right-Click`, `2.0s` CD):**
  - Summons a `5.0m` Anemo domain at the target location or beneath your feet, dealing AoE Anemo damage, triggering **Swirl**, launching enemies skyward with slow-fall suspension, and creating a lingering **Updraft Wind Current** (`10s`) that propels gliding players upward!
- **Elemental Burst — *Wind's Grand Ode* (`[R]` Key, `15.0s` CD):**
  - Grants `2.0s` (`40t`) I-frames and fires a gigantic **Stormeye Vortex** (`8.0s` duration, `8.0m` Basic / `12.0m` Netherite vacuum pull radius).
  - Continuously pulls in enemies and item drops, deals rapid Anemo DoT ticks, and performs **Elemental Absorption** on first contact with **Pyro > Hydro > Electro > Cryo** (converting the vortex's core color and dealing +50% additional absorbed elemental damage every tick!).

---

### 5. Raiden Shogun — *Engulfing Lightning* (`engulfing_lightning`)
- **Element:** ⚡ Electro | **Role:** Electro Archon Polearm (Coordinated Sub-DPS, Resolve Battery & Musou Isshin Hypercarry) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `4.0` (`2.0❤` Basic / `3.0❤` Netherite) | **Skill CD:** `10.0s` | **Burst CD:** `18.0s` | **Durability:** `420` Basic / `840` Netherite
- **Crescent Charged Attack (`Hold Left-Click`):**
  - Unleashes a vertical upward **Crescent Lightning Sweep** (`1.35×` DMG, `7.5m` Basic / `9.5m` Netherite reach) with 3rd-person look-direction lock that launches enemies upward.
- **Elemental Skill — *Transcendence: Baleful Omen* (`Right-Click`, `10.0s` CD):**
  - Deals AoE Electro slash damage and opens the **Eye of Stormy Judgment** above the player for `25.0s` (`500t`).
  - Whenever the player deals damage to any enemy, the Eye unleashes a **Coordinated AoE Electro Slash** at the victim's position (ICD: `0.9s` / `18t`), generating **+4 Resolve** per proc.
  - Casting any staff skill or Elemental Burst generates **Resolve Stacks** (up to **60 stacks**), visualized as glowing purple segments around the 3D **Chakra Desiderata** ring behind the player's back.
- **Elemental Burst — *Secret Art: Musou Shinsetsu* (`[R]` Key, `18.0s` CD):**
  - Grants `1.6s` (`34t`) I-frames, consumes all accumulated **Resolve** (`0–60` stacks, multiplying Burst damage up to **2.5×**), and after a `1.4s` chest-draw windup unleashes **Musou no Hitotachi** (`6.0m` Basic / `9.0m` Netherite frontal AoE slash, `6.0❤➔9.0❤` base before Resolve multiplier, electrifying water along its path).
  - Enters the **Musou Isshin** stance for `7.0s` (`140t`):
    - **5-Step Electro Tachi Combo (`Tap Left-Click`):** Wide `5.0m × 3.0m` violet lightning sword slashes that cannot be overridden by other infusions, restoring `1.0s` cooldown to all staves in your inventory on hit!
    - **Musou Charged Attack (`Hold Left-Click`):** Double crescent cross-slash with extended reach.
    - **Electro Immunity & Poise:** Complete immunity to Electro-Charged damage and knockback resistance while Musou Isshin is active.

---

### 6. Nahida — *A Thousand Floating Dreams* (`thousand_floating_dreams`)
- **Element:** 🌿 Dendro | **Role:** Dendro Archon Catalyst (Tri-Karma Linker, Remote Harvester & EM Dome Buffer) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `4.0` (`2.0❤` Basic / `3.0❤` Netherite) | **Tap E CD:** `5.0s` | **Hold E CD:** `6.0s` | **Burst CD:** `13.5s` | **Durability:** `380` Basic / `760` Netherite
- **Normal & Charged Attack (`Left-Click` / `Hold Left-Click`):**
  - **Tap Left-Click (*Akara* 4-Step Hopscotch Combo):** Projects 4 glowing green keyboard tiles (`2.0m wide × 5.0m forward`) dealing AoE Dendro damage (`0.5❤` base) and **automatically bone-mealing crops and saplings** along the path!
  - **Hold Left-Click (Charged Attack Cube):** Constructs a 3D wireframe **Dendro Containment Cube** (`3×3×3m`) ahead of the player that detonates after `0.65s` (`13t`) for heavy AoE Dendro damage (`3.0❤` Basic / `4.5❤` Netherite, `2.0U`).
- **Elemental Skill — *All Schemes to Know* (`Right-Click`):**
  - **Tap Right-Click (`5.0s` CD):** Sweeps a `2.0m` floral ring around the player, dealing Dendro damage and marking up to `8` enemies with the **Seed of Skandha** (`25.0s` duration), linked by glowing green tethers.
  - **Hold Right-Click (`6.0s` CD — *Camera Viewfinder*):** Enters a 360° ** Green Viewfinder Aiming Mode** (`20.0m` Basic / `28.0m` Netherite range) with brief hover suspension:
    - Sweep your crosshair over up to **8 enemies** to lock on; releasing Right-Click strikes all marked enemies simultaneously with Dendro and binds them with **Seeds of Skandha**.
    - **Remote Botanical Harvesting:** Aiming the viewfinder at mature crops, sweet berries, cocoa, flowers, or dropped items within range **instantly harvests, replants, and teleports the drops directly into your inventory**!
  - **Tri-Karma Purification:** Whenever any enemy marked with a *Seed of Skandha* suffers an Elemental Reaction (or takes Dendro Core damage), **ALL linked enemies** are struck by *Tri-Karma Purification* (`1.5×` Dendro DMG, `1.5U`, `2.5s` base ICD).
- **Elemental Burst — *Illusory Heart* (`[R]` Key, `13.5s` CD):**
  - Grants `1.8s` (`36t`) I-frames and manifests the colossal **Shrine of Maya** palace dome (`18.0m` Basic / `22.5m` Netherite radius, `15.0s` Basic / `23.0s` Netherite duration).
  - Grants **+1 EM Level** (Basic) / **+2 EM Levels** (Netherite) to players inside, plus dynamic elemental buffs based on your carried staves:
    - **Pyro Present:** Increases *Tri-Karma Purification* damage by **+25% / +40%**.
    - **Electro Present:** Shortens *Tri-Karma Purification* trigger interval by **-0.4s / -0.6s**.
    - **Hydro Present:** Extends *Shrine of Maya* duration by **+5.0s / +8.0s**.

---

### 7. The Tsaritsa — *Silver Dawn* (`tsaritsa_bow` ⇄ `tsaritsa_spear`)
- **Element:** ❄️ Cryo | **Role:** Cryo Archon Dual-Stance Regalia (Stellar Cryo Buffer & Orbital Spear DPS) | **Archon:** Yes (`isArchon = true`)
- **Config Base Damage:** `4.5` (`2.25❤` Basic / `3.38❤` Netherite) | **Skill / Stance Switch CD:** `6.0s` | **Burst CD:** `15.0s` | **Durability:** `420` Basic / `840` Netherite
- **Dual-Stance Switching & Elemental Skill (`Right-Click`, `6.0s` CD):**
  - Pressing **Right-Click** seamlessly transforms the weapon item between **Cryo Bow (`tsaritsa_bow` — Support Stance)** and **Cryo Spear (`tsaritsa_spear` — DPS Stance)** while summoning a **Rime-Pillar** (`12.0s` duration, `4.5m` Basic / `6.0m` Netherite radius) that pulses Cryo waves every `2.0s`!
  - **Bow Stance (*Benevolent Regalia*):**
    - `Tap Left-Click`: Fires a rapid Cryo arrow.
    - `Hold Left-Click`: Draws a 3-stage **Frostflake Arrow** (`6t` partial / `16t` full charge) that blooms into an exploding ice lotus on impact (`3.5m` AoE Cryo).
  - **Spear Stance (*Glorious Regalia*):**
    - `Left-Click` (Tap or Hold): Executes a rapid 3-step polearm cleave combo infused with Cryo.
    - **Passive — *Sovereign's Permafrost*:** Walking across water in Spear Stance automatically freezes water blocks beneath your feet into temporary Frosted Ice.
- **Dual-Variant Elemental Burst (`[R]` Key, `15.0s` CD, `1.9s` I-frames):**
  - **Variant 1 — Support Burst (Cast in Bow Stance: *Benevolent Blessing*):**
    - Summons a `10.0m` **Polar Ice Field & Central Ice Pillar** for `15.0s` and bestows the **Azure Crown** (`+15%` All DMG on Basic / `+25%` on Netherite).
    - **Unlocks Stellar Cryo Reactions:** While Azure Crown is active, your Cryo reactions upgrade into **Stellar Melt**, **Stellar Frozen**, and **Stellar Conduct** (see [Elements and Reactions](Elements-and-Reactions))!
    - Seals Spear Stance for `8.0s`; each Stellar Reaction triggered reduces the seal by **2.0s**, and once unsealed, grants a free instant stance switch!
  - **Variant 2 — DPS Burst (Cast in Spear Stance: *Glorious Blessing*):**
    - Hurls a **Colossal Ice Spear** `5.0m` ahead followed by `8` raining Cryo arrows (`7.5m` impact zone), dealing massive AoE Cryo damage (`2.9×` Basic / `3.4×` Netherite).
    - Seals all other staves in your inventory for `10.0s` (hotbar switching to food/blocks/tools remains completely free!) and grants **3 Stellar Punishment Charges**.
    - **Stellar Punishment (`Right-Click` during DPS Burst):** Consumes 1 charge (up to 3 times) to call down a giant orbital Cryo spear onto the nearest target (`2.2×➔2.6×` AoE Cryo DMG)!

---

## 🎭 Archon & Character 3D Idle Animations (AFK Showcase)

Standing completely still for **3.0 seconds** (`60t`) while holding a signature weapon triggers a custom 3D procedural idle animation (automatically canceled the instant you move, jump, or attack):

| Character | Weapon | 3D Procedural Idle Animation & Visual Details |
| :--- | :--- | :--- |
| **Furina** | *Splendor of Tranquil Waters* | • **Pneuma (Light):** Sits gracefully on a translucent Hydro bubble chair while a carousel of **6 Fontaine pastries** (Strawberry Shortcake, Blueberry Mousse, Mont Blanc, Rainbow Macaron, Caramel Pudding, Matcha Opera) orbits around her; picks up and nibbles a cake with falling crumbs.<br>• **Ousia (Dark):** Cradles baby **Surintendante Chevalmarin** in both hands while the seahorse blows playful water arcs (and auto-summons the companion if none is active). |
| **Zhongli** | *Vortex Vanquisher* | Crosses arms in contemplative poise (with smooth 3rd-person camera angle lock) while a miniature golden **Planet Befall meteorite** orbits his torso leaving Geo dust trails. |
| **Venti** | *The Daybreak Chronicles* | Gently levitates into midair cradling the **Holy Lyre der Himmel**, strumming melodies above a 5-tier counter-rotating jade whirlwind. |
| **Raiden Shogun** | *Engulfing Lightning* | Meditates in midair levitation as the **Eye of Stormy Judgment** and **Chakra Desiderata halo** ignite behind her back with violet arcs. |
| **Mavuika** | *A Thousand Blazing Suns* | Casually leans on the **Flamestrider Motorcycle** (in 1st-person view, applies a warm cinematic visor tint `0x140e18` without obstructing vision). |
| **Nahida** | *A Thousand Floating Dreams* | Manifests a glowing emerald **botanical vine swing** suspended from above, swaying gently back and forth. |
| **The Tsaritsa** | *Silver Dawn* | Conjures the crystalline **Rime-Throne** and an ice desk, writing royal decrees with a glowing frost quill in both 1st-person and 3rd-person views. |
| **Xilonen** | *Peak Patrol Song* | Deploys a neon-lit Natlan **DJ Turntable Booth** with dual spinning vinyl decks and equalizer bars pulsing to the beat. |

---

## 🎶 Elemental Resonance & 4-Staff Inventory Cap

Carrying combat staves in your inventory (up to the configurable **4-Staff Cap**, default `max_staves_in_inventory = 4`) forms a 4-weapon party composition and activates **Elemental Resonances**:

| Resonance | Requirement | Active Buffs & Mechanics |
| :--- | :--- | :--- |
| 🔥 **Fervent Flames (Pyro)** | `>= 2` Pyro Staves | **+10% All Attack Damage** (configurable via `pyro_resonance_damage_bonus`, default `0.10`); reduces Cryo aura & Freeze duration on you by **-40%**. |
| 💧 **Soothing Water (Hydro)** | `>= 2` Hydro Staves | **+25% Max HP** (`+5.0❤` base); extinguishes fire `2×` faster; reduces Pyro aura duration on you by **-40%**. |
| 🌪️ **Impetuous Winds (Anemo)** | `>= 2` Anemo Staves | **+10% Movement Speed**; **-15% Hunger/Exhaustion consumption**; **-10% Skill & Elemental Burst Cooldowns** (including Archon `[R]` Bursts!). |
| ⚡ **High Voltage (Electro)** | `>= 2` Electro Staves | Triggering *Superconduct, Overloaded, Electro-Charged, Quicken, Aggravate,* or *Hyperbloom* spawns an **Electro Elemental Particle** (`5.0s` ICD) that restores **1.0s cooldown** to all staves in your inventory; reduces Hydro aura duration by **-40%**. |
| 🌿 **Sprawling Greenery (Dendro)** | `>= 2` Dendro Staves | Passively grants **+1 Elemental Mastery (EM) Level**. Triggering *Burning, Bloom,* or *Quicken* grants **+1 additional EM Level** for `6.0s`; triggering *Aggravate, Spread, Hyperbloom,* or *Burgeon* grants another **+1 EM Level** for `6.0s`! |
| ❄️ **Shattering Ice (Cryo)** | `>= 2` Cryo Staves | Attacks against enemies affected by Cryo or Frozen have a **+15% Critical Hit Chance** (`1.5×` damage multiplier); reduces Electro aura duration on you by **-40%**. |
| 🪨 **Enduring Rock (Geo)** | `>= 2` Geo Staves | **+15% Shield Strength**. While protected by any shield (*Crystallize, Jade Shield,* or *Curtain of Slumber*), deals **+15% All Damage** and shreds hit enemies' **Geo RES by -20% for 15.0s**. |
| 🛡️ **Protective Canopy (4-Unique)** | `4` Staves of 4 Different Elements | Reduces all incoming **Elemental & Physical Damage by -15%**. |

> [!WARNING]
> **Elemental Overload (`> 4` Staves) & Swap Cooldown:**
> - Carrying more than `max_staves_in_inventory` (default `4`) combat staves at once triggers **Elemental Overload**, disabling all staff skills, bursts, and resonances until excess staves are placed in a chest or bundle (Creative & Spectator players are exempt; *Imbued Swords*, *Void Translocator*, and *Magic Mirror* do **not** count toward the cap).
