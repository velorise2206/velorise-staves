# Elements and Reactions (Elemental Gauge Theory)

The mod implements a faithful, mathematical adaptation of Genshin Impact's **Elemental Gauge Theory (EGT)**, Internal Cooldowns (ICD), and all 16 elemental reactions.

---

## Elemental Gauge Theory (EGT) Fundamentals

### 1. Gauge Units (U)
- **1U (Standard):** Applied by basic elemental attacks (duration: 9.5s).
- **2U (Medium):** Applied by elemental skills and Netherite staves (duration: 12.0s).
- **4U (Heavy):** Applied by Archon bursts and charged ultimate abilities (duration: 17.0s).
- **Aura Decay Rate:** When an aura is applied to an entity, its initial gauge decays linearly based on:
  $$\text{Duration} = 2.5 \times U + 7.0 \text{ seconds}$$
  When an aura triggers a reaction, the applied element consumes the existing aura multiplied by its **Consumption Modifier**.

### 2. Internal Cooldown (ICD)
- Default Rule: **2.5 seconds or every 3 hits** (standard Genshin ICD).
- Certain special attacks (e.g. Neuvillette beam ticks, Venti Stormeye, Nahida Tri-Karma) possess independent, custom ICD counters.

---

## Amplifying Reactions (Damage Multipliers)

| Reaction | Formula | DMG Multiplier | Aura Consumption | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Forward Vaporize** | 💧 Hydro on 🔥 Pyro | **2.0× DMG** | 2.0× U | Strong side. Consumes twice as much Pyro aura. |
| **Reverse Vaporize** | 🔥 Pyro on 💧 Hydro | **1.5× DMG** | 0.5× U | Weak side. Consumes half as much Hydro aura, allowing multiple reactions. |
| **Forward Melt** | 🔥 Pyro on ❄️ Cryo | **2.0× DMG** | 2.0× U | Strong side. Consumes twice as much Cryo aura. |
| **Reverse Melt** | ❄️ Cryo on 🔥 Pyro | **1.5× DMG** | 0.5× U | Weak side. Consumes half as much Pyro aura. |

---

## Transformative & Crowd-Control Reactions

| Reaction | Formula | Effect & Behavior |
| :--- | :--- | :--- |
| **Frozen** | 💧 Hydro + ❄️ Cryo | Encases the victim in a solid translucent **Ice Statue**. Mobs and players are completely immobilized. Players suffer **Camera Angle Lock** and cannot attack or interact until thawed or shattered. |
| **Shatter** | 🪨 Geo / Heavy Attack on 🧊 Frozen | Cracks the ice encasement, instantly breaking the freeze and dealing **massive bonus Physical/Geo damage**. |
| **Overloaded** | 🔥 Pyro + ⚡ Electro | Triggers a violent explosion with fiery particle shockwaves, knocking enemies away and dealing **AoE Pyro DMG**. |
| **Superconduct** | ❄️ Cryo + ⚡ Electro | Unleashes a frost-lightning burst dealing **AoE Cryo DMG** and shredding the target's **Physical Defense by -40% for 12 seconds**. |
| **Electro-Charged** | 💧 Hydro + ⚡ Electro | Coexists as dual auras! Deals continuous Electro shock ticks every second and arcs electric currents to nearby wet targets. |
| **Swirl** | 🌪️ Anemo + (🔥/💧/⚡/❄️) | Spreads the absorbed element in a **5.0m AoE**, dealing Anemo DMG plus elemental damage of the swirled element to all surrounding targets. |
| **Crystallize** | 🪨 Geo + (🔥/💧/⚡/❄️) | Spawns an **Elemental Shard** on the ground. Walking over it grants an **Elemental Shield** with 250% absorption efficiency against that element and HUD indicator. |
| **Burning** | 🔥 Pyro + 🌿 Dendro | Sets the target ablaze with a ferocious **2U Burning Aura**, dealing rapid Pyro damage ticks over 2.0s–4.0s until extinguished or consumed. |

---

## Catalyze & Dendro Reactions

| Reaction | Formula | Mechanics |
| :--- | :--- | :--- |
| **Quicken** | 🌿 Dendro + ⚡ Electro | Applies an underlying **Quicken Aura** (green-yellow electrified vines) for 8–12 seconds without dealing direct damage. |
| **Aggravate** | ⚡ Electro on Quicken | Flat damage bonus added directly to the triggering Electro attack! Scales with Elemental Mastery (EM). |
| **Spread** | 🌿 Dendro on Quicken | Flat damage bonus added directly to the triggering Dendro attack! Scales with Elemental Mastery (EM). |
| **Bloom** | 💧 Hydro + 🌿 Dendro | Drops a green **Dendro Core (Bountiful Seed)** on the ground (max 5 active per player). After 6 seconds, it explodes for Dendro DMG. |
| **Hyperbloom** | ⚡ Electro on Dendro Core | Electro hit transforms the core into a high-speed **Homing Sprawling Shot** that tracks down the nearest enemy dealing high single-target Dendro DMG. |
| **Burgeon** | 🔥 Pyro on Dendro Core | Pyro hit instantly detonates the core into a violent **AoE Dendro Explosion**, dealing high AoE Dendro DMG to all surrounding entities. |
| **Overvape** | 🔥 Pyro on (💧 + ⚡) | When attacking an Electro-Charged target with Pyro, triggers **Overloaded AND Vaporize simultaneously**! |
