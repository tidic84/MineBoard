package fr.mineboard.fabric.client;

import fr.mineboard.core.Layouts;
import fr.mineboard.core.PieceMotion;
import fr.mineboard.core.TableGame;
import fr.mineboard.fabric.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public final class TableRenderer implements BlockEntityRenderer<TableBlockEntity> {
    private static final ItemStack[] MODELS = new ItemStack[68];
    public TableRenderer(BlockEntityRendererFactory.Context context) {}
    public static ItemStack card(int id) {
        return model(Layouts.handModelData(id));
    }
    public static ItemStack piece(TableGame.Piece piece) {
        return model(Layouts.customModelData(piece.kind(), piece.card()));
    }
    private static ItemStack model(int index) {
        if (MODELS[index] == null) {
            ItemStack stack = new ItemStack(MineBoard.CARD);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(index));
            MODELS[index] = stack;
        }
        return MODELS[index];
    }
    @Override public void render(TableBlockEntity table, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertices, int light, int overlay) {
        TableGame.View view = table.publicView();
        double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 520_000_000.0);
        java.util.List<TableGame.Piece> previous = table.previousView == null ? java.util.List.of() : table.previousView.pieces();
        for (TableGame.Piece piece : view.pieces()) {
            TableGame.Piece pose = PieceMotion.pose(previous, view.pieces(), piece, t);
            renderStack(piece(pose), pose.x(), pose.y(), pose.z(), pose.angle(), pose.scale(), matrices, vertices, light, overlay);
        }
    }
    private static void renderStack(ItemStack stack, double x, double y, double z, float angle, float scale,
                                    MatrixStack matrices, VertexConsumerProvider vertices, int light, int overlay) {
        matrices.push(); matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(scale, scale, scale);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.NONE,
            light, overlay, matrices, vertices, MinecraftClient.getInstance().world, 0);
        matrices.pop();
    }
}
