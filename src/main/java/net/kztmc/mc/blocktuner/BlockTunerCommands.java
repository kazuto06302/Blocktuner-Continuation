package net.kztmc.mc.blocktuner;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

public class BlockTunerCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("tune")
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("note", IntegerArgumentType.integer(0, 24))
                .executes(context -> tune(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "note"))))
        ));
    }

    private static int tune(ServerCommandSource source, BlockPos pos, int note){
        ServerWorld world = source.getWorld();
        if (world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) {
            return -1;
        }
        world.setBlockState(pos, world.getBlockState(pos).with(NoteBlock.NOTE, note));
        // please do not change this to world.addSyncedBlockEvent() as it does not allow chords to be played.
        world.getBlockState(pos).onSyncedBlockEvent(world, pos, 0, 0);
        world.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 0, (double)note / 24.0D, 0.0D, 0.0D, 1.0D);
        world.emitGameEvent(source.getEntity(), GameEvent.NOTE_BLOCK_PLAY, pos);
        return note;
    }
}
