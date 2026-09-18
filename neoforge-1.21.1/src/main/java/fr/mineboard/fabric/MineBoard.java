package fr.mineboard.fabric;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
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
    public static Block TABLE;
    public static Item TABLE_ITEM;
    public static Item CARD;
    public static BlockEntityType<TableBlockEntity> TABLE_ENTITY;
    // Client entrypoint fills this without forcing client classes to load on a dedicated server.
    public static Consumer<TableNetworking.State> clientReceiver = packet -> {};
    public static Identifier id(String path) { return Identifier.of(ID, path); }

    public MineBoard(IEventBus modBus) {
        modBus.addListener(this::register);
        modBus.addListener(this::creative);
        modBus.addListener(this::payloads);
        TableNetworking.setSender(PacketDistributor::sendToPlayer);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> TableNetworking.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> TableNetworking.stop());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayerEntity player) TableNetworking.disconnect(player);
        });
    }
    private void register(RegisterEvent event) {
        event.register(RegistryKeys.BLOCK, helper -> {
            TABLE = new TableBlock(AbstractBlock.Settings.create().strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque());
            helper.register(id("table"), TABLE);
        });
        event.register(RegistryKeys.ITEM, helper -> {
            TABLE_ITEM = new BlockItem(TABLE, new Item.Settings());
            CARD = new Item(new Item.Settings().maxCount(1));
            helper.register(id("table"), TABLE_ITEM);
            helper.register(id("card"), CARD);
        });
        event.register(RegistryKeys.BLOCK_ENTITY_TYPE, helper -> {
            TABLE_ENTITY = BlockEntityType.Builder.create(TableBlockEntity::new, TABLE).build(null);
            helper.register(id("table"), TABLE_ENTITY);
        });
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ItemGroups.FUNCTIONAL)) event.add(TABLE_ITEM);
    }
    private void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(TableNetworking.Action.ID, TableNetworking.Action.CODEC,
            (packet, context) -> TableNetworking.handle((ServerPlayerEntity) context.player(), packet));
        registrar.playToClient(TableNetworking.State.ID, TableNetworking.State.CODEC,
            (packet, context) -> clientReceiver.accept(packet));
    }
}
