package com.velorise.staves.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Resets vanilla floating-kick counter while the player is legitimately wall-climbing.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
    @Accessor("aboveGroundTickCount")
    void velorise$setAboveGroundTickCount(int value);
}
