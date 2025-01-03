package net.villagerzock.neocraft.mixins;

import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringHelper.class)
public class StringHelperMixin {
    @Inject(method = "isValidChar",at = @At(value = "HEAD"),cancellable = true)
    private static void isValidChar(char c, CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(true);
    }
}