package com.rettwilson.saturationguy.test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.authlib.GameProfile;

import com.rettwilson.saturationguy.DurabilityCallout;
import com.rettwilson.saturationguy.GuyCallout;
import com.rettwilson.saturationguy.RuhCallout;
import com.rettwilson.saturationguy.SaturationGuyClient;
import com.rettwilson.saturationguy.SaturationGuyConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

public class SaturationGuyClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		AtomicInteger guyMessages = new AtomicInteger();
		AtomicInteger ruhMessages = new AtomicInteger();
		List<String> durabilityMessages = new CopyOnWriteArrayList<>();
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			if (message.signedContent().equals(GuyCallout.MESSAGE)) {
				guyMessages.incrementAndGet();
			} else if (message.signedContent().equals(RuhCallout.MESSAGE)) {
				ruhMessages.incrementAndGet();
			} else if (message.signedContent().startsWith(DurabilityCallout.MESSAGE_PREFIX)) {
				durabilityMessages.add(message.signedContent());
			}
		});

		TestWorldSave worldSave;
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			worldSave = singleplayer.getWorldSave();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("difficulty easy");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			context.waitTicks(20);

			setFood(context, server, 20, 20.0F);
			context.takeScreenshot("1-saturation-full");

			// Hunger stays at 20 here, so vanilla would never tell the client. Only the mod's sync can.
			setFood(context, server, 20, 7.6F);
			context.takeScreenshot("2-saturation-7.6");

			// J turns the number and "ruhhhhh" off together...
			context.getInput().pressKey(SaturationGuyClient.toggleKey);
			context.waitTicks(5);
			check(!SaturationGuyConfig.showNumber() && !SaturationGuyConfig.sayRuh(), "J should turn both off");
			context.takeScreenshot("2b-extras-off");
			setFood(context, server, 20, 0.0F);
			context.waitTicks(40);
			check(ruhMessages.get() == 0, "no ruhhhhh while toggled off, got " + ruhMessages.get());

			// ...and back on.
			setFood(context, server, 20, 7.6F);
			context.getInput().pressKey(SaturationGuyClient.toggleKey);
			context.waitTicks(5);
			check(SaturationGuyConfig.showNumber() && SaturationGuyConfig.sayRuh(), "second J press should turn both on");

			setFood(context, server, 6, 0.0F);
			server.waitFor(s -> ruhMessages.get() >= 1, 100);
			context.waitTicks(20);
			check(ruhMessages.get() == 1, "expected 1 ruhhhhh after dipping to 0, got " + ruhMessages.get());
			context.takeScreenshot("3-saturation-empty-ruh");

			setFood(context, server, 20, 12.4F);
			server.runCommand("execute at @p run fill ~-1 ~ ~-1 ~1 ~1 ~1 water");
			context.waitTicks(60);
			context.takeScreenshot("4-underwater-air-bubbles-move-up");
			server.runCommand("execute at @p run fill ~-1 ~ ~-1 ~1 ~1 ~1 air");

			// Hurting yourself, or damage with no attacker, should stay quiet.
			context.runOnClient(mc -> {
				mc.getConnection().handleDamageEvent(new ClientboundDamageEventPacket(mc.player, mc.level.damageSources().playerAttack(mc.player)));
				mc.getConnection().handleDamageEvent(new ClientboundDamageEventPacket(mc.player, mc.level.damageSources().generic()));
			});
			context.waitTicks(40);
			check(guyMessages.get() == 0, "expected no message for self/no-attacker damage, got " + guyMessages.get());

			// Two hits from another player back to back: one message, thanks to the cooldown.
			context.runOnClient(mc -> {
				RemotePlayer attacker = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Attacker"));
				attacker.setId(1_000_000);
				attacker.setPos(mc.player.position().add(2, 0, 0));
				mc.level.addEntity(attacker);

				for (int i = 0; i < 2; i++) {
					mc.getConnection().handleDamageEvent(new ClientboundDamageEventPacket(mc.player, mc.level.damageSources().playerAttack(attacker)));
				}
			});
			server.waitFor(s -> guyMessages.get() >= 1, 100);
			context.waitTicks(40);
			check(guyMessages.get() == 1, "expected exactly 1 GUYY message, got " + guyMessages.get());
			context.takeScreenshot("5-guy-chat-message");

			// N copies your block coordinates. It's the real clipboard, so put back what was there.
			String savedClipboard = context.computeOnClient(mc -> mc.keyboardHandler.getClipboard());
			context.getInput().pressKey(SaturationGuyClient.copyCoordinatesKey);
			context.waitTicks(5);
			String expected = context.computeOnClient(mc -> {
				BlockPos pos = mc.player.blockPosition();
				return pos.getX() + " " + pos.getY() + " " + pos.getZ();
			});
			String copied = context.computeOnClient(mc -> mc.keyboardHandler.getClipboard());
			context.takeScreenshot("6-copied-coordinates");
			context.runOnClient(mc -> mc.keyboardHandler.setClipboard(savedClipboard));
			check(expected.equals(copied), "clipboard should be '" + expected + "', got '" + copied + "'");

			// Durability. Diamond pickaxe: 1561 max, so 5% is 78 left (damage 1483+). Iron chestplate: 240 max, 12 left (228+).
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_pickaxe[minecraft:damage=1400]");
			server.runCommand("item replace entity @p armor.chest with minecraft:iron_chestplate[minecraft:damage=220]");
			// Already nearly broken when it shows up, and then switched to: neither is "getting to" 5%.
			server.runCommand("item replace entity @p hotbar.1 with minecraft:diamond_pickaxe[minecraft:damage=1550]");
			context.waitTicks(20);
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(20);
			check(durabilityMessages.isEmpty(), "no durability message yet, got " + durabilityMessages);

			// Wear both down past 5%: one message each, naming the item.
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.getMainHandItem().setDamageValue(1490);
				player.getItemBySlot(EquipmentSlot.CHEST).setDamageValue(230);
			});
			server.waitFor(s -> durabilityMessages.size() >= 2, 100);
			context.waitTicks(20);
			check(Set.copyOf(durabilityMessages).equals(Set.of(
					DurabilityCallout.MESSAGE_PREFIX + "Diamond Pickaxe",
					DurabilityCallout.MESSAGE_PREFIX + "Iron Chestplate")) && durabilityMessages.size() == 2,
					"unexpected durability messages: " + durabilityMessages);
			context.takeScreenshot("7-durability-warning");

			// Wearing further doesn't repeat it.
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().getMainHandItem().setDamageValue(1500));
			context.waitTicks(20);
			check(durabilityMessages.size() == 2, "durability message repeated: " + durabilityMessages);

			// Leave the world sitting at 0 saturation for the rejoin check below.
			setFood(context, server, 20, 0.0F);
			server.waitFor(s -> ruhMessages.get() >= 2, 100);
		}

		// Rejoining at 0 isn't a dip, even though the client briefly holds a placeholder 5.0.
		try (TestSingleplayerContext rejoined = worldSave.open()) {
			context.waitFor(mc -> mc.player != null && clientSaturation(mc) == 0.0F, 200);
			context.waitTicks(60);
			check(ruhMessages.get() == 2, "rejoining at 0 saturation shouldn't say ruhhhhh, got " + ruhMessages.get());
		}
	}

	private static void setFood(ClientGameTestContext context, TestServerContext server, int food, float saturation) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.getFoodData().setFoodLevel(food);
			player.getFoodData().setSaturation(saturation);
		});
		context.waitFor(mc -> clientSaturation(mc) == saturation && mc.player.getFoodData().getFoodLevel() == food, 100);
		context.waitTicks(5);
	}

	private static float clientSaturation(Minecraft mc) {
		return mc.player.getFoodData().getSaturationLevel();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
