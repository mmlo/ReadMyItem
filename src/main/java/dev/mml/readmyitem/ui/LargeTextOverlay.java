package dev.mml.readmyitem.ui;

import dev.mml.readmyitem.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class LargeTextOverlay {
	private static volatile String text = "";
	private static volatile ModConfig.OverlayPosition position = ModConfig.OverlayPosition.TOP;
	private static volatile boolean visible;

	private LargeTextOverlay() {
	}

	public static void show(String value, ModConfig.OverlayPosition pos) {
		text = value == null ? "" : value;
		position = pos == null ? ModConfig.OverlayPosition.TOP : pos;
		visible = !text.isBlank();
	}

	public static void hide() {
		visible = false;
	}

	public static void render(GuiGraphics graphics, Screen screen) {
		if (!visible || !ModConfig.get().overlay) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		Font font = client.font;
		int maxWidth = Math.max(80, screen.width - 40);
		List<FormattedCharSequence> lines = font.split(Component.literal(text), maxWidth);
		if (lines.isEmpty()) {
			return;
		}
		int lineHeight = Math.max(12, font.lineHeight + 4);
		int boxHeight = lines.size() * lineHeight + 10;
		int boxWidth = 20;
		for (FormattedCharSequence line : lines) {
			boxWidth = Math.max(boxWidth, font.width(line) + 16);
		}
		boxWidth = Math.min(boxWidth, maxWidth + 16);

		int x = switch (position) {
			case TOP -> (screen.width - boxWidth) / 2;
			case TOP_LEFT -> 8;
			case TOP_RIGHT -> screen.width - boxWidth - 8;
		};
		int y = 8;

		graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xE0000000);
		graphics.fill(x, y, x + boxWidth, y + 2, 0xFFFFFF00);

		int textY = y + 6;
		for (FormattedCharSequence line : lines) {
			int textX = switch (position) {
				case TOP -> x + (boxWidth - font.width(line)) / 2;
				case TOP_LEFT -> x + 8;
				case TOP_RIGHT -> x + boxWidth - font.width(line) - 8;
			};
			graphics.drawString(font, line, textX, textY, 0xFFFFFFFF, false);
			textY += lineHeight;
		}
	}
}
