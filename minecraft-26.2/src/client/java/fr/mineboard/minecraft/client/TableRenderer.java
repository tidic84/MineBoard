package fr.mineboard.minecraft.client;

import fr.mineboard.core.Layouts;
import fr.mineboard.core.PieceMotion;
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
    private static final ItemStack[] MODELS = new ItemStack[68];
    private final ItemModelResolver models;
    public TableRenderer(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static ItemStack card(int id) {
        return model(Layouts.handItemModel(id));
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
            case "wonder" -> 66;
            case "coin" -> 67;
            default -> named(name);
        };
    }
    private static int named(String name) {
        if (name.startsWith("card_")) return Integer.parseInt(name.substring(5)) + 1;
        String[] ages = { "brown", "grey", "yellow", "blue", "green", "red", "purple" };
        for (int i = 0; i < ages.length; i++) if (name.equals("ages_" + ages[i])) return 59 + i;
        String[] chess = { "pawn", "knight", "bishop", "rook", "queen", "king" };
        for (int i = 0; i < chess.length; i++) {
            if (name.equals("chess_" + chess[i] + "_light")) return 47 + i;
            if (name.equals("chess_" + chess[i] + "_dark")) return 53 + i;
        }
        return 0;
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
        double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 520_000_000.0);
        List<TableGame.Piece> previous = table.previousView == null ? List.of() : table.previousView.pieces();
        for (TableGame.Piece piece : view.pieces()) {
            TableGame.Piece pose = PieceMotion.pose(previous, view.pieces(), piece, t);
            add(state, table, piece(pose), pose.x(), pose.y(), pose.z(), pose.angle(), pose.scale());
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
