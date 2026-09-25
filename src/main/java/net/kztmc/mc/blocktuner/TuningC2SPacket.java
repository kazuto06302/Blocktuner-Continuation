package net.kztmc.mc.blocktuner;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record TuningC2SPacket(BlockPos blockPos, int note) implements CustomPayload {

    public static final CustomPayload.Id<TuningC2SPacket> ID = new CustomPayload.Id<>(BlockTuner.TUNING_CHANNEL);
    public static final PacketCodec<RegistryByteBuf, TuningC2SPacket> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            TuningC2SPacket::blockPos,
            PacketCodecs.INTEGER,
            TuningC2SPacket::note,
            TuningC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void receive(TuningC2SPacket payload, ServerPlayNetworking.Context context) {
        BlockPos pos = payload.blockPos();
        int note = payload.note();
        World world = context.player().getEntityWorld();

        if (!isNoteBlock(world, pos)) return;

        tuneBlockNote(world, pos, note);
        try2PlayNoteBlock(world, pos, note);
        swingPlayerHand(context);
    }

    private static void swingPlayerHand(ServerPlayNetworking.Context context) {
        context.player().swingHand(Hand.MAIN_HAND);
    }

    private static void try2PlayNoteBlock(World world, BlockPos pos, double note) {
        if (isAirAbove(world, pos)) playNoteBlock(world, pos, note);
    }

    private static boolean isAirAbove(World world, BlockPos pos) {
        return world.getBlockState(pos.up()).isAir();
    }

    private static void playNoteBlock(World world, BlockPos pos, double note) {
        world.getBlockState(pos).onSyncedBlockEvent(world, pos, 0, 0);
        ((ServerWorld) world).spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 0, note / 24.0D, 0.0D, 0.0D, 1.0D);
    }

    private static void tuneBlockNote(World world, BlockPos pos, int note) {
        world.setBlockState(pos, world.getBlockState(pos).with(NoteBlock.NOTE, note), 3);
    }

    private static boolean isNoteBlock(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK;
    }

}
