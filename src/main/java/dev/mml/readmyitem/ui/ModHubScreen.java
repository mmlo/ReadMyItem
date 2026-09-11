package dev.mml.readmyitem.ui;

import dev.mml.readmyitem.tts.VoiceLibrary;
//? if >= 1.20.5 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;*/
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.fabricmc.loader.api.FabricLoader;

public final class ModHubScreen extends Screen {
	private final Screen parent;

	public ModHubScreen(Screen parent) {
		super(Component.translatable("readmyitem.hub.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 40;
		
		boolean hasCloth = FabricLoader.getInstance().isModLoaded("cloth-config2");
		if (!hasCloth) {
			hasCloth = FabricLoader.getInstance().isModLoaded("cloth-config");
		}
		
		Button settingsBtn = Button.builder(
				hasCloth ? Component.translatable("readmyitem.hub.settings") : Component.literal("Configurações (Requer mod Cloth Config)"),
				b -> openSettings())
				.bounds(cx - 150, y, 300, 20)
				.build();
		settingsBtn.active = hasCloth;
		addRenderableWidget(settingsBtn);

		addRenderableWidget(Button.builder(Component.translatable("readmyitem.hub.download"),
						b -> this.minecraft.setScreen(new VoiceDownloadScreen(this)))
				.bounds(cx - 150, y + 24, 300, 20)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.hub.open_folder"), b -> openVoiceFolder())
				.bounds(cx - 150, y + 48, 300, 20)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.hub.done"), b -> onClose())
				.bounds(cx - 150, y + 72, 300, 20)
				.build());
	}

	private boolean clothConfigMissing = false;

	private void openSettings() {
		try {
			this.minecraft.setScreen(ClothConfigScreenFactory.create(this));
		} catch (Throwable t) {
			this.clothConfigMissing = true;
		}
	}

	private void openVoiceFolder() {
		VoiceLibrary.ensureVoiceDirectory();
		//? if >= 1.20.5 {
		Util.getPlatform().openPath(VoiceLibrary.voiceDirectory());
		//?} else {
		/*Util.getPlatform().openFile(VoiceLibrary.voiceDirectory().toFile());*/
		//?}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (this.minecraft != null && this.minecraft.screen != this) {
			return; // Cloth Config calls parent.render() in 1.20.1; prevent overlapping
		}
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 28, 0xFFFFFF);
		if (this.clothConfigMissing) {
			graphics.drawCenteredString(this.font, Component.literal("O mod 'Cloth Config' é necessário para abrir as configurações."), this.width / 2, 45, 0xFF5555);
		}
		String stem = VoiceLibrary.installedStemOrEmpty();
		Component status = stem.isEmpty()
				? Component.translatable("readmyitem.hub.voice_missing")
				: Component.translatable("readmyitem.hub.voice_ready", stem);
		graphics.drawCenteredString(this.font, status, this.width / 2, this.height / 2 + 70, stem.isEmpty() ? 0xFF6666 : 0x66FF66);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}
}
