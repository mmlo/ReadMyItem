package dev.mml.readmyitem.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/**
 * Speaks through the game's SoundEngine. The javax Clip path is in-process
 * and used only before the engine exists — never a subprocess or a second OpenAL device.
 */
public final class NarrationPlayer {
	private static final NarrationPlayer INSTANCE = new NarrationPlayer();

	private final JavaxSoundFallback fallback = new JavaxSoundFallback();
	private volatile float cachedGain = 1f;
	private volatile boolean usingFallback;

	private NarrationPlayer() {
	}

	public static NarrationPlayer get() {
		return INSTANCE;
	}

	public void sampleGain(Minecraft client) {
		cachedGain = readGameGain(client);
	}

	public void play(short[] pcm, int sampleRate, float volume) {
		float gain = cachedGain * Math.max(0f, Math.min(1f, volume));
		PcmSoundInstance instance = new PcmSoundInstance(pcm, sampleRate);
		if (MinecraftSoundBridge.play(instance, gain)) {
			usingFallback = false;
			return;
		}
		if (MinecraftSoundBridge.available()) {
			return;
		}
		usingFallback = true;
		fallback.play(pcm, sampleRate, gain);
	}

	public void stop() {
		MinecraftSoundBridge.stop();
		if (usingFallback) {
			usingFallback = false;
			fallback.stop();
		}
	}

	public void shutdown() {
		stop();
		MinecraftSoundBridge.release();
	}

	private static float readGameGain(Minecraft client) {
		if (client == null || client.options == null) {
			return 1.0f;
		}
		//? if >= 1.21.11 {
		float voice = client.options.getFinalSoundSourceVolume(SoundSource.VOICE);
		if (voice > 0.001f) {
			return voice;
		}
		return client.options.getFinalSoundSourceVolume(SoundSource.MASTER);
		//?} else {
		/*float voice = client.options.getSoundSourceVolume(SoundSource.VOICE);
		if (voice > 0.001f) {
			return voice;
		}
		return client.options.getSoundSourceVolume(SoundSource.MASTER);*/
		//?}
	}
}
