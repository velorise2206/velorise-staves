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

