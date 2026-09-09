package dev.mml.readmyitem.ui;

import dev.mml.readmyitem.ReadMyItemClient;
import dev.mml.readmyitem.tts.OptionalOfficialDownload;
import dev.mml.readmyitem.tts.VoiceChecksums;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoiceDownloadScreen extends Screen {
	private final Screen parent;
	private final OptionalOfficialDownload download = new OptionalOfficialDownload();
	private String statusLine = "";
	private boolean busy;

	public VoiceDownloadScreen(Screen parent) {
		super(Component.translatable("readmyitem.download.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height / 2 - 40;
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.download.faber"), b -> start(VoiceChecksums.FABER))
				.bounds(this.width / 2 - 150, y, 300, 20)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.download.edresson"), b -> start(VoiceChecksums.EDRESSON))
				.bounds(this.width / 2 - 150, y + 24, 300, 20)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.download.cancel"), b -> {
			download.cancel();
			statusLine = Component.translatable("readmyitem.download.cancelled").getString();
		}).bounds(this.width / 2 - 150, y + 48, 300, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("readmyitem.download.close"), b -> onClose())
				.bounds(this.width / 2 - 150, y + 72, 300, 20)
				.build());
	}

	private void start(String stem) {
		if (busy) {
			return;
		}
		busy = true;
		statusLine = "Baixando…";
		Thread thread = new Thread(() -> {
			OptionalOfficialDownload.Result result = download.downloadStem(stem, (pct, msg) -> statusLine = msg + " (" + pct + "%)");
			statusLine = result.message();
			busy = false;
			if (result.status() == OptionalOfficialDownload.Status.DONE && ReadMyItemClient.engine() != null) {
				ReadMyItemClient.engine().reloadVoice();
				ReadMyItemClient.resetMissingVoiceNotice();
			}
		}, "readmyitem-download");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
		if (!statusLine.isEmpty()) {
			graphics.drawCenteredString(this.font, statusLine, this.width / 2, this.height / 2 + 60, 0xFFFFAA);
		}
	}

	@Override
	public void onClose() {
		download.cancel();
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}
}
