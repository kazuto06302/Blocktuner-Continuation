package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.NoteNames;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class NoteNameMixin {

    @Unique
    private static final Style NOTE_STYLE = Style.EMPTY.withColor(ChatFormatting.AQUA);

    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void getNoteName(CallbackInfoReturnable<Component> cir){
        ItemStack itemStack = (ItemStack)(Object)this;
        if (itemStack.is(Items.NOTE_BLOCK) && itemStack.has(DataComponents.BLOCK_STATE)) {
            BlockItemStateProperties stateProperties = itemStack.get(DataComponents.BLOCK_STATE);
            if (stateProperties != null) {
                int note = stateProperties.apply(Blocks.NOTE_BLOCK.defaultBlockState()).getValue(NoteBlock.NOTE);
                cir.setReturnValue(Component.translatable(itemStack.getItem().getDescriptionId())
                        .append(Component.literal(" (" + NoteNames.get(note) + ", " + note + ")").withStyle(NOTE_STYLE)));
            }
        }
    }
}
