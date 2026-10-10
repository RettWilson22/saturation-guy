package com.rettwilson.saturationguy;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

/** Client settings, saved to {@code config/saturationguy.properties}. */
public final class SaturationGuyConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(SaturationGuy.MOD_ID);
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("saturationguy.properties");

	private static boolean showNumber = true;
	private static boolean sayRuh = true;
	private static boolean publicChat = false;

	private SaturationGuyConfig() {
	}

	public static boolean showNumber() {
		return showNumber;
	}

	public static boolean sayRuh() {
		return sayRuh;
	}

	/** Whether the callouts go to public chat instead of just your own. Off unless set in the file. */
	public static boolean publicChat() {
		return publicChat;
	}

	/** Turns the number and the "ruhhhhh" message on or off together (the J key). */
	public static void setExtras(boolean value) {
		showNumber = value;
		sayRuh = value;
		save();
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}

		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			properties.load(reader);
		} catch (IOException e) {
			LOGGER.warn("Couldn't read {}", FILE, e);
			return;
		}
		showNumber = Boolean.parseBoolean(properties.getProperty("showNumber", "true"));
		sayRuh = Boolean.parseBoolean(properties.getProperty("sayRuh", "true"));
		publicChat = Boolean.parseBoolean(properties.getProperty("publicChat", "false"));
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("showNumber", Boolean.toString(showNumber));
		properties.setProperty("sayRuh", Boolean.toString(sayRuh));
		properties.setProperty("publicChat", Boolean.toString(publicChat));
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			properties.store(writer, "Saturation Guy settings");
		} catch (IOException e) {
			LOGGER.warn("Couldn't save {}", FILE, e);
		}
	}
}
