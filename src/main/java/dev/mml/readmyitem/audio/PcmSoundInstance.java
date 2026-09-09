package dev.mml.readmyitem.audio;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * In-memory 16-bit mono PCM exposed as a Minecraft {@link AudioStream}.
 * Nothing is written to disk.
 */
public final class PcmSoundInstance implements AudioStream {
	private final short[] samples;
	private final int sampleRate;
	private final AudioFormat format;
	private int index;

	public PcmSoundInstance(short[] samples, int sampleRate) {
		this.samples = samples == null ? new short[0] : samples;
		this.sampleRate = sampleRate <= 0 ? 22050 : sampleRate;
		this.format = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				this.sampleRate,
				16,
				1,
				2,
				this.sampleRate,
				false);
	}

	public short[] samples() {
		return samples;
	}

	public int sampleRate() {
		return sampleRate;
	}

	public ByteBuffer toDirectBuffer() {
		ByteBuffer data = ByteBuffer.allocateDirect(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
		data.asShortBuffer().put(samples);
		data.position(0).limit(samples.length * 2);
		return data;
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	@Override
	public ByteBuffer read(int size) throws IOException {
		if (index >= samples.length || size <= 0) {
			return ByteBuffer.allocateDirect(0);
		}
		int shorts = Math.min(samples.length - index, Math.max(1, size / 2));
		ByteBuffer data = ByteBuffer.allocateDirect(shorts * 2).order(ByteOrder.LITTLE_ENDIAN);
		data.asShortBuffer().put(samples, index, shorts);
		index += shorts;
		data.position(0).limit(shorts * 2);
		return data;
	}

	@Override
	public void close() {
		index = samples.length;
	}
}
