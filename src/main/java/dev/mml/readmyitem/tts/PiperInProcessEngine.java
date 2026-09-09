package dev.mml.readmyitem.tts;

import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.audio.NarrationPlayer;
import dev.mml.readmyitem.perf.PerfProbe;
import io.github.jvoiceproject.piperjni.PiperJNI;
import io.github.jvoiceproject.piperjni.PiperVoice;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class PiperInProcessEngine implements TtsEngine {
	private static final long SYNTHESIS_WARN_NS = 2_500_000_000L;

	private final SpeechQueue<Job> queue = new SpeechQueue<>();
	private final UtteranceCache cache = new UtteranceCache(64, 280);
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean stopRequested = new AtomicBoolean();
	private final AtomicInteger generation = new AtomicInteger();
	private final Object loadLock = new Object();

	private Thread worker;
	private volatile boolean nativeFailed;
	private volatile boolean ready;
	private PiperJNI piper;
	private PiperVoice voice;
	private String loadedStem = "";
	private int sampleRate = 22050;

	@Override
	public void init() {
		startWorker();
	}

	@Override
	public boolean isReady() {
		if (nativeFailed) {
			return false;
		}
		if (ready) {
			return true;
		}
		return VoiceLibrary.isReady();
	}

	@Override
	public void speak(String text, float speed, float volume) {
		if (text == null || text.isBlank()) {
			return;
		}
		startWorker();
		stopRequested.set(true);
		generation.incrementAndGet();
		queue.offer(new Job(text, clampSpeed(speed), clampVolume(volume), generation.get()));
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public void stop() {
		stopRequested.set(true);
		queue.clear();
		generation.incrementAndGet();
		NarrationPlayer.get().stop();
	}

	@Override
	public void shutdown() {
		running.set(false);
		stop();
		synchronized (this) {
			notifyAll();
		}
		Thread current = worker;
		if (current != null) {
			current.interrupt();
		}
		closeVoice();
		cache.clear();
	}

	public void reloadVoice() {
		VoiceLibrary.invalidateCache();
		ready = false;
		loadedStem = "";
		closeVoice();
	}

	public void preloadVoice() {
		startWorker();
		ensureLoaded();
	}

	private void startWorker() {
		if (running.get()) {
			return;
		}
		if (!running.compareAndSet(false, true)) {
			return;
		}
		worker = new Thread(this::loop, "readmyitem-tts");
		worker.setDaemon(true);
		worker.start();
	}

	private void loop() {
		while (running.get()) {
			Job job = queue.take();
			if (job == null) {
				synchronized (this) {
					if (queue.isEmpty() && running.get()) {
						try {
							wait(250);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				}
				continue;
			}
			if (job.generation != generation.get()) {
				continue;
			}
			stopRequested.set(false);
			synthesizeAndPlay(job);
		}
	}

	private void synthesizeAndPlay(Job job) {
		if (stopRequested.get() || job.generation != generation.get()) {
			return;
		}
		String stem = loadedStem;
		if (stem.isEmpty()) {
			stem = VoiceLibrary.resolveConfiguredVoice().map(VoiceLibrary.VoiceFiles::stem).orElse("");
		}
		if (stem.isEmpty()) {
			return;
		}
		String cacheKey = UtteranceCache.key(stem, job.speed, job.text);
		UtteranceCache.Entry cached = cache.get(cacheKey);
		if (cached != null) {
			PerfProbe.countCacheMem();
		} else {
			long readStarted = System.nanoTime();
			cached = UtteranceDiskCache.load(stem, job.speed, job.text);
			String readAlert = PerfProbe.record(PerfProbe.Kind.CACHE_DISK_READ, System.nanoTime() - readStarted);
			if (readAlert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", readAlert);
			}
			if (cached != null) {
				cache.put(cacheKey, cached);
				PerfProbe.countCacheDisk();
			}
		}
		short[] pcm;
		int rate;
		PerfProbe.countSpeak();
		if (cached != null) {
			pcm = cached.pcm();
			rate = cached.sampleRate();
		} else {
			PerfProbe.countCacheMiss();
			if (!ensureLoaded()) {
				return;
			}
			if (stopRequested.get() || job.generation != generation.get()) {
				return;
			}
			long start = System.nanoTime();
			try {
				pcm = piper.textToAudio(voice, job.text);
			} catch (Exception e) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] síntese falhou: {}", e.toString());
				return;
			}
			long elapsed = System.nanoTime() - start;
			String synthAlert = PerfProbe.record(PerfProbe.Kind.SYNTH, elapsed);
			if (synthAlert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", synthAlert);
			} else if (elapsed > SYNTHESIS_WARN_NS) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem] síntese lenta: {} ms", elapsed / 1_000_000L);
			}
			rate = sampleRate;
			pcm = applySpeed(pcm, job.speed);
			cache.put(cacheKey, new UtteranceCache.Entry(pcm, rate));
		}
		if (stopRequested.get() || job.generation != generation.get() || pcm == null || pcm.length == 0) {
			return;
		}
		NarrationPlayer.get().play(pcm, rate, job.volume);
		if (cached == null) {
			long writeStarted = System.nanoTime();
			UtteranceDiskCache.save(loadedStem, job.speed, job.text, pcm, rate);
			String writeAlert = PerfProbe.record(PerfProbe.Kind.CACHE_DISK_WRITE, System.nanoTime() - writeStarted);
			if (writeAlert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", writeAlert);
			}
		}
		PerfProbe.dumpIfDue();
	}

	private boolean ensureLoaded() {
		if (nativeFailed) {
			return false;
		}
		Optional<VoiceLibrary.VoiceFiles> files = VoiceLibrary.resolveConfiguredVoice();
		if (files.isEmpty()) {
			ready = false;
			return false;
		}
		VoiceLibrary.VoiceFiles chosen = files.get();
		if (ready && chosen.stem().equals(loadedStem) && voice != null) {
			return true;
		}
		synchronized (loadLock) {
			try {
				if (piper == null) {
					piper = new PiperJNI();
					piper.initialize(true);
				}
				closeVoiceOnly();
				long loadStarted = System.nanoTime();
				voice = piper.loadVoice(chosen.onnx(), chosen.json());
				sampleRate = voice.getSampleRate();
				loadedStem = chosen.stem();
				ready = true;
				String loadAlert = PerfProbe.record(PerfProbe.Kind.MODEL_LOAD, System.nanoTime() - loadStarted);
				ReadMyItemMod.LOGGER.info("[ReadMyItem] voz Piper carregada: {} @ {} Hz", loadedStem, sampleRate);
				if (loadAlert != null) {
					ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", loadAlert);
				}
				return true;
			} catch (UnsatisfiedLinkError | Exception e) {
				nativeFailed = true;
				ready = false;
				ReadMyItemMod.LOGGER.error("[ReadMyItem] nativo Piper não carregou. TTS desligado. {}", e.toString());
				return false;
			}
		}
	}

	private void closeVoice() {
		synchronized (loadLock) {
			closeVoiceOnly();
			if (piper != null) {
				try {
					piper.close();
				} catch (Exception ignored) {
					// shutdown
				}
				piper = null;
			}
			ready = false;
		}
	}

	private void closeVoiceOnly() {
		if (voice != null) {
			try {
				voice.close();
			} catch (Exception ignored) {
				// reload
			}
			voice = null;
		}
	}

	static short[] applySpeed(short[] pcm, float speed) {
		if (pcm == null || pcm.length == 0) {
			return pcm;
		}
		float clamped = clampSpeed(speed);
		if (Math.abs(clamped - 1.0f) < 0.02f) {
			return pcm;
		}
		double ratio = 1.0 / clamped;
		int outLen = Math.max(1, (int) Math.round(pcm.length * ratio));
		short[] out = new short[outLen];
		for (int i = 0; i < outLen; i++) {
			double src = i / ratio;
			int i0 = (int) Math.floor(src);
			int i1 = Math.min(pcm.length - 1, i0 + 1);
			i0 = Math.max(0, Math.min(pcm.length - 1, i0));
			double t = src - i0;
			out[i] = (short) Math.round(pcm[i0] * (1.0 - t) + pcm[i1] * t);
		}
		return out;
	}

	private static float clampSpeed(float speed) {
		if (Float.isNaN(speed)) {
			return 1.0f;
		}
		return Math.max(0.75f, Math.min(1.35f, speed));
	}

	private static float clampVolume(float volume) {
		if (Float.isNaN(volume)) {
			return 1.0f;
		}
		return Math.max(0f, Math.min(1f, volume));
	}

	private record Job(String text, float speed, float volume, int generation) {
	}
}
