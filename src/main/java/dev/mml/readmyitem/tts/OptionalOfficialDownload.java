package dev.mml.readmyitem.tts;

import dev.mml.readmyitem.ReadMyItemMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Optional restore from the official allowlist. Not used on boot. */
public final class OptionalOfficialDownload {
	public enum Status {
		IDLE, RUNNING, DONE, FAILED, CANCELLED, CHECKSUM_MISMATCH
	}

	public record Result(Status status, String message) {
	}

	private final AtomicBoolean cancel = new AtomicBoolean();
	private volatile Status status = Status.IDLE;
	private volatile int percent;
	private volatile String message = "";

	public Status status() {
		return status;
	}

	public int percent() {
		return percent;
	}

	public String message() {
		return message;
	}

	public void cancel() {
		cancel.set(true);
	}

	public Result downloadStem(String stem, BiConsumer<Integer, String> progress) {
		cancel.set(false);
		status = Status.RUNNING;
		percent = 0;
		if (!VoiceChecksums.isKnownStem(stem)) {
			status = Status.FAILED;
			message = "Voz não está na lista oficial.";
			return new Result(status, message);
		}
		VoiceLibrary.ensureVoiceDirectory();
		Path dir = VoiceLibrary.voiceDirectory();
		try {
			downloadOne(stem + ".onnx.json", dir, progress);
			if (cancel.get()) {
				return cancelled();
			}
			downloadOne(stem + ".onnx", dir, progress);
			if (cancel.get()) {
				return cancelled();
			}
			status = Status.DONE;
			message = "Pronto! A voz " + stem + " está instalada.";
			percent = 100;
			progress.accept(100, message);
			return new Result(status, message);
		} catch (CancelledException e) {
			return cancelled();
		} catch (ChecksumException e) {
			status = Status.CHECKSUM_MISMATCH;
			message = "O arquivo baixado não é o oficial. Foi apagado por segurança.";
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] checksum inválido: {}", e.getMessage());
			return new Result(status, message);
		} catch (Exception e) {
			status = Status.FAILED;
			message = "Não deu para baixar. Confira a internet e tente de novo.";
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] download falhou: {}", e.toString());
			return new Result(status, message);
		}
	}

	private Result cancelled() {
		status = Status.CANCELLED;
		message = "Download cancelado.";
		return new Result(status, message);
	}

	private void downloadOne(String fileName, Path dir, BiConsumer<Integer, String> progress) throws Exception {
		String url = VoiceChecksums.urlForFileName(fileName);
		String expected = VoiceChecksums.sha256ForFileName(fileName);
		if (url == null || expected == null || !url.startsWith("https://")) {
			throw new IOException("URL não allowlisted: " + fileName);
		}
		long max = VoiceChecksums.maxBytesForFileName(fileName);
		Path target = VoiceChecksums.resolveInside(dir, fileName);
		if (target == null) {
			throw new IOException("URL não allowlisted: " + fileName);
		}
		Path tmp = target.resolveSibling(target.getFileName().toString() + ".part");
		Files.createDirectories(target.getParent());
		HttpClient client = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER)
				.connectTimeout(Duration.ofSeconds(20))
				.build();
		HttpResponse<InputStream> response = getAllowlisted(client, url);
		if (response.statusCode() != 200) {
			closeQuietly(response.body());
			throw new IOException("HTTP " + response.statusCode());
		}
		long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
		if (contentLength > max) {
			closeQuietly(response.body());
			throw new IOException("arquivo maior que o limite");
		}
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		long read = 0;
		try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(tmp)) {
			byte[] buf = new byte[32 * 1024];
			int n;
			while ((n = in.read(buf)) >= 0) {
				if (cancel.get()) {
					throw new CancelledException();
				}
				if (n == 0) {
					continue;
				}
				read += n;
				if (read > max) {
					throw new IOException("arquivo maior que o limite");
				}
				digest.update(buf, 0, n);
				out.write(buf, 0, n);
				int pct = contentLength > 0 ? (int) Math.min(99, (read * 100) / contentLength) : 50;
				percent = pct;
				progress.accept(pct, "Baixando " + fileName);
			}
		} catch (Exception e) {
			Files.deleteIfExists(tmp);
			throw e;
		}
		String actual = HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
		if (!VoiceChecksums.sha256Equals(expected, actual)) {
			Files.deleteIfExists(tmp);
			throw new ChecksumException(fileName);
		}
		try {
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException ignored) {
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static HttpResponse<InputStream> getAllowlisted(HttpClient client, String startUrl) throws Exception {
		URI current = URI.create(startUrl);
		if (!VoiceChecksums.isAllowlistedUrl(startUrl)) {
			throw new IOException("URL não allowlisted: " + startUrl);
		}
		for (int hop = 0; hop < 6; hop++) {
			if (!VoiceChecksums.isAllowedDownloadUri(current)) {
				throw new IOException("redirect não allowlisted");
			}
			HttpRequest request = HttpRequest.newBuilder(current)
					.timeout(Duration.ofMinutes(5))
					.header("User-Agent", "ReadMyItem/1.0 (Minecraft accessibility mod)")
					.GET()
					.build();
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			int code = response.statusCode();
			if (code >= 300 && code < 400) {
				String location = response.headers().firstValue("Location").orElse("");
				closeQuietly(response.body());
				if (location.isBlank()) {
					throw new IOException("redirect sem Location");
				}
				current = current.resolve(location);
				continue;
			}
			return response;
		}
		throw new IOException("muitos redirects");
	}

	private static void closeQuietly(InputStream body) {
		if (body == null) {
			return;
		}
		try {
			body.close();
		} catch (IOException ignored) {
			// hop
		}
	}

	private static final class CancelledException extends IOException {
	}

	private static final class ChecksumException extends IOException {
		ChecksumException(String file) {
			super(file);
		}
	}
}
