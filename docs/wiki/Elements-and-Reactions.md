# Elements & Elemental Reactions

The combat mechanics of **Velorise's Staves & Elements** are governed by a specialized **Elemental Gauge Theory (EGT)** engine inspired by *Genshin Impact*. When an entity is struck by an elemental spell, environmental hazard, or full-charge imbued sword swing, an **Aura** measured in **Gauge Units (U)** is applied to the target. Applying a reactive element while an aura is active triggers an **Elemental Reaction**.

---

## The 7 Elements + Omni

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png" width="32" height="32" /><br/><b>Pyro</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="32" height="32" /><br/><b>Hydro</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png" width="32" height="32" /><br/><b>Cryo</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="32" height="32" /><br/><b>Electro</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png" width="32" height="32" /><br/><b>Dendro</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="32" height="32" /><br/><b>Anemo</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png" width="32" height="32" /><br/><b>Geo</b></td>
      <td align="center"><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/omni.png" width="32" height="32" /><br/><b>Omni</b></td>
    </tr>
  </table>
</div>

* **5 Persistent Aura Elements:** **Pyro**, **Hydro**, **Cryo**, **Electro**, and **Dendro** linger on targets as active auras (overhead icon blinks during the final 3 seconds before expiring).
  * **Cryo Aura Passive Debuff:** Any entity carrying an active unfrozen Cryo aura suffers **-15% Movement Speed** and **-15% Attack Speed** (up to -30% combined with Frostbolt slowness).
* **2 Trigger-Only Elements:** **Anemo** and **Geo** never linger as standalone auras; they immediately react with existing auras to trigger **Swirl** or **Crystallize / Shatter**.
* **Omni:** Primordial adaptive energy that dynamically evaluates the target's active/innate state to trigger the highest-value reaction, or locks onto any of the 7 elements via the `[Tab]` wheel.

---

## Elemental Gauge Theory (EGT) Math & Decay Formulas

When an attack with source gauge $U_{\text{source}}$ applies a fresh aura, the engine enforces authentic EGT rules (`GaugeState.java` & `ElementalGaugeManager.java`):

1. **0.8x Aura Tax & Base Duration:**
   * Initial stored gauge: $U_{\text{stored}} = 0.8 \times U_{\text{source}}$
   * Base duration: $T = 2.5 \times U_{\text{source}} + 7.0\text{ seconds}$ (e.g., `1.0U` $\rightarrow 0.8\text{U}$ stored for `9.5s`; `1.5U` $\rightarrow 1.2\text{U}$ for `10.75s`; `2.0U` $\rightarrow 1.6\text{U}$ for `12.0s`; `4.0U` $\rightarrow 3.2\text{U}$ for `17.0s`).
2. **Decay Rate Inheritance & Pyro 3.0 Rule:**
   * **Hydro, Cryo, Electro & Dendro:** Refreshing an existing aura with the same element updates $U = \max(U_{\text{current}}, 0.8 \times U_{\text{new}})$ while **inheriting the initial aura's decay rate** until the gauge fully expires.
   * **Pyro (3.0+ Rule):** Refreshing Pyro updates the decay rate to the new trigger's decay rate **only if** the new taxed gauge strictly exceeds the remaining Pyro gauge ($0.8 \times U_{\text{new}} > U_{\text{current}}$).
3. **Special Reaction State Formulas:**
   * **Quicken Aura:** $U_{\text{Quicken}} = \min(U_{\text{aura}}, 0.8 \times U_{\text{trigger}})$, Duration $T_{\text{Quicken}} = 5 \times U_{\text{Quicken}} + 6.0\text{s}$. Stored in an independent Quicken gauge so underlying Dendro or Electro can coexist.
   * **Frozen Aura:** $U_{\text{Freeze}} = 2 \times \min(U_{\text{aura}}, U_{\text{trigger}})$, Duration $T_{\text{Freeze}} = 2\sqrt{5 \times U_{\text{Freeze}} + 4} - 4.0\text{s}$. Preserves any unconsumed underlying Hydro or Cryo aura underneath the ice!
   * **Burning Aura:** Sustains an untaxed `2.0U` Pyro aura ($T = 12.0\text{s}$), dealing Pyro damage every `0.5s` (10 ticks) while consuming Dendro gauge.
4. **Simultaneous Reaction Priority & Double Reactions:**
   * When multiple auras coexist on a target (such as **Hydro + Electro** during Electro-Charged, or **Quicken + Electro/Dendro**), incoming trigger attacks evaluate reactions in strict priority order:
     $$\text{Electro} \rightarrow \text{Pyro} \rightarrow \text{Hydro} \rightarrow \text{Cryo} \rightarrow \text{Dendro}$$
   * If the trigger attack has enough gauge units to completely consume the first aura with leftover gauge remaining ($U_{\text{remaining}} > 0.01\text{U}$), it **immediately chains a secondary reaction** against the next reactive aura on the same hit!

---

## Multiplicative (Amplifying) Reactions

Multiplicative reactions scale the triggering attack's damage directly, further boosted by [**Elemental Mastery**](Staves-List#staff-enchantments-elemental-mastery) (`+15%` per level, up to `+45%` at EM III).

| Reaction | Trigger Combination | Base Multiplier | Gauge Consumption | Mechanism |
| :--- | :--- | :---: | :---: | :--- |
| <a id="vaporize"></a><a id="forward-vaporize"></a>**Forward Vaporize** | Hydro applied to Pyro aura | **2.0x (200%)** | `2.0x` Gauge | Douses the flame in a violent steam eruption, typically clearing the Pyro aura in one strike. |
| <a id="reverse-vaporize"></a>**Reverse Vaporize** | Pyro applied to Hydro aura | **1.5x (150%)** | `0.5x` Gauge | Flash-boils the target while consuming only half of the Hydro gauge, allowing **2 consecutive Vaporize procs** per `1.0U` Hydro aura! |
| <a id="melt"></a><a id="forward-melt"></a>**Forward Melt** | Pyro applied to Cryo / Frozen | **2.0x (200%)** | `2.0x` Gauge | Rapidly liquefies the frost layer for massive thermal shock damage. Also triggers on Cryo-innate mobs (e.g. Strays) burning in sunlight or fire! |
| <a id="reverse-melt"></a>**Reverse Melt** | Cryo applied to Pyro aura | **1.5x (150%)** | `0.5x` Gauge | Chills the flame while consuming only half the Pyro aura gauge. |
| <a id="overvape"></a>**Overvape** | Pyro on coexisting **(Hydro + Electro)** | **1.5x Vape + Overloaded AoE** | `0.5x` Hydro + `1.0x` Electro | Simultaneously triggers **Reverse Vaporize** amplification on the main hit AND detonates an **Overloaded** AoE blast! |

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Vaporize Reaction</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/vaporize.gif" width="380" /></td>
      <td align="center"><b>Forward Melt Reaction</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/melt.gif" width="380" /></td>
    </tr>
  </table>
</div>

---

## Transformative, Catalyze & Crowd Control Reactions

Transformative reactions deal independent elemental burst damage (scaling `+30%` per level of [Elemental Mastery](Staves-List#staff-enchantments-elemental-mastery), up to `+90%` at EM III), while Catalyze reactions add flat bonus damage (`+25%` per EM level).

| Reaction | Elements Involved | Gauge Mod | Base Effect & Combat Mechanics |
| :--- | :--- | :---: | :--- |
| <a id="frozen"></a>**Frozen** | Hydro + Cryo | `1.0x` | Encases non-boss targets in a solid 3D ice layer, locking their pose, rotation, and AI movement. **Frozen Players** have their camera and movement locked, and can mash **`[SPACE]`** (`[SPACE] Break Free` prompt) to shave **0.3s (6 ticks)** off the freeze timer per press (0.15s ICD) accompanied by glass-crack sounds and snowflake particles! |
| <a id="shatter"></a>**Shatter** | Geo / Blunt Explosion / Heavy hit on **Frozen** | Consumes Freeze | Breaches the ice block immediately, dealing heavy physical/Geo shatter damage before any follow-up reaction. |
| <a id="electro-charged"></a>**Electro-Charged** | Hydro + Electro | `0.4x` / tick | Both Hydro and Electro auras **coexist** on the target! Shocks the target every **1.0s (20 ticks)** and arcs lightning bolts to all wet/Hydro-afflicted entities within **5.0m**. |
| <a id="overloaded"></a>**Overloaded** | Pyro + Electro | `1.0x` | Detonates a **3.0m–3.5m AoE** fiery blast dealing Pyro damage, launching enemies backward, priming TNT, melting ice, and mining **Amethyst Clusters into 2–4 Amethyst Shards**! |
| <a id="superconduct"></a>**Superconduct** | Cryo + Electro | `1.0x` | Deals AoE Cryo shockwave damage within **4.0m** and inflicts **-40% Physical Resistance Shred** for **12 seconds (240 ticks)**. |
| <a id="burning"></a>**Burning** | Pyro + Dendro | `0.4x` Dendro | Sustains a `2.0U` Pyro aura and inflicts rapid Pyro damage every **0.5s (10 ticks)** while igniting nearby brush. |
| <a id="swirl"></a>**Swirl** | Anemo + (Pyro / Hydro / Cryo / Electro) | `0.5x` | Absorbs the active element, deals bonus Swirl damage, and spreads the absorbed aura to all enemies within a **5.0m AoE** (triggered by Anemo staves, Breeze charges, Warden Sonic Boom, or a **Mace enchanted with Wind Burst**). |
| <a id="crystallize"></a>**Crystallize** | Geo + (Pyro / Hydro / Cryo / Electro) | `0.5x` | Spawns a 3D spinning [**Crystallize Elemental Shard**](Relics-and-Utilities#crystallize-shield-shards) on the ground (17.5s lifetime; 3s per-mob spawn ICD). Picking it up grants a **15s elemental Absorption shield** (`2.0 HP` base + `1.0 HP` per EM level, `+15%` with Geo Resonance) with **250% absorption efficiency** (reducing matching elemental damage by **60%**). |

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Electro-Charged</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/electro_charged.gif" width="360" /></td>
      <td align="center"><b>Overloaded Explosion</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/overloaded.gif" width="360" /></td>
    </tr>
    <tr>
      <td align="center"><b>Swirl Vortex (AoE Aura Spread)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/swirl.gif" width="360" /></td>
      <td align="center"><b>Geo Crystallize (Elemental Shield Shards)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/crystallize.gif" width="360" /></td>
    </tr>
  </table>
</div>

---

## Dendro Reaction System (Bloom & Catalyze)

Dendro reactions branch into two distinct combat pathways: **Dendro Core generation (Bloom)** and **Quicken amplification (Catalyze)**.

```
                  ┌──(Hydro)──> [ Dendro Core Entity (Max 5, 6s Lifetime) ]
                  │                     │
[Dendro Target] ──┤                     ├──(Electro)──> Hyperbloom (16.0 HP / 8.0♥ Homing Missile, 12m)
                  │                     │
                  │                     └──(Pyro)─────> Burgeon (17.5 HP / 8.75♥ Instant 5.0m AoE Blast)
                  │
                  └──(Electro)─> [ Quicken State (5*U + 6s) ] ──┬──(Electro)──> Aggravate (+Flat Electro DMG)
                                                                 └──(Dendro)───> Spread (+Flat Dendro DMG)
```

* <a id="bloom"></a>**Bloom (`0.5x` Forward when Hydro hits Dendro / `2.0x` Reverse when Dendro hits Hydro):** Spawns a custom 3D 16-faceted **Dendro Core** entity (`DendroCoreEntity`) that floats on water. Up to **5 Dendro Cores** can exist simultaneously; spawning a 6th core or waiting **6 seconds (120 ticks)** causes the oldest core to detonate in a **3.0m–3.5m AoE** Dendro burst. *(Firing the [Webcaster](Staves-List#webcaster) directly into water also spawns a Dendro Core!)*
* <a id="hyperbloom"></a>**Hyperbloom (Electro on Dendro Core):** Transforms the Dendro Core into a neon-green homing missile that arcs toward the nearest hostile target within **12m**, dealing **16.0 HP (8.0 Hearts)** base Dendro damage.
* <a id="burgeon"></a>**Burgeon (Pyro on Dendro Core):** Detonates the Dendro Core immediately in a **4.0m–5.0m AoE** fiery botanical blast dealing **17.5 HP (8.75 Hearts)** base Dendro damage without destroying terrain blocks.
* <a id="quicken"></a><a id="aggravate"></a><a id="spread"></a>**Quicken, Aggravate & Spread:** Combining Dendro and Electro applies the **Quicken** state ($T = 5U + 6\text{s}$) without consuming either element's ability to react with Pyro/Hydro. Striking a Quickened target with Electro triggers **Aggravate** (`0.0x` gauge consumed), while striking with Dendro triggers **Spread** (`0.0x` gauge consumed).

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Bloom (Creation)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/bloom.gif" width="240" /></td>
      <td align="center"><b>Hyperbloom (Electro)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/hyperbloom.gif" width="240" /></td>
      <td align="center"><b>Burgeon (Pyro)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/burgeon.gif" width="240" /></td>
    </tr>
  </table>
</div>

---

## Internal Cooldown (ICD) System

Velorise's Staves enforces two independent cooldown layers so rapid multi-hit spells remain balanced while intentional elemental combos always connect:

### 1. Standard Aura ICD (The 3-Hit / 2.5s Rule)
When repeatedly hitting a target with the same element:
* **Hit 1:** Applies elemental aura (or triggers reaction).
* **Hits 2 & 3:** Deal full weapon/spell damage, but **do not** apply aura.
* **Hit 4 (or after 50 ticks / 2.5s):** Resets the counter and applies aura again.
* Tracked per **`Attacker + Target Entity + Element`**. Switching elements, alternating players, or using full-charge imbued sword swings, [Wither Scepter](Staves-List#wither-scepter) skulls, Lingering Potion clouds, or tamed mutated pets explicitly bypasses standard ICD (`applyElementBypassIcd`).

### 2. Reaction Trigger ICD
Each reaction enforces a per-target trigger cooldown (`getReactionCooldownTicks`):

| Elemental Reaction | Cooldown (Ticks) | Cooldown (Seconds) | Behavior & Notes |
|:---|:---:|:---:|:---|
| **Aggravate / Spread** | `5 ticks` | **0.25s** | High-cadence additive flat damage on Quicken aura |
| **Forward / Reverse Melt & Vaporize, Overvape** | `8 ticks` | **0.40s** | Multiplicative damage scaling window |
| **Overloaded / Superconduct / Swirl / Shatter** | `10 ticks` | **0.50s** | Transformative burst & resistance shred throttle |
| **Electro-Charged** | `10 ticks` | **0.50s** | Minimum shock trigger interval |
| **Bloom / Reverse Bloom / Hyperbloom / Burgeon** | `10 ticks` | **0.50s** | Dendro Core generation & detonation throttle (plus 2-hit/0.5s per-target cap) |
| **Frozen** | `10 ticks` | **0.50s** | Re-freeze lockout prevention |
| **Crystallize** | `20 ticks` | **1.00s** | Reaction ICD (plus 3.0s / 60-tick per-mob shard spawn cap) |
| **Burning** | `40 ticks` | **2.00s** | Sustained Pyro DoT re-trigger cooldown |

### 3. Vanilla i-Frame (`invulnerableTime`) Synchronization
Vanilla Minecraft grants **10 ticks (0.5s)** of damage immunity after every hit, which normally swallows fast elemental combos. Velorise's Staves zeroes out `invulnerableTime = 0` on valid elemental reaction hits and spaces sequential burst staff shots at **11 ticks (0.55s)** so every single orb, skull, and fang connects cleanly.

---

## Elemental Resonance System (All 8 Resonances)

Carrying matching elemental weapons in your inventory unlocks passive **Elemental Resonances** (`ElementalResonanceManager.java`). Up to **2 distinct 2-piece resonances** can be active at the same time.

### Complete 8-Resonance Reference Table

| Resonance Name | Activation Requirement | Passive Buffs & Combat Effects |
| :--- | :--- | :--- |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png" width="16" height="16" /> **Pyro: Fervent Flames** | 2 distinct Pyro Staves *(or 1 Pyro + Auto Omni)* | **-40% Cryo Aura duration** on you; grants **+10% Direct Elemental & Melee Damage** (`pyro_resonance_damage_bonus = 0.10`, without reaction double-dipping). |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="16" height="16" /> **Hydro: Soothing Water** | 2 distinct Hydro weapons *([Guardian Lens](Staves-List#guardian-lens) + Vanilla Trident or Auto Omni)* | **-40% Pyro Aura & Fire burn duration** on you; grants **+25% Max Health** (`+0.25 ADD_MULTIPLIED_TOTAL`) and **+25% Incoming Healing Effectiveness**, directly boosting sustain, Guardian Lens & [Geo Pillar](Environmental-Interactions#3-geo-constructs--fang-pillars) HP! |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="16" height="16" /> **Anemo: Impetuous Winds** | 2 distinct Anemo Staves *(or 1 Anemo + Auto Omni)* | **+10% Movement Speed**, **-15% sprint/jump hunger exhaustion**, and **-10% Cooldown Duration** on all Staves & Utility Relics. |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="16" height="16" /> **Electro: High Voltage** | 2 distinct Electro Staves *(or 1 Electro + Auto Omni)* | **-40% Hydro Aura duration** on you; triggering any Electro reaction **restores +1 Durability** to your held staff and **reduces all active Staff Cooldowns by 1.0s (20 ticks)** *(5.0s / 100-tick ICD)*. |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png" width="16" height="16" /> **Dendro: Sprawling Greenery** | 2 distinct Dendro Staves *(or 1 Dendro + Auto Omni)* | Grants **+1 permanent [Elemental Mastery](Staves-List#staff-enchantments-elemental-mastery) tier** (`+15%` Amp / `+30%` Trans / `+25%` Cat); triggering any Dendro reaction grants **+1 additional EM tier for 6.0s (120 ticks)**. |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png" width="16" height="16" /> **Cryo: Shattering Ice** | 1 Cryo Staff + Auto/Locked [Omni Staff](Staves-List#the-apex-weapon-omni-staff) | **-40% Electro Aura duration** on you; grants **+15% CRIT Rate (1.5x damage multiplier)** on both Elemental Staff hits and Physical Melee attacks against Cryo-afflicted or Frozen targets. |
| <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png" width="16" height="16" /> **Geo: Enduring Rock** | 1 Geo Staff + Auto/Locked [Omni Staff](Staves-List#the-apex-weapon-omni-staff) | **+15% [Crystallize Shield](Relics-and-Utilities#crystallize-shield-shards) HP**; while protected by any Crystallize Shield or Absorption hearts, you deal **+15% Elemental & Physical Damage** and inflict **-20% Geo Resistance Shred for 15.0s (300 ticks)** on struck enemies. |
| 🛡️ **Protective Canopy** | **4 Distinct Elements** *(when no 2-piece resonance is active)* | Reduces all incoming **Physical and Elemental Damage taken by 15%** (`0.85x` damage multiplier). |

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/resonance_priority_electro.png" width="700" alt="Resonance Priority Case 1: Electro Active" />
  <br/><sub><i>Case 1: Omni Staff auto-matches with the 1st solo element found (Slot 4: Arc Caster ➔ Electro Resonance). Slots 5 & 6 pair for Pyro Resonance. Slot 8 (Cryo) is inactive.</i></sub>
</div>

### Slot Priority & Omni Staff Wildcard Pairing
The resonance engine scans your inventory in strict priority order:  
`Mainhand ➔ Offhand ➔ Hotbar (Slots 0–8) ➔ Main Inventory (Slots 9–35)`

1. **2-Weapon Pairing Rules:** Two staves must be of **different `Kind`s** (e.g., `Inferno Rod` + `Ghastfire Staff`) to form a natural pair. Carrying two copies of the exact same staff kind (such as 2x `Inferno Rod`, or Basic `Inferno Rod` + `Netherite Inferno Rod`) counts as only **1 unique staff kind** and does not self-pair.
2. **Omni Staff Modes:**
   * **Auto Mode (`mode = 0`):** Acts as a universal wildcard that scans in slot priority order and pairs with the **first solo element** that does not already have an active resonance (enabling single-staff elements like **Cryo** `Frostbolt Wand` and **Geo** `Evoker Fang Staff` to unlock their 2-piece resonances!).
   * **Locked Mode (`mode = 1..7`):** Counts as a distinct weapon key (`OMNI_<ELEMENT>`) of that locked element, allowing it to pair directly with any staff of the same element!
3. **Imbued Swords Excluded:** [Element-Imbued Swords](Anvil-Imbuing#how-to-imbue-swords) never count toward Elemental Resonance or the 4-Staff inventory cap.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/resonance_priority_cryo.png" width="700" alt="Resonance Priority Case 2: Cryo Active" />
  <br/><sub><i>Case 2: Reordering hotbar slots. Moving Frostbolt Wand (Cryo) to Slot 3 causes Omni Staff to pair with Cryo first, instantly switching your active resonance from Electro to Cryo!</i></sub>
</div>

---

## Arcane Overload (4-Staff Inventory Cap)

Carrying more than **4 distinct Arcane Staff types** (`max_staves_in_inventory = 4`) in your main inventory + offhand in Survival/Adventure mode triggers **Arcane Overload**:

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/arcane_overload.gif" width="480" alt="Arcane Overload Demonstration" />
  <br/><sub><i>Exceeding the 4-staff inventory limit triggers Arcane Overload, displaying a red Triquetra warning icon and disabling staff casting.</i></sub>
</div>

* **Penalties:** Disables all staff casting, suppresses all active Elemental Resonances, and displays an `Arcane Overload (X/4)` warning on the HUD and Inventory screen.
* **Exemptions:** Duplicate backup copies of the same staff type (including Basic + Netherite of the same kind) count as **1** toward the cap. Vanilla Tridents, [Element-Imbued Swords](Anvil-Imbuing#how-to-imbue-swords), and staves stored inside Shulker Boxes, Bundles, Ender Chests, or backpack mods do **not** count. Creative and Spectator players are exempt.

---

## Innate Elemental Mobs, Drops & Rare Omni Mutations

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Natural Innate Aura (Blaze)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/innate_blaze.gif" width="340" /><br/><sub><i>Innate mobs permanently carry their element and are 100% immune to same-element damage.</i></sub></td>
      <td align="center"><b>Rare Omni Mutation (Magic Immune)</b><br/><img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/omni_mob_immunity.gif" width="340" /><br/><sub><i>Omni-mutated mobs are 100% immune to all 7 magical elements — only physical damage can harm them!</i></sub></td>
    </tr>
  </table>
</div>

### 1. Natural Innate Mobs & Special Mob Attack Elements
Mobs defined in the `velorise_staves:*_creatures` entity tags (plus special combat rules) permanently carry innate elements, are **100% immune to their own element**, and apply their element on hit:
* **Pyro (`#pyro_creatures`):** `Blaze`, `Magma Cube`, `Ghast`, `Wither Skeleton`, `Strider`, `Zombified Piglin`, `Zoglin`.
* **Hydro (`#hydro_creatures`):** `Guardian`, `Elder Guardian`, `Drowned`, `Axolotl`.
* **Cryo (`#cryo_creatures`):** `Stray`, `Snow Golem`, `Polar Bear`. *(Strays burning in sunlight or fire automatically suffer **2.0x Forward Melt** damage!)*
* **Electro (`#electro_creatures` + Ender attacks):** `Ender Dragon`, `Enderman`, `Shulker`, `Endermite`, and **Charged Creepers**. Direct hits from Dragon Fireballs, Shulker Bullets, and thrown Ender Pearls also apply `1.5U Electro`.
* **Dendro (`#dendro_creatures` + Wither):** `Bogged`, `Cave Spider`, `Slime`, `Bee`, `Mooshroom`. Furthermore, **Wither Boss / Wither Skulls** apply `1.0U Dendro`.
* **Anemo (`#anemo_creatures`):** `Breeze`, `Phantom`, `Vex`. **Warden Sonic Booms** also apply `2.0U Anemo` (triggering Swirl), and **Ravager Roars** apply `1.5U Anemo`.
* **Geo (`#geo_creatures` + Earth attacks):** `Armadillo` and `Iron Golem` (innate Geo defense). In addition, **Husk melee attacks** apply `0.5U Geo`, **Ravager melee/stomps** apply `1.0U Geo`, and **wild Evoker Fangs** apply `1.0U Geo` and summon a **Basic Geo Fang Pillar** at the end of the Evoker's fang line!
* **Mutated / Innate Creeper Explosions:** When a Creeper with an innate or mutated element (or Omni mutation) detonates, its explosion burst applies `1.5U`–`2.0U` of its element across all entities caught in the blast radius!

### 2. Mob Mutation Rules (7% Default: 6% Elemental + 1% Omni)
Eligible hostile mobs (`allow_hostile_mob_mutation = true`) and Golems (`allow_golem_mutation = true`) without a natural innate element have a **7% chance** (`innate_mutation_chance = 0.07`) to mutate upon spawning, plus whitelisted neutral companions (`minecraft:iron_golem`, `minecraft:wolf`, `minecraft:fox`):
* **Standard Elemental Mutation (~6%):** Rolls 1 of the 7 elements. The mob gains an overhead elemental badge, becomes 100% immune to that element, and applies `1.0U` on every attack.
* **The Omni Mutation (~1%, `omni_mutation_share = 0.143`):**
  * 🛡️ **100% Magic & Reaction Immunity:** Immune to all 7 elements and all elemental reaction damage (`IMMUNE` floating combat text).
  * ⚔️ **Physical Vulnerability & Adaptive Strikes:** Takes full physical damage from swords (including [Element-Imbued Swords](Anvil-Imbuing#how-to-imbue-swords)), axes, bows, and crossbows. When an Omni-mutated mob attacks a target with an active aura, it dynamically picks the strongest counter-element to detonate a reaction (or applies a random element if the target has no aura).
* **Fixed Elemental Essence Drop Rates (Player Hostile Kills, No Looting Scaling):**
  * **Natural Innate Hostiles:** **5%** chance to drop their matching Element.
  * **Elemental Mutated Hostiles:** **15%** chance to drop their mutated Element.
  * **Omni Mutated Hostiles:** **25%** chance to drop [**Omni Element**](Relics-and-Utilities#omni-element).
  * *(Split medium/tiny Slimes and Magma Cubes with `size < 3` do not drop essences).*

### 3. Mob Base Resistances & Special Elemental Traits

| Creature / Trait | Resistance Profile | Strategic Notes |
|:---|:---:|:---|
| **All Standard Mobs** | **10% Base Elemental RES**<br/>**10% Base Physical RES** | All mobs reduce incoming elemental and physical damage by 10% by default. |
| **Iron Golem** | **50% Physical Resistance** | Absorbs 50% of raw physical damage. Use elemental magic or **Superconduct (-40% Physical RES)** to shred its armor. |
| **Warden** | **70% Anemo Resistance**<br/>*(+ Fire & Pyro Immunity)* | Resists sonic/wind attacks by 70%, is immune to fire/Pyro damage, and cannot be immobilized by Frozen. |
| **Boss Entities** *(Warden, Ender Dragon, Wither, Elder Guardian)* | **Frozen Immobilization Immunity** | Bosses still take reaction damage from Cryo + Hydro, but cannot be locked in a Frozen statue pose. |
| **Geo-Innate Mobs** | **+50% Projectile & Explosion RES** | Reduces damage from ranged projectiles and explosive reactions (**Overloaded**, **Burgeon**, **Shatter**) by an additional 50% (capped at 90% total RES). |
| **Anemo-Innate Mobs** *(Breeze, Phantom, Vex)* | **100% Fall Damage Immunity** | Immune to fall impact damage (except pointed stalagmites). |
| **Superconduct Shred** | **-40% Physical RES (12s)** | Reduces target Physical Resistance by 40% for 12 seconds (pushing a 10% base RES mob to `-30% RES` = **+15% physical damage amplification** under the 3-tier Genshin curve). |

> **Authentic 3-Tier Genshin Resistance Formula (`computeResistanceMultiplier`):**
> * **Negative Resistance ($RES < 0$):** $\text{Multiplier} = 1.0 - \frac{RES}{2.0}$ *(Resistance shred below 0% is halved in effectiveness).*
> * **Standard Resistance ($0 \le RES \le 0.75$):** $\text{Multiplier} = 1.0 - RES$.
> * **Extreme High Resistance ($RES > 0.75$):** $\text{Multiplier} = \frac{1.0}{4.0 \times RES + 1.0}$ *(Prevents 100% invulnerability on stacked resistances).*

### 4. Elemental Pets & Tamed Companions
**Wolves** (`minecraft:wolf`) and **Foxes** (`minecraft:fox`) are included in `mutable_mob_whitelist` by default:
* Taming a mutated Wolf preserves its elemental aura, particles, and Jade HUD status permanently.
* Every bite from a tamed mutated pet applies **1.0U of its element with 0 ICD** (`applyElementBypassIcd`), while an **Omni Wolf** is 100% immune to magic and dynamically triggers the best reaction on every bite.

---

## Vanilla Mechanics Synergy (Potions, Arrows, Tridents, Mace & Thorns)

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/trident_hydro.gif" width="480" alt="Trident counts as Hydro demonstration" />
  <br/><sub><i>Vanilla Tridents inherently channel Hydro properties, applying a 1.0U Hydro aura upon striking targets and partnering for Hydro Resonance.</i></sub>
</div>

### 1. Drinking Potions, Splash/Lingering Potions & Tipped Arrows
Drinking a potion (`LivingEntityUseItemEvent.Finish` / `completeUsingItem`), throwing a **Splash / Lingering Potion**, or hitting a target with a **Tipped Arrow** applies **1.0U** of the mapped element (bypassing standard ICD):

| Vanilla Potion / Effect | Mapped Element | Reaction & Application Notes |
|:---|:---:|:---|
| **Water Bottle** (`Potions.WATER`) | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="16" height="16" /> **Hydro (1.0U)** | Drinking, splashing, or shooting Water Tipped Arrows applies `1.0U Hydro`. |
| **Poison / Oozing** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png" width="16" height="16" /> **Dendro (1.0U)** | Triggers Bloom on wet targets or Quicken with Electro. |
| **Slowness** (`MOVEMENT_SLOWDOWN`) | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png" width="16" height="16" /> **Cryo (1.0U)** | Splash/Lingering Slowness potions also **freeze surface water (radius 2)** on impact! |
| **Instant Damage** (`HARM`) | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="16" height="16" /> **Electro (1.0U)** | Triggers Overloaded, Electro-Charged, Superconduct, or Hyperbloom. |
| **Speed / Wind Charged** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="16" height="16" /> **Anemo (1.0U)** | Triggers **Swirl** to spread active auras across `5.0m`. |
| **Resistance / Infested** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png" width="16" height="16" /> **Geo (1.0U)** | Triggers **Crystallize** shield shards or **Shatter** on Frozen targets. |

* **Lingering Potion Clouds (`AreaEffectCloud`):** Pulse every **20 ticks (1.0s)**, applying `1.0U` of their mapped element(s) to all entities inside the cloud.
* **Plain Snowballs & Firework Rockets:** Throwing a vanilla **Snowball** applies `0.5U Cryo` on direct hit and freezes surface water (`1m` radius). **Firework Rocket** explosions apply `1.5U Pyro`.

### 2. Vanilla Trident, Wind Burst Mace & Thorns Armor
* **Vanilla Trident (`1.0U Hydro` + Resonance Partner):** Melee and thrown Trident hits apply `1.0U Hydro`. Carrying a Trident alongside at least 1 Arcane Staff counts as a free **Hydro partner** for activating **Hydro Elemental Resonance** without consuming a slot toward the 4-Staff Overload cap!
* **Mace with `Wind Burst` Enchantment (`Anemo Swirl`):** Striking an enemy with a Mace enchanted with `Wind Burst I–III` inflicts bonus Anemo damage (`2.0 + 1.5 * level` HP) and applies **`1.0U` / `1.5U` / `2.0U` Anemo** (bypassing ICD), immediately triggering a **5.0m Swirl** blast!
* **`Thorns` Armor Elemental Reflection (`0.5U / level`):** When an entity wearing `Thorns` armor (or a Guardian with natural spikes) is struck while carrying an active elemental aura, innate element, or burning fire, its Thorns retaliation reflects **`0.5U × Thorns Level`** of that element directly back onto the attacker (bypassing ICD)!
