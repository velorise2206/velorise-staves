# Configuration Guide

**Velorise's Staves & Elements** provides extensive configuration options tailored for server administrators and modpack creators looking to tune combat balance, cooldown pacing, mob mutation rates, and environmental rules.

* **Configuration File Locations:**
  * **All Loaders (NeoForge, Fabric & Forge):** `.minecraft/config/velorise_staves.toml` (Common TOML Config across client & dedicated server, section `[mechanics]` & `[elemental_gauge]`).

---

## Combat & General Balancing

Tune global damage and cooldown multipliers for all [**Arcane Staves**](Staves-List#arcane-staves--netherite-upgrades) and configure the [**Arcane Overload**](Elements-and-Reactions#arcane-overload-4-staff-inventory-cap) inventory limit:

```toml
[mechanics]
# Global damage multiplier applied to all elemental staves (Default: 1.0)
# Scale up to 1.5 - 2.0 for hardcore modpacks with high-health custom mobs.
damage_multiplier = 1.0

# Global cooldown multiplier for all staves (Default: 1.0)
# Set to 0.5 to halve cooldown durations for fast-paced combat.
cooldown_multiplier = 1.0
```

Configure [**Arcane Overload**](Elements-and-Reactions#arcane-overload-4-staff-inventory-cap) and Elemental Gauge mechanics:

```toml
[elemental_gauge]
# Maximum number of Arcane Staves allowed in a single player inventory (Default: 4)
# Carrying more than this limit triggers the Arcane Overload penalty.
# Set to 0 to remove all inventory capacity limits.
max_staves_in_inventory = 4

# Direct damage bonus granted by Pyro Elemental Resonance (Default: 0.10 -> +10%)
pyro_resonance_damage_bonus = 0.10

# Armor durability wear mode on elemental hits: "NORMAL", "REDUCED", or "NONE"
elemental_armor_durability_wear = "REDUCED"

[scaling]
# Genshin-style Co-Op Multi-Player Dynamic Mob HP & ATK Scaling
enable_coop_mob_scaling = true
hp_scale_per_extra_player = 0.50
enable_coop_atk_scaling = true
max_coop_players_cap = 4
coop_scan_radius = 48.0
```

---

## Utility Artifacts

Customize recall channeling and blink reach for the [**Magic Mirror**](Relics-and-Utilities#magic-mirror) and [**Void Translocator**](Relics-and-Utilities#void-translocator):

```toml
[mechanics]
# Allows the standard Magic Mirror to teleport across dimensions (Default: false)
# When set to false, cross-dimensional teleportation requires the Netherite variant.
basic_mirror_cross_dimension = false

# Channel duration in seconds required to trigger the Magic Mirror (Default: 2.5)
magic_mirror_channel_seconds = 2.5

# Blink teleport distance in blocks for the standard Void Translocator (Default: 24.0)
void_translocator_range_basic = 24.0

# Blink teleport distance in blocks for the Netherite Void Translocator (Default: 36.0)
void_translocator_range_netherite = 36.0
```

---

## Mob Mutation Rates

Control spawn probabilities for [**Innate Elemental Mobs**](Elements-and-Reactions#innate-elemental-mobs-drops--rare-omni-mutations) and rare [**Omni-Mutated Mob**](Elements-and-Reactions#2-mob-mutation-rules-7-default-6-elemental--1-omni) encounters:

```toml
[elemental_gauge]
# Chance (from 0.0 to 1.0) for newly spawned mobs to carry innate elemental mutations (Default: 0.07 -> 7%)
# Mutated mobs drop their corresponding Elemental Essence upon defeat.
innate_mutation_chance = 0.07

[innate_mutation]
# Allows hostile mobs (Zombies, Skeletons, Creepers) to mutate with elemental auras (Default: true)
allow_hostile_mob_mutation = true

# Allows Golems to spawn with elemental mutations (Default: true)
allow_golem_mutation = true

# Allows passive animals to spawn with elemental mutations (Default: false)
allow_neutral_and_passive_mutation = false
```

---

## Environmental Interactions

Configure [**Arc Caster**](Staves-List#arc-caster) Creeper charging and [**Webcaster**](Staves-List#webcaster) temporary cobweb duration:

```toml
[mechanics]
# Striking a Creeper with Arc Caster lightning transforms it into a Charged Creeper (Default: true)
arc_caster_charge_creeper = true

# Duration in ticks that temporary Webcaster webs persist before dissolving (Default: 100 ticks = 5 seconds)
temporary_web_duration_ticks = 100
```

---

## Server Performance Notes

* **Spatial Grid Caching:** The mod utilizes a spatial partition grid for elemental particle broadcasts and environmental block checks.
* **Network Culling:** Aura sync packets are dispatched only to players within render range of the affected entity, preserving 20.0 server TPS on populated multiplayer servers.
