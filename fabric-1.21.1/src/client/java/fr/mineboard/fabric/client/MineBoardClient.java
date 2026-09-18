package fr.mineboard.fabric.client;

import fr.mineboard.fabric.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class MineBoardClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRendererFactories.register(MineBoard.TABLE_ENTITY, TableRenderer::new);
        ClientTransport.setSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(TableNetworking.State.ID,
            (packet, context) -> context.client().execute(() -> ClientTransport.receive(packet)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TableCamera.reset());
    }
}
