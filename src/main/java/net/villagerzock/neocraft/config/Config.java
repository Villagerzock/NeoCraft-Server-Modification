package net.villagerzock.neocraft.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.villagerzock.BetterCloneable;
import net.villagerzock.neocraft.Commands;
import net.villagerzock.neocraft.Neocraft;
import net.villagerzock.neocraft.Registries;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public abstract class Config<T> implements BetterCloneable<Config<T>> {
    public static final File CONFIGPATH;
    public static final File CONFIGFILE;
    public static IntegerConfig CLAIMS_PER_PERSON = register(new IntegerConfig(100,Identifier.of(Neocraft.MODID,"claims_per_person")));
    public static FloatConfig VILLAGER_DISTANCE = register(new FloatConfig(16f,Identifier.of(Neocraft.MODID,"villager_distance")));
    public static ArrayConfig<StringConfig> BANNED_NAMES = register(new ArrayConfig<StringConfig>(new ArrayList<>(),Identifier.of(Neocraft.MODID,"banned_names"),new StringConfig("",null)));
    public static StringConfig COMMAND_NAME = register(new StringConfig("neocraft",Identifier.of(Neocraft.MODID,"command_name")));
    public static StringConfig CHAT_MESSAGE_WEBHOOK = register(new StringConfig("https://discord.com/api/webhooks/1330628996492296244/GkZVuZ0v-xG9ulcZyfgLR66VVCILjVUwlvh47ebiVMLVQmrTFWATssCbmS-vApWq_ywa",Identifier.of(Neocraft.MODID,"chat_message_webhook")));

    protected final Identifier id;
    protected T value;
    public Config(T initialValue, Identifier id){
        this.id = id;
        this.value = initialValue;
    }
    public abstract JsonElement serialize();
    public abstract Config<T> deserialize(JsonElement element);
    public T get(){
        return value;
    }

    public static void load(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (CONFIGFILE.exists()){
            try (FileReader reader = new FileReader(CONFIGFILE)){
                JsonObject object = gson.fromJson(reader,JsonObject.class);
                for (int i = 0; i < Registries.CONFIG_TYPES.size(); i++) {
                    Config<?> config = Registries.CONFIG_TYPES.get(i);
                    Identifier id = Registries.CONFIG_TYPES.getId(config);
                    if (object.has(id.toString())){
                        config.deserialize(object.get(id.toString()));
                    }else {
                        save();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            boolean created = CONFIGPATH.mkdirs();
            if (created){
                System.out.println("Created Files");
            }
            save();
        }
    }
    public static <CONFIG extends Config<?>> CONFIG register(CONFIG config){
        Registry.register(Registries.CONFIG_TYPES,config.id,config);
        return config;
    }
    public static void save(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject object = new JsonObject();
        for (int i = 0; i < Registries.CONFIG_TYPES.size(); i++) {
            Config<?> config = Registries.CONFIG_TYPES.get(i);
            Identifier id = Registries.CONFIG_TYPES.getId(config);
            object.add(id.toString(),config.serialize());
        }
        try (FileWriter writer = new FileWriter(CONFIGFILE)){
            gson.toJson(object,writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void staticLoad(){
        load();
    }
    static {
        CONFIGPATH = FabricLoader.getInstance().getConfigDir().toFile();
        CONFIGFILE = new File(CONFIGPATH,"neocraft.json");
    }

    public static class IntegerConfig extends Config<Integer>{
        public IntegerConfig(Integer initialValue, Identifier id) {
            super(initialValue, id);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(this.value);
        }

        @Override
        public Config<Integer> deserialize(JsonElement element) {
            value = element.getAsInt();
            return this.betterClone();
        }

        @Override
        public Config<Integer> betterClone() {
            return new IntegerConfig(value,id);
        }
    }
    public static class FloatConfig extends Config<Float>{

        public FloatConfig(Float initialValue, Identifier id) {
            super(initialValue, id);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(this.value);
        }

        @Override
        public Config<Float> deserialize(JsonElement element) {
            value = element.getAsFloat();
            return this.betterClone();
        }

        @Override
        public Config<Float> betterClone() {
            return new FloatConfig(value,id);
        }
    }
    public static class StringConfig extends Config<String> {

        public StringConfig(String initialValue, Identifier id) {
            super(initialValue, id);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(value);
        }

        @Override
        public Config<String> deserialize(JsonElement element) {
            this.value = element.getAsString();
            return this.betterClone();
        }

        @Override
        public Config<String> betterClone() {
            return new StringConfig(value,id);
        }
    }
    public static class ArrayConfig<T extends Config<?>> extends Config<List<T>> implements Iterable<T>{
        private final T serializer;
        public ArrayConfig(List<T> initialValue, Identifier id,T serializer) {
            super(initialValue, id);
            this.serializer = serializer;
        }

        @Override
        public JsonElement serialize() {
            JsonArray array = new JsonArray();
            for (T t : value){
                array.add(t.serialize());
            }
            return array;
        }

        @Override
        public Config<List<T>> deserialize(JsonElement element) {
            JsonArray jsonArray = element.getAsJsonArray();
            List<T> values = new ArrayList<>();
            for (JsonElement jsonElement : jsonArray){
                values.add((T) serializer.deserialize(jsonElement));
            }
            this.value = values;
            return this.betterClone();
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return get().iterator();
        }
        public int size(){
            return value.size();
        }

        @Override
        public Config<List<T>> betterClone() {
            return new ArrayConfig<>(value,id,serializer);
        }
    }
}
