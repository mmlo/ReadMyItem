package dev.mml.readmyitem.tts;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceChecksumsTest {
	@Test
	void allowlistContainsOfficialUrlsOnly() {
		assertTrue(VoiceChecksums.isAllowlistedUrl(VoiceChecksums.FABER_ONNX_URL));
		assertTrue(VoiceChecksums.isAllowlistedUrl(VoiceChecksums.EDRESSON_JSON_URL));
		assertFalse(VoiceChecksums.isAllowlistedUrl("https://example.com/voice.onnx"));
		assertFalse(VoiceChecksums.isAllowlistedUrl("http://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx"));
	}

	@Test
	void sha256Lookup() {
		assertEquals(VoiceChecksums.FABER_ONNX_SHA256, VoiceChecksums.sha256ForFileName("pt_BR-faber-medium.onnx"));
		assertTrue(VoiceChecksums.sha256Equals(VoiceChecksums.FABER_JSON_SHA256, VoiceChecksums.FABER_JSON_SHA256.toUpperCase()));
	}

	@Test
	void knownStems() {
		assertTrue(VoiceChecksums.isKnownStem("pt_BR-faber-medium"));
		assertFalse(VoiceChecksums.isKnownStem("en_US-lessac-medium"));
		assertFalse(VoiceChecksums.isKnownStem("../pt_BR-faber-medium"));
		assertEquals(VoiceChecksums.FABER, VoiceChecksums.canonicalizeStem("../../evil"));
		assertEquals(VoiceChecksums.EDRESSON, VoiceChecksums.canonicalizeStem("pt_BR-edresson-low"));
	}

	@Test
	void resolveInsideRejectsTraversal() throws Exception {
		Path dir = Files.createTempDirectory("readmyitem-voices");
		assertNull(VoiceChecksums.resolveInside(dir, "../x.onnx"));
		assertNull(VoiceChecksums.resolveInside(dir, "pt_BR-faber-medium.onnx/../../x"));
		Path ok = VoiceChecksums.resolveInside(dir, "pt_BR-faber-medium.onnx");
		assertNotNull(ok);
		assertTrue(ok.normalize().startsWith(dir.toAbsolutePath().normalize()));
		Path okEvil = VoiceChecksums.resolveInside(dir, "evil.onnx");
		assertNotNull(okEvil);
	}

	@Test
	void downloadHostAllowlist() {
		assertTrue(VoiceChecksums.isAllowedDownloadUri(URI.create(VoiceChecksums.FABER_ONNX_URL)));
		assertTrue(VoiceChecksums.isAllowedDownloadUri(URI.create("https://cdn-lfs.huggingface.co/file.onnx")));
		assertTrue(VoiceChecksums.isAllowedDownloadUri(URI.create("https://cas-bridge.xethub.hf.co/file")));
		assertFalse(VoiceChecksums.isAllowedDownloadUri(URI.create("https://example.com/file.onnx")));
		assertFalse(VoiceChecksums.isAllowedDownloadUri(URI.create("http://huggingface.co/file.onnx")));
	}

	@Test
	void bundledResourcesCoverBothOfficialPairs() {
		String[] files = VoiceChecksums.bundledFileNames();
		assertEquals(6, files.length);
		assertEquals("/assets/readmyitem/voices/pt_BR/pt_BR-edresson-low.onnx",
				VoiceChecksums.bundledResourcePath("pt_BR-edresson-low.onnx"));
		assertEquals("/assets/readmyitem/voices/en_US/en_US-lessac-low.onnx",
				VoiceChecksums.bundledResourcePath("en_US-lessac-low.onnx"));
	}
}
