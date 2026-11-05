package client.network;

import client.enums.PacketDirection;

public final class PacketEvent {
   private final Object packet;
   private final PacketDirection direction;

   public PacketEvent(Object packet, PacketDirection direction) {
      this.packet = packet;
      this.direction = direction;
   }

   public Object getPacket() {
      return this.packet;
   }

   public PacketDirection getPacketDirection() {
      return this.direction;
   }
}