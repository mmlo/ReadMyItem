package dev.mml.readmyitem.text;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PtBrNormalizer {
	/**
	 * Unicode-aware boundaries: Java {@code \\b} treats {@code ã} as a non-word
	 * character, so {@code Lã} would become {@code 50ã} ("cinquenta ã").
	 */
	private static final Pattern ROMAN = Pattern.compile(
			"(?<![\\p{L}\\p{N}_])(I{1,3}|IV|VI{0,3}|IX|X{1,3}|XL|L|XC|C)(?![\\p{L}\\p{N}_])",
			Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern DURABILITY_EN = Pattern.compile(
			"(?i)durability:\\s*(\\d+)\\s*/\\s*(\\d+)");
	private static final Pattern DURABILITY_PT = Pattern.compile(
			"(?i)durabilidade:\\s*(\\d+)\\s*/\\s*(\\d+)");

	private PtBrNormalizer() {
	}

	public static String normalizeUtterance(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		String out = text;
		out = DURABILITY_EN.matcher(out).replaceAll("durabilidade $1 de $2");
		out = DURABILITY_PT.matcher(out).replaceAll("durabilidade $1 de $2");
		out = replaceRomanNumerals(out);
		out = out.replace(" / ", " de ");
		return MinecraftTextSanitizer.sanitize(out);
	}

	public static String joinDetails(Iterable<String> lines, int maxChars) {
		StringBuilder builder = new StringBuilder();
		for (String line : lines) {
			String piece = normalizeUtterance(line);
			if (piece.isEmpty()) {
				continue;
			}
			if (!piece.endsWith(".") && !piece.endsWith("!") && !piece.endsWith("?")) {
				piece = piece + ".";
			}
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(piece);
		}
		return MinecraftTextSanitizer.truncate(builder.toString(), maxChars);
	}

	static String replaceRomanNumerals(String text) {
		Matcher matcher = ROMAN.matcher(text);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			int value = romanToInt(matcher.group());
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(Integer.toString(value)));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	private static int romanToInt(String roman) {
		return switch (roman.toUpperCase(Locale.ROOT)) {
			case "I" -> 1;
			case "II" -> 2;
			case "III" -> 3;
			case "IV" -> 4;
			case "V" -> 5;
			case "VI" -> 6;
			case "VII" -> 7;
			case "VIII" -> 8;
			case "IX" -> 9;
			case "X" -> 10;
			case "XI" -> 11;
			case "XII" -> 12;
			case "XIII" -> 13;
			case "XIV" -> 14;
			case "XV" -> 15;
			case "XX" -> 20;
			case "XXX" -> 30;
			case "XL" -> 40;
			case "L" -> 50;
			case "C" -> 100;
			default -> {
				int total = 0;
				int prev = 0;
				for (int i = roman.length() - 1; i >= 0; i--) {
					int cur = switch (roman.charAt(i)) {
						case 'I', 'i' -> 1;
						case 'V', 'v' -> 5;
						case 'X', 'x' -> 10;
						case 'L', 'l' -> 50;
						case 'C', 'c' -> 100;
						default -> 0;
					};
					if (cur < prev) {
						total -= cur;
					} else {
						total += cur;
						prev = cur;
					}
				}
				yield total;
			}
		};
	}
}
