package net.kztmc.mc.blocktuner;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProtocolCheckS2CPacket(int tuningProtocol) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ProtocolCheckS2CPacket> TYPE = new CustomPacketPayload.Type<>(BlockTuner.CLIENT_CHECK);
    public static final StreamCodec<ByteBuf, ProtocolCheckS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ProtocolCheckS2CPacket::tuningProtocol,
            ProtocolCheckS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(ProtocolCheckS2CPacket payload, ClientPlayNetworking.Context context) {
        int serverProtocol = payload.tuningProtocol();
        if (BlockTuner.TUNING_PROTOCOL == serverProtocol) {
            Minecraft.getInstance().execute(() -> BlockTunerConfig.onBlockTunerServer = true);
        }
    }
}
