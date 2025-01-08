package net.villagerzock.neocraft.Teams;

import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.villagerzock.neocraft.Neocraft;
import net.villagerzock.neocraft.PlayerAccessor;
import net.villagerzock.neocraft.config.Config;

import java.io.*;
import java.util.*;
import java.util.function.IntFunction;

public class TeamManager {
    //EMPTY
    public static final TeamManager WILDERNESS = new TeamManager(Text.empty(),"",UUID.randomUUID());
    //Static
    public static Map<String,TeamManager> TEAMS = new HashMap<>();
    public static final File TEAMSPATH;
    public static final File TEAMSFILE;
    public final UUID owner;
    public int maxClaims = 1;
    public List<String> invites = new ArrayList<>();

    //Object data
    protected Text displayName;
    protected String id;
    public Settings settings;

    public TeamManager(Text displayName, String id, UUID uuid){
        this.displayName = displayName;
        this.settings = new Settings();
        this.id = id;
        this.owner = uuid;
    }
    public List<ServerPlayerEntity> findAllMembers(MinecraftServer server){
        List<ServerPlayerEntity> allPlayers = server.getPlayerManager().getPlayerList();
        List<ServerPlayerEntity> teamMembers = new ArrayList<>();
        for (ServerPlayerEntity player : allPlayers){
            if (player instanceof PlayerAccessor accessor){
                if (accessor.getTeam() == id){
                    teamMembers.add(player);
                }
            }
        }
        return teamMembers;
    }
    public void join(){
        maxClaims++;
    }
    public void leave(){
        --maxClaims;
    }
    public TeamManager(Text displayName,Settings settings,String id,UUID uuid){
        this.displayName = displayName;
        this.settings = settings;
        this.id = id;
        this.owner = uuid;
    }

    public Text getDisplayName() {
        return displayName;
    }

    public void changeDisplayname(Text displayName){
        this.displayName = displayName;
        save();
    }
    public void changeDisplayname(String displayName){
        Text text = Text.empty();
        try {
            JsonElement element = new Gson().fromJson(displayName,JsonElement.class);
            text = TextCodecs.CODEC.
                    decode(JsonOps.INSTANCE,element)
                    .getOrThrow()
                    .getFirst();
        }catch (Exception ignored){

        }
        this.displayName = text;
    }
    public JsonObject serialize(){
        JsonObject object = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        object.add("display_name",gson.toJsonTree(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE,displayName).getOrThrow()));
        object.addProperty("uuid",this.owner.toString());
        object.addProperty("maxClaims",maxClaims);
        object.add("settings",this.settings.serialize());
        return object;
    }
    public static TeamManager deserialize(JsonObject object,String id){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Text displayName = TextCodecs.CODEC.
                decode(JsonOps.INSTANCE,object.get("display_name"))
                .getOrThrow()
                .getFirst();
        System.out.println("Amount of Banned Names: " + Config.BANNED_NAMES.size());
        for (Config.StringConfig config : Config.BANNED_NAMES){
            String s = config.get();
            System.out.println("Checking for Banned Name: " + s);
            if (displayName.getString().toLowerCase().contains(s.toLowerCase())){
                displayName = Text.literal("§cBANNED NAME");
                System.out.println("Found Banned Name: " + s);
            }

        }
        UUID uuid = UUID.fromString(object.get("uuid").getAsString());
        int maxClaims = object.get("maxClaims").getAsInt();
        TeamManager manager = new TeamManager(displayName,id,uuid);
        manager.settings.deserialize(object.get("settings"));
        manager.maxClaims = maxClaims;
        return manager;
    }
    static {
        TEAMSPATH = new File(FabricLoader.getInstance().getGameDir().toFile(),"neocraft");
        TEAMSFILE = new File(TEAMSPATH,"teams.json");
        load();
    }
    public static void load(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (TEAMSFILE.exists()){
            try (FileReader reader = new FileReader(TEAMSFILE)){
                JsonArray array = gson.fromJson(reader,JsonArray.class);
                for (int i = 0; i<array.size(); i++){
                    JsonObject object = array.get(i).getAsJsonObject();
                    String name = object.get("name").getAsString();
                    TeamManager manager = deserialize(object.get("team").getAsJsonObject(),name);
                    TEAMS.put(name,manager);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            boolean created = TEAMSPATH.mkdirs();
            if (created){
                System.out.println("Created Files");
            }
        }
        save();
    }

    public static void save(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        for (int i = 0; i<TEAMS.size(); i++){
            String name = (String) TEAMS.keySet().toArray()[i];
            TeamManager manager = (TeamManager) TEAMS.values().toArray()[i];
            JsonObject object = new JsonObject();
            object.addProperty("name",name);
            object.add("team",manager.serialize());
            array.add(object);
        }
        try (FileWriter writer = new FileWriter(TEAMSFILE)){
            gson.toJson(array,writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Optional<TeamManager> create(String teamName, UUID owner){
        if (TEAMS.containsKey(teamName)){
            return Optional.empty();
        }else {
            TeamManager tm = new TeamManager(Text.literal(teamName),teamName,owner);
            Optional<TeamManager> teamManager = Optional.of(tm);
            TEAMS.put(teamName,tm);
            save();
            return teamManager;
        }
    }
    public static void staticLoad(){

    }
    public static TeamManager get(String team){
        if (TEAMS.containsKey(team)){
            return TEAMS.get(team);
        }
        return WILDERNESS;
    }

    public Settings getSettings() {
        System.out.println("Get Settings");
        return settings;
    }
    public void setDisplayName(Text text){
        this.displayName = text;
        if (MinecraftClient.getInstance() != null){
            if (MinecraftClient.getInstance().world != null) {
                if (MinecraftClient.getInstance().world.isClient){
                }
            }
        }
    }
    public void setSettings(Settings settings){
        this.settings = settings;
        if (MinecraftClient.getInstance() != null){
            if (MinecraftClient.getInstance().world != null) {
                if (MinecraftClient.getInstance().world.isClient){
                }
            }
        }
    }

    public static class Settings{
        public boolean friendlyFire = false;
        public boolean attack_entities = false;
        public boolean interact_entities = false;
        public Settings(){}
        public JsonElement serialize(){
            JsonObject object = new JsonObject();
            object.addProperty("interact_with_entities",interact_entities);
            return object;
        }
        public void deserialize(JsonElement element){
            JsonObject object = element.getAsJsonObject();
            this.interact_entities = object.get("interact_with_entities").getAsBoolean();
        }
    }
    public enum Setting{
        EVERYONE(Text.literal("Jeder"),0),
        ALLY(Text.literal("Verbündeter"),1),
        MEMBERS(Text.literal("Mitglieder"),2);
        private static final IntFunction<Setting> BY_ID = ValueLists.createIdToValueFunction(Setting::getId, values(), ValueLists.OutOfBoundsHandling.WRAP);
        static {
        }
        Text text;
        int id;
        Setting(Text text, int id){
            this.text = text;
            this.id = id;
        }
        public Text getText(){
            return this.text;
        }

        public int getId() {
            return id;
        }
        public static Setting byId(int id){
            return (Setting) BY_ID.apply(id);
        }
    }
}
