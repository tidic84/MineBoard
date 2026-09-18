package fr.mineboard.neoforge;

import fr.mineboard.fabric.MineBoard;
import fr.mineboard.fabric.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = MineBoard.ID, dist = Dist.CLIENT)
public final class NeoClient {
    public NeoClient(IEventBus modBus) {
        ClientTransport.setSender(PacketDistributor::sendToServer);
        MineBoard.clientReceiver = ClientTransport::receive;
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerBlockEntityRenderer(MineBoard.TABLE_ENTITY, TableRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> TableCamera.reset());
    }
}
