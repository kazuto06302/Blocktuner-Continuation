package net.kztmc.mc.blocktuner;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;

public record TuningC2SPacket(BlockPos blockPos, int note) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TuningC2SPacket> TYPE = new CustomPacketPayload.Type<>(BlockTuner.TUNING_CHANNEL);
    public static final StreamCodec<ByteBuf, TuningC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TuningC2SPacket::blockPos,
            ByteBufCodecs.INT,
            TuningC2SPacket::note,
            TuningC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(TuningC2SPacket payload, ServerPlayNetworking.Context context) {
        BlockPos pos = payload.blockPos();
        int note = payload.note();
        Level world = context.player().level();

        if (!isNoteBlock(world, pos)) return;

        tuneBlockNote(world, pos, note);
        try2PlayNoteBlock(world, pos, note);
        swingPlayerHand(context);
    }

    private static void swingPlayerHand(ServerPlayNetworking.Context context) {
        context.player().swing(
                InteractionHand.MAIN_HAND,
                SwingAnimation.DEFAULT,
                true
        );
    }
    private static void try2PlayNoteBlock(Level world, BlockPos pos, double note) {
        if (isAirAbove(world, pos)) playNoteBlock(world, pos, note);
    }

    private static boolean isAirAbove(Level world, BlockPos pos) {
        return world.getBlockState(pos.above()).isAir();
    }

    private static void playNoteBlock(Level world, BlockPos pos, double note) {
        world.getBlockState(pos).triggerEvent(world, pos, 0, 0);
        ((ServerLevel) world).sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 0, note / 24.0D, 0.0D, 0.0D, 1.0D);
    }

    private static void tuneBlockNote(Level world, BlockPos pos, int note) {
        world.setBlock(pos, world.getBlockState(pos).setValue(NoteBlock.NOTE, note), 3);
    }

    private static boolean isNoteBlock(Level world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.NOTE_BLOCK);
    }

}
