package dev.mml.readmyitem.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftTextSanitizerTest {
	@Test
	void stripsSectionCodes() {
		assertEquals("Espada Afiada", MinecraftTextSanitizer.sanitize("§cEspada §eAfiada"));
	}

	@Test
	void emptyString() {
		assertEquals("", MinecraftTextSanitizer.sanitize(""));
		assertEquals("", MinecraftTextSanitizer.sanitize(null));
	}

	@Test
	void loreWithNewlines() {
		assertEquals(2, MinecraftTextSanitizer.splitLines("uma linha\noutra linha").size());
		assertEquals("uma linha", MinecraftTextSanitizer.splitLines("uma linha\noutra linha").get(0));
	}

	@Test
	void truncatesLongText() {
		String longText = "a".repeat(1000);
		String cut = MinecraftTextSanitizer.truncate(longText, 280);
		assertTrue(cut.length() <= 280);
		assertTrue(cut.endsWith("…"));
	}

	@Test
	void technicalItemIds() {
		assertTrue(MinecraftTextSanitizer.isTechnicalLine("minecraft:stone"));
		assertFalse(MinecraftTextSanitizer.isTechnicalLine("Pedra"));
	}
}
