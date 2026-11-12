# Velorise's Staves & Elements — Issue & Roadmap Backlog

This document tracks planned features, enhancements, and quality-of-life improvements queued for upcoming updates (v1.1.0+).

---

## 📌 Planned Enhancements & QoL Tasks

### 1. [QOL-01] Jade HUD Environmental Wet / Hydro Indicator
* **Type:** Feature Request / Quality of Life (QoL)
* **Status:** ⏳ Queued for Next Update (v1.1.0)
* **Affected Components:** 
  * `fabric-1.21.1/src/main/java/com/velorise/staves/compat/jade/AuraServerDataProvider.java`
  * `neoforge-1.21.1/src/main/java/com/velorise/staves/compat/jade/AuraServerDataProvider.java`
  * `forge-1.20.1/src/main/java/com/velorise/staves/compat/jade/AuraServerDataProvider.java`
* **Current Mechanics & Why It Is Currently Suppressed:**
  * When entities are standing in water, rain, or bubble columns, the mod's combat engine evaluates their environmental Hydro status on-demand (`ElementalGaugeManager.isEntityInWater(living)`).
  * The mod **intentionally avoids** creating persistent `GaugeState` aura objects or sending continuous network sync packets for every entity in water. This is a critical performance safeguard that prevents immense packet storms and TPS drops across vast oceans and rivers (which contain hundreds of fish, squids, and drowned).
  * All elemental reactions (**Freeze**, **Electro-Charged**, **Bloom**, **Vaporize**, **Crystallize**) trigger 100% reliably and instantaneously when striking any wet mob.
* **Task Specification for Next Version:**
  * In `AuraServerDataProvider.appendServerData`:
    * If `living.isInWaterRainOrBubble()` or `ElementalGaugeManager.isEntityInWater(living)` is `true`, and no explicit Hydro gauge exists in `ENTITY_GAUGES`, append a lightweight tag:
      ```java
      tag.putBoolean("vs_env_wet", true);
      ```
  * In `AuraComponentProvider.appendTooltip`:
    * If `tag.getBoolean("vs_env_wet")` is `true`, render a lightweight ambient icon:
      `💧 Wet (Ambient)` or `Hydro (Env)`
    * This visual indicator lets players know the mob is primed for elemental reactions without attaching heavy persistent aura timers to ocean entities.
