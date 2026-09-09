package dev.mml.readmyitem.mixin;

import dev.mml.readmyitem.audio.MinecraftSoundBridge;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Shadow
	@Final
	private ChannelAccess channelAccess;

	@Shadow
	@Final
	private SoundEngineExecutor executor;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void readmyitem$capture(CallbackInfo ci) {
		MinecraftSoundBridge.capture(channelAccess, executor);
	}

	@Inject(method = "reload", at = @At("RETURN"))
	private void readmyitem$afterReload(CallbackInfo ci) {
		MinecraftSoundBridge.capture(channelAccess, executor);
	}

	@Inject(method = "destroy", at = @At("HEAD"))
	private void readmyitem$destroy(CallbackInfo ci) {
		MinecraftSoundBridge.releaseFrom(channelAccess);
	}
}
