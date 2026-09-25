package net.kztmc.mc.blocktuner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

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
                    && !player.isSneaking()
                    && world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.NOTE_BLOCK
                    && player.getMainHandStack().getItem() != Items.BLAZE_ROD) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> client.setScreen(new TuningScreen(Text.empty(), hitResult.getBlockPos())));
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // knowing a BlockTuner server
        ClientPlayNetworking.registerGlobalReceiver(ProtocolCheckS2CPacket.ID, ProtocolCheckS2CPacket::receive);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BlockTunerConfig.onBlockTunerServer = false);
    }

    public static boolean isControlDown() {
        return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
}
