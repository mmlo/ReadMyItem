package dev.mml.readmyitem.mixin;

import dev.mml.readmyitem.ReadMyItemMod;
import dev.mml.readmyitem.hover.HoverTracker;
import dev.mml.readmyitem.perf.PerfProbe;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void readmyitem$onContainerTick(CallbackInfo ci) {
		long started = System.nanoTime();
		try {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
			ItemStack stack = hoveredSlot == null || !hoveredSlot.hasItem()
					? ItemStack.EMPTY
					: hoveredSlot.getItem();
			HoverTracker.onSlot(screen, hoveredSlot, stack);
		} catch (Throwable ignored) {
			// A third-party screen that breaks this hook must not crash the game.
		} finally {
			String alert = PerfProbe.record(PerfProbe.Kind.HOVER_TICK, System.nanoTime() - started);
			if (alert != null) {
				ReadMyItemMod.LOGGER.warn("[ReadMyItem][perf] {}", alert);
			}
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void readmyitem$onRemoved(CallbackInfo ci) {
		try {
			HoverTracker.onLeftContainer();
		} catch (Throwable ignored) {
			// isolated
		}
	}
}
