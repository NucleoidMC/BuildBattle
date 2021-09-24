package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.mixin.ItemUsageContextAccessor;
import eu.pb4.polymer.item.ItemHelper;
import eu.pb4.polymer.item.VirtualItem;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class WrappedItem extends Item implements VirtualItem {
    public WrappedItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getStack().hasTag()) {
            ItemStack stack = BBItems.WRAPPED_ITEMS.get(context.getStack().getTag().getString("wrapped"));
            if (stack != null) {
                var ctx = new ItemPlacementContext(context.getPlayer(), context.getHand(), stack.copy(), ((ItemUsageContextAccessor) context).bb_getHitResult());
                ActionResult actionResult = stack.getItem().useOnBlock(ctx);
                return actionResult;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public Item getVirtualItem() {
        return Items.BARRIER;
    }

    @Override
    public ItemStack getVirtualItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        if (itemStack.hasTag()) {
            ItemStack stack = BBItems.WRAPPED_ITEMS.get(itemStack.getTag().getString("wrapped"));
            if (stack != null) {
                ItemStack out = stack.getItem() instanceof VirtualItem virtualItem
                        ? virtualItem.getVirtualItemStack(itemStack, player)
                        : stack.copy();

                if (itemStack.hasTag()) {
                    out.getOrCreateTag().put(ItemHelper.REAL_TAG, itemStack.getTag());
                }

                out.setCount(itemStack.getCount());
                out.getOrCreateTag().putString(ItemHelper.VIRTUAL_ITEM_ID, Registry.ITEM.getId(itemStack.getItem()).toString());

                return out;
            }
        }
        return VirtualItem.super.getVirtualItemStack(itemStack, player);
    }
    
    public static final ItemStack createWrapped(String id) {
        var stack = new ItemStack(BBItems.WRAPPED);
        stack.getOrCreateTag().putString("wrapped", id);
        return stack;
    }
}
