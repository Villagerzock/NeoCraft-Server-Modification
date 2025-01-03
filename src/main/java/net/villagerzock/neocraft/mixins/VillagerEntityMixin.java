package net.villagerzock.neocraft.mixins;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.villagerzock.neocraft.PlayerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "tick",cancellable = true,at = @At("HEAD"))
    public void tick(CallbackInfo ci){
        boolean cancel = true;
        for (PlayerEntity player : this.getWorld().getPlayers()){
            if (this.distanceTo(player) <= 16f) {
                cancel = false;
                break;
            }
        }
        if (cancel)
            ci.cancel();
    }
}
