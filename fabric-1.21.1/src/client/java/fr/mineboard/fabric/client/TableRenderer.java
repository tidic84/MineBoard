package fr.mineboard.fabric.client;

import fr.mineboard.core.Layouts;
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
    private static final ItemStack[] MODELS = new ItemStack[47];
    public TableRenderer(BlockEntityRendererFactory.Context context) {}
    public static ItemStack card(int id) {
        return model(Math.max(0, Math.min(40, id + 1)));
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
        double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 550_000_000.0);
        boolean animated = view.event().equals("played") && t < 1 && view.topCard() >= 0;
        for (TableGame.Piece piece : view.pieces()) {
            if (animated && "discard".equals(piece.kind())) continue;
            renderStack(piece(piece), piece.x(), piece.y(), piece.z(), piece.angle(), piece.scale(), matrices, vertices, light, overlay);
        }
        if (animated) {
            if (table.previousTop >= 0) renderStack(card(table.previousTop), .67, .165, .5, 8, .46f, matrices, vertices, light, overlay);
            double ease = 1 - Math.pow(1 - t, 3);
            double z = (view.lastActor() == 0 ? .93 : .07) * (1 - ease) + .5 * ease;
            double x = .5 * (1 - ease) + .67 * ease;
            double y = .18 + Math.sin(t * Math.PI) * .2;
            renderStack(card(view.topCard()), x, y, z, 8, .46f, matrices, vertices, light, overlay);
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
