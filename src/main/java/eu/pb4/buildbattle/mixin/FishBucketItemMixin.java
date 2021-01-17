package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.plasmid.game.ManagedGameSpace;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

@Mixin(FishBucketItem.class)
public class FishBucketItemMixin {

    @Inject(method = "onEmptied", at = @At("HEAD"), cancellable = true)
    private void onEmptied(World world, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        ManagedGameSpace gameSpace = ManagedGameSpace.forWorld(world);
        if (gameSpace != null && gameSpace.testRule(BuildBattle.CREATIVE_LIMIT) == RuleResult.ALLOW) {
            ci.cancel();
        }
    }

}
