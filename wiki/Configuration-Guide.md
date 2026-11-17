# Configuration Guide

Velorise's Staves & Elements provides extensive configuration options through `config/velorise_staves.json` (or `staves-common.json` depending on loader), with real-time server-to-client synchronization.

---

## Configuration Categories

### 1. General Mechanics (`mechanics`)
- `damage_multiplier` (default: `1.0`, range: `0.1`–`10.0`): Global damage multiplier applied to all elemental attacks.
- `cooldown_multiplier` (default: `1.0`, range: `0.1`–`10.0`): Global cooldown multiplier.
- `void_translocator_range_basic` (default: `24.0m`): Blink distance for Basic Void Translocator.
- `void_translocator_range_netherite` (default: `36.0m`): Blink distance for Netherite Void Translocator.
- `void_translocator_cooldown_seconds` (default: `5.0s`): Cooldown between spatial blinks.
- `magic_mirror_cooldown_seconds` (default: `15.0s`): Recall mirror cooldown.
- `magic_mirror_channel_seconds` (default: `2.5s`): Channeling duration to teleport home.
- `basic_mirror_cross_dimension` (default: `false`): Whether basic mirrors can recall from Nether/End (Netherite mirror always can).

### 2. Elemental Gauge & Resonance (`elemental_gauge`)
- `enable_elemental_gauge` (default: `true`): Toggles the entire Elemental Gauge Theory & reaction system.
- `enable_elemental_resonance` (default: `true`): Enables party elemental resonances.
- `max_staves_in_inventory` (default: `4`, range: `0`–`36`): Maximum number of staves a player can carry before triggering **Elemental Overload** (set to 0 to disable the limit).
- `show_elemental_particles` (default: `true`): Visual reaction particles.
- `show_overhead_element_icons` (default: `true`): Overhead element badges above affected entities.
- `character_voice_language` (default: `"auto"`): Voice language for Archon abilities (`"en"` for English, `"zh"` for Chinese, `"auto"` to match client language).

### 3. Multiplayer Co-op Scaling (`scaling`)
- `enable_coop_mob_scaling` (default: `true`): Dynamically scales nearby monster HP when multiple players engage in combat together.
- `hp_scale_per_extra_player` (default: `0.50` / `+50%`): Added monster health per additional player.
- `max_coop_players_cap` (default: `4`): Cap on co-op multiplier scaling.
- `coop_scan_radius` (default: `40.0m`): Radius around combat to detect co-op participants.

### 4. Aerial Mechanics
- `enable_wind_glider` (default: `true`): Enables deployment of Wings of First Flight.
- `enable_plunge_attack` (default: `true`): Enables downward plunging attack from air/glider.

---

## Live In-Game Sync via Elemental Codex
When opening the **Elemental Codex** in multiplayer, the guidebook queries the server and displays the exact live values active on that server, ensuring complete transparency between singleplayer and dedicated server environments.
