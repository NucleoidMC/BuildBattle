package eu.pb4.buildbattle.event;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.event.EventType;

public interface BBPlayerFluidPlaceListener {
    EventType<BBPlayerFluidPlaceListener> EVENT = EventType.create(BBPlayerFluidPlaceListener.class, listeners -> (player, pos, blockHitResult) -> {
        for (BBPlayerFluidPlaceListener listener : listeners) {
            ActionResult result = listener.onPlace(player, pos, blockHitResult);

            if (result != ActionResult.PASS) {
                return result;
            }
        }

        return ActionResult.PASS;
    });

    ActionResult onPlace(ServerPlayerEntity player, BlockPos pos, BlockHitResult blockHitResult);
}