package eu.pb4.buildbattle.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundSetCreativeModeSlotPacket.class)
public interface CreativeActionPacketAccessor {
    @Mutable
    @Accessor("itemStack")
    void bb_setStack(ItemStack stack);
}
