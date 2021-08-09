package eu.pb4.buildbattle.mixin;

import eu.pb4.buildbattle.other.MarkedPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({EntityEquipmentUpdateS2CPacket.class})
public class PacketMixin implements MarkedPacket {
    @Unique boolean bbIsMarked = false;

    @Override
    public boolean bb_isMarked() {
        return this.bbIsMarked;
    }

    @Override
    public void bb_mark() {
        this.bbIsMarked = true;
    }
}
