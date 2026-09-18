package fr.mineboard.fabric.client.mixin;

import fr.mineboard.fabric.client.TableCamera;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class HudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mineboard$hideHud(CallbackInfo ci) { if (TableCamera.active()) ci.cancel(); }
}
