package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTuningGuiPacket {

    private final String packId;

    public OpenTuningGuiPacket(String packId) {
        this.packId = packId != null ? packId : "blue_sedan";
    }

    public OpenTuningGuiPacket(FriendlyByteBuf buf) {
        this.packId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.packId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.evgen.cardecor.client.ClientPacketHandlers.handleOpenTuningGui(this.packId));
        });
        ctx.get().setPacketHandled(true);
    }
}
