package dev.combomacros;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class MacroConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("combomacros.json");

	public static MacroConfig INSTANCE = new MacroConfig();

	public boolean enabled = true;
	/** Задержка между шагами, мс (в .ahk было 15) */
	public int delayMs = 15;

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				MacroConfig loaded = GSON.fromJson(Files.readString(FILE), MacroConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(INSTANCE));
		} catch (Exception ignored) {
		}
	}
}
