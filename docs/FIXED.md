# Velorise's Staves & Elements — Fixed & Resolved Issues

> Archive of resolved bugs, root-cause analyses, and shipped hotfixes. For upcoming features and open bugfixes, see [PLANS.md](PLANS.md).

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
