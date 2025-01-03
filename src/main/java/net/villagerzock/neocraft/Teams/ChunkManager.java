package net.villagerzock.neocraft.Teams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.villagerzock.neocraft.config.Config;
import net.villagerzock.neocraft.PlayerAccessor;
import org.joml.Vector2i;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkManager {
    public static Map<Vector2i,ChunkData> chunkClaims = new HashMap<>();
    public static final File TEAMSPATH;
    public static final File TEAMSFILE;
    static {
        TEAMSPATH = new File(FabricLoader.getInstance().getGameDir().toFile(),"neocraft");
        TEAMSFILE = new File(TEAMSPATH,"chunkdata.json");
        load();
    }
    public static void staticLoad(){

    }

    public static int getAmountOfClaimedChunks(PlayerEntity player){
        AtomicInteger amount = new AtomicInteger();
        chunkClaims.forEach((vector2i, chunkData) -> {
            if (player instanceof PlayerAccessor accessor){
                if (chunkData.owningTeam == accessor.getTeam())
                    amount.getAndIncrement();
            }
        });
        return amount.get();
    }
    public static ClaimResult claim(Vector2i chunk, PlayerEntity player){
        if (player instanceof PlayerAccessor accessor){
            TeamManager manager = accessor.getTeamManager();
            if (manager.maxClaims * Config.CLAIMS_PER_PERSON.get() <= getAmountOfClaimedChunks(player))
                return ClaimResult.NOT_ENOUGHT_CLAIMS;
            if (chunkClaims.containsKey(chunk)) {
                if (chunkClaims.get(chunk).owningTeam == accessor.getTeam()){
                    return ClaimResult.ALREADY_YOURS;
                }
                return ClaimResult.ALREADY_CLAIMED;
            }
            ChunkData data = new ChunkData(accessor.getTeam(),player.getUuid());
            chunkClaims.put(chunk,data);
            save();
        }
        return ClaimResult.SUCCESS;
    }
    public static ClaimResult claim(Vector2i chunk, String team){
        if (chunkClaims.containsKey(chunk)) {
            if (chunkClaims.get(chunk).owningTeam == team){
                return ClaimResult.ALREADY_YOURS;
            }
            return ClaimResult.ALREADY_CLAIMED;
        }
        ChunkData data = new ChunkData(team,UUID.randomUUID());
        chunkClaims.put(chunk,data);
        save();
        return ClaimResult.SUCCESS;
    }

    public static boolean isSame(Vector2i oldChunk, Vector2i newChunk){
        if (!chunkClaims.containsKey(oldChunk) && !chunkClaims.containsKey(newChunk))
            return true;
        if (!chunkClaims.containsKey(oldChunk))
            return false;
        if (!chunkClaims.containsKey(newChunk))
            return false;

        return chunkClaims.get(newChunk).equals(chunkClaims.get(oldChunk));
    }
    public static Vector2i convert(ChunkPos chunkPos){
        return new Vector2i(chunkPos.x,chunkPos.z);
    }
    public static String getOwner(Vector2i chunk){
        if (chunkClaims.containsKey(chunk)){
            return chunkClaims.get(chunk).owningTeam;
        }else {
            return "";
        }
    }
    public static TeamManager getOwnedManager(ChunkPos pos){
        if (TeamManager.TEAMS.containsKey(getOwner(convert(pos)))){
            return TeamManager.TEAMS.get(getOwner(convert(pos)));
        }
        return TeamManager.WILDERNESS;
    }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (TEAMSFILE.exists()) {
            try (FileReader reader = new FileReader(TEAMSFILE)) {
                JsonArray array = gson.fromJson(reader, JsonArray.class);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject object = array.get(i).getAsJsonObject();
                    Vector2i chunk = new Vector2i(object.get("chunk").getAsJsonArray().get(0).getAsInt(), object.get("chunk").getAsJsonArray().get(1).getAsInt());
                    String team = object.get("team").getAsString();
                    UUID player = UUID.fromString(object.get("player").getAsString());
                    chunkClaims.put(chunk, new ChunkData(team,player));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            boolean created = TEAMSPATH.mkdirs();
            if (created) {
                System.out.println("Created Files");
            }
            save();
        }
    }
    public static void save(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        for (int i = 0; i<chunkClaims.size(); i++){
            Vector2i chunk = (Vector2i) chunkClaims.keySet().toArray()[i];
            ChunkData team = (ChunkData) chunkClaims.values().toArray()[i];
            JsonObject object = new JsonObject();
            JsonArray chunkVec = new JsonArray();
            chunkVec.add(chunk.x);
            chunkVec.add(chunk.y);

            object.addProperty("team",team.owningTeam);
            object.addProperty("player",team.owningPlayer.toString());
            object.add("chunk",chunkVec);
            array.add(object);
        }

        try (FileWriter writer = new FileWriter(TEAMSFILE)){
            gson.toJson(array,writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static class ChunkData {
        public final String owningTeam;
        public final UUID owningPlayer;
        public ChunkData(String owningTeam, UUID owningPlayer){
            this.owningTeam = owningTeam;
            this.owningPlayer = owningPlayer;
        }
    }
}
