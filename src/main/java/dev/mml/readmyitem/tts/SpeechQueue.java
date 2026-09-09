package dev.mml.readmyitem.tts;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Size-1 queue: a newer utterance replaces anything waiting.
 */
public final class SpeechQueue<T> {
	private final AtomicReference<T> slot = new AtomicReference<>();

	public void offer(T item) {
		slot.set(item);
	}

	public T take() {
		return slot.getAndSet(null);
	}

	public void clear() {
		slot.set(null);
	}

	public boolean isEmpty() {
		return slot.get() == null;
	}
}
