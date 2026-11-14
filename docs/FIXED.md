# Velorise's Staves & Elements — Fixed & Resolved Issues

> Archive of resolved bugs, root-cause analyses, and shipped hotfixes. For upcoming features and open bugfixes, see [PLANS.md](PLANS.md).

---

## v1.0.2 (PLANS.md Full Implementation — NeoForge 1.21.1, Fabric 1.21.1, Forge 1.20.1)

- **1. Neuvillette (`Guardian Lens`):** 8% Max HP drain per 0.5s (stops at `<= 50% Max HP`), 16% Max HP heal per creator-owned Sourcewater Droplet, multi-target piercing Hydro beam, and dense water VFX/SFX.
- **2. Zhongli (`Vortex Vanquisher` & `GeoFangPillarEntity`):** 4-block resonance wave radius, underwater/mid-air solid surface & construct-stacking placement, Fabric 1.21.1 water-standing fix, and 50% smaller neck-to-knee Jade Shield model.
- **3. Furina (`Splendor of Tranquil Waters`):** Fixed 3rd-person (F5/Multiplayer) summon animation removal bug in `ClientAuraRenderer.render()`, lowered front arm with 90° vertical downward scepter rotation, and set 20s CD / 30s pet duration.
- **4–11. Tooltips, 5 Hidden Advancements, Recipes, JEI & 1.5x Netherite Rule:** Fixed `Arc Caster` (`0.5♥` / `0.75♥`) and tier-split `Vortex Vanquisher` (`+20%` / `+30%`) tooltips, added 5 hidden advancements (`Beheaded`, `I Don't Have Order`, `Web Developer`, `Take My Ball`, `Solidify!`), updated 4 staff recipes (`splendor_of_tranquil_waters`, `ghastfire_staff`, `frostbolt_wand`, `webcaster`), synchronized all Omni Staff tags & JEI anvil recipes, and enforced 1.5x Netherite stat scaling without cooldown reduction.

---

## v1.0.0.1 (Fabric 1.21.1 Hotfix)

### 1. Anvil Sword Imbuing & Melee Combat Elemental Application on Fabric 1.21.1
- **Status:** ✅ FIXED & VERIFIED — Released in Fabric `1.0.0.1` *(NeoForge 1.21.1 and Forge 1.20.1 unaffected)*
- **Root Cause & Fix:**
  1. **Anvil Imbuing (`AnvilMenuMixin.java` & `SwordImbueHelper.java`):**
     * `AnvilMenuMixin.java` injects at `createResult()` (`@At("HEAD")`), setting `resultSlots.setItem(0, res.output())`, `cost.set(res.cost())`, and `repairItemCountCost = res.materialCost()`, and explicitly validates output pickup via `mayPickup()`.
     * Note on observed behavior: `SwordImbueHelper.tryBuildAnvilResult` intentionally rejects re-imbuing a sword with the *exact same element* (`if (targetKey.equals(getImbuedKey(left))) return null;`) to prevent players from wasting essences.
  2. **Combat Elemental Application (`PlayerAttackMixin.java` & `ElementalGaugeManager.java`):**
     * **Root Cause 1 — Vanilla `Player.attack()` Cooldown Reset Timing:** In vanilla/Fabric 1.21.1, `Player.attack(Entity)` calls `this.resetAttackStrengthTicker()` **before** invoking `target.hurt(...)`. Forge 1.20.1 and NeoForge 1.21.1 patch this to move the reset **after** `target.hurt()`. Consequently, on Fabric, `attacker.getAttackStrengthScale(0.5F)` always returned `0.0F` inside `actuallyHurt`, causing imbued swords to silently skip elemental application on every swing.
     * **Root Cause 2 — `Player.actuallyHurt()` PvP Override:** `Player` overrides `LivingEntity.actuallyHurt(DamageSource, float)` without calling `super`, so `LivingEntityDamageMixin` never fired for PvP damage on Fabric.
     * **Fix Applied:** Added `PlayerAttackMixin` (Fabric 1.21.1) to snapshot `getAttackStrengthScale(0.5F)` at `@At("HEAD")` of `Player.attack()` via `ElementalGaugeManager.recordPreAttackStrength(...)` before the ticker resets (cleaned up at `@At("RETURN")`), and hooked `Player.actuallyHurt` via `modifyFabricIncomingDamage(...)` for full PvE + PvP parity.
