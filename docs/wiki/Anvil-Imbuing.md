# Anvil Weapon Imbuing

For players who prefer close-quarters melee combat over ranged spellcasting, the **Anvil Imbuing System** (`SwordImbueHelper.java`) allows you to infuse any standard sword (including modded `SwordItem` weapons) with an elemental essence, empowering full-strength melee strikes with `1.0U` elemental aura applications and reaction scaling.

---

## How to Imbue Swords

Imbuing requires an **Anvil**, an **Elemental Essence**, and **3 levels of experience**:

1. Place an **Anvil** and open its interface.
2. Place any sword (Wooden, Stone, Iron, Golden, Diamond, Netherite, or any modded `SwordItem`) in the **left slot**.
3. Place **1 Elemental Essence** (any of the 7 elements, or <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/omni_element.png" width="16" height="16" /> [**Omni Element**](Relics-and-Utilities#omni-element)) in the **right slot**.
4. Retrieve the imbued sword from the output slot for a base cost of **3 XP levels** (**4 XP levels** if simultaneously renaming or clearing a custom name, consuming `1` essence). All existing enchantments, durability, and Netherite properties are 100% preserved.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/crafting/craft_anvil_imbuing.png" width="460" alt="In-Game Anvil Imbuing UI" />
  <br/><sub><i>In-Game Anvil UI: Imbuing a Netherite Sword with an elemental essence for 3 XP levels.</i></sub>
</div>

---

## Critical Imbuing Rules & Restrictions

* **Full-Strength Strike Requirement (`>= 90% / 0.9F` Charge):** An imbued sword only applies its `1.0U` elemental aura (bypassing standard ICD) and triggers EGT reactions on **full-strength swings** when the vanilla attack recharge indicator reaches at least **90% (`0.9F`)**. Rapid spam-clicking deals normal physical damage without discharging the elemental aura.
* **Switching Elements vs. Duplicate Element Prevention:**
  * You can freely **overwrite/switch** an already-imbued sword to a **different element** (e.g. switching a `Cryo` sword to `Pyro` or `Omni`) in the Anvil for 3 XP levels.
  * By design, the Anvil **rejects re-imbuing a sword with the exact same element it already carries** (`if (targetKey.equals(getImbuedKey(left))) return null;`) so players never waste essences and XP levels on a redundant action.
* **Fire Aspect Enchantment Conflict & Suppression:**
  * **Anvil Prevention:** A sword carrying the vanilla `Fire Aspect` enchantment **cannot** be imbued with non-Pyro elements (`Hydro`, `Cryo`, `Electro`, `Dendro`, `Anemo`, `Geo`, or `Omni`), and an already non-Pyro imbued sword cannot have a `Fire Aspect` book applied to it in an Anvil (`isFireAspectElementConflict`). `Fire Aspect` swords only accept **Pyro Element**.
  * **Tooltip Strikethrough Safeguard:** If a non-Pyro imbued sword acquires `Fire Aspect` via commands or external means, its imbued element badge is rendered in **dark gray strikethrough text** and its non-Pyro aura is suppressed in combat so `Fire Aspect` takes precedence cleanly.
* **Excluded from [Elemental Resonance](Elements-and-Reactions#elemental-resonance-system-all-8-resonances) & 4-Staff Cap:** Imbued swords are pure melee weapons (`"Full-charge swings apply 1.0U (no Resonance)"`)—they do **not** activate [Elemental Resonances](Elements-and-Reactions#elemental-resonance-system-all-8-resonances) and never count toward the 4-Staff [**Arcane Overload**](Elements-and-Reactions#arcane-overload-4-staff-inventory-cap) inventory limit.
* **Guaranteed Physical Damage Against Magic-Immune Mobs:** When an imbued sword strikes an entity that is immune to magic/elements (such as an [**Omni-Mutated Mob**](Elements-and-Reactions#2-mob-mutation-rules-7-default-6-elemental--1-omni), a same-element innate mob, or a fire-immune mob struck by a Pyro sword), **the sword's full base physical melee damage is always dealt!**
* **Omni-Imbued Weapon Intelligence (`Imbued: Omni Adaptive`):** When a sword is imbued with an [**Omni Element**](Relics-and-Utilities#omni-element), it dynamically scans the target on every full-strength swing (`OmniOrbEntity.chooseBestElementForTarget`) and automatically selects the optimal counter-element (e.g. choosing `Pyro` against Cryo/[Frozen](Elements-and-Reactions#frozen) or Hydro targets for `2.0x Melt` / `1.5x Vaporize`, `Hydro` against Pyro targets for `2.0x Vaporize`, or `Electro` against Dendro/Quickened targets).

---

## Elemental Properties on Full Melee Strikes (`>= 90%` Charge)

| Blade Infusion | Applied Aura | On-Hit Combat Behavior |
| :--- | :---: | :--- |
| **Pyro Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/pyro.png" width="16" height="16" /> Pyro (`1.0U`) | Ignites targets for **4 seconds** (ensuring cooked meat drops on fatal strikes). Striking a Cryo/[Frozen](Elements-and-Reactions#frozen) enemy triggers **2.0x Forward Melt**, and striking a wet enemy triggers **1.5x Reverse Vaporize**, directly multiplying your sword's physical hit damage! |
| **Cryo Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/cryo.png" width="16" height="16" /> Cryo (`1.0U`) | Applies Cryo aura (`-15%` move/attack speed). Striking an entity standing in water or rain **freezes them solid** immediately; striking a Pyro target triggers **1.5x Reverse Melt**. |
| **Electro Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="16" height="16" /> Electro (`1.0U`) | Delivers a `1.0U` Electro charge. Triggers [**Superconduct**](Elements-and-Reactions#superconduct) (`-40%` Physical RES shred for 12s) on Cryo targets, [**Aggravate**](Elements-and-Reactions#aggravate) bonus damage on Quickened targets, or [**Electro-Charged**](Elements-and-Reactions#electro-charged) arcs on wet targets. |
| **Dendro Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/dendro.png" width="16" height="16" /> Dendro (`1.0U`) | Applies `1.0U` Dendro aura. Striking a wet enemy immediately spawns a **Dendro Core ([Bloom](Elements-and-Reactions#bloom))**; striking a Quickened enemy adds [**Spread**](Elements-and-Reactions#spread) bonus damage. |
| **Hydro Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="16" height="16" /> Hydro (`1.0U`) | Inflicts `1.0U` Hydro. Striking a Pyro-afflicted enemy triggers a **2.0x Forward Vaporize** melee crit! |
| **Anemo Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="16" height="16" /> Anemo (`1.0U`) | Executes an Anemo slash that triggers [**Swirl**](Elements-and-Reactions#swirl) on existing auras, spreading them across a `5.0m AoE`. |
| **Geo Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/geo.png" width="16" height="16" /> Geo (`1.0U`) | Triggers [**Shatter**](Elements-and-Reactions#shatter) against [Frozen](Elements-and-Reactions#frozen) targets for heavy bonus damage, and dislodges [**Crystallize Shield Shards**](Relics-and-Utilities#crystallize-shield-shards) (`15s` elemental Absorption shield). |
| **Omni Blade** | <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/omni.png" width="16" height="16" /> Omni (`1.0U`) | Dynamically chooses the highest-value reaction element against the target's current state on every full-strength swing. |

---

## In-Game Demonstration

The following clip illustrates imbuing a sword with **Cryo Element** at an anvil for 3 XP levels, then striking a zombie wading in water to trigger an immediate freeze:

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/cryo_imbued_sword_freeze.gif" width="480" alt="Cryo Sword Freeze Demo" />
</div>
