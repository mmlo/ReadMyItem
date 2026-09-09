package dev.mml.readmyitem.tts;

public interface TtsEngine {
	void init();

	boolean isReady();

	void speak(String text, float speed, float volume);

	void stop();

	void shutdown();
}
