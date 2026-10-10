package com.rettwilson.saturationguy;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

final class Callouts {
	private Callouts() {
	}

	/**
	 * Sends the message to public chat if the player turned that on, otherwise shows it
	 * only in their own chat. Plenty of servers kick players for automated messages.
	 */
	static void say(LocalPlayer player, String message) {
		if (SaturationGuyConfig.publicChat()) {
			player.connection.sendChat(message);
		} else {
			player.sendSystemMessage(Component.literal(message));
		}
	}
}
