# FAQ, Keybinds & Mod Compatibility

---

## ❓ Frequently Asked Questions (FAQ)

### Q1: Why can't I craft or absorb Mavuika, Furina, Zhongli, Venti, Raiden, Nahida, or Tsaritsa into the Omni Staff?

**A:** By design. Archon weapons feature complex, specialized mechanics (vehicles, stances, dual bursts) that cannot be combined into the Omni Staff.

### Q2: Why do my staves stop working when I carry more than 4 staves?

**A:** An intended balance mechanic reflecting Genshin's 4-character party limit. You can disable or change this cap via the config file (`max_staves_in_inventory = 0`).

### Q3: How do I cast an Elemental Burst `[Q]` from my Offhand?

**A:** Simply press **`[R]`** (Elemental Burst key). It triggers seamlessly even when the weapon is held in your Offhand.

### Q4: How do I defeat an `Omni ∞` (Omni-Innate) mutated mob that says `IMMUNE` to all my staves?

**A:** **Omni-Innate (`Omni ∞`)** mobs are 100% immune to all Elemental and Magic damage! Switch to a **Physical Melee Sword** or **Vanilla Bow/Crossbow** to deal physical damage — or, if it is a tamable Wolf/Fox, feed it an **Omni Element** (`omni_element`) to tame it as an elemental-immune pet!

### Q5: Are Archon idle/hover animations (Raiden levitating, Nahida swing) compatible with player animation mods?

**A:** Yes! As of **v1.1.1**, Archon levitation and swing/seat offsets are isolated inside `LivingEntityRenderer`'s internal `PoseStack` block, keeping Raiden's levitation and Nahida's swing intact alongside **`player-animation-lib`**, **`EMF`**, and **`Mo' Bends`** without prop stacking or pose overrides.


---

## ⌨️ Complete Keybind Reference

All keybinds can be rebound in Minecraft's standard **Options ➔ Controls ➔ Key Binds** menu under the **Velorise's Staves** category:

- **`[R]` (`key.velorise_staves.elemental_burst`):** Cast Elemental Burst `[Q]` (Mavuika, Furina, Zhongli, Venti, Raiden, Nahida, Tsaritsa, Neuvillette, Xilonen, Layla, Dragonfire).
- **`[Tab]` (`key.velorise_staves.omni_wheel`):** Open the **Omni Element Wheel** while holding the Omni Staff.
- **`Unbound` -> falls back to `Use / Right-Click` (`key.velorise_staves.wind_glider`):** Deploy / Fold the **Wings of First Flight (Wind Glider)** in midair (`enable_wind_glider`). You can bind a dedicated custom key in Controls without overriding vanilla Right-Click.
- **`Unbound` -> falls back to `Attack / Left-Click` (`key.velorise_staves.plunge_attack`):** Execute a midair **Plunging Attack** (`enable_plunge_attack`). You can bind a dedicated custom key in Controls without overriding vanilla Left-Click.

---

## 🎨 Resource Pack Guide: Custom Wind Glider Textures & Voice Packs

**Velorise's Staves** natively supports **Minecraft Resource Packs** so players and creators can swap the *Wings of First Flight* textures or replace/add character voice-overs (`pack.mcmeta`: use `"pack_format": 34` for Minecraft `1.21.1` or `"pack_format": 15` for Minecraft `1.20.1`).

### 1. Resource Pack Directory Layout

```text
MyVeloriseResourcePack/
├── pack.mcmeta
└── assets/
    └── velorise_staves/
        ├── textures/
        │   └── entity/
        │       ├── wind_glider.png         (Standard Wind Glider wing sheet, 64×32)
        │       └── wind_glider_32px.png    (HD Wind Glider wing sheet when use_32px_glider_texture = true)
        └── sounds/
            └── voice/
                └── zhongli/                (Live playback + UI Available status)
                    ├── e_tap_en_1.ogg
                    ├── e_tap_en_2.ogg
                    ├── e_tap_en_3.ogg
                    ├── e_hold_en_1.ogg
                    ├── e_hold_en_2.ogg
                    ├── e_hold_en_3.ogg
                    ├── q_burst_en_1.ogg
                    ├── q_burst_en_2.ogg
                    ├── q_burst_en_3.ogg
                    ├── e_tap_cn_1.ogg
                    ├── e_tap_cn_2.ogg
                    ├── e_tap_cn_3.ogg
                    ├── e_hold_cn_1.ogg
                    ├── e_hold_cn_2.ogg
                    ├── e_hold_cn_3.ogg
                    ├── q_burst_cn_1.ogg
                    ├── q_burst_cn_2.ogg
                    └── q_burst_cn_3.ogg
```

Minimal `pack.mcmeta` example (`1.21.1`):

```json
{
  "pack": {
    "pack_format": 34,
    "description": "Custom Wind Glider & Voice Pack for Velorise's Staves"
  }
}
```

### 2. Changing the Wind Glider Texture

- Place your custom PNG wing sheet inside `assets/velorise_staves/textures/entity/`:
  - Default config (`use_32px_glider_texture = false`): override **`wind_glider.png`**.
  - HD config (`use_32px_glider_texture = true`): override **`wind_glider_32px.png`**.
- Wing UV region is the top-left **`20×16`** pixels on a standard **`64×32`** canvas (`u = 0..11` inner wing panel, `u = 11..20` outer wing tip, `v = 0..16`).
- Enable the Resource Pack in **Options ➔ Resource Packs** (`F3 + T` reloads textures immediately).

### 3. Custom Character Voice Pack

- **Audio Format Requirement:** All voice files must be encoded as **Ogg Vorbis (`.ogg`)**, **Mono**, **44100 Hz** (Minecraft's sound engine requires Mono for directional 3D world playback).
- **UI Detection (`CharacterVoiceScreen`):** The mod automatically scans `assets/velorise_staves/sounds/voice/<charId>/*.ogg` for any of the 10 characters (`zhongli`, `raiden`, `venti`, `nahida`, `furina`, `neuvillette`, `mavuika`, `xilonen`, `tsaritsa`, `layla`) and marks a language as **`Available`** whenever a filename contains language tokens:
  - English (`en_us`): `_en` or `_en_us` (or `en/...`)
  - Chinese (`zh_cn`): `_cn`, `_zh`, or `_zh_cn`
  - Japanese (`ja_jp`): `_jp`, `_ja`, or `_ja_jp`
  - Korean (`ko_kr`): `_kr`, `_ko`, or `_ko_kr`
- **Current Live Combat Playback Support:**
  - **Zhongli (`EN` & `ZH/CN`):** Fully wired to `SoundEvent`s in `sounds.json` and plays live on Tap E (`voice_zhongli_e_tap_*`), Hold E (`voice_zhongli_e_hold_*`), and Burst `[R]` (`voice_zhongli_q_burst_order_*` on first cast per world, then `voice_zhongli_q_burst_*`). Replacing any of the 18 `.ogg` files under `assets/velorise_staves/sounds/voice/zhongli/` immediately replaces Zhongli's in-game voice-over.
  - **Other Characters / `JP` & `KR`:** Placing `.ogg` files in `assets/velorise_staves/sounds/voice/<charId>/` makes them appear as **`Available`** in the Voice Profile UI; live skill triggers currently use Zhongli's registered `SoundEvent`s.

---

## 🤝 Mod Compatibility Matrix


| Mod / Ecosystem                              | Loader Support          | Integration Details                                                                                                                                                                                              |
| :--------------------------------------------- | :------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Jade / Waila**                             | NeoForge, Fabric, Forge | Built-in `VeloriseStavesJadePlugin` displays active Elemental Auras, exact Gauge Units (`U`), remaining timers, `Omni ∞` badges, and **Geo Construct HP / Lifetime** directly in the Jade HUD!                  |
| **JEI / REI / EMI**                          | NeoForge, Fabric, Forge | Displays all Shaped/Shapeless Crafting recipes, Elemental Rod transmutations, and Smithing Table Netherite upgrades.                                                                                             |
| **Iris / Oculus Shaders**                    | NeoForge, Fabric, Forge | Custom `ShieldRenderTypes` (`TRANSLUCENT_NO_DEPTH_WRITE`, `TRANSLUCENT_DEPTH_WRITE`, `METEOR_SOLID`) render 3D shields, lightning arcs, Nahida domes, and meteors cleanly without shader depth-buffer artifacts. |
| **Sodium / Embeddium / Rubidium**            | NeoForge, Fabric, Forge | Zero custom chunk mesh hooks; all particle and vertex batches are fully compatible with Sodium's rendering pipeline.                                                                                             |
| **First-Person Model / Better First Person** | NeoForge, Fabric, Forge | Dedicated 1st-person vs 3rd-person camera checks (`CameraType.FIRST_PERSON`) prevent 3D idle props or sunglasses overlays from clipping the camera.                                                              |
| **Player Animation Lib / EMF / Mo' Bends**   | NeoForge, Fabric, Forge | Archon idle `PoseStack` offsets (Raiden levitation, Nahida swing) are isolated inside `LivingEntityRenderer` so idle animations remain intact without external pose overrides. |
| **FTB Chunks / Flan / OpenPAC / WorldGuard** | NeoForge, Fabric, Forge | Cross-loader claim protection automatically prevents Raiden's *Grasscutter* weed clearing and Zhongli's *Vortex Vanquisher* Geo ore mining from modifying protected claims. |
