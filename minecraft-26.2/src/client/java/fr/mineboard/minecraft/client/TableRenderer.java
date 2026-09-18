package fr.mineboard.minecraft.client;

import fr.mineboard.core.TableGame;
import fr.mineboard.minecraft.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TableRenderer implements BlockEntityRenderer<TableBlockEntity, TableRenderer.State> {
    private static final ItemStack[] CARDS = new ItemStack[41];
    private final ItemModelResolver models;
    public TableRenderer(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static ItemStack card(int id) {
        int index = Math.max(0, Math.min(40, id + 1));
        if (CARDS[index] == null) {
            ItemStack stack = new ItemStack(MineBoardContent.CARD);
            stack.set(DataComponents.ITEM_MODEL, MineBoardContent.id(index == 0 ? "card" : "card_" + (index - 1)));
            CARDS[index] = stack;
        }
        return CARDS[index];
    }
    private record CardRender(ItemStackRenderState item, double x, double y, double z, float angle, float scale) {}
    public static final class State extends BlockEntityRenderState {
        private final List<CardRender> cards = new ArrayList<>();
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TableBlockEntity table, State state, float delta, Vec3 camera,
                                             ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(table, state, delta, camera, breaking);
        state.cards.clear();
        TableGame.View view = table.publicView();
        for (int i = 0; i < 5; i++) add(state, table, -1, .32, .16 + i * .008, .5, 0, .46f);
        if (view.topCard() >= 0) {
            double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 550_000_000.0);
            boolean animated = view.event().equals("played") && t < 1;
            if (animated && table.previousTop >= 0) add(state, table, table.previousTop, .67, .165, .5, 8, .46f);
            double ease = 1 - Math.pow(1 - t, 3);
            double z = animated ? (view.lastActor() == 0 ? .93 : .07) * (1 - ease) + .5 * ease : .5;
            double x = animated ? .5 * (1 - ease) + .67 * ease : .67;
            double y = .18 + (animated ? Math.sin(t * Math.PI) * .2 : 0);
            add(state, table, view.topCard(), x, y, z, 8, .46f);
        }
        for (int seat = 0; seat < 2; seat++) {
            int count = Math.min(7, view.seats().get(seat).count());
            for (int i = 0; i < count; i++) add(state, table, -1, .5 + (i - (count - 1) / 2.0) * .06,
                .16 + i * .002, seat == 0 ? .87 : .13, (i - (count - 1) / 2f) * 5 + seat * 180,
                .27f);
        }
    }
    private void add(State state, TableBlockEntity table, int id, double x, double y, double z, float angle, float scale) {
        ItemStackRenderState item = new ItemStackRenderState();
        models.updateForTopItem(item, card(id), ItemDisplayContext.NONE, table.getLevel(), null, 0);
        state.cards.add(new CardRender(item, x, y, z, angle, scale));
    }
    @Override public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        for (CardRender card : state.cards) {
            matrices.pushPose();
            matrices.translate(card.x, card.y, card.z);
            matrices.mulPose(Axis.YP.rotationDegrees(card.angle));
            matrices.scale(card.scale, card.scale, card.scale);
            card.item.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }
}
