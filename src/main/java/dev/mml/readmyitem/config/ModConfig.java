package dev.mml.readmyitem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.tts.VoiceChecksums;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
	public enum AppLanguage {
		AUTO("auto"),
		PT_BR("pt_br"),
		EN_US("en_us");

		public final String code;
		AppLanguage(String code) {
			this.code = code;
		}

		public static AppLanguage fromCode(String code) {
			if ("pt_br".equals(code)) return PT_BR;
			if ("en_us".equals(code)) return EN_US;
			return AUTO;
		}
	}

	public boolean enabled = true;
	public int hoverDelayMs = 450;
	public int dwellDetalhesMs = 900;
	public boolean lerNome = true;
	public boolean lerDescricao = true;
	public boolean lerQuantidade = true;
	public boolean falarSlotVazio = false;
	public boolean sempreDurabilidade = false;
	public boolean modoTecnico = false;
	public boolean overlay = false;
	public OverlayPosition overlayPosition = OverlayPosition.TOP;
	public float velocidadeFala = 0.92f;
	public float volumeFala = 1.0f;
	public String voz = VoiceChecksums.FABER;
	public AppLanguage linguagem = AppLanguage.AUTO;
	public boolean interromperAoSair = true;
	public int maxCaracteres = 280;

	public enum OverlayPosition {
		TOP,
		TOP_LEFT,
		TOP_RIGHT
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance = new ModConfig();

	public static ModConfig get() {
		return instance;
	}

	public static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("readmyitem.json");
	}

	public static Path voiceDir() {
		String langDir = get().linguagem == AppLanguage.EN_US ? "en_US" : "pt_BR";
		return FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem").resolve("voices").resolve(langDir);
	}

	public static void load() {
		Path file = configFile();
		if (Files.isRegularFile(file)) {
			try {
				if (Files.size(file) > 64 * 1024) {
					ReadMyItemMod.LOGGER.warn("[ReadMyItem] config grande demais, usando padrão");
					instance = new ModConfig();
				} else {
					try (Reader reader = Files.newBufferedReader(file)) {
						ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
						if (loaded != null) {
							instance = loaded;
							instance.sanitize();
						}
					}
				}
			} catch (Exception e) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] config inválida, usando padrão: {}", e.toString());
				instance = new ModConfig();
			}
		}
		save();
	}

	public static void save() {
		instance.sanitize();
		Path file = configFile();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] não foi possível gravar config: {}", e.toString());
		}
	}

	public void sanitize() {
		hoverDelayMs = clamp(hoverDelayMs, 0, 3000);
		dwellDetalhesMs = clamp(dwellDetalhesMs, 0, 5000);
		velocidadeFala = clamp(velocidadeFala, 0.75f, 1.35f);
		volumeFala = clamp(volumeFala, 0f, 1f);
		maxCaracteres = clamp(maxCaracteres, 40, 2000);
		if (voz == null || voz.isBlank()) {
			voz = linguagem == AppLanguage.EN_US ? VoiceChecksums.EN_US_DEFAULT : VoiceChecksums.FABER;
		}
		if (linguagem == null) {
			linguagem = AppLanguage.AUTO;
		}
		if (overlayPosition == null) {
			overlayPosition = OverlayPosition.TOP;
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
