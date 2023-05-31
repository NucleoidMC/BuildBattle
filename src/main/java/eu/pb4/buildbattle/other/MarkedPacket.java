package eu.pb4.buildbattle.other;


import net.minecraft.network.packet.Packet;

public interface MarkedPacket {
    boolean bb_isMarked();

    void bb_mark();


    static boolean is(Packet<?> packet) {
        if (packet instanceof MarkedPacket markedPacket) {
            return markedPacket.bb_isMarked();
        }
        return true;
    }

    static <T extends Packet<?>> T mark(T packet) {
        if (packet instanceof MarkedPacket markedPacket) {
            markedPacket.bb_mark();
        }
        return packet;
    }
}
