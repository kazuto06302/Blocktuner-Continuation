package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.NoteNameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
     @Inject(method = "render", at = @At("TAIL"))
     private void renderNoteNameHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
         NoteNameHud.render(context);
     }
}
