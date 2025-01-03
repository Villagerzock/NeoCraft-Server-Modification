package net.villagerzock.neocraft.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.Commands;
import net.villagerzock.neocraft.Neocraft;
import net.villagerzock.neocraft.Registries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public abstract class Config<T> {
    public static final File CONFIGPATH;
    public static final File CONFIGFILE;
    public static IntegerConfig CLAIMS_PER_PERSON = register(new IntegerConfig(100,Identifier.of(Neocraft.MODID,"claims_per_person")));
    public static IntegerConfig VILLAGER_DISTANCE = register(new IntegerConfig(16,Identifier.of(Neocraft.MODID,"villager_distance")));

    protected final Identifier id;
    protected T value;
    public Config(T initialValue, Identifier id){
        this.id = id;
        this.value = initialValue;
    }
    public abstract JsonElement serialize();
    public abstract void deserialize(JsonElement element);
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

    }
    static {
        CONFIGPATH = FabricLoader.getInstance().getConfigDir().toFile();
        CONFIGFILE = new File(CONFIGPATH,"neocraft.json");
        load();
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
        public void deserialize(JsonElement element) {
            value = element.getAsInt();
        }
    }
}
