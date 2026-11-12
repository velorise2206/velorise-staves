# FAQ & Compatibility

Frequently asked questions regarding compatibility, performance, and server deployment for **Velorise's Staves & Elements**.

---

### Does this mod work on dedicated multiplayer servers?
* **Yes, fully supported.** All aura timers, environmental block interactions, custom particle displays, and audio cues are synchronized between server and client via custom network payloads.
* Install the mod jar into both the server's `mods` folder and each player's client `mods` folder.

---

### Is there support for recipe viewers like JEI, REI, and EMI?
* **Yes.** Built-in recipe integration displays:
  * Standard Crafting Table staff recipes.
  * Smithing Table Netherite upgrade templates.
  * Anvil weapon imbuing recipes with live elemental essence preview slots.

---

### How do I see what aura is active on a mob?
* **Jade Integration:** When pointing your crosshair at any mob with an active elemental aura, Jade displays an aura status bar indicating the exact element type, remaining duration, and active reaction multiplier.

---

### Is this mod compatible with Better Combat?
* **Fully compatible.** Elemental swords infused at an anvil retain all Better Combat weapon categories, attack animations, and sweep ranges while simultaneously applying their elemental aura upon impact.

---

### Can I run this with Shaders (Iris, Oculus, OptiFine)?
* **Yes.** All magic projectile trails, beams, and impact particles utilize standard Minecraft render layers. They render properly under modern shader pipelines such as *Complementary Shaders*, *BSL*, and *MakeUp Ultra Fast* without graphical artifacting.

---

### What are the differences between Forge, NeoForge, and Fabric versions?
* **100% Feature Parity.** All 3 builds share the exact same gameplay logic, damage formulas, sound effects, and configurations:
  * **NeoForge 1.21.1** (Recommended for modern gameplay and active modding ecosystems)
  * **Forge 1.20.1** (Designed for large legacy modpacks)
  * **Fabric 1.21.1** (Optimized for lightweight, high-framerate client setups)
