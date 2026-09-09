package dev.mml.readmyitem.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PtBrNormalizerTest {
	@Test
	void romanNumeralsBecomeDigits() {
		assertEquals("Afiação 4", PtBrNormalizer.normalizeUtterance("Afiação IV"));
		assertEquals("Proteção 5", PtBrNormalizer.normalizeUtterance("Proteção V"));
	}

	@Test
	void woolLaIsNotRomanFifty() {
		assertEquals("Lã cinza", PtBrNormalizer.normalizeUtterance("Lã cinza"));
		assertEquals("Lã", PtBrNormalizer.normalizeUtterance("Lã"));
		assertEquals("Bloco de lã", PtBrNormalizer.normalizeUtterance("Bloco de lã"));
	}

	@Test
	void durabilityPhrase() {
		assertEquals("durabilidade 500 de 1561", PtBrNormalizer.normalizeUtterance("Durability: 500 / 1561"));
	}

	@Test
	void joinDetailsRespectsLimit() {
		String joined = PtBrNormalizer.joinDetails(List.of("a".repeat(200), "b".repeat(200)), 80);
		assertTrue(joined.length() <= 80);
	}
}
