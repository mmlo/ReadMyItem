package dev.mml.readmyitem.tts;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UtteranceCache {
	public record Entry(short[] pcm, int sampleRate) {
	}

	private final int maxEntries;
	private final int maxTextLength;
	private final LinkedHashMap<String, Entry> map;

	public UtteranceCache(int maxEntries, int maxTextLength) {
		this.maxEntries = maxEntries;
		this.maxTextLength = maxTextLength;
		this.map = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
				return size() > UtteranceCache.this.maxEntries;
			}
		};
	}

	public synchronized Entry get(String key) {
		return map.get(key);
	}

	public synchronized void put(String key, Entry entry) {
		if (key == null || entry == null) {
			return;
		}
		map.put(key, entry);
	}

	public synchronized void clear() {
		map.clear();
	}

	static String key(String voice, float speed, String text) {
		return voice + "|" + Math.round(speed * 100) + "|" + text;
	}
}
