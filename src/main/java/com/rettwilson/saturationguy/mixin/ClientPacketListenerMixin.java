package com.rettwilson.saturationguy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rettwilson.saturationguy.GuyCallout;
import com.rettwilson.saturationguy.RuhCallout;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;

// TAIL is past the hop onto the main thread, so these run once per packet, on the game thread.
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleDamageEvent", at = @At("TAIL"))
	private void saturationguy$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
		GuyCallout.onDamageEvent((ClientPacketListener) (Object) this, packet);
	}

	@Inject(method = "handleSetHealth", at = @At("TAIL"))
	private void saturationguy$onSetHealth(ClientboundSetHealthPacket packet, CallbackInfo ci) {
		RuhCallout.onSaturationUpdate((ClientPacketListener) (Object) this, packet.getSaturation());
	}
}
