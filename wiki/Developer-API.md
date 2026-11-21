# Developer API, Elemental Shields & KubeJS Integration

**Velorise's Staves** exposes a lightweight, loader-agnostic Java API (`com.velorise.staves.api`) embedded directly inside the mod across **NeoForge 1.21.1**, **Fabric 1.21.1**, and **Forge 1.20.1**.

---

## 1. Static Helper Facade (`VeloriseElementAPI`)

```java
import com.velorise.staves.api.IcdPolicy;
import com.velorise.staves.api.ReactionResult;
import com.velorise.staves.api.VeloriseElementAPI;
import com.velorise.staves.element.Element;

// 1. Apply 2.0U Cryo aura (0 damage, standard 2.5s / 3-hit ICD)
VeloriseElementAPI.applyAura(target, Element.CRYO, 2.0F, attacker);

// 2. Deal 12.0 Pyro damage + 1.0U Pyro gauge (triggers Vaporize/Melt, pop-ups, RES)
ReactionResult res = VeloriseElementAPI.dealElementalDamage(
    target, attacker, Element.PYRO, 12.0F, 1.0F, IcdPolicy.BYPASS
);

// 3. Assign or clear a permanent Innate Element (or Omni-Innate)
VeloriseElementAPI.setInnateElement(entity, Element.ELECTRO);
VeloriseElementAPI.clearInnateElement(entity);

// 4. Attach a Genshin-style Boss Elemental Shield Gauge (measured in U)
VeloriseElementAPI.setElementalShield(boss, Element.CRYO, 16.0F);
float progress = VeloriseElementAPI.getShieldProgress(boss); // 0.0 - 1.0

// 5. Query active elemental state
if (VeloriseElementAPI.hasAura(target, Element.HYDRO)) {
    float units = VeloriseElementAPI.getAuraUnits(target, Element.HYDRO);
}
```

---

## 2. Loader-Agnostic Event Callbacks (`VeloriseElementEvents`)

Register pure-Java listeners without touching NeoForge or Fabric event buses:

```java
import com.velorise.staves.api.VeloriseElementEvents;

VeloriseElementEvents.registerPreAuraListener((level, target, attacker, element, units) -> {
    return true; // return false to cancel elemental application
});

VeloriseElementEvents.registerReactionListener((level, target, attacker, trigger, reaction, reactedU) -> {
    // React to Vaporize, Melt, Overloaded, Frozen, Superconduct, etc.
});

VeloriseElementEvents.registerShieldBreakListener((level, target, attacker, shieldElement, breakerElement) -> {
    // Trigger boss stun/paralyzed phase when Elemental Shield reaches 0U
});
```

---

## 3. Datapack Entity Tags

Add modded mobs to innate elements or exclude vanilla mobs using standard Minecraft Entity Type Tags:

- `#velorise_staves:pyro_creatures`, `hydro_creatures`, `cryo_creatures`, `electro_creatures`, `dendro_creatures`, `anemo_creatures`, `geo_creatures`
- `#velorise_staves:no_innate_creatures` — explicitly strips natural innate elements from any listed entity type (overrides built-in vanilla fallbacks).

---

## 4. KubeJS Scripting (Zero Addon Required)

KubeJS scripts can call `VeloriseElementAPI` directly via `Java.loadClass(...)` using string element names:

```js
const VeloriseAPI = Java.loadClass('com.velorise.staves.api.VeloriseElementAPI');

EntityEvents.hurt(event => {
  if (event.source.player) {
    VeloriseAPI.applyAura(event.entity, 'cryo', 2.0, event.source.player);
  }
});
```
