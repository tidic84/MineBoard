package fr.mineboard.minecraft;

import fr.mineboard.core.TableGame;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.function.BiConsumer;

public final class TableNetworking {
    public record Action(BlockPos pos, String action, int card, long revision) implements CustomPacketPayload {
        public static final Type<Action> ID = new Type<>(MineBoardContent.id("action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.ofMember(
            (value, buf) -> { buf.writeBlockPos(value.pos); buf.writeUtf(value.action, 16); buf.writeVarInt(value.card); buf.writeLong(value.revision); },
            buf -> new Action(buf.readBlockPos(), buf.readUtf(16), buf.readVarInt(), buf.readLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
    public record State(BlockPos pos, String view, String message, boolean open, boolean closed) implements CustomPacketPayload {
        public static final Type<State> ID = new Type<>(MineBoardContent.id("state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.ofMember(
            (value, buf) -> { buf.writeBlockPos(value.pos); buf.writeUtf(value.view, 16384); buf.writeUtf(value.message, 128); buf.writeBoolean(value.open); buf.writeBoolean(value.closed); },
            buf -> new State(buf.readBlockPos(), buf.readUtf(16384), buf.readUtf(128), buf.readBoolean(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
    private static final Map<UUID, TableBlockEntity> SESSIONS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_ACTION = new HashMap<>();
    private static BiConsumer<ServerPlayer, State> sender;

    public static void setSender(BiConsumer<ServerPlayer, State> transport) { sender = Objects.requireNonNull(transport); }
    public static void disconnect(ServerPlayer player) { close(player, false); }
    public static void stop() { SESSIONS.clear(); LAST_ACTION.clear(); }
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        for (UUID id : List.copyOf(SESSIONS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            TableBlockEntity table = SESSIONS.get(id);
            if (player != null && !valid(player, table)) close(player, true);
        }
    }
    private static boolean valid(ServerPlayer player, TableBlockEntity table) {
        return table != null && !table.isRemoved() && player.isAlive() && !player.isSpectator()
            && player.level() == table.getLevel()
            && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(table.getBlockPos())) <= 64;
    }
    public static void open(ServerPlayer player, TableBlockEntity table) {
        if (!valid(player, table)) return;
        if (SESSIONS.get(player.getUUID()) != table) {
            if (SESSIONS.values().stream().filter(t -> t == table).count() >= 16) {
                player.sendSystemMessage(Component.translatable("mineboard.error.too_many_viewers"), true); return;
            }
            close(player, false);
            SESSIONS.put(player.getUUID(), table);
        }
        send(player, table, "", true, false);
    }
    public static void handle(ServerPlayer player, Action packet) {
        TableBlockEntity table = SESSIONS.get(player.getUUID());
        if (table == null || !table.getBlockPos().equals(packet.pos)) return;
        if (packet.action.equals("leave")) { close(player, false); return; }
        if (!valid(player, table)) { close(player, true); return; }
        int now = player.level().getServer().getTickCount();
        if (LAST_ACTION.getOrDefault(player.getUUID(), -100) == now) return;
        LAST_ACTION.put(player.getUUID(), now);
        TableGame game = table.game;
        String error = switch (packet.action) {
            case "join" -> game.join(player.getUUID(), player.getGameProfile().name());
            case "ready" -> game.ready(player.getUUID());
            case "start" -> game.start(player.getUUID());
            case "play" -> game.play(player.getUUID(), packet.card, packet.revision);
            case "draw" -> game.draw(player.getUUID(), packet.revision);
            case "rematch" -> game.rematch(player.getUUID());
            default -> "invalid_action";
        };
        if (error.isEmpty()) broadcast(table);
        else send(player, table, error, false, false);
    }
    private static void close(ServerPlayer player, boolean notify) {
        TableBlockEntity table = SESSIONS.remove(player.getUUID());
        LAST_ACTION.remove(player.getUUID());
        if (table == null) return;
        table.game.leave(player.getUUID());
        if (notify) send(player, table, "closed", false, true);
        broadcast(table);
    }
    private static void broadcast(TableBlockEntity table) {
        if (table.getLevel() == null || table.getLevel().getServer() == null) return;
        table.syncPublic();
        SESSIONS.forEach((id, current) -> {
            if (current == table) {
                ServerPlayer player = table.getLevel().getServer().getPlayerList().getPlayer(id);
                if (player != null) send(player, table, "", false, false);
            }
        });
    }
    private static void send(ServerPlayer player, TableBlockEntity table, String message, boolean open, boolean closed) {
        sender.accept(player, new State(table.getBlockPos(),
            TableBlockEntity.JSON.toJson(table.game.view(player.getUUID())), message, open, closed));
    }
}

