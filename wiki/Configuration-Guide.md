# Configuration Guide (TOML & In-Game Config GUI)

**Velorise's Staves** provides a comprehensive configuration system with **automatic Server-to-Client synchronization** (`ModNetwork.sendConfigSyncToPlayer`) and a built-in graphical configuration screen (`VeloriseConfigScreen`).

- **Config File Locations:**
  - **NeoForge 1.21.1 & Forge 1.20.1:** `config/velorise_staves-common.toml`
  - **Fabric 1.21.1:** `config/velorise_staves.toml`
- **In-Game GUI:** Open **Mods** (NeoForge/Forge) or **Mod Menu** (Fabric) ➔ select **Velorise's Staves** ➔ click **Config** to edit settings with sliders, toggles, and instant save!

---

## ⚙️ Complete Configuration Reference

### 1. General Mechanics (`[mechanics]`)
| Key | Default | Range | Description |
| :--- | :--- | :--- | :--- |
| `damage_multiplier` | `1.0` | `0.1` – `10.0` | Global multiplier applied to all staff, skill, burst, and reaction damage. |
| `cooldown_multiplier` | `1.0` | `0.1` – `10.0` | Global multiplier applied to all staff skill cooldowns. |
| `arc_caster_charge_creeper` | `true` | `boolean` | Whether *Arc Caster* lightning beams charge vanilla Creepers into Powered Creepers. |
| `basic_mirror_cross_dimension` | `false` | `boolean` | Allows Basic *Magic Mirror* to recall across dimensions (Netherite Mirror always can). |
| `temporary_web_duration_ticks` | `100` (`5.0s`) | `20` – `1200` | Lifetime (in ticks) of temporary toxic cobwebs spawned by *Webcaster*. |
| `void_translocator_range_basic` | `24.0` | `4.0` – `128.0` | Max blink distance (blocks) for Basic *Void Translocator*. |
| `void_translocator_range_netherite` | `36.0` | `4.0` – `128.0` | Max blink distance (blocks) for Netherite *Void Translocator*. |
| `void_translocator_cooldown_seconds` | `5.0` | `0.5` – `60.0` | Cooldown (seconds) between spatial blinks. |
| `magic_mirror_cooldown_seconds` | `15.0` | `1.0` – `300.0` | Cooldown (seconds) after completing a Magic Mirror recall. |
| `magic_mirror_channel_seconds` | `2.5` | `0.5` – `10.0` | Channeling time (seconds) required before Magic Mirror teleports. |

---

### 2. Elemental Gauge, Resonance & Visuals (`[elemental_gauge]`, `[gameplay]`, `[audio]`)
| Key | Default | Range / Options | Description |
| :--- | :--- | :--- | :--- |
| `enable_elemental_gauge` | `true` | `boolean` | Master toggle for the Elemental Gauge Theory (EGT) & reaction system. |
| `enable_elemental_resonance` | `true` | `boolean` | Enables party Elemental Resonance buffs based on carried staves. |
| `max_staves_in_inventory` | `4` | `0` – `36` | Max combat staves before triggering **Elemental Overload** (`0` disables the cap). |
| `pyro_resonance_damage_bonus` | `0.10` (`+10%`) | `0.0` – `1.0` | Bonus attack damage multiplier granted by Pyro Resonance (*Fervent Flames*). |
| `elemental_armor_durability_wear` | `"auto"` | `"auto"`, `"true"`, `"false"` | Controls whether elemental reactions deal extra durability wear to armor (`"auto"` enables on Hard difficulty / PvP). |
| `show_elemental_particles` | `true` | `boolean` | Toggles elemental aura particles on affected entities. |
| `show_overhead_element_icons` | `true` | `boolean` | Renders 3D elemental aura icons and reaction badges above entities' heads. |
| `show_reaction_text` | `true` | `boolean` | Renders floating 3D reaction titles (`VAPORIZE`, `MELT`, `HYPERBLOOM`, etc.). |
| `show_damage_numbers` | `true` | `boolean` | Renders floating 3D elemental color-coded damage numbers. |
| `use_32px_textures` | `false` | `boolean` | Switches staff item textures to high-resolution 32×32 sprites. |
| `use_32px_element_icons` | `false` | `boolean` | Uses 32×32 HD overhead element icons instead of 16×16 pixel-art icons. |
| `use_32px_glider_texture` | `false` | `boolean` | Uses 32×32 HD texture for the *Wings of First Flight* Wind Glider. |
| `render_dragon_glider` | `true` | `boolean` | Renders the 3D Ender Dragon model when gliding with *Dragonfire Scepter* (set `false` to use standard Wind Glider wings). |
| `character_voice_language` | `"auto"` | `"auto"`, `"en"`, `"zh"` | Voice-over language for Zhongli & Archon abilities. |
| `enable_plunge_attack` | `true` | `boolean` | Enables mid-air Left-Click Plunging Attacks. |
| `enable_wind_glider` | `true` | `boolean` | Enables mid-air Right-Click (rebindable) Wind Glider deployment. |
| `enable_custom_stamina` | `true` | `boolean` | Enables Genshin-style Stamina (`100` Overworld ➔ `170` Nether ➔ `240` End milestones) for dashing, sprinting, gliding, and wall-climbing. |
| `enable_custom_dash` | `true` | `boolean` | Enables Genshin-style quick Dash impulse with I-frames when pressing `[Sprint / Ctrl]`. |
| `enable_wall_climb` | `true` | `boolean` | Enables Genshin-style vertical wall-climbing when pressing forward against walls (consumes Stamina). |
| `enable_mavuika_wall_climb` | `true` | `boolean` | Allows Mavuika's Flamestrider motorcycle to climb vertical walls. |
| `mavuika_bike_speed_multiplier` | `1.0` | `0.5` – `3.0` | Speed multiplier for Mavuika's Flamestrider motorcycle. |

---

### 3. Co-Op Multi-Player Dynamic Mob Scaling (`[scaling]`)
| Key | Default | Range | Description |
| :--- | :--- | :--- | :--- |
| `enable_coop_mob_scaling` | `true` | `boolean` | Scales monster HP & ATK when multiple players fight together within `coop_scan_radius`. |
| `hp_scale_per_extra_player` | `0.50` (`+50%`) | `0.0` – `5.0` | Additional monster Max HP per extra nearby player (`2P = 1.5x, 3P = 2.0x, 4P = 2.5x`). |
| `enable_coop_atk_scaling` | `true` | `boolean` | Scales monster attack damage slightly in co-op encounters. |
| `max_coop_players_cap` | `4` | `1` – `16` | Maximum player count considered for co-op scaling. |
| `coop_scan_radius` | `40.0` | `8.0` – `128.0` | Radius (blocks) around a spawning/engaged monster to count co-op players. |

---

### 4. Innate Mob Mutation Rules (`[innate_mutation]`)
| Key | Default | Range / Format | Description |
| :--- | :--- | :--- | :--- |
| `innate_mutation_chance` | `0.08` (`8%`) | `0.0` – `1.0` | Default probability for an eligible mob to spawn with a mutated elemental aura (`7%` standard elemental + `1%` Omni). |
| `omni_mutation_share` | `0.125` (`1/8`) | `0.0` – `1.0` | Share of mutated mobs that become **Omni-Innate (`Omni ∞`)** (`0.08 × 0.125 = 1%`). |
| `allow_hostile_mob_mutation` | `true` | `boolean` | Allows hostile monsters (`Monster` category) to mutate. |
| `allow_golem_mutation` | `true` | `boolean` | Allows Golems (`Iron Golem, Snow Golem`) to mutate. |
| `allow_neutral_and_passive_mutation` | `false` | `boolean` | Allows all passive/neutral animals to mutate globally (even if not in whitelist). |
| `mutable_mob_whitelist` | `["minecraft:iron_golem", "minecraft:wolf", "minecraft:fox"]` | `List<String>` | Entity IDs or `#namespace:tag` entries always eligible for mutation (enables Omni Wolf/Fox taming!). |
| `mutable_mob_blacklist` | `[]` | `List<String>` | Entity IDs or `#namespace:tag` entries strictly excluded from mutation (Bosses & aquatic ambient mobs are excluded automatically). |
| `custom_mob_mutation_rates` | `[]` | `List<"id=rate">` | Per-entity custom mutation rates, e.g. `["minecraft:zombie=0.15", "minecraft:creeper=0.0"]`. |

---

### 5. Per-Staff Cooldowns (`[cooldowns]`), Base Damages (`[damage]`) & Durabilities (`[durability]`)
Every single one of the **22 combat staves** and **2 utility relics** has dedicated individual config entries for:
- **Cooldowns (`*_cooldown`, in seconds):** e.g., `thousand_blazing_suns_cooldown = 15.0`, `splendor_cooldown = 20.0`, `vortex_cooldown = 4.0`, `daybreak_cooldown = 2.0`, `engulfing_lightning_cooldown = 10.0`, `thousand_floating_dreams_cooldown = 5.0`, `tsaritsa_bow_cooldown = 6.0`, `peak_patrol_song_cooldown = 7.0`, `guardian_cooldown = 12.0`, `nights_focus_cooldown = 12.0`, `omni_cooldown = 6.0`, etc.
- **Basic Base Damages (`*_damage`, in HP where `2.0 = 1.0❤`; Netherite automatically scales by `1.5×`):** e.g., `dragonfire_damage = 7.0`, `echo_damage = 11.0`, `omni_damage = 10.0`, etc.
- **Basic & Netherite Max Durabilities (`*_durability_basic` and `*_durability_netherite`):** e.g., `omni_durability_basic = 2340`, `omni_durability_netherite = 4680`, etc.
