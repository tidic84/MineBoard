package fr.mineboard.fabric.client.mixin;

import fr.mineboard.fabric.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "update", at = @At("TAIL"))
    private void mineboard$camera(BlockView world, Entity entity, boolean thirdPerson, boolean inverse, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !client.player.isAlive()) { TableCamera.reset(); return; }
        TableScreen screen = client.currentScreen instanceof TableScreen table ? table : null;
        TableCamera.Pose pose = TableCamera.frame((Camera) (Object) this, screen);
        if (pose != null) {
            setPos(pose.position().x, pose.position().y, pose.position().z);
            setRotation(pose.yaw(), pose.pitch());
        }
    }
}
