package net.villagerzock.neocraft.Teams.Configurations;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.villagerzock.neocraft.Commands;
import net.villagerzock.neocraft.Teams.Configuration;

import java.util.function.Consumer;
import java.util.function.Function;

public class BooleanConfiguration extends Configuration<RequiredArgumentBuilder<ServerCommandSource,Boolean>> {
    private final ConfigurationFunction<Boolean> command;
    public BooleanConfiguration(ConfigurationFunction<Boolean> command){
        this.command = command;
    }
    @Override
    public ArgumentBuilder<ServerCommandSource, RequiredArgumentBuilder<ServerCommandSource, Boolean>> build(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        return CommandManager.argument("boolean", BoolArgumentType.bool()).executes(commandContext -> {
            return command.apply(commandContext.getSource(),commandContext.getArgument("boolean",Boolean.class));
        });
    }
}
