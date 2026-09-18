package fr.mineboard.neoforge26;
import fr.mineboard.minecraft.*;
import static fr.mineboard.minecraft.MineBoardContent.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.util.function.Consumer;

/** NeoForge bootstrap. The historical Java package is shared with the Fabric implementation. */
@Mod(MineBoard.ID)
public final class MineBoard {
    public static final String ID = "mineboard";
    // Client entrypoint fills this without forcing client classes to load on a dedicated server.
    public static Consumer<TableNetworking.State> clientReceiver = packet -> {};

    public MineBoard(IEventBus modBus) {
        modBus.addListener(this::register);
        modBus.addListener(this::creative);
        modBus.addListener(this::payloads);
        TableNetworking.setSender(PacketDistributor::sendToPlayer);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> TableNetworking.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> TableNetworking.stop());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) TableNetworking.disconnect(player);
        });
    }
    private void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            createBlocks();
            helper.register(id("table"), TABLE);
        });
        event.register(Registries.ITEM, helper -> {
            createItems();
            
            helper.register(id("table"), TABLE_ITEM);
            helper.register(id("card"), CARD);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            createBlockEntities();
            helper.register(id("table"), TABLE_ENTITY);
        });
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(TABLE_ITEM);
    }
    private void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(TableNetworking.Action.ID, TableNetworking.Action.CODEC,
            (packet, context) -> TableNetworking.handle((ServerPlayer) context.player(), packet));
        registrar.playToClient(TableNetworking.State.ID, TableNetworking.State.CODEC,
            (packet, context) -> clientReceiver.accept(packet));
    }
}

