package fr.mineboard.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class MineBoard implements ModInitializer {
    public static final String ID = "mineboard";
    public static final Block TABLE = new TableBlock(AbstractBlock.Settings.create()
        .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque());
    public static final Item TABLE_ITEM = new BlockItem(TABLE, new Item.Settings());
    public static final Item CARD = new Item(new Item.Settings().maxCount(1));
    public static BlockEntityType<TableBlockEntity> TABLE_ENTITY;
    public static Identifier id(String path) { return Identifier.of(ID, path); }
    @Override public void onInitialize() {
        Registry.register(Registries.BLOCK, id("table"), TABLE);
        Registry.register(Registries.ITEM, id("table"), TABLE_ITEM);
        Registry.register(Registries.ITEM, id("card"), CARD);
        TABLE_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("table"),
            FabricBlockEntityTypeBuilder.create(TableBlockEntity::new, TABLE).build());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(TABLE_ITEM));
        FabricNetworking.register();
    }
}
