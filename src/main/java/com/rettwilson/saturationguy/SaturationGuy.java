package com.rettwilson.saturationguy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class SaturationGuy implements ModInitializer {
	public static final String MOD_ID = "saturationguy";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(SaturationPayload.TYPE, SaturationPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(SaturationSync::tick);
	}
}
