# Environmental Interactions

Unlike conventional magic mods where spells exist only as isolated projectiles, **Velorise's Staves & Elements** seamlessly links its elemental logic with Minecraft's physical environment.

---

## 1. Electrified Water

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/hydro.png" width="28" height="28" />
  &nbsp;&nbsp;&nbsp;&nbsp;<b>+</b>&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/electro.png" width="28" height="28" />
</div>

* **Mechanism:** Discharging an Electro attack (such as the <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/arc_caster.png" width="16" height="16" /> [**Arc Caster**](Staves-List#arc-caster), the <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/dragonfire_scepter.png" width="16" height="16" /> [**Dragonfire Scepter**](Staves-List#dragonfire-scepter), or an Electro-imbued sword) into a water body electrifies surrounding water blocks for 120 ticks (6 seconds).
* **Effect:** All entities submerged or wading in that water body—including zombies, drowned, aquatic life, and the player—receive continuous electrical damage ticks and movement stutter.
* **Tactical Application:** Lure swarms of enemies into swamps, rivers, or rainstorms, then discharge lightning from the shoreline to electrocute the entire pack safely.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/electrified_water.gif" width="450" alt="Electrified Water Demo" />
  <br/><sub><i>Electrifying water pools to continuously stun and damage wading hostile mobs.</i></sub>
</div>

---

## 2. Temporary Frost Walking

* **Mechanism:** Firing a Cryo spell (such as the <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/frostbolt_wand.png" width="16" height="16" /> [**Frostbolt Wand**](Staves-List#frostbolt-wand)) directly at a water surface converts top-layer water blocks into temporary frosted ice sheets (radius 1 for Basic, radius 2 for Netherite).
* **Terrain Safety:** The frost lasts long enough for players to sprint or leap across rivers and ocean straits. After a short duration, the frosted ice naturally melts back into regular water, leaving the landscape completely unaltered.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/freeze_water.gif" width="450" alt="Freeze Water Demo" />
  <br/><sub><i>Freezing surface water into temporary walkable frost bridges.</i></sub>
</div>

---

## 3. Geo Constructs, Fang Pillars & Stone Steles

* **Mechanism:**
  * Casting the <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/evoker_fang_staff.png" width="16" height="16" /> [**Evoker Fang Staff**](Staves-List#evoker-fang-staff) summons 9 (Basic) or 13 (Netherite) biting fangs along with a solid **Geo Fang Pillar** (`1x1x1` with `1x Max HP` on Basic; `1x2x1` with `2x Max HP` on Netherite; up to 3 active).
  * Casting <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/vortex_vanquisher.png" width="16" height="16" /> [**Vortex Vanquisher**](Staves-List#vortex-vanquisher) summons a monolithic `1x2x1` **Stone Stele** (max 1 on Basic, 2 on Netherite) that resonates across a **6-block radius** (`1.0U` / `1.5U Geo` + `1.9% Max HP` bonus damage).
* **Properties:**
  * **Resonant Geo Shockwaves:** Pulses square Geo shockwaves every **2 seconds (40 ticks)** (`3m` radius, `0.5♥ / 0.5U Geo` on Basic Fang Pillar; `5m` radius, `1.0♥ / 1.0U Geo` on Netherite Fang Pillar; `6m` radius on Stone Stele) that chain-resonate with nearby Geo Constructs!
  * **Taunt & Projectile Interception:** Draws enemy hostility without aggroing mobs onto the creator, intercepts hostile projectiles, and blocks melee pathways.
  * **Self-Elevation (Cast Beneath Your Feet):** Looking downward (`>= 50°` pitch) when casting spawns the Geo Pillar or Stone Stele **directly beneath your feet** and immediately lifts you onto the top of the pillar (`liftEntityOntoPillar`), safely elevating you out of reach from melee mobs and Creepers.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/geo_construct.gif" width="450" alt="Geo Construct Demo" />
  <br/><sub><i>Summoning a Geo pillar beneath the player to evade ground-level threats and scale terrain.</i></sub>
</div>

---

## 4. Sculk Sensor & Deep Dark Resonance

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/element/anemo.png" width="28" height="28" />
  &nbsp;&nbsp;&nbsp;&nbsp;<b>+</b>&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/shared-resources/assets/velorise_staves/textures/item/resonant_echo.png" width="28" height="28" />
</div>

* Deep within the subterranean Deep Dark biome, **Sculk Sensors** are acutely sensitive to arcane vibrations and sonic frequencies.
* Discharging a sonic boom from the [**Resonant Echo**](Staves-List#resonant-echo) staff or firing lightning near a Sculk block causes the sensor to vibrate violently, activating remote redstone mechanisms and propagating shock currents to nearby mobs.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/sculk_sensor_electro.gif" width="450" alt="Sculk Sensor Interaction" />
  <br/><sub><i>Remotely triggering Sculk sensors through sonic frequencies to cascade electrical pulses.</i></sub>
</div>

---

## 5. Tesla Coils & Storm Overcharge

Placing a copper **Lightning Rod directly on top of a Block of Redstone** transforms the structure into an automated **Tesla Defense Coil**.

* **Standard Mode (Clear Weather):**
  * Discharges electric arcs every **2.0 seconds** (40 ticks).
  * Auto-targets hostile mobs within a **2-block radius** with 3–5 lightning arcs per pulse.
  * Deals 1.0 damage per arc, applies **0.5U Electro**, and electrifies surrounding water pools.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/tesla_coil_normal.gif" width="450" alt="Tesla Coil Normal Mode" />
  <br/><sub><i>Standard Tesla Coil defending a perimeter against approaching hostile mobs.</i></sub>
</div>

* **Overcharged Mode (Rain & Storms):**
  * Activates automatically during **rain or thunderstorms**, or for **30 seconds** following a natural lightning strike.
  * Emits dense electrical spark particles and doubles all combat statistics:
    * **Discharge Rate:** Fires every **1.0 second** (20 ticks, 2x faster).
    * **Detection Range:** Extends to a **4-block radius** (2x range).
    * **Volts & Damage:** Discharges **6–10 arcs** per pulse dealing **2.0 damage** + **1.0U Electro** (2x damage).
    * **Aquatic [Spread](Elements-and-Reactions#spread):** Electrifies adjacent water bodies across a 3.0m radius for 70 ticks.

<div align="center">
  <img src="https://raw.githubusercontent.com/velorise2206/velorise-staves/main/docs/showcase/tesla_coil_rain.gif" width="450" alt="Tesla Coil Overcharged Mode" />
  <br/><sub><i>Overcharged Tesla Coil during rain unleashing high-frequency electrical arcs.</i></sub>
</div>
