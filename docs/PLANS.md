# Velorise's Staves & Elements — Roadmap & Development Plans

---

## 🔧 Fixed & Resolved

### 8. [Fixed] Anvil Sword Imbuing & Melee Combat on Fabric 1.21.1
- **Status:** ✅ FIXED — released in Fabric `1.0.0.1`
- **Root Cause & Fix:**
  1. **Anvil Imbuing (`AnvilMenuMixin.java` & `SwordImbueHelper.java`):**
     * `AnvilMenuMixin.java` injects at `createResult()` (`@At("HEAD")`), setting `resultSlots.setItem(0, res.output())`, `cost.set(res.cost())`, and `repairItemCountCost = res.materialCost()`, and explicitly validates output pickup via `mayPickup()`.
     * Why it appeared unresponsive in the earlier Anvil test: the Diamond Sword in the left slot was **already imbued with Cryo**, and `SwordImbueHelper.tryBuildAnvilResult` rejects re-imbuing a sword with the *exact same element* (`if (targetKey.equals(getImbuedKey(left))) return null;`) — by design, to prevent wasting essences.
  2. **Combat Elemental Application (`PlayerAttackMixin.java` & `ElementalGaugeManager.java`):**
     * **Root Cause 1 — Vanilla `Player.attack()` Cooldown Reset Timing:** In vanilla/Fabric 1.21.1, `Player.attack(Entity)` calls `this.resetAttackStrengthTicker()` **before** invoking `target.hurt(...)`. Forge 1.20.1 and NeoForge 1.21.1 patch this to move the reset **after** `target.hurt()`. On Fabric, `attacker.getAttackStrengthScale(0.5F)` always returned `0.0F` inside `actuallyHurt`, causing imbued swords to silently skip elemental application on every swing.
     * **Root Cause 2 — `Player.actuallyHurt()` PvP Override:** `Player` overrides `LivingEntity.actuallyHurt(DamageSource, float)` without calling `super`, so `LivingEntityDamageMixin` never fired for PvP damage on Fabric.
     * **Fix:** Added `PlayerAttackMixin` (Fabric 1.21.1) to snapshot `getAttackStrengthScale(0.5F)` at `@At("HEAD")` of `Player.attack()` via `ElementalGaugeManager.recordPreAttackStrength(...)` before the ticker resets, and hooked `Player.actuallyHurt` via `modifyFabricIncomingDamage(...)` for full PvE + PvP parity.

---

## 📋 Planned

### 1. [QoL] Jade HUD Environmental Wet / Hydro Indicator
* **Goal:** Display an ambient indicator (e.g. `💧 Wet (Ambient)` or `Hydro (Env)`) on Jade when a mob is submerged in water or standing in rain.
* **Implementation:** Append a lightweight tag `tag.putBoolean("vs_env_wet", true)` in `AuraServerDataProvider` on-demand without creating persistent aura timer objects on ocean entities.

---

### 2. [Balance] Nerf Recipe for Void Translocator (Teleport Wand)
* **New 3x3 Shaped Recipe:**
  * Slot 3 (Top-Right): `minecraft:ender_pearl`
  * Slot 5 (Center): `velorise_staves:electro_element` *(replaces Stick)*
  * Slot 7 (Bottom-Left): `minecraft:blaze_rod` *(replaces Stick)*
* **Reason:** Balances teleport accessibility and directly reflects the wand's self-inflicted Electro static discharge mechanic.

---

### 3. [Recipe] Adjust Guardian's Lens Crafting Recipe
* **Change:** Replace `minecraft:heart_of_the_sea` &rarr; `velorise_staves:hydro_element`.
* **Reason:** Aligns crafting components with the mod's elemental essence progression and removes dependence on rare buried treasure loot.

---

### 4. [Visual] Corner Indicator for Element-Imbued Swords
* **Goal:** Add a miniature elemental emblem/badge in the corner of the sword icon across Inventory, Hotbar, and JEI/REI/EMI slots.
* **Reason:** Imbued swords currently share the default vanilla sword texture and are only distinguishable via tooltips. A corner overlay provides instant visual recognition of the infused element.

---

### 5. [UI/QoL] Multi-Line Word Wrap for Resonance Inventory Tooltips
* **Issue:** Hovering over active Elemental Resonance icons in the Inventory currently renders the entire description string on a single unbroken line (e.g. `-40% Hydro Aura | Electro reactions: -1s Staff CD & +1 Durability (5s ICD)`), stretching horizontally across almost the whole screen.
* **Goal:** Wrap/split resonance tooltips into compact multi-line bullet points:
  * Title: `Electro: High Voltage` (Element color)
  * Line 1: `• -40% Hydro Aura Duration`
  * Line 2: `• Electro Reactions: -1s Staff CD & +1 Durability (5s ICD)`
* **Implementation:** Split at the ` | ` delimiter or use `mc.font.split(description, maxWidth)` in `ClientAuraRenderer.java` (`renderResonanceInventory`), keeping tooltips neat, readable, and compact.

---

### 6. [UI/Visual] Add Hydro Element Indicator & Tooltip to Vanilla Trident
* **Goal:** Render a miniature Hydro emblem or append an elemental tooltip line (e.g. `✦ Element: Hydro (Resonance Partner)`) on the Vanilla Trident.
* **Reason:** In Velorise's Staves, the Vanilla Trident inherently acts as a Hydro partner for activating Hydro Elemental Resonance (when carrying at least 1 Arcane Staff) and applies Hydro in combat. Adding a clear tooltip and visual indicator eliminates confusion and highlights this vanilla synergy.

---

### 7. [UI/Tooltip] Guardian's Lens Max HP Scaling Tooltip Clarification
* **Insight:** The in-game tooltip `1-2 ♥ x 10` (Basic) and `1.5-3 ♥ x 10` (Netherite) is completely dynamic and accurately reflects the 2.5s channeling beam (50 ticks / 5-tick interval = 10 hits, each hitting for 10% / 15% of caster Max HP).
* **Issue:** Players cannot deduce from the raw numbers that their damage is actively scaling with their own Max HP.
* **Goal:** Append an explanatory subtitle / tooltip line:
  `✦ Scales with Caster's Max HP (10%/hit | 15% Netherite)`
  making the HP-stacking build synergy obvious (especially when paired with Hydro Resonance +20% HP or Health Boost armor/buffs).

---

### 9. [Bugfix] Frozen Mob Desync: Movement & Attacks While Frozen Statue Pose Persists
* **Issue:** Occasionally, a mob inflicted with Frozen reaction (Cryo + Hydro) resumes moving across the ground and attacking the player while its model remains locked in a statue pose with the ice crystal outline still active.
* **Root Cause Analysis:**
  1. **Server AI & Navigation:** Server-side `enforceFrozenPose` applies `mob.setNoAi(true)` and `mob.getNavigation().stop()`, but vanilla AI or goal selectors may reset navigation upon taking damage if `setNoAi(true)` is interrupted or if server freeze timers (`FROZEN_UNTIL` / `FROZEN_GAUGES`) expire before client-side packet sync.
  2. **Client/Server Timer Desync:** Client `ClientAuraTracker` tracks `frozenUntil = now + remMs`. If the server unfreezes the mob without sending an immediate unfreeze packet (`isFrozen = false`), the client continues rendering the mob in frozen pose (`FrozenRenderState`) while the server entity is freely walking and attacking.
  3. **Packet Enforcement:** Ensure server actively sends an explicit packet when freeze ends, and cancels target attack goals / melee interactions throughout the entire freeze duration.

---

### 10. [QoL/Immersion] Elemental Death Messages for Mutated Pets & Dynamic Omni Rainbow Cycling
* **Current Issue:**
  1. When a player's mutated pet (e.g. Omni Wolf or Pyro Wolf) dies, vanilla displays generic unstyled death messages (e.g. `Wolf was shot by Pillager`), completely omitting its element and elemental colors.
  2. In `ElementalDeathMessageHelper.java`, the `Omni` element name is currently hardcoded to static gold (`ChatFormatting.GOLD`) instead of dynamically cycling through elemental rainbow colors.
* **Goal & Implementation:**
  * **Elemental Pet Death Formatting:** Format the deceased pet's name with its elemental prefix and color code in chat (e.g. `Dev's [Cryo] Wolf was slain by Pillager` or `Dev's [Omni] Wolf was blown apart by Overloaded`).
  * **Dynamic Omni Rainbow / Chromatic Animation:**
    * In death messages (e.g. `Player was slain by Omni Skeleton` or `Omni Wolf was slain by Pillager`), tooltips, and chat, render the `Omni` tag with an animated/time-based chromatic color cycle (cycling smoothly through Pyro Red &rarr; Hydro Blue &rarr; Cryo Cyan &rarr; Electro Purple &rarr; Dendro Green &rarr; Anemo Teal &rarr; Geo Gold) or multi-color gradient characters (`§cO§9m§bn§di`).
  * **Symmetrical Killer & Victim Formatting:** Update `formatVictimDisplayName` alongside `formatKillerDisplayName` so both attacker and victim display full elemental prefixes and styling across all death log events.

---

### 11. [Advancement] "A Good Pet!" — Tame an Omni-Mutated Companion
* **Goal:** Award a dedicated challenge advancement when a player successfully tames a Wolf or Fox possessing the ultra-rare Omni Mutation.
* **Advancement Details:**
  * **ID:** `velorise_staves:tame_omni_pet`
  * **Title:** `A Good Pet!`
  * **Description:** `Tame a loyal Wolf or Fox imbued with the legendary Omni Mutation.`
  * **Frame:** `challenge` (Spiked golden frame with fanfare audio trigger).
  * **Trigger:** Listens to `TameAnimalTrigger` checking `ElementalGaugeManager.isOmniInnate(animal)`.

---

### 12. [Staff/Feature] New Geo Staff: Dominance of Earth / Jade Shield & Geo Construct (Zhongli Reference)
* **Concept:** A high-tier defensive and tactical Geo staff inspired by Zhongli (*Genshin Impact*), providing both unyielding survivability and area-of-effect elemental application.
* **Ability 1 (Tap / Quick Cast - Geo Stele):**
  * Summons a monolithic **Geo Stele (Stone Pillar)** at the target location.
  * Periodically pulses resonant Geo shockwaves in a 6-block radius, dealing Geo damage and applying 1.0U Geo to mobs within range.
  * Resonates with nearby Geo constructs or Crystallize shields, extending pulse range and triggering frequent Crystallize reactions.
* **Ability 2 (Hold / Charge Cast - Jade Shield):**
  * Forms a fortified **Jade Shield** around the caster that scales with the player's Max Health.
  * Possesses **150% absorption effectiveness** against all Elemental and Physical damage.
  * While active, creates a close-range aura (3-block radius) that decreases surrounding enemy Elemental and Physical Resistances by 20%.

---

### 13. [Combat/Balance] Align Resistance Formulas with Authentic Genshin Impact Mechanics (Halved Below 0%)
* **Goal:** Adopt the official *Genshin Impact* resistance scaling curves for both Physical and Elemental resistances across all combat calculations.
* **Authentic Resistance Formula:**
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
  * Update Wiki ([Elements-and-Reactions.md](file:///c:/Documents/Coder/Minecraft/myMod/velorise_staves/docs/wiki/Elements-and-Reactions.md)) table and notes to reflect the 3-tier Genshin piecewise resistance formula.











