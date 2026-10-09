package com.rettwilson.saturationguy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Says "Now here's a GUYY" in chat when another player hurts you. */
public final class GuyCallout {
	public static final String MESSAGE = "Now here's a GUYY";

	/**
	 * Minimum time between messages. A PvP fight can land two hits a second, and
	 * servers kick players who chat that fast.
	 */
	private static final long COOLDOWN_MS = 3000;

	private static long nextAllowedAt = Long.MIN_VALUE;

	private GuyCallout() {
	}

	public static void onDamageEvent(ClientPacketListener connection, ClientboundDamageEventPacket packet) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = minecraft.player;
		ClientLevel level = minecraft.level;
		if (self == null || level == null || packet.entityId() != self.getId()) {
			return;
		}

		// The "cause" is whoever is responsible: the attacker for a punch or sword hit,
		// the shooter for an arrow or trident, the thrower for a splash potion.
		Entity cause = level.getEntity(packet.sourceCauseId());
		if (!(cause instanceof Player) || cause == self) {
			return;
		}

		long now = Util.getMillis();
		if (now < nextAllowedAt) {
			return;
		}
		nextAllowedAt = now + COOLDOWN_MS;

		connection.sendChat(MESSAGE);
	}
}
