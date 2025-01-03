package net.villagerzock.neocraft.mixins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.PlayerAccessor;
import net.villagerzock.neocraft.Teams.TeamManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerAccessor {
    @Shadow public abstract Text getName();

    public String team = "";
    public Text displayName = Text.empty();
    @Override
    public String getTeam() {
        return this.team;
    }

    @Override
    public void setTeam(String name) {
        TeamManager newTeam = TeamManager.get(name);
        TeamManager oldTeam = TeamManager.get(team);
        newTeam.join();
        oldTeam.leave();
        team = name;
    }

    @Override
    public void setDisplayName(Text text) {
        this.displayName = text;
    }

    @Override
    public boolean isInATeam() {
        return TeamManager.TEAMS.containsKey(team);
    }

    @Override
    public TeamManager getTeamManager() {
        return TeamManager.TEAMS.get(team);
    }

    @Inject(method = "readCustomDataFromNbt",at = @At(value = "RETURN"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci){
        team =  nbt.getString("neocraft_team");
        Gson gson = new Gson();
        System.out.println(nbt.getString("displayName"));
        JsonElement object = gson.fromJson(nbt.getString("displayName"),JsonElement.class);
        try {
            displayName = TextCodecs.CODEC.
                    decode(JsonOps.INSTANCE,object)
                    .getOrThrow()
                    .getFirst();
        }catch (IllegalStateException e){
            displayName = getName();
        }
    }
    @Inject(method = "writeCustomDataToNbt",at = @At(value = "RETURN"))
    public void writeCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci){
        nbt.putString("neocraft_team",team);
        Gson gson = new Gson();
        if (displayName == Text.empty()){
            displayName = getName();
        }
        nbt.putString("displayName",gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE,displayName).getOrThrow()));
    }
    @Inject(method = "getDisplayName",cancellable = true,at = @At(value = "RETURN"))
    public void getDisplayName(CallbackInfoReturnable<Text> cir){
        Text baseValue = cir.getReturnValue();
        Text result = displayName;
        if (result == Text.empty()){
            cir.setReturnValue(baseValue);
        }else {
            cir.setReturnValue(result);
        }
    }
}
