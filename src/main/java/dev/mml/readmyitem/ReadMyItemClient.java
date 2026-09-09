package dev.mml.readmyitem;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mml.readmyitem.audio.NarrationPlayer;
import dev.mml.readmyitem.config.ModConfig;
import dev.mml.readmyitem.hover.HoverTracker;
import dev.mml.readmyitem.perf.PerfProbe;
import dev.mml.readmyitem.tts.BundledVoiceExtractor;
import dev.mml.readmyitem.tts.PiperInProcessEngine;
import dev.mml.readmyitem.ui.LargeTextOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ReadMyItemClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(ReadMyItemMod.MOD_ID, "main"));

	public static final KeyMapping TOGGLE_KEY = new KeyMapping(
			"key.readmyitem.toggle",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KEY_CATEGORY);
	public static final KeyMapping REPEAT_KEY = new KeyMapping(
			"key.readmyitem.repeat",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KEY_CATEGORY);
	public static final KeyMapping STOP_KEY = new KeyMapping(
			"key.readmyitem.stop",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KEY_CATEGORY);

	private static PiperInProcessEngine engine;
	private static boolean missingVoiceToastShown;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		PerfProbe.setReportFile(FabricLoader.getInstance().getConfigDir()
				.resolve("readmyitem").resolve("perf-ultimo.txt"));

		engine = new PiperInProcessEngine();
		HoverTracker.setEngine(engine);

		Thread extract = new Thread(() -> {
			BundledVoiceExtractor.extractAll();
			engine.preloadVoice();
		}, "readmyitem-voice-extract");
		extract.setDaemon(true);
		extract.start();

		KeyBindingHelper.registerKeyBinding(TOGGLE_KEY);
		KeyBindingHelper.registerKeyBinding(REPEAT_KEY);
		KeyBindingHelper.registerKeyBinding(STOP_KEY);

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?>) {
				ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) ->
						LargeTextOverlay.render(graphics, s));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(ReadMyItemClient::onClientTick);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			PerfProbe.dump();
			for (String line : PerfProbe.diagnose()) {
				ReadMyItemMod.LOGGER.info("[ReadMyItem][perf] {}", line);
			}
			if (engine != null) {
				engine.shutdown();
			}
			NarrationPlayer.get().shutdown();
		});

		ReadMyItemMod.LOGGER.info("[ReadMyItem] cliente iniciado (Piper in-process, sem subprocesso)");
	}

	public static PiperInProcessEngine engine() {
		return engine;
	}

	public static void notifyMissingVoiceOnce(Minecraft client) {
		if (missingVoiceToastShown) {
			return;
		}
		missingVoiceToastShown = true;
		if (client != null && client.gui != null) {
			client.gui.setOverlayMessage(Component.translatable("readmyitem.missing_voice"), false);
		}
		ReadMyItemMod.LOGGER.warn("[ReadMyItem] {}", Component.translatable("readmyitem.missing_voice").getString());
	}

	public static void resetMissingVoiceNotice() {
		missingVoiceToastShown = false;
	}

	private static boolean languageChecked;

	private static void onClientTick(Minecraft client) {
		if (!languageChecked && client.options != null) {
			languageChecked = true;
			ModConfig config = ModConfig.get();
			if (config.linguagem == ModConfig.AppLanguage.AUTO) {
				String code = client.options.languageCode;
				if (code != null && code.toLowerCase().startsWith("pt")) {
					config.linguagem = ModConfig.AppLanguage.PT_BR;
					config.voz = dev.mml.readmyitem.tts.VoiceChecksums.FABER;
				} else {
					config.linguagem = ModConfig.AppLanguage.EN_US;
					config.voz = dev.mml.readmyitem.tts.VoiceChecksums.EN_US_DEFAULT;
				}
				ModConfig.save();
				ReadMyItemMod.LOGGER.info("[ReadMyItem] idioma auto-detectado: {}", config.linguagem);
				if (engine != null) {
					engine.reloadVoice();
				}
			}
		}

		NarrationPlayer.get().sampleGain(client);
		if (TOGGLE_KEY.consumeClick()) {
			ModConfig config = ModConfig.get();
			config.enabled = !config.enabled;
			ModConfig.save();
			if (!config.enabled) {
				HoverTracker.stopAndIdle();
			}
		}
		if (STOP_KEY.consumeClick()) {
			HoverTracker.stopSpeech();
		}
		if (REPEAT_KEY.consumeClick()) {
			HoverTracker.repeatLast();
		}

		if (!(client.screen instanceof AbstractContainerScreen<?>)) {
			HoverTracker.onLeftContainer();
		}
		HoverTracker.tick();
	}
}
