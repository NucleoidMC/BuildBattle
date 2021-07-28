package eu.pb4.buildbattle.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeInventoryActionC2SPacket.class)
public interface CreativeActionPacketAccessor {
    @Mutable
    @Accessor("stack")
    void bb_setStack(ItemStack stack);
}
