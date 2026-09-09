package dev.mml.readmyitem.hover;

import dev.mml.readmyitem.config.ModConfig;
import dev.mml.readmyitem.hover.platform.ContainerHoverAccess;
import dev.mml.readmyitem.text.MinecraftTextSanitizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TooltipExtractor {
	private TooltipExtractor() {
	}

	public static SpokenItem extract(
			AbstractContainerScreen<?> screen,
			ItemStack stack,
			int slotIndex,
			ModConfig config
	) {
		if (stack == null || stack.isEmpty()) {
			return SpokenItem.empty(slotIndex);
		}

		String name = MinecraftTextSanitizer.sanitize(preferredName(stack));
		List<String> details = new ArrayList<>();
		boolean skipName = true;
		for (Component line : tooltipLines(screen, stack)) {
			String text = MinecraftTextSanitizer.sanitize(line.getString());
			if (text.isEmpty()) {
				continue;
			}
			if (skipName && text.equalsIgnoreCase(name)) {
				skipName = false;
				continue;
			}
			skipName = false;
			if (!config.modoTecnico && MinecraftTextSanitizer.isTechnicalLine(text)) {
				continue;
			}
			details.add(text);
		}

		if (config.sempreDurabilidade && stack.isDamageableItem()) {
			int remaining = stack.getMaxDamage() - stack.getDamageValue();
			String dura = "durabilidade " + remaining + " de " + stack.getMaxDamage();
			boolean already = details.stream().anyMatch(s -> s.toLowerCase().contains("durabilidade"));
			if (!already) {
				details.add(dura);
			}
		}

		return new SpokenItem(name, stack.getCount(), details, ItemIdentity.of(stack, slotIndex));
	}

	private static String preferredName(ItemStack stack) {
		Component custom = stack.getCustomName();
		if (custom != null) {
			return custom.getString();
		}
		return stack.getHoverName().getString();
	}

	private static List<Component> tooltipLines(AbstractContainerScreen<?> screen, ItemStack stack) {
		try {
			List<Component> fromScreen = ContainerHoverAccess.tooltipLines(screen, stack);
			if (fromScreen != null && !fromScreen.isEmpty()) {
				return fromScreen;
			}
		} catch (Throwable ignored) {
			// Legendary Tooltips and similar mods can break the screen hook.
		}
		Minecraft client = Minecraft.getInstance();
		Player player = client != null ? client.player : null;
		TooltipFlag flag = client != null && client.options.advancedItemTooltips
				? TooltipFlag.ADVANCED
				: TooltipFlag.NORMAL;
		Item.TooltipContext context = client != null && client.level != null
				? Item.TooltipContext.of(client.level)
				: Item.TooltipContext.EMPTY;
		try {
			return stack.getTooltipLines(context, player, flag);
		} catch (Throwable t) {
			return List.of(stack.getHoverName());
		}
	}

	public static @Nullable SpokenItem tryExtract(
			AbstractContainerScreen<?> screen,
			ItemStack stack,
			int slotIndex,
			ModConfig config
	) {
		try {
			return extract(screen, stack, slotIndex, config);
		} catch (Throwable t) {
			return null;
		}
	}
}
