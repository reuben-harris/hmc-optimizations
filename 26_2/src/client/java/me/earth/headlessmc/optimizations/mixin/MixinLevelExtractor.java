package me.earth.headlessmc.optimizations.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips building transient world render state when no UI is being rendered. */
@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void hmcOptimizations$skipWorldRenderState(
        DeltaTracker deltaTracker,
        Camera camera,
        float partialTick,
        CallbackInfo callback
    ) {
        boolean skip = minecraft.isGameLoadFinished()
            && minecraft.gui.overlay() == null
            && minecraft.gui.screen() == null;

        if (skip) {
            // Drop entity, block-entity, weather, particle, and section state
            // retained from the last real frame. This is vanilla's own early
            // per-frame reset, not a gameplay/world mutation.
            levelRenderState.reset();

            // Vanilla swaps these four tracking sets only from extract(). Keep
            // draining them while extraction is disabled so movement cannot
            // retain every chunk/section delta encountered by the client.
            if (minecraft.level != null) {
                minecraft.level.getChunkSource().flipUpdateTrackingSets();
            }

            callback.cancel();
        }
    }
}
