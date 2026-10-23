package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;

public class OpenTuningGuiPacket {
    public final String packId;

    public OpenTuningGuiPacket(String packId) {
        this.packId = packId != null ? packId : "blue_sedan";
    }

    public OpenTuningGuiPacket(FriendlyByteBuf buf) {
        this.packId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.packId);
    }
}
