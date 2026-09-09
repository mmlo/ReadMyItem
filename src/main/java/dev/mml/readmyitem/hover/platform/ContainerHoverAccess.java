package dev.mml.readmyitem.hover.platform;

import dev.mml.readmyitem.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Isolates 1.21.11 container-screen mapping names.
 * // stonecutter: swap this class (or the mixin accessor) when porting.
 */
public final class ContainerHoverAccess {
	private ContainerHoverAccess() {
	}

	public static @Nullable Slot hoveredSlot(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).readmyitem$hoveredSlot();
	}

	public static ItemStack stackOf(@Nullable Slot slot) {
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		return slot.getItem();
	}

	public static int slotIndex(@Nullable Slot slot) {
		return slot == null ? -1 : slot.index;
	}

	public static List<Component> tooltipLines(AbstractContainerScreen<?> screen, ItemStack stack) {
		return ((AbstractContainerScreenAccessor) screen).readmyitem$tooltip(stack);
	}
}
