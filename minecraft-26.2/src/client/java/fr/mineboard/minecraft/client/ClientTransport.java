package fr.mineboard.minecraft.client;

import fr.mineboard.core.TableGame;
import fr.mineboard.minecraft.*;
import net.minecraft.client.Minecraft;
import java.util.Objects;
import java.util.function.Consumer;

/** Configured by the loader's client entrypoint. */
public final class ClientTransport {
    private static Consumer<TableNetworking.Action> sender;
    public static void setSender(Consumer<TableNetworking.Action> transport) { sender = Objects.requireNonNull(transport); }
    public static void send(TableNetworking.Action action) { sender.accept(action); }
    public static void receive(TableNetworking.State packet) {
        Minecraft client = Minecraft.getInstance();
        if (packet.closed()) {
            if (client.gui.screen() instanceof TableScreen screen && screen.pos().equals(packet.pos())) screen.closeFromServer();
            return;
        }
        TableGame.View view = TableBlockEntity.JSON.fromJson(packet.view(), TableGame.View.class);
        if (packet.open()) {
            if (client.gui.screen() instanceof TableScreen existing && existing.pos().equals(packet.pos())) existing.update(view, packet.message());
            else client.gui.setScreen(new TableScreen(packet.pos(), view));
        } else if (client.gui.screen() instanceof TableScreen screen && screen.pos().equals(packet.pos())) {
            screen.update(view, packet.message());
        }
    }
}

