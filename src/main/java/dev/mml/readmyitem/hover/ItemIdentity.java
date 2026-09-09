package dev.mml.readmyitem.hover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemIdentity {
	private ItemIdentity() {
	}

	public static String of(ItemStack stack, int slotIndex) {
		if (stack == null || stack.isEmpty()) {
			return "empty:" + slotIndex;
		}
		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String custom = "";
		var name = stack.getCustomName();
		if (name != null) {
			custom = name.getString();
		}
		return key + "x" + stack.getCount() + "d" + stack.getDamageValue()
				+ "@" + slotIndex + "~" + custom;
	}

	public static String ofIgnoringSlot(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "empty";
		}
		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key + "x" + stack.getCount() + "d" + stack.getDamageValue();
	}
}
