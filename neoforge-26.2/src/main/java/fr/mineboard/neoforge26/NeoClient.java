package fr.mineboard.neoforge26;

import fr.mineboard.minecraft.MineBoardContent;
import fr.mineboard.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@Mod(value = MineBoard.ID, dist = Dist.CLIENT)
public final class NeoClient {
    public NeoClient(IEventBus modBus) {
        ClientTransport.setSender(ClientPacketDistributor::sendToServer);
        MineBoard.clientReceiver = ClientTransport::receive;
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerBlockEntityRenderer(MineBoardContent.TABLE_ENTITY, TableRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> TableCamera.reset());
    }
}


