package net.villagerzock.neocraft.Teams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.villagerzock.neocraft.Neocraft;
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
    //public static Map<Vector2i,ChunkData> chunkClaims = new HashMap<>();
    public static Map<World,Map<Vector2i,ChunkData>> chunkClaims = new HashMap<>();
    public static final File TEAMSPATH;
    public static final File TEAMSFILE;
    private static MinecraftServer server;
    static {
        TEAMSPATH = new File(FabricLoader.getInstance().getGameDir().toFile(),"neocraft");
        TEAMSFILE = new File(TEAMSPATH,"chunkdata.json");

    }
    public static void staticLoad(MinecraftServer server){
        ChunkManager.server = server;
        for (World world : server.getWorlds()){
            chunkClaims.put(world,new HashMap<>());
        }
        load();
    }

    public static int getAmountOfClaimedChunks(PlayerEntity player){
        AtomicInteger amount = new AtomicInteger();
        try {
            chunkClaims.forEach((vector2i, chunkData) -> {
                chunkData.forEach((vector2i1, data) -> {
                    if (player instanceof PlayerAccessor accessor){
                        if (data.getOwningTeam() == accessor.getTeam())
                            amount.getAndIncrement();
                    }
                });
            });
        }catch (Throwable t){
            t.printStackTrace();
        }
        return amount.get();
    }
    public static ClaimResult claim(Vector2i chunk, PlayerEntity player,boolean force){
        try{
            if (player instanceof PlayerAccessor accessor){
                if (force){
                    ChunkData data = new ChunkData(player.getUuid(), player.getWorld());
                    chunkClaims.get(player.getWorld()).put(chunk,data);
                    System.out.println("Forcing Claim");
                    save();
                    return ClaimResult.SUCCESS;
                }
                TeamManager manager = accessor.getTeamManager();
                if (manager.maxClaims * Config.CLAIMS_PER_PERSON.get() <= getAmountOfClaimedChunks(player))
                    return ClaimResult.NOT_ENOUGHT_CLAIMS;
                if (chunkClaims.get(player.getWorld()).containsKey(chunk)) {
                    if (chunkClaims.get(player.getWorld()).get(chunk).getOwningTeam() == accessor.getTeam()){
                        return ClaimResult.ALREADY_YOURS;
                    }
                    return ClaimResult.ALREADY_CLAIMED;
                }
                ChunkData data = new ChunkData(player.getUuid(), player.getWorld());
                chunkClaims.get(player.getWorld()).put(chunk,data);
                Neocraft.addMarkerForChunk(convert(chunk),data.getOwningTeam(),data);
                save();
            }
        }catch (Throwable t){
            t.printStackTrace();
            return ClaimResult.ERROR_OCCURED;
        }
        return ClaimResult.SUCCESS;
    }

    public static boolean isSame(World world,Vector2i oldChunk, Vector2i newChunk){
        try {
            if (!chunkClaims.get(world).containsKey(oldChunk) && !chunkClaims.get(world).containsKey(newChunk))
                return true;
            if (!chunkClaims.get(world).containsKey(oldChunk))
                return false;
            if (!chunkClaims.get(world).containsKey(newChunk))
                return false;

            return chunkClaims.get(world).get(newChunk).getOwningTeam().equals(chunkClaims.get(world).get(oldChunk).getOwningTeam());
        }catch (Throwable t){
            t.printStackTrace();
            return false;
        }

    }
    public static Vector2i convert(ChunkPos chunkPos){
        return new Vector2i(chunkPos.x,chunkPos.z);
    }
    public static ChunkPos convert(Vector2i chunkPos){
        return new ChunkPos(chunkPos.x,chunkPos.y);
    }
    public static String getOwner(World world,Vector2i chunk){
        if (chunkClaims.containsKey(chunk)){
            if (TeamManager.get(chunkClaims.get(world).get(chunk).getOwningTeam()) == TeamManager.WILDERNESS){
                server.getPlayerManager().getPlayer(chunkClaims.get(world).get(chunk).owningPlayer).getDisplayName();
            }
            return chunkClaims.get(world).get(chunk).getOwningTeam();
        }else {
            return "";
        }
    }
    public static TeamManager getOwnedManager(World world,ChunkPos pos){
        if (chunkClaims.containsKey(world) && chunkClaims.get(world).containsKey(pos)){
            return ((PlayerAccessor)(chunkClaims.get(world).get(pos).getOwner())).getTeamManager();
        }
        return TeamManager.WILDERNESS;
    }
    public static void ChangeTeam(PlayerEntity player, String newTeam){
    }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (TEAMSFILE.exists()) {
            try (FileReader reader = new FileReader(TEAMSFILE)) {
                JsonArray array = gson.fromJson(reader, JsonArray.class);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject object = array.get(i).getAsJsonObject();
                    Vector2i chunk = new Vector2i(object.get("chunk").getAsJsonArray().get(0).getAsInt(), object.get("chunk").getAsJsonArray().get(1).getAsInt());
                    UUID player = UUID.fromString(object.get("player").getAsString());
                    World world = Neocraft.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(object.get("dim").getAsString())));
                    ChunkData data = new ChunkData(player, world);
                    chunkClaims.get(world).put(chunk, data);
                    Neocraft.addMarkerForChunk(convert(chunk),data.getOwningTeam(),data);
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
            World world = chunkClaims.keySet().toArray(World[]::new)[i];
            Map<Vector2i,ChunkData> map = chunkClaims.get(world);
            for (int j = 0; j < map.size(); j++) {
                Vector2i chunk = map.keySet().toArray(Vector2i[]::new)[j];
                ChunkData team = map.values().toArray(ChunkData[]::new)[j];
                JsonObject object = new JsonObject();
                JsonArray chunkVec = new JsonArray();
                System.out.println(chunk.x + ":" + chunk.y);
                chunkVec.add(chunk.x);
                chunkVec.add(chunk.y);

                object.addProperty("player",team.owningPlayer.toString());
                object.add("chunk",chunkVec);

                object.addProperty("dim",team.world.getRegistryKey().getValue().toString());

                array.add(object);
            }
        }

        try (FileWriter writer = new FileWriter(TEAMSFILE)){
            gson.toJson(array,writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static class ChunkData {
        public String owningTeam;
        public final UUID owningPlayer;
        public World world;
        public ChunkData(UUID owningPlayer, World world){
            this.owningPlayer = owningPlayer;
            this.world = world;
        }

        public String  getOwningTeam() {
            if (world.getServer().getPlayerManager().getPlayer(owningPlayer) instanceof PlayerAccessor accessor){
                return accessor.getTeam();
            }
            return "";
        }
        public PlayerEntity getOwner() {
            return world.getServer().getPlayerManager().getPlayer(owningPlayer);
        }
    }
}
