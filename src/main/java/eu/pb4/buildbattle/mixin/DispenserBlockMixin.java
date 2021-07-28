package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.game.manager.ManagedGameSpace;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    private void dispense(ServerWorld world, BlockPos pos, CallbackInfo ci) {
        ManagedGameSpace gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && gameSpace.getBehavior().testRule(BuildBattle.CREATIVE_LIMIT) != ActionResult.PASS) {
            ci.cancel();
        }
    }
}
