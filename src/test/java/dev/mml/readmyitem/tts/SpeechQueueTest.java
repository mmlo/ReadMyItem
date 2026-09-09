package dev.mml.readmyitem.tts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeechQueueTest {
	@Test
	void newestReplacesOldest() {
		SpeechQueue<String> queue = new SpeechQueue<>();
		queue.offer("um");
		queue.offer("dois");
		assertEquals("dois", queue.take());
		assertNull(queue.take());
		assertTrue(queue.isEmpty());
	}

	@Test
	void applySpeedStretchesPcm() {
		short[] pcm = new short[100];
		short[] slower = PiperInProcessEngine.applySpeed(pcm, 0.92f);
		assertTrue(slower.length > pcm.length);
	}
}
