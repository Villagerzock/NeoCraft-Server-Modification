package net.villagerzock.neocraft.mixins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.PlayerAccessor;
import net.villagerzock.neocraft.Teams.ChunkManager;
import net.villagerzock.neocraft.Teams.SinglePlayerTeamManager;
import net.villagerzock.neocraft.Teams.TeamManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerAccessor {
    @Shadow public abstract Text getName();

    @Shadow @Final private PlayerInventory inventory;

    @Shadow public abstract Text getDisplayName();

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
        ChunkManager.ChangeTeam( ((PlayerEntity) (Object)this),name);
        team = name;
    }
    @Unique
    public PlayerEntity getAsPlayerEntity(){
        return ((PlayerEntity) (Object) this);
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
        return TeamManager.TEAMS.getOrDefault(team,new SinglePlayerTeamManager(getAsPlayerEntity()));
    }

    @Inject(method = "readCustomData",at = @At(value = "RETURN"))
    public void readCustomDataFromNbt(ReadView view, CallbackInfo ci){
        team =  view.getString("neocraft_team","");
        Gson gson = new Gson();
        System.out.println(view.getString("displayName",""));
        JsonElement object = gson.fromJson(view.getString("displayName",""),JsonElement.class);
        try {
            displayName = TextCodecs.CODEC.
                    decode(JsonOps.INSTANCE,object)
                    .getOrThrow()
                    .getFirst();
        }catch (IllegalStateException e){
            displayName = getName();
        }
    }
    @Inject(method = "writeCustomData",at = @At(value = "RETURN"))
    public void writeCustomDataFromNbt(WriteView view, CallbackInfo ci){
        view.putString("neocraft_team",team);
        Gson gson = new Gson();
        if (displayName == Text.empty()){
            displayName = getName();
        }
        view.putString("displayName",gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE,displayName).getOrThrow()));
    }
}
