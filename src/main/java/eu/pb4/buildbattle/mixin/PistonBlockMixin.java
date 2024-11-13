package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(World world, BlockPos pos, Direction dir, boolean retract, CallbackInfoReturnable<Boolean> cir) {
        var gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && gameSpace.getBehavior().testRule(BuildBattle.CREATIVE_LIMIT) != EventResult.PASS) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
