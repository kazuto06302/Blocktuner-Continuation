package net.kztmc.mc.blocktuner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

@Environment(EnvType.CLIENT)
public class BlockTunerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockTunerConfig.load();
        MidiManager.getMidiManager().refreshMidiDevice();
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (BlockTunerConfig.onBlockTunerServer
                    && isControlDown()
                    && !player.isSpectator()
                    && !player.isShiftKeyDown()
                    && world.getBlockState(hitResult.getBlockPos()).is(Blocks.NOTE_BLOCK)
                    && !player.getMainHandItem().is(Items.BLAZE_ROD)) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> client.gui.setScreen(new TuningScreen(Component.empty(), hitResult.getBlockPos())));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // knowing a BlockTuner server
        ClientPlayNetworking.registerGlobalReceiver(ProtocolCheckS2CPacket.TYPE, ProtocolCheckS2CPacket::receive);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BlockTunerConfig.onBlockTunerServer = false);
    }

    public static boolean isControlDown() {
        return Minecraft.getInstance().hasControlDown();
    }
}
