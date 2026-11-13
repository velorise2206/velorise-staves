# Velorise's Staves & Elements — Roadmap & Development Plans

> Upcoming features, balance adjustments, visual polish, and open bugfixes. For resolved issues and shipped hotfixes, see [FIXED.md](FIXED.md).

## Contents
- [Part I — New Staves & Combat Reworks (Genshin References)](#part-i--new-staves--combat-reworks-genshin-references)
- [Part II — Combat Mechanics, Balance & Recipes](#part-ii--combat-mechanics-balance--recipes)
- [Part III — UI, Visuals & Tooltips](#part-iii--ui-visuals--tooltips)
- [Part IV — Immersion, Pets & Advancements](#part-iv--immersion-pets--advancements)
- [Part V — Pending Bugfixes](#part-v--pending-bugfixes)

---

## Part I — New Staves & Combat Reworks (Genshin References)

### 1. [Geo Staff] Vortex Vanquisher / Dominus Lapidis (Zhongli Reference)
* **Visual & Naming:** Reimagined as a combat staff based on Zhongli's signature weapon (**Vortex Vanquisher**), using authentic *Genshin Impact* naming for abilities and mechanics.
* **Ability 1 (Tap / Quick Cast — Stone Stele / Dominus Lapidis):**
  * Summons a tall, monolithic **Stone Stele (Geo Construct)** at the target location.
  * Periodically pulses resonant Geo shockwaves in a 6-block radius, dealing Geo damage and applying `1.0U` Geo to mobs within range.
  * Resonates with nearby Geo constructs or Crystallize shields, extending pulse range and triggering frequent Crystallize reactions.
* **Ability 2 (Hold / Charge Cast — Jade Shield):**
  * Forms a fortified **Jade Shield** around the caster that scales with the player's Max Health.
  * Possesses **150% absorption effectiveness** against all Elemental and Physical damage.
  * While active, creates a close-range aura (3-block radius) that decreases surrounding enemy Elemental and Physical Resistances by **20%**.

---

### 2. [Hydro Staff] Splendor of Tranquil Waters / Salon Solitaire (Furina Reference)
* **Visual & Naming:** Reimagined as a combat staff modeled after Furina's signature weapon (**Splendor of Tranquil Waters**), using authentic *Genshin Impact* naming and dual-alignment (Ousia / Pneuma) mechanics.
* **Dual-Mode Switching (Arkhe: Ousia ↔ Pneuma):** Players can toggle between two stances (e.g., via Crouch + Right-Click or Charged Attack):
  * **Mode 1 — Ousia (*Salon Solitaire*):**
    * Summons 3 autonomous Hydro Salon Members (*Gentilhomme Usher*, *Surintendante Chevalmarin*, and *Mademoiselle Crabaletta*) that actively attack nearby hostile mobs and apply Hydro.
    * Their attack damage scales mildly with the caster's **Max HP**.
    * On each hit, the Salon Members drain a small percentage of HP from the caster and nearby allied players, **stopping HP drain once an entity reaches the 50% Max HP cap** (mirroring Furina's signature HP drain mechanic).
  * **Mode 2 — Pneuma (*Singer of Many Waters*):**
    * Summons the *Singer of Many Waters* construct/companion, which periodically pulses **AoE Hydro healing** to the caster and nearby allies within range.

---

### 3. [Hydro Rework] Guardian's Lens: Authentic Neuvillette (*Sourcewater Droplets* & *Equitable Judgment*)
* **Goal:** Upgrade the existing **Guardian's Lens** continuous Hydro beam into an authentic *Neuvillette* combat loop (*Genshin Impact*).
* **Sourcewater Droplet Passive:**
  * Whenever the holder or a nearby ally triggers a **Hydro-related Elemental Reaction** (Vaporize, Frozen, Electro-Charged, Bloom, Hydro Swirl, Hydro Crystallize) while Guardian's Lens is carried/equipped, **Sourcewater Droplets** spawn on the ground (up to **6 active droplets** maximum).
  * **Ally Pickup:** Regular players walking near a Sourcewater Droplet can pick it up for a small flat HP heal.
* **Equitable Judgment Channeling Mechanic:**
  * **With Sourcewater Droplets Nearby:** Holding Right-Click automatically absorbs nearby Sourcewater Droplets (up to 3), **instantly completing the charge** to unleash the continuous Hydro beam immediately while **healing the caster**.
  * **Without Sourcewater Droplets:** Holding Right-Click requires a brief **few-second charge-up / channel** (mirroring Neuvillette's Charged Attack wind-up) before firing the continuous *Equitable Judgment* beam (retaining its current Max HP scaling).
* **Self HP Drain While Firing:** While channeling the *Equitable Judgment* beam, the caster continuously drains a small percentage of their own HP per beam tick **until reaching the 50% Max HP cap** (mirroring Neuvillette's Fontaine HP-fluctuation mechanic).
* **Damage Scaling Rebalance (*Past Draconic Glories* Passive):**
  * Replace the current flat `2x` HP-fluctuation multiplier (which would be overly strong with 100% uptime from self-drain and Furina) with a **3-stack Hydro Reaction bonus**.
  * Triggering distinct Hydro-related reactions (Vaporize, Frozen, Electro-Charged, Bloom, Hydro Swirl, Hydro Crystallize) grants independent stacks (up to **3 stacks**, scaling beam damage up to **~1.5x–1.6x** max), encouraging multi-element team setups over solo spamming.

---

## Part II — Combat Mechanics, Balance & Recipes

### 4. [Combat/Balance] Align Resistance Formulas with Authentic Genshin Impact Mechanics
* **Goal:** Adopt the official *Genshin Impact* resistance scaling curves for both Physical and Elemental resistances across all combat calculations.
* **Authentic 3-Tier Resistance Formula:**
  * **Negative Resistance ($RES < 0$):**
    $$\text{Multiplier} = 1.0 - \frac{RES}{2.0}$$
    *Effect:* Any resistance shred pushing RES below 0% is halved in effectiveness. For example, a mob with 10% base RES suffering from Superconduct (-40% RES) reaches $-30\%$ RES, dealing $1.0 - (-0.30 / 2.0) = 1.15$ (**+15% physical damage amplification**, aligning with Genshin instead of the current flat +30%).
  * **Standard Resistance ($0 \le RES \le 0.75$):**
    $$\text{Multiplier} = 1.0 - RES$$
  * **Extreme High Resistance ($RES > 0.75$):**
    $$\text{Multiplier} = \frac{1.0}{4.0 \times RES + 1.0}$$
    *Effect:* Diminishing returns on extreme resistances (such as Warden with 70% Anemo RES combined with innate Geo projectile buffs), preventing total invulnerability.
* **Implementation Scope:**
  * Update `getEffectivePhysicalMultiplier()` in `ElementalGaugeManager.java` across NeoForge 1.21.1, Forge 1.20.1, and Fabric 1.21.1.
  * Update `applyMobElementalResistance()` and `applyMobDamageResistances()` to universally support negative elemental RES (preparing for Jade Shield -20% all RES and future Swirl resistance shredding).
  * Update Wiki ([Elements-and-Reactions.md](wiki/Elements-and-Reactions.md)) table and notes to reflect the 3-tier Genshin piecewise resistance formula.

---

### 5. [Balance/Recipe] Nerf Recipe for Void Translocator (Teleport Wand)
* **New 3x3 Shaped Recipe:**
  * Slot 3 (Top-Right): `minecraft:ender_pearl`
  * Slot 5 (Center): `velorise_staves:electro_element` *(replaces Stick)*
  * Slot 7 (Bottom-Left): `minecraft:blaze_rod` *(replaces Stick)*
* **Reason:** Balances teleport accessibility and directly reflects the wand's self-inflicted Electro static discharge mechanic.

---

### 6. [Recipe] Adjust Guardian's Lens Crafting Recipe
* **Change:** Replace `minecraft:heart_of_the_sea` &rarr; `velorise_staves:hydro_element`.
* **Reason:** Aligns crafting components with the mod's elemental essence progression and removes dependence on rare buried treasure loot.

---

## Part III — UI, Visuals & Tooltips

### 7. [Visual] Corner Indicator for Element-Imbued Swords
* **Goal:** Add a miniature elemental emblem/badge in the corner of the sword icon across Inventory, Hotbar, and JEI/REI/EMI slots.
* **Reason:** Imbued swords currently share the default vanilla sword texture and are only distinguishable via tooltips. A corner overlay provides instant visual recognition of the infused element.

---

### 8. [UI/QoL] Multi-Line Word Wrap for Resonance Inventory Tooltips
* **Issue:** Hovering over active Elemental Resonance icons in the Inventory currently renders the entire description string on a single unbroken line (e.g. `-40% Hydro Aura | Electro reactions: -1s Staff CD & +1 Durability (5s ICD)`), stretching horizontally across almost the whole screen.
* **Goal:** Wrap/split resonance tooltips into compact multi-line bullet points:
  * Title: `Electro: High Voltage` (Element color)
  * Line 1: `• -40% Hydro Aura Duration`
  * Line 2: `• Electro Reactions: -1s Staff CD & +1 Durability (5s ICD)`
* **Implementation:** Split at the ` | ` delimiter or use `mc.font.split(description, maxWidth)` in `ClientAuraRenderer.java` (`renderResonanceInventory`), keeping tooltips neat, readable, and compact.

---

### 9. [UI/Visual] Hydro Element Indicator & Tooltip on Vanilla Trident
* **Goal:** Render a miniature Hydro emblem or append an elemental tooltip line (e.g. `✦ Element: Hydro (Resonance Partner)`) on the Vanilla Trident.
* **Reason:** In Velorise's Staves, the Vanilla Trident inherently acts as a Hydro partner for activating Hydro Elemental Resonance (when carrying at least 1 Arcane Staff) and applies Hydro in combat. Adding a clear tooltip and visual indicator eliminates confusion and highlights this vanilla synergy.

---

### 10. [UI/Tooltip] Guardian's Lens Max HP Scaling Tooltip Clarification
* **Insight:** The in-game tooltip `1-2 ♥ x 10` (Basic) and `1.5-3 ♥ x 10` (Netherite) is completely dynamic and accurately reflects the 2.5s channeling beam (50 ticks / 5-tick interval = 10 hits, each hitting for 10% / 15% of caster Max HP).
* **Issue:** Players cannot deduce from the raw numbers that their damage is actively scaling with their own Max HP.
* **Goal:** Append an explanatory subtitle / tooltip line:
  `✦ Scales with Caster's Max HP (10%/hit | 15% Netherite)`
  making the HP-stacking build synergy obvious (especially when paired with Hydro Resonance +20% HP or Health Boost armor/buffs).

---

### 11. [QoL] Jade HUD Environmental Wet / Hydro Indicator
* **Goal:** Display an ambient indicator (e.g. `💧 Wet (Ambient)` or `Hydro (Env)`) on Jade when a mob is submerged in water or standing in rain.
* **Implementation:** Append a lightweight tag `tag.putBoolean("vs_env_wet", true)` in `AuraServerDataProvider` on-demand without creating persistent aura timer objects on ocean entities.

---

## Part IV — Immersion, Pets & Advancements

### 12. [QoL/Immersion] Elemental Death Messages for Mutated Pets & Dynamic Omni Rainbow Cycling
* **Current Issue:**
  1. When a player's mutated pet (e.g. Omni Wolf or Pyro Wolf) dies, vanilla displays generic unstyled death messages (e.g. `Wolf was shot by Pillager`), completely omitting its element and elemental colors.
  2. In `ElementalDeathMessageHelper.java`, the `Omni` element name is currently hardcoded to static gold (`ChatFormatting.GOLD`) instead of dynamically cycling through elemental rainbow colors.
* **Goal & Implementation:**
  * **Elemental Pet Death Formatting:** Format the deceased pet's name with its elemental prefix and color code in chat (e.g. `Dev's [Cryo] Wolf was slain by Pillager` or `Dev's [Omni] Wolf was blown apart by Overloaded`).
  * **Dynamic Omni Rainbow / Chromatic Animation:**
    * In death messages (e.g. `Player was slain by Omni Skeleton` or `Omni Wolf was slain by Pillager`), tooltips, and chat, render the `Omni` tag with an animated/time-based chromatic color cycle (cycling smoothly through Pyro Red &rarr; Hydro Blue &rarr; Cryo Cyan &rarr; Electro Purple &rarr; Dendro Green &rarr; Anemo Teal &rarr; Geo Gold) or multi-color gradient characters (`§cO§9m§bn§di`).
  * **Symmetrical Killer & Victim Formatting:** Update `formatVictimDisplayName` alongside `formatKillerDisplayName` so both attacker and victim display full elemental prefixes and styling across all death log events.

---

### 13. [Advancement] "A Good Pet!" — Tame an Omni-Mutated Companion
* **Goal:** Award a dedicated challenge advancement when a player successfully tames a Wolf or Fox possessing the ultra-rare Omni Mutation.
* **Advancement Details:**
  * **ID:** `velorise_staves:tame_omni_pet`
  * **Title:** `A Good Pet!`
  * **Description:** `Tame a loyal Wolf or Fox imbued with the legendary Omni Mutation.`
  * **Frame:** `challenge` (Spiked golden frame with fanfare audio trigger).
  * **Trigger:** Listens to `TameAnimalTrigger` checking `ElementalGaugeManager.isOmniInnate(animal)`.

---

## Part V — Pending Bugfixes

### 14. [Bugfix] Frozen Mob Desync: Movement & Attacks While Frozen Statue Pose Persists
* **Issue:** Occasionally, a mob inflicted with Frozen reaction (Cryo + Hydro) resumes moving across the ground and attacking the player while its model remains locked in a statue pose with the ice crystal outline still active.
* **Root Cause Analysis:**
  1. **Server AI & Navigation:** Server-side `enforceFrozenPose` applies `mob.setNoAi(true)` and `mob.getNavigation().stop()`, but vanilla AI or goal selectors may reset navigation upon taking damage if `setNoAi(true)` is interrupted or if server freeze timers (`FROZEN_UNTIL` / `FROZEN_GAUGES`) expire before client-side packet sync.
  2. **Client/Server Timer Desync:** Client `ClientAuraTracker` tracks `frozenUntil = now + remMs`. If the server unfreezes the mob without sending an immediate unfreeze packet (`isFrozen = false`), the client continues rendering the mob in frozen pose (`FrozenRenderState`) while the server entity is freely walking and attacking.
  3. **Packet Enforcement:** Ensure server actively sends an explicit packet when freeze ends, and cancels target attack goals / melee interactions throughout the entire freeze duration.
