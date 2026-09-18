package fr.mineboard.minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Set;

/** Construct only during the appropriate loader registration phase. */
public final class MineBoardContent {
    public static final String ID = "mineboard";
    public static Block TABLE;
    public static Item TABLE_ITEM, CARD;
    public static BlockEntityType<TableBlockEntity> TABLE_ENTITY;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    public static void createBlocks() {
        TABLE = new TableBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("table")))
            .strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    }
    public static void createItems() {
        TABLE_ITEM = new BlockItem(TABLE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("table"))).useBlockDescriptionPrefix());
        CARD = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("card"))).stacksTo(1));
    }
    public static void createBlockEntities() {
        TABLE_ENTITY = new BlockEntityType<>(TableBlockEntity::new, Set.of(TABLE));
    }
}
