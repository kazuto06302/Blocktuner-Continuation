package net.kztmc.mc.blocktuner;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record ProtocolCheckS2CPacket(int tuningProtocol) implements CustomPayload {

    public static final CustomPayload.Id<ProtocolCheckS2CPacket> ID = new CustomPayload.Id<>(BlockTuner.CLIENT_CHECK);
    public static final PacketCodec<RegistryByteBuf, ProtocolCheckS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            ProtocolCheckS2CPacket::tuningProtocol,
            ProtocolCheckS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void receive(ProtocolCheckS2CPacket payload, ClientPlayNetworking.Context context) {
        int serverProtocol = payload.tuningProtocol();
        if (BlockTuner.TUNING_PROTOCOL == serverProtocol) {
            MinecraftClient.getInstance().execute(() -> BlockTunerConfig.onBlockTunerServer = true);
        }
    }

}
