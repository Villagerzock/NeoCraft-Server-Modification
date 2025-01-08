package net.villagerzock.neocraft.mixins;

import com.mojang.serialization.Dynamic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.villagerzock.neocraft.PlayerAccessor;
import net.villagerzock.neocraft.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    private int tick = 0;

    @Shadow protected abstract Brain<?> deserializeBrain(Dynamic<?> dynamic);

    @Shadow public abstract boolean canGather(ServerWorld world, ItemStack stack);

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "tick",cancellable = true,at = @At("HEAD"))
    public void tick(CallbackInfo ci){
        boolean cancel = true;
        for (PlayerEntity player : this.getWorld().getPlayers()){
            if (performanceDistanceTo(player,Config.VILLAGER_DISTANCE.get())) {
                cancel = false;
                break;
            }
        }
        if (cancel){
            if (Math.floorMod(tick,80) != 0){
                ci.cancel();
            }
        }else {
            if (Math.floorMod(tick,2) != 0){
                ci.cancel();
            }
        }
    }
    @Unique
    public boolean performanceDistanceTo(Entity entity, float dist){
        float f = (float)(this.getX() - entity.getX());
        float g = (float)(this.getY() - entity.getY());
        float h = (float)(this.getZ() - entity.getZ());
        float z = f*f+g*g+h*h;
        return z <= (dist * dist);
    }
}
