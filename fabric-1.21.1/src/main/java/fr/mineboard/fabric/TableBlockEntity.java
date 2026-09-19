package fr.mineboard.fabric;

import com.google.gson.Gson;
import fr.mineboard.core.TableGame;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import java.util.Random;

public final class TableBlockEntity extends BlockEntity {
    public static final Gson JSON = new Gson();
    // Sessions are deliberately transient in the first prototype. No private hand is saved in chunk sync NBT.
    public final TableGame game = new TableGame(new Random());
    private TableGame.View publicView = game.view(null);
    public long visualUpdateNanos;
    public int previousTop = -1;
    public TableGame.View previousView;
    public TableBlockEntity(BlockPos pos, BlockState state) { super(MineBoard.TABLE_ENTITY, pos, state); }
    public TableGame.View publicView() { return publicView; }
    public void syncPublic() {
        publicView = game.view(null);
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("PublicView", JSON.toJson(game.view(null)));
        return nbt;
    }
    @Override public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.contains("PublicView")) {
            previousTop = publicView.topCard();
            previousView = publicView;
            TableGame.View next = JSON.fromJson(nbt.getString("PublicView"), TableGame.View.class);
            if (next.revision() != publicView.revision()) visualUpdateNanos = System.nanoTime();
            publicView = next;
        }
    }
}
