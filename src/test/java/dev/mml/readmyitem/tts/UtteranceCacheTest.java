package dev.mml.readmyitem.tts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtteranceCacheTest {
	@Test
	void storesKeysLongerThanEightyChars() {
		UtteranceCache cache = new UtteranceCache(8, 80);
		String key = UtteranceCache.key("pt_BR-faber-medium", 0.92f, "Espada de diamante encantada com Afiação 4 e durabilidade 1400 de 1561");
		assertTrue(key.length() > 80, () -> "key len=" + key.length());
		cache.put(key, new UtteranceCache.Entry(new short[] {1, 2, 3}, 22050));
		assertNotNull(cache.get(key));
		assertEquals(3, cache.get(key).pcm().length);
	}

	@Test
	void diskFileNameUsesPtBrPrefix() {
		String name = UtteranceDiskCache.fileName("pt_BR-faber-medium", 0.92f, "Pedra");
		assertTrue(name.startsWith("pt-br_"));
		assertTrue(name.endsWith(".pcm"));
		assertEquals(name, UtteranceDiskCache.fileName("pt_BR-faber-medium", 0.92f, "Pedra"));
	}

	@Test
	void diskRoundTripPreservesPcm() throws Exception {
		Path dir = Files.createTempDirectory("readmyitem-cache");
		short[] pcm = new short[4096];
		for (int i = 0; i < pcm.length; i++) {
			pcm[i] = (short) (i - 2000);
		}
		UtteranceDiskCache.saveTo(dir, "pt_BR-faber-medium", 0.92f, "Pedra", pcm, 22050);
		Path file = dir.resolve(UtteranceDiskCache.fileName("pt_BR-faber-medium", 0.92f, "Pedra"));
		assertTrue(Files.isRegularFile(file));
		UtteranceCache.Entry loaded = UtteranceDiskCache.loadFrom(dir, "pt_BR-faber-medium", 0.92f, "Pedra");
		assertNotNull(loaded);
		assertEquals(22050, loaded.sampleRate());
		assertEquals(pcm.length, loaded.pcm().length);
		assertEquals(pcm[0], loaded.pcm()[0]);
		assertEquals(pcm[2048], loaded.pcm()[2048]);
		assertEquals(pcm[4095], loaded.pcm()[4095]);
	}

	@Test
	void diskLoadMissesWhenFileIsAbsent() throws Exception {
		Path dir = Files.createTempDirectory("readmyitem-cache-empty");
		assertEquals(null, UtteranceDiskCache.loadFrom(dir, "pt_BR-faber-medium", 0.92f, "Nunca ouvido"));
	}
}
