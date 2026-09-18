package fr.mineboard.minecraft.client;

import fr.mineboard.core.Layouts;
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
    private static final ItemStack[] MODELS = new ItemStack[47];
    private final ItemModelResolver models;
    public TableRenderer(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static ItemStack card(int id) {
        return model(id < 0 ? "card" : "card_" + id);
    }
    public static ItemStack piece(TableGame.Piece piece) {
        return model(Layouts.itemModel(piece.kind(), piece.card()));
    }
    private static ItemStack model(String name) {
        int index = indexOf(name);
        if (MODELS[index] == null) {
            ItemStack stack = new ItemStack(MineBoardContent.CARD);
            stack.set(DataComponents.ITEM_MODEL, MineBoardContent.id(name));
            MODELS[index] = stack;
        }
        return MODELS[index];
    }
    private static int indexOf(String name) {
        return switch (name) {
            case "card" -> 0;
            case "cell_light" -> 41;
            case "cell_dark" -> 42;
            case "token_light" -> 43;
            case "token_dark" -> 44;
            case "token_king_light" -> 45;
            case "token_king_dark" -> 46;
            default -> name.startsWith("card_") ? Integer.parseInt(name.substring(5)) + 1 : 0;
        };
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
        double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 550_000_000.0);
        boolean animated = view.event().equals("played") && t < 1 && view.topCard() >= 0;
        for (TableGame.Piece piece : view.pieces()) {
            if (animated && "discard".equals(piece.kind())) continue;
            add(state, table, piece(piece), piece.x(), piece.y(), piece.z(), piece.angle(), piece.scale());
        }
        if (animated) {
            if (table.previousTop >= 0) add(state, table, card(table.previousTop), .67, .165, .5, 8, .46f);
            double ease = 1 - Math.pow(1 - t, 3);
            double z = (view.lastActor() == 0 ? .93 : .07) * (1 - ease) + .5 * ease;
            double x = .5 * (1 - ease) + .67 * ease;
            double y = .18 + Math.sin(t * Math.PI) * .2;
            add(state, table, card(view.topCard()), x, y, z, 8, .46f);
        }
    }
    private void add(State state, TableBlockEntity table, ItemStack stack, double x, double y, double z, float angle, float scale) {
        ItemStackRenderState item = new ItemStackRenderState();
        models.updateForTopItem(item, stack, ItemDisplayContext.NONE, table.getLevel(), null, 0);
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
