package com.rettwilson.saturationguy;

import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps each player's client up to date on their saturation.
 *
 * <p>Vanilla only sends saturation along with a health or hunger change, so it goes
 * stale whenever it drains on its own (sprinting or jumping at full health, for
 * example). This sends it every tick it changes, to players who have the mod.
 */
public final class SaturationSync {
	/** Last value sent to each player. Weak keys, so players who leave drop out. */
	private static final Map<ServerPlayer, Float> LAST_SENT = new WeakHashMap<>();

	private SaturationSync() {
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ServerPlayNetworking.canSend(player, SaturationPayload.TYPE)) {
				continue;
			}

			float saturation = player.getFoodData().getSaturationLevel();
			Float last = LAST_SENT.get(player);
			if (last == null || last != saturation) {
				LAST_SENT.put(player, saturation);
				ServerPlayNetworking.send(player, new SaturationPayload(saturation));
			}
		}
	}
}
