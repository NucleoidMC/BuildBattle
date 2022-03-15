package eu.pb4.buildbattle.custom;

import eu.pb4.buildbattle.mixin.ItemUsageContextAccessor;
import eu.pb4.polymer.api.item.PolymerItem;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class WrappedItem extends Item implements PolymerItem {
    public WrappedItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getStack().hasNbt()) {
            ItemStack stack = BBItems.WRAPPED_ITEMS.get(context.getStack().getNbt().getString("wrapped"));
            if (stack != null) {
                var ctx = new ItemPlacementContext(context.getPlayer(), context.getHand(), stack.copy(), ((ItemUsageContextAccessor) context).bb_getHitResult());
                ActionResult actionResult = stack.getItem().useOnBlock(ctx);
                return actionResult;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.BARRIER;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        if (itemStack.hasNbt()) {
            ItemStack stack = BBItems.WRAPPED_ITEMS.get(itemStack.getNbt().getString("wrapped"));
            if (stack != null) {
                ItemStack out = PolymerItemUtils.getPolymerItemStack(stack, player);

                if (itemStack.hasNbt()) {
                    out.getOrCreateNbt().put(PolymerItemUtils.REAL_TAG, itemStack.getNbt());
                }

                out.setCount(itemStack.getCount());
                out.getOrCreateNbt().putString(PolymerItemUtils.POLYMER_ITEM_ID, Registry.ITEM.getId(itemStack.getItem()).toString());

                return out;
            }
        }
        return  PolymerItemUtils.createItemStack(itemStack, player);
    }
    
    public static final ItemStack createWrapped(String id) {
        var stack = new ItemStack(BBItems.WRAPPED);
        stack.getOrCreateNbt().putString("wrapped", id);
        return stack;
    }
}
