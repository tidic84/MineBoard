package fr.mineboard.minecraft.client.mixin;
import fr.mineboard.minecraft.client.TableCamera;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
abstract class HandMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void mineboard$hide(CallbackInfo ci) { if (TableCamera.active()) ci.cancel(); }
}
