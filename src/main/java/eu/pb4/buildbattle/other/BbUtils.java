package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.mixin.BucketItemAccessor;
import eu.pb4.buildbattle.mixin.CreativeActionPacketAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BbUtils {
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BuildBattle.ID, path);
    }

    public static MutableComponent getText(String type, String path, Object... values) {
        return Component.translatable(Util.makeDescriptionId(type, Identifier.fromNamespaceAndPath(BuildBattle.ID, path)), values);
    }

    public static boolean equalsOrInstance(Object tested, Object... objects) {
        boolean result;
        for (Object object : objects) {
            if (object instanceof Class<?> clazz) {
                result = clazz.isInstance(tested);
            } else {
                result = tested.equals(object);
            }

            if (result) {
                return true;
            }
        }
        return false;
    }


    public static void setCreativeStack(ServerboundSetCreativeModeSlotPacket packet, ItemStack stack) {
        ((CreativeActionPacketAccessor) (Object) packet).bb_setStack(stack);
    }

    public static BlockState getStateFrom(ServerPlayer player, ItemStack stack) {
        var item = stack.getItem();
        if (stack.isEmpty()) {
            return null;
        } else if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock().getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, (BlockHitResult) player.pick(8, 0, false)));
        } else if (item instanceof BucketItem bucketItem) {
            return ((BucketItemAccessor) bucketItem).getContent().defaultFluidState().createLegacyBlock();
        } else {
            return null;
        }
    }
}
