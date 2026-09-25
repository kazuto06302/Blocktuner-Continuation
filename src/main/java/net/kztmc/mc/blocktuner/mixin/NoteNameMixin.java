package net.kztmc.mc.blocktuner.mixin;

import net.kztmc.mc.blocktuner.NoteNames;
import net.minecraft.block.NoteBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class NoteNameMixin {

    @Unique
    private static final Style NOTE_STYLE = Style.EMPTY.withColor(Formatting.AQUA);

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void getNoteName(CallbackInfoReturnable<Text> cir){
        ItemStack itemStack = (ItemStack)(Object)this;
        if (itemStack.getItem() == Items.NOTE_BLOCK && itemStack.get(DataComponentTypes.BLOCK_STATE) != null) {
            int note = itemStack.get(DataComponentTypes.BLOCK_STATE).getValue(NoteBlock.NOTE);
            cir.setReturnValue(MutableText.of(new TranslatableTextContent(itemStack.getItem().getTranslationKey(), null, null))
                    .append(MutableText.of(new PlainTextContent.Literal(" (" + NoteNames.get(note) + ", "+ note + ")")).setStyle(NOTE_STYLE)));
        }
    }
}
