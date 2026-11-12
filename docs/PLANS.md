# Velorise's Staves & Elements — Roadmap & Development Plans

---

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

### 8. [Bugfix/Fabric] Investigate Anvil Sword Imbuing on Fabric 1.21.1
* **Observation:** Sword imbuing with Elemental Essences via Anvil works flawlessly on NeoForge 1.21.1 (driven by `AnvilUpdateEvent`), but appears unresponsive or fails to produce output on Fabric 1.21.1.
* **Root Cause Investigation:** On Fabric, anvil manipulation relies on `AnvilMenuMixin.java` injecting into `createResult(CallbackInfo ci)`. Check:
  1. Whether `velorise$handleSwordImbue` correctly syncs result slot and XP cost across client and server (`ItemCombinerMenu.resultSlots.setItem` vs `AnvilMenu.onTake`).
  2. Verify if Fabric mixin applies properly or if `createResult` gets called/reset by other menu methods or vanilla item combiner logic.
  3. Ensure custom data / components (`TAG_IMBUED_ELEMENT`) are properly read and written in Fabric environment.

---

### 9. [Bugfix] Frozen Mob Desync: Movement & Attacks While Frozen Statue Pose Persists
* **Issue:** Occasionally, a mob inflicted with Frozen reaction (Cryo + Hydro) resumes moving across the ground and attacking the player while its model remains locked in a statue pose with the ice crystal outline still active.
* **Root Cause Analysis:**
  1. **Server AI & Navigation:** Server-side `enforceFrozenPose` applies `mob.setNoAi(true)` and `mob.getNavigation().stop()`, but vanilla AI or goal selectors may reset navigation upon taking damage if `setNoAi(true)` is interrupted or if server freeze timers (`FROZEN_UNTIL` / `FROZEN_GAUGES`) expire before client-side packet sync.
  2. **Client/Server Timer Desync:** Client `ClientAuraTracker` tracks `frozenUntil = now + remMs`. If the server unfreezes the mob without sending an immediate unfreeze packet (`isFrozen = false`), the client continues rendering the mob in frozen pose (`FrozenRenderState`) while the server entity is freely walking and attacking.
  3. **Packet Enforcement:** Ensure server actively sends an explicit packet when freeze ends, and cancels target attack goals / melee interactions throughout the entire freeze duration.

---

### 10. [QoL/Immersion] Custom Death Messages for Tamed Pets & Elemental Combos
* **Goal:** Extend custom elemental death messages (`death.attack.velorise_staves.*`) to support tamed pets (`TamableAnimal` such as Wolves, Cats, and Foxes):
  * **Pet Killed by Reaction:** If a player's pet perishes from an elemental reaction (e.g. Overloaded explosion, Burgeon core blast, Electro-Charged zap, or Shatter), display the styled elemental death message in chat (e.g. `<PetName> was blown apart by Overloaded`) rather than falling back to vanilla generic strings.
  * **Elemental Pet Combat Feats:** Attribute elemental reaction kills triggered by the pet's bypass-ICD bites to the pet and its owner in combat logs.

---

### 11. [Advancement] "A Good Pet!" — Tame an Omni-Mutated Companion
* **Goal:** Award a dedicated challenge advancement when a player successfully tames a Wolf or Fox possessing the ultra-rare Omni Mutation.
* **Advancement Details:**
  * **ID:** `velorise_staves:tame_omni_pet`
  * **Title:** `A Good Pet!`
  * **Description:** `Tame a loyal Wolf or Fox imbued with the legendary Omni Mutation.`
  * **Frame:** `challenge` (Spiked golden frame with fanfare audio trigger).
  * **Trigger:** Listens to `TameAnimalTrigger` checking `ElementalGaugeManager.isOmniInnate(animal)`.











