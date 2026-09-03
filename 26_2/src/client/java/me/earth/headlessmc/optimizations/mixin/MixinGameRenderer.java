package me.earth.headlessmc.optimizations.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips submitting a frame once loading is complete and no UI needs a frame. */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    @Unique
    private boolean hmcOptimizations$skippedRenderFrame;

    @Inject(method = "extract", at = @At("HEAD"))
    private void hmcOptimizations$rebuildAfterSkippedFrames(
        DeltaTracker deltaTracker,
        boolean renderLevel,
        CallbackInfo callback
    ) {
        boolean willSkip = minecraft.isGameLoadFinished()
            && minecraft.gui.overlay() == null
            && minecraft.gui.screen() == null;

        if (!willSkip && hmcOptimizations$skippedRenderFrame) {
            // Extraction can publish and clear chunk-render deltas even when
            // render() never consumes them. Rebuild before the first real UI
            // frame so a menu's world background cannot use stale geometry.
            hmcOptimizations$skippedRenderFrame = false;
            minecraft.levelExtractor.allChanged();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hmcOptimizations$skipHeadlessFrame(
        DeltaTracker deltaTracker,
        boolean renderLevel,
        CallbackInfo callback
    ) {
        // Loading overlays advance their completion from render callbacks.
        // Screens are also left intact for HMC-Specifics GUI inspection.
        if (minecraft.isGameLoadFinished()
            && minecraft.gui.overlay() == null
            && minecraft.gui.screen() == null) {
            // These calls are the lifecycle tail of vanilla render(). Even a
            // skipped frame must rotate staged buffers and age pooled native
            // resources so an earlier real UI/loading frame cannot be retained.
            fogRenderer.endFrame();
            guiRenderer.endFrame();
            renderBuffers.endFrame();
            resourcePool.endFrame();
            hmcOptimizations$skippedRenderFrame = true;
            callback.cancel();
        }
    }
}
