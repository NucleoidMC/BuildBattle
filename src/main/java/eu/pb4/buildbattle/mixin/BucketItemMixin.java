package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
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
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.game.manager.ManagedGameSpace;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Inject(method = "placeFluid", at = @At("HEAD"), cancellable = true)
    private void disallowPlacingFluids(PlayerEntity player, World world, BlockPos pos, BlockHitResult blockHitResult, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        ManagedGameSpace gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && gameSpace.getBehavior().testRule(BuildBattle.CREATIVE_LIMIT) != ActionResult.PASS) {
            try (var invokers = Stimuli.select().forEntityAt(player, pos)) {
                var result = invokers.get(BuildBattle.ON_BUCKET_USAGE).onUse((ServerPlayerEntity) player, pos);

                if (result == ActionResult.FAIL) {
                    cir.setReturnValue(false);
                    cir.cancel();
                }
            }
        }
    }
}
