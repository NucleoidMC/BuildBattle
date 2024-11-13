package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.BuildBattle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(EntityBucketItem.class)
public class EntityBucketItemMixin {

    @Inject(method = "onEmptied", at = @At("HEAD"), cancellable = true)
    private void onEmptied(PlayerEntity player, World world, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        var gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && gameSpace.getBehavior().testRule(BuildBattle.CREATIVE_LIMIT) != EventResult.PASS) {
            ci.cancel();
        }
    }
}
