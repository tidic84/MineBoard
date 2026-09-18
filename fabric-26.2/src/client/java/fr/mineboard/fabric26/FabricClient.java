package fr.mineboard.fabric26;

import fr.mineboard.minecraft.*;
import fr.mineboard.minecraft.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class FabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRenderers.register(MineBoardContent.TABLE_ENTITY, TableRenderer::new);
        ClientTransport.setSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(TableNetworking.State.ID,
            (packet, context) -> context.client().execute(() -> ClientTransport.receive(packet)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TableCamera.reset());
    }
}
