package fr.mineboard.minecraft;

import com.google.gson.Gson;
import fr.mineboard.core.TableGame;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import java.util.Random;

public final class TableBlockEntity extends BlockEntity {
    public static final Gson JSON = new Gson();
    public final TableGame game = new TableGame(new Random());
    private TableGame.View publicView = game.view(null);
    public long visualUpdateNanos;
    public int previousTop = -1;
    public TableBlockEntity(BlockPos pos, BlockState state) { super(MineBoardContent.TABLE_ENTITY, pos, state); }
    public TableGame.View publicView() { return publicView; }
    public void syncPublic() {
        publicView = game.view(null);
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("PublicView", JSON.toJson(game.view(null)));
        return nbt;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("PublicView").ifPresent(json -> {
            previousTop = publicView.topCard();
            TableGame.View next = JSON.fromJson(json, TableGame.View.class);
            if (next.revision() != publicView.revision()) visualUpdateNanos = System.nanoTime();
            publicView = next;
        });
    }
}
