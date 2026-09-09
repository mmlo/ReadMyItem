package dev.mml.readmyitem.tts;

import dev.mml.readmyitem.ReadMyItemMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copies bundled Piper voices from the mod jar onto disk so piper-jni can mmap them.
 * Not a subprocess and not a network download.
 */
public final class BundledVoiceExtractor {
	private static final AtomicBoolean running = new AtomicBoolean();
	private static final AtomicBoolean finished = new AtomicBoolean();
	private static volatile boolean failed;

	private BundledVoiceExtractor() {
	}

	public static boolean isFinished() {
		return finished.get();
	}

	public static boolean failed() {
		return failed;
	}

	public static void extractAll() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		failed = false;
		try {
			VoiceLibrary.ensureVoiceDirectory();
			int copied = 0;
			for (String fileName : VoiceChecksums.bundledFileNames()) {
				String langDir = fileName.startsWith("en_US") ? "en_US" : "pt_BR";
				Path destDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
						.resolve("readmyitem").resolve("voices").resolve(langDir);
				Files.createDirectories(destDir);
				if (extractOne(destDir, fileName)) {
					copied++;
				}
			}
			if (copied > 0) {
				ReadMyItemMod.LOGGER.info("[ReadMyItem] voz bundled extraída ({} arquivo(s))", copied);
			}
			logReadyStem();
		} catch (Throwable t) {
			failed = true;
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] falha ao extrair voz bundled: {}", t.toString());
		} finally {
			finished.set(true);
			running.set(false);
		}
	}

	static boolean extractOne(Path dir, String fileName) throws Exception {
		Path dest = VoiceChecksums.resolveInside(dir, fileName);
		if (dest == null) {
			failed = true;
			return false;
		}
		String expected = VoiceChecksums.sha256ForFileName(fileName);
		if (expected != null && Files.isRegularFile(dest) && hashMatches(dest, expected)) {
			return false;
		}
		String resource = VoiceChecksums.bundledResourcePath(fileName);
		try (InputStream in = BundledVoiceExtractor.class.getResourceAsStream(resource)) {
			if (in == null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] resource bundled ausente: {}", resource);
				failed = true;
				return false;
			}
			Files.createDirectories(dest.getParent());
			Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".part");
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (OutputStream out = Files.newOutputStream(tmp)) {
				byte[] buf = new byte[32 * 1024];
				int n;
				while ((n = in.read(buf)) >= 0) {
					if (n == 0) {
						continue;
					}
					digest.update(buf, 0, n);
					out.write(buf, 0, n);
				}
			}
			String actual = HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
			if (expected != null && !VoiceChecksums.sha256Equals(expected, actual)) {
				Files.deleteIfExists(tmp);
				failed = true;
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] checksum bundled divergente: {}", fileName);
				return false;
			}
			try {
				Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException ignored) {
				Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
			}
			String stem = fileName.endsWith(".onnx.json")
					? fileName.substring(0, fileName.length() - ".onnx.json".length())
					: fileName.endsWith(".onnx")
							? fileName.substring(0, fileName.length() - ".onnx".length())
							: fileName;
			ReadMyItemMod.LOGGER.info("[ReadMyItem] voz bundled extraída: {}", stem);
			return true;
		}
	}

	private static void logReadyStem() {
		String stem = VoiceLibrary.installedStemOrEmpty();
		if (!stem.isEmpty()) {
			ReadMyItemMod.LOGGER.info("[ReadMyItem] voz bundled pronta: {}", stem);
		}
	}

	static boolean hashMatches(Path file, String expectedSha) {
		try {
			return VoiceChecksums.sha256Equals(expectedSha, VoiceLibrary.sha256(file));
		} catch (Exception e) {
			return false;
		}
	}
}
