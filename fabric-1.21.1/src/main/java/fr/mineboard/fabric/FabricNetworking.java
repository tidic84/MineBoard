package fr.mineboard.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;

/** Fabric hooks only; session rules and packet definitions remain loader-independent. */
final class FabricNetworking {
    static void register() {
        PayloadTypeRegistry.playC2S().register(TableNetworking.Action.ID, TableNetworking.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(TableNetworking.State.ID, TableNetworking.State.CODEC);
        TableNetworking.setSender(ServerPlayNetworking::send);
        ServerPlayNetworking.registerGlobalReceiver(TableNetworking.Action.ID, (packet, context) ->
            context.server().execute(() -> TableNetworking.handle(context.player(), packet)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TableNetworking.disconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TableNetworking.stop());
        ServerTickEvents.END_SERVER_TICK.register(TableNetworking::tick);
    }
}
