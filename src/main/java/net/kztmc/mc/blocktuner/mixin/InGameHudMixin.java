package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.NoteNameHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderNoteNameHud(
            GuiGraphicsExtractor context,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        NoteNameHud.render(context);
    }
}