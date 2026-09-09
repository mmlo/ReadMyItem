package dev.mml.readmyitem.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Autonomous timings. Warns in the log and writes a Portuguese report
 * so a freeze does not require reading a raw profiler dump.
 */
public final class PerfProbe {
	public enum Kind {
		HOVER_TICK(5_000_000L, 8_000_000L, "tick do hover (thread do jogo)"),
		TOOLTIP(5_000_000L, 8_000_000L, "leitura do tooltip"),
		SYNTH(800_000_000L, 1_500_000_000L, "síntese Piper (texto→áudio)"),
		MODEL_LOAD(1_000_000_000L, 2_000_000_000L, "carregar modelo ONNX"),
		CACHE_DISK_READ(20_000_000L, 50_000_000L, "ler cache de fala no disco"),
		CACHE_DISK_WRITE(40_000_000L, 80_000_000L, "gravar cache de fala no disco"),
		PLAY(20_000_000L, 40_000_000L, "enviar PCM ao som");

		final long warnNanos;
		final long problemNanos;
		final String label;

		Kind(long warnNanos, long problemNanos, String label) {
			this.warnNanos = warnNanos;
			this.problemNanos = problemNanos;
			this.label = label;
		}
	}

	private static final Map<Kind, Stats> STATS = new EnumMap<>(Kind.class);
	private static final AtomicLong SPEAK = new AtomicLong();
	private static final AtomicLong CACHE_MEM = new AtomicLong();
	private static final AtomicLong CACHE_DISK = new AtomicLong();
	private static final AtomicLong CACHE_MISS = new AtomicLong();
	private static final AtomicLong LAST_DUMP_MS = new AtomicLong();

	static {
		for (Kind kind : Kind.values()) {
			STATS.put(kind, new Stats());
		}
	}

	private PerfProbe() {
	}

	public static void countSpeak() {
		SPEAK.incrementAndGet();
	}

	public static void countCacheMem() {
		CACHE_MEM.incrementAndGet();
	}

	public static void countCacheDisk() {
		CACHE_DISK.incrementAndGet();
	}

	public static void countCacheMiss() {
		CACHE_MISS.incrementAndGet();
	}

	public static String record(Kind kind, long nanos) {
		STATS.get(kind).add(nanos);
		if (nanos >= kind.problemNanos) {
			return "PROBLEMA: " + kind.label + " levou " + formatMs(nanos)
					+ " (limite " + formatMs(kind.problemNanos) + ")";
		}
		if (nanos >= kind.warnNanos) {
			return "AVISO: " + kind.label + " levou " + formatMs(nanos);
		}
		return null;
	}

	public static String report() {
		StringBuilder out = new StringBuilder();
		out.append("ReadMyItem — relatório automático de performance\n");
		out.append("Não precisa de profiler externo. Gerado pelo próprio mod.\n\n");
		out.append("Falas pedidas: ").append(SPEAK.get())
				.append(" | cache memória: ").append(CACHE_MEM.get())
				.append(" | cache disco: ").append(CACHE_DISK.get())
				.append(" | síntese nova: ").append(CACHE_MISS.get())
				.append('\n');
		out.append('\n');
		for (Kind kind : Kind.values()) {
			Stats stats = STATS.get(kind);
			out.append(String.format(Locale.ROOT,
					"%-34s n=%-5d  média=%s  máx=%s%s\n",
					kind.label,
					stats.count,
					formatMs(stats.count == 0 ? 0 : stats.sum / stats.count),
					formatMs(stats.max),
					stats.max >= kind.problemNanos ? "  << LENTO" : ""));
		}
		out.append('\n');
		out.append("Diagnóstico automático:\n");
		for (String line : diagnose()) {
			out.append("- ").append(line).append('\n');
		}
		return out.toString();
	}

	public static List<String> diagnose() {
		List<String> lines = new ArrayList<>();
		Stats hover = STATS.get(Kind.HOVER_TICK);
		Stats tooltip = STATS.get(Kind.TOOLTIP);
		Stats synth = STATS.get(Kind.SYNTH);
		Stats load = STATS.get(Kind.MODEL_LOAD);
		long miss = CACHE_MISS.get();
		long hits = CACHE_MEM.get() + CACHE_DISK.get();

		if (hover.max >= Kind.HOVER_TICK.problemNanos) {
			Stats write = STATS.get(Kind.CACHE_DISK_WRITE);
			if (tooltip.max >= Kind.TOOLTIP.problemNanos) {
				lines.add("O tick do inventário travou a thread do jogo (" + formatMs(hover.max)
						+ "). Montar o texto do item também estava lento.");
			} else if (write.max >= Kind.CACHE_DISK_WRITE.problemNanos) {
				lines.add("O tick do inventário travou a thread do jogo (" + formatMs(hover.max)
						+ ") no mesmo período em que o cache de fala gravava no disco ("
						+ formatMs(write.max) + "). A escrita do PCM estava disputando o notebook com o jogo.");
			} else {
				lines.add("O tick do inventário travou a thread do jogo (" + formatMs(hover.max)
						+ "). Tooltip estava rápido; a causa é contenção (som, disco ou GC) no mesmo tick.");
			}
		}
		if (tooltip.max >= Kind.TOOLTIP.problemNanos) {
			lines.add("Montar o texto do item está lento (" + formatMs(tooltip.max)
					+ "). Itens com muita descrição/encantamento pesam no scroll.");
		}
		if (load.max >= Kind.MODEL_LOAD.problemNanos) {
			lines.add("Carregar o modelo Piper/ONNX levou " + formatMs(load.max)
					+ ". Isso disputa CPU com o mundo e congela o notebook na primeira fala.");
		}
		if (synth.max >= Kind.SYNTH.problemNanos) {
			lines.add("A síntese (texto→áudio) chegou a " + formatMs(synth.max)
					+ ". Itens novos na primeira escuta ainda passam pelo Piper.");
		}
		Stats play = STATS.get(Kind.PLAY);
		if (play.max >= Kind.PLAY.problemNanos && hover.max < Kind.HOVER_TICK.problemNanos) {
			lines.add("A voz demorou para entrar no mixer do jogo (" + formatMs(play.max)
					+ "). O inventário não travou; o OpenAL do Minecraft estava ocupado.");
		}
		if (miss > 0 && hits == 0 && synth.count > 0) {
			lines.add("Nenhum hit de cache ainda: toda fala está sintetizando. Depois da primeira vez o arquivo pt-br_*.pcm deve evitar o Piper.");
		}
		if (CACHE_DISK.get() > 0 && synth.max < Kind.SYNTH.warnNanos) {
			lines.add("Cache em disco está sendo usado e a síntese recente não está no vermelho.");
		}
		if (lines.isEmpty()) {
			if (SPEAK.get() == 0) {
				lines.add("Ainda não houve fala nesta sessão. Passe o mouse em itens no inventário para gerar dados.");
			} else {
				lines.add("Nenhum trecho acima do limite de congelamento. Se ainda houver engasgo, é carga do próprio Minecraft (chunks/visão), não do hover.");
			}
		}
		return lines;
	}

	private static volatile Path reportFile;

	public static void setReportFile(Path file) {
		reportFile = file;
	}

	public static void dumpIfDue() {
		Path file = reportFile;
		if (file == null) {
			return;
		}
		long now = System.currentTimeMillis();
		long last = LAST_DUMP_MS.get();
		if (now - last < 60_000L) {
			return;
		}
		if (!LAST_DUMP_MS.compareAndSet(last, now)) {
			return;
		}
		dump(file);
	}

	public static void dump() {
		Path file = reportFile;
		if (file != null) {
			dump(file);
		}
	}

	public static void dump(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, report(), StandardCharsets.UTF_8);
		} catch (IOException ignored) {
			// never crash the game because of a perf dump
		}
	}

	public static void resetForTests() {
		for (Stats stats : STATS.values()) {
			stats.clear();
		}
		SPEAK.set(0);
		CACHE_MEM.set(0);
		CACHE_DISK.set(0);
		CACHE_MISS.set(0);
		LAST_DUMP_MS.set(0);
	}

	static String formatMs(long nanos) {
		return String.format(Locale.ROOT, "%.1f ms", nanos / 1_000_000.0);
	}

	private static final class Stats {
		private long count;
		private long sum;
		private long max;

		synchronized void add(long nanos) {
			count++;
			sum += nanos;
			if (nanos > max) {
				max = nanos;
			}
		}

		synchronized void clear() {
			count = 0;
			sum = 0;
			max = 0;
		}
	}
}
