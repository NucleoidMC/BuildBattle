package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.mixin.BucketItemAccessor;
import eu.pb4.buildbattle.mixin.CreativeActionPacketAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;

public class BbUtils {
    public static Identifier id(String path) {
        return new Identifier(BuildBattle.ID, path);
    }

    public static MutableText getText(String type, String path, Object... values) {
        return Text.translatable(Util.createTranslationKey(type, new Identifier(BuildBattle.ID, path)), values);
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


    public static void setCreativeStack(CreativeInventoryActionC2SPacket packet, ItemStack stack) {
        ((CreativeActionPacketAccessor) packet).bb_setStack(stack);
    }

    public static BlockState getStateFrom(ServerPlayerEntity player, ItemStack stack) {
        var item = stack.getItem();
        if (stack.isEmpty()) {
            return null;
        } else if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock().getPlacementState(new ItemPlacementContext(player, Hand.MAIN_HAND, stack, (BlockHitResult) player.raycast(8, 0, false)));
        } else if (item instanceof BucketItem bucketItem) {
            return ((BucketItemAccessor) bucketItem).getFluid().getDefaultState().getBlockState();
        } else {
            return null;
        }
    }
}
