package net.kztmc.mc.blocktuner;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockTuner implements ModInitializer {

    public static final String MOD_ID = "blocktuner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier CLIENT_CHECK = identifier("client_check");
    public static final Identifier TUNING_CHANNEL = identifier("tune");

    // tuning protocol version. The number on client needs to match that on server.
    public static final int TUNING_PROTOCOL = 2;

    public static Identifier identifier(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[BlockTuner] Now Loading BlockTuner!");
        CommandRegistrationCallback.EVENT.register(BlockTunerCommands::register);
        PayloadTypeRegistry.playS2C().register(ProtocolCheckS2CPacket.ID, ProtocolCheckS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TuningC2SPacket.ID, TuningC2SPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(new ProtocolCheckS2CPacket(TUNING_PROTOCOL)));
        ServerPlayNetworking.registerGlobalReceiver(TuningC2SPacket.ID, TuningC2SPacket::receive);
    }
}
