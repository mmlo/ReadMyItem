package dev.mml.readmyitem.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MinecraftTextSanitizer {
	private static final Pattern SECTION_CODES = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
	private static final Pattern HEX_COLOR = Pattern.compile("§x(§[0-9a-f]){6}", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEFTOVER_SECTION = Pattern.compile("§.");
	private static final Pattern ITEM_ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private MinecraftTextSanitizer() {
	}

	public static String sanitize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		String text = raw.replace('\u00A0', ' ');
		text = HEX_COLOR.matcher(text).replaceAll("");
		text = SECTION_CODES.matcher(text).replaceAll("");
		text = LEFTOVER_SECTION.matcher(text).replaceAll("");
		text = text.replace('\r', ' ').replace('\t', ' ');
		text = WHITESPACE.matcher(text).replaceAll(" ").trim();
		return text;
	}

	public static String truncate(String text, int maxChars) {
		if (text == null) {
			return "";
		}
		if (maxChars <= 0) {
			return "";
		}
		if (text.length() <= maxChars) {
			return text;
		}
		int cut = Math.max(0, maxChars - 1);
		int space = text.lastIndexOf(' ', cut);
		if (space >= maxChars / 2) {
			cut = space;
		}
		return text.substring(0, cut).trim() + "…";
	}

	public static boolean isTechnicalLine(String line) {
		if (line == null || line.isBlank()) {
			return true;
		}
		String trimmed = line.trim();
		if (ITEM_ID.matcher(trimmed).matches()) {
			return true;
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		return lower.startsWith("nbt") || lower.startsWith("{") || lower.startsWith("#");
	}

	public static List<String> splitLines(String text) {
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return out;
		}
		for (String part : text.split("\\R")) {
			String clean = sanitize(part);
			if (!clean.isEmpty()) {
				out.add(clean);
			}
		}
		return out;
	}
}
