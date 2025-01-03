package net.villagerzock.neocraft.Teams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.Commands;
import net.villagerzock.neocraft.Neocraft;
import net.villagerzock.neocraft.Registries;
import net.villagerzock.neocraft.Teams.Configurations.BooleanConfiguration;
import net.villagerzock.neocraft.Teams.Configurations.DisplayNameConfiguration;

public abstract class Configuration<T extends ArgumentBuilder<ServerCommandSource, T>> {
    public abstract ArgumentBuilder<ServerCommandSource,T> build(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment);

    public static final DisplayNameConfiguration DISPLAY_NAME_CONFIGURATION = register("displayname",new DisplayNameConfiguration());
    public static final BooleanConfiguration FRIENDLY_FIRE = register("friendly_fire",new BooleanConfiguration(Commands::friendlyFire));
    public static final BooleanConfiguration ATTACK_ENTITIES = register("attack_entities", new BooleanConfiguration(Commands::attack_entities));
    public static final BooleanConfiguration INTERACT_ENTITIES = register("interact_with_entities", new BooleanConfiguration(Commands::interact_entities));

    public static <T extends ArgumentBuilder<ServerCommandSource, T>,S extends Configuration<T>> S register(String id,S entry){
        return Registry.register(Registries.CONFIGURATIONS, Identifier.of(Neocraft.MODID,id),entry);
    }
    public static void staticLoad(){

    }
}
