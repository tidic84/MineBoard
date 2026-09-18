package fr.mineboard.fabric;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.function.BiConsumer;

public final class TableNetworking {
    public record Action(BlockPos pos, String action, int card, long revision) implements CustomPayload {
        public static final Id<Action> ID = new Id<>(MineBoard.id("action"));
        public static final PacketCodec<RegistryByteBuf, Action> CODEC = PacketCodec.of(
            (value, buf) -> { buf.writeBlockPos(value.pos); buf.writeString(value.action, 16); buf.writeVarInt(value.card); buf.writeLong(value.revision); },
            buf -> new Action(buf.readBlockPos(), buf.readString(16), buf.readVarInt(), buf.readLong()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record State(BlockPos pos, String view, String message, boolean open, boolean closed) implements CustomPayload {
        public static final Id<State> ID = new Id<>(MineBoard.id("state"));
        public static final PacketCodec<RegistryByteBuf, State> CODEC = PacketCodec.of(
            (value, buf) -> { buf.writeBlockPos(value.pos); buf.writeString(value.view, 16384); buf.writeString(value.message, 128); buf.writeBoolean(value.open); buf.writeBoolean(value.closed); },
            buf -> new State(buf.readBlockPos(), buf.readString(16384), buf.readString(128), buf.readBoolean(), buf.readBoolean()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    private static final Map<UUID, TableBlockEntity> SESSIONS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_ACTION = new HashMap<>();
    private static BiConsumer<ServerPlayerEntity, State> sender;

    public static void setSender(BiConsumer<ServerPlayerEntity, State> transport) { sender = Objects.requireNonNull(transport); }
    public static void disconnect(ServerPlayerEntity player) { close(player, false); }
    public static void stop() { SESSIONS.clear(); LAST_ACTION.clear(); }
    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 10 != 0) return;
        for (UUID id : List.copyOf(SESSIONS.keySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            TableBlockEntity table = SESSIONS.get(id);
            if (player != null && !valid(player, table)) close(player, true);
        }
    }
    private static boolean valid(ServerPlayerEntity player, TableBlockEntity table) {
        return table != null && !table.isRemoved() && player.isAlive() && !player.isSpectator()
            && player.getWorld() == table.getWorld()
            && player.squaredDistanceTo(table.getPos().toCenterPos()) <= 64;
    }
    public static void open(ServerPlayerEntity player, TableBlockEntity table) {
        if (!valid(player, table)) return;
        if (SESSIONS.get(player.getUuid()) != table) {
            if (SESSIONS.values().stream().filter(t -> t == table).count() >= 16) {
                player.sendMessage(Text.translatable("mineboard.error.too_many_viewers"), true); return;
            }
            close(player, false);
            SESSIONS.put(player.getUuid(), table);
        }
        send(player, table, "", true, false);
    }
    public static void handle(ServerPlayerEntity player, Action packet) {
        TableBlockEntity table = SESSIONS.get(player.getUuid());
        if (table == null || !table.getPos().equals(packet.pos)) return;
        if (packet.action.equals("leave")) { close(player, false); return; }
        if (!valid(player, table)) { close(player, true); return; }
        int now = player.getServer().getTicks();
        if (LAST_ACTION.getOrDefault(player.getUuid(), -100) == now) return;
        LAST_ACTION.put(player.getUuid(), now);
        String error = table.game.apply(player.getUuid(), player.getGameProfile().getName(),
            packet.action, packet.card, packet.revision);
        if (error.isEmpty()) broadcast(table);
        else send(player, table, error, false, false);
    }
    private static void close(ServerPlayerEntity player, boolean notify) {
        TableBlockEntity table = SESSIONS.remove(player.getUuid());
        LAST_ACTION.remove(player.getUuid());
        if (table == null) return;
        table.game.leave(player.getUuid());
        if (notify) send(player, table, "closed", false, true);
        broadcast(table);
    }
    private static void broadcast(TableBlockEntity table) {
        if (table.getWorld() == null || table.getWorld().getServer() == null) return;
        table.syncPublic();
        SESSIONS.forEach((id, current) -> {
            if (current == table) {
                ServerPlayerEntity player = table.getWorld().getServer().getPlayerManager().getPlayer(id);
                if (player != null) send(player, table, "", false, false);
            }
        });
    }
    private static void send(ServerPlayerEntity player, TableBlockEntity table, String message, boolean open, boolean closed) {
        sender.accept(player, new State(table.getPos(),
            TableBlockEntity.JSON.toJson(table.game.view(player.getUuid())), message, open, closed));
    }
}
