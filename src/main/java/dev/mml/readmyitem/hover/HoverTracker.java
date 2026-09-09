package dev.mml.readmyitem.hover;

import dev.mml.readmyitem.ReadMyItemClient;
import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.config.ModConfig;
import dev.mml.readmyitem.hover.platform.ContainerHoverAccess;
import dev.mml.readmyitem.perf.PerfProbe;
import dev.mml.readmyitem.tts.BundledVoiceExtractor;
import dev.mml.readmyitem.tts.TtsEngine;
import dev.mml.readmyitem.ui.LargeTextOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class HoverTracker {
	private static final HoverStateMachine MACHINE = new HoverStateMachine();

	private static TtsEngine engine;
	private static @Nullable SpokenItem lastSpoken;
	private static String lastNameUtterance = "";
	private static String lastDetailUtterance = "";
	private static long overlayHideAt;
	private static boolean overlayShowingDetails;

	private HoverTracker() {
	}

	public static void setEngine(TtsEngine ttsEngine) {
		engine = ttsEngine;
	}

	public static void onSlot(AbstractContainerScreen<?> container, @Nullable Slot slot, ItemStack stack) {
		ModConfig config = ModConfig.get();
		if (!config.enabled) {
			return;
		}
		int index = ContainerHoverAccess.slotIndex(slot);
		boolean empty = stack == null || stack.isEmpty();
		String id = empty ? (config.falarSlotVazio ? "empty:" + index : "") : ItemIdentity.of(stack, index);
		long now = System.currentTimeMillis();
		HoverStateMachine.Event event = MACHINE.onFrame(
				id, empty, config.falarSlotVazio, now,
				config.hoverDelayMs, config.dwellDetalhesMs, HoverStateMachine.GRACE_MS);

		if (config.overlay && event == HoverStateMachine.Event.NONE && !id.isEmpty()
				&& MACHINE.phase() != HoverStateMachine.Phase.IDLE) {
			SpokenItem preview = TooltipExtractor.tryExtract(container, empty ? ItemStack.EMPTY : stack, index, config);
			if (preview != null) {
				String overlay = SpeechPlanner.nameUtterance(preview, config.lerQuantidade, config.maxCaracteres);
				if (!overlay.isEmpty()) {
					LargeTextOverlay.show(overlay, config.overlayPosition);
					overlayHideAt = 0;
				}
			}
		}

		handleEvent(event, container, stack, index, empty, config);
	}

	public static void tick() {
		if (overlayHideAt > 0 && System.currentTimeMillis() >= overlayHideAt) {
			LargeTextOverlay.hide();
			overlayHideAt = 0;
		}
	}

	public static void onLeftContainer() {
		if (MACHINE.phase() != HoverStateMachine.Phase.IDLE) {
			stopAndIdle();
			overlayHideAt = System.currentTimeMillis() + 400;
		}
	}

	public static void stopSpeech() {
		if (engine != null) {
			engine.stop();
		}
	}

	public static void stopAndIdle() {
		stopSpeech();
		MACHINE.reset(System.currentTimeMillis());
	}

	public static void repeatLast() {
		ModConfig config = ModConfig.get();
		if (!config.enabled || lastSpoken == null || engine == null) {
			return;
		}
		String text = overlayShowingDetails && !lastDetailUtterance.isEmpty()
				? lastDetailUtterance
				: lastNameUtterance;
		if (text.isEmpty()) {
			return;
		}
		LargeTextOverlay.show(text, config.overlayPosition);
		speak(text, config);
	}

	private static void handleEvent(
			HoverStateMachine.Event event,
			AbstractContainerScreen<?> container,
			ItemStack stack,
			int index,
			boolean empty,
			ModConfig config
	) {
		switch (event) {
			case NONE -> {
			}
			case STOP, STOP_THEN_WAIT -> {
				if (config.interromperAoSair) {
					stopSpeech();
				}
				overlayHideAt = System.currentTimeMillis() + 400;
				overlayShowingDetails = false;
				if (event == HoverStateMachine.Event.STOP) {
					return;
				}
			}
			case SPEAK_NAME -> speakName(container, stack, index, empty, config);
			case SPEAK_DETAILS -> speakDetails(config);
		}
	}

	private static void speakName(
			AbstractContainerScreen<?> container,
			ItemStack stack,
			int index,
			boolean empty,
			ModConfig config
	) {
		SpokenItem spoken;
		if (empty) {
			spoken = SpokenItem.empty(index);
		} else {
			long started = System.nanoTime();
			spoken = TooltipExtractor.tryExtract(container, stack, index, config);
			String alert = PerfProbe.record(PerfProbe.Kind.TOOLTIP, System.nanoTime() - started);
			if (alert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", alert);
			}
		}
		if (spoken == null) {
			return;
		}
		lastSpoken = spoken;
		String utterance = empty
				? SpeechPlanner.emptySlotUtterance()
				: SpeechPlanner.nameUtterance(spoken, config.lerQuantidade, config.maxCaracteres);
		lastNameUtterance = utterance;
		lastDetailUtterance = SpeechPlanner.detailUtterance(spoken, config.maxCaracteres);
		overlayShowingDetails = false;
		if (config.overlay) {
			LargeTextOverlay.show(utterance, config.overlayPosition);
		}
		ReadMyItemMod.LOGGER.debug("[ReadMyItem] nome: {}", utterance);
		if (config.lerNome) {
			speak(utterance, config);
		}
	}

	private static void speakDetails(ModConfig config) {
		if (!config.lerDescricao || lastDetailUtterance.isEmpty()) {
			return;
		}
		overlayShowingDetails = true;
		if (config.overlay) {
			LargeTextOverlay.show(lastDetailUtterance, config.overlayPosition);
		}
		ReadMyItemMod.LOGGER.debug("[ReadMyItem] detalhes: {}", lastDetailUtterance);
		speak(lastDetailUtterance, config);
	}

	private static void speak(String text, ModConfig config) {
		if (text == null || text.isBlank() || engine == null) {
			return;
		}
		if (!engine.isReady()) {
			if (!BundledVoiceExtractor.isFinished()) {
				return;
			}
			ReadMyItemClient.notifyMissingVoiceOnce(Minecraft.getInstance());
			return;
		}
		engine.speak(text, config.velocidadeFala, config.volumeFala);
	}

	public static @Nullable SpokenItem lastSpoken() {
		return lastSpoken;
	}
}
