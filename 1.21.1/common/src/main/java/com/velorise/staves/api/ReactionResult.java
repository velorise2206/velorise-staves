package com.velorise.staves.api;

import com.velorise.staves.element.ElementalReaction;

/**
 * Immutable outcome snapshot returned by {@link VeloriseElementAPI} calls.
 *
 * @param applied               Whether elemental processing ran on the target (false if cancelled or invalid).
 * @param immune                Whether the target was immune to the incoming element (Innate or Omni-Innate).
 * @param reaction              The primary reaction triggered, or {@link ElementalReaction#NONE}.
 * @param finalDamage           Effective damage dealt or mitigated during the call (0 if aura-only).
 * @param shieldRemainingUnits  Remaining elemental shield gauge units on the target (0 if no active shield).
 * @param shieldBroken          True if this hit depleted and shattered the target's active elemental shield.
 */
public record ReactionResult(
        boolean applied,
        boolean immune,
        ElementalReaction reaction,
        float finalDamage,
        float shieldRemainingUnits,
        boolean shieldBroken
) {
    public static final ReactionResult EMPTY = new ReactionResult(false, false, ElementalReaction.NONE, 0.0F, 0.0F, false);

    public static ReactionResult immune(float shieldRemainingUnits) {
        return new ReactionResult(true, true, ElementalReaction.IMMUNE, 0.0F, shieldRemainingUnits, false);
    }
}
