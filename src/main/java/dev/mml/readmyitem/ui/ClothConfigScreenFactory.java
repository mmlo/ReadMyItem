package dev.mml.readmyitem.ui;

import dev.mml.readmyitem.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ClothConfigScreenFactory {
	private ClothConfigScreenFactory() {
	}

	public static Screen create(Screen parent) {
		ModConfig config = ModConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("readmyitem.config.title"))
				.setSavingRunnable(() -> {
					ModConfig.save();
					if (dev.mml.readmyitem.ReadMyItemClient.engine() != null) {
						dev.mml.readmyitem.ReadMyItemClient.engine().reloadVoice();
					}
				});
		ConfigEntryBuilder entry = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("readmyitem.config.category.general"));
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.enabled"), config.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("readmyitem.config.enabled.tooltip"))
				.setSaveConsumer(v -> config.enabled = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.read_name"), config.lerNome)
				.setDefaultValue(true)
				.setSaveConsumer(v -> config.lerNome = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.read_description"), config.lerDescricao)
				.setDefaultValue(true)
				.setSaveConsumer(v -> config.lerDescricao = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.read_count"), config.lerQuantidade)
				.setDefaultValue(true)
				.setSaveConsumer(v -> config.lerQuantidade = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.read_empty"), config.falarSlotVazio)
				.setDefaultValue(false)
				.setSaveConsumer(v -> config.falarSlotVazio = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.always_durability"), config.sempreDurabilidade)
				.setDefaultValue(false)
				.setSaveConsumer(v -> config.sempreDurabilidade = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.technical"), config.modoTecnico)
				.setDefaultValue(false)
				.setSaveConsumer(v -> config.modoTecnico = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.interrupt"), config.interromperAoSair)
				.setDefaultValue(true)
				.setSaveConsumer(v -> config.interromperAoSair = v)
				.build());
		general.addEntry(entry.startIntField(Component.translatable("readmyitem.config.max_chars"), config.maxCaracteres)
				.setDefaultValue(280)
				.setMin(40)
				.setMax(2000)
				.setSaveConsumer(v -> config.maxCaracteres = v)
				.build());

		ConfigCategory timing = builder.getOrCreateCategory(Component.translatable("readmyitem.config.category.timing"));
		timing.addEntry(entry.startIntSlider(Component.translatable("readmyitem.config.hover_delay"), config.hoverDelayMs, 0, 2000)
				.setDefaultValue(450)
				.setTooltip(Component.translatable("readmyitem.config.hover_delay.tooltip"))
				.setSaveConsumer(v -> config.hoverDelayMs = v)
				.build());
		timing.addEntry(entry.startIntSlider(Component.translatable("readmyitem.config.dwell"), config.dwellDetalhesMs, 0, 3000)
				.setDefaultValue(900)
				.setTooltip(Component.translatable("readmyitem.config.dwell.tooltip"))
				.setSaveConsumer(v -> config.dwellDetalhesMs = v)
				.build());

		ConfigCategory voice = builder.getOrCreateCategory(Component.translatable("readmyitem.config.category.voice"));
		// Removed Idioma selector as requested

		java.util.List<String> stems = dev.mml.readmyitem.tts.VoiceLibrary.getAvailableStems();
		if (!stems.contains(config.voz)) {
			stems = new java.util.ArrayList<>(stems);
			stems.add(config.voz);
		}
		voice.addEntry(entry.startSelector(
						Component.translatable("readmyitem.config.voice"),
						stems.toArray(new String[0]),
						config.voz)
				.setDefaultValue(dev.mml.readmyitem.tts.VoiceChecksums.FABER)
				.setNameProvider(stem -> {
					String display = stem;
					if (display.startsWith("pt_BR-")) {
						display = "pt_BR (" + display.substring(6) + ")";
					} else if (display.startsWith("en_US-")) {
						display = "en_US (" + display.substring(6) + ")";
					}
					return Component.literal(display);
				})
				.setTooltip(Component.translatable("readmyitem.config.voice.tooltip"))
				.setSaveConsumer(v -> {
					config.voz = v;
					if (v.startsWith("en_US-")) {
						config.linguagem = ModConfig.AppLanguage.EN_US;
					} else if (v.startsWith("pt_BR-")) {
						config.linguagem = ModConfig.AppLanguage.PT_BR;
					}
				})
				.build());
		voice.addEntry(entry.startFloatField(Component.translatable("readmyitem.config.speed"), config.velocidadeFala)
				.setDefaultValue(0.92f)
				.setMin(0.75f)
				.setMax(1.35f)
				.setTooltip(Component.translatable("readmyitem.config.speed.tooltip"))
				.setSaveConsumer(v -> config.velocidadeFala = v)
				.build());
		voice.addEntry(entry.startFloatField(Component.translatable("readmyitem.config.volume"), config.volumeFala)
				.setDefaultValue(1.0f)
				.setMin(0f)
				.setMax(1f)
				.setSaveConsumer(v -> config.volumeFala = v)
				.build());

		ConfigCategory overlay = builder.getOrCreateCategory(Component.translatable("readmyitem.config.category.overlay"));
		overlay.addEntry(entry.startBooleanToggle(Component.translatable("readmyitem.config.overlay"), config.overlay)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("readmyitem.config.overlay.tooltip"))
				.setSaveConsumer(v -> config.overlay = v)
				.build());
		overlay.addEntry(entry.startEnumSelector(
						Component.translatable("readmyitem.config.overlay_position"),
						ModConfig.OverlayPosition.class,
						config.overlayPosition)
				.setDefaultValue(ModConfig.OverlayPosition.TOP)
				.setSaveConsumer(v -> config.overlayPosition = v)
				.build());

		return builder.build();
	}
}
