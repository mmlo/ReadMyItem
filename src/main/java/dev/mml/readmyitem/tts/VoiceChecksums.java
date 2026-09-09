package dev.mml.readmyitem.tts;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * SHA-256 of the official Hugging Face v1.0.0 Piper pt_BR files.
 * ONNX hashes are Hugging Face LFS SHA-256; JSON hashes were computed from the
 * published bytes of the same revision.
 */
public final class VoiceChecksums {
	public static final long MAX_ONNX_BYTES = 80L * 1024L * 1024L;
	public static final long MAX_JSON_BYTES = 256L * 1024L;

	public static final String FABER = "pt_BR-faber-medium";
	public static final String EDRESSON = "pt_BR-edresson-low";
	public static final String LESSAC = "en_US-lessac-low";
	public static final String EN_US_DEFAULT = LESSAC;

	public static final String FABER_ONNX_SHA256 =
			"858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7";
	public static final String FABER_JSON_SHA256 =
			"7e694de195ae3fc36dd732c445eb04fb49b649854893cb5506b978f0d50a1d6f";
	public static final String EDRESSON_ONNX_SHA256 =
			"de4cecee38b30bb1a6378a337af605d59f0c377df702c6a6752870db8991cd84";
	public static final String EDRESSON_JSON_SHA256 =
			"f138992d2e777d1e3aa0bbb14c2d324307b0f342c1bcf20978765b3bea506c56";
	public static final String LESSAC_ONNX_SHA256 =
			"f7d01dde371555732c4c314111ac79672b1a5ce2fc19266ab42178fd8df7f375";
	public static final String LESSAC_JSON_SHA256 =
			"45754dfdebb3b8661c3fc564713772deec6e064feeb5b4e9594857dc7305193a";

	public static final String FABER_ONNX_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx";
	public static final String FABER_JSON_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx.json";
	public static final String EDRESSON_ONNX_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/edresson/low/pt_BR-edresson-low.onnx";
	public static final String EDRESSON_JSON_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/edresson/low/pt_BR-edresson-low.onnx.json";
	public static final String LESSAC_ONNX_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/low/en_US-lessac-low.onnx";
	public static final String LESSAC_JSON_URL =
			"https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/low/en_US-lessac-low.onnx.json";

	private static final Map<String, String> SHA_BY_FILENAME = Map.of(
			FABER + ".onnx", FABER_ONNX_SHA256,
			FABER + ".onnx.json", FABER_JSON_SHA256,
			EDRESSON + ".onnx", EDRESSON_ONNX_SHA256,
			EDRESSON + ".onnx.json", EDRESSON_JSON_SHA256,
			LESSAC + ".onnx", LESSAC_ONNX_SHA256,
			LESSAC + ".onnx.json", LESSAC_JSON_SHA256
	);

	private static final Map<String, String> URL_BY_FILENAME = Map.of(
			FABER + ".onnx", FABER_ONNX_URL,
			FABER + ".onnx.json", FABER_JSON_URL,
			EDRESSON + ".onnx", EDRESSON_ONNX_URL,
			EDRESSON + ".onnx.json", EDRESSON_JSON_URL,
			LESSAC + ".onnx", LESSAC_ONNX_URL,
			LESSAC + ".onnx.json", LESSAC_JSON_URL
	);

	public static String bundledResourcePath(String fileName) {
		String lang = fileName != null && fileName.startsWith("en_US") ? "en_US" : "pt_BR";
		return "/assets/readmyitem/voices/" + lang + "/" + fileName;
	}

	public static String[] bundledFileNames() {
		return new String[] {
				LESSAC + ".onnx",
				LESSAC + ".onnx.json",
				EDRESSON + ".onnx",
				EDRESSON + ".onnx.json",
				FABER + ".onnx",
				FABER + ".onnx.json"
		};
	}

	public static String[] bundledStems() {
		return new String[] { LESSAC, FABER, EDRESSON };
	}

	private VoiceChecksums() {
	}

	public static boolean isAllowlistedUrl(String url) {
		if (url == null) {
			return false;
		}
		return URL_BY_FILENAME.containsValue(url);
	}

	public static String sha256ForFileName(String fileName) {
		return SHA_BY_FILENAME.get(fileName);
	}

	public static String urlForFileName(String fileName) {
		return URL_BY_FILENAME.get(fileName);
	}

	public static boolean isKnownStem(String stem) {
		return FABER.equals(stem) || EDRESSON.equals(stem) || LESSAC.equals(stem);
	}

	public static String canonicalizeStem(String stem) {
		return isKnownStem(stem) ? stem : FABER;
	}

	public static boolean isSafeFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return false;
		}
		if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0 || fileName.contains("..")) {
			return false;
		}
		return fileName.endsWith(".onnx") || fileName.endsWith(".onnx.json");
	}

	public static Path resolveInside(Path dir, String fileName) {
		if (dir == null || !isSafeFileName(fileName)) {
			return null;
		}
		Path root = dir.toAbsolutePath().normalize();
		Path resolved = root.resolve(fileName).normalize();
		if (!resolved.startsWith(root)) {
			return null;
		}
		return resolved;
	}

	public static boolean isAllowedDownloadUri(URI uri) {
		if (uri == null || uri.getHost() == null) {
			return false;
		}
		if (!"https".equalsIgnoreCase(uri.getScheme())) {
			return false;
		}
		String host = uri.getHost().toLowerCase(Locale.ROOT);
		return host.equals("huggingface.co")
				|| host.endsWith(".huggingface.co")
				|| host.equals("hf.co")
				|| host.endsWith(".hf.co");
	}

	public static boolean sha256Equals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return expected.toLowerCase(Locale.ROOT).equals(actual.toLowerCase(Locale.ROOT));
	}

	public static long maxBytesForFileName(String fileName) {
		return fileName != null && fileName.endsWith(".json") ? MAX_JSON_BYTES : MAX_ONNX_BYTES;
	}
}
