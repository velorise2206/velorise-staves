# FAQ & Compatibility

Frequently asked questions regarding gameplay mechanics, compatibility, performance, and server deployment for **Velorise's Staves & Elements**.

---

### Does this mod work on dedicated multiplayer servers?
* **Yes, fully supported.** All aura timers, environmental block interactions, custom particle displays, audio cues, and server config multipliers (`ConfigSyncPayload`) are synchronized between server and client via custom network payloads.
* Install the mod jar into both the server's `mods` folder and each player's client `mods` folder.

---

### Why does the Anvil show no output when I try to imbue my Sword?
* Check two intentional design rules in `SwordImbueHelper.java`:
  1. **Already Imbued with the Same Element:** If your sword is *already imbued* with that exact same element (e.g. placing a `Cryo`-imbued Diamond Sword + `Cryo Element`), the Anvil intentionally produces no output so you don't waste essences and XP levels. Placing a **different** element (e.g. `Pyro` or `Omni`) will overwrite the element normally for 3 XP levels.
  2. **Fire Aspect Conflict:** Swords enchanted with `Fire Aspect` strictly reject non-Pyro elements (`Hydro`, `Cryo`, `Electro`, `Dendro`, `Anemo`, `Geo`, `Omni`) and can only be imbued with **Pyro Element**.

---

### How do I break free when Frozen by Cryo + Hydro?
* When your player is [Frozen](Elements-and-Reactions#frozen), your movement and camera rotation are locked and a **`[SPACE] Break Free`** prompt appears on screen.
* Rapidly mash **`[Spacebar]`** (Jump) to struggle out of the ice: each press shaves **0.3 seconds (6 ticks)** off your remaining freeze duration (`0.15s / 3-tick` cooldown per press) with ice-cracking audio and snowflake bursts!

---

### How is the Wind Staff crafted on Forge 1.20.1 (which doesn't have Trial Chambers)?
* **Full 15-Staff Backport:** On Forge 1.20.1, Velorise's Staves registers custom `velorise_staves:breeze_rod` and throwable `velorise_staves:wind_charge` items!
* Simply harvest **Anemo Element** (`anemo_element`) from Phantoms, Vexes, or Anemo-mutated mobs, then craft:
  * `1x Anemo Element` (shapeless) $\rightarrow$ **`4x Wind Charges`**
  * `1x Anemo Element + 1x Blaze Rod` (shapeless) $\rightarrow$ **`1x Breeze Rod`**

---

### Is there support for recipe viewers like JEI, REI, and EMI?
* **Yes (`VeloriseJeiPlugin.java`).** Built-in recipe integration displays:
  * Standard Crafting Table staff & essence conversion recipes.
  * Smithing Table Netherite upgrade & **Omni Staff transformation** recipes (preserving enchantments).
  * Anvil [**Sword Imbuing**](Anvil-Imbuing#how-to-imbue-swords) recipes (for all 7 elements + Omni across all sword tiers) and **Staff Essence Repair** examples, plus JEI information pages for Elemental Essences.

---

### How do I see what aura is active on a mob or how much HP a Geo Pillar has?
* **Jade / Waila Integration (`VeloriseStavesJadePlugin.java`):**
  * Pointing your crosshair at any mob displays active elemental auras, remaining gauge duration, and `Omni ∞` immunity badges.
  * Pointing at a [**Geo Fang Pillar**](Environmental-Interactions#3-geo-constructs-fang-pillars--stone-steles) displays its live **Geo Construct Health** bar (`HP / Max HP`).

---

### How does Jade display mobs standing in water or rain (`Wet (Env)`)?
* **Intentional Optimization & Performance Safeguard:**
  * Attaching persistent aura objects and continuous network sync packets to every fish, squid, and drowned in oceans and rivers would cause unnecessary server-side packet traffic.
  * Jade HUD automatically detects environmental wetness (**Submerged in Water**, **Standing in Rain**, or **Bubble Columns**) and displays a light-blue **`Wet (Env)`** badge without spamming persistent server aura packets! Striking any `Wet (Env)` mob with Cryo, Electro, Pyro, Dendro, or Geo triggers [**Frozen**](Elements-and-Reactions#frozen), [**Electro-Charged**](Elements-and-Reactions#electro-charged), [**Vaporize**](Elements-and-Reactions#vaporize), [**Bloom**](Elements-and-Reactions#bloom), or [**Crystallize**](Elements-and-Reactions#crystallize) 100% reliably.

---

### Is this mod compatible with Better Combat and Shaders?
* **Better Combat:** Fully compatible. Element-imbued swords retain all weapon categories and animations while applying `1.0U` on full-charge swings (`>= 90%`).
* **Shaders (Iris, Oculus, OptiFine):** All 3D electric arcs, concentric Geo waves, [Dendro Core](Elements-and-Reactions#dendro-reaction-system-bloom--catalyze) buds, Omni Prism Orbs, and frozen ice overlays render cleanly under modern shader pipelines (*Complementary*, *BSL*, *MakeUp Ultra Fast*).

---

### What are the differences between NeoForge, Forge, and Fabric versions?
* **100% Feature Parity Across All 3 Loaders:**
  * **NeoForge 1.21.1** (Java 21)
  * **Fabric 1.21.1** (Java 21)
  * **Forge 1.20.1** (Java 17 — includes backported [Wind Staff](Staves-List#wind-staff), Breeze Rod, and Wind Charge)
