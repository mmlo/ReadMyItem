package dev.mml.readmyitem.hover;

import dev.mml.readmyitem.text.MinecraftTextSanitizer;
import dev.mml.readmyitem.text.PtBrNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpeechPlanner {
	private SpeechPlanner() {
	}

	public static String nameUtterance(SpokenItem item, boolean readCount, int maxChars) {
		if (item == null) {
			return "";
		}
		boolean isPtBr = dev.mml.readmyitem.config.ModConfig.get().linguagem == dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR;
		String name = isPtBr ? PtBrNormalizer.normalizeUtterance(item.displayName()) : MinecraftTextSanitizer.sanitize(item.displayName());
		if (name.isEmpty()) {
			return "";
		}
		if (readCount && item.count() > 1) {
			name = item.count() + " " + name;
		}
		return MinecraftTextSanitizer.truncate(name, maxChars);
	}

	public static String detailUtterance(SpokenItem item, int maxChars) {
		if (item == null || item.detailLines().isEmpty()) {
			return "";
		}
		List<String> cleaned = new ArrayList<>();
		String name = MinecraftTextSanitizer.sanitize(item.displayName()).toLowerCase(Locale.ROOT);
		for (String line : item.detailLines()) {
			String sanitized = MinecraftTextSanitizer.sanitize(line);
			if (sanitized.isEmpty()) {
				continue;
			}
			if (sanitized.equalsIgnoreCase(item.displayName()) || sanitized.equalsIgnoreCase(name)) {
				continue;
			}
			cleaned.add(sanitized);
		}
		boolean isPtBr = dev.mml.readmyitem.config.ModConfig.get().linguagem == dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR;
		if (isPtBr) {
			return PtBrNormalizer.joinDetails(cleaned, maxChars);
		}
		String joined = String.join(". ", cleaned) + ".";
		return MinecraftTextSanitizer.truncate(joined, maxChars);
	}

	public static String emptySlotUtterance() {
		return dev.mml.readmyitem.config.ModConfig.get().linguagem == dev.mml.readmyitem.config.ModConfig.AppLanguage.PT_BR ? "espaço vazio" : "empty slot";
	}
}
