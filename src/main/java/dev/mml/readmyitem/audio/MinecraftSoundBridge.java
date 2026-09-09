package dev.mml.readmyitem.audio;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.SoundBuffer;
import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.perf.PerfProbe;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngineExecutor;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Playback on Minecraft's sound executor only. No second OpenAL device,
 * no subprocess, and {@code ChannelHandle.release()} is never called here
 * (that runs OpenAL on the caller thread).
 */
public final class MinecraftSoundBridge {
	private static volatile ChannelAccess channelAccess;
	private static volatile SoundEngineExecutor executor;
	private static final AtomicReference<ChannelAccess.ChannelHandle> current = new AtomicReference<>();
	private static final AtomicInteger playbackGeneration = new AtomicInteger();
	private static SoundBuffer uploaded;
	private static ByteBuffer scratch;

	private MinecraftSoundBridge() {
	}

	public static void capture(ChannelAccess access, SoundEngineExecutor soundExecutor) {
		channelAccess = access;
		executor = soundExecutor;
		ReadMyItemMod.LOGGER.info("[ReadMyItem] SoundEngine do jogo capturado");
	}

	public static void release() {
		releaseFrom(channelAccess);
	}

	public static void releaseFrom(ChannelAccess access) {
		if (access == null || channelAccess != access) {
			return;
		}
		stop();
		channelAccess = null;
		executor = null;
	}

	public static boolean available() {
		return channelAccess != null && executor != null;
	}

	public static void stop() {
		int gen = playbackGeneration.incrementAndGet();
		runOnSoundThread(gen, MinecraftSoundBridge::stopNow);
	}

	public static boolean play(PcmSoundInstance instance, float gain) {
		ChannelAccess access = channelAccess;
		if (access == null || executor == null || instance == null || instance.samples().length == 0) {
			return false;
		}
		final short[] samples = instance.samples();
		final AudioFormat format = instance.getFormat();
		final float clamped = Math.max(0f, Math.min(1f, gain));
		int gen = playbackGeneration.incrementAndGet();
		runOnSoundThread(gen, () -> playNow(access, samples, format, clamped));
		return true;
	}

	private static void runOnSoundThread(int generation, Runnable task) {
		SoundEngineExecutor ex = executor;
		Runnable guarded = () -> {
			if (generation != playbackGeneration.get()) {
				return;
			}
			task.run();
		};
		if (ex != null) {
			ex.execute(guarded);
			return;
		}
		guarded.run();
	}

	private static void stopNow() {
		ChannelAccess.ChannelHandle handle = current.get();
		if (handle == null || handle.isStopped()) {
			return;
		}
		handle.execute(Channel::stop);
	}

	private static void playNow(ChannelAccess access, short[] samples, AudioFormat format, float gain) {
		long started = System.nanoTime();
		try {
			ChannelAccess.ChannelHandle handle = current.get();
			if (handle != null && !handle.isStopped()) {
				handle.execute(channel -> {
					if (channel == null) {
						current.compareAndSet(handle, null);
						acquireThenPlay(access, samples, format, gain);
						return;
					}
					attachPlay(channel, samples, format, gain);
				});
				return;
			}
			acquireThenPlay(access, samples, format, gain);
		} finally {
			String alert = PerfProbe.record(PerfProbe.Kind.PLAY, System.nanoTime() - started);
			if (alert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", alert);
			}
		}
	}

	private static void acquireThenPlay(ChannelAccess access, short[] samples, AudioFormat format, float gain) {
		try {
			access.createHandle(Library.Pool.STATIC).thenAccept(handle -> {
				if (handle == null) {
					ReadMyItemMod.LOGGER.warn("[ReadMyItem] sem canal estático no SoundEngine");
					return;
				}
				current.set(handle);
				handle.execute(channel -> attachPlay(channel, samples, format, gain));
			});
		} catch (Throwable t) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] canal de som indisponível: {}", t.toString());
		}
	}

	private static ByteBuffer fillScratch(short[] samples) {
		int bytes = samples.length * 2;
		ByteBuffer buf = scratch;
		if (buf == null || buf.capacity() < bytes) {
			buf = ByteBuffer.allocateDirect(Math.max(bytes, 128 * 1024)).order(ByteOrder.LITTLE_ENDIAN);
			scratch = buf;
		}
		buf.clear();
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.asShortBuffer().put(samples);
		buf.position(0).limit(bytes);
		return buf;
	}

	private static void attachPlay(Channel channel, short[] samples, AudioFormat format, float gain) {
		try {
			channel.stop();
			SoundBuffer previous = uploaded;
			uploaded = null;
			if (previous != null) {
				previous.discardAlBuffer();
			}
			SoundBuffer buffer = new SoundBuffer(fillScratch(samples), format);
			uploaded = buffer;
			channel.attachStaticBuffer(buffer);
			channel.setRelative(true);
			channel.disableAttenuation();
			channel.setVolume(gain);
			channel.setPitch(1.0f);
			channel.play();
		} catch (Throwable t) {
			ReadMyItemMod.LOGGER.warn("[ReadMyItem] OpenAL do jogo falhou: {}", t.toString());
		}
	}

	static SoundEngineExecutor executor() {
		return executor;
	}
}
