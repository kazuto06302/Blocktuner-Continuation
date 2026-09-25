package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.BlockTunerClient;
import net.kztmc.mc.blocktuner.TuningScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(NoteBlock.class)
public class NoteBlockMixinClient extends Block {

    public NoteBlockMixinClient(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getPickStack(world, pos, state, includeData);
        if (BlockTunerClient.isControlDown()) {
            copyBlockState(state, stack);
        }
        return stack;
    }

    @Unique
    private static void copyBlockState(BlockState state, ItemStack stack) {
        stack.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(NoteBlock.NOTE, state));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (placer!= null && client != null && placer == client.player && BlockTunerClient.isControlDown()) {
            client.execute(() -> client.setScreen(new TuningScreen(Text.empty(), pos)));
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }
}
