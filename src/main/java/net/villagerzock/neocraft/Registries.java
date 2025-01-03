package net.villagerzock.neocraft;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.Teams.Configuration;
import net.villagerzock.neocraft.config.Config;
import org.apache.logging.log4j.core.LifeCycle;

public class Registries {
    public static final Registry<Configuration> CONFIGURATIONS;
    public static final Registry<Config<?>> CONFIG_TYPES;
    static {
        CONFIGURATIONS = create(RegistryKeys.CONFIGURATIONS);
        CONFIG_TYPES = create(RegistryKeys.CONFIG_TYPES);
    }
    public static <T> Registry<T> create(RegistryKey<Registry<T>> registryKey){
        return new SimpleRegistry<>(registryKey, Lifecycle.stable(), false);
    }
    public static class RegistryKeys {
        public static final RegistryKey<Registry<Configuration>> CONFIGURATIONS = of(ofThis("configurations"));
        public static final RegistryKey<Registry<Config<?>>> CONFIG_TYPES = of(ofThis("config_types"));
        private static Identifier ofThis(String id){
            return Identifier.of(Neocraft.MODID,id);
        }
        public static <T> RegistryKey<Registry<T>> of(Identifier identifier){
            return RegistryKey.ofRegistry(identifier);
        }
    }
}
