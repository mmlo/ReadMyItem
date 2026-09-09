package dev.mml.readmyitem.audio;

import dev.mml.readmyitem.ReadMyItemMod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process Java Sound fallback used only when Minecraft's SoundEngine is not up yet.
 * Same JVM, no subprocess, no extra device OpenAL.
 */
public final class JavaxSoundFallback {
	private final Object lock = new Object();
	private final AtomicInteger epoch = new AtomicInteger();
	private volatile Clip clip;

	public void play(short[] samples, int sampleRate, float volume) {
		int token = epoch.incrementAndGet();
		Clip previous;
		synchronized (lock) {
			previous = this.clip;
			this.clip = null;
		}
		closeQuietly(previous);
		if (samples == null || samples.length == 0) {
			return;
		}
		Clip opened = null;
		try {
			AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			opened = (Clip) AudioSystem.getLine(info);
			byte[] bytes = new byte[samples.length * 2];
			ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples);
			opened.open(format, bytes, 0, bytes.length);
			if (opened.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				FloatControl gain = (FloatControl) opened.getControl(FloatControl.Type.MASTER_GAIN);
				float clamped = Math.max(0.0001f, Math.min(1f, volume));
				float db = (float) (20.0 * Math.log10(clamped));
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
			}
			synchronized (lock) {
				if (epoch.get() != token) {
					closeQuietly(opened);
					return;
				}
				this.clip = opened;
				opened.start();
			}
		} catch (Throwable t) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] fallback javax.sound falhou: {}", t.toString());
			closeQuietly(opened);
		}
	}

	public void stop() {
		epoch.incrementAndGet();
		Clip toClose;
		synchronized (lock) {
			toClose = this.clip;
			this.clip = null;
		}
		closeQuietly(toClose);
	}

	private static void closeQuietly(Clip toClose) {
		if (toClose == null) {
			return;
		}
		try {
			toClose.stop();
			toClose.flush();
			toClose.close();
		} catch (Throwable ignored) {
			// already closed
		}
	}
}
