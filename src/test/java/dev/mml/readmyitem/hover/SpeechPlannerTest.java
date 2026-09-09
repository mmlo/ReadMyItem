package dev.mml.readmyitem.hover;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeechPlannerTest {
	@Test
	void nameWithCount() {
		dev.mml.readmyitem.config.ModConfig.get().linguagem = dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR;
		SpokenItem item = new SpokenItem("pedras", 64, List.of(), "id");
		assertEquals("64 pedras", SpeechPlanner.nameUtterance(item, true, 280));
		assertEquals("pedras", SpeechPlanner.nameUtterance(item, false, 280));
	}

	@Test
	void detailsSkipDuplicateName() {
		dev.mml.readmyitem.config.ModConfig.get().linguagem = dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR;
		SpokenItem item = new SpokenItem("Espada de diamante", 1, List.of("Espada de diamante", "Afiação IV"), "id");
		String details = SpeechPlanner.detailUtterance(item, 280);
		assertTrue(details.toLowerCase().contains("afiação"));
		assertTrue(details.contains("4"));
	}

	@Test
	void emptySlot() {
		dev.mml.readmyitem.config.ModConfig.get().linguagem = dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR;
		assertEquals("espaço vazio", SpeechPlanner.emptySlotUtterance());
	}
}
