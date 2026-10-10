package com.rettwilson.saturationguy;

import java.lang.ref.WeakReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Says "ruhhhhh" in chat when your saturation runs out. */
public final class RuhCallout {
	public static final String MESSAGE = "ruhhhhh";

	/** The player the last value belongs to. A new one appears on join, respawn and dimension change. */
	private static WeakReference<LocalPlayer> trackedPlayer = new WeakReference<>(null);
	private static float lastSaturation;

	private RuhCallout() {
	}

	/**
	 * Called with each vanilla health/hunger update. Vanilla always sends one the tick
	 * saturation hits zero, so this catches the moment on any server.
	 */
	public static void onSaturationUpdate(float saturation) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		// A new player starts at a placeholder 5.0 until the server's first update, so
		// that first update only sets the baseline. Otherwise joining at 0 would count as a dip.
		if (trackedPlayer.get() != player) {
			trackedPlayer = new WeakReference<>(player);
			lastSaturation = saturation;
			return;
		}

		boolean dipped = lastSaturation > 0.0F && saturation <= 0.0F;
		lastSaturation = saturation;
		if (dipped && SaturationGuyConfig.sayRuh()) {
			Callouts.say(player, MESSAGE);
		}
	}
}
