package com.rettwilson.saturationguy;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SaturationGuyClient implements ClientModInitializer {
	/** Toggles the number next to the bar and the "ruhhhhh" message together. */
	public static KeyMapping toggleKey;
	public static KeyMapping copyCoordinatesKey;

	@Override
	public void onInitializeClient() {
		SaturationGuyConfig.load();

		// Drawn right after the food bar, and registered as a right-side status bar so
		// the air bubbles (and other mods' bars) move up to make room for it.
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, SaturationBar.ID, new SaturationBar());
		HudStatusBarHeightRegistry.addRight(SaturationBar.ID, SaturationBar::rowHeight);

		// Only arrives when the server (or your singleplayer world) also runs this mod.
		ClientPlayNetworking.registerGlobalReceiver(SaturationPayload.TYPE,
				(payload, context) -> context.player().getFoodData().setSaturation(payload.saturation()));

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SaturationGuy.MOD_ID, "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.saturationguy.toggle_number", InputConstants.KEY_J, category));
		copyCoordinatesKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.saturationguy.copy_coordinates", InputConstants.KEY_N, category));
		ClientTickEvents.END_CLIENT_TICK.register(SaturationGuyClient::handleKeys);
		ClientTickEvents.END_CLIENT_TICK.register(DurabilityCallout::tick);
	}

	private static void handleKeys(Minecraft minecraft) {
		while (toggleKey.consumeClick()) {
			// If either one is off (say, from an older version's setting), the press turns both on.
			boolean on = !(SaturationGuyConfig.showNumber() && SaturationGuyConfig.sayRuh());
			SaturationGuyConfig.setExtras(on);
			if (minecraft.player != null) {
				minecraft.player.sendOverlayMessage(Component.translatable(
						on ? "message.saturationguy.extras_on" : "message.saturationguy.extras_off"));
			}
		}

		while (copyCoordinatesKey.consumeClick()) {
			LocalPlayer player = minecraft.player;
			if (player != null) {
				BlockPos pos = player.blockPosition();
				String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
				minecraft.keyboardHandler.setClipboard(coordinates);
				player.sendOverlayMessage(Component.translatable("message.saturationguy.copied_coordinates", coordinates));
			}
		}
	}
}
