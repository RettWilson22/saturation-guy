package com.rettwilson.saturationguy;

import java.util.Locale;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * A gold bar above the hunger bar showing the player's saturation.
 *
 * <p>Saturation is hidden in vanilla, but the client still holds a copy. With this
 * mod on the server (always true in singleplayer) {@link SaturationSync} keeps that
 * copy exact every tick. On a server without it, it is vanilla's copy, which only
 * refreshes when your health or hunger changes.
 */
public final class SaturationBar implements HudElement {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(SaturationGuy.MOD_ID, "saturation_bar");

	/** Same row height vanilla uses for each status bar. */
	private static final int ROW_HEIGHT = 10;
	/** Spans the ten food icons exactly. */
	private static final int WIDTH = 81;
	private static final int HEIGHT = 6;
	/** Saturation can never exceed the food level, which tops out at 20. */
	private static final float MAX_SATURATION = 20.0F;

	private static final int BORDER = 0xFF1E1400;
	private static final int EMPTY = 0xFF4A3B12;
	private static final int GOLD_TOP = 0xFFFFE57A;
	private static final int GOLD_BOTTOM = 0xFFE09B00;
	private static final int TEXT = 0xFFFFD23F;

	/** Takes up a row only while the food bar itself is showing (not on a horse, etc.). */
	public static int rowHeight(Player player) {
		return HudStatusBarHeightRegistry.getElementHeight(VanillaHudElements.FOOD_BAR) > 0 ? ROW_HEIGHT : 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.getCameraEntity() instanceof Player player)) {
			return;
		}

		float saturation = Mth.clamp(player.getFoodData().getSaturationLevel(), 0.0F, MAX_SATURATION);

		int right = graphics.guiWidth() / 2 + 91;
		int left = right - WIDTH;
		int top = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ID) + 2;
		int bottom = top + HEIGHT;

		graphics.fill(left, top, right, bottom, BORDER);
		graphics.fill(left + 1, top + 1, right - 1, bottom - 1, EMPTY);

		// Fills from the right, the same direction the hunger bar fills.
		int innerWidth = WIDTH - 2;
		int filled = Math.round(saturation / MAX_SATURATION * innerWidth);
		if (filled > 0) {
			graphics.fillGradient(right - 1 - filled, top + 1, right - 1, bottom - 1, GOLD_TOP, GOLD_BOTTOM);
		}

		if (SaturationGuyConfig.showNumber()) {
			String value = String.format(Locale.ROOT, "%.1f", saturation);
			graphics.text(minecraft.font, value, right + 3, top - 1, TEXT, true);
		}
	}
}
