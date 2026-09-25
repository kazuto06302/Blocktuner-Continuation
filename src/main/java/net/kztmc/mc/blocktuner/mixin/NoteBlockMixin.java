package net.kztmc.mc.blocktuner.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
public class NoteBlockMixin extends Block {

    public NoteBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "onUse",
            cancellable = true,
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/block/BlockState;cycle(Lnet/minecraft/state/property/Property;)Ljava/lang/Object;"))
    private void onTune(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir){

        // allows playing with right clicks while holding blaze rods
        if (player.getMainHandStack().getItem() == Items.BLAZE_ROD) {

            if (world.getBlockState(pos.up()).isAir()) {
                world.addSyncedBlockEvent(pos, (NoteBlock) (Object) this, 0, 0);
            }
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }
}
