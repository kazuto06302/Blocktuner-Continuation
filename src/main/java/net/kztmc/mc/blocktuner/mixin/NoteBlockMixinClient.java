package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.BlockTunerClient;
import net.kztmc.mc.blocktuner.TuningScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(NoteBlock.class)
public class NoteBlockMixinClient extends Block {

    public NoteBlockMixinClient(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(world, pos, state, includeData);
        if (BlockTunerClient.isControlDown()) {
            copyBlockState(state, stack);
        }
        return stack;
    }

    @Unique
    private static void copyBlockState(BlockState state, ItemStack stack) {
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(NoteBlock.NOTE, state.getValue(NoteBlock.NOTE)));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        Minecraft client = Minecraft.getInstance();
        if (placer != null && client != null && placer == client.player && BlockTunerClient.isControlDown()) {
            client.execute(() -> client.gui.setScreen(new TuningScreen(Component.empty(), pos)));
        }
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }
}
