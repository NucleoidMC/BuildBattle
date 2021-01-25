package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.event.BBPlayerFluidPlaceListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.plasmid.game.ManagedGameSpace;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Inject(method = "placeFluid", at = @At("HEAD"), cancellable = true)
    private void disallowPlacingFluids(PlayerEntity player, World world, BlockPos pos, BlockHitResult blockHitResult, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        ManagedGameSpace gameSpace = ManagedGameSpace.forWorld(player.world);
        if (gameSpace != null) {
            try {
                BBPlayerFluidPlaceListener invoker = gameSpace.invoker(BBPlayerFluidPlaceListener.EVENT);
                ActionResult result = invoker.onPlace(((ServerPlayerEntity) player), pos, blockHitResult);
                if (result == ActionResult.FAIL) {
                    cir.setReturnValue(false);
                }
            } catch (Throwable t) {
                BuildBattle.LOGGER.error("An unexpected exception occurred while dispatching fluid place event", t);
                gameSpace.reportError(t, "Placing fluid block");
            }
        }
    }
}
