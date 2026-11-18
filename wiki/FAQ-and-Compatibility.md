# FAQ, Keybinds & Mod Compatibility

---

## ❓ Frequently Asked Questions (FAQ)

### Q1: Why can't I craft or absorb Mavuika, Furina, Zhongli, Venti, Raiden, Nahida, or Tsaritsa into the Omni Staff?
**A:** Those 7 weapons are **Archon Signature Weapons (`isArchon() == true`)**. Because Archon weapons use complex multi-input kits (stance switches, vehicles, 3-stage bow draws, viewfinders, and dual bursts), the **Omni Staff** only accepts **Non-Archon Staves** (the 11 standard staves + the 3 non-Archon Genshin character staves: **Neuvillette's *Guardian Lens***, **Xilonen's *Peak Patrol Song***, and **Layla's *Nights of Formal Focus***, plus **Vanilla Trident** for Hydro and **Void Translocator** for Electro).

### Q2: Why do my staves stop working when I carry more than 4 staves?
**A:** Carrying more than **4 combat staves** (configurable via `max_staves_in_inventory`) triggers **Elemental Overload**, reflecting Genshin Impact's 4-character party limit. Store extra staves in a chest/shulker box or set `max_staves_in_inventory = 0` in the config to disable the limit. Note that **Element-Imbued Swords**, **Void Translocator**, **Magic Mirror**, and **Wind Glider** do **not** count toward the 4-staff cap!

### Q3: How do I cast an Elemental Burst `[Q]` from my Offhand?
**A:** If your **Main Hand** is holding a normal item (such as an Imbued Sword, food, blocks, or a standard non-character staff) and your **Offhand** holds an Archon/Character weapon (e.g., *Engulfing Lightning*, *Splendor*, *Vortex Vanquisher*, *The Daybreak Chronicles*, *Guardian Lens*), pressing **`[R]`** or **`Right-Click`** will seamlessly trigger the offhand character's Burst or Skill!

### Q4: How do I defeat an `Omni ∞` (Omni-Innate) mutated mob that says `IMMUNE` to all my staves?
**A:** **Omni-Innate (`Omni ∞`)** mobs are 100% immune to all Elemental and Magic damage! Switch to a **Physical Melee Sword** or **Vanilla Bow/Crossbow** to deal physical damage — or, if it is a tamable Wolf/Fox, feed it an **Omni Element** (`omni_element`) to tame it as an elemental-immune pet!

### Q5: How do I perform Mavuika's infinite Dash Cancel (`CD CD CC`) on the Flamestrider bike?
**A:** While riding the Flamestrider, you can dash twice in a row with `[Sprint]` before hitting a `1.5s` (`30t`) dash cooldown. However, if you hold **Left-Click** to enter the **Donut Charged Attack spin** for at least **`0.4s` (`8 ticks`)**, both the Client and Server immediately reset your dash cooldown to `0`, allowing you to dash-cancel again right away!

### Q6: How do I build an Overcharged Tesla Coil?
**A:** Place a vanilla **Lightning Rod** directly on top of a **Redstone Block**. It will passively zap nearby mobs within `6m` every `2.5s`. Strike the Lightning Rod with any **Electro attack** (*Arc Caster, Engulfing Lightning, Dragonfire Scepter, Shulker Seeker,* or *Electro-Imbued Sword*) to overcharge it for `15s` (`8m` multi-target zaps every `1s` + unlocks the *High-Voltage Architect* advancement)!

---

## ⌨️ Complete Keybind Reference

All keybinds can be rebound in Minecraft's standard **Options ➔ Controls ➔ Key Binds** menu under the **Velorise's Staves** category:
- **`[R]` (`key.velorise_staves.archon_ulti`):** Cast Elemental Burst `[Q]` (Mavuika, Furina, Zhongli, Venti, Raiden, Nahida, Tsaritsa, Neuvillette, Xilonen, Layla).
- **`[Tab]` (`key.velorise_staves.omni_wheel`):** Open the **Omni Element Wheel** while holding the Omni Staff.
- **`[V]` (`key.velorise_staves.character_voice`):** Open the **Character Voice Screen** (Zhongli voice lines & language selector).
- **`[Space]` in Midair:** Deploy / Fold the **Wings of First Flight (Wind Glider)**.
- **`[Left-Click]` in Midair:** Execute a **Plunging Attack**.

---

## 🤝 Mod Compatibility Matrix

| Mod / Ecosystem | Loader Support | Integration Details |
| :--- | :--- | :--- |
| **Jade / Waila** | NeoForge, Fabric, Forge | Built-in `VeloriseStavesJadePlugin` displays active Elemental Auras, exact Gauge Units (`U`), remaining timers, `Omni ∞` badges, and **Geo Construct HP / Lifetime** directly in the Jade HUD! |
| **JEI / REI / EMI** | NeoForge, Fabric, Forge | Displays all Shaped/Shapeless Crafting recipes, Elemental Rod transmutations, and Smithing Table Netherite upgrades. |
| **Iris / Oculus Shaders** | NeoForge, Fabric, Forge | Custom `ShieldRenderTypes` (`TRANSLUCENT_NO_DEPTH_WRITE`, `TRANSLUCENT_DEPTH_WRITE`, `METEOR_SOLID`) render 3D shields, lightning arcs, Nahida domes, and meteors cleanly without shader depth-buffer artifacts. |
| **Sodium / Embeddium / Rubidium** | NeoForge, Fabric, Forge | Zero custom chunk mesh hooks; all particle and vertex batches are fully compatible with Sodium's rendering pipeline. |
| **First-Person Model / Better First Person** | NeoForge, Fabric, Forge | Dedicated 1st-person vs 3rd-person camera checks (`CameraType.FIRST_PERSON`) prevent 3D idle props or sunglasses overlays from clipping the camera. |
