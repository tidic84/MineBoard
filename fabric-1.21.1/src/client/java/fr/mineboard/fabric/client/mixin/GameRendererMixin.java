package fr.mineboard.fabric.client.mixin;

import fr.mineboard.fabric.client.TableCamera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void mineboard$hideVanillaHand(CallbackInfo ci) { if (TableCamera.active()) ci.cancel(); }
}
