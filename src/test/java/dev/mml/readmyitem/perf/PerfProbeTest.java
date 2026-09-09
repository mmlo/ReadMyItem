package dev.mml.readmyitem.perf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfProbeTest {
	@BeforeEach
	void reset() {
		PerfProbe.resetForTests();
	}

	@Test
	void flagsSlowSynthesisWithoutHumanReadingRawLogs() {
		PerfProbe.countSpeak();
		PerfProbe.countCacheMiss();
		String alert = PerfProbe.record(PerfProbe.Kind.SYNTH, 2_000_000_000L);
		assertTrue(alert != null && alert.startsWith("PROBLEMA"));
		List<String> diagnosis = PerfProbe.diagnose();
		assertTrue(diagnosis.stream().anyMatch(line -> line.toLowerCase().contains("síntese")));
	}

	@Test
	void flagsSlowHoverOnGameThread() {
		PerfProbe.record(PerfProbe.Kind.HOVER_TICK, 12_000_000L);
		List<String> diagnosis = PerfProbe.diagnose();
		assertTrue(diagnosis.stream().anyMatch(line -> line.contains("tick do inventário")));
	}

	@Test
	void linksTwoSecondHoverFreezeToSlowDiskCacheWrite() {
		PerfProbe.record(PerfProbe.Kind.HOVER_TICK, 2_008_000_000L);
		PerfProbe.record(PerfProbe.Kind.CACHE_DISK_WRITE, 450_000_000L);
		List<String> diagnosis = PerfProbe.diagnose();
		assertTrue(diagnosis.stream().anyMatch(line -> line.contains("cache de fala")));
	}

	@Test
	void flagsSlowPlaybackWithoutBlamingHover() {
		PerfProbe.record(PerfProbe.Kind.PLAY, 2_000_000_000L);
		List<String> diagnosis = PerfProbe.diagnose();
		assertTrue(diagnosis.stream().anyMatch(line -> line.contains("mixer do jogo")));
	}
}
