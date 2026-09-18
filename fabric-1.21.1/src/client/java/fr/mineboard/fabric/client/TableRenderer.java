package fr.mineboard.fabric.client;

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
    private static final ItemStack[] CARDS = new ItemStack[41];
    public TableRenderer(BlockEntityRendererFactory.Context context) {}
    public static ItemStack card(int id) {
        int index = Math.max(0, Math.min(40, id + 1));
        if (CARDS[index] == null) {
            ItemStack stack = new ItemStack(MineBoard.CARD);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(index));
            CARDS[index] = stack;
        }
        return CARDS[index];
    }
    @Override public void render(TableBlockEntity table, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertices, int light, int overlay) {
        TableGame.View view = table.publicView();
        for (int i = 0; i < 5; i++) renderCard(-1, .32, .16 + i * .008, .5, 0, .46f, matrices, vertices, light, overlay);
        if (view.topCard() >= 0) {
            double t = Math.min(1, (System.nanoTime() - table.visualUpdateNanos) / 550_000_000.0);
            boolean animated = view.event().equals("played") && t < 1;
            if (animated && table.previousTop >= 0) renderCard(table.previousTop, .67, .165, .5, 8, .46f, matrices, vertices, light, overlay);
            double ease = 1 - Math.pow(1 - t, 3);
            double z = animated ? (view.lastActor() == 0 ? .93 : .07) * (1 - ease) + .5 * ease : .5;
            double x = animated ? .5 * (1 - ease) + .67 * ease : .67;
            double y = .18 + (animated ? Math.sin(t * Math.PI) * .2 : 0);
            renderCard(view.topCard(), x, y, z, 8, .46f, matrices, vertices, light, overlay);
        }
        for (int seat = 0; seat < 2; seat++) {
            int count = Math.min(7, view.seats().get(seat).count());
            for (int i = 0; i < count; i++) renderCard(-1, .5 + (i - (count - 1) / 2.0) * .06,
                .16 + i * .002, seat == 0 ? .87 : .13, (i - (count - 1) / 2f) * 5 + seat * 180,
                .27f, matrices, vertices, light, overlay);
        }
    }
    private static void renderCard(int id, double x, double y, double z, float angle, float scale,
                                   MatrixStack matrices, VertexConsumerProvider vertices, int light, int overlay) {
        matrices.push(); matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(scale, scale, scale);
        MinecraftClient.getInstance().getItemRenderer().renderItem(card(id), ModelTransformationMode.NONE,
            light, overlay, matrices, vertices, MinecraftClient.getInstance().world, 0);
        matrices.pop();
    }
}
