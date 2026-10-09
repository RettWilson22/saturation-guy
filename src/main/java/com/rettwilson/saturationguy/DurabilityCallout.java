package com.rettwilson.saturationguy;

import java.lang.ref.WeakReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Calls it out in chat when a tool, weapon or piece of armor gets down to 5% durability. */
public final class DurabilityCallout {
	public static final String MESSAGE_PREFIX = "woah woah woah now heres a GUY whose about to have a broken ";

	/** What was in each inventory slot last tick, and whether it was already at 5% or under. */
	private static Item[] lastItems = new Item[0];
	private static boolean[] lastLow = new boolean[0];
	private static WeakReference<LocalPlayer> trackedPlayer = new WeakReference<>(null);

	private DurabilityCallout() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.getConnection() == null) {
			return;
		}

		Inventory inventory = player.getInventory();
		int size = inventory.getContainerSize();
		boolean fresh = trackedPlayer.get() != player || lastItems.length != size;
		if (fresh) {
			trackedPlayer = new WeakReference<>(player);
			lastItems = new Item[size];
			lastLow = new boolean[size];
		}

		// Swapping two of the same tool in an open inventory looks just like one slot's
		// tool wearing down, so only watch while no inventory screen is open.
		boolean watching = !fresh && !(minecraft.gui.screen() instanceof AbstractContainerScreen<?>);

		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = inventory.getItem(slot);
			boolean low = isLow(stack);
			if (watching && low && !lastLow[slot] && stack.getItem() == lastItems[slot]) {
				String name = StringUtil.filterText(stack.getHoverName().getString());
				minecraft.getConnection().sendChat(StringUtil.trimChatMessage(MESSAGE_PREFIX + name));
			}
			lastItems[slot] = stack.getItem();
			lastLow[slot] = low;
		}
	}

	/** At or under 5% of its durability left. */
	private static boolean isLow(ItemStack stack) {
		if (!stack.isDamageableItem()) {
			return false;
		}
		int left = stack.getMaxDamage() - stack.getDamageValue();
		return left * 20 <= stack.getMaxDamage();
	}
}
