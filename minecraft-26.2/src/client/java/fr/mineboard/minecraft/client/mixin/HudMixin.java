package fr.mineboard.minecraft.client.mixin;
import fr.mineboard.minecraft.client.TableCamera;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void mineboard$hide(CallbackInfo ci) { if (TableCamera.active()) ci.cancel(); }
}
