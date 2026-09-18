package fr.mineboard.minecraft.client.mixin;

import fr.mineboard.minecraft.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void mineboard$camera(float tickDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !client.player.isAlive()) { TableCamera.reset(); return; }
        TableScreen screen = client.gui.screen() instanceof TableScreen table ? table : null;
        TableCamera.Pose pose = TableCamera.frame((Camera) (Object) this, screen);
        if (pose != null) {
            setPosition(pose.position().x, pose.position().y, pose.position().z);
            setRotation(pose.yaw(), pose.pitch());
        }
    }
}

