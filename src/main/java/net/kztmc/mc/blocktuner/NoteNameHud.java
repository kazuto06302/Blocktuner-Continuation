package net.kztmc.mc.blocktuner;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class NoteNameHud {

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.world != null;
        assert client.player != null;
        if(BlockTunerClient.isControlDown() && !client.player.isSpectator()) {
            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
                BlockState state = client.world.getBlockState(blockPos);
                if (state.getBlock() == Blocks.NOTE_BLOCK) {
                    int note = state.get(NoteBlock.NOTE);
                    int x = client.getWindow().getScaledWidth() / 2 + 4;
                    int y = client.getWindow().getScaledHeight() / 2 + 4;
                    context.drawText(client.textRenderer, NoteNames.get(note) + ", " + note, x, y, 0x55FFFF, true);
                }
            }
        }
    }
}
