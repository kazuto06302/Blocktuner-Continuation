package net.kztmc.mc.blocktuner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class NoteNameHud {

    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        assert client.level != null;
        assert client.player != null;
        if(BlockTunerClient.isControlDown() && !client.player.isSpectator()) {
            HitResult hitResult = client.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
                BlockState state = client.level.getBlockState(blockPos);
                if (state.is(Blocks.NOTE_BLOCK)) {
                    int note = state.getValue(NoteBlock.NOTE);
                    int x = client.getWindow().getGuiScaledWidth() / 2 + 4;
                    int y = client.getWindow().getGuiScaledHeight() / 2 + 4;
                    context.text(client.font, NoteNames.get(note) + ", " + note, x, y, 0x55FFFF, true);
                }
            }
        }
    }
}
