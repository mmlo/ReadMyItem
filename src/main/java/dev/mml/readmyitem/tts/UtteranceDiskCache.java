package dev.mml.readmyitem.tts;

import dev.mml.readmyitem.ReadMyItemMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Global PCM cache of already-heard utterances (not per-world).
 * Lives under the game config dir so it survives close/open and is shared by every save.
 * Lookups stay on the TTS thread. Filenames use a locale tag for future languages.
 */
public final class UtteranceDiskCache {
	public static final String LOCALE_TAG = "pt-br";
	private static final String MAGIC = "INP1";
	private static final int MAX_FILES = 512;

	private UtteranceDiskCache() {
	}

	public static Path cacheDir() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem")
				.resolve("cache")
				.resolve(LOCALE_TAG);
	}

	public static String fileName(String voice, float speed, String text) {
		String digest = sha256Utf8(UtteranceCache.key(voice, speed, text));
		return LOCALE_TAG + "_" + digest + ".pcm";
	}

	public static Path pathFor(String voice, float speed, String text) {
		return cacheDir().resolve(fileName(voice, speed, text));
	}

	public static UtteranceCache.Entry load(String voice, float speed, String text) {
		return loadFrom(cacheDir(), voice, speed, text);
	}

	public static UtteranceCache.Entry loadFrom(Path dir, String voice, float speed, String text) {
		Path file = dir.resolve(fileName(voice, speed, text));
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try {
			byte[] raw = Files.readAllBytes(file);
			if (raw.length < 12) {
				Files.deleteIfExists(file);
				return null;
			}
			ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
			byte[] magic = new byte[4];
			buf.get(magic);
			if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
				Files.deleteIfExists(file);
				return null;
			}
			int sampleRate = buf.getInt();
			int count = buf.getInt();
			if (sampleRate < 8000 || sampleRate > 48000 || count < 0 || count > 2_000_000
					|| buf.remaining() < count * 2) {
				Files.deleteIfExists(file);
				return null;
			}
			short[] pcm = new short[count];
			buf.asShortBuffer().get(pcm);
			return new UtteranceCache.Entry(pcm, sampleRate);
		} catch (Exception e) {
			ReadMyItemMod.LOGGER.debug("[ReadMyItem] cache de fala ilegível: {}", e.toString());
			try {
				Files.deleteIfExists(file);
			} catch (IOException ignored) {
				// next synthesis will recreate
			}
			return null;
		}
	}

	public static void save(String voice, float speed, String text, short[] pcm, int sampleRate) {
		saveTo(cacheDir(), voice, speed, text, pcm, sampleRate);
	}

	public static void saveTo(Path dir, String voice, float speed, String text, short[] pcm, int sampleRate) {
		if (pcm == null || pcm.length == 0 || text == null || text.isBlank() || dir == null) {
			return;
		}
		Path file = dir.resolve(fileName(voice, speed, text));
		Path tmp = dir.resolve(file.getFileName().toString() + ".part");
		try {
			Files.createDirectories(dir);
			ByteBuffer buf = ByteBuffer.allocate(12 + pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
			buf.put(MAGIC.getBytes(StandardCharsets.US_ASCII));
			buf.putInt(sampleRate);
			buf.putInt(pcm.length);
			buf.asShortBuffer().put(pcm);
			Files.write(tmp, buf.array());
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
			trimIfNeeded(dir);
		} catch (Exception e) {
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException ignored) {
				// ignore
			}
			ReadMyItemMod.LOGGER.debug("[ReadMyItem] não gravou cache de fala: {}", e.toString());
		}
	}

	private static void trimIfNeeded(Path dir) throws IOException {
		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, LOCALE_TAG + "_*.pcm")) {
			for (Path path : stream) {
				files.add(path);
			}
		}
		if (files.size() <= MAX_FILES) {
			return;
		}
		files.sort(Comparator.comparingLong(p -> {
			try {
				return Files.getLastModifiedTime(p).toMillis();
			} catch (IOException e) {
				return 0L;
			}
		}));
		int extra = files.size() - MAX_FILES;
		for (int i = 0; i < extra; i++) {
			Files.deleteIfExists(files.get(i));
		}
	}

	static String sha256Utf8(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash).toLowerCase(Locale.ROOT);
		} catch (Exception e) {
			return Integer.toHexString(value.hashCode());
		}
	}
}
