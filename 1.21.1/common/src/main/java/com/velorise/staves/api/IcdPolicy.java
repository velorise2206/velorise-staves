package com.velorise.staves.api;

/**
 * Controls how Internal Cooldown (ICD) is evaluated when applying an element through {@link VeloriseElementAPI}.
 */
public enum IcdPolicy {
    /** Standard Genshin Impact ICD (2.5 seconds or every 3 hits per attacker-target-element). */
    STANDARD,
    /** Bypass standard ICD and guarantee elemental gauge application on this hit. */
    BYPASS
}
