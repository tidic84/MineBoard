package fr.mineboard.fabric26;

import fr.mineboard.minecraft.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

public final class FabricMineBoard implements ModInitializer {
    @Override public void onInitialize() {
        MineBoardContent.createBlocks();
        Registry.register(BuiltInRegistries.BLOCK, MineBoardContent.id("table"), MineBoardContent.TABLE);
        MineBoardContent.createItems();
        Registry.register(BuiltInRegistries.ITEM, MineBoardContent.id("table"), MineBoardContent.TABLE_ITEM);
        Registry.register(BuiltInRegistries.ITEM, MineBoardContent.id("card"), MineBoardContent.CARD);
        MineBoardContent.createBlockEntities();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MineBoardContent.id("table"), MineBoardContent.TABLE_ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(MineBoardContent.TABLE_ITEM));
        PayloadTypeRegistry.serverboundPlay().register(TableNetworking.Action.ID, TableNetworking.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TableNetworking.State.ID, TableNetworking.State.CODEC);
        TableNetworking.setSender(ServerPlayNetworking::send);
        ServerPlayNetworking.registerGlobalReceiver(TableNetworking.Action.ID, (packet, context) ->
            context.server().execute(() -> TableNetworking.handle(context.player(), packet)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TableNetworking.disconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TableNetworking.stop());
        ServerTickEvents.END_SERVER_TICK.register(TableNetworking::tick);
    }
}

