package dev.mml.readmyitem.tts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

public final class VoiceLibrary {
	public record VoiceFiles(String stem, Path onnx, Path json) {
	}

	private static volatile VoiceFiles cached;
	private static volatile String cachedConfigKey;

	private VoiceLibrary() {
	}

	public static void invalidateCache() {
		cached = null;
		cachedConfigKey = null;
	}

	public static Path voiceDirectory() {
		return ModConfig.voiceDir();
	}

	public static void ensureVoiceDirectory() {
		try {
			Files.createDirectories(voiceDirectory());
		} catch (IOException e) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] não criou pasta de vozes: {}", e.toString());
		}
	}

	public static Optional<VoiceFiles> resolveConfiguredVoice() {
		String conf = ModConfig.get().voz;
		if (conf == null || conf.isBlank()) {
			conf = ModConfig.get().linguagem == ModConfig.AppLanguage.EN_US ? VoiceChecksums.EN_US_DEFAULT : VoiceChecksums.FABER;
		}
		final String configured = conf;
		VoiceFiles hit = cached;
		if (hit != null && configured.equals(cachedConfigKey)) {
			return Optional.of(hit);
		}
		ensureVoiceDirectory();
		Optional<VoiceFiles> found = findByStem(configured);
		if (found.isEmpty() && !VoiceChecksums.FABER.equals(configured)) {
			found = findByStem(VoiceChecksums.FABER);
		}
		if (found.isEmpty() && !VoiceChecksums.EN_US_DEFAULT.equals(configured)) {
			found = findByStem(VoiceChecksums.EN_US_DEFAULT);
		}
		if (found.isEmpty()) {
			// fallback to any available voice
			found = getAvailableStems().stream().findFirst().flatMap(VoiceLibrary::findByStem);
		}
		found.ifPresent(voice -> {
			cached = voice;
			cachedConfigKey = configured;
		});
		return found;
	}

	public static Optional<VoiceFiles> findByStem(String stem) {
		Path root = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem").resolve("voices");
		String langDir = stem.startsWith("en_US-") ? "en_US" : "pt_BR";
		Path dir = root.resolve(langDir);
		Path onnx = VoiceChecksums.resolveInside(dir, stem + ".onnx");
		Path json = VoiceChecksums.resolveInside(dir, stem + ".onnx.json");
		if (onnx == null || json == null) {
			return Optional.empty();
		}
		return validatePair(stem, onnx, json);
	}

	public static boolean isReady() {
		if (cached != null) {
			return true;
		}
		String configured = ModConfig.get().voz;
		if (configured == null || configured.isBlank()) {
			configured = ModConfig.get().linguagem == ModConfig.AppLanguage.EN_US ? VoiceChecksums.EN_US_DEFAULT : VoiceChecksums.FABER;
		}
		return pairExists(configured) || !getAvailableStems().isEmpty();
	}

	private static boolean pairExists(String stem) {
		Path root = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem").resolve("voices");
		String langDir = stem.startsWith("en_US-") ? "en_US" : "pt_BR";
		Path dir = root.resolve(langDir);
		Path onnx = VoiceChecksums.resolveInside(dir, stem + ".onnx");
		Path json = VoiceChecksums.resolveInside(dir, stem + ".onnx.json");
		return onnx != null && json != null && Files.isRegularFile(onnx) && Files.isRegularFile(json);
	}

	public static java.util.List<String> getAvailableStems() {
		ensureVoiceDirectory();
		java.util.List<String> result = new java.util.ArrayList<>();
		Path root = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem").resolve("voices");
		for (String langDir : new String[]{"pt_BR", "en_US"}) {
			Path dir = root.resolve(langDir);
			if (Files.isDirectory(dir)) {
				try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
					stream.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.filter(n -> n.endsWith(".onnx"))
							.map(n -> n.substring(0, n.length() - 5))
							.filter(stem -> pairExists(stem))
							.forEach(result::add);
				} catch (IOException ignored) {}
			}
		}
		java.util.Collections.sort(result);
		return result;
	}

	public static String installedStemOrEmpty() {
		return resolveConfiguredVoice().map(VoiceFiles::stem).orElse("");
	}

	private static Optional<VoiceFiles> validatePair(String stem, Path onnx, Path json) {
		if (!Files.isRegularFile(onnx) || !Files.isRegularFile(json)) {
			return Optional.empty();
		}
		try {
			if (Files.size(onnx) > VoiceChecksums.MAX_ONNX_BYTES || Files.size(json) > VoiceChecksums.MAX_JSON_BYTES) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] voz {} rejeitada: arquivo grande demais", stem);
				return Optional.empty();
			}
			if (!acceptsConfig(json)) {
				return Optional.empty();
			}
			String expectedOnnx = VoiceChecksums.sha256ForFileName(stem + ".onnx");
			if (expectedOnnx != null) {
				String actual = sha256(onnx);
				if (!VoiceChecksums.sha256Equals(expectedOnnx, actual)) {
					ReadMyItemMod.LOGGER.warn("[ReadMyItem] checksum ONNX de {} não confere", stem);
					return Optional.empty();
				}
			}
			return Optional.of(new VoiceFiles(stem, onnx, json));
		} catch (Exception e) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] voz {} inválida: {}", stem, e.toString());
			return Optional.empty();
		}
	}

	private static boolean acceptsConfig(Path json) throws IOException {
		JsonElement parsed = JsonParser.parseString(Files.readString(json));
		if (!parsed.isJsonObject()) {
			return false;
		}
		JsonObject root = parsed.getAsJsonObject();
		int speakers = root.has("num_speakers") ? root.get("num_speakers").getAsInt() : 1;
		if (speakers > 1) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] modelo multi-speaker recusado");
			return false;
		}
		if (root.has("speaker_id_map") && root.get("speaker_id_map").isJsonObject()
				&& root.getAsJsonObject("speaker_id_map").size() > 1) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] modelo multi-speaker recusado");
			return false;
		}
		return true;
	}

	public static String sha256(Path file) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (var in = Files.newInputStream(file)) {
			byte[] buf = new byte[32 * 1024];
			int n;
			while ((n = in.read(buf)) >= 0) {
				if (n > 0) {
					digest.update(buf, 0, n);
				}
			}
		}
		return HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
	}
}
