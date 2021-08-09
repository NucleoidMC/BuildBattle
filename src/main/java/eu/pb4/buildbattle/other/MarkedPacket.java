package eu.pb4.buildbattle.other;

import net.minecraft.network.Packet;

public interface MarkedPacket {
    boolean bb_isMarked();

    void bb_mark();


    static boolean is(Packet<?> packet) {
        if (packet instanceof MarkedPacket markedPacket) {
            return markedPacket.bb_isMarked();
        }
        return false;
    }

    static <T extends Packet<?>> T mark(T packet) {
        ((MarkedPacket) packet).bb_mark();
        return packet;
    }
}
